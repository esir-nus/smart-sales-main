package com.smartsales.prism.ui.sim

import com.smartsales.prism.ui.components.connectivity.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimConnectivityRoutingTest {

    @Test
    fun `openRuntimeConnectivityModal closes other overlays and opens modal`() {
        val state = RuntimeShellState(
            activeDrawer = RuntimeDrawerType.AUDIO,
            audioDrawerMode = RuntimeAudioDrawerMode.CHAT_RESELECT,
            showHistory = true,
            showSettings = true
        )

        val updated = openRuntimeConnectivityModal(state)

        assertEquals(RuntimeConnectivitySurface.MODAL, updated.activeConnectivitySurface)
        assertEquals(null, updated.activeDrawer)
        assertEquals(RuntimeAudioDrawerMode.BROWSE, updated.audioDrawerMode)
        assertFalse(updated.showHistory)
        assertFalse(updated.showSettings)
        assertFalse(updated.connectivityModalAutoOpened)
    }

    @Test
    fun `auto opened connectivity modal records automatic launch mode`() {
        val updated = openRuntimeConnectivityModal(RuntimeShellState(), autoOpened = true)

        assertEquals(RuntimeConnectivitySurface.MODAL, updated.activeConnectivitySurface)
        assertTrue(updated.connectivityModalAutoOpened)
    }

    @Test
    fun `openRuntimeConnectivitySetup uses full screen route without scrim`() {
        val updated = openRuntimeConnectivitySetup(
            RuntimeShellState(connectivityModalAutoOpened = true)
        )

        assertEquals(RuntimeConnectivitySurface.SETUP, updated.activeConnectivitySurface)
        assertFalse(updated.connectivityModalAutoOpened)
        assertFalse(shouldShowRuntimeShellScrim(updated))
    }

    @Test
    fun `shouldShowRuntimeShellScrim only uses connectivity scrim for modal`() {
        assertTrue(
            shouldShowRuntimeShellScrim(
                RuntimeShellState(activeConnectivitySurface = RuntimeConnectivitySurface.MODAL)
            )
        )
        assertFalse(
            shouldShowRuntimeShellScrim(
                RuntimeShellState(activeConnectivitySurface = RuntimeConnectivitySurface.SETUP)
            )
        )
    }

    @Test
    fun `openRuntimeConnectivityManager uses full screen route without scrim`() {
        val updated = openRuntimeConnectivityManager(RuntimeShellState())

        assertEquals(RuntimeConnectivitySurface.MANAGER, updated.activeConnectivitySurface)
        assertFalse(shouldShowRuntimeShellScrim(updated))
    }

    @Test
    fun `resolveRuntimeConnectivityEntrySurface routes needs setup to modal`() {
        assertEquals(
            RuntimeConnectivitySurface.MODAL,
            resolveRuntimeConnectivityEntrySurface(ConnectionState.NEEDS_SETUP)
        )
    }

    @Test
    fun `resolveRuntimeConnectivityEntrySurface routes configured states to manager`() {
        assertEquals(
            RuntimeConnectivitySurface.MANAGER,
            resolveRuntimeConnectivityEntrySurface(ConnectionState.DISCONNECTED)
        )
        assertEquals(
            RuntimeConnectivitySurface.MANAGER,
            resolveRuntimeConnectivityEntrySurface(ConnectionState.CONNECTED)
        )
    }

    @Test
    fun `openRuntimeConnectivityEntry uses resolved manager route for configured state`() {
        val updated = openRuntimeConnectivityEntry(
            state = RuntimeShellState(showHistory = true, showSettings = true),
            connectionState = ConnectionState.DISCONNECTED
        )

        assertEquals(RuntimeConnectivitySurface.MANAGER, updated.activeConnectivitySurface)
        assertFalse(updated.showHistory)
        assertFalse(updated.showSettings)
    }

    @Test
    fun `handleRuntimeConnectivityEntryRequest emits bootstrap telemetry for needs setup`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleRuntimeConnectivityEntryRequest(
            state = RuntimeShellState(showHistory = true),
            connectionState = ConnectionState.NEEDS_SETUP,
            source = "chat_badge",
            emitTelemetry = { summary, detail -> telemetry += summary to detail }
        )

        assertEquals(RuntimeConnectivitySurface.MODAL, updated.activeConnectivitySurface)
        assertEquals(SIM_CONNECTIVITY_ENTRY_OPENED_SUMMARY, telemetry.single().first)
        assertTrue(telemetry.single().second.contains("source=chat_badge"))
        assertTrue(telemetry.single().second.contains("target=MODAL"))
    }

    @Test
    fun `handleRuntimeConnectivityEntryRequest emits manager telemetry for configured states`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleRuntimeConnectivityEntryRequest(
            state = RuntimeShellState(showSettings = true),
            connectionState = ConnectionState.DISCONNECTED,
            source = "history_device",
            emitTelemetry = { summary, detail -> telemetry += summary to detail }
        )

        assertEquals(RuntimeConnectivitySurface.MANAGER, updated.activeConnectivitySurface)
        assertEquals(SIM_CONNECTIVITY_MANAGER_DIRECT_ENTRY_OPENED_SUMMARY, telemetry.single().first)
        assertTrue(telemetry.single().second.contains("source=history_device"))
        assertTrue(telemetry.single().second.contains("target=MANAGER"))
    }

    @Test
    fun `handleRuntimeConnectivitySetupStart emits setup start telemetry`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleRuntimeConnectivitySetupStart(
            state = RuntimeShellState(),
            source = "bootstrap_modal",
            emitTelemetry = { summary, detail -> telemetry += summary to detail }
        )

        assertEquals(RuntimeConnectivitySurface.SETUP, updated.activeConnectivitySurface)
        assertEquals(SIM_CONNECTIVITY_SETUP_STARTED_SUMMARY, telemetry.single().first)
        assertTrue(telemetry.single().second.contains("source=bootstrap_modal"))
    }

    @Test
    fun `handleRuntimeConnectivitySetupCompleted returns to home and emits success telemetry`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleRuntimeConnectivitySetupCompleted(
            state = RuntimeShellState(
                activeConnectivitySurface = RuntimeConnectivitySurface.SETUP,
                isForcedFirstLaunchOnboarding = true
            ),
            emitTelemetry = { summary, detail -> telemetry += summary to detail }
        )

        assertEquals(null, updated.activeConnectivitySurface)
        assertFalse(updated.isForcedFirstLaunchOnboarding)
        assertEquals(SIM_CONNECTIVITY_SETUP_COMPLETED_SUMMARY, telemetry.single().first)
        assertTrue(telemetry.single().second.contains("source=pairing_success"))
        assertTrue(telemetry.single().second.contains("target=HOME"))
    }

    @Test
    fun `handleRuntimeConnectivitySetupSkipped closes setup and clears forced first launch flag`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleRuntimeConnectivitySetupSkipped(
            state = RuntimeShellState(
                activeConnectivitySurface = RuntimeConnectivitySurface.SETUP,
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
    fun `initialRuntimeShellState routes forced first launch into setup`() {
        val state = initialRuntimeShellState(forceSetupOnLaunch = true)

        assertEquals(RuntimeConnectivitySurface.SETUP, state.activeConnectivitySurface)
        assertTrue(state.isForcedFirstLaunchOnboarding)
    }

    @Test
    fun `startRuntimeForcedFirstLaunchOnboarding clears overlays and flags forced flow`() {
        val updated = startRuntimeForcedFirstLaunchOnboarding(
            RuntimeShellState(
                activeDrawer = RuntimeDrawerType.AUDIO,
                audioDrawerMode = RuntimeAudioDrawerMode.CHAT_RESELECT,
                showHistory = true,
                showSettings = true
            )
        )

        assertEquals(RuntimeConnectivitySurface.SETUP, updated.activeConnectivitySurface)
        assertEquals(null, updated.activeDrawer)
        assertEquals(RuntimeAudioDrawerMode.BROWSE, updated.audioDrawerMode)
        assertFalse(updated.showHistory)
        assertFalse(updated.showSettings)
        assertTrue(updated.isForcedFirstLaunchOnboarding)
    }

    @Test
    fun `handleRuntimeAddDeviceStart clears overlays and opens add device surface`() {
        val telemetry = mutableListOf<Pair<String, String>>()

        val updated = handleRuntimeAddDeviceStart(
            state = RuntimeShellState(
                activeDrawer = RuntimeDrawerType.AUDIO,
                showHistory = true,
                showSettings = true
            ),
            source = "bootstrap_modal",
            emitTelemetry = { summary, detail -> telemetry += summary to detail }
        )

        assertEquals(RuntimeConnectivitySurface.ADD_DEVICE, updated.activeConnectivitySurface)
        assertEquals(null, updated.activeDrawer)
        assertFalse(updated.showHistory)
        assertFalse(updated.showSettings)
        assertFalse(updated.isForcedFirstLaunchOnboarding)
        assertEquals(SIM_CONNECTIVITY_SETUP_STARTED_SUMMARY, telemetry.single().first)
        assertTrue(telemetry.single().second.contains("source=bootstrap_modal"))
        assertTrue(telemetry.single().second.contains("target=ADD_DEVICE"))
    }
}
