package com.smartsales.prism.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import com.smartsales.prism.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * onboarding 互动状态管理器。
 */
@HiltViewModel
class OnboardingInteractionViewModel @Inject constructor(
    private val audioCapture: OnboardingAudioCapture,
    private val asrService: AsrService,
    private val interactionService: OnboardingInteractionService,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _consultationState = MutableStateFlow(OnboardingConsultationUiState())
    val consultationState: StateFlow<OnboardingConsultationUiState> = _consultationState.asStateFlow()

    private val _profileState = MutableStateFlow(OnboardingProfileUiState())
    val profileState: StateFlow<OnboardingProfileUiState> = _profileState.asStateFlow()

    private val _effects = MutableSharedFlow<OnboardingInteractionEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<OnboardingInteractionEffect> = _effects.asSharedFlow()

    fun resetInteractionState() {
        cancelActiveRecording()
        _consultationState.value = OnboardingConsultationUiState()
        _profileState.value = OnboardingProfileUiState()
    }

    fun startConsultationRecording(): Boolean {
        val state = _consultationState.value
        if (state.isProcessing || state.isCompleted || audioCapture.isRecording()) return false
        return runCatching {
            audioCapture.startRecording()
            _consultationState.value = state.copy(
                hasStartedInteracting = true,
                isRecording = true,
                errorMessage = null
            )
            true
        }.getOrElse {
            _consultationState.value = state.copy(
                errorMessage = "当前无法开始录音，请重试。"
            )
            false
        }
    }

    fun finishConsultationRecording() {
        val state = _consultationState.value
        if (!state.isRecording) return
        _consultationState.value = state.copy(
            isRecording = false,
            isProcessing = true,
            errorMessage = null
        )
        viewModelScope.launch {
            val file = runCatching { audioCapture.stopRecording() }.getOrElse {
                _consultationState.value = _consultationState.value.copy(
                    isProcessing = false,
                    errorMessage = "当前无法完成录音，请重试。"
                )
                return@launch
            }
            processConsultationRecording(file)
        }
    }

    fun onConsultationMicPermissionDenied() {
        _consultationState.value = _consultationState.value.copy(
            errorMessage = "无法录音：未授予麦克风权限"
        )
    }

    fun startProfileRecording(): Boolean {
        val state = _profileState.value
        if (state.isProcessing || state.hasExtractionResult || audioCapture.isRecording()) return false
        return runCatching {
            audioCapture.startRecording()
            _profileState.value = state.copy(
                hasStartedInteracting = true,
                isRecording = true,
                errorMessage = null
            )
            true
        }.getOrElse {
            _profileState.value = state.copy(
                errorMessage = "当前无法开始录音，请重试。"
            )
            false
        }
    }

    fun finishProfileRecording() {
        val state = _profileState.value
        if (!state.isRecording) return
        _profileState.value = state.copy(
            isRecording = false,
            isProcessing = true,
            errorMessage = null
        )
        viewModelScope.launch {
            val file = runCatching { audioCapture.stopRecording() }.getOrElse {
                _profileState.value = _profileState.value.copy(
                    isProcessing = false,
                    errorMessage = "当前无法完成录音，请重试。"
                )
                return@launch
            }
            processProfileRecording(file)
        }
    }

    fun onProfileMicPermissionDenied() {
        _profileState.value = _profileState.value.copy(
            errorMessage = "无法录音：未授予麦克风权限"
        )
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
        if (audioCapture.isRecording()) {
            runCatching { audioCapture.cancelRecording() }
        }
        _consultationState.value = _consultationState.value.copy(isRecording = false, isProcessing = false)
        _profileState.value = _profileState.value.copy(isRecording = false, isProcessing = false)
    }

    private suspend fun processConsultationRecording(file: File) {
        val transcript = transcribe(file) ?: return
        if (transcript.length < MIN_TRANSCRIPT_LENGTH) {
            _consultationState.value = _consultationState.value.copy(
                isProcessing = false,
                errorMessage = "录音太短，请多说一点"
            )
            return
        }
        val round = _consultationState.value.completedRounds + 1
        when (val result = interactionService.generateConsultationReply(transcript, round)) {
            is OnboardingConsultationServiceResult.Success -> {
                val nextMessages = _consultationState.value.messages + listOf(
                    OnboardingInteractionMessage(OnboardingMessageRole.USER, transcript),
                    OnboardingInteractionMessage(OnboardingMessageRole.AI, result.reply)
                )
                _consultationState.value = _consultationState.value.copy(
                    isProcessing = false,
                    messages = nextMessages,
                    completedRounds = round,
                    errorMessage = null
                )
            }

            is OnboardingConsultationServiceResult.Failure -> {
                _consultationState.value = _consultationState.value.copy(
                    isProcessing = false,
                    errorMessage = result.message
                )
            }
        }
    }

    private suspend fun processProfileRecording(file: File) {
        val transcript = transcribe(file) ?: return
        if (transcript.length < MIN_TRANSCRIPT_LENGTH) {
            _profileState.value = _profileState.value.copy(
                isProcessing = false,
                errorMessage = "录音太短，请多说一点"
            )
            return
        }
        when (val result = interactionService.extractProfile(transcript)) {
            is OnboardingProfileExtractionServiceResult.Success -> {
                _profileState.value = _profileState.value.copy(
                    isProcessing = false,
                    transcript = transcript,
                    acknowledgement = result.acknowledgement,
                    draft = result.draft,
                    errorMessage = null
                )
            }

            is OnboardingProfileExtractionServiceResult.Failure -> {
                _profileState.value = _profileState.value.copy(
                    isProcessing = false,
                    errorMessage = result.message
                )
            }
        }
    }

    private suspend fun transcribe(file: File): String? {
        return try {
            when (val result = asrService.transcribe(file)) {
                is AsrResult.Success -> result.text.trim().also { file.delete() }
                is AsrResult.Error -> {
                    file.delete()
                    val message = when (result.code) {
                        AsrResult.ErrorCode.NETWORK_ERROR -> "网络连接波动，请重试"
                        else -> "当前无法完成语音识别，请重试"
                    }
                    if (_consultationState.value.isProcessing) {
                        _consultationState.value = _consultationState.value.copy(
                            isProcessing = false,
                            errorMessage = message
                        )
                    }
                    if (_profileState.value.isProcessing) {
                        _profileState.value = _profileState.value.copy(
                            isProcessing = false,
                            errorMessage = message
                        )
                    }
                    null
                }
            }
        } catch (_: Exception) {
            file.delete()
            val message = "当前无法完成语音识别，请重试"
            if (_consultationState.value.isProcessing) {
                _consultationState.value = _consultationState.value.copy(
                    isProcessing = false,
                    errorMessage = message
                )
            }
            if (_profileState.value.isProcessing) {
                _profileState.value = _profileState.value.copy(
                    isProcessing = false,
                    errorMessage = message
                )
            }
            null
        }
    }

    private companion object {
        private const val MIN_TRANSCRIPT_LENGTH = 4
    }
}
