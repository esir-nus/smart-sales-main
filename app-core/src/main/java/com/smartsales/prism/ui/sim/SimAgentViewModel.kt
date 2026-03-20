package com.smartsales.prism.ui.sim

import android.util.Log
import com.smartsales.core.telemetry.PipelineValve
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.pipeline.AgentActivity
import com.smartsales.core.pipeline.MascotInteraction
import com.smartsales.core.pipeline.MascotState
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.analyst.TaskBoardItem
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.ui.IAgentViewModel
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal const val SIM_SCHEDULER_SHELF_HANDOFF_REQUEST_SUMMARY =
    "SIM scheduler shelf Ask AI handoff requested"
internal const val SIM_SCHEDULER_SHELF_SESSION_STARTED_SUMMARY =
    "SIM scheduler shelf seeded chat session started"
private const val SIM_SCHEDULER_SHELF_LOG_TAG = "SimSchedulerShelf"

/**
 * SIM 壳层专用聊天 ViewModel。
 * 仅维护本地会话/历史，不触碰智能版运行时。
 */
class SimAgentViewModel : ViewModel(), IAgentViewModel {

    private data class SimSessionRecord(
        val preview: SessionPreview,
        val messages: List<ChatMessage>
    )

    data class ArtifactTranscriptRevealState(
        val consumed: Boolean = false,
        val isLongTranscript: Boolean = false
    )

    private val sessions = linkedMapOf<String, SimSessionRecord>()
    private val audioBindings = linkedMapOf<String, String>()
    private var currentSessionId: String? = null
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _agentActivity = MutableStateFlow<AgentActivity?>(null)
    override val agentActivity: StateFlow<AgentActivity?> = _agentActivity.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    override val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    override val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    override val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    override val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _history = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val history: StateFlow<List<ChatMessage>> = _history.asStateFlow()

    private val _taskBoardItems = MutableStateFlow<List<TaskBoardItem>>(emptyList())
    override val taskBoardItems: StateFlow<List<TaskBoardItem>> = _taskBoardItems.asStateFlow()

    private val _sessionTitle = MutableStateFlow("SIM")
    override val sessionTitle: StateFlow<String> = _sessionTitle.asStateFlow()

    private val _heroUpcoming = MutableStateFlow<List<ScheduledTask>>(emptyList())
    override val heroUpcoming: StateFlow<List<ScheduledTask>> = _heroUpcoming.asStateFlow()

    private val _heroAccomplished = MutableStateFlow<List<ScheduledTask>>(emptyList())
    override val heroAccomplished: StateFlow<List<ScheduledTask>> = _heroAccomplished.asStateFlow()

    private val _mascotState = MutableStateFlow<MascotState>(MascotState.Hidden)
    override val mascotState: StateFlow<MascotState> = _mascotState.asStateFlow()

    override val currentDisplayName: String = "SIM 用户"

    private val _heroGreeting = MutableStateFlow("欢迎进入 SIM")
    override val heroGreeting: StateFlow<String> = _heroGreeting.asStateFlow()

    private val _groupedSessions = MutableStateFlow<Map<String, List<SessionPreview>>>(emptyMap())
    val groupedSessions: StateFlow<Map<String, List<SessionPreview>>> = _groupedSessions.asStateFlow()

    private val _currentLinkedAudioId = MutableStateFlow<String?>(null)
    val currentLinkedAudioId: StateFlow<String?> = _currentLinkedAudioId.asStateFlow()

    private val _artifactTranscriptRevealState =
        MutableStateFlow<Map<String, ArtifactTranscriptRevealState>>(emptyMap())
    val artifactTranscriptRevealState: StateFlow<Map<String, ArtifactTranscriptRevealState>> =
        _artifactTranscriptRevealState.asStateFlow()

    init {
        seedWave1SampleSessions()
    }

    fun startNewSession() {
        currentSessionId = null
        _history.value = emptyList()
        _sessionTitle.value = "新对话"
        _inputText.value = ""
        _uiState.value = UiState.Idle
        _errorMessage.value = null
        _currentLinkedAudioId.value = null
    }

