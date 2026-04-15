package com.smartsales.prism.ui.sim

import android.util.Log
import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.data.audio.SimAudioRepository
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.SessionKind
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class SimAgentChatCoordinator(
    private val sessionCoordinator: SimAgentSessionCoordinator,
    private val audioRepository: SimAudioRepository,
    private val executor: Executor,
    private val userProfileRepository: UserProfileRepository,
    private val bridge: SimAgentUiBridge
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val pendingAutoTitleSnapshots = mutableMapOf<String, AutoTitleSnapshot>()
    private val autoTitleRetryBudget = mutableMapOf<String, Int>()

    fun startSeededSession(initialUserInput: String, sendAction: () -> Unit): String? {
        if (initialUserInput.isBlank()) return null
        sessionCoordinator.startNewSession()
        val sessionId = sessionCoordinator.createGeneralSession()
        bridge.setInputText(initialUserInput)
        sendAction()
        return sessionId
    }

    fun startSchedulerShelfSession(initialUserInput: String, startSeededSession: (String) -> Unit) {
        if (initialUserInput.isBlank()) return
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
            payloadSize = initialUserInput.length,
            summary = SIM_SCHEDULER_SHELF_SESSION_STARTED_SUMMARY,
            rawDataDump = initialUserInput
        )
        Log.d(
            SIM_SCHEDULER_SHELF_LOG_TAG,
            "scheduler shelf seeded chat session started: $initialUserInput"
        )
        startSeededSession(initialUserInput)
    }

    // 音频讨论会话路由：
    // 1. 已有绑定会话 → 切回
    // 2. 无绑定 → 创建专属新会话（不污染当前通用聊天）
    fun openAudioDiscussion(
        audioId: String,
        title: String,
        summary: String?,
        summaryLabel: String = "当前预览"
    ): String {
        // 优先检查：该音频是否已有绑定会话
        sessionCoordinator.existingSessionIdForAudio(audioId)?.let { existing ->
            sessionCoordinator.markSessionHasAudioContextHistory(existing)
            sessionCoordinator.switchSession(existing)
            return existing
        }

        // 无绑定 → 创建专属音频讨论会话
        val sessionId = UUID.randomUUID().toString()
        val preview = SessionPreview(
            id = sessionId,
            clientName = UNTITLED_AUDIO_SESSION_TITLE,
            summary = (summary ?: "音频讨论").take(6),
            timestamp = System.currentTimeMillis(),
            linkedAudioId = audioId,
            sessionKind = SessionKind.AUDIO_GROUNDED
        )
        val firstMessage = ChatMessage.Ai(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            uiState = UiState.Response(buildAudioDiscussionIntro(title, summary, summaryLabel))
        )
        return sessionCoordinator.createSession(
            preview = preview,
            messages = listOf(firstMessage),
            autoSelect = true,
            bindLinkedAudio = true
        ).also { sessionCoordinator.markSessionHasAudioContextHistory(it) }
    }

    fun selectAudioForChat(
        audioId: String,
        title: String,
        summary: String?,
        entersPendingFlow: Boolean
    ): String {
        val effectiveSummary = if (entersPendingFlow) {
            "SIM 已接管该音频，正在自动提交 Tingwu 转写任务。完成后结果会同步回录音抽屉。"
        } else {
            summary
        }
        val summaryLabel = if (entersPendingFlow) "当前状态" else "当前预览"
        val sessionId = openAudioDiscussion(audioId, title, effectiveSummary, summaryLabel)
        if (entersPendingFlow) {
            updatePendingAudioState(audioId, TranscriptionStatus.PENDING, 0f)
        } else if (sessionCoordinator.currentSessionId() == sessionId) {
            bridge.setUiState(UiState.Idle)
        }
        return sessionId
    }

    fun updatePendingAudioState(audioId: String, status: TranscriptionStatus, progress: Float) {
        val sessionId = sessionCoordinator.existingSessionIdForAudio(audioId) ?: return
        if (sessionCoordinator.currentSessionId() != sessionId) return

        val sessionTitle = sessionCoordinator.getSession(sessionId)?.preview?.clientName ?: "当前音频"
        bridge.setUiState(
            UiState.Thinking(
                hint = buildPendingAudioHint(
                    title = sessionTitle,
                    status = status,
                    progress = progress
                )
            )
        )
    }

    fun completePendingAudio(audioId: String) {
        val sessionId = sessionCoordinator.existingSessionIdForAudio(audioId) ?: return
        val title = sessionCoordinator.getSession(sessionId)?.preview?.clientName ?: "当前音频"
        sessionCoordinator.appendAiMessage(
            sessionId = sessionId,
            uiState = UiState.Response("《$title》转写已完成，结果已同步回录音抽屉，现在可以继续围绕这段音频讨论。")
        )
        if (sessionCoordinator.currentSessionId() == sessionId) {
            bridge.setUiState(UiState.Idle)
        }
    }

    fun appendCompletedAudioArtifacts(audioId: String, artifacts: TingwuJobArtifacts) {
        val sessionId = sessionCoordinator.existingSessionIdForAudio(audioId) ?: return
        val record = sessionCoordinator.getSession(sessionId) ?: return
        val title = record.preview.clientName
        val alreadyPresent = record.messages.any { message ->
            val state = (message as? ChatMessage.Ai)?.uiState
            state is UiState.AudioArtifacts && state.audioId == audioId
        }
        if (alreadyPresent) {
            if (sessionCoordinator.currentSessionId() == sessionId) {
                bridge.setUiState(UiState.Idle)
            }
            return
        }

        sessionCoordinator.appendAiMessage(
            sessionId = sessionId,
            uiState = UiState.AudioArtifacts(
                audioId = audioId,
                title = title,
                artifactsJson = json.encodeToString(artifacts)
            )
        )
        if (sessionCoordinator.currentSessionId() == sessionId) {
            bridge.setUiState(UiState.Idle)
        }
    }

    fun failPendingAudio(audioId: String, message: String) {
        val sessionId = sessionCoordinator.existingSessionIdForAudio(audioId)
            ?: sessionCoordinator.currentSessionId()
            ?: return
        sessionCoordinator.appendAiMessage(
            sessionId = sessionId,
            uiState = UiState.Error(message)
        )
        if (sessionCoordinator.currentSessionId() == sessionId) {
            bridge.setUiState(UiState.Error(message))
        }
    }

    suspend fun handleGeneralSend(sessionId: String, content: String) {
        val record = sessionCoordinator.getSession(sessionId)
        if (record == null) {
            bridge.setIsSending(false)
            bridge.setUiState(UiState.Idle)
            return
        }

        delay(180)
        val prompt = buildGeneralChatPrompt(record, content)
        when (val result = executor.execute(ModelRegistry.COACH, prompt)) {
            is ExecutorResult.Success -> {
                val aiReply = result.content.ifBlank {
                    "我在这里，刚刚没有组织出合适的回复。你可以换个说法继续聊。"
                }
                sessionCoordinator.appendAiMessage(
                    sessionId,
                    UiState.Response(aiReply)
                )
                tryGenerateSessionTitle(sessionId, aiReply)
                bridge.setIsSending(false)
                bridge.setUiState(UiState.Idle)
            }

            is ExecutorResult.Failure -> {
                sessionCoordinator.appendAiMessage(
                    sessionId,
                    UiState.Error(
                        "当前无法继续这段聊天，请稍后重试。错误：${result.error}",
                        retryable = result.retryable
                    )
                )
                bridge.setIsSending(false)
                bridge.setUiState(UiState.Error("聊天暂时不可用"))
            }
        }
    }

    suspend fun handleAudioGroundedSend(sessionId: String, latestUserInput: String) {
        val record = sessionCoordinator.getSession(sessionId)
        if (record == null) {
            bridge.setIsSending(false)
            bridge.setUiState(UiState.Idle)
            return
        }

        val artifacts = loadGroundingArtifacts(record)
        if (artifacts == null) {
            sessionCoordinator.appendAiMessage(
                sessionId,
                UiState.Error(
                    "当前讨论尚未加载这段录音的转写结果。请先从录音抽屉打开已转写录音，或等待转写完成后再继续提问。",
                    retryable = false
                )
            )
            bridge.setIsSending(false)
            bridge.setUiState(UiState.Error("缺少可用的录音上下文"))
            return
        }

        val prompt = buildAudioGroundedPrompt(
            record = record,
            artifacts = artifacts,
            latestUserInput = latestUserInput
        )
        Log.d(
            SIM_AUDIO_CHAT_LOG_TAG,
            "audio-grounded chat prompt built for audioId=${record.preview.linkedAudioId}"
        )

        when (val result = executor.execute(ModelRegistry.COACH, prompt)) {
            is ExecutorResult.Success -> {
                sessionCoordinator.appendAiMessage(
                    sessionId,
                    UiState.Response(
                        result.content.ifBlank {
                            "我暂时没能从这段录音里整理出可回答的内容，请换个问法试试。"
                        }
                    )
                )
                tryGenerateSessionTitle(
                    sessionId,
                    result.content.ifBlank {
                        "我暂时没能从这段录音里整理出可回答的内容，请换个问法试试。"
                    }
                )
                bridge.setIsSending(false)
                bridge.setUiState(UiState.Idle)
            }

            is ExecutorResult.Failure -> {
                sessionCoordinator.appendAiMessage(
                    sessionId,
                    UiState.Error(
                        "当前无法继续这段录音的讨论，请稍后重试。错误：${result.error}",
                        retryable = result.retryable
                    )
                )
                bridge.setIsSending(false)
                bridge.setUiState(UiState.Error("录音讨论暂时不可用"))
            }
        }
    }

    private suspend fun loadGroundingArtifacts(record: SimSessionRecord): TingwuJobArtifacts? {
        val audioId = record.preview.linkedAudioId ?: return null
        return audioRepository.getArtifacts(audioId) ?: extractArtifactsFromHistory(
            messages = record.messages,
            audioId = audioId
        )
    }

    private fun extractArtifactsFromHistory(
        messages: List<ChatMessage>,
        audioId: String
    ): TingwuJobArtifacts? {
        val state = messages
            .asReversed()
            .mapNotNull { (it as? ChatMessage.Ai)?.uiState as? UiState.AudioArtifacts }
            .firstOrNull { it.audioId == audioId }
            ?: return null
        return runCatching {
            json.decodeFromString(TingwuJobArtifacts.serializer(), state.artifactsJson)
        }.getOrNull()
    }

    private fun buildAudioDiscussionIntro(
        title: String,
        summary: String?,
        summaryLabel: String
    ): String {
        return buildString {
            append("已接入《")
            append(title)
            append("》的录音上下文。")
            if (summaryLabel == "当前状态" && !summary.isNullOrBlank()) {
                append("\n\n")
                append(summaryLabel)
                append("：")
                append(summary)
            } else {
                append("\n\n结构化结果已载入，现在可以继续围绕这段录音讨论。")
            }
        }
    }


    private fun buildGeneralChatPrompt(
        record: SimSessionRecord,
        latestUserInput: String
    ): String {
        val profile = userProfileRepository.profile.value
        val conversationContext = buildAudioConversationContext(record.messages)
        return buildString {
            appendLine("你是 SIM，一位轻量、直接、可信的中文聊天助手。")
            appendLine("你现在运行在独立的 SIM 壳层内，不是智能代理系统，不要假装自己能调工具、改数据库、执行任务或访问隐藏系统。")
            appendLine("你的职责是基于用户资料和当前会话上下文，给用户自然、有帮助、不过度夸张的回复。")
            appendLine("如果用户需要录音相关讨论，可以提醒他通过录音抽屉补充上下文；但在没有录音时，也要正常聊天。")
            appendLine("回答使用简洁自然的中文。")
            appendLine()
            appendLine("用户资料：")
            appendLine("姓名：${profile.displayName}")
            appendLine("角色：${profile.role}")
            appendLine("行业：${profile.industry}")
            appendLine("经验等级：${profile.experienceLevel}")
            profile.experienceYears.takeIf { it.isNotBlank() }?.let {
                appendLine("从业时长：$it")
            }
            profile.communicationPlatform.takeIf { it.isNotBlank() }?.let {
                appendLine("常用沟通平台：$it")
            }
            appendLine("偏好语言：${profile.preferredLanguage}")
            appendLine()
            appendLine("当前会话标题：${record.preview.clientName}")
            appendLine("最近对话：")
            appendLine(conversationContext.ifBlank { "无" })
            appendLine()
            appendLine("用户刚刚说：")
            appendLine(latestUserInput)
        }
    }

    private fun buildAudioGroundedPrompt(
        record: SimSessionRecord,
        artifacts: TingwuJobArtifacts,
        latestUserInput: String
    ): String {
        val title = record.preview.clientName
        val transcript = truncateForPrompt(artifacts.transcriptMarkdown, 6_000)
        val summary = truncateForPrompt(artifacts.smartSummary?.summary, 1_200)
        val highlights = artifacts.smartSummary?.keyPoints
            ?.take(8)
            ?.joinToString("\n") { "- $it" }
        val speakerSummaries = artifacts.smartSummary?.speakerSummaries
            ?.take(8)
            ?.joinToString("\n") { item ->
                val label = item.name?.takeIf { it.isNotBlank() } ?: "发言人"
                "- $label：${item.summary}"
            }
        val questionAnswers = artifacts.smartSummary?.questionAnswers
            ?.take(8)
            ?.joinToString("\n\n") { item ->
                "Q: ${item.question}\nA: ${item.answer}"
            }
        val chapters = artifacts.chapters
            ?.take(8)
            ?.joinToString("\n") { chapter ->
                buildString {
                    append("- ")
                    append(chapter.title)
                    chapter.summary?.takeIf { it.isNotBlank() }?.let {
                        append("：")
                        append(it)
                    }
                }
            }
        val conversationContext = buildAudioConversationContext(record.messages)

        return buildString {
            appendLine("你是 SIM 的录音讨论助手。")
            appendLine("你的职责仅限于围绕当前选中的录音内容回答。")
            appendLine("不要把自己说成智能代理、任务执行器或通用系统助手。")
            appendLine("如果录音内容里没有答案，必须明确说明“这段录音里没有提到”或“我无法从这段录音确认”。")
            appendLine("回答使用简洁自然的中文。")
            appendLine()
            appendLine("当前录音标题：$title")
            record.preview.linkedAudioId?.let { appendLine("当前录音 ID：$it") }
            appendLine()
            appendLine("摘要：")
            appendLine(summary ?: "无")
            appendLine()
            appendLine("重点：")
            appendLine(highlights ?: "无")
            appendLine()
            appendLine("发言人总结：")
            appendLine(speakerSummaries ?: "无")
            appendLine()
            appendLine("问答回顾：")
            appendLine(questionAnswers ?: "无")
            appendLine()
            appendLine("章节：")
            appendLine(chapters ?: "无")
            appendLine()
            appendLine("转写内容（节选）：")
            appendLine(transcript ?: "无")
            appendLine()
            appendLine("最近对话：")
            appendLine(conversationContext.ifBlank { "无" })
            appendLine()
            appendLine("用户刚刚的问题：")
            appendLine(latestUserInput)
        }
    }

    private fun buildAudioConversationContext(messages: List<ChatMessage>): String {
        return messages
            .takeLast(8)
            .mapNotNull { message ->
                when (message) {
                    is ChatMessage.User -> "用户：${message.content}"
                    is ChatMessage.Ai -> when (val state = message.uiState) {
                        is UiState.Response -> "助手：${state.content}"
                        is UiState.Error -> "助手：${state.message}"
                        else -> null
                    }
                }
            }
            .joinToString("\n")
    }

    private fun truncateForPrompt(value: String?, maxChars: Int): String? {
        val normalized = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return if (normalized.length <= maxChars) {
            normalized
        } else {
            normalized.take(maxChars) + "\n[已截断]"
        }
    }

    private fun buildPendingAudioHint(
        title: String,
        status: TranscriptionStatus,
        progress: Float
    ): String {
        return when (status) {
            TranscriptionStatus.PENDING -> "已选择《$title》，正在提交 Tingwu 任务"
            TranscriptionStatus.TRANSCRIBING -> when {
                progress < 0.2f -> "《$title》已提交 Tingwu，正在准备转写"
                progress < 0.7f -> "《$title》正在转写中"
                else -> "《$title》正在整理转写结果"
            }
            TranscriptionStatus.TRANSCRIBED -> "《$title》转写已完成"
        }
    }

    // 会话自动命名：只看首个真实助手答复；若首轮生成失败或过于泛化，仅允许下一次真实答复重试一次。
    private suspend fun tryGenerateSessionTitle(
        sessionId: String,
        latestAssistantReply: String? = null
    ) {
        val record = sessionCoordinator.getSession(sessionId) ?: return
        if (!shouldAutoTitle(record.preview) ||
            record.preview.sessionKind == SessionKind.SCHEDULER_FOLLOW_UP
        ) {
            pendingAutoTitleSnapshots.remove(sessionId)
            autoTitleRetryBudget.remove(sessionId)
            return
        }
        val remainingAttempts = autoTitleRetryBudget[sessionId] ?: AUTO_TITLE_MAX_ATTEMPTS
        if (remainingAttempts <= 0) {
            return
        }
        val existingSnapshot = pendingAutoTitleSnapshots[sessionId]
        val latestEligibleReply = latestAssistantReply
            ?.trim()
            ?.takeIf(::isEligibleAutoTitleReply)
        val candidateReply = when {
            existingSnapshot == null -> {
                extractFirstEligibleAutoTitleReply(record.messages) ?: latestEligibleReply
            }
            remainingAttempts == AUTO_TITLE_RETRY_ATTEMPTS && latestEligibleReply != null -> {
                latestEligibleReply
            }
            else -> existingSnapshot.assistantReply
        }
        val shouldAttempt = when {
            candidateReply.isNullOrBlank() -> false
            existingSnapshot == null -> true
            remainingAttempts == AUTO_TITLE_RETRY_ATTEMPTS && latestEligibleReply != null -> true
            else -> false
        }
        if (!shouldAttempt || candidateReply == null) {
            return
        }
        val snapshot = AutoTitleSnapshot(candidateReply)
        pendingAutoTitleSnapshots[sessionId] = snapshot
        autoTitleRetryBudget.putIfAbsent(sessionId, AUTO_TITLE_MAX_ATTEMPTS)

        try {
            autoTitleRetryBudget[sessionId] = remainingAttempts - 1
            val titlePrompt = buildTitlePrompt(snapshot.assistantReply)
            when (val result = executor.execute(ModelRegistry.COACH, titlePrompt)) {
                is ExecutorResult.Success -> {
                    val title = sanitizeGeneratedTitle(result.content)
                    if (isValidGeneratedTitle(record.preview, title)) {
                        sessionCoordinator.updateSession(sessionId) { r ->
                            r.copy(
                                preview = r.preview.copy(clientName = title)
                            )
                        }
                        if (sessionCoordinator.currentSessionId() == sessionId) {
                            bridge.setSessionTitle(title)
                        }
                        bridge.bumpSessionTitleInterruptToken()
                        pendingAutoTitleSnapshots.remove(sessionId)
                        autoTitleRetryBudget.remove(sessionId)
                        Log.d(TAG, "Auto-titled session $sessionId: $title")
                    } else {
                        Log.d(TAG, "Auto title rejected for session $sessionId: ${result.content}")
                    }
                }
                is ExecutorResult.Failure -> {
                    Log.d(TAG, "Title generation failed: ${result.error}")
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Title generation error", e)
        }
    }

    private fun extractFirstEligibleAutoTitleReply(messages: List<ChatMessage>): String? {
        val firstUserTimestamp = messages
            .filterIsInstance<ChatMessage.User>()
            .minOfOrNull { it.timestamp }
            ?: return null
        return messages
            .asSequence()
            .mapNotNull { it as? ChatMessage.Ai }
            .filter { it.timestamp >= firstUserTimestamp }
            .mapNotNull { (it.uiState as? UiState.Response)?.content?.trim() }
            .firstOrNull(::isEligibleAutoTitleReply)
    }

    private fun buildTitlePrompt(assistantReply: String): String {
        return buildString {
            appendLine("请根据以下这一次助手答复，为当前会话生成一个4-6个中文字的中文标题。")
            appendLine("标题必须抓住这次答复里的具体问题、任务、对象或讨论焦点。")
            appendLine("不要总结助手的人设、用户职业、行业、背景或泛化领域。")
            appendLine("如果答复主要是寒暄、泛泛自我介绍、行业/角色判断，或看不出明确主题，请只返回 NO_TITLE。")
            appendLine("坏例子：教育行业、教育管理、销售顾问、沟通建议。")
            appendLine("好例子：预算复盘、试点启动、客户跟进、转写问答。")
            appendLine("只返回标题本身或 NO_TITLE，不要标点，不要解释。")
            appendLine()
            appendLine("助手答复：${assistantReply.take(AUTO_TITLE_REPLY_MAX_CHARS)}")
        }
    }

    private fun shouldAutoTitle(preview: SessionPreview): Boolean {
        if (preview.sessionKind == SessionKind.SCHEDULER_FOLLOW_UP) return false
        if (preview.clientName in UNTITLED_SESSION_NAMES) return true
        
        val cn = preview.clientName.trim()
        if (cn.endsWith(".mp3", ignoreCase = true) || 
            cn.endsWith(".wav", ignoreCase = true) || 
            cn.endsWith(".m4a", ignoreCase = true)) {
            return true
        }
        
        val linkedAudioId = preview.linkedAudioId ?: return false
        val linkedAudioFilename = audioRepository.getAudio(linkedAudioId)?.filename?.trim()
        return !linkedAudioFilename.isNullOrEmpty() && preview.clientName.trim() == linkedAudioFilename
    }

    private fun isEligibleAutoTitleReply(content: String): Boolean {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return false
        if (trimmed.startsWith("已接入《") && trimmed.contains("录音上下文。")) return false
        if (trimmed.startsWith("《") &&
            trimmed.contains("》转写已完成") &&
            trimmed.contains("现在可以继续围绕这段音频讨论")
        ) {
            return false
        }
        return true
    }

    private fun sanitizeGeneratedTitle(raw: String): String {
        return raw
            .filterNot { it.isWhitespace() || it in "，。！？、；：\"'" }
            .take(6)
    }

    private fun isValidGeneratedTitle(preview: SessionPreview, title: String): Boolean {
        if (title.isBlank()) return false
        if (title == AUTO_TITLE_NO_RESULT) return false
        if (title in UNTITLED_SESSION_NAMES) return false
        if (title in AUTO_TITLE_BLOCKED_TITLES) return false
        val linkedAudioId = preview.linkedAudioId
        if (linkedAudioId != null) {
            val audioFilename = audioRepository.getAudio(linkedAudioId)?.filename?.trim()
            if (!audioFilename.isNullOrBlank() && title == audioFilename.take(title.length)) {
                return false
            }
        }
        return true
    }

    companion object {
        private const val TAG = "SimChatCoordinator"
        private val UNTITLED_SESSION_NAMES = setOf("SIM", "新对话")
        private const val UNTITLED_AUDIO_SESSION_TITLE = "新对话"
        private const val AUTO_TITLE_MAX_ATTEMPTS = 2
        private const val AUTO_TITLE_RETRY_ATTEMPTS = 1
        private const val AUTO_TITLE_REPLY_MAX_CHARS = 200
        private const val AUTO_TITLE_NO_RESULT = "NO_TITLE"
        private val AUTO_TITLE_BLOCKED_TITLES = setOf(
            "教育行业",
            "教育管理",
            "销售顾问",
            "销售助手",
            "行业分析",
            "行业建议",
            "沟通建议"
        )
    }
}

private data class AutoTitleSnapshot(
    val assistantReply: String
)
