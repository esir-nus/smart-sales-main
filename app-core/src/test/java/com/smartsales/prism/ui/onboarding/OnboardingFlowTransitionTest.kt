package com.smartsales.prism.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingFlowTransitionTest {

    @Test
    fun `full onboarding pairing completion continues into remaining onboarding steps`() {
        assertEquals(OnboardingStep.DEVICE_NAMING, nextStepAfterPairingForFullOnboarding())
        assertEquals(OnboardingStep.ACCOUNT_GATE, nextStepAfterDeviceNaming())
        assertEquals(OnboardingStep.PROFILE, nextStepAfterAccountGate())
        assertEquals(OnboardingStep.NOTIFICATION_PERMISSION, nextStepAfterProfile())
    }
}