    fun startSeededSession(initialUserInput: String) {
        if (initialUserInput.isBlank()) return
        startNewSession()
        updateInput(initialUserInput)
        send()
    }

    fun startSchedulerShelfSession(initialUserInput: String) {
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

    fun switchSession(sessionId: String) {
        val record = sessions[sessionId] ?: return
        currentSessionId = sessionId
        _history.value = record.messages
        _sessionTitle.value = record.preview.clientName
        _uiState.value = UiState.Idle
        _inputText.value = ""
        _currentLinkedAudioId.value = record.preview.linkedAudioId
    }

    fun togglePin(sessionId: String) {
        updateSession(sessionId) { record ->
            record.copy(preview = record.preview.copy(isPinned = !record.preview.isPinned))
        }
    }

    fun renameSession(sessionId: String, clientName: String, summary: String) {
        updateSession(sessionId) { record ->
            record.copy(
                preview = record.preview.copy(
                    clientName = clientName.ifBlank { record.preview.clientName },
                    summary = summary.ifBlank { record.preview.summary }
                )
            )
        }
    }

    fun deleteSession(sessionId: String) {
        val removed = sessions.remove(sessionId)
        audioBindings.entries.removeAll { it.value == sessionId }
        removed?.messages?.let(::clearTranscriptRevealState)
        if (currentSessionId == sessionId) {
            currentSessionId = null
            _history.value = emptyList()
            _sessionTitle.value = "SIM"
            _uiState.value = UiState.Idle
            _currentLinkedAudioId.value = null
        }
        refreshGroupedSessions()
    }

    fun openAudioDiscussion(
        audioId: String,
        title: String,
        summary: String?,
        summaryLabel: String = "当前预览"
    ): String {
        val existing = audioBindings[audioId]
        if (existing != null && sessions.containsKey(existing)) {
            switchSession(existing)
            return existing
        }

        val sessionId = UUID.randomUUID().toString()
        val intro = buildString {
            append("已进入《")
            append(title)
            append("》的讨论模式。")
            if (!summary.isNullOrBlank()) {
                append("\n\n")
                append(summaryLabel)
                append("：")
                append(summary)
            } else {
                append("\n\n当前为 Wave 1 壳层接入，完整转写内容将在后续音频波次接入。")
            }
        }
        val preview = SessionPreview(
            id = sessionId,
            clientName = title,
            summary = (summary ?: "音频讨论").take(6),
            timestamp = System.currentTimeMillis(),
            linkedAudioId = audioId
        )
        val firstMessage = ChatMessage.Ai(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            uiState = UiState.Response(intro)
        )
        sessions[sessionId] = SimSessionRecord(preview = preview, messages = listOf(firstMessage))
        audioBindings[audioId] = sessionId
        switchSession(sessionId)
        refreshGroupedSessions()
        return sessionId
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
        } else if (currentSessionId == sessionId) {
            _uiState.value = UiState.Idle
        }
        return sessionId
    }

    fun updatePendingAudioState(audioId: String, status: TranscriptionStatus, progress: Float) {
        val sessionId = audioBindings[audioId] ?: return
        if (currentSessionId != sessionId) return

        val sessionTitle = sessions[sessionId]?.preview?.clientName ?: "当前音频"
        _uiState.value = UiState.Thinking(
            hint = buildPendingAudioHint(
                title = sessionTitle,
                status = status,
                progress = progress
            )
        )
    }

    fun completePendingAudio(audioId: String) {
        val sessionId = audioBindings[audioId] ?: return
        val title = sessions[sessionId]?.preview?.clientName ?: "当前音频"
        appendAiMessage(
            sessionId = sessionId,
            uiState = UiState.Response("《$title》转写已完成，结果已同步回录音抽屉，现在可以继续围绕这段音频讨论。")
        )
        if (currentSessionId == sessionId) {
            _uiState.value = UiState.Idle
        }
    }

