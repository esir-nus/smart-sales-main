package com.smartsales.feature.usercenter

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/UserCenterViewModel.kt
// 模块：:feature:usercenter
// 说明：管理用户中心数据、保存与登出的 ViewModel
// 作者：创建于 2025-11-21

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.usercenter.data.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SAVE_DELAY_MS = 500L

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

    fun onUserNameChanged(value: String) {
        _uiState.update { it.copy(userName = value) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onToggleFeatureFlag(key: String) {
        _uiState.update { state ->
            val updated = LinkedHashMap(state.featureFlags)
            val current = updated[key] ?: false
            updated[key] = !current
            state.copy(featureFlags = updated)
        }
    }

    fun onSaveClicked() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            delay(SAVE_DELAY_MS)
            val profile = uiState.value.toProfile()
            when (val result = repository.save(profile)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = null) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.throwable.message ?: "保存失败"
                        )
                    }
                }
            }
        }
    }

    fun onLogoutClicked() {
        viewModelScope.launch(dispatchers.default) {
            _events.emit(UserCenterEvent.Logout)
        }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadUser() {
        viewModelScope.launch(dispatchers.io) {
            when (val result = repository.load()) {
                is Result.Success -> applyProfile(result.data)
                is Result.Error -> _uiState.update {
                    it.copy(errorMessage = result.throwable.message ?: "加载用户失败")
                }
            }
        }
    }

    private suspend fun applyProfile(profile: UserProfile) = withContext(dispatchers.default) {
        _uiState.update {
            it.copy(
                userName = profile.userName,
                email = profile.email,
                tokensRemaining = profile.tokensRemaining,
                featureFlags = LinkedHashMap(profile.featureFlags),
                isSaving = false,
                errorMessage = null,
                canLogout = true
            )
        }
    }

    private fun UserCenterUiState.toProfile(): UserProfile =
        UserProfile(
            userName = userName,
            email = email,
            tokensRemaining = tokensRemaining,
            featureFlags = LinkedHashMap(featureFlags)
        )

    fun onLogoutConfirmed() {
        _uiState.update {
            it.copy(
                userName = "",
                email = "",
                tokensRemaining = null,
                featureFlags = emptyMap(),
                canLogout = false,
                errorMessage = null,
                isSaving = false
            )
        }
    }
}
