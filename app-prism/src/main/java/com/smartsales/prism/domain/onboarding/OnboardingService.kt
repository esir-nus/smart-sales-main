package com.smartsales.prism.domain.onboarding

/**
 * Onboarding Service — BLE设备扫描
 * Fake I/O pattern: 延迟在Fake实现中，不在UI
 */
interface OnboardingService {
    /**
     * 扫描BLE设备
     */
    suspend fun scanForDevices(): ScanResult
}

/**
 * 扫描结果
 */
sealed class ScanResult {
    data class Found(val deviceId: String, val name: String) : ScanResult()
    data object NotFound : ScanResult()
}