    fun markArtifactTranscriptRevealConsumed(messageId: String, isLongTranscript: Boolean) {
        val current = _artifactTranscriptRevealState.value[messageId]
        if (current?.consumed == true && (current.isLongTranscript || !isLongTranscript)) {
            return
        }
        _artifactTranscriptRevealState.value = _artifactTranscriptRevealState.value + (
            messageId to ArtifactTranscriptRevealState(
                consumed = true,
                isLongTranscript = current?.isLongTranscript == true || isLongTranscript
            )
        )
    }

    fun appendCompletedAudioArtifacts(audioId: String, artifacts: TingwuJobArtifacts) {
        val sessionId = audioBindings[audioId] ?: return
        val record = sessions[sessionId] ?: return
        val title = record.preview.clientName
        val alreadyPresent = record.messages.any { message ->
            val state = (message as? ChatMessage.Ai)?.uiState
            state is UiState.AudioArtifacts && state.audioId == audioId
        }
        if (alreadyPresent) {
            if (currentSessionId == sessionId) {
                _uiState.value = UiState.Idle
            }
            return
        }

        appendAiMessage(
            sessionId = sessionId,
            uiState = UiState.AudioArtifacts(
                audioId = audioId,
                title = title,
                artifactsJson = json.encodeToString(artifacts)
            )
        )
        if (currentSessionId == sessionId) {
            _uiState.value = UiState.Idle
        }
    }

    fun failPendingAudio(audioId: String, message: String) {
        val sessionId = audioBindings[audioId] ?: currentSessionId ?: return
        appendAiMessage(
            sessionId = sessionId,
            uiState = UiState.Error(message)
        )
        if (currentSessionId == sessionId) {
            _uiState.value = UiState.Error(message)
        }
    }

    override fun clearToast() {
        _toastMessage.value = null
    }

    override fun updateInput(text: String) {
        _inputText.value = text
    }

    override fun clearError() {
        _errorMessage.value = null
    }

    override fun confirmAnalystPlan() {
        _uiState.value = UiState.Idle
    }

    override fun send() {
        val content = _inputText.value.trim()
        if (content.isEmpty()) return

        val sessionId = currentSessionId ?: createSession()
        val userMessage = ChatMessage.User(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = content
        )

        val current = sessions.getValue(sessionId)
        sessions[sessionId] = current.copy(
            preview = current.preview.copy(
                timestamp = System.currentTimeMillis(),
                summary = content.take(6).ifBlank { current.preview.summary }
            ),
            messages = current.messages + userMessage
        )

        _history.value = sessions.getValue(sessionId).messages
        _inputText.value = ""
        _isSending.value = true
        _uiState.value = UiState.Thinking("SIM 正在整理当前对话")
        refreshGroupedSessions()

        viewModelScope.launch {
            delay(350)
            val response = ChatMessage.Ai(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                uiState = UiState.Response(buildReply(content, sessions.getValue(sessionId).preview.linkedAudioId))
            )
            val updated = sessions.getValue(sessionId)
            sessions[sessionId] = updated.copy(
                preview = updated.preview.copy(timestamp = System.currentTimeMillis()),
                messages = updated.messages + response
            )
            _history.value = sessions.getValue(sessionId).messages
            _isSending.value = false
            _uiState.value = UiState.Idle
            refreshGroupedSessions()
        }
    }

    override fun amendAnalystPlan() {
        _uiState.value = UiState.Idle
    }

    override fun interactWithMascot(interaction: MascotInteraction) {
        // SIM 不启用 mascot。
    }

    override fun updateSessionTitle(newTitle: String) {
        val sessionId = currentSessionId ?: return
        updateSession(sessionId) { record ->
            record.copy(preview = record.preview.copy(clientName = newTitle.ifBlank { record.preview.clientName }))
        }
    }

    override fun selectTaskBoardItem(itemId: String) {
        _toastMessage.value = "SIM Wave 1 不启用任务板入口"
    }

    override fun debugRunScenario(scenario: String) {
        _toastMessage.value = "SIM 屏蔽调试 HUD"
    }

    private fun createSession(): String {
        val sessionId = UUID.randomUUID().toString()
        val preview = SessionPreview(
            id = sessionId,
            clientName = "新对话",
            summary = "新会话",
            timestamp = System.currentTimeMillis()
        )
        sessions[sessionId] = SimSessionRecord(preview = preview, messages = emptyList())
        currentSessionId = sessionId
        _sessionTitle.value = preview.clientName
        _currentLinkedAudioId.value = preview.linkedAudioId
        refreshGroupedSessions()
        return sessionId
    }

