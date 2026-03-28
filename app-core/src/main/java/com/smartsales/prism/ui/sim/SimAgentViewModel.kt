package com.smartsales.prism.ui.sim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.llm.Executor
import com.smartsales.core.pipeline.AgentActivity
import com.smartsales.core.pipeline.MascotInteraction
import com.smartsales.core.pipeline.MascotState
import com.smartsales.core.pipeline.RealFollowUpRescheduleExtractionService
import com.smartsales.core.pipeline.RealGlobalRescheduleExtractionService
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.prism.data.audio.DeviceSpeechFailureReason
import com.smartsales.prism.data.audio.DeviceSpeechRecognitionResult
import com.smartsales.prism.data.audio.DeviceSpeechRecognizer
import com.smartsales.prism.data.audio.SimAudioRepository
import com.smartsales.prism.data.session.SimSessionRepository
import com.smartsales.prism.domain.analyst.TaskBoardItem
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.config.SubscriptionTier
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.SchedulerFollowUpContext
import com.smartsales.prism.domain.model.SchedulerFollowUpTaskSummary
import com.smartsales.prism.domain.model.SessionKind
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.domain.scheduler.ActiveTaskRetrievalIndex
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.time.TimeProvider
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.ui.IAgentViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

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
internal const val SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_STARTED_SUMMARY =
    "SIM follow-up reschedule V2 shadow started"
internal const val SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_PARITY_SUMMARY =
    "SIM follow-up reschedule V2 shadow parity"
internal const val SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_MISMATCH_TIME_SUMMARY =
    "SIM follow-up reschedule V2 shadow mismatch time"
internal const val SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_MISMATCH_SUPPORT_SUMMARY =
    "SIM follow-up reschedule V2 shadow mismatch support"
internal const val SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_INVALID_SUMMARY =
    "SIM follow-up reschedule V2 shadow invalid"
internal const val SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_FAILURE_SUMMARY =
    "SIM follow-up reschedule V2 shadow failure"
internal const val SIM_SCHEDULER_GLOBAL_SHORTLIST_BUILT_SUMMARY =
    "SIM scheduler global shortlist built"
internal const val SIM_SCHEDULER_GLOBAL_SUGGESTION_RECEIVED_SUMMARY =
    "SIM scheduler global suggestion received"
