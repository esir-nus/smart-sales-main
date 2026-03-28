package com.smartsales.prism.ui.sim

import com.smartsales.prism.ui.components.connectivity.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimConnectivityRoutingTest {

    @Test
    fun `openSimConnectivityModal closes other overlays and opens modal`() {
        val state = SimShellState(
            activeDrawer = SimDrawerType.AUDIO,
            audioDrawerMode = SimAudioDrawerMode.CHAT_RESELECT,
            showHistory = true,
            showSettings = true
        )

        val updated = openSimConnectivityModal(state)

        assertEquals(SimConnectivitySurface.MODAL, updated.activeConnectivitySurface)
        assertEquals(null, updated.activeDrawer)
        assertEquals(SimAudioDrawerMode.BROWSE, updated.audioDrawerMode)
        assertFalse(updated.showHistory)
        assertFalse(updated.showSettings)
    }

    @Test
    fun `openSimConnectivitySetup uses full screen route without scrim`() {
        val updated = openSimConnectivitySetup(SimShellState())

        assertEquals(SimConnectivitySurface.SETUP, updated.activeConnectivitySurface)
        assertFalse(shouldShowSimShellScrim(updated))
    }

    @Test
    fun `shouldShowSimShellScrim only uses connectivity scrim for modal`() {
        assertTrue(
            shouldShowSimShellScrim(
                SimShellState(activeConnectivitySurface = SimConnectivitySurface.MODAL)
            )
        )
        assertFalse(
            shouldShowSimShellScrim(
                SimShellState(activeConnectivitySurface = SimConnectivitySurface.SETUP)
            )
        )
    }

    @Test
    fun `openSimConnectivityManager uses full screen route without scrim`() {
        val updated = openSimConnectivityManager(SimShellState())

        assertEquals(SimConnectivitySurface.MANAGER, updated.activeConnectivitySurface)
        assertFalse(shouldShowSimShellScrim(updated))
    }

    @Test
    fun `resolveSimConnectivityEntrySurface routes needs setup to modal`() {
        assertEquals(
            SimConnectivitySurface.MODAL,
            resolveSimConnectivityEntrySurface(ConnectionState.NEEDS_SETUP)
        )
    }

    @Test
    fun `resolveSimConnectivityEntrySurface routes configured states to manager`() {
        assertEquals(
            SimConnectivitySurface.MANAGER,
            resolveSimConnectivityEntrySurface(ConnectionState.DISCONNECTED)
        )
        assertEquals(
            SimConnectivitySurface.MANAGER,
            resolveSimConnectivityEntrySurface(ConnectionState.CONNECTED)
        )
    }

    @Test
    fun `openSimConnectivityEntry uses resolved manager route for configured state`() {
        val updated = openSimConnectivityEntry(
            state = SimShellState(showHistory = true, showSettings = true),
            connectionState = ConnectionState.DISCONNECTED
        )

        assertEquals(SimConnectivitySurface.MANAGER, updated.activeConnectivitySurface)
        assertFalse(updated.showHistory)
        assertFalse(updated.showSettings)
    }

    @Test
    fun `handleSimConnectivityEntryRequest emits bootstrap telemetry for needs setup`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleSimConnectivityEntryRequest(
            state = SimShellState(showHistory = true),
            connectionState = ConnectionState.NEEDS_SETUP,
            source = "chat_badge",
            emitTelemetry = { summary, detail -> telemetry += summary to detail }
        )

        assertEquals(SimConnectivitySurface.MODAL, updated.activeConnectivitySurface)
        assertEquals(SIM_CONNECTIVITY_ENTRY_OPENED_SUMMARY, telemetry.single().first)
        assertTrue(telemetry.single().second.contains("source=chat_badge"))
        assertTrue(telemetry.single().second.contains("target=MODAL"))
    }

    @Test
    fun `handleSimConnectivityEntryRequest emits manager telemetry for configured states`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleSimConnectivityEntryRequest(
            state = SimShellState(showSettings = true),
            connectionState = ConnectionState.DISCONNECTED,
            source = "history_device",
            emitTelemetry = { summary, detail -> telemetry += summary to detail }
        )

        assertEquals(SimConnectivitySurface.MANAGER, updated.activeConnectivitySurface)
        assertEquals(SIM_CONNECTIVITY_MANAGER_DIRECT_ENTRY_OPENED_SUMMARY, telemetry.single().first)
        assertTrue(telemetry.single().second.contains("source=history_device"))
        assertTrue(telemetry.single().second.contains("target=MANAGER"))
    }

    @Test
    fun `handleSimConnectivitySetupStart emits setup start telemetry`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleSimConnectivitySetupStart(
            state = SimShellState(),
            source = "bootstrap_modal",
            emitTelemetry = { summary, detail -> telemetry += summary to detail }
        )

        assertEquals(SimConnectivitySurface.SETUP, updated.activeConnectivitySurface)
        assertEquals(SIM_CONNECTIVITY_SETUP_STARTED_SUMMARY, telemetry.single().first)
        assertTrue(telemetry.single().second.contains("source=bootstrap_modal"))
    }

    @Test
    fun `handleSimConnectivityOnboardingReplayRequest always reopens setup and clears overlays`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleSimConnectivityOnboardingReplayRequest(
            state = SimShellState(
                activeDrawer = SimDrawerType.AUDIO,
                audioDrawerMode = SimAudioDrawerMode.CHAT_RESELECT,
                activeConnectivitySurface = SimConnectivitySurface.MANAGER,
                showHistory = true,
                showSettings = true
            ),
            source = "audio_drawer_replay",
            emitTelemetry = { summary, detail -> telemetry += summary to detail }
        )

        assertEquals(SimConnectivitySurface.SETUP, updated.activeConnectivitySurface)
        assertEquals(null, updated.activeDrawer)
        assertEquals(SimAudioDrawerMode.BROWSE, updated.audioDrawerMode)
        assertFalse(updated.showHistory)
        assertFalse(updated.showSettings)
        assertEquals(SIM_CONNECTIVITY_SETUP_STARTED_SUMMARY, telemetry.single().first)
        assertTrue(telemetry.single().second.contains("source=audio_drawer_replay"))
        assertTrue(telemetry.single().second.contains("replay=true"))
    }

    @Test
    fun `handleSimConnectivitySetupCompleted emits success telemetry`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleSimConnectivitySetupCompleted(
            state = SimShellState(
                activeConnectivitySurface = SimConnectivitySurface.SETUP,
                isForcedFirstLaunchOnboarding = true
            ),
            emitTelemetry = { summary, detail -> telemetry += summary to detail }
        )

        assertEquals(SimConnectivitySurface.MANAGER, updated.activeConnectivitySurface)
        assertFalse(updated.isForcedFirstLaunchOnboarding)
        assertEquals(SIM_CONNECTIVITY_SETUP_COMPLETED_SUMMARY, telemetry.single().first)
        assertTrue(telemetry.single().second.contains("source=pairing_success"))
    }

    @Test
    fun `handleSimConnectivitySetupSkipped closes setup and clears forced first launch flag`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleSimConnectivitySetupSkipped(
            state = SimShellState(
                activeConnectivitySurface = SimConnectivitySurface.SETUP,
                isForcedFirstLaunchOnboarding = true
            ),
            source = "first_launch_skip",
            emitTelemetry = { summary, detail -> telemetry += summary to detail }
        )

        assertEquals(null, updated.activeConnectivitySurface)
        assertFalse(updated.isForcedFirstLaunchOnboarding)
        assertEquals(SIM_CONNECTIVITY_SETUP_SKIPPED_SUMMARY, telemetry.single().first)
        assertTrue(telemetry.single().second.contains("source=first_launch_skip"))
        assertTrue(telemetry.single().second.contains("forced=true"))
    }

    @Test
    fun `initialSimShellState routes forced first launch into setup`() {
        val state = initialSimShellState(forceSetupOnLaunch = true)

        assertEquals(SimConnectivitySurface.SETUP, state.activeConnectivitySurface)
        assertTrue(state.isForcedFirstLaunchOnboarding)
    }

    @Test
    fun `startSimForcedFirstLaunchOnboarding clears overlays and flags forced flow`() {
        val updated = startSimForcedFirstLaunchOnboarding(
            SimShellState(
                activeDrawer = SimDrawerType.AUDIO,
                audioDrawerMode = SimAudioDrawerMode.CHAT_RESELECT,
                showHistory = true,
                showSettings = true
            )
        )

        assertEquals(SimConnectivitySurface.SETUP, updated.activeConnectivitySurface)
        assertEquals(null, updated.activeDrawer)
        assertEquals(SimAudioDrawerMode.BROWSE, updated.audioDrawerMode)
        assertFalse(updated.showHistory)
        assertFalse(updated.showSettings)
        assertTrue(updated.isForcedFirstLaunchOnboarding)
    }
}
