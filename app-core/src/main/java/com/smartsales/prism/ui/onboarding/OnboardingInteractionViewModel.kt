package com.smartsales.prism.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private var consultationRequestId = 0L
    private var profileRequestId = 0L
    private var activeSpeechLane: ProcessingLane? = null

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
                        origin = resolution.origin
                    )
                }

                is TranscriptResolution.Fallback -> {
                    runConsultationFallback(requestId, resolution.delayMillis)
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
                        origin = resolution.origin
                    )
                }

                is TranscriptResolution.Fallback -> {
                    runProfileFallback(requestId, resolution.delayMillis)
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
        origin: OnboardingResultOrigin
    ) {
        val normalized = transcript.trim()
        if (normalized.length < MIN_TRANSCRIPT_LENGTH) {
            runConsultationFallback(requestId, FALLBACK_DWELL_MS)
            return
        }
        updateConsultationStateIfCurrent(requestId) {
            it.copy(processingPhase = OnboardingProcessingPhase.BUILDING_CONSULTATION_REPLY)
        }
        val round = _consultationState.value.completedRounds + 1
        when (val result = interactionService.generateConsultationReply(normalized, round)) {
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
                        lastResultOrigin = origin
                    )
                }
            }

            is OnboardingConsultationServiceResult.Failure -> {
                runConsultationFallback(requestId, 0L)
            }
        }
    }

    private suspend fun processProfileTranscript(
        requestId: Long,
        transcript: String,
        origin: OnboardingResultOrigin
    ) {
        val normalized = transcript.trim()
        if (normalized.length < MIN_TRANSCRIPT_LENGTH) {
            runProfileFallback(requestId, FALLBACK_DWELL_MS)
            return
        }
        updateProfileStateIfCurrent(requestId) {
            it.copy(processingPhase = OnboardingProcessingPhase.BUILDING_PROFILE_RESULT)
        }
        when (val result = interactionService.extractProfile(normalized)) {
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
                        draftOrigin = origin
                    )
                }
            }

            is OnboardingProfileExtractionServiceResult.Failure -> {
                runProfileFallback(requestId, 0L)
            }
        }
    }

    private suspend fun runConsultationFallback(requestId: Long, delayMillis: Long) {
        activeSpeechLane = null
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
        val transcript = consultationFallbackTranscript(round)
        val reply = when (val result = interactionService.generateConsultationReply(transcript, round)) {
            is OnboardingConsultationServiceResult.Success -> result.reply
            is OnboardingConsultationServiceResult.Failure -> consultationFallbackReply(round)
        }
        updateConsultationStateIfCurrent(requestId) { current ->
            current.copy(
                isProcessing = false,
                liveTranscript = "",
                messages = current.messages + listOf(
                    OnboardingInteractionMessage(OnboardingMessageRole.USER, transcript),
                    OnboardingInteractionMessage(OnboardingMessageRole.AI, reply)
                ),
                completedRounds = round,
                errorMessage = null,
                guidanceMessage = null,
                micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
                processingPhase = OnboardingProcessingPhase.NONE,
                lastResultOrigin = OnboardingResultOrigin.DETERMINISTIC_FALLBACK
            )
        }
    }

    private suspend fun runProfileFallback(requestId: Long, delayMillis: Long) {
        activeSpeechLane = null
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
        val transcript = PROFILE_FALLBACK_TRANSCRIPT
        updateProfileStateIfCurrent(requestId) {
            it.copy(
                isProcessing = false,
                liveTranscript = "",
                transcript = transcript,
                acknowledgement = PROFILE_FALLBACK_ACKNOWLEDGEMENT,
                draft = PROFILE_FALLBACK_DRAFT,
                errorMessage = null,
                micInteractionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
                processingPhase = OnboardingProcessingPhase.NONE,
                draftOrigin = OnboardingResultOrigin.DETERMINISTIC_FALLBACK
            )
        }
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
            return currentResolutionOrNull(requestId, lane, TranscriptResolution.Fallback(delayMillis = 0L))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return currentResolutionOrNull(requestId, lane, TranscriptResolution.Fallback(delayMillis = FALLBACK_DWELL_MS))
        }

        return when (result) {
            is DeviceSpeechRecognitionResult.Success -> {
                val normalized = result.text.trim()
                if (normalized.length < MIN_TRANSCRIPT_LENGTH) {
                    currentResolutionOrNull(requestId, lane, TranscriptResolution.Fallback(delayMillis = FALLBACK_DWELL_MS))
                } else {
                    currentResolutionOrNull(
                        requestId = requestId,
                        lane = lane,
                        resolution = TranscriptResolution.Transcript(
                            text = normalized,
                            origin = OnboardingResultOrigin.DEVICE_SPEECH
                        )
                    )
                }
            }

            is DeviceSpeechRecognitionResult.Failure -> {
                if (result.reason == DeviceSpeechFailureReason.CANCELLED) {
                    null
                } else {
                    currentResolutionOrNull(
                        requestId = requestId,
                        lane = lane,
                        resolution = TranscriptResolution.Fallback(delayMillis = FALLBACK_DWELL_MS)
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
        return when (lane) {
            ProcessingLane.CONSULTATION -> if (isCurrentConsultationRequest(requestId)) resolution else null
            ProcessingLane.PROFILE -> if (isCurrentProfileRequest(requestId)) resolution else null
        }
    }

    private fun beginConsultationProcessing(
        state: OnboardingConsultationUiState
    ): Long {
        consultationRequestId += 1
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
            val origin: OnboardingResultOrigin
        ) : TranscriptResolution

        data class Fallback(
            val delayMillis: Long
        ) : TranscriptResolution
    }

    private enum class ProcessingLane {
        CONSULTATION,
        PROFILE
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

    private companion object {
        private const val MIN_TRANSCRIPT_LENGTH = 4
        private const val DEVICE_RECOGNITION_TIMEOUT_MS = 1_200L
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
    }
}
