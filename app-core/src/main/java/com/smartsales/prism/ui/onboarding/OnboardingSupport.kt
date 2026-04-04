package com.smartsales.prism.ui.onboarding

import com.smartsales.prism.domain.pairing.DiscoveredBadge
import com.smartsales.prism.domain.pairing.PairingState

internal data class OnboardingVisualCaptureState(
    val host: OnboardingHost,
    val step: OnboardingStep,
    val pairingState: PairingState = PairingState.Idle,
    val badge: DiscoveredBadge? = null,
    val ssid: String = "Office-WiFi",
    val password: String = "wave-a-preview",
    val permissionDenied: Boolean = false,
    val showProvisioningForm: Boolean = true,
    val consultationCaptureState: OnboardingConsultationCaptureState =
        OnboardingConsultationCaptureState.COMPLETE,
    val profileCaptureState: OnboardingProfileCaptureState =
        OnboardingProfileCaptureState.EXTRACTED,
    val quickStartCaptureState: OnboardingQuickStartCaptureState =
        OnboardingQuickStartCaptureState.UPDATED
)

enum class OnboardingExitPolicy {
    ALLOW_EXIT,
    EXPLICIT_ACTION_ONLY,
    BLOCK_EXIT
}

internal fun shouldShowOnboardingExitAction(exitPolicy: OnboardingExitPolicy): Boolean {
    return exitPolicy != OnboardingExitPolicy.BLOCK_EXIT
}

internal fun shouldBlockOnboardingSystemBack(exitPolicy: OnboardingExitPolicy): Boolean {
    return exitPolicy != OnboardingExitPolicy.ALLOW_EXIT
}

internal fun resolveOnboardingExitActionLabel(
    host: OnboardingHost,
    exitPolicy: OnboardingExitPolicy
): String {
    return when {
        host == OnboardingHost.SIM_CONNECTIVITY &&
            exitPolicy == OnboardingExitPolicy.ALLOW_EXIT -> "关闭"
        else -> "跳过"
    }
}
