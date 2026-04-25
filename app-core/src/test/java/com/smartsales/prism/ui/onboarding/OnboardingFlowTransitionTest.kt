package com.smartsales.prism.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
            OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            nextOnboardingStep(OnboardingStep.PERMISSIONS_PRIMER, OnboardingHost.FULL_APP)
        )
        assertEquals(
            OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            nextOnboardingStep(OnboardingStep.VOICE_HANDSHAKE_CONSULTATION, OnboardingHost.FULL_APP)
        )
        assertEquals(
            OnboardingStep.HARDWARE_WAKE,
            nextOnboardingStep(OnboardingStep.VOICE_HANDSHAKE_PROFILE, OnboardingHost.FULL_APP)
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
            OnboardingStep.SCHEDULER_QUICK_START,
            nextOnboardingStep(OnboardingStep.PROVISIONING, OnboardingHost.FULL_APP)
        )
        assertEquals(
            OnboardingStep.COMPLETE,
            nextOnboardingStep(OnboardingStep.SCHEDULER_QUICK_START, OnboardingHost.FULL_APP)
        )
    }

    @Test
    fun `sim host now follows the same production path through quick start`() {
        assertEquals(OnboardingStep.WELCOME, initialOnboardingStep(OnboardingHost.SIM_CONNECTIVITY))
        assertEquals(
            OnboardingStep.PERMISSIONS_PRIMER,
            nextOnboardingStep(OnboardingStep.WELCOME, OnboardingHost.SIM_CONNECTIVITY)
        )
        assertEquals(
            OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            nextOnboardingStep(OnboardingStep.PERMISSIONS_PRIMER, OnboardingHost.SIM_CONNECTIVITY)
        )
        assertEquals(
            OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            nextOnboardingStep(OnboardingStep.VOICE_HANDSHAKE_CONSULTATION, OnboardingHost.SIM_CONNECTIVITY)
        )
        assertEquals(
            OnboardingStep.HARDWARE_WAKE,
            nextOnboardingStep(OnboardingStep.VOICE_HANDSHAKE_PROFILE, OnboardingHost.SIM_CONNECTIVITY)
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
            OnboardingStep.SCHEDULER_QUICK_START,
            nextOnboardingStep(OnboardingStep.PROVISIONING, OnboardingHost.SIM_CONNECTIVITY)
        )
        assertEquals(
            OnboardingStep.COMPLETE,
            nextOnboardingStep(OnboardingStep.SCHEDULER_QUICK_START, OnboardingHost.SIM_CONNECTIVITY)
        )
    }

    @Test
    fun `add device host skips intro quick start and closes after provisioning`() {
        assertEquals(OnboardingStep.HARDWARE_WAKE, initialOnboardingStep(OnboardingHost.SIM_ADD_DEVICE))
        assertEquals(
            OnboardingStep.SCAN,
            nextOnboardingStep(OnboardingStep.HARDWARE_WAKE, OnboardingHost.SIM_ADD_DEVICE)
        )
        assertEquals(
            OnboardingStep.DEVICE_FOUND,
            nextOnboardingStep(OnboardingStep.SCAN, OnboardingHost.SIM_ADD_DEVICE)
        )
        assertEquals(
            OnboardingStep.PROVISIONING,
            nextOnboardingStep(OnboardingStep.DEVICE_FOUND, OnboardingHost.SIM_ADD_DEVICE)
        )
        assertFalse(OnboardingHost.SIM_ADD_DEVICE.usesSchedulerQuickStart())
        assertTrue(OnboardingHost.SIM_ADD_DEVICE.closesAfterSuccessfulProvisioning())
    }

    @Test
    fun `full app and sim connectivity hosts require scheduler quick start`() {
        assertTrue(OnboardingHost.FULL_APP.usesSchedulerQuickStart())
        assertTrue(OnboardingHost.SIM_CONNECTIVITY.usesSchedulerQuickStart())
        assertFalse(OnboardingHost.FULL_APP.closesAfterSuccessfulProvisioning())
        assertFalse(OnboardingHost.SIM_CONNECTIVITY.closesAfterSuccessfulProvisioning())
    }

    @Test
    fun `allow exit policy keeps explicit exit affordance and back free`() {
        assertTrue(shouldShowOnboardingExitAction(OnboardingExitPolicy.ALLOW_EXIT))
        assertFalse(shouldBlockOnboardingSystemBack(OnboardingExitPolicy.ALLOW_EXIT))
        assertEquals(
            "关闭",
            resolveOnboardingExitActionLabel(
                OnboardingHost.SIM_CONNECTIVITY,
                OnboardingExitPolicy.ALLOW_EXIT
            )
        )
    }

    @Test
    fun `add device host exposes close affordance when exit is allowed`() {
        assertTrue(
            shouldShowCoordinatorExitAction(
                OnboardingHost.SIM_ADD_DEVICE,
                OnboardingExitPolicy.ALLOW_EXIT
            )
        )
        assertFalse(
            shouldShowCoordinatorExitAction(
                OnboardingHost.SIM_ADD_DEVICE,
                OnboardingExitPolicy.BLOCK_EXIT
            )
        )
        assertFalse(
            shouldShowCoordinatorExitAction(
                OnboardingHost.SIM_CONNECTIVITY,
                OnboardingExitPolicy.ALLOW_EXIT
            )
        )
    }

    @Test
    fun `explicit action only keeps skip affordance while back stays blocked`() {
        assertTrue(shouldShowOnboardingExitAction(OnboardingExitPolicy.EXPLICIT_ACTION_ONLY))
        assertTrue(shouldBlockOnboardingSystemBack(OnboardingExitPolicy.EXPLICIT_ACTION_ONLY))
        assertEquals(
            "跳过",
            resolveOnboardingExitActionLabel(
                OnboardingHost.SIM_CONNECTIVITY,
                OnboardingExitPolicy.EXPLICIT_ACTION_ONLY
            )
        )
    }

    @Test
    fun `block exit policy hides explicit exit affordance and consumes back`() {
        assertFalse(shouldShowOnboardingExitAction(OnboardingExitPolicy.BLOCK_EXIT))
        assertTrue(shouldBlockOnboardingSystemBack(OnboardingExitPolicy.BLOCK_EXIT))
    }
}
