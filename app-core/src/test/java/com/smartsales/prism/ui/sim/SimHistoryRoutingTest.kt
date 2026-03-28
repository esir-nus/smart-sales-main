package com.smartsales.prism.ui.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimHistoryRoutingTest {

    @Test
    fun `openSimHistory closes other overlays and keeps history visible`() {
        val updated = openSimHistory(
            SimShellState(
                activeDrawer = SimDrawerType.AUDIO,
                audioDrawerMode = SimAudioDrawerMode.CHAT_RESELECT,
                activeConnectivitySurface = SimConnectivitySurface.MODAL,
                showSettings = true
            )
        )

        assertEquals(null, updated.activeDrawer)
        assertEquals(SimAudioDrawerMode.BROWSE, updated.audioDrawerMode)
        assertEquals(null, updated.activeConnectivitySurface)
        assertTrue(updated.showHistory)
        assertFalse(updated.showSettings)
    }

    @Test
    fun `handleSimHistoryEntryRequest emits one open telemetry event`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleSimHistoryEntryRequest(
            state = SimShellState(activeDrawer = SimDrawerType.SCHEDULER),
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
        assertTrue(shouldShowSimShellScrim(SimShellState(showHistory = true)))
    }

    @Test
    fun `history drawer uses stronger scrim than generic overlay routes`() {
        assertEquals(0.56f, resolveSimShellScrimAlpha(SimShellState(showHistory = true)))
        assertEquals(0.4f, resolveSimShellScrimAlpha(SimShellState(activeDrawer = SimDrawerType.AUDIO)))
    }
}
