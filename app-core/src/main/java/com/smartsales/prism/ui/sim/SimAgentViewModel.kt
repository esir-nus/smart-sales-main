package com.smartsales.prism.ui.sim

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.AgentActivity
import com.smartsales.core.pipeline.MascotInteraction
import com.smartsales.core.pipeline.MascotState
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.data.audio.SimAudioRepository
import com.smartsales.prism.data.session.SimPersistedSession
import com.smartsales.prism.data.session.SimSessionRepository
import com.smartsales.prism.domain.analyst.TaskBoardItem
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.SchedulerFollowUpContext
import com.smartsales.prism.domain.model.SchedulerFollowUpTaskSummary
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.model.SessionKind
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.domain.time.TimeProvider
import com.smartsales.prism.ui.IAgentViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal const val SIM_SCHEDULER_SHELF_HANDOFF_REQUEST_SUMMARY =
    "SIM scheduler shelf Ask AI handoff requested"
internal const val SIM_SCHEDULER_SHELF_SESSION_STARTED_SUMMARY =
    "SIM scheduler shelf seeded chat session started"
internal const val SIM_BADGE_SCHEDULER_FOLLOW_UP_SESSION_CREATED_SUMMARY =
    "SIM badge scheduler follow-up session created"
internal const val SIM_BADGE_SCHEDULER_FOLLOW_UP_ACTION_COMPLETED_SUMMARY =
    "SIM badge scheduler follow-up action completed"
internal const val SIM_BADGE_SCHEDULER_FOLLOW_UP_ACTION_BLOCKED_SUMMARY =
    "SIM badge scheduler follow-up action blocked"
private const val SIM_AUDIO_CHAT_LOG_TAG = "SimAudioChat"
private const val SIM_SCHEDULER_SHELF_LOG_TAG = "SimSchedulerShelf"
private const val SIM_BADGE_FOLLOW_UP_LOG_TAG = "SimBadgeFollowUpChat"

enum class SimSchedulerFollowUpQuickAction {
    EXPLAIN,
    STATUS,
    PREFILL_RESCHEDULE,
    MARK_DONE,
    DELETE
}

/**
 * SIM 壳层专用聊天 ViewModel。
 * 仅维护 SIM 本地会话/历史，不触碰智能版运行时。
 */
