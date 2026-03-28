package com.smartsales.prism.ui.onboarding

import com.smartsales.prism.domain.pairing.ErrorReason
import com.smartsales.prism.domain.pairing.PairingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimConnectivityPairingFlowTest {

    @Test
    fun `isConnectivityPairingStep only returns true for pairing subset`() {
        assertTrue(OnboardingStep.HARDWARE_WAKE.isConnectivityPairingStep())
        assertTrue(OnboardingStep.SCAN.isConnectivityPairingStep())
        assertTrue(OnboardingStep.DEVICE_FOUND.isConnectivityPairingStep())
        assertTrue(OnboardingStep.PROVISIONING.isConnectivityPairingStep())

        assertFalse(OnboardingStep.WELCOME.isConnectivityPairingStep())
        assertFalse(OnboardingStep.PERMISSIONS_PRIMER.isConnectivityPairingStep())
        assertFalse(OnboardingStep.VOICE_HANDSHAKE_CONSULTATION.isConnectivityPairingStep())
        assertFalse(OnboardingStep.VOICE_HANDSHAKE_PROFILE.isConnectivityPairingStep())
        assertFalse(OnboardingStep.COMPLETE.isConnectivityPairingStep())
    }

    @Test
    fun `resolveConnectivityPairingErrorUiModel maps scan errors to retry scan`() {
        val error = PairingState.Error(
            message = "未发现设备，请检查设备电源与距离",
            reason = ErrorReason.SCAN_TIMEOUT,
            canRetry = true
        )

        val uiModel = resolveConnectivityPairingErrorUiModel(OnboardingStep.SCAN, error)

        assertEquals("未发现设备", uiModel.title)
        assertEquals("重新扫描", uiModel.primaryLabel)
        assertEquals("返回上一步", uiModel.secondaryLabel)
        assertEquals(ConnectivityPairingRetryAction.RETRY_SCAN, uiModel.retryAction)
    }

    @Test
    fun `resolveConnectivityPairingErrorUiModel maps pairing errors to retry provisioning`() {
        val error = PairingState.Error(
            message = "设备尚未上线，请检查 WiFi",
            reason = ErrorReason.NETWORK_CHECK_FAILED,
            canRetry = true
        )

        val uiModel = resolveConnectivityPairingErrorUiModel(OnboardingStep.PROVISIONING, error)

        assertEquals("设备尚未上线", uiModel.title)
        assertEquals("重试配网", uiModel.primaryLabel)
        assertEquals("返回上一步", uiModel.secondaryLabel)
        assertEquals(ConnectivityPairingRetryAction.RETRY_PROVISIONING, uiModel.retryAction)
    }

    @Test
    fun `resolveConnectivityPairingErrorUiModel maps device not found to rescan`() {
        val error = PairingState.Error(
            message = "设备已不可用，请重新扫描",
            reason = ErrorReason.DEVICE_NOT_FOUND,
            canRetry = true
        )

        val uiModel = resolveConnectivityPairingErrorUiModel(OnboardingStep.PROVISIONING, error)

        assertEquals("设备已不可用", uiModel.title)
        assertEquals("重新扫描", uiModel.primaryLabel)
        assertEquals("返回上一步", uiModel.secondaryLabel)
        assertEquals(ConnectivityPairingRetryAction.RETRY_SCAN, uiModel.retryAction)
    }
}
