package com.smartsales.feature.chat

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatResponse
import com.smartsales.data.aicore.AiChatService
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.data.aicore.ExportManager
import com.smartsales.data.aicore.ExportResult
import com.smartsales.data.aicore.TingwuCoordinator
import com.smartsales.data.aicore.TingwuJobState
import com.smartsales.data.aicore.TingwuRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

// 文件路径: feature/chat/src/main/java/com/smartsales/feature/chat/ChatController.kt
// 文件作用: 管理聊天状态机、技能切换、导出与转写流程
// 最近修改: 2025-11-14
interface ChatController {
    val state: StateFlow<ChatState>
    fun updateDraft(text: String)
    fun toggleSkill(skill: ChatSkill)
    fun clearClipboardMessage()
    fun clearError()
    fun copyMarkdown()
    suspend fun send(prompt: String)
    suspend fun requestExport(format: ExportFormat)
    suspend fun startTranscriptJob(request: TingwuRequest)
    suspend fun importTranscript(markdown: String, sourceName: String? = null)
}

@Singleton
class DefaultChatController @Inject constructor(
    private val chatService: AiChatService,
    private val exportManager: ExportManager,
    private val tingwuCoordinator: TingwuCoordinator,
    private val sessionRepository: AiSessionRepository,
    private val dispatchers: DispatcherProvider,
    private val shareHandler: ChatShareHandler
) : ChatController {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val lock = Mutex()
    private val _state = MutableStateFlow(ChatState())
    override val state: StateFlow<ChatState> = _state.asStateFlow()

    private var transcriptWatcher: Job? = null
    private val sessionId = "session-${System.currentTimeMillis()}"

    override fun updateDraft(text: String) {
        _state.update { it.copy(draft = text) }
    }

    override fun toggleSkill(skill: ChatSkill) {
        _state.update { current ->
            val mutable = current.selectedSkills.toMutableSet()
            if (mutable.contains(skill)) {
                if (mutable.size > 1) {
                    mutable.remove(skill)
                }
            } else {
                mutable.add(skill)
            }
            current.copy(selectedSkills = mutable.ifEmpty { setOf(ChatSkill.Analyze) })
        }
    }

    override fun clearClipboardMessage() {
        _state.update { it.copy(clipboardMessage = null) }
    }

    override fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    override fun copyMarkdown() {
        val markdown = _state.value.structuredMarkdown
        if (markdown.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "暂无Markdown可复制") }
        } else {
            scope.launch(dispatchers.default) {
                when (val result = shareHandler.copyMarkdown(markdown)) {
                    is Result.Success -> _state.update {
                        it.copy(clipboardMessage = "Markdown已复制，可粘贴分享。", errorMessage = null)
                    }
                    is Result.Error -> _state.update {
                        it.copy(errorMessage = result.throwable.asUserFacingMessage("复制失败"))
                    }
                }
            }
        }
    }

    override suspend fun send(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(errorMessage = "请输入聊天内容") }
            return
        }
        lock.withLock {
            emitUserMessage(trimmed)
            val currentState = _state.value
            val transcriptMarkdown = (currentState.transcriptState as? TranscriptState.Ready)?.markdown
            val request = AiChatRequest(
                prompt = trimmed,
                skillTags = currentState.selectedSkills.map { it.tag }.toSet(),
                transcriptMarkdown = transcriptMarkdown
            )
            when (val result = chatService.sendMessage(request)) {
                is Result.Success -> handleAiSuccess(result.data)
                is Result.Error -> _state.update {
                    it.copy(
                        isSending = false,
                        errorMessage = result.throwable.asUserFacingMessage("AI 服务失败")
                    )
                }
            }
        }
    }

    private fun emitUserMessage(prompt: String) {
        val message = ChatMessage.fromUser(prompt)
        _state.update {
            it.copy(
                draft = "",
                isSending = true,
                errorMessage = null,
                messages = it.messages + message
            )
        }
    }

    private suspend fun handleAiSuccess(response: AiChatResponse) {
        val message = ChatMessage.fromAssistant(response.displayText)
        val clipboardResult = response.displayText
            .takeIf { it.isNotBlank() }
            ?.let { shareHandler.copyAssistantReply(it) }
        _state.update { current ->
            current.copy(
                isSending = false,
                messages = current.messages + message,
                structuredMarkdown = response.structuredMarkdown,
                exportState = ChatExportState.Idle,
                clipboardMessage = when (clipboardResult) {
                    is Result.Success -> "助手回复已复制，可直接粘贴。"
                    else -> current.clipboardMessage
                },
                errorMessage = if (clipboardResult is Result.Error) {
                    clipboardResult.throwable.asUserFacingMessage("复制失败，请手动粘贴")
                } else current.errorMessage
            )
        }
        persistSession(message, response.structuredMarkdown)
    }

    private suspend fun persistSession(latestMessage: ChatMessage, markdown: String?) {
        val title = buildTitle(latestMessage.timestampMillis)
        val summary = AiSessionSummary(
            id = sessionId,
            title = title,
            lastMessagePreview = markdown ?: latestMessage.content.take(80),
            updatedAtMillis = latestMessage.timestampMillis
        )
        sessionRepository.upsert(summary)
    }

    private fun buildTitle(timestampMillis: Long): String {
        val localDate = Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return "${localDate} 客户复盘 - 会话"
    }

    override suspend fun requestExport(format: ExportFormat) {
        val markdown = _state.value.structuredMarkdown
        if (markdown.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "当前没有可导出的Markdown") }
            return
        }
        _state.update { it.copy(exportState = ChatExportState.InProgress(format)) }
        when (val result = exportManager.exportMarkdown(markdown, format)) {
            is Result.Success -> {
                _state.update {
                    it.copy(exportState = ChatExportState.Completed(result.data))
                }
                scope.launch(dispatchers.default) {
                    handleShareResult(result.data)
                }
            }

            is Result.Error -> _state.update {
                val message = result.throwable.asUserFacingMessage("导出失败")
                it.copy(
                    exportState = ChatExportState.Failed(message),
                    errorMessage = message
                )
            }
        }
    }

    private suspend fun handleShareResult(result: ExportResult) {
        when (val share = shareHandler.shareExport(result)) {
            is Result.Success -> Unit
            is Result.Error -> _state.update {
                it.copy(errorMessage = share.throwable.asUserFacingMessage("分享失败"))
            }
        }
    }

    override suspend fun startTranscriptJob(request: TingwuRequest) {
        _state.update {
            it.copy(
                transcriptState = TranscriptState.InProgress(
                    jobId = "-",
                    percent = 5,
                    sourceName = request.audioAssetName
                )
            )
        }
        when (val submitResult = tingwuCoordinator.submit(request)) {
            is Result.Success -> observeTranscript(submitResult.data, request.audioAssetName)
            is Result.Error -> _state.update {
                val message = submitResult.throwable.asUserFacingMessage("无法启动转写")
                it.copy(
                    transcriptState = TranscriptState.Error(
                        jobId = null,
                        reason = message
                    ),
                    errorMessage = message
                )
            }
        }
    }

    override suspend fun importTranscript(markdown: String, sourceName: String?) {
        val normalized = formatTranscript(markdown)
        if (normalized.isEmpty()) return
        val resolvedSource = sourceName ?: "Tingwu"
        val resolvedJobId = sourceName ?: "-"
        lock.withLock {
            val transcriptMessage = ChatMessage.fromTranscript(normalized)
            _state.update {
                it.copy(
                    messages = it.messages + transcriptMessage,
                    transcriptState = TranscriptState.Ready(
                        jobId = resolvedJobId,
                        markdown = normalized,
                        sourceName = resolvedSource
                    ),
                    errorMessage = null
                )
            }
            persistSession(transcriptMessage, normalized)
        }
    }

    private fun observeTranscript(jobId: String, sourceName: String) {
        _state.update {
            it.copy(transcriptState = TranscriptState.InProgress(jobId, percent = 10, sourceName = sourceName))
        }
        transcriptWatcher?.cancel()
        transcriptWatcher = scope.launch {
            tingwuCoordinator.observeJob(jobId).collect { state ->
                when (state) {
                    is TingwuJobState.InProgress -> _state.update {
                        it.copy(
                            transcriptState = TranscriptState.InProgress(
                                jobId = state.jobId,
                                percent = state.progressPercent,
                                sourceName = sourceName
                            )
                        )
                    }

                    is TingwuJobState.Completed -> {
                        val formatted = formatTranscript(state.transcriptMarkdown)
                        val transcriptMessage = ChatMessage.fromTranscript(formatted)
                        _state.update {
                            it.copy(
                                messages = it.messages + transcriptMessage,
                                transcriptState = TranscriptState.Ready(
                                    jobId = state.jobId,
                                    markdown = formatted,
                                    sourceName = sourceName
                                )
                            )
                        }
                    }

                    is TingwuJobState.Failed -> _state.update {
                        it.copy(
                            transcriptState = TranscriptState.Error(
                                jobId = state.jobId,
                                reason = state.reason
                            ),
                            errorMessage = state.reason
                        )
                    }

                    TingwuJobState.Idle -> Unit
                }
            }
        }
    }

    private fun formatTranscript(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return trimmed
        val normalized = trimmed.replace("\r\n", "\n")
        if (isPreFormatted(normalized)) return normalized
        val sentences = mutableListOf<String>()
        val current = StringBuilder()
        normalized.forEach { ch ->
            current.append(ch)
            if (ch in SENTENCE_DELIMITERS) {
                val sentence = current.toString().trim()
                if (sentence.isNotEmpty()) sentences.add(sentence)
                current.clear()
            }
        }
        val remainder = current.toString().trim()
        if (remainder.isNotEmpty()) sentences.add(remainder)
        if (sentences.isEmpty()) return normalized
        val builder = StringBuilder()
        builder.append("### 转写内容\n\n")
        sentences.forEach { sentence ->
            builder.append("- ").append(sentence).append('\n')
        }
        return builder.toString().trim()
    }

    private fun isPreFormatted(markdown: String): Boolean =
        markdown.contains("\n- ") ||
            markdown.trimStart().startsWith("#") ||
            markdown.contains("\n\n")

    private fun Throwable.asUserFacingMessage(default: String): String =
        (this as? AiCoreException)?.userFacingMessage ?: message ?: default

    private companion object {
        private val SENTENCE_DELIMITERS = setOf('。', '？', '！', '.', '!', '?')
    }
}
