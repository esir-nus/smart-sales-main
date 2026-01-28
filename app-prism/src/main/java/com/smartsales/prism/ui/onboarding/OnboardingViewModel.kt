package com.smartsales.prism.ui.onboarding

import androidx.lifecycle.ViewModel
import com.smartsales.prism.domain.onboarding.OnboardingService
import com.smartsales.prism.domain.onboarding.ScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Onboarding ViewModel — 注入OnboardingService
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingService: OnboardingService
) : ViewModel() {
    
    /**
     * 扫描设备
     */
    suspend fun scanForDevices(): ScanResult {
        return onboardingService.scanForDevices()
    }
}
