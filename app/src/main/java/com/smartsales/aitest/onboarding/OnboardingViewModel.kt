package com.smartsales.aitest.onboarding

// 文件：app/src/main/java/com/smartsales/aitest/onboarding/OnboardingViewModel.kt
// 模块：:app
// 说明：引导流程的表单状态与保存逻辑
// 作者：创建于 2025-12-10

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.feature.usercenter.UserProfile
import com.smartsales.feature.usercenter.data.OnboardingStateRepository
import com.smartsales.feature.usercenter.data.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val displayName: String = "",
    val role: String = "",
    val industry: String = "",
    val mainChannel: String = "",
    val experienceLevel: String = "",
    val stylePreference: String = "",
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val completed: Boolean = false
)

sealed interface OnboardingEvent {
    data object Completed : OnboardingEvent
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val onboardingStateRepository: OnboardingStateRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        preloadProfile()
    }

    private fun preloadProfile() {
        viewModelScope.launch(dispatchers.io) {
            runCatching { profileRepository.load() }
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            displayName = profile.displayName,
                            role = (profile.salesPersona?.role ?: profile.role).orEmpty(),
                            industry = (profile.salesPersona?.industry ?: profile.industry).orEmpty(),
                            mainChannel = profile.salesPersona?.mainChannel.orEmpty(),
                            experienceLevel = profile.salesPersona?.experienceLevel.orEmpty(),
                            stylePreference = profile.salesPersona?.stylePreference.orEmpty()
                        )
                    }
                }
        }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value, errorMessage = null) }
    }

    fun onRoleChange(value: String) {
        _uiState.update { it.copy(role = value) }
    }

    fun onIndustryChange(value: String) {
        _uiState.update { it.copy(industry = value) }
    }

    fun onMainChannelChange(value: String) {
        _uiState.update { it.copy(mainChannel = value) }
    }

    fun onExperienceLevelChange(value: String) {
        _uiState.update { it.copy(experienceLevel = value) }
    }

    fun onStylePreferenceChange(value: String) {
        _uiState.update { it.copy(stylePreference = value) }
    }

    fun onSubmit(onCompleted: () -> Unit) {
        val current = _uiState.value
        if (current.displayName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请填写姓名") }
            return
        }
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val persona = com.smartsales.feature.usercenter.SalesPersona(
                role = current.role.trim().ifBlank { null },
                industry = current.industry.trim().ifBlank { null },
                mainChannel = current.mainChannel.trim().ifBlank { null },
                experienceLevel = current.experienceLevel.trim().ifBlank { null },
                stylePreference = current.stylePreference.trim().ifBlank { null }
            )
            val profile = UserProfile(
                displayName = current.displayName.trim(),
                email = "",
                isGuest = false,
                role = persona.role,
                industry = persona.industry,
                salesPersona = persona
            )
            runCatching {
                profileRepository.save(profile)
                onboardingStateRepository.markCompleted(true)
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, completed = true) }
                onCompleted()
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "保存失败") }
            }
        }
    }
}