@HiltViewModel
class SimAgentViewModel @Inject constructor(
    private val sessionRepository: SimSessionRepository,
    private val audioRepository: SimAudioRepository,
    private val taskRepository: ScheduledTaskRepository,
    private val scheduleBoard: ScheduleBoard,
    private val alarmScheduler: AlarmScheduler,
    private val uniAExtractionService: RealUniAExtractionService,
    private val executor: Executor,
    private val userProfileRepository: UserProfileRepository,
    private val timeProvider: TimeProvider
) : ViewModel(), IAgentViewModel {

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
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

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

    private val _mascotState = MutableStateFlow(MascotState.Hidden)
    override val mascotState: StateFlow<MascotState> = _mascotState.asStateFlow()

    override val currentDisplayName: String
        get() = userProfileRepository.profile.value.displayName

    private val _heroGreeting = MutableStateFlow("欢迎回来，${currentDisplayName}")
    override val heroGreeting: StateFlow<String> = _heroGreeting.asStateFlow()

    private val _groupedSessions = MutableStateFlow<Map<String, List<SessionPreview>>>(emptyMap())
    val groupedSessions: StateFlow<Map<String, List<SessionPreview>>> = _groupedSessions.asStateFlow()

    private val _currentLinkedAudioId = MutableStateFlow<String?>(null)
    val currentLinkedAudioId: StateFlow<String?> = _currentLinkedAudioId.asStateFlow()

    private val _currentSchedulerFollowUpContext = MutableStateFlow<SchedulerFollowUpContext?>(null)
    val currentSchedulerFollowUpContext: StateFlow<SchedulerFollowUpContext?> =
        _currentSchedulerFollowUpContext.asStateFlow()

    private val _selectedSchedulerFollowUpTaskId = MutableStateFlow<String?>(null)
    val selectedSchedulerFollowUpTaskId: StateFlow<String?> =
        _selectedSchedulerFollowUpTaskId.asStateFlow()

    private val _artifactTranscriptRevealState =
        MutableStateFlow<Map<String, ArtifactTranscriptRevealState>>(emptyMap())
    val artifactTranscriptRevealState: StateFlow<Map<String, ArtifactTranscriptRevealState>> =
        _artifactTranscriptRevealState.asStateFlow()

    init {
        loadPersistedSessions()
        reconcileAudioBindings()
    }

    fun startNewSession() {
        _currentSessionId.value = null
        _history.value = emptyList()
        _sessionTitle.value = "新对话"
        _inputText.value = ""
        _uiState.value = UiState.Idle
        _errorMessage.value = null
        _currentLinkedAudioId.value = null
        _currentSchedulerFollowUpContext.value = null
        _selectedSchedulerFollowUpTaskId.value = null
    }

    fun startSeededSession(initialUserInput: String) {
        if (initialUserInput.isBlank()) return
        startFreshSeededSession(initialUserInput)
    }

    fun createBadgeSchedulerFollowUpSession(
        threadId: String,
        transcript: String,
        tasks: List<SchedulerFollowUpTaskSummary>,
        batchId: String? = null
    ): String? {
        if (transcript.isBlank() || tasks.isEmpty()) return null

        val now = System.currentTimeMillis()
        val sessionId = UUID.randomUUID().toString()
        val context = SchedulerFollowUpContext(
            sourceBadgeThreadId = threadId,
            boundTaskIds = tasks.map { it.taskId },
            batchId = batchId,
            taskSummaries = tasks,
            createdAt = now,
            updatedAt = now
        )
        val preview = SessionPreview(
            id = sessionId,
            clientName = if (tasks.size == 1) {
                tasks.first().title
            } else {
                "工牌日程跟进"
            },
            summary = if (tasks.size == 1) "跟进" else "批量跟进",
            timestamp = now,
            sessionKind = SessionKind.SCHEDULER_FOLLOW_UP,
            schedulerFollowUpContext = context
        )
        val firstMessage = ChatMessage.Ai(
            id = UUID.randomUUID().toString(),
            timestamp = now,
            uiState = UiState.Response(
                buildBadgeSchedulerFollowUpSummary(
                    transcript = transcript,
                    taskSummaries = tasks
                )
            )
        )
        sessions[sessionId] = SimSessionRecord(
            preview = preview,
            messages = listOf(firstMessage)
        )
        persistSession(sessionId)
        refreshGroupedSessions()
        emitSchedulerFollowUpTelemetry(
            summary = SIM_BADGE_SCHEDULER_FOLLOW_UP_SESSION_CREATED_SUMMARY,
            detail = "threadId=$threadId sessionId=$sessionId taskCount=${tasks.size}"
        )
        return sessionId
    }

    suspend fun createDebugBadgeSchedulerFollowUpSession(
        threadId: String,
        transcript: String,
        tasks: List<SchedulerFollowUpTaskSummary>,
        batchId: String? = null
    ): String? {
        tasks.forEach { summary ->
            taskRepository.upsertTask(summary.toScheduledTask())
        }
        return createBadgeSchedulerFollowUpSession(
            threadId = threadId,
            transcript = transcript,
            tasks = tasks,
            batchId = batchId
        )
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
        _currentSessionId.value = sessionId
        _history.value = record.messages
        _sessionTitle.value = record.preview.clientName
        _uiState.value = UiState.Idle
        _inputText.value = ""
        _currentLinkedAudioId.value = record.preview.linkedAudioId
        _currentSchedulerFollowUpContext.value = record.preview.schedulerFollowUpContext
        _selectedSchedulerFollowUpTaskId.value =
            defaultSelectedFollowUpTaskId(record.preview.schedulerFollowUpContext)
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
        val removed = sessions.remove(sessionId) ?: return
        removed.preview.linkedAudioId?.let { audioId ->
            audioBindings.remove(audioId)
            audioRepository.clearBoundSession(audioId)
        }
        clearTranscriptRevealState(removed.messages)
        sessionRepository.deleteSession(sessionId)
        if (_currentSessionId.value == sessionId) {
            _currentSessionId.value = null
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
        val currentSessionId = _currentSessionId.value
        val currentRecord = currentSessionId?.let { sessions[it] }
        if (currentSessionId != null && currentRecord != null) {
            if (currentRecord.preview.sessionKind != SessionKind.SCHEDULER_FOLLOW_UP) {
                if (currentRecord.preview.linkedAudioId == audioId) {
                    switchSession(currentSessionId)
                    return currentSessionId
                }
                attachAudioToSession(
                    sessionId = currentSessionId,
                    audioId = audioId,
                    title = title,
                    summary = summary,
                    summaryLabel = summaryLabel,
                    retainExistingTitle = shouldRetainSessionTitleOnAudioAttach(currentRecord)
                )
                switchSession(currentSessionId)
                return currentSessionId
            }
        }

        val existing = audioBindings[audioId]
        if (existing != null && sessions.containsKey(existing)) {
            switchSession(existing)
            return existing
        }

        val sessionId = UUID.randomUUID().toString()
        val intro = buildAudioDiscussionIntro(title, summary, summaryLabel)
        val preview = SessionPreview(
            id = sessionId,
            clientName = title,
            summary = (summary ?: "音频讨论").take(6),
            timestamp = System.currentTimeMillis(),
            linkedAudioId = audioId,
            sessionKind = SessionKind.AUDIO_GROUNDED
        )
        val firstMessage = ChatMessage.Ai(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            uiState = UiState.Response(intro)
        )
        val record = SimSessionRecord(preview = preview, messages = listOf(firstMessage))
        sessions[sessionId] = record
        audioBindings[audioId] = sessionId
        audioRepository.bindSession(audioId, sessionId)
        persistSession(sessionId)
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
        } else if (_currentSessionId.value == sessionId) {
            _uiState.value = UiState.Idle
        }
        return sessionId
    }

    fun updatePendingAudioState(audioId: String, status: TranscriptionStatus, progress: Float) {
        val sessionId = audioBindings[audioId] ?: return
        if (_currentSessionId.value != sessionId) return

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
        if (_currentSessionId.value == sessionId) {
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
            if (_currentSessionId.value == sessionId) {
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
        if (_currentSessionId.value == sessionId) {
            _uiState.value = UiState.Idle
        }
    }

    fun failPendingAudio(audioId: String, message: String) {
        val sessionId = audioBindings[audioId] ?: _currentSessionId.value ?: return
        appendAiMessage(
            sessionId = sessionId,
            uiState = UiState.Error(message)
        )
        if (_currentSessionId.value == sessionId) {
            _uiState.value = UiState.Error(message)
        }
    }

    fun selectSchedulerFollowUpTask(taskId: String) {
        val context = _currentSchedulerFollowUpContext.value ?: return
        if (context.boundTaskIds.contains(taskId)) {
            _selectedSchedulerFollowUpTaskId.value = taskId
        }
    }

    fun performSchedulerFollowUpQuickAction(action: SimSchedulerFollowUpQuickAction) {
        viewModelScope.launch {
            performSchedulerFollowUpQuickActionInternal(action)
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

        if (_currentSchedulerFollowUpContext.value != null) {
            _inputText.value = ""
            viewModelScope.launch {
                handleSchedulerFollowUpInput(content)
            }
            return
        }

        val sessionId = _currentSessionId.value ?: createSession()
        appendUserMessageForSend(sessionId, content)
        _inputText.value = ""
        _isSending.value = true

        when (sessions.getValue(sessionId).preview.sessionKind) {
            SessionKind.AUDIO_GROUNDED -> {
                _uiState.value = UiState.Thinking("SIM 正在整理这段录音的上下文")
                viewModelScope.launch {
                    handleAudioGroundedSend(sessionId, content)
                }
            }

            SessionKind.GENERAL -> {
                _uiState.value = UiState.Thinking("SIM 正在确认当前支持的讨论范围")
                viewModelScope.launch {
                    handleGeneralSend(sessionId, content)
                }
            }

            SessionKind.SCHEDULER_FOLLOW_UP -> {
                _uiState.value = UiState.Thinking("SIM 正在处理当前日程跟进")
                viewModelScope.launch {
                    handleSchedulerFollowUpInput(content)
                    _isSending.value = false
                    if (_uiState.value !is UiState.Error) {
                        _uiState.value = UiState.Idle
                    }
                }
            }
        }
    }

    override fun amendAnalystPlan() {
        _uiState.value = UiState.Idle
    }

    override fun interactWithMascot(interaction: MascotInteraction) {
        // SIM 不启用 mascot。
    }

    override fun updateSessionTitle(newTitle: String) {
        val sessionId = _currentSessionId.value ?: return
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
            timestamp = System.currentTimeMillis(),
            sessionKind = SessionKind.GENERAL
        )
        sessions[sessionId] = SimSessionRecord(preview = preview, messages = emptyList())
        persistSession(sessionId)
        _currentSessionId.value = sessionId
        _sessionTitle.value = preview.clientName
        _currentLinkedAudioId.value = preview.linkedAudioId
        _currentSchedulerFollowUpContext.value = null
        _selectedSchedulerFollowUpTaskId.value = null
        refreshGroupedSessions()
        return sessionId
    }

    private fun updateSession(sessionId: String, transform: (SimSessionRecord) -> SimSessionRecord) {
        val current = sessions[sessionId] ?: return
        val updated = transform(current)
        sessions[sessionId] = updated
        persistSession(sessionId)
        if (_currentSessionId.value == sessionId) {
            _history.value = updated.messages
            _sessionTitle.value = updated.preview.clientName
            _currentLinkedAudioId.value = updated.preview.linkedAudioId
            _currentSchedulerFollowUpContext.value = updated.preview.schedulerFollowUpContext
            if (updated.preview.schedulerFollowUpContext == null) {
                _selectedSchedulerFollowUpTaskId.value = null
            }
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

    private fun loadPersistedSessions() {
        val loadedSessions = normalizeDuplicateAudioLinks(sessionRepository.loadSessions())
        sessions.clear()
        audioBindings.clear()
        loadedSessions.forEach { session ->
            sessions[session.preview.id] = SimSessionRecord(
                preview = session.preview,
                messages = session.messages
            )
            session.preview.linkedAudioId?.let { audioId ->
                audioBindings[audioId] = session.preview.id
            }
        }
        refreshGroupedSessions()
    }

    private fun normalizeDuplicateAudioLinks(
        loadedSessions: List<SimPersistedSession>
    ): List<SimPersistedSession> {
        val newestByAudioId = loadedSessions
            .filter { !it.preview.linkedAudioId.isNullOrBlank() }
            .groupBy { it.preview.linkedAudioId!! }
            .mapValues { (_, sessionsForAudio) ->
                sessionsForAudio.maxByOrNull { it.preview.timestamp }?.preview?.id
            }

        var changed = false
        val normalized = loadedSessions.map { session ->
            val linkedAudioId = session.preview.linkedAudioId
            val shouldKeepLink = linkedAudioId != null && newestByAudioId[linkedAudioId] == session.preview.id
            if (linkedAudioId != null && !shouldKeepLink) {
                changed = true
                session.copy(preview = session.preview.copy(linkedAudioId = null))
            } else {
                session
            }
        }

        if (changed) {
            normalized.forEach { session ->
                sessionRepository.saveSession(session.preview, session.messages)
            }
        }
        return normalized
    }

    private fun reconcileAudioBindings() {
        val sessionIds = sessions.keys.toSet()
        audioRepository.getAudioFilesSnapshot().forEach { audio ->
            val boundSessionId = audio.boundSessionId ?: return@forEach
            if (boundSessionId !in sessionIds) {
                audioRepository.clearBoundSession(audio.id)
            }
        }

        sessions.values
            .sortedByDescending { it.preview.timestamp }
            .forEach { record ->
                val audioId = record.preview.linkedAudioId ?: return@forEach
                val audio = audioRepository.getAudio(audioId)
                if (audio == null) {
                    updateSession(record.preview.id) { current ->
                        current.copy(preview = current.preview.copy(linkedAudioId = null))
                    }
                    audioBindings.remove(audioId)
                    return@forEach
                }

                if (audio.boundSessionId != record.preview.id) {
                    audioRepository.bindSession(audioId, record.preview.id)
                }
                audioBindings[audioId] = record.preview.id
            }
    }

    private suspend fun handleGeneralSend(sessionId: String, content: String) {
        val record = sessions[sessionId]
        if (record == null) {
            _isSending.value = false
            _uiState.value = UiState.Idle
            return
        }

        delay(180)
        val prompt = buildGeneralChatPrompt(record, content)
        when (val result = executor.execute(ModelRegistry.COACH, prompt)) {
            is ExecutorResult.Success -> {
                appendAiMessage(
                    sessionId,
                    UiState.Response(
                        result.content.ifBlank {
                            "我在这里，刚刚没有组织出合适的回复。你可以换个说法继续聊。"
                        }
                    )
                )
                _isSending.value = false
                _uiState.value = UiState.Idle
            }

            is ExecutorResult.Failure -> {
                appendAiMessage(
                    sessionId,
                    UiState.Error(
                        "当前无法继续这段聊天，请稍后重试。错误：${result.error}",
                        retryable = result.retryable
                    )
                )
                _isSending.value = false
                _uiState.value = UiState.Error("聊天暂时不可用")
            }
        }
    }

    private suspend fun handleAudioGroundedSend(sessionId: String, latestUserInput: String) {
        val record = sessions[sessionId]
        if (record == null) {
            _isSending.value = false
            _uiState.value = UiState.Idle
            return
        }

        val artifacts = loadGroundingArtifacts(record)
        if (artifacts == null) {
            appendAiMessage(
                sessionId,
                UiState.Error(
                    "当前讨论尚未加载这段录音的转写结果。请先从录音抽屉打开已转写录音，或等待转写完成后再继续提问。",
                    retryable = false
                )
            )
            _isSending.value = false
            _uiState.value = UiState.Error("缺少可用的录音上下文")
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
                appendAiMessage(
                    sessionId,
                    UiState.Response(
                        result.content.ifBlank {
                            "我暂时没能从这段录音里整理出可回答的内容，请换个问法试试。"
                        }
                    )
                )
                _isSending.value = false
                _uiState.value = UiState.Idle
            }

            is ExecutorResult.Failure -> {
                appendAiMessage(
                    sessionId,
                    UiState.Error(
                        "当前无法继续这段录音的讨论，请稍后重试。错误：${result.error}",
                        retryable = result.retryable
                    )
                )
                _isSending.value = false
                _uiState.value = UiState.Error("录音讨论暂时不可用")
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
            if (!summary.isNullOrBlank()) {
                append("\n\n")
                append(summaryLabel)
                append("：")
                append(summary)
            } else {
                append("\n\n当前为 SIM 壳层接入，完整转写内容将在后续音频波次接入。")
            }
        }
    }

    private fun attachAudioToSession(
        sessionId: String,
        audioId: String,
        title: String,
        summary: String?,
        summaryLabel: String,
        retainExistingTitle: Boolean
    ) {
        val currentRecord = sessions[sessionId] ?: return
        currentRecord.preview.linkedAudioId
            ?.takeIf { it != audioId }
            ?.let { previousAudioId ->
                audioBindings.remove(previousAudioId)
                audioRepository.clearBoundSession(previousAudioId)
            }

        val previousBoundSessionId = audioBindings[audioId]
        if (previousBoundSessionId != null && previousBoundSessionId != sessionId) {
            updateSession(previousBoundSessionId) { record ->
                record.copy(
                    preview = record.preview.copy(
                        linkedAudioId = null,
                        sessionKind = SessionKind.GENERAL
                    )
                )
            }
        }

        audioBindings[audioId] = sessionId
        audioRepository.bindSession(audioId, sessionId)
        updateSession(sessionId) { record ->
            val currentTitle = if (retainExistingTitle) {
                record.preview.clientName
            } else {
                title
            }
            val currentSummary = if (retainExistingTitle) {
                record.preview.summary
            } else {
                (summary ?: "音频讨论").take(6)
            }
            record.copy(
                preview = record.preview.copy(
                    clientName = currentTitle,
                    summary = currentSummary,
                    linkedAudioId = audioId,
                    sessionKind = SessionKind.AUDIO_GROUNDED,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        appendAiMessage(
            sessionId = sessionId,
            uiState = UiState.Response(buildAudioDiscussionIntro(title, summary, summaryLabel))
        )
    }

    private fun shouldRetainSessionTitleOnAudioAttach(record: SimSessionRecord): Boolean {
        if (record.messages.isEmpty()) return false
        return record.preview.clientName !in setOf("SIM", "新对话")
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

    private suspend fun handleSchedulerFollowUpInput(content: String) {
        val sessionId = _currentSessionId.value ?: return
        appendUserMessage(sessionId, content)
        _isSending.value = true
        _uiState.value = UiState.Thinking("SIM 正在处理当前日程跟进")

        val normalized = content.lowercase()
        when {
            normalized.contains("删除") ||
                normalized.contains("取消") ||
                normalized.contains("删掉") ||
                normalized.contains("delete") ||
                normalized.contains("remove") -> {
                performSchedulerFollowUpQuickActionInternal(SimSchedulerFollowUpQuickAction.DELETE)
            }
            normalized.contains("完成") ||
                normalized.contains("done") ||
                normalized.contains("mark done") -> {
                performSchedulerFollowUpQuickActionInternal(SimSchedulerFollowUpQuickAction.MARK_DONE)
            }
            normalized.contains("说明") ||
                normalized.contains("explain") ||
                normalized.contains("是什么") -> {
                appendSchedulerFollowUpResponse(buildSchedulerFollowUpExplanation())
            }
            normalized.contains("状态") ||
                normalized.contains("什么时候") ||
                normalized.contains("提醒") ||
                normalized.contains("status") -> {
                appendSchedulerFollowUpResponse(buildSchedulerFollowUpStatus())
            }
            else -> {
                handleSchedulerFollowUpReschedule(content)
            }
        }

        _isSending.value = false
        if (_uiState.value !is UiState.Error) {
            _uiState.value = UiState.Idle
        }
    }

    private suspend fun performSchedulerFollowUpQuickActionInternal(
        action: SimSchedulerFollowUpQuickAction
    ) {
        when (action) {
            SimSchedulerFollowUpQuickAction.EXPLAIN -> {
                appendSchedulerFollowUpResponse(buildSchedulerFollowUpExplanation())
            }
            SimSchedulerFollowUpQuickAction.STATUS -> {
                appendSchedulerFollowUpResponse(buildSchedulerFollowUpStatus())
            }
            SimSchedulerFollowUpQuickAction.PREFILL_RESCHEDULE -> {
                val target = resolveSelectedSchedulerFollowUpTask()
                if (target == null) {
                    blockSchedulerFollowUpAction("请先选择要改期的日程")
                } else {
                    _inputText.value = "把“${target.title}”改到"
                }
            }
            SimSchedulerFollowUpQuickAction.MARK_DONE -> {
                runSchedulerFollowUpTaskMutation("mark_done") { task ->
                    val updated = task.copy(isDone = !task.isDone)
                    taskRepository.updateTask(updated)
                    if (updated.isDone) {
                        cancelReminderSafely(task.id)
                    } else {
                        scheduleReminderIfExact(updated)
                    }
                    val actionLabel = if (updated.isDone) "已标记完成" else "已恢复为待办"
                    appendSchedulerFollowUpResponse(
                        "$actionLabel：${updated.title}\n\n${formatSchedulerTaskSummary(updated)}"
                    )
                }
            }
            SimSchedulerFollowUpQuickAction.DELETE -> {
                runSchedulerFollowUpTaskMutation("delete") { task ->
                    taskRepository.deleteItem(task.id)
                    cancelReminderSafely(task.id)
                    updateSchedulerFollowUpContext { current ->
                        current.removeTask(task.id)
                    }
                    appendSchedulerFollowUpResponse("已删除：${task.title}")
                }
            }
        }
    }

    private suspend fun handleSchedulerFollowUpReschedule(content: String) {
        val task = resolveSelectedSchedulerFollowUpTask()
        if (task == null) {
            blockSchedulerFollowUpAction("请先选择要改期的日程，再输入新的时间。")
            return
        }

        val resolvedTime = SimRescheduleTimeInterpreter.resolve(
            originalTask = task,
            transcript = content,
            displayedDateIso = LocalDate.ofInstant(task.startTime, timeProvider.zoneId).toString(),
            timeProvider = timeProvider,
            uniAExtractionService = uniAExtractionService
        )
        val resolved = when (resolvedTime) {
            is SimRescheduleTimeInterpreter.Result.Success -> resolvedTime
            SimRescheduleTimeInterpreter.Result.Unsupported -> {
                blockSchedulerFollowUpAction("当前跟进只支持明确时间改期，请直接输入新的具体时间。")
                return
            }
            SimRescheduleTimeInterpreter.Result.InvalidExactTime -> {
                blockSchedulerFollowUpAction("改期时间格式无法解析，请换一种明确说法。")
                return
            }
        }

        val newStart = resolved.startTime
        val newDuration = resolved.durationMinutes ?: task.durationMinutes
        val conflict = scheduleBoard.checkConflict(
            proposedStart = newStart.toEpochMilli(),
            durationMinutes = newDuration,
            excludeId = task.id
        ) as? ConflictResult.Conflict

        val updatedTask = task.copy(
            startTime = newStart,
            durationMinutes = newDuration,
            hasConflict = conflict != null,
            conflictWithTaskId = conflict?.overlaps?.firstOrNull()?.entryId,
            conflictSummary = conflict?.overlaps?.firstOrNull()?.let { "与「${it.title}」时间冲突" },
            isVague = false
        )

        taskRepository.rescheduleTask(task.id, updatedTask)
        cancelReminderSafely(task.id)
        scheduleReminderIfExact(updatedTask)
        updateSchedulerFollowUpContext { current ->
            current.updateTask(
                taskId = task.id,
                dayOffset = dayOffsetFor(updatedTask.startTime),
                scheduledAtMillis = updatedTask.startTime.toEpochMilli(),
                durationMinutes = updatedTask.durationMinutes
            )
        }
        emitSchedulerFollowUpTelemetry(
            summary = SIM_BADGE_SCHEDULER_FOLLOW_UP_ACTION_COMPLETED_SUMMARY,
            detail = "action=reschedule taskId=${task.id}"
        )
        appendSchedulerFollowUpResponse(
            buildString {
                append("已改期：")
                append(updatedTask.title)
                append("\n\n")
                append(formatSchedulerTaskSummary(updatedTask))
                if (updatedTask.hasConflict) {
                    append("\n\n注意：")
                    append(updatedTask.conflictSummary ?: "当前存在时间冲突")
                }
            }
        )
    }

    private suspend fun runSchedulerFollowUpTaskMutation(
        action: String,
        block: suspend (ScheduledTask) -> Unit
    ) {
        val task = resolveSelectedSchedulerFollowUpTask()
        if (task == null) {
            blockSchedulerFollowUpAction("请先选择要跟进的日程。")
            return
        }
        block(task)
        emitSchedulerFollowUpTelemetry(
            summary = SIM_BADGE_SCHEDULER_FOLLOW_UP_ACTION_COMPLETED_SUMMARY,
            detail = "action=$action taskId=${task.id}"
        )
    }

    private suspend fun resolveSelectedSchedulerFollowUpTask(): ScheduledTask? {
        val context = _currentSchedulerFollowUpContext.value ?: return null
        val selectedTaskId = _selectedSchedulerFollowUpTaskId.value
            ?: defaultSelectedFollowUpTaskId(context)
            ?: return null
        return taskRepository.getTask(selectedTaskId)
            ?: context.taskSummaries.firstOrNull { it.taskId == selectedTaskId }?.let { stale ->
                appendSchedulerFollowUpResponse("未找到「${stale.title}」的当前日程记录，请重新选择其他任务。")
                _selectedSchedulerFollowUpTaskId.value = null
                null
            }
    }

    private fun buildSchedulerFollowUpExplanation(): String {
        val context = _currentSchedulerFollowUpContext.value ?: return "当前没有工牌日程跟进上下文。"
        if (context.taskSummaries.isEmpty()) {
            return "当前跟进会话已没有可继续操作的绑定日程。"
        }
        return buildString {
            append("这是工牌创建后的任务级跟进会话，仅作用于当前绑定日程。\n\n")
            context.taskSummaries.forEachIndexed { index, task ->
                append(index + 1)
                append(". ")
                append(formatSchedulerTaskSummary(task))
                if (index != context.taskSummaries.lastIndex) append('\n')
            }
        }
    }

    private suspend fun buildSchedulerFollowUpStatus(): String {
        val context = _currentSchedulerFollowUpContext.value ?: return "当前没有工牌日程跟进上下文。"
        val selectedTaskId = _selectedSchedulerFollowUpTaskId.value
        val selectedSummaries = if (selectedTaskId != null) {
            context.taskSummaries.filter { it.taskId == selectedTaskId }
        } else {
            context.taskSummaries
        }
        if (selectedSummaries.isEmpty()) {
            return "请先选择要查看状态的日程。"
        }
        return buildString {
            selectedSummaries.forEachIndexed { index, summary ->
                val liveTask = taskRepository.getTask(summary.taskId)
                append(
                    if (liveTask != null) {
                        formatSchedulerTaskSummary(liveTask)
                    } else {
                        "「${summary.title}」当前已不在日程库中。"
                    }
                )
                if (index != selectedSummaries.lastIndex) append("\n")
            }
        }
    }

    private fun buildBadgeSchedulerFollowUpSummary(
        transcript: String,
        taskSummaries: List<SchedulerFollowUpTaskSummary>
    ): String {
        return buildString {
            append("工牌已完成日程创建，并生成了一个任务级跟进会话。\n\n")
            append("原始指令：")
            append(transcript)
            append("\n\n")
            taskSummaries.forEachIndexed { index, task ->
                append(index + 1)
                append(". ")
                append(formatSchedulerTaskSummary(task))
                if (index != taskSummaries.lastIndex) append('\n')
            }
            append("\n\n可继续执行：说明、状态、改期、完成、删除。多任务场景请先点选目标任务。")
        }
    }

    private fun formatSchedulerTaskSummary(task: ScheduledTask): String {
        val status = if (task.isDone) "已完成" else if (task.hasConflict) "有冲突" else "待办"
        return "${task.title} · ${formatDayOffset(dayOffsetFor(task.startTime))} · ${task.timeDisplay} · ${status}"
    }

    private fun formatSchedulerTaskSummary(task: SchedulerFollowUpTaskSummary): String {
        return "${task.title} · ${formatDayOffset(task.dayOffset)} · ${formatTimeMillis(task.scheduledAtMillis)}"
    }

    private fun formatDayOffset(dayOffset: Int): String {
        return when (dayOffset) {
            0 -> "今天"
            1 -> "明天"
            2 -> "后天"
            else -> if (dayOffset > 0) "${dayOffset}天后" else "${-dayOffset}天前"
        }
    }

    private fun formatTimeMillis(millis: Long): String {
        return runCatching {
            val local = Instant.ofEpochMilli(millis).atZone(timeProvider.zoneId).toLocalTime()
            "%02d:%02d".format(local.hour, local.minute)
        }.getOrDefault("--:--")
    }

    private fun appendUserMessage(sessionId: String, content: String) {
        val record = sessions[sessionId] ?: return
        val timestamp = System.currentTimeMillis()
        val newMessage = ChatMessage.User(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            content = content
        )
        sessions[sessionId] = record.copy(
            preview = record.preview.copy(timestamp = timestamp),
            messages = record.messages + newMessage
        )
        _history.value = sessions.getValue(sessionId).messages
        persistSession(sessionId)
        refreshGroupedSessions()
    }

    private fun appendUserMessageForSend(sessionId: String, content: String) {
        appendUserMessage(sessionId, content)
        updateSession(sessionId) { record ->
            record.copy(
                preview = record.preview.copy(
                    summary = content.take(6).ifBlank { record.preview.summary }
                )
            )
        }
    }

    private fun appendSchedulerFollowUpResponse(content: String) {
        val sessionId = _currentSessionId.value ?: return
        appendAiMessage(sessionId, UiState.Response(content))
    }

    private fun blockSchedulerFollowUpAction(message: String) {
        emitSchedulerFollowUpTelemetry(
            summary = SIM_BADGE_SCHEDULER_FOLLOW_UP_ACTION_BLOCKED_SUMMARY,
            detail = message
        )
        appendSchedulerFollowUpResponse(message)
        _uiState.value = UiState.Error(message)
    }

    private fun defaultSelectedFollowUpTaskId(context: SchedulerFollowUpContext?): String? {
        if (context == null) return null
        return context.boundTaskIds.singleOrNull()
    }

    private fun SchedulerFollowUpTaskSummary.toScheduledTask(): ScheduledTask {
        return ScheduledTask(
            id = taskId,
            timeDisplay = formatTimeMillis(scheduledAtMillis),
            title = title,
            urgencyLevel = UrgencyLevel.L3_NORMAL,
            startTime = Instant.ofEpochMilli(scheduledAtMillis),
            durationMinutes = durationMinutes
        )
    }

    private fun updateSchedulerFollowUpContext(
        transform: (SchedulerFollowUpContext) -> SchedulerFollowUpContext
    ) {
        val sessionId = _currentSessionId.value ?: return
        updateSession(sessionId) { record ->
            val currentContext = record.preview.schedulerFollowUpContext ?: return@updateSession record
            val updatedContext = transform(currentContext).copy(updatedAt = System.currentTimeMillis())
            val updatedPreview = record.preview.copy(schedulerFollowUpContext = updatedContext)
            _currentSchedulerFollowUpContext.value = updatedContext
            _selectedSchedulerFollowUpTaskId.value = when {
                updatedContext.boundTaskIds.size == 1 -> updatedContext.boundTaskIds.single()
                _selectedSchedulerFollowUpTaskId.value in updatedContext.boundTaskIds ->
                    _selectedSchedulerFollowUpTaskId.value
                else -> null
            }
            record.copy(preview = updatedPreview)
        }
    }

    private fun SchedulerFollowUpContext.removeTask(taskId: String): SchedulerFollowUpContext {
        return copy(
            boundTaskIds = boundTaskIds.filterNot { it == taskId },
            taskSummaries = taskSummaries.filterNot { it.taskId == taskId }
        )
    }

    private fun SchedulerFollowUpContext.updateTask(
        taskId: String,
        dayOffset: Int,
        scheduledAtMillis: Long,
        durationMinutes: Int
    ): SchedulerFollowUpContext {
        return copy(
            taskSummaries = taskSummaries.map { summary ->
                if (summary.taskId == taskId) {
                    summary.copy(
                        dayOffset = dayOffset,
                        scheduledAtMillis = scheduledAtMillis,
                        durationMinutes = durationMinutes
                    )
                } else {
                    summary
                }
            }
        )
    }

    private fun dayOffsetFor(start: Instant): Int {
        return LocalDate.ofInstant(start, timeProvider.zoneId)
            .toEpochDay()
            .minus(timeProvider.today.toEpochDay())
            .toInt()
    }

    private suspend fun scheduleReminderIfExact(task: ScheduledTask) {
        if (task.isVague || task.isDone) return
        val cascade = task.alarmCascade.ifEmpty {
            UrgencyLevel.buildCascade(task.urgencyLevel)
        }
        if (cascade.isEmpty()) return
        runCatching {
            alarmScheduler.scheduleCascade(
                taskId = task.id,
                taskTitle = task.title,
                eventTime = task.startTime,
                cascade = cascade
            )
        }
    }

    private suspend fun cancelReminderSafely(taskId: String) {
        runCatching {
            alarmScheduler.cancelReminder(taskId)
        }
    }

    private fun emitSchedulerFollowUpTelemetry(summary: String, detail: String) {
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
            payloadSize = detail.length,
            summary = summary,
            rawDataDump = detail
        )
        Log.d(SIM_BADGE_FOLLOW_UP_LOG_TAG, "$summary: $detail")
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
        persistSession(sessionId)
        if (_currentSessionId.value == sessionId) {
            _history.value = sessions.getValue(sessionId).messages
        }
        refreshGroupedSessions()
    }

    private fun persistSession(sessionId: String) {
        val record = sessions[sessionId] ?: return
        sessionRepository.saveSession(
            preview = record.preview,
            messages = record.messages
        )
    }

    private fun startFreshSeededSession(initialUserInput: String): String? {
        if (initialUserInput.isBlank()) return null
        startNewSession()
        val sessionId = createSession()
        updateInput(initialUserInput)
        send()
        return sessionId
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
