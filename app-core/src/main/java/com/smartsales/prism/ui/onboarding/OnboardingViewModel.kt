package com.smartsales.prism.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Onboarding ViewModel。
 *
 * 只负责完整 onboarding 中的资料保存步骤。
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    /**
     * 保存用户资料（Onboarding ProfileStep）
     */
    fun saveProfile(displayName: String, role: String) {
        viewModelScope.launch {
            val current = userProfileRepository.getProfile()
            userProfileRepository.updateProfile(
                current.copy(
                    displayName = displayName.ifBlank { current.displayName },
                    role = role.ifBlank { current.role },
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
