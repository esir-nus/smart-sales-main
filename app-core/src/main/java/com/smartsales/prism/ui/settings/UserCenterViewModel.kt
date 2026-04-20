// File: app-core/src/main/java/com/smartsales/prism/ui/settings/UserCenterViewModel.kt
// Module: :app-core
// Summary: 用户中心视图模型，管理个人资料、主题偏好与徽章语音音量。
// Author: updated on 2026-04-20

package com.smartsales.prism.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManager
import com.smartsales.prism.domain.memory.UserProfile
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.ui.theme.PrismThemeMode
import com.smartsales.prism.ui.theme.ThemePreferenceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserCenterViewModel @Inject constructor(
    private val repository: UserProfileRepository,
    private val notificationService: com.smartsales.prism.domain.notification.NotificationService,
    private val themePreferenceStore: ThemePreferenceStore,
    private val voiceVolumeStore: VoiceVolumePreferenceStore,
    private val connectionManager: DeviceConnectionManager
) : ViewModel() {

    val profile: StateFlow<UserProfile?> = repository.profile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val themeMode: StateFlow<PrismThemeMode> = themePreferenceStore.themeMode

    // 滑块当前显示值（跟手）。拖动期间只更新该 UI 状态，不触发 BLE 写入。
    private val _voiceVolume = MutableStateFlow(voiceVolumeStore.desiredVolume.value)
    val voiceVolume: StateFlow<Int> = _voiceVolume.asStateFlow()

    private var volumeSendJob: Job? = null

    fun hasNotificationPermission(): Boolean {
        return notificationService.hasPermission()
    }

    fun setThemeMode(mode: PrismThemeMode) {
        themePreferenceStore.setThemeMode(mode)
    }

    // 拖动中调用：仅更新 UI 显示，禁止发 BLE（ESP32 无法承受高频写入）
    fun onVoiceVolumeDrag(level: Int) {
        _voiceVolume.value = level.coerceIn(0, 100)
    }

    // 松手提交：持久化并下发一次 BLE 命令，取消任何在途的旧写入任务
    fun onVoiceVolumeCommitted() {
        val level = _voiceVolume.value
        voiceVolumeStore.setDesiredVolume(level)
        if (level == voiceVolumeStore.lastAppliedVolume.value) return
        volumeSendJob?.cancel()
        volumeSendJob = viewModelScope.launch {
            if (connectionManager.setVoiceVolume(level)) {
                voiceVolumeStore.markAppliedVolume(level)
            }
        }
    }

    fun updateProfile(
        displayName: String,
        role: String,
        industry: String,
        experienceYears: String,
        communicationPlatform: String
    ) {
        val current = profile.value ?: return
        val updated = current.copy(
            displayName = displayName,
            role = role,
            industry = industry,
            experienceYears = experienceYears,
            communicationPlatform = communicationPlatform,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.updateProfile(updated)
        }
    }
}
