package com.smartsales.prism.ui.onboarding

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.prism.data.audio.DeviceSpeechFailureReason
import com.smartsales.prism.data.audio.DeviceSpeechRecognitionEvent
import com.smartsales.prism.data.audio.DeviceSpeechMode
import com.smartsales.prism.data.audio.DeviceSpeechRecognitionResult
import com.smartsales.prism.data.audio.DeviceSpeechRecognizer
import com.smartsales.prism.data.notification.ReminderReliabilityAdvisor
import com.smartsales.prism.data.onboarding.RuntimeOnboardingHandoffGate
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * onboarding 互动状态管理器。
 */
@HiltViewModel
class OnboardingInteractionViewModel @Inject constructor(
    private val speechRecognizer: DeviceSpeechRecognizer,
    private val interactionService: OnboardingInteractionService,
    private val userProfileRepository: UserProfileRepository,
    private val quickStartService: OnboardingQuickStartService,
    private val quickStartCommitter: OnboardingSchedulerQuickStartCommitter,
    private val quickStartReminderGuideCoordinator: OnboardingQuickStartReminderGuideCoordinator,
    private val quickStartCalendarExporter: OnboardingQuickStartCalendarExporter,
    private val onboardingHandoffGate: RuntimeOnboardingHandoffGate,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private var consultationRequestId = 0L
    private var profileRequestId = 0L
    private var quickStartRequestId = 0L
    private var activeSpeechLane: ProcessingLane? = null
    private var currentHost: OnboardingHost? = null

    private val _consultationState = MutableStateFlow(OnboardingConsultationUiState())
    val consultationState: StateFlow<OnboardingConsultationUiState> = _consultationState.asStateFlow()

    private val _profileState = MutableStateFlow(OnboardingProfileUiState())
    val profileState: StateFlow<OnboardingProfileUiState> = _profileState.asStateFlow()

    private val _quickStartState = MutableStateFlow(OnboardingQuickStartUiState())
    val quickStartState: StateFlow<OnboardingQuickStartUiState> = _quickStartState.asStateFlow()

    private val _effects = MutableSharedFlow<OnboardingInteractionEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<OnboardingInteractionEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            speechRecognizer.events.collect(::handleSpeechEvent)
        }
    }

    fun resetInteractionState() {
        cancelActiveRecording()
        _consultationState.value = OnboardingConsultationUiState()
        _profileState.value = OnboardingProfileUiState()
        clearQuickStartSandbox()
    }

    fun bindHost(host: OnboardingHost) {
        currentHost = host
    }

    fun startConsultationRecording(): Boolean {
        return startConsultationRecording(OnboardingMicInteractionMode.TAP_TO_SEND)
    }

    fun onConsultationMicPermissionRequested() {
        val state = _consultationState.value
        if (state.isProcessing || state.isCompleted || state.isRecording || speechRecognizer.isListening()) return
        _consultationState.value = state.copy(
            awaitingMicPermission = true,
            micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
            guidanceMessage = null,
            errorMessage = null
        )
    }

    fun onConsultationMicPermissionResult(granted: Boolean) {
        val state = _consultationState.value
        if (!granted) {
            _consultationState.value = state.copy(
                awaitingMicPermission = false,
                micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                guidanceMessage = null,
                errorMessage = "无法录音：未授予麦克风权限"
            )
            return
        }
        if (state.awaitingMicPermission) {
            _consultationState.value = state.copy(
                awaitingMicPermission = false,
                micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                guidanceMessage = MIC_PERMISSION_GRANTED_GUIDANCE,
                errorMessage = null
            )
        }
    }

    private fun startConsultationRecording(
        interactionMode: OnboardingMicInteractionMode
    ): Boolean {
        val state = _consultationState.value
        if (state.isProcessing || state.isCompleted || speechRecognizer.isListening()) return false
        return runCatching {
            speechRecognizer.startListening(DeviceSpeechMode.FUN_ASR_REALTIME)
            activeSpeechLane = ProcessingLane.CONSULTATION
            _consultationState.value = state.copy(
                hasStartedInteracting = true,
                isRecording = true,
                liveTranscript = "",
                errorMessage = null,
                guidanceMessage = null,
                awaitingMicPermission = false,
                micInteractionMode = interactionMode,
                processingPhase = OnboardingProcessingPhase.NONE
            )
            true
        }.getOrElse {
            activeSpeechLane = null
            _consultationState.value = state.copy(
                errorMessage = "当前无法开始录音，请重试。",
                guidanceMessage = null,
                awaitingMicPermission = false,
                micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                processingPhase = OnboardingProcessingPhase.NONE
            )
            false
        }
    }

    fun finishConsultationRecording() {
        val state = _consultationState.value
        if (!state.isRecording) return
        val requestId = beginConsultationProcessing(state)
        viewModelScope.launch {
            when (val resolution = resolveTranscript(requestId, ProcessingLane.CONSULTATION)) {
                is TranscriptResolution.Transcript -> {
                    processConsultationTranscript(
                        requestId = requestId,
                        transcript = resolution.text,
                        transcriptOrigin = resolution.origin
                    )
                }

                is TranscriptResolution.Failure -> {
                    handleConsultationFailure(
                        requestId = requestId,
                        cause = resolution.cause
                    )
                }

                null -> Unit
            }
        }
    }

    fun startProfileRecording(): Boolean {
        return startProfileRecording(OnboardingMicInteractionMode.TAP_TO_SEND)
    }

    fun onProfileMicPermissionRequested() {
        val state = _profileState.value
        if (state.isProcessing || state.hasExtractionResult || state.isRecording || speechRecognizer.isListening()) return
        _profileState.value = state.copy(
            awaitingMicPermission = true,
            micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
            guidanceMessage = null,
            errorMessage = null
        )
    }

    fun onProfileMicPermissionResult(granted: Boolean) {
        val state = _profileState.value
        if (!granted) {
            _profileState.value = state.copy(
                awaitingMicPermission = false,
                micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                guidanceMessage = null,
                errorMessage = "无法录音：未授予麦克风权限"
            )
            return
        }
        if (state.awaitingMicPermission) {
            _profileState.value = state.copy(
                awaitingMicPermission = false,
                micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                guidanceMessage = MIC_PERMISSION_GRANTED_GUIDANCE,
                errorMessage = null
            )
        }
    }

    private fun startProfileRecording(
        interactionMode: OnboardingMicInteractionMode
    ): Boolean {
        val state = _profileState.value
        if (state.isProcessing || state.hasExtractionResult || speechRecognizer.isListening()) return false
        return runCatching {
            speechRecognizer.startListening(DeviceSpeechMode.FUN_ASR_REALTIME)
            activeSpeechLane = ProcessingLane.PROFILE
            _profileState.value = state.copy(
                hasStartedInteracting = true,
                isRecording = true,
                liveTranscript = "",
                errorMessage = null,
                guidanceMessage = null,
                awaitingMicPermission = false,
                micInteractionMode = interactionMode,
                processingPhase = OnboardingProcessingPhase.NONE
            )
            true
        }.getOrElse {
            activeSpeechLane = null
            _profileState.value = state.copy(
                errorMessage = "当前无法开始录音，请重试。",
                guidanceMessage = null,
                awaitingMicPermission = false,
                micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                processingPhase = OnboardingProcessingPhase.NONE
            )
            false
        }
    }

    fun finishProfileRecording() {
        val state = _profileState.value
        if (!state.isRecording) return
        val requestId = beginProfileProcessing(state)
        viewModelScope.launch {
            when (val resolution = resolveTranscript(requestId, ProcessingLane.PROFILE)) {
                is TranscriptResolution.Transcript -> {
                    processProfileTranscript(
                        requestId = requestId,
                        transcript = resolution.text,
                        transcriptOrigin = resolution.origin
                    )
                }

                is TranscriptResolution.Failure -> {
                    handleProfileFailure(
                        requestId = requestId,
                        cause = resolution.cause
                    )
                }

                null -> Unit
            }
        }
    }

    fun startQuickStartRecording(): Boolean {
        return startQuickStartRecording(OnboardingMicInteractionMode.TAP_TO_SEND)
    }

    fun onQuickStartMicPermissionRequested() {
        val state = _quickStartState.value
        if (state.isProcessing || state.isRecording || speechRecognizer.isListening()) return
        _quickStartState.value = state.copy(
            awaitingMicPermission = true,
            micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
            transientNoticeMessage = null,
            guidanceMessage = null,
            errorMessage = null
        )
    }

    fun onQuickStartMicPermissionResult(granted: Boolean) {
        val state = _quickStartState.value
        if (!granted) {
            _quickStartState.value = state.copy(
                awaitingMicPermission = false,
                micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                transientNoticeMessage = null,
                guidanceMessage = null,
                errorMessage = "无法录音：未授予麦克风权限"
            )
            return
        }
        if (state.awaitingMicPermission) {
            _quickStartState.value = state.copy(
                awaitingMicPermission = false,
                micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                transientNoticeMessage = null,
                guidanceMessage = QUICK_START_MIC_PERMISSION_GRANTED_GUIDANCE,
                errorMessage = null
            )
        }
    }

    fun onQuickStartCalendarPermissionResult(granted: Boolean) {
        val state = _quickStartState.value
        _quickStartState.value = state.copy(
            calendarPermissionGranted = granted,
            transientNoticeMessage = if (granted) {
                "系统日历已开启，完成后会同步到系统日历。"
            } else {
                "未开启系统日历权限，仍可继续体验，完成后不会同步到系统日历。"
            },
            transientNoticeToken = state.transientNoticeToken + 1,
            errorMessage = null
        )
    }

    fun clearQuickStartTransientNotice() {
        val state = _quickStartState.value
        if (state.transientNoticeMessage == null) return
        _quickStartState.value = state.copy(transientNoticeMessage = null)
    }

    fun dismissQuickStartReminderGuide() {
        val state = _quickStartState.value
        if (state.reminderGuide == null) return
        _quickStartState.value = state.copy(reminderGuide = null)
    }

    fun openQuickStartReminderAction(action: ReminderReliabilityAdvisor.Action) {
        quickStartReminderGuideCoordinator.openAction(action)
        dismissQuickStartReminderGuide()
    }

    private fun startQuickStartRecording(
        interactionMode: OnboardingMicInteractionMode
    ): Boolean {
        val state = _quickStartState.value
        if (state.isProcessing || state.isRecording || speechRecognizer.isListening()) return false
        return runCatching {
            speechRecognizer.startListening(DeviceSpeechMode.FUN_ASR_REALTIME)
            activeSpeechLane = ProcessingLane.QUICK_START
            _quickStartState.value = state.copy(
                isRecording = true,
                liveTranscript = "",
                errorMessage = null,
                transientNoticeMessage = null,
                guidanceMessage = null,
                awaitingMicPermission = false,
                micInteractionMode = interactionMode,
                processingPhase = OnboardingProcessingPhase.NONE
            )
            true
        }.getOrElse {
            activeSpeechLane = null
            _quickStartState.value = state.copy(
                errorMessage = "当前无法开始录音，请重试。",
                transientNoticeMessage = null,
                guidanceMessage = null,
                awaitingMicPermission = false,
                micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                processingPhase = OnboardingProcessingPhase.NONE
            )
            false
        }
    }

    fun finishQuickStartRecording() {
        val state = _quickStartState.value
        if (!state.isRecording) return
        val requestId = beginQuickStartProcessing(state)
        viewModelScope.launch {
            when (val resolution = resolveTranscript(requestId, ProcessingLane.QUICK_START)) {
                is TranscriptResolution.Transcript -> {
                    processQuickStartTranscript(
                        requestId = requestId,
                        transcript = resolution.text,
                        transcriptOrigin = resolution.origin
                    )
                }

                is TranscriptResolution.Failure -> {
                    handleQuickStartFailure(
                        requestId = requestId,
                        transcript = state.liveTranscript,
                        transcriptOrigin = null,
                        cause = resolution.cause
                    )
                }

                null -> Unit
            }
        }
    }

    suspend fun finalizeFullAppCompletion(): String? {
        val quickStartState = _quickStartState.value
        if (quickStartState.isRecording || quickStartState.isProcessing) {
            return "体验日程仍在处理中，请稍候。"
        }
        if (quickStartState.items.isEmpty()) {
            return "请先完成一次真实日程体验。"
        }
        return when (val result = quickStartCommitter.commitIfNeeded()) {
            is OnboardingSchedulerQuickStartCommitResult.Success -> {
                if (quickStartState.calendarPermissionGranted) {
                    runCatching {
                        quickStartCalendarExporter.exportCommittedTaskIds(result.taskIds)
                    }
                }
                onboardingHandoffGate.markSchedulerAutoOpenPending()
                clearQuickStartSandbox()
                null
            }
            OnboardingSchedulerQuickStartCommitResult.Noop -> {
                onboardingHandoffGate.markSchedulerAutoOpenPending()
                clearQuickStartSandbox()
                null
            }
            is OnboardingSchedulerQuickStartCommitResult.Failure -> result.message
        }
    }

    fun clearQuickStartSandbox() {
        quickStartRequestId += 1
        quickStartCommitter.clear()
        _quickStartState.value = OnboardingQuickStartUiState()
    }

    fun saveProfileDraft() {
        val state = _profileState.value
        val draft = state.draft ?: return
        if (state.isSaving) return
        _profileState.value = state.copy(
            isSaving = true,
            errorMessage = null
        )
        viewModelScope.launch {
            val current = userProfileRepository.getProfile()
            val updated = current.copy(
                displayName = draft.displayName.ifBlank { current.displayName },
                role = draft.role.ifBlank { current.role },
                industry = draft.industry.ifBlank { current.industry },
                experienceYears = draft.experienceYears.ifBlank { current.experienceYears },
                communicationPlatform = draft.communicationPlatform.ifBlank { current.communicationPlatform },
                experienceLevel = deriveExperienceLevel(
                    currentLevel = current.experienceLevel,
                    experienceYears = draft.experienceYears.ifBlank { current.experienceYears }
                ),
                updatedAt = System.currentTimeMillis()
            )
            runCatching {
                userProfileRepository.updateProfile(updated)
            }.onSuccess {
                _profileState.value = _profileState.value.copy(
                    isSaving = false,
                    errorMessage = null
                )
                _effects.tryEmit(OnboardingInteractionEffect.AdvanceProfileStep)
            }.onFailure {
                _profileState.value = _profileState.value.copy(
                    isSaving = false,
                    errorMessage = "资料保存失败，您可以重试或直接继续下一步。"
                )
            }
        }
    }

    fun skipProfileSave() {
        _effects.tryEmit(OnboardingInteractionEffect.AdvanceProfileStep)
    }

    fun cancelActiveRecording() {
        invalidateProcessingRequests()
        activeSpeechLane = null
        runCatching { speechRecognizer.cancelListening() }
        _consultationState.value = _consultationState.value.copy(
            isRecording = false,
            isProcessing = false,
            liveTranscript = "",
            awaitingMicPermission = false,
            guidanceMessage = null,
            micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
            processingPhase = OnboardingProcessingPhase.NONE
        )
        _profileState.value = _profileState.value.copy(
            isRecording = false,
            isProcessing = false,
            liveTranscript = "",
            awaitingMicPermission = false,
            guidanceMessage = null,
            micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
            processingPhase = OnboardingProcessingPhase.NONE
        )
        _quickStartState.value = _quickStartState.value.copy(
            isRecording = false,
            isProcessing = false,
            liveTranscript = "",
            awaitingMicPermission = false,
            errorMessage = null,
            transientNoticeMessage = null,
            guidanceMessage = null,
            micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
            processingPhase = OnboardingProcessingPhase.NONE
        )
    }

    override fun onCleared() {
        cancelActiveRecording()
        super.onCleared()
    }

    private suspend fun processConsultationTranscript(
        requestId: Long,
        transcript: String,
        transcriptOrigin: OnboardingTranscriptOrigin
    ) {
        val normalized = transcript.trim()
        if (normalized.length < MIN_TRANSCRIPT_LENGTH) {
            handleConsultationFailure(
                requestId = requestId,
                transcript = normalized,
                transcriptOrigin = transcriptOrigin,
                cause = FailureCause.TRANSCRIPT_TOO_SHORT
            )
            return
        }
        updateConsultationStateIfCurrent(requestId) {
            it.copy(processingPhase = OnboardingProcessingPhase.BUILDING_CONSULTATION_REPLY)
        }
        val round = _consultationState.value.completedRounds + 1
        var failureCause = FailureCause.LLM_FAILURE
        val result = try {
            withTimeout(CONSULTATION_LLM_DEADLINE_MS) {
                interactionService.generateConsultationReply(normalized, round)
            }
        } catch (_: TimeoutCancellationException) {
            failureCause = FailureCause.LLM_TIMEOUT
            OnboardingConsultationServiceResult.Failure("当前无法继续这段咨询，请稍后重试。")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failureCause = FailureCause.LLM_EXCEPTION
            OnboardingConsultationServiceResult.Failure("当前无法继续这段咨询，请稍后重试。")
        }
        when (result) {
            is OnboardingConsultationServiceResult.Success -> {
                activeSpeechLane = null
                updateConsultationStateIfCurrent(requestId) { current ->
                    current.copy(
                        isProcessing = false,
                        liveTranscript = "",
                        messages = current.messages + listOf(
                            OnboardingInteractionMessage(OnboardingMessageRole.USER, normalized),
                            OnboardingInteractionMessage(OnboardingMessageRole.AI, result.reply)
                        ),
                        completedRounds = round,
                        errorMessage = null,
                        guidanceMessage = null,
                        micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                        processingPhase = OnboardingProcessingPhase.NONE,
                        lastTranscriptOrigin = transcriptOrigin,
                        lastGenerationOrigin = OnboardingGenerationOrigin.LLM
                    )
                }
                logProcessingCleared(
                    lane = ProcessingLane.CONSULTATION,
                    requestId = requestId,
                    outcome = "consultation_reply",
                    transcriptOrigin = transcriptOrigin,
                    generationOrigin = OnboardingGenerationOrigin.LLM
                )
            }

            is OnboardingConsultationServiceResult.Failure -> {
                handleConsultationFailure(
                    requestId = requestId,
                    transcript = normalized,
                    transcriptOrigin = transcriptOrigin,
                    cause = failureCause
                )
            }
        }
    }

    private suspend fun processProfileTranscript(
        requestId: Long,
        transcript: String,
        transcriptOrigin: OnboardingTranscriptOrigin
    ) {
        val normalized = transcript.trim()
        if (normalized.length < MIN_TRANSCRIPT_LENGTH) {
            handleProfileFailure(
                requestId = requestId,
                transcript = normalized,
                transcriptOrigin = transcriptOrigin,
                cause = FailureCause.TRANSCRIPT_TOO_SHORT
            )
            return
        }
        updateProfileStateIfCurrent(requestId) {
            it.copy(processingPhase = OnboardingProcessingPhase.BUILDING_PROFILE_RESULT)
        }
        var failureCause = FailureCause.LLM_FAILURE
        val result = try {
            withTimeout(PROFILE_LLM_DEADLINE_MS) {
                interactionService.extractProfile(normalized)
            }
        } catch (_: TimeoutCancellationException) {
            failureCause = FailureCause.LLM_TIMEOUT
            OnboardingProfileExtractionServiceResult.Failure("当前无法提取资料，请稍后重试。")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failureCause = FailureCause.LLM_EXCEPTION
            OnboardingProfileExtractionServiceResult.Failure("当前无法提取资料，请稍后重试。")
        }
        when (result) {
            is OnboardingProfileExtractionServiceResult.Success -> {
                activeSpeechLane = null
                updateProfileStateIfCurrent(requestId) {
                    it.copy(
                        isProcessing = false,
                        liveTranscript = "",
                        transcript = normalized,
                        acknowledgement = result.acknowledgement,
                        draft = result.draft,
                        errorMessage = null,
                        guidanceMessage = null,
                        micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                        processingPhase = OnboardingProcessingPhase.NONE,
                        transcriptOrigin = transcriptOrigin,
                        generationOrigin = OnboardingGenerationOrigin.LLM
                    )
                }
                logProcessingCleared(
                    lane = ProcessingLane.PROFILE,
                    requestId = requestId,
                    outcome = "profile_result",
                    transcriptOrigin = transcriptOrigin,
                    generationOrigin = OnboardingGenerationOrigin.LLM
                )
            }

            is OnboardingProfileExtractionServiceResult.Failure -> {
                handleProfileFailure(
                    requestId = requestId,
                    transcript = normalized,
                    transcriptOrigin = transcriptOrigin,
                    cause = failureCause
                )
            }
        }
    }

    private fun handleConsultationFailure(
        requestId: Long,
        transcript: String? = null,
        transcriptOrigin: OnboardingTranscriptOrigin? = null,
        cause: FailureCause
    ) {
        surfaceConsultationFailure(
            requestId = requestId,
            transcript = transcript,
            transcriptOrigin = transcriptOrigin,
            cause = cause
        )
    }

    private fun handleProfileFailure(
        requestId: Long,
        transcript: String? = null,
        transcriptOrigin: OnboardingTranscriptOrigin? = null,
        cause: FailureCause
    ) {
        surfaceProfileFailure(
            requestId = requestId,
            transcript = transcript,
            transcriptOrigin = transcriptOrigin,
            cause = cause
        )
    }

    private fun surfaceConsultationFailure(
        requestId: Long,
        transcript: String?,
        transcriptOrigin: OnboardingTranscriptOrigin?,
        cause: FailureCause
    ) {
        activeSpeechLane = null
        val resolvedTranscript = transcript?.trim()?.takeIf { it.isNotBlank() }
        updateConsultationStateIfCurrent(requestId) { current ->
            current.copy(
                isProcessing = false,
                liveTranscript = "",
                messages = if (resolvedTranscript != null) {
                    current.messages + OnboardingInteractionMessage(
                        OnboardingMessageRole.USER,
                        resolvedTranscript
                    )
                } else {
                    current.messages
                },
                errorMessage = consultationFailureMessage(cause),
                guidanceMessage = null,
                micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                processingPhase = OnboardingProcessingPhase.NONE,
                lastTranscriptOrigin = transcriptOrigin,
                lastGenerationOrigin = null
            )
        }
        logProcessingFailed(
            lane = ProcessingLane.CONSULTATION,
            requestId = requestId,
            cause = cause,
            transcriptOrigin = transcriptOrigin
        )
    }

    private fun surfaceProfileFailure(
        requestId: Long,
        transcript: String?,
        transcriptOrigin: OnboardingTranscriptOrigin?,
        cause: FailureCause
    ) {
        activeSpeechLane = null
        updateProfileStateIfCurrent(requestId) {
            it.copy(
                isProcessing = false,
                liveTranscript = "",
                transcript = transcript?.trim().orEmpty(),
                acknowledgement = "",
                draft = null,
                errorMessage = profileFailureMessage(cause),
                guidanceMessage = null,
                micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                processingPhase = OnboardingProcessingPhase.NONE,
                transcriptOrigin = transcriptOrigin,
                generationOrigin = null
            )
        }
        logProcessingFailed(
            lane = ProcessingLane.PROFILE,
            requestId = requestId,
            cause = cause,
            transcriptOrigin = transcriptOrigin
        )
    }

    private suspend fun processQuickStartTranscript(
        requestId: Long,
        transcript: String,
        transcriptOrigin: OnboardingTranscriptOrigin
    ) {
        val normalized = transcript.trim()
        if (normalized.length < MIN_TRANSCRIPT_LENGTH) {
            handleQuickStartFailure(
                requestId = requestId,
                transcript = normalized,
                transcriptOrigin = transcriptOrigin,
                cause = FailureCause.TRANSCRIPT_TOO_SHORT
            )
            return
        }
        updateQuickStartStateIfCurrent(requestId) {
            it.copy(processingPhase = OnboardingProcessingPhase.BUILDING_QUICK_START_RESULT)
        }

        val startedAt = SystemClock.elapsedRealtime()
        Log.d(
            TAG,
            "quick_start_apply_begin host=${currentHost.logName} requestId=$requestId transcriptLength=${normalized.length} stagedItems=${_quickStartState.value.items.size}"
        )
        val result = try {
            withTimeout(QUICK_START_LLM_DEADLINE_MS) {
                quickStartService.applyTranscript(normalized, _quickStartState.value.items)
            }
        } catch (_: TimeoutCancellationException) {
            Log.w(
                TAG,
                "quick_start_apply_timeout host=${currentHost.logName} requestId=$requestId durationMs=${SystemClock.elapsedRealtime() - startedAt} transcriptOrigin=${transcriptOrigin.logName}"
            )
            handleQuickStartFailure(
                requestId = requestId,
                transcript = normalized,
                transcriptOrigin = transcriptOrigin,
                cause = FailureCause.LLM_TIMEOUT
            )
            return
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(
                TAG,
                "quick_start_apply_exception host=${currentHost.logName} requestId=$requestId durationMs=${SystemClock.elapsedRealtime() - startedAt} transcriptOrigin=${transcriptOrigin.logName} message=${error.message}",
                error
            )
            handleQuickStartFailure(
                requestId = requestId,
                transcript = normalized,
                transcriptOrigin = transcriptOrigin,
                cause = FailureCause.LLM_EXCEPTION
            )
            return
        }

        when (result) {
            is OnboardingQuickStartServiceResult.Success -> {
                Log.d(
                    TAG,
                    "quick_start_apply_success host=${currentHost.logName} requestId=$requestId durationMs=${SystemClock.elapsedRealtime() - startedAt} items=${result.items.size} touchedExact=${result.touchedExactTask} mutation=${result.mutationKind.name.lowercase()}"
                )
                activeSpeechLane = null
                quickStartCommitter.stage(result.items)
                val reminderGuide = if (
                    result.touchedExactTask &&
                    !quickStartState.value.reminderGuidePrompted
                ) {
                    quickStartReminderGuideCoordinator.consumeGuideIfNeeded()
                } else {
                    null
                }
                updateQuickStartStateIfCurrent(requestId) { current ->
                    val nextCalendarToken =
                        if (result.touchedExactTask && !current.calendarPermissionRequested) {
                            current.calendarPermissionRequestToken + 1
                        } else {
                            current.calendarPermissionRequestToken
                        }
                    current.copy(
                        isProcessing = false,
                        liveTranscript = "",
                        transcript = normalized,
                        items = result.items,
                        errorMessage = null,
                        transientNoticeMessage = null,
                        guidanceMessage = when (result.mutationKind) {
                            OnboardingQuickStartServiceResult.Success.MutationKind.CREATE ->
                                "可以继续补充或修改，也可以直接下一步。"
                            OnboardingQuickStartServiceResult.Success.MutationKind.UPDATE ->
                                "已更新体验日程，你也可以继续补充。"
                        },
                        micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                        processingPhase = OnboardingProcessingPhase.NONE,
                        lastTranscriptOrigin = transcriptOrigin,
                        lastGenerationOrigin = OnboardingGenerationOrigin.SCHEDULER_PATH_A,
                        reminderGuide = reminderGuide,
                        reminderGuidePrompted = current.reminderGuidePrompted || reminderGuide != null,
                        calendarPermissionRequested = current.calendarPermissionRequested || result.touchedExactTask,
                        calendarPermissionRequestToken = nextCalendarToken
                    )
                }
                logProcessingCleared(
                    lane = ProcessingLane.QUICK_START,
                    requestId = requestId,
                    outcome = "quick_start_result",
                    transcriptOrigin = transcriptOrigin,
                    generationOrigin = OnboardingGenerationOrigin.SCHEDULER_PATH_A
                )
            }

            is OnboardingQuickStartServiceResult.Failure -> {
                Log.w(
                    TAG,
                    "quick_start_apply_failure host=${currentHost.logName} requestId=$requestId durationMs=${SystemClock.elapsedRealtime() - startedAt} message=${result.message}"
                )
                handleQuickStartFailure(
                    requestId = requestId,
                    transcript = normalized,
                    transcriptOrigin = transcriptOrigin,
                    cause = FailureCause.LLM_FAILURE,
                    messageOverride = result.message
                )
            }
        }
    }

    private fun handleQuickStartFailure(
        requestId: Long,
        transcript: String? = null,
        transcriptOrigin: OnboardingTranscriptOrigin? = null,
        cause: FailureCause,
        messageOverride: String? = null
    ) {
        activeSpeechLane = null
        updateQuickStartStateIfCurrent(requestId) {
            it.copy(
                isProcessing = false,
                liveTranscript = "",
                transcript = transcript?.trim().orEmpty(),
                errorMessage = messageOverride ?: quickStartFailureMessage(cause),
                transientNoticeMessage = null,
                guidanceMessage = null,
                micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                processingPhase = OnboardingProcessingPhase.NONE,
                lastTranscriptOrigin = transcriptOrigin,
                lastGenerationOrigin = null
            )
        }
        logProcessingFailed(
            lane = ProcessingLane.QUICK_START,
            requestId = requestId,
            cause = cause,
            transcriptOrigin = transcriptOrigin
        )
    }

    private suspend fun resolveTranscript(
        requestId: Long,
        lane: ProcessingLane
    ): TranscriptResolution? {
        val result = try {
            withTimeout(DEVICE_RECOGNITION_TIMEOUT_MS) {
                speechRecognizer.finishListening()
            }
        } catch (_: TimeoutCancellationException) {
            runCatching { speechRecognizer.cancelListening() }
            logRecognizerResolution(
                lane = lane,
                requestId = requestId,
                outcome = "timeout",
                current = isCurrentRequest(lane, requestId)
            )
            return currentResolutionOrNull(
                requestId,
                lane,
                TranscriptResolution.Failure(cause = FailureCause.RECOGNIZER_TIMEOUT)
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            runCatching { speechRecognizer.cancelListening() }
            logRecognizerResolution(
                lane = lane,
                requestId = requestId,
                outcome = "exception",
                current = isCurrentRequest(lane, requestId)
            )
            return currentResolutionOrNull(
                requestId,
                lane,
                TranscriptResolution.Failure(cause = FailureCause.RECOGNIZER_EXCEPTION)
            )
        }

        logRecognizerResolution(
            lane = lane,
            requestId = requestId,
            outcome = when (result) {
                is DeviceSpeechRecognitionResult.Success -> "success"
                is DeviceSpeechRecognitionResult.Failure -> "failure:${result.reason.name}"
            },
            current = isCurrentRequest(lane, requestId)
        )

        return when (result) {
            is DeviceSpeechRecognitionResult.Success -> {
                val normalized = result.text.trim()
                if (normalized.length < MIN_TRANSCRIPT_LENGTH) {
                    currentResolutionOrNull(
                        requestId,
                        lane,
                        TranscriptResolution.Failure(cause = FailureCause.TRANSCRIPT_TOO_SHORT)
                    )
                } else {
                    currentResolutionOrNull(
                        requestId = requestId,
                        lane = lane,
                        resolution = TranscriptResolution.Transcript(
                            text = normalized,
                            origin = OnboardingTranscriptOrigin.DEVICE_SPEECH
                        )
                    )
                }
            }

            is DeviceSpeechRecognitionResult.Failure -> {
                if (result.reason == DeviceSpeechFailureReason.CANCELLED) {
                    currentResolutionOrNull(
                        requestId = requestId,
                        lane = lane,
                        resolution = TranscriptResolution.Failure(cause = FailureCause.RECOGNIZER_CANCELLED)
                    )
                } else {
                    currentResolutionOrNull(
                        requestId = requestId,
                        lane = lane,
                        resolution = TranscriptResolution.Failure(cause = FailureCause.RECOGNIZER_FAILURE)
                    )
                }
            }
        }
    }

    private fun currentResolutionOrNull(
        requestId: Long,
        lane: ProcessingLane,
        resolution: TranscriptResolution
    ): TranscriptResolution? {
        return if (isCurrentRequest(lane, requestId)) resolution else null
    }

    private fun beginConsultationProcessing(
        state: OnboardingConsultationUiState
    ): Long {
        consultationRequestId += 1
        logProcessingStarted(
            lane = ProcessingLane.CONSULTATION,
            requestId = consultationRequestId
        )
        _consultationState.value = state.copy(
            isRecording = false,
            isProcessing = true,
            errorMessage = null,
            guidanceMessage = null,
            awaitingMicPermission = false,
            micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
            processingPhase = OnboardingProcessingPhase.RECOGNIZING
        )
        return consultationRequestId
    }

    private fun beginProfileProcessing(
        state: OnboardingProfileUiState
    ): Long {
        profileRequestId += 1
        logProcessingStarted(
            lane = ProcessingLane.PROFILE,
            requestId = profileRequestId
        )
        _profileState.value = state.copy(
            isRecording = false,
            isProcessing = true,
            errorMessage = null,
            guidanceMessage = null,
            awaitingMicPermission = false,
            micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
            processingPhase = OnboardingProcessingPhase.RECOGNIZING
        )
        return profileRequestId
    }

    private fun beginQuickStartProcessing(
        state: OnboardingQuickStartUiState
    ): Long {
        quickStartRequestId += 1
        logProcessingStarted(
            lane = ProcessingLane.QUICK_START,
            requestId = quickStartRequestId
        )
        _quickStartState.value = state.copy(
            isRecording = false,
            isProcessing = true,
            errorMessage = null,
            guidanceMessage = null,
            awaitingMicPermission = false,
            micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
            processingPhase = OnboardingProcessingPhase.RECOGNIZING
        )
        return quickStartRequestId
    }

    private fun invalidateProcessingRequests() {
        consultationRequestId += 1
        profileRequestId += 1
        quickStartRequestId += 1
    }

    private fun isCurrentConsultationRequest(requestId: Long): Boolean = consultationRequestId == requestId

    private fun isCurrentProfileRequest(requestId: Long): Boolean = profileRequestId == requestId

    private fun isCurrentQuickStartRequest(requestId: Long): Boolean = quickStartRequestId == requestId

    private fun isCurrentRequest(
        lane: ProcessingLane,
        requestId: Long
    ): Boolean {
        return when (lane) {
            ProcessingLane.CONSULTATION -> isCurrentConsultationRequest(requestId)
            ProcessingLane.PROFILE -> isCurrentProfileRequest(requestId)
            ProcessingLane.QUICK_START -> isCurrentQuickStartRequest(requestId)
        }
    }

    private inline fun updateConsultationStateIfCurrent(
        requestId: Long,
        transform: (OnboardingConsultationUiState) -> OnboardingConsultationUiState
    ) {
        if (!isCurrentConsultationRequest(requestId)) return
        _consultationState.value = transform(_consultationState.value)
    }

    private inline fun updateProfileStateIfCurrent(
        requestId: Long,
        transform: (OnboardingProfileUiState) -> OnboardingProfileUiState
    ) {
        if (!isCurrentProfileRequest(requestId)) return
        _profileState.value = transform(_profileState.value)
    }

    private inline fun updateQuickStartStateIfCurrent(
        requestId: Long,
        transform: (OnboardingQuickStartUiState) -> OnboardingQuickStartUiState
    ) {
        if (!isCurrentQuickStartRequest(requestId)) return
        _quickStartState.value = transform(_quickStartState.value)
    }

    private sealed interface TranscriptResolution {
        data class Transcript(
            val text: String,
            val origin: OnboardingTranscriptOrigin
        ) : TranscriptResolution

        data class Failure(val cause: FailureCause) : TranscriptResolution
    }

    private enum class ProcessingLane {
        CONSULTATION,
        PROFILE,
        QUICK_START
    }

    private enum class FailureCause {
        RECOGNIZER_TIMEOUT,
        RECOGNIZER_EXCEPTION,
        RECOGNIZER_FAILURE,
        RECOGNIZER_CANCELLED,
        TRANSCRIPT_TOO_SHORT,
        LLM_TIMEOUT,
        LLM_FAILURE,
        LLM_EXCEPTION
    }

    private fun handleSpeechEvent(event: DeviceSpeechRecognitionEvent) {
        when (event) {
            DeviceSpeechRecognitionEvent.ListeningStarted -> Unit
            DeviceSpeechRecognitionEvent.CaptureLimitReached -> handleCaptureLimitReached()
            is DeviceSpeechRecognitionEvent.PartialTranscript -> updateLiveTranscript(event.text)
            is DeviceSpeechRecognitionEvent.FinalTranscript -> updateLiveTranscript(event.text)
            is DeviceSpeechRecognitionEvent.Failure -> handleRealtimeFailure(event)
            DeviceSpeechRecognitionEvent.Cancelled -> {
                handleRealtimeCancelled()
            }
        }
    }

    private fun handleRealtimeFailure(event: DeviceSpeechRecognitionEvent.Failure) {
        when (activeSpeechLane) {
            ProcessingLane.CONSULTATION -> {
                val state = _consultationState.value
                if (state.isRecording && !state.isProcessing) {
                    activeSpeechLane = null
                    _consultationState.value = state.copy(
                        isRecording = false,
                        isProcessing = false,
                        liveTranscript = "",
                        errorMessage = consultationFailureMessage(event.reason),
                        guidanceMessage = null,
                        micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                        processingPhase = OnboardingProcessingPhase.NONE
                    )
                    logRealtimeFailureWhileRecording(
                        lane = ProcessingLane.CONSULTATION,
                        reason = event.reason,
                        message = event.message
                    )
                }
            }

            ProcessingLane.PROFILE -> {
                val state = _profileState.value
                if (state.isRecording && !state.isProcessing) {
                    activeSpeechLane = null
                    _profileState.value = state.copy(
                        isRecording = false,
                        isProcessing = false,
                        liveTranscript = "",
                        errorMessage = profileFailureMessage(event.reason),
                        guidanceMessage = null,
                        micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                        processingPhase = OnboardingProcessingPhase.NONE
                    )
                    logRealtimeFailureWhileRecording(
                        lane = ProcessingLane.PROFILE,
                        reason = event.reason,
                        message = event.message
                    )
                }
            }

            ProcessingLane.QUICK_START -> {
                val state = _quickStartState.value
                if (state.isRecording && !state.isProcessing) {
                    activeSpeechLane = null
                    _quickStartState.value = state.copy(
                        isRecording = false,
                        isProcessing = false,
                        liveTranscript = "",
                        errorMessage = quickStartFailureMessage(event.reason),
                        guidanceMessage = null,
                        micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                        processingPhase = OnboardingProcessingPhase.NONE
                    )
                    logRealtimeFailureWhileRecording(
                        lane = ProcessingLane.QUICK_START,
                        reason = event.reason,
                        message = event.message
                    )
                }
            }

            null -> Unit
        }
    }

    private fun handleRealtimeCancelled() {
        when (activeSpeechLane) {
            ProcessingLane.CONSULTATION -> {
                val state = _consultationState.value
                if (state.isRecording && !state.isProcessing) {
                    activeSpeechLane = null
                    _consultationState.value = state.copy(
                        isRecording = false,
                        isProcessing = false,
                        liveTranscript = "",
                        errorMessage = consultationFailureMessage(DeviceSpeechFailureReason.CANCELLED),
                        guidanceMessage = null,
                        micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                        processingPhase = OnboardingProcessingPhase.NONE
                    )
                    logRealtimeFailureWhileRecording(
                        lane = ProcessingLane.CONSULTATION,
                        reason = DeviceSpeechFailureReason.CANCELLED,
                        message = "realtime_cancelled_while_recording"
                    )
                } else {
                    activeSpeechLane = null
                    clearLiveTranscript()
                }
            }

            ProcessingLane.PROFILE -> {
                val state = _profileState.value
                if (state.isRecording && !state.isProcessing) {
                    activeSpeechLane = null
                    _profileState.value = state.copy(
                        isRecording = false,
                        isProcessing = false,
                        liveTranscript = "",
                        errorMessage = profileFailureMessage(DeviceSpeechFailureReason.CANCELLED),
                        guidanceMessage = null,
                        micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                        processingPhase = OnboardingProcessingPhase.NONE
                    )
                    logRealtimeFailureWhileRecording(
                        lane = ProcessingLane.PROFILE,
                        reason = DeviceSpeechFailureReason.CANCELLED,
                        message = "realtime_cancelled_while_recording"
                    )
                } else {
                    activeSpeechLane = null
                    clearLiveTranscript()
                }
            }

            ProcessingLane.QUICK_START -> {
                val state = _quickStartState.value
                if (state.isRecording && !state.isProcessing) {
                    activeSpeechLane = null
                    _quickStartState.value = state.copy(
                        isRecording = false,
                        isProcessing = false,
                        liveTranscript = "",
                        errorMessage = quickStartFailureMessage(DeviceSpeechFailureReason.CANCELLED),
                        guidanceMessage = null,
                        micInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                        processingPhase = OnboardingProcessingPhase.NONE
                    )
                    logRealtimeFailureWhileRecording(
                        lane = ProcessingLane.QUICK_START,
                        reason = DeviceSpeechFailureReason.CANCELLED,
                        message = "realtime_cancelled_while_recording"
                    )
                } else {
                    activeSpeechLane = null
                    clearLiveTranscript()
                }
            }

            null -> {
                activeSpeechLane = null
                clearLiveTranscript()
            }
        }
    }

    private fun handleCaptureLimitReached() {
        when (activeSpeechLane) {
            ProcessingLane.CONSULTATION -> {
                if (_consultationState.value.isRecording) {
                    finishConsultationRecording()
                }
            }

            ProcessingLane.PROFILE -> {
                if (_profileState.value.isRecording) {
                    finishProfileRecording()
                }
            }

            ProcessingLane.QUICK_START -> {
                if (_quickStartState.value.isRecording) {
                    finishQuickStartRecording()
                }
            }

            null -> Unit
        }
    }

    private fun updateLiveTranscript(text: String) {
        when (activeSpeechLane) {
            ProcessingLane.CONSULTATION -> {
                val state = _consultationState.value
                if (!state.isRecording && !state.isProcessing) return
                _consultationState.value = state.copy(liveTranscript = text)
            }

            ProcessingLane.PROFILE -> {
                val state = _profileState.value
                if (!state.isRecording && !state.isProcessing) return
                _profileState.value = state.copy(liveTranscript = text)
            }

            ProcessingLane.QUICK_START -> {
                val state = _quickStartState.value
                if (!state.isRecording && !state.isProcessing) return
                _quickStartState.value = state.copy(liveTranscript = text)
            }

            null -> Unit
        }
    }

    private fun clearLiveTranscript() {
        _consultationState.value = _consultationState.value.copy(liveTranscript = "")
        _profileState.value = _profileState.value.copy(liveTranscript = "")
        _quickStartState.value = _quickStartState.value.copy(liveTranscript = "")
    }

    private fun logProcessingStarted(
        lane: ProcessingLane,
        requestId: Long
    ) {
        Log.d(
            TAG,
            "processing_start host=${currentHost.logName} lane=${lane.logName} requestId=$requestId"
        )
    }

    private fun logRecognizerResolution(
        lane: ProcessingLane,
        requestId: Long,
        outcome: String,
        current: Boolean
    ) {
        Log.d(
            TAG,
            "recognizer_result host=${currentHost.logName} lane=${lane.logName} requestId=$requestId outcome=$outcome current=$current"
        )
    }

    private fun logRealtimeFailureWhileRecording(
        lane: ProcessingLane,
        reason: DeviceSpeechFailureReason,
        message: String
    ) {
        Log.w(
            TAG,
            "recording_failed host=${currentHost.logName} lane=${lane.logName} reason=${reason.name} message=$message profile=${lane.profileName} model=${lane.modelId}"
        )
    }

    private fun logProcessingCleared(
        lane: ProcessingLane,
        requestId: Long,
        outcome: String,
        transcriptOrigin: OnboardingTranscriptOrigin,
        generationOrigin: OnboardingGenerationOrigin
    ) {
        Log.d(
            TAG,
            "processing_cleared host=${currentHost.logName} lane=${lane.logName} requestId=$requestId outcome=$outcome transcriptOrigin=${transcriptOrigin.logName} generationOrigin=${generationOrigin.logName} profile=${lane.profileName} model=${lane.modelId}"
        )
    }

    private fun logProcessingFailed(
        lane: ProcessingLane,
        requestId: Long,
        cause: FailureCause,
        transcriptOrigin: OnboardingTranscriptOrigin?
    ) {
        Log.w(
            TAG,
            "processing_failed host=${currentHost.logName} lane=${lane.logName} requestId=$requestId cause=${cause.logName} transcriptOrigin=${transcriptOrigin.logNameOrNone} profile=${lane.profileName} model=${lane.modelId}"
        )
    }

    private companion object {
        private const val TAG = "OnboardingInteraction"
        private const val MIN_TRANSCRIPT_LENGTH = 4
        private const val DEVICE_RECOGNITION_TIMEOUT_MS = 5_000L
        private const val CONSULTATION_LLM_DEADLINE_MS = 2_500L
        private const val PROFILE_LLM_DEADLINE_MS = 3_500L
        private const val QUICK_START_LLM_DEADLINE_MS = 10_000L
        private const val MIC_PERMISSION_GRANTED_GUIDANCE = "麦克风已开启，请点击开始说话"
        private const val QUICK_START_MIC_PERMISSION_GRANTED_GUIDANCE = "麦克风已开启，请点击开始说话"

        private fun consultationFailureMessage(cause: FailureCause): String {
            return when (cause) {
                FailureCause.LLM_TIMEOUT,
                FailureCause.LLM_FAILURE,
                FailureCause.LLM_EXCEPTION ->
                    "当前 AI 咨询暂时没有返回，请再试一次。"

                FailureCause.TRANSCRIPT_TOO_SHORT ->
                    "这次语音太短了，请点击后多说一点。"

                FailureCause.RECOGNIZER_TIMEOUT,
                FailureCause.RECOGNIZER_EXCEPTION,
                FailureCause.RECOGNIZER_FAILURE,
                FailureCause.RECOGNIZER_CANCELLED ->
                    "这次语音识别没有完成，请再试一次。"
            }
        }

        private fun consultationFailureMessage(reason: DeviceSpeechFailureReason): String {
            return when (reason) {
                DeviceSpeechFailureReason.NO_MATCH ->
                    "这次语音太短了，请点击后多说一点。"

                DeviceSpeechFailureReason.UNAVAILABLE ->
                    "当前语音识别暂不可用，请稍后重试。"

                DeviceSpeechFailureReason.ERROR,
                DeviceSpeechFailureReason.CANCELLED ->
                    "这次语音识别没有完成，请再试一次。"
            }
        }

        private fun profileFailureMessage(cause: FailureCause): String {
            return when (cause) {
                FailureCause.LLM_TIMEOUT,
                FailureCause.LLM_FAILURE,
                FailureCause.LLM_EXCEPTION ->
                    "当前 AI 资料提取暂时没有返回，请再试一次。"

                FailureCause.TRANSCRIPT_TOO_SHORT ->
                    "这次语音太短了，请点击后多说一点。"

                FailureCause.RECOGNIZER_TIMEOUT,
                FailureCause.RECOGNIZER_EXCEPTION,
                FailureCause.RECOGNIZER_FAILURE,
                FailureCause.RECOGNIZER_CANCELLED ->
                    "这次语音识别没有完成，请再试一次。"
            }
        }

        private fun profileFailureMessage(reason: DeviceSpeechFailureReason): String {
            return when (reason) {
                DeviceSpeechFailureReason.NO_MATCH ->
                    "这次语音太短了，请点击后多说一点。"

                DeviceSpeechFailureReason.UNAVAILABLE ->
                    "当前语音识别暂不可用，请稍后重试。"

                DeviceSpeechFailureReason.ERROR,
                DeviceSpeechFailureReason.CANCELLED ->
                    "这次语音识别没有完成，请再试一次。"
            }
        }

        private fun quickStartFailureMessage(cause: FailureCause): String {
            return when (cause) {
                FailureCause.LLM_TIMEOUT,
                FailureCause.LLM_FAILURE,
                FailureCause.LLM_EXCEPTION ->
                    "当前日程整理暂时没有返回，请再试一次。"

                FailureCause.TRANSCRIPT_TOO_SHORT ->
                    "这次语音太短了，请点击后多说一点。"

                FailureCause.RECOGNIZER_TIMEOUT,
                FailureCause.RECOGNIZER_EXCEPTION,
                FailureCause.RECOGNIZER_FAILURE,
                FailureCause.RECOGNIZER_CANCELLED ->
                    "这次语音识别没有完成，请再试一次。"
            }
        }

        private fun quickStartFailureMessage(reason: DeviceSpeechFailureReason): String {
            return when (reason) {
                DeviceSpeechFailureReason.NO_MATCH ->
                    "这次语音太短了，请点击后多说一点。"

                DeviceSpeechFailureReason.UNAVAILABLE ->
                    "当前语音识别暂不可用，请稍后重试。"

                DeviceSpeechFailureReason.ERROR,
                DeviceSpeechFailureReason.CANCELLED ->
                    "这次语音识别没有完成，请再试一次。"
            }
        }
    }

    private val ProcessingLane.logName: String
        get() = when (this) {
            ProcessingLane.CONSULTATION -> "consultation"
            ProcessingLane.PROFILE -> "profile"
            ProcessingLane.QUICK_START -> "quick_start"
        }

    private val ProcessingLane.profileName: String
        get() = when (this) {
            ProcessingLane.CONSULTATION -> ONBOARDING_CONSULTATION_PROFILE_NAME
            ProcessingLane.PROFILE -> ONBOARDING_PROFILE_EXTRACTION_PROFILE_NAME
            ProcessingLane.QUICK_START -> "scheduler_quick_start"
        }

    private val ProcessingLane.modelId: String
        get() = when (this) {
            ProcessingLane.CONSULTATION -> ModelRegistry.ONBOARDING_CONSULTATION.modelId
            ProcessingLane.PROFILE -> ModelRegistry.ONBOARDING_PROFILE_EXTRACTION.modelId
            ProcessingLane.QUICK_START -> "scheduler_path_a_reuse"
        }

    private val OnboardingTranscriptOrigin.logName: String
        get() = when (this) {
            OnboardingTranscriptOrigin.DEVICE_SPEECH -> "device_speech"
        }

    private val OnboardingTranscriptOrigin?.logNameOrNone: String
        get() = this?.logName ?: "none"

    private val OnboardingGenerationOrigin.logName: String
        get() = when (this) {
            OnboardingGenerationOrigin.LLM -> "llm"
            OnboardingGenerationOrigin.SCHEDULER_PATH_A -> "scheduler_path_a"
        }

    private val FailureCause.logName: String
        get() = when (this) {
            FailureCause.RECOGNIZER_TIMEOUT -> "recognizer_timeout"
            FailureCause.RECOGNIZER_EXCEPTION -> "recognizer_exception"
            FailureCause.RECOGNIZER_FAILURE -> "recognizer_failure"
            FailureCause.RECOGNIZER_CANCELLED -> "recognizer_cancelled"
            FailureCause.TRANSCRIPT_TOO_SHORT -> "transcript_too_short"
            FailureCause.LLM_TIMEOUT -> "llm_timeout"
            FailureCause.LLM_FAILURE -> "llm_failure"
            FailureCause.LLM_EXCEPTION -> "llm_exception"
        }

    private val OnboardingHost?.logName: String
        get() = when (this) {
            OnboardingHost.FULL_APP -> "full_app"
            OnboardingHost.SIM_CONNECTIVITY -> "sim_connectivity"
            null -> "unbound"
        }
}
