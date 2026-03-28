package com.smartsales.prism.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.memory.UserProfile
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.ui.theme.PrismThemeMode
import com.smartsales.prism.ui.theme.ThemePreferenceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserCenterViewModel @Inject constructor(
    private val repository: UserProfileRepository,
    private val notificationService: com.smartsales.prism.domain.notification.NotificationService,
    private val themePreferenceStore: ThemePreferenceStore
) : ViewModel() {

    val profile: StateFlow<UserProfile?> = repository.profile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val themeMode: StateFlow<PrismThemeMode> = themePreferenceStore.themeMode

    fun hasNotificationPermission(): Boolean {
        return notificationService.hasPermission()
    }

    fun setThemeMode(mode: PrismThemeMode) {
        themePreferenceStore.setThemeMode(mode)
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
