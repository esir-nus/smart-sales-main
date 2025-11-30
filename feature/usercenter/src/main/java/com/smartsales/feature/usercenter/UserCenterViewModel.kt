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
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch(dispatchers.io) {
            val result = repository.load()
            applyProfile(result)
        }
    }

    private fun applyProfile(profile: UserProfile) {
        _uiState.update {
            it.copy(
                displayName = profile.displayName,
                email = profile.email,
                isGuest = profile.isGuest,
                canLogout = !profile.isGuest
            )
        }
    }

    fun onDeviceManagerClick() {
        viewModelScope.launch(dispatchers.default) {
            _events.emit(UserCenterEvent.DeviceManager)
        }
    }

    fun onSubscriptionClick() {
        viewModelScope.launch(dispatchers.default) {
            _events.emit(UserCenterEvent.Subscription)
        }
    }

    fun onPrivacyClick() {
        viewModelScope.launch(dispatchers.default) {
            _events.emit(UserCenterEvent.Privacy)
        }
    }

    fun onGeneralSettingsClick() {
        viewModelScope.launch(dispatchers.default) {
            _events.emit(UserCenterEvent.General)
        }
    }

    fun onLoginClick() {
        if (!_uiState.value.isGuest) return
        viewModelScope.launch(dispatchers.io) {
            val profile = UserProfile(
                displayName = "SmartSales 用户",
                email = "user@example.com",
                isGuest = false
            )
            repository.save(profile)
            applyProfile(profile)
            _events.emit(UserCenterEvent.Login)
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
