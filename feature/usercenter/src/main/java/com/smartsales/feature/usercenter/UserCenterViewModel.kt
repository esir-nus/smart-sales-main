package com.smartsales.feature.usercenter

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/UserCenterViewModel.kt
// 模块：:feature:usercenter
// 说明：管理用户中心数据与导航事件的 ViewModel
// 作者：创建于 2025-11-30

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.feature.usercenter.data.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UserCenterViewModel @Inject constructor(
    private val repository: UserProfileRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserCenterUiState())
    val uiState: StateFlow<UserCenterUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UserCenterEvent>()
    val events: SharedFlow<UserCenterEvent> = _events.asSharedFlow()

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.profileFlow.collect { profile ->
                applyProfile(profile, loadingDone = true)
            }
        }
    }

    private fun applyProfile(profile: UserProfile, loadingDone: Boolean = false) {
        _uiState.update {
            it.copy(
                displayName = profile.displayName,
                role = profile.role.orEmpty(),
                industry = profile.industry.orEmpty(),
                email = profile.email,
                isGuest = profile.isGuest,
                canLogout = !profile.isGuest,
                organization = profile.organization,
                phone = profile.phone,
                isLoading = !loadingDone,
                errorMessage = null
            )
        }
    }

    fun onDeviceManagerClick() {
        viewModelScope.launch(dispatchers.default) {
            _events.emit(UserCenterEvent.DeviceManager)
        }
    }

    fun onPrivacyClick() {
        viewModelScope.launch(dispatchers.default) {
            _events.emit(UserCenterEvent.Privacy)
        }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value) }
    }

    fun onRoleChange(value: String) {
        _uiState.update { it.copy(role = value) }
    }

    fun onIndustryChange(value: String) {
        _uiState.update { it.copy(industry = value) }
    }

    fun onSaveProfile() {
        val current = _uiState.value
        viewModelScope.launch(dispatchers.io) {
            val profile = UserProfile(
                displayName = current.displayName,
                email = current.email,
                isGuest = false,
                organization = current.organization,
                role = current.role,
                industry = current.industry,
                phone = current.phone
            )
            runCatching { repository.save(profile) }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "保存失败") }
                }
        }
    }

    fun onLogoutClick() {
        if (!_uiState.value.canLogout) return
        viewModelScope.launch(dispatchers.io) {
            repository.clear()
            val guest = UserProfile(displayName = "", email = "", isGuest = true)
            applyProfile(guest)
            _events.emit(UserCenterEvent.Logout)
        }
    }
}