    private fun updateSession(sessionId: String, transform: (SimSessionRecord) -> SimSessionRecord) {
        val current = sessions[sessionId] ?: return
        val updated = transform(current)
        sessions[sessionId] = updated
        if (currentSessionId == sessionId) {
            _history.value = updated.messages
            _sessionTitle.value = updated.preview.clientName
            _currentLinkedAudioId.value = updated.preview.linkedAudioId
        }
        refreshGroupedSessions()
    }

    private fun refreshGroupedSessions() {
        val sorted = sessions.values
            .map { it.preview }
            .sortedByDescending { it.timestamp }

        val pinned = sorted.filter { it.isPinned }
        val regular = sorted.filterNot { it.isPinned }

        val grouped = linkedMapOf<String, List<SessionPreview>>()
        if (pinned.isNotEmpty()) {
            grouped["置顶"] = pinned
        }
        if (regular.isNotEmpty()) {
            grouped["今天"] = regular
        }
        _groupedSessions.value = grouped
    }

    private fun seedWave1SampleSessions() {
        if (sessions.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val pinnedSessionId = UUID.randomUUID().toString()
        val audioSessionId = UUID.randomUUID().toString()

        sessions[pinnedSessionId] = SimSessionRecord(
            preview = SessionPreview(
                id = pinnedSessionId,
                clientName = "SIM 演示会话",
                summary = "可置顶改名",
                timestamp = now - 60_000,
                isPinned = true
            ),
            messages = listOf(
                ChatMessage.Ai(
                    id = UUID.randomUUID().toString(),
                    timestamp = now - 60_000,
                    uiState = UiState.Response("这是 Wave 1 的本地演示会话，用来验证历史抽屉的切换、置顶、改名和删除能力。")
                )
            )
        )

        sessions[audioSessionId] = SimSessionRecord(
            preview = SessionPreview(
                id = audioSessionId,
                clientName = "录音讨论样例",
                summary = "音频讨论",
                timestamp = now - 30_000,
                linkedAudioId = "sim_audio_seed"
            ),
            messages = listOf(
                ChatMessage.Ai(
                    id = UUID.randomUUID().toString(),
                    timestamp = now - 30_000,
                    uiState = UiState.Response("这是 SIM 的音频讨论样例。Wave 1 先验证壳层路由和会话边界。")
                )
            )
        )

        refreshGroupedSessions()
    }

    private fun buildReply(content: String, linkedAudioId: String?): String {
        return if (linkedAudioId != null) {
            "当前是音频讨论会话。\n\n你刚才说的是：$content\n\nWave 1 先证明壳层和会话边界，完整的音频上下文注入会在后续波次接入。"
        } else {
            "SIM Wave 1 已接管聊天壳层。\n\n你刚才说的是：$content\n\n当前阶段只验证独立 Shell、历史、抽屉路由和无污染会话能力。"
        }
    }

    private fun appendAiMessage(sessionId: String, uiState: UiState) {
        val record = sessions[sessionId] ?: return
        val timestamp = System.currentTimeMillis()
        val newMessage = ChatMessage.Ai(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            uiState = uiState
        )
        sessions[sessionId] = record.copy(
            preview = record.preview.copy(timestamp = timestamp),
            messages = record.messages + newMessage
        )
        if (currentSessionId == sessionId) {
            _history.value = sessions.getValue(sessionId).messages
        }
        refreshGroupedSessions()
    }

    private fun clearTranscriptRevealState(messages: List<ChatMessage>) {
        val messageIds = messages.mapNotNull { message ->
            (message as? ChatMessage.Ai)
                ?.takeIf { it.uiState is UiState.AudioArtifacts }
                ?.id
        }
        if (messageIds.isEmpty()) return
        _artifactTranscriptRevealState.value =
            _artifactTranscriptRevealState.value - messageIds.toSet()
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
}
