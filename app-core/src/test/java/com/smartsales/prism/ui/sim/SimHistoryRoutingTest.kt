package com.smartsales.prism.ui.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimHistoryRoutingTest {

    @Test
    fun `openRuntimeHistory closes other overlays and keeps history visible`() {
        val updated = openRuntimeHistory(
            RuntimeShellState(
                activeDrawer = RuntimeDrawerType.AUDIO,
                audioDrawerMode = RuntimeAudioDrawerMode.CHAT_RESELECT,
                activeConnectivitySurface = RuntimeConnectivitySurface.MODAL,
                showSettings = true
            )
        )

        assertEquals(null, updated.activeDrawer)
        assertEquals(RuntimeAudioDrawerMode.BROWSE, updated.audioDrawerMode)
        assertEquals(null, updated.activeConnectivitySurface)
        assertTrue(updated.showHistory)
        assertFalse(updated.showSettings)
    }

    @Test
    fun `handleRuntimeHistoryEntryRequest emits one open telemetry event`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleRuntimeHistoryEntryRequest(
            state = RuntimeShellState(activeDrawer = RuntimeDrawerType.SCHEDULER),
            source = "hamburger",
            emitTelemetry = { summary, detail -> telemetry += summary to detail }
        )

        assertTrue(updated.showHistory)
        assertEquals(null, updated.activeDrawer)
        assertEquals(
            listOf(SIM_HISTORY_DRAWER_OPENED_SUMMARY to "source=hamburger"),
            telemetry
        )
    }

    @Test
    fun `history drawer keeps scrim visible`() {
        assertTrue(shouldShowRuntimeShellScrim(RuntimeShellState(showHistory = true)))
    }

    @Test
    fun `history drawer uses stronger scrim than generic overlay routes`() {
        assertEquals(0.56f, resolveRuntimeShellScrimAlpha(RuntimeShellState(showHistory = true)))
        assertEquals(0.4f, resolveRuntimeShellScrimAlpha(RuntimeShellState(activeDrawer = RuntimeDrawerType.AUDIO)))
    }
}