internal const val SIM_AUDIO_CHAT_LOG_TAG = "SimAudioChat"
internal const val SIM_SCHEDULER_SHELF_LOG_TAG = "SimSchedulerShelf"
internal const val SIM_BADGE_FOLLOW_UP_CHAT_LOG_TAG = "SimBadgeFollowUpChat"
internal const val SIM_EMPTY_HOME_GREETING_FALLBACK_NAME = "SmartSales 用户"
private const val SIM_VOICE_DRAFT_TIMEOUT_MILLIS = 1_200L

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
    sessionRepository: SimSessionRepository,
    private val audioRepository: SimAudioRepository,
    private val speechRecognizer: DeviceSpeechRecognizer,
    taskRepository: ScheduledTaskRepository,
    scheduleBoard: ScheduleBoard,
    activeTaskRetrievalIndex: ActiveTaskRetrievalIndex,
    alarmScheduler: AlarmScheduler,
    uniAExtractionService: RealUniAExtractionService,
    globalRescheduleExtractionService: RealGlobalRescheduleExtractionService,
    followUpRescheduleExtractionService: RealFollowUpRescheduleExtractionService,
    executor: Executor,
    private val userProfileRepository: UserProfileRepository,
    timeProvider: TimeProvider
) : ViewModel(), IAgentViewModel {

    data class ArtifactTranscriptRevealState(
        val consumed: Boolean = false,
        val isLongTranscript: Boolean = false
    )

    private var voiceDraftRequestId = 0L

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

    private val _voiceDraftState = MutableStateFlow(SimVoiceDraftUiState())
    val voiceDraftState: StateFlow<SimVoiceDraftUiState> = _voiceDraftState.asStateFlow()

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

    val currentSubscriptionTier: SubscriptionTier
        get() = userProfileRepository.profile.value.subscriptionTier

    override val heroGreeting: StateFlow<String> = userProfileRepository.profile
        .map { profile -> formatSimHeroGreeting(profile.displayName) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = formatSimHeroGreeting(currentDisplayName)
        )

    private val _groupedSessions = MutableStateFlow<Map<String, List<SessionPreview>>>(emptyMap())
    val groupedSessions: StateFlow<Map<String, List<SessionPreview>>> =
        _groupedSessions.asStateFlow()

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

    private val bridge = SimAgentUiBridge(
        getCurrentSessionId = { _currentSessionId.value },
        getCurrentSchedulerFollowUpContext = { _currentSchedulerFollowUpContext.value },
        getSelectedSchedulerFollowUpTaskId = { _selectedSchedulerFollowUpTaskId.value },
        getUiState = { _uiState.value },
        setCurrentSessionId = { _currentSessionId.value = it },
        setUiState = { _uiState.value = it },
        setInputText = { _inputText.value = it },
        setIsSending = { _isSending.value = it },
        setErrorMessage = { _errorMessage.value = it },
        setToastMessage = { _toastMessage.value = it },
        setHistory = { _history.value = it },
        setSessionTitle = { _sessionTitle.value = it },
        setGroupedSessions = { _groupedSessions.value = it },
        setCurrentLinkedAudioId = { _currentLinkedAudioId.value = it },
        setCurrentSchedulerFollowUpContext = { _currentSchedulerFollowUpContext.value = it },
        setSelectedSchedulerFollowUpTaskId = { _selectedSchedulerFollowUpTaskId.value = it },
        removeArtifactTranscriptReveal = { messageIds ->
            _artifactTranscriptRevealState.value =
                _artifactTranscriptRevealState.value - messageIds
        }
    )

    private val sessionCoordinator = SimAgentSessionCoordinator(
        sessionRepository = sessionRepository,
        audioRepository = audioRepository,
        bridge = bridge
    )

    private val chatCoordinator = SimAgentChatCoordinator(
        sessionCoordinator = sessionCoordinator,
        audioRepository = audioRepository,
        executor = executor,
        userProfileRepository = userProfileRepository,
        bridge = bridge
    )

    private val followUpCoordinator = SimAgentFollowUpCoordinator(
        sessionCoordinator = sessionCoordinator,
        taskRepository = taskRepository,
        scheduleBoard = scheduleBoard,
        activeTaskRetrievalIndex = activeTaskRetrievalIndex,
        alarmScheduler = alarmScheduler,
        uniAExtractionService = uniAExtractionService,
        globalRescheduleExtractionService = globalRescheduleExtractionService,
        followUpRescheduleExtractionService = followUpRescheduleExtractionService,
        timeProvider = timeProvider,
        bridge = bridge
    )

    init {
        sessionCoordinator.loadPersistedSessions()
        sessionCoordinator.reconcileAudioBindings()
    }

    fun startNewSession() {
        cancelVoiceDraft()
        sessionCoordinator.startNewSession()
    }

    fun startSeededSession(initialUserInput: String) {
        cancelVoiceDraft()
        chatCoordinator.startSeededSession(initialUserInput, ::send)
    }

    fun createBadgeSchedulerFollowUpSession(
        threadId: String,
        transcript: String,
        tasks: List<SchedulerFollowUpTaskSummary>,
        batchId: String? = null
    ): String? {
        return followUpCoordinator.createBadgeSchedulerFollowUpSession(
            threadId = threadId,
            transcript = transcript,
            tasks = tasks,
            batchId = batchId
        )
    }

    suspend fun createDebugBadgeSchedulerFollowUpSession(
        threadId: String,
        transcript: String,
        tasks: List<SchedulerFollowUpTaskSummary>,
        batchId: String? = null
    ): String? {
        return followUpCoordinator.createDebugBadgeSchedulerFollowUpSession(
            threadId = threadId,
            transcript = transcript,
            tasks = tasks,
            batchId = batchId
        )
    }

    fun startSchedulerShelfSession(initialUserInput: String) {
        cancelVoiceDraft()
        chatCoordinator.startSchedulerShelfSession(initialUserInput, ::startSeededSession)
    }

    fun switchSession(sessionId: String) {
        cancelVoiceDraft()
        sessionCoordinator.switchSession(sessionId)
    }

    fun togglePin(sessionId: String) {
        sessionCoordinator.togglePin(sessionId)
    }

    fun renameSession(sessionId: String, clientName: String, summary: String) {
        sessionCoordinator.renameSession(sessionId, clientName, summary)
    }

    fun deleteSession(sessionId: String) {
        cancelVoiceDraft()
        sessionCoordinator.deleteSession(sessionId)
    }

    fun handleDeletedAudio(audioId: String) {
        sessionCoordinator.handleDeletedAudio(audioId)
    }

    fun openAudioDiscussion(
        audioId: String,
        title: String,
        summary: String?,
        summaryLabel: String = "当前预览"
    ): String {
        cancelVoiceDraft()
        val sessionId = chatCoordinator.openAudioDiscussion(
            audioId = audioId,
            title = title,
            summary = summary,
            summaryLabel = summaryLabel
        )
        if (summaryLabel != "当前状态") {
            enqueueAttachedAudioArtifacts(audioId)
        }
        return sessionId
    }

    fun selectAudioForChat(
        audioId: String,
        title: String,
        summary: String?,
        entersPendingFlow: Boolean
    ): String {
        cancelVoiceDraft()
        val sessionId = chatCoordinator.selectAudioForChat(
            audioId = audioId,
            title = title,
            summary = summary,
            entersPendingFlow = entersPendingFlow
        )
        if (!entersPendingFlow) {
            enqueueAttachedAudioArtifacts(audioId)
        }
        return sessionId
    }

    fun startVoiceDraft(): Boolean {
        return startVoiceDraft(SimVoiceDraftInteractionMode.HOLD_TO_SEND)
    }

    fun onVoiceDraftPermissionRequested() {
        val state = _voiceDraftState.value
        if (
            state.isRecording ||
            state.isProcessing ||
            state.awaitingMicPermission ||
            _isSending.value ||
            _currentSchedulerFollowUpContext.value != null ||
            speechRecognizer.isListening()
        ) {
            return
        }
        _voiceDraftState.value = state.copy(
            awaitingMicPermission = true,
            interactionMode = SimVoiceDraftInteractionMode.HOLD_TO_SEND,
            errorMessage = null
        )
    }

    fun onVoiceDraftPermissionResult(granted: Boolean) {
        val state = _voiceDraftState.value
        if (!granted) {
            _voiceDraftState.value = state.copy(
                awaitingMicPermission = false,
                interactionMode = SimVoiceDraftInteractionMode.HOLD_TO_SEND,
                errorMessage = "无法录音：未授予麦克风权限"
            )
            _toastMessage.value = "无法录音：未授予麦克风权限"
            return
        }
        if (state.awaitingMicPermission) {
            startVoiceDraft(SimVoiceDraftInteractionMode.TAP_TO_SEND)
        }
    }

    fun finishVoiceDraft() {
        val state = _voiceDraftState.value
        if (!state.isRecording) return
        val requestId = beginVoiceDraftProcessing(state)
        viewModelScope.launch {
            when (val result = resolveVoiceDraftResult(requestId)) {
                is DeviceSpeechRecognitionResult.Success -> {
                    if (!isActiveVoiceDraftRequest(requestId)) return@launch
                    _inputText.value = result.text
                    _voiceDraftState.value = SimVoiceDraftUiState()
                }

                is DeviceSpeechRecognitionResult.Failure -> {
                    if (!isActiveVoiceDraftRequest(requestId)) return@launch
                    _voiceDraftState.value = SimVoiceDraftUiState(errorMessage = result.message)
                    if (result.reason != DeviceSpeechFailureReason.CANCELLED) {
                        _toastMessage.value = result.message
                    }
                }
            }
        }
    }

    fun cancelVoiceDraft() {
        invalidateVoiceDraftRequests()
        runCatching { speechRecognizer.cancelListening() }
        _voiceDraftState.value = SimVoiceDraftUiState()
    }

    fun updatePendingAudioState(audioId: String, status: TranscriptionStatus, progress: Float) {
        chatCoordinator.updatePendingAudioState(audioId, status, progress)
    }

    fun completePendingAudio(audioId: String) {
        chatCoordinator.completePendingAudio(audioId)
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
        chatCoordinator.appendCompletedAudioArtifacts(audioId, artifacts)
    }

    private fun enqueueAttachedAudioArtifacts(audioId: String) {
        runBlocking {
            audioRepository.getArtifacts(audioId)?.let { artifacts ->
                chatCoordinator.appendCompletedAudioArtifacts(audioId, artifacts)
            }
        }
    }

    fun failPendingAudio(audioId: String, message: String) {
        chatCoordinator.failPendingAudio(audioId, message)
    }

    fun selectSchedulerFollowUpTask(taskId: String) {
        followUpCoordinator.selectSchedulerFollowUpTask(taskId)
    }

    fun performSchedulerFollowUpQuickAction(action: SimSchedulerFollowUpQuickAction) {
        viewModelScope.launch {
            followUpCoordinator.performSchedulerFollowUpQuickAction(action)
        }
    }

    override fun clearToast() {
        _toastMessage.value = null
    }

    override fun updateInput(text: String) {
        _inputText.value = text
        if (_voiceDraftState.value.errorMessage != null) {
            _voiceDraftState.value = _voiceDraftState.value.copy(errorMessage = null)
        }
    }

    override fun clearError() {
        _errorMessage.value = null
    }

    override fun confirmAnalystPlan() {
        _uiState.value = UiState.Idle
    }

    override fun send() {
        cancelVoiceDraft()
        val content = _inputText.value.trim()
        if (content.isEmpty()) return

        if (_currentSchedulerFollowUpContext.value != null) {
            _inputText.value = ""
            viewModelScope.launch {
                followUpCoordinator.handleSchedulerFollowUpInput(content)
            }
            return
        }

        val sessionId = _currentSessionId.value ?: sessionCoordinator.createGeneralSession()
        sessionCoordinator.appendUserMessageForSend(sessionId, content)
        _inputText.value = ""
        _isSending.value = true

        when (sessionCoordinator.getSession(sessionId)?.preview?.sessionKind) {
            SessionKind.AUDIO_GROUNDED -> {
                _uiState.value = UiState.Thinking("SIM 正在整理这段录音的上下文")
                viewModelScope.launch {
                    chatCoordinator.handleAudioGroundedSend(sessionId, content)
                }
            }

            SessionKind.GENERAL -> {
                _uiState.value = UiState.Thinking("SIM 正在确认当前支持的讨论范围")
                viewModelScope.launch {
                    chatCoordinator.handleGeneralSend(sessionId, content)
                }
            }

            SessionKind.SCHEDULER_FOLLOW_UP -> {
                _uiState.value = UiState.Thinking("SIM 正在处理当前日程跟进")
                viewModelScope.launch {
                    followUpCoordinator.handleSchedulerFollowUpInput(content)
                }
            }

            null -> {
                _isSending.value = false
                _uiState.value = UiState.Idle
            }
        }
    }

    private fun startVoiceDraft(
        interactionMode: SimVoiceDraftInteractionMode
    ): Boolean {
        val state = _voiceDraftState.value
        if (
            state.isRecording ||
            state.isProcessing ||
            _isSending.value ||
            _currentSchedulerFollowUpContext.value != null ||
            speechRecognizer.isListening()
        ) {
            return false
        }
        return runCatching {
            speechRecognizer.startListening()
            _voiceDraftState.value = state.copy(
                isRecording = true,
                isProcessing = false,
                awaitingMicPermission = false,
                interactionMode = interactionMode,
                errorMessage = null
            )
            true
        }.getOrElse {
            _voiceDraftState.value = SimVoiceDraftUiState(
                errorMessage = "当前无法开始录音，请重试。"
            )
            _toastMessage.value = "当前无法开始录音，请重试。"
            false
        }
    }

    private fun beginVoiceDraftProcessing(
        state: SimVoiceDraftUiState
    ): Long {
        val requestId = invalidateVoiceDraftRequests()
        _voiceDraftState.value = state.copy(
            isRecording = false,
            isProcessing = true,
            awaitingMicPermission = false,
            errorMessage = null
        )
        return requestId
    }

    private suspend fun resolveVoiceDraftResult(
        requestId: Long
    ): DeviceSpeechRecognitionResult {
        return try {
            val result = withTimeout(SIM_VOICE_DRAFT_TIMEOUT_MILLIS) {
                speechRecognizer.finishListening()
            }
            if (isActiveVoiceDraftRequest(requestId)) {
                result
            } else {
                DeviceSpeechRecognitionResult.Failure(
                    reason = DeviceSpeechFailureReason.CANCELLED,
                    message = "语音识别已取消"
                )
            }
        } catch (_: TimeoutCancellationException) {
            DeviceSpeechRecognitionResult.Failure(
                reason = DeviceSpeechFailureReason.NO_MATCH,
                message = "语音识别超时"
            )
        } catch (_: CancellationException) {
            DeviceSpeechRecognitionResult.Failure(
                reason = DeviceSpeechFailureReason.CANCELLED,
                message = "语音识别已取消"
            )
        } catch (_: IllegalStateException) {
            DeviceSpeechRecognitionResult.Failure(
                reason = DeviceSpeechFailureReason.ERROR,
                message = "当前无法识别语音，请重试"
            )
        }
    }

    private fun invalidateVoiceDraftRequests(): Long {
        voiceDraftRequestId += 1L
        return voiceDraftRequestId
    }

    private fun isActiveVoiceDraftRequest(requestId: Long): Boolean {
        return requestId == voiceDraftRequestId
    }

    override fun amendAnalystPlan() {
        _uiState.value = UiState.Idle
    }

    override fun interactWithMascot(interaction: MascotInteraction) {
        // SIM 不启用 mascot。
    }

    override fun updateSessionTitle(newTitle: String) {
        val sessionId = _currentSessionId.value ?: return
        sessionCoordinator.updateSession(sessionId) { record ->
            record.copy(
                preview = record.preview.copy(
                    clientName = newTitle.ifBlank { record.preview.clientName }
                )
            )
        }
    }

    override fun selectTaskBoardItem(itemId: String) {
        _toastMessage.value = "SIM Wave 1 不启用任务板入口"
    }

    override fun debugRunScenario(scenario: String) {
        _toastMessage.value = "SIM 屏蔽调试 HUD"
    }

    override fun onCleared() {
        cancelVoiceDraft()
        super.onCleared()
    }
}

private fun formatSimHeroGreeting(displayName: String): String {
    val resolvedName = displayName.trim().ifBlank { SIM_EMPTY_HOME_GREETING_FALLBACK_NAME }
    return "你好, $resolvedName"
}
