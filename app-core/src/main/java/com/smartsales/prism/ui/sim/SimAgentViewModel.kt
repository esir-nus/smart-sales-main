package com.smartsales.prism.ui.sim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.pipeline.AgentActivity
import com.smartsales.core.pipeline.MascotInteraction
import com.smartsales.core.pipeline.MascotState
import com.smartsales.prism.domain.analyst.TaskBoardItem
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.ui.IAgentViewModel
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SIM 壳层专用聊天 ViewModel。
 * 仅维护本地会话/历史，不触碰智能版运行时。
 */
class SimAgentViewModel : ViewModel(), IAgentViewModel {

    private data class SimSessionRecord(
        val preview: SessionPreview,
        val messages: List<ChatMessage>
    )

    private val sessions = linkedMapOf<String, SimSessionRecord>()
    private val audioBindings = linkedMapOf<String, String>()
    private var currentSessionId: String? = null

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

    fun startNewSession() {
        currentSessionId = null
        _history.value = emptyList()
        _sessionTitle.value = "新对话"
        _inputText.value = ""
        _uiState.value = UiState.Idle
        _errorMessage.value = null
    }

    fun switchSession(sessionId: String) {
        val record = sessions[sessionId] ?: return
        currentSessionId = sessionId
        _history.value = record.messages
        _sessionTitle.value = record.preview.clientName
        _uiState.value = UiState.Idle
        _inputText.value = ""
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
        sessions.remove(sessionId)
        audioBindings.entries.removeAll { it.value == sessionId }
        if (currentSessionId == sessionId) {
            currentSessionId = null
            _history.value = emptyList()
            _sessionTitle.value = "SIM"
        }
        refreshGroupedSessions()
    }

    fun openAudioDiscussion(audioId: String, title: String, summary: String?): String {
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
                append("\n\n当前预览：")
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

    private fun buildReply(content: String, linkedAudioId: String?): String {
        return if (linkedAudioId != null) {
            "当前是音频讨论会话。\n\n你刚才说的是：$content\n\nWave 1 先证明壳层和会话边界，完整的音频上下文注入会在后续波次接入。"
        } else {
            "SIM Wave 1 已接管聊天壳层。\n\n你刚才说的是：$content\n\n当前阶段只验证独立 Shell、历史、抽屉路由和无污染会话能力。"
        }
    }
}
