package com.smartsales.prism.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingFlowTransitionTest {

    @Test
    fun `full app host starts at welcome and walks the full prototype path`() {
        assertEquals(OnboardingStep.WELCOME, initialOnboardingStep(OnboardingHost.FULL_APP))
        assertEquals(
            OnboardingStep.PERMISSIONS_PRIMER,
            nextOnboardingStep(OnboardingStep.WELCOME, OnboardingHost.FULL_APP)
        )
        assertEquals(
            OnboardingStep.VOICE_HANDSHAKE,
            nextOnboardingStep(OnboardingStep.PERMISSIONS_PRIMER, OnboardingHost.FULL_APP)
        )
        assertEquals(
            OnboardingStep.HARDWARE_WAKE,
            nextOnboardingStep(OnboardingStep.VOICE_HANDSHAKE, OnboardingHost.FULL_APP)
        )
        assertEquals(
            OnboardingStep.SCAN,
            nextOnboardingStep(OnboardingStep.HARDWARE_WAKE, OnboardingHost.FULL_APP)
        )
        assertEquals(
            OnboardingStep.DEVICE_FOUND,
            nextOnboardingStep(OnboardingStep.SCAN, OnboardingHost.FULL_APP)
        )
        assertEquals(
            OnboardingStep.PROVISIONING,
            nextOnboardingStep(OnboardingStep.DEVICE_FOUND, OnboardingHost.FULL_APP)
        )
        assertEquals(
            OnboardingStep.COMPLETE,
            nextOnboardingStep(OnboardingStep.PROVISIONING, OnboardingHost.FULL_APP)
        )
    }

    @Test
    fun `sim host starts at permissions and skips welcome plus voice handshake`() {
        assertEquals(OnboardingStep.PERMISSIONS_PRIMER, initialOnboardingStep(OnboardingHost.SIM_CONNECTIVITY))
        assertEquals(
            OnboardingStep.HARDWARE_WAKE,
            nextOnboardingStep(OnboardingStep.PERMISSIONS_PRIMER, OnboardingHost.SIM_CONNECTIVITY)
        )
        assertEquals(
            OnboardingStep.SCAN,
            nextOnboardingStep(OnboardingStep.HARDWARE_WAKE, OnboardingHost.SIM_CONNECTIVITY)
        )
        assertEquals(
            OnboardingStep.DEVICE_FOUND,
            nextOnboardingStep(OnboardingStep.SCAN, OnboardingHost.SIM_CONNECTIVITY)
        )
        assertEquals(
            OnboardingStep.PROVISIONING,
            nextOnboardingStep(OnboardingStep.DEVICE_FOUND, OnboardingHost.SIM_CONNECTIVITY)
        )
        assertEquals(
            OnboardingStep.COMPLETE,
            nextOnboardingStep(OnboardingStep.PROVISIONING, OnboardingHost.SIM_CONNECTIVITY)
        )
    }
}
