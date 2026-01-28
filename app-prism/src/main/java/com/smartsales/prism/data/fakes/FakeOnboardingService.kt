package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.onboarding.OnboardingService
import com.smartsales.prism.domain.onboarding.ScanResult
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake Onboarding Service — 骨架开发
 * BLE扫描延迟在这里，不在UI层
 */
@Singleton
class FakeOnboardingService @Inject constructor() : OnboardingService {
    
    override suspend fun scanForDevices(): ScanResult {
        delay(2000) // BLE扫描模拟
        return ScanResult.Found(
            deviceId = "FF:23:44:A1",
            name = "SmartBadge (Frank's)"
        )
    }
}
