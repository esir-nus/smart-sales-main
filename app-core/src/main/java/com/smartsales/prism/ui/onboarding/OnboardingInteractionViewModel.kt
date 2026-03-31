package com.smartsales.prism.ui.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.prism.data.audio.DeviceSpeechFailureReason
import com.smartsales.prism.data.audio.DeviceSpeechRecognitionEvent
import com.smartsales.prism.data.audio.DeviceSpeechMode
import com.smartsales.prism.data.audio.DeviceSpeechRecognitionResult
import com.smartsales.prism.data.audio.DeviceSpeechRecognizer
import com.smartsales.prism.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
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
    private val runtimePolicy: OnboardingInteractionRuntimePolicy
) : ViewModel() {

    private var consultationRequestId = 0L
    private var profileRequestId = 0L
    private var activeSpeechLane: ProcessingLane? = null
    private var currentHost: OnboardingHost? = null

    private val _consultationState = MutableStateFlow(OnboardingConsultationUiState())
    val consultationState: StateFlow<OnboardingConsultationUiState> = _consultationState.asStateFlow()

    private val _profileState = MutableStateFlow(OnboardingProfileUiState())
    val profileState: StateFlow<OnboardingProfileUiState> = _profileState.asStateFlow()

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
    }

    fun bindHost(host: OnboardingHost) {
        currentHost = host
    }

    fun startConsultationRecording(): Boolean {
        return startConsultationRecording(OnboardingMicInteractionMode.HOLD_TO_SEND)
    }

    fun onConsultationMicPermissionRequested() {
        val state = _consultationState.value
        if (state.isProcessing || state.isCompleted || state.isRecording || speechRecognizer.isListening()) return
        _consultationState.value = state.copy(
            awaitingMicPermission = true,
            micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
            guidanceMessage = null,
            errorMessage = null
        )
    }

    fun onConsultationMicPermissionResult(granted: Boolean) {
        val state = _consultationState.value
        if (!granted) {
            _consultationState.value = state.copy(
                awaitingMicPermission = false,
                micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
                guidanceMessage = null,
                errorMessage = "无法录音：未授予麦克风权限"
            )
            return
        }
        if (state.awaitingMicPermission) {
            _consultationState.value = state.copy(
                awaitingMicPermission = false,
                micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
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
                micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
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

                is TranscriptResolution.Fallback -> {
                    handleConsultationFailure(
                        requestId = requestId,
                        delayMillis = resolution.delayMillis,
                        cause = resolution.cause
                    )
                }

                null -> Unit
            }
        }
    }

    fun startProfileRecording(): Boolean {
        return startProfileRecording(OnboardingMicInteractionMode.HOLD_TO_SEND)
    }

    fun onProfileMicPermissionRequested() {
        val state = _profileState.value
        if (state.isProcessing || state.hasExtractionResult || state.isRecording || speechRecognizer.isListening()) return
        _profileState.value = state.copy(
            awaitingMicPermission = true,
            micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
            guidanceMessage = null,
            errorMessage = null
        )
    }

    fun onProfileMicPermissionResult(granted: Boolean) {
        val state = _profileState.value
        if (!granted) {
            _profileState.value = state.copy(
                awaitingMicPermission = false,
                micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
                guidanceMessage = null,
                errorMessage = "无法录音：未授予麦克风权限"
            )
            return
        }
        if (state.awaitingMicPermission) {
            _profileState.value = state.copy(
                awaitingMicPermission = false,
                micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
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
                micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
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

                is TranscriptResolution.Fallback -> {
                    handleProfileFailure(
                        requestId = requestId,
                        delayMillis = resolution.delayMillis,
                        cause = resolution.cause
                    )
                }

                null -> Unit
            }
        }
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
            micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
            processingPhase = OnboardingProcessingPhase.NONE
        )
        _profileState.value = _profileState.value.copy(
            isRecording = false,
            isProcessing = false,
            liveTranscript = "",
            awaitingMicPermission = false,
            guidanceMessage = null,
            micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
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
                delayMillis = FALLBACK_DWELL_MS,
                cause = FallbackCause.TRANSCRIPT_TOO_SHORT
            )
            return
        }
        updateConsultationStateIfCurrent(requestId) {
            it.copy(processingPhase = OnboardingProcessingPhase.BUILDING_CONSULTATION_REPLY)
        }
        val round = _consultationState.value.completedRounds + 1
        var fallbackCause = FallbackCause.LLM_FAILURE
        val result = try {
            withTimeout(CONSULTATION_LLM_DEADLINE_MS) {
                interactionService.generateConsultationReply(normalized, round)
            }
        } catch (_: TimeoutCancellationException) {
            fallbackCause = FallbackCause.LLM_TIMEOUT
            OnboardingConsultationServiceResult.Failure("当前无法继续这段咨询，请稍后重试。")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            fallbackCause = FallbackCause.LLM_EXCEPTION
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
                        micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
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
                    delayMillis = 0L,
                    transcript = normalized,
                    transcriptOrigin = transcriptOrigin,
                    cause = fallbackCause
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
                delayMillis = FALLBACK_DWELL_MS,
                cause = FallbackCause.TRANSCRIPT_TOO_SHORT
            )
            return
        }
        updateProfileStateIfCurrent(requestId) {
            it.copy(processingPhase = OnboardingProcessingPhase.BUILDING_PROFILE_RESULT)
        }
        var fallbackCause = FallbackCause.LLM_FAILURE
        val result = try {
            withTimeout(PROFILE_LLM_DEADLINE_MS) {
                interactionService.extractProfile(normalized)
            }
        } catch (_: TimeoutCancellationException) {
            fallbackCause = FallbackCause.LLM_TIMEOUT
            OnboardingProfileExtractionServiceResult.Failure("当前无法提取资料，请稍后重试。")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            fallbackCause = FallbackCause.LLM_EXCEPTION
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
                        micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
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
                    delayMillis = 0L,
                    transcript = normalized,
                    transcriptOrigin = transcriptOrigin,
                    cause = fallbackCause
                )
            }
        }
    }

    private suspend fun handleConsultationFailure(
        requestId: Long,
        delayMillis: Long,
        transcript: String? = null,
        transcriptOrigin: OnboardingTranscriptOrigin = OnboardingTranscriptOrigin.DETERMINISTIC_FALLBACK,
        cause: FallbackCause
    ) {
        if (runtimePolicy.allowDeterministicFallback) {
            runConsultationFallback(
                requestId = requestId,
                delayMillis = delayMillis,
                transcript = transcript,
                transcriptOrigin = transcriptOrigin,
                cause = cause
            )
        } else {
            surfaceConsultationFailure(
                requestId = requestId,
                transcript = transcript,
                transcriptOrigin = transcript.takeIf { !it.isNullOrBlank() }?.let { transcriptOrigin },
                cause = cause
            )
        }
    }

    private suspend fun handleProfileFailure(
        requestId: Long,
        delayMillis: Long,
        transcript: String? = null,
        transcriptOrigin: OnboardingTranscriptOrigin = OnboardingTranscriptOrigin.DETERMINISTIC_FALLBACK,
        cause: FallbackCause
    ) {
        if (runtimePolicy.allowDeterministicFallback) {
            runProfileFallback(
                requestId = requestId,
                delayMillis = delayMillis,
                transcript = transcript,
                transcriptOrigin = transcriptOrigin,
                cause = cause
            )
        } else {
            surfaceProfileFailure(
                requestId = requestId,
                transcript = transcript,
                transcriptOrigin = transcript.takeIf { !it.isNullOrBlank() }?.let { transcriptOrigin },
                cause = cause
            )
        }
    }

    private suspend fun runConsultationFallback(
        requestId: Long,
        delayMillis: Long,
        transcript: String? = null,
        transcriptOrigin: OnboardingTranscriptOrigin = OnboardingTranscriptOrigin.DETERMINISTIC_FALLBACK,
        cause: FallbackCause
    ) {
        activeSpeechLane = null
        logFallbackEntered(
            lane = ProcessingLane.CONSULTATION,
            requestId = requestId,
            delayMillis = delayMillis,
            cause = cause
        )
        updateConsultationStateIfCurrent(requestId) {
            it.copy(
                isProcessing = true,
                liveTranscript = "",
                processingPhase = OnboardingProcessingPhase.DETERMINISTIC_FALLBACK,
                guidanceMessage = null,
                errorMessage = null
            )
        }
        if (delayMillis > 0L) {
            delay(delayMillis)
        }
        if (!isCurrentConsultationRequest(requestId)) return
        val round = _consultationState.value.completedRounds + 1
        val resolvedTranscript = transcript?.trim()?.takeIf { it.isNotBlank() } ?: consultationFallbackTranscript(round)
        val resolvedTranscriptOrigin = if (transcript.isNullOrBlank()) {
            OnboardingTranscriptOrigin.DETERMINISTIC_FALLBACK
        } else {
            transcriptOrigin
        }
        val reply = buildDeterministicConsultationReply(resolvedTranscript, round)
            ?: consultationFallbackReply(round)
        updateConsultationStateIfCurrent(requestId) { current ->
            current.copy(
                isProcessing = false,
                liveTranscript = "",
                messages = current.messages + listOf(
                    OnboardingInteractionMessage(OnboardingMessageRole.USER, resolvedTranscript),
                    OnboardingInteractionMessage(OnboardingMessageRole.AI, reply)
                ),
                completedRounds = round,
                errorMessage = null,
                guidanceMessage = null,
                micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
                processingPhase = OnboardingProcessingPhase.NONE,
                lastTranscriptOrigin = resolvedTranscriptOrigin,
                lastGenerationOrigin = OnboardingGenerationOrigin.DETERMINISTIC_FALLBACK
            )
        }
        logProcessingCleared(
            lane = ProcessingLane.CONSULTATION,
            requestId = requestId,
            outcome = "deterministic_fallback",
            transcriptOrigin = resolvedTranscriptOrigin,
            generationOrigin = OnboardingGenerationOrigin.DETERMINISTIC_FALLBACK
        )
    }

    private fun surfaceConsultationFailure(
        requestId: Long,
        transcript: String?,
        transcriptOrigin: OnboardingTranscriptOrigin?,
        cause: FallbackCause
    ) {
        activeSpeechLane = null
        logFallbackSuppressed(
            lane = ProcessingLane.CONSULTATION,
            requestId = requestId,
            cause = cause,
            transcriptAvailable = !transcript.isNullOrBlank()
        )
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
                micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
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

    private suspend fun runProfileFallback(
        requestId: Long,
        delayMillis: Long,
        transcript: String? = null,
        transcriptOrigin: OnboardingTranscriptOrigin = OnboardingTranscriptOrigin.DETERMINISTIC_FALLBACK,
        cause: FallbackCause
    ) {
        activeSpeechLane = null
        logFallbackEntered(
            lane = ProcessingLane.PROFILE,
            requestId = requestId,
            delayMillis = delayMillis,
            cause = cause
        )
        updateProfileStateIfCurrent(requestId) {
            it.copy(
                isProcessing = true,
                liveTranscript = "",
                processingPhase = OnboardingProcessingPhase.DETERMINISTIC_FALLBACK,
                guidanceMessage = null,
                errorMessage = null
            )
        }
        if (delayMillis > 0L) {
            delay(delayMillis)
        }
        if (!isCurrentProfileRequest(requestId)) return
        val resolvedTranscript = transcript?.trim()?.takeIf { it.isNotBlank() } ?: PROFILE_FALLBACK_TRANSCRIPT
        val resolvedTranscriptOrigin = if (transcript.isNullOrBlank()) {
            OnboardingTranscriptOrigin.DETERMINISTIC_FALLBACK
        } else {
            transcriptOrigin
        }
        val deterministic = buildDeterministicProfileResult(resolvedTranscript)
        updateProfileStateIfCurrent(requestId) {
            it.copy(
                isProcessing = false,
                liveTranscript = "",
                transcript = resolvedTranscript,
                acknowledgement = deterministic?.acknowledgement ?: PROFILE_FALLBACK_ACKNOWLEDGEMENT,
                draft = deterministic?.draft ?: PROFILE_FALLBACK_DRAFT,
                errorMessage = null,
                micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
                processingPhase = OnboardingProcessingPhase.NONE,
                transcriptOrigin = resolvedTranscriptOrigin,
                generationOrigin = OnboardingGenerationOrigin.DETERMINISTIC_FALLBACK
            )
        }
        logProcessingCleared(
            lane = ProcessingLane.PROFILE,
            requestId = requestId,
            outcome = "deterministic_fallback",
            transcriptOrigin = resolvedTranscriptOrigin,
            generationOrigin = OnboardingGenerationOrigin.DETERMINISTIC_FALLBACK
        )
    }

    private fun surfaceProfileFailure(
        requestId: Long,
        transcript: String?,
        transcriptOrigin: OnboardingTranscriptOrigin?,
        cause: FallbackCause
    ) {
        activeSpeechLane = null
        logFallbackSuppressed(
            lane = ProcessingLane.PROFILE,
            requestId = requestId,
            cause = cause,
            transcriptAvailable = !transcript.isNullOrBlank()
        )
        updateProfileStateIfCurrent(requestId) {
            it.copy(
                isProcessing = false,
                liveTranscript = "",
                transcript = transcript?.trim().orEmpty(),
                acknowledgement = "",
                draft = null,
                errorMessage = profileFailureMessage(cause),
                guidanceMessage = null,
                micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
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

    private suspend fun resolveTranscript(
        requestId: Long,
        lane: ProcessingLane
    ): TranscriptResolution? {
        val result = try {
            withTimeout(DEVICE_RECOGNITION_TIMEOUT_MS) {
                speechRecognizer.finishListening()
            }
        } catch (_: TimeoutCancellationException) {
            logRecognizerResolution(
                lane = lane,
                requestId = requestId,
                outcome = "timeout",
                current = isCurrentRequest(lane, requestId)
            )
            return currentResolutionOrNull(
                requestId,
                lane,
                TranscriptResolution.Fallback(
                    delayMillis = 0L,
                    cause = FallbackCause.RECOGNIZER_TIMEOUT
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            logRecognizerResolution(
                lane = lane,
                requestId = requestId,
                outcome = "exception",
                current = isCurrentRequest(lane, requestId)
            )
            return currentResolutionOrNull(
                requestId,
                lane,
                TranscriptResolution.Fallback(
                    delayMillis = FALLBACK_DWELL_MS,
                    cause = FallbackCause.RECOGNIZER_EXCEPTION
                )
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
                        TranscriptResolution.Fallback(
                            delayMillis = FALLBACK_DWELL_MS,
                            cause = FallbackCause.TRANSCRIPT_TOO_SHORT
                        )
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
                        resolution = TranscriptResolution.Fallback(
                            delayMillis = 0L,
                            cause = FallbackCause.RECOGNIZER_CANCELLED
                        )
                    )
                } else {
                    currentResolutionOrNull(
                        requestId = requestId,
                        lane = lane,
                        resolution = TranscriptResolution.Fallback(
                            delayMillis = FALLBACK_DWELL_MS,
                            cause = FallbackCause.RECOGNIZER_FAILURE
                        )
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
            micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
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
            micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
            processingPhase = OnboardingProcessingPhase.RECOGNIZING
        )
        return profileRequestId
    }

    private fun invalidateProcessingRequests() {
        consultationRequestId += 1
        profileRequestId += 1
    }

    private fun isCurrentConsultationRequest(requestId: Long): Boolean = consultationRequestId == requestId

    private fun isCurrentProfileRequest(requestId: Long): Boolean = profileRequestId == requestId

    private fun isCurrentRequest(
        lane: ProcessingLane,
        requestId: Long
    ): Boolean {
        return when (lane) {
            ProcessingLane.CONSULTATION -> isCurrentConsultationRequest(requestId)
            ProcessingLane.PROFILE -> isCurrentProfileRequest(requestId)
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

    private sealed interface TranscriptResolution {
        data class Transcript(
            val text: String,
            val origin: OnboardingTranscriptOrigin
        ) : TranscriptResolution

        data class Fallback(
            val delayMillis: Long,
            val cause: FallbackCause
        ) : TranscriptResolution
    }

    private enum class ProcessingLane {
        CONSULTATION,
        PROFILE
    }

    private enum class FallbackCause {
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
            is DeviceSpeechRecognitionEvent.Failure -> Unit
            DeviceSpeechRecognitionEvent.Cancelled -> {
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

            null -> Unit
        }
    }

    private fun clearLiveTranscript() {
        _consultationState.value = _consultationState.value.copy(liveTranscript = "")
        _profileState.value = _profileState.value.copy(liveTranscript = "")
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

    private fun logFallbackEntered(
        lane: ProcessingLane,
        requestId: Long,
        delayMillis: Long,
        cause: FallbackCause
    ) {
        Log.d(
            TAG,
            "fallback_entered host=${currentHost.logName} lane=${lane.logName} requestId=$requestId cause=${cause.logName} delayMs=$delayMillis profile=${lane.profileName} model=${lane.modelId}"
        )
    }

    private fun logFallbackSuppressed(
        lane: ProcessingLane,
        requestId: Long,
        cause: FallbackCause,
        transcriptAvailable: Boolean
    ) {
        Log.w(
            TAG,
            "fallback_suppressed host=${currentHost.logName} lane=${lane.logName} requestId=$requestId cause=${cause.logName} transcriptAvailable=$transcriptAvailable profile=${lane.profileName} model=${lane.modelId}"
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
        cause: FallbackCause,
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
        private const val DEVICE_RECOGNITION_TIMEOUT_MS = 1_200L
        private const val CONSULTATION_LLM_DEADLINE_MS = 2_500L
        private const val PROFILE_LLM_DEADLINE_MS = 3_500L
        private const val FALLBACK_DWELL_MS = 1_200L
        private const val MIC_PERMISSION_GRANTED_GUIDANCE = "麦克风已开启，请重新按住说话"

        private const val PROFILE_FALLBACK_TRANSCRIPT =
            "我是李经理，在科技行业做销售经理三年了，平时主要用微信和电话跟客户沟通。"
        private const val PROFILE_FALLBACK_ACKNOWLEDGEMENT =
            "谢谢您的分享，我先为您整理了一份基础档案。"
        private val PROFILE_FALLBACK_DRAFT = OnboardingProfileDraft(
            displayName = "李经理",
            role = "销售经理",
            industry = "科技",
            experienceYears = "3年",
            communicationPlatform = "微信"
        )

        private fun consultationFallbackTranscript(round: Int): String {
            return when (round) {
                1 -> "我想试试怎么更自然地开始和客户沟通。"
                else -> "如果客户一直比较冷淡，我接下来该怎么推进？"
            }
        }

        private fun consultationFallbackReply(round: Int): String {
            return when (round) {
                1 -> "先别急着讲方案，先用一个开放问题把客户最在意的事情聊出来。"
                else -> "先确认对方当前优先级，再给一个很小的下一步，让对话继续往前走。"
            }
        }

        private fun consultationFailureMessage(cause: FallbackCause): String {
            return when (cause) {
                FallbackCause.LLM_TIMEOUT,
                FallbackCause.LLM_FAILURE,
                FallbackCause.LLM_EXCEPTION ->
                    "当前 AI 咨询暂时没有返回，请再试一次。"

                FallbackCause.TRANSCRIPT_TOO_SHORT ->
                    "这次语音太短了，请再按住多说一点。"

                FallbackCause.RECOGNIZER_TIMEOUT,
                FallbackCause.RECOGNIZER_EXCEPTION,
                FallbackCause.RECOGNIZER_FAILURE,
                FallbackCause.RECOGNIZER_CANCELLED ->
                    "这次语音识别没有完成，请再试一次。"
            }
        }

        private fun profileFailureMessage(cause: FallbackCause): String {
            return when (cause) {
                FallbackCause.LLM_TIMEOUT,
                FallbackCause.LLM_FAILURE,
                FallbackCause.LLM_EXCEPTION ->
                    "当前 AI 资料提取暂时没有返回，请再试一次。"

                FallbackCause.TRANSCRIPT_TOO_SHORT ->
                    "这次语音太短了，请再按住多说一点。"

                FallbackCause.RECOGNIZER_TIMEOUT,
                FallbackCause.RECOGNIZER_EXCEPTION,
                FallbackCause.RECOGNIZER_FAILURE,
                FallbackCause.RECOGNIZER_CANCELLED ->
                    "这次语音识别没有完成，请再试一次。"
            }
        }
    }

    private val ProcessingLane.logName: String
        get() = when (this) {
            ProcessingLane.CONSULTATION -> "consultation"
            ProcessingLane.PROFILE -> "profile"
        }

    private val ProcessingLane.profileName: String
        get() = when (this) {
            ProcessingLane.CONSULTATION -> ONBOARDING_CONSULTATION_PROFILE_NAME
            ProcessingLane.PROFILE -> ONBOARDING_PROFILE_EXTRACTION_PROFILE_NAME
        }

    private val ProcessingLane.modelId: String
        get() = when (this) {
            ProcessingLane.CONSULTATION -> ModelRegistry.ONBOARDING_CONSULTATION.modelId
            ProcessingLane.PROFILE -> ModelRegistry.ONBOARDING_PROFILE_EXTRACTION.modelId
        }

    private val OnboardingTranscriptOrigin.logName: String
        get() = when (this) {
            OnboardingTranscriptOrigin.DEVICE_SPEECH -> "device_speech"
            OnboardingTranscriptOrigin.DETERMINISTIC_FALLBACK -> "deterministic_fallback"
        }

    private val OnboardingTranscriptOrigin?.logNameOrNone: String
        get() = this?.logName ?: "none"

    private val OnboardingGenerationOrigin.logName: String
        get() = when (this) {
            OnboardingGenerationOrigin.LLM -> "llm"
            OnboardingGenerationOrigin.DETERMINISTIC_FALLBACK -> "deterministic_fallback"
        }

    private val FallbackCause.logName: String
        get() = when (this) {
            FallbackCause.RECOGNIZER_TIMEOUT -> "recognizer_timeout"
            FallbackCause.RECOGNIZER_EXCEPTION -> "recognizer_exception"
            FallbackCause.RECOGNIZER_FAILURE -> "recognizer_failure"
            FallbackCause.RECOGNIZER_CANCELLED -> "recognizer_cancelled"
            FallbackCause.TRANSCRIPT_TOO_SHORT -> "transcript_too_short"
            FallbackCause.LLM_TIMEOUT -> "llm_timeout"
            FallbackCause.LLM_FAILURE -> "llm_failure"
            FallbackCause.LLM_EXCEPTION -> "llm_exception"
        }

    private val OnboardingHost?.logName: String
        get() = when (this) {
            OnboardingHost.FULL_APP -> "full_app"
            OnboardingHost.SIM_CONNECTIVITY -> "sim_connectivity"
            null -> "unbound"
        }
}
