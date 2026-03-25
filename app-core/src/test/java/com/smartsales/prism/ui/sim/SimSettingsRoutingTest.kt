package com.smartsales.prism.ui.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimSettingsRoutingTest {

    @Test
    fun `openSimSettings closes other overlays and opens settings`() {
        val updated = openSimSettings(
            SimShellState(
                activeDrawer = SimDrawerType.AUDIO,
                audioDrawerMode = SimAudioDrawerMode.CHAT_RESELECT,
                activeConnectivitySurface = SimConnectivitySurface.MODAL,
                showHistory = true
            )
        )

        assertEquals(null, updated.activeDrawer)
        assertEquals(SimAudioDrawerMode.BROWSE, updated.audioDrawerMode)
        assertEquals(null, updated.activeConnectivitySurface)
        assertFalse(updated.showHistory)
        assertTrue(updated.showSettings)
    }

    @Test
    fun `closeSimSettings keeps other shell state intact`() {
        val updated = closeSimSettings(
            SimShellState(showSettings = true)
        )

        assertFalse(updated.showSettings)
        assertEquals(null, updated.activeDrawer)
        assertEquals(null, updated.activeConnectivitySurface)
    }

    @Test
    fun `settings drawer keeps scrim visible with generic overlay alpha`() {
        val state = SimShellState(showSettings = true)

        assertTrue(shouldShowSimShellScrim(state))
        assertEquals(0.4f, resolveSimShellScrimAlpha(state))
    }

    @Test
    fun `settings drawer blocks shell edge gestures`() {
        val state = SimShellState(showSettings = true)

        assertFalse(canOpenSimSchedulerFromEdge(state))
        assertFalse(canOpenSimAudioFromEdge(state, isImeVisible = false))
    }
}
