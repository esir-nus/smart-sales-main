package com.smartsales.prism.ui.onboarding

import com.smartsales.prism.AppFlavor

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
    SCHEDULER_QUICK_START,
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
    @Suppress("UNUSED_PARAMETER") host: OnboardingHost
): OnboardingStep = when (currentStep) {
    OnboardingStep.WELCOME -> OnboardingStep.PERMISSIONS_PRIMER
    OnboardingStep.PERMISSIONS_PRIMER -> OnboardingStep.VOICE_HANDSHAKE_CONSULTATION
    OnboardingStep.VOICE_HANDSHAKE_CONSULTATION -> OnboardingStep.VOICE_HANDSHAKE_PROFILE
    OnboardingStep.VOICE_HANDSHAKE_PROFILE -> OnboardingStep.HARDWARE_WAKE
    OnboardingStep.SCHEDULER_QUICK_START -> OnboardingStep.COMPLETE
    OnboardingStep.HARDWARE_WAKE -> OnboardingStep.SCAN
    OnboardingStep.SCAN -> OnboardingStep.DEVICE_FOUND
    OnboardingStep.DEVICE_FOUND -> OnboardingStep.PROVISIONING
    OnboardingStep.PROVISIONING -> if (AppFlavor.schedulerEnabled) {
        OnboardingStep.SCHEDULER_QUICK_START
    } else {
        OnboardingStep.COMPLETE
    }
    OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
}

internal fun OnboardingStep.isConnectivityPairingStep(): Boolean = when (this) {
    OnboardingStep.HARDWARE_WAKE,
    OnboardingStep.SCAN,
    OnboardingStep.DEVICE_FOUND,
    OnboardingStep.PROVISIONING -> true
    else -> false
}
