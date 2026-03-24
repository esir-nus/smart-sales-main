package com.smartsales.prism.ui.sim

import com.smartsales.prism.ui.components.connectivity.ConnectionState

internal fun deriveSimFollowUpSurface(state: SimShellState): SimBadgeFollowUpSurface {
    return when {
        state.showHistory -> SimBadgeFollowUpSurface.HISTORY
        state.showSettings -> SimBadgeFollowUpSurface.SETTINGS
        state.activeConnectivitySurface != null -> SimBadgeFollowUpSurface.CONNECTIVITY
        state.activeDrawer == SimDrawerType.SCHEDULER -> SimBadgeFollowUpSurface.SCHEDULER
        else -> SimBadgeFollowUpSurface.CHAT
    }
}

internal fun closeSimOverlays(state: SimShellState): SimShellState = state.copy(
    activeDrawer = null,
    audioDrawerMode = SimAudioDrawerMode.BROWSE,
    activeConnectivitySurface = null,
    showHistory = false,
    showSettings = false
)

internal fun openSimScheduler(state: SimShellState): SimShellState = state.copy(
    activeDrawer = SimDrawerType.SCHEDULER,
    showHistory = false,
    activeConnectivitySurface = null,
    showSettings = false
)

internal fun openSimHistory(state: SimShellState): SimShellState = state.copy(
    activeDrawer = null,
    audioDrawerMode = SimAudioDrawerMode.BROWSE,
    activeConnectivitySurface = null,
    showHistory = true,
    showSettings = false
)

internal fun handleSimHistoryEntryRequest(
    state: SimShellState,
    source: String,
    emitTelemetry: (String, String) -> Unit = { summary, detail ->
        emitSimHistoryRouteTelemetry(summary, detail)
    }
): SimShellState {
    emitTelemetry(SIM_HISTORY_DRAWER_OPENED_SUMMARY, "source=$source")
    return openSimHistory(state)
}

internal fun openSimAudioDrawer(
    state: SimShellState,
    mode: SimAudioDrawerMode
): SimShellState = state.copy(
    activeDrawer = SimDrawerType.AUDIO,
    audioDrawerMode = mode,
    showHistory = false,
    activeConnectivitySurface = null,
    showSettings = false
)

internal fun openSimConnectivityModal(state: SimShellState): SimShellState = state.copy(
    activeDrawer = null,
    audioDrawerMode = SimAudioDrawerMode.BROWSE,
    activeConnectivitySurface = SimConnectivitySurface.MODAL,
    showHistory = false,
    showSettings = false
)

internal fun openSimConnectivitySetup(state: SimShellState): SimShellState = state.copy(
    activeDrawer = null,
    audioDrawerMode = SimAudioDrawerMode.BROWSE,
    activeConnectivitySurface = SimConnectivitySurface.SETUP,
    showHistory = false,
    showSettings = false
)

internal fun openSimConnectivityManager(state: SimShellState): SimShellState = state.copy(
    activeDrawer = null,
    audioDrawerMode = SimAudioDrawerMode.BROWSE,
    activeConnectivitySurface = SimConnectivitySurface.MANAGER,
    showHistory = false,
    showSettings = false
)

internal fun resolveSimConnectivityEntrySurface(
    connectionState: ConnectionState
): SimConnectivitySurface = when (connectionState) {
    ConnectionState.NEEDS_SETUP -> SimConnectivitySurface.MODAL
    else -> SimConnectivitySurface.MANAGER
}

internal fun openSimConnectivityEntry(
    state: SimShellState,
    connectionState: ConnectionState
): SimShellState = state.copy(
    activeDrawer = null,
    audioDrawerMode = SimAudioDrawerMode.BROWSE,
    activeConnectivitySurface = resolveSimConnectivityEntrySurface(connectionState),
    showHistory = false,
    showSettings = false
)

internal fun closeSimConnectivitySurface(state: SimShellState): SimShellState = state.copy(
    activeConnectivitySurface = null
)

internal fun handleSimConnectivityEntryRequest(
    state: SimShellState,
    connectionState: ConnectionState,
    source: String,
    emitTelemetry: (String, String) -> Unit = { summary, detail ->
        emitSimConnectivityRouteTelemetry(summary, detail)
    }
): SimShellState {
    val target = resolveSimConnectivityEntrySurface(connectionState)
    val summary = if (target == SimConnectivitySurface.MODAL) {
        SIM_CONNECTIVITY_ENTRY_OPENED_SUMMARY
    } else {
        SIM_CONNECTIVITY_MANAGER_DIRECT_ENTRY_OPENED_SUMMARY
    }
    emitTelemetry(summary, "source=$source state=$connectionState target=$target")
    return openSimConnectivityEntry(state, connectionState)
}

internal fun handleSimConnectivitySetupStart(
    state: SimShellState,
    source: String,
    emitTelemetry: (String, String) -> Unit = { summary, detail ->
        emitSimConnectivityRouteTelemetry(summary, detail)
    }
): SimShellState {
    emitTelemetry(
        SIM_CONNECTIVITY_SETUP_STARTED_SUMMARY,
        "source=$source target=${SimConnectivitySurface.SETUP}"
    )
    return openSimConnectivitySetup(state)
}

internal fun handleSimConnectivitySetupCompleted(
    state: SimShellState,
    emitTelemetry: (String, String) -> Unit = { summary, detail ->
        emitSimConnectivityRouteTelemetry(summary, detail)
    }
): SimShellState {
    emitTelemetry(
        SIM_CONNECTIVITY_SETUP_COMPLETED_SUMMARY,
        "source=pairing_success target=${SimConnectivitySurface.MANAGER}"
    )
    return openSimConnectivityManager(state)
}

internal fun shouldShowSimShellScrim(state: SimShellState): Boolean =
    state.activeDrawer == SimDrawerType.AUDIO ||
        state.showHistory ||
        state.activeConnectivitySurface == SimConnectivitySurface.MODAL

internal fun shouldAttemptSimAudioDrawerAutoSync(
    isDrawerOpen: Boolean,
    mode: SimAudioDrawerMode
): Boolean = isDrawerOpen && mode == SimAudioDrawerMode.BROWSE
