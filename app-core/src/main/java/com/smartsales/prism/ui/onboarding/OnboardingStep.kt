package com.smartsales.prism.ui.onboarding

/**
 * Onboarding 宿主类型。
 */
enum class OnboardingHost {
    FULL_APP,
    SIM_CONNECTIVITY
}

/**
 * 当前生产态 onboarding 步骤。
 */
enum class OnboardingStep {
    WELCOME,
    PERMISSIONS_PRIMER,
    VOICE_HANDSHAKE_CONSULTATION,
    VOICE_HANDSHAKE_PROFILE,
    HARDWARE_WAKE,
    SCAN,
    DEVICE_FOUND,
    PROVISIONING,
    COMPLETE
}

internal fun initialOnboardingStep(host: OnboardingHost): OnboardingStep = when (host) {
    OnboardingHost.FULL_APP -> OnboardingStep.WELCOME
    OnboardingHost.SIM_CONNECTIVITY -> OnboardingStep.WELCOME
}

internal fun nextOnboardingStep(
    currentStep: OnboardingStep,
    host: OnboardingHost
): OnboardingStep = when (currentStep) {
    OnboardingStep.WELCOME -> OnboardingStep.PERMISSIONS_PRIMER
    OnboardingStep.PERMISSIONS_PRIMER -> OnboardingStep.VOICE_HANDSHAKE_CONSULTATION
    OnboardingStep.VOICE_HANDSHAKE_CONSULTATION -> OnboardingStep.VOICE_HANDSHAKE_PROFILE
    OnboardingStep.VOICE_HANDSHAKE_PROFILE -> OnboardingStep.HARDWARE_WAKE
    OnboardingStep.HARDWARE_WAKE -> OnboardingStep.SCAN
    OnboardingStep.SCAN -> OnboardingStep.DEVICE_FOUND
    OnboardingStep.DEVICE_FOUND -> OnboardingStep.PROVISIONING
    OnboardingStep.PROVISIONING -> OnboardingStep.COMPLETE
    OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
}

internal fun OnboardingStep.isConnectivityPairingStep(): Boolean = when (this) {
    OnboardingStep.HARDWARE_WAKE,
    OnboardingStep.SCAN,
    OnboardingStep.DEVICE_FOUND,
    OnboardingStep.PROVISIONING -> true
    else -> false
}
