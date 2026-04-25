package com.smartsales.prism.ui.onboarding

/**
 * Onboarding 宿主类型。
 */
enum class OnboardingHost {
    FULL_APP,
    SIM_CONNECTIVITY,
    /** 已有设备后添加新徽章，跳过欢迎/语音采集，直接进入配网流程。 */
    SIM_ADD_DEVICE
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
    OnboardingHost.SIM_ADD_DEVICE -> OnboardingStep.HARDWARE_WAKE
}

internal fun nextOnboardingStep(
    currentStep: OnboardingStep,
    host: OnboardingHost
): OnboardingStep = when (currentStep) {
    OnboardingStep.WELCOME -> OnboardingStep.PERMISSIONS_PRIMER
    OnboardingStep.PERMISSIONS_PRIMER -> OnboardingStep.VOICE_HANDSHAKE_CONSULTATION
    OnboardingStep.VOICE_HANDSHAKE_CONSULTATION -> OnboardingStep.VOICE_HANDSHAKE_PROFILE
    OnboardingStep.VOICE_HANDSHAKE_PROFILE -> OnboardingStep.HARDWARE_WAKE
    OnboardingStep.SCHEDULER_QUICK_START -> OnboardingStep.COMPLETE
    OnboardingStep.HARDWARE_WAKE -> OnboardingStep.SCAN
    OnboardingStep.SCAN -> OnboardingStep.DEVICE_FOUND
    OnboardingStep.DEVICE_FOUND -> OnboardingStep.PROVISIONING
    OnboardingStep.PROVISIONING -> if (host == OnboardingHost.SIM_ADD_DEVICE) {
        OnboardingStep.COMPLETE
    } else {
        OnboardingStep.SCHEDULER_QUICK_START
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

internal fun OnboardingHost.usesSchedulerQuickStart(): Boolean = when (this) {
    OnboardingHost.FULL_APP,
    OnboardingHost.SIM_CONNECTIVITY -> true
    OnboardingHost.SIM_ADD_DEVICE -> false
}

internal fun OnboardingHost.closesAfterSuccessfulProvisioning(): Boolean = when (this) {
    OnboardingHost.SIM_ADD_DEVICE -> true
    OnboardingHost.FULL_APP,
    OnboardingHost.SIM_CONNECTIVITY -> false
}
