package com.smartsales.prism.ui.sim

import com.smartsales.prism.ui.components.connectivity.ConnectionState
import com.smartsales.prism.ui.shell.BaseRuntimeAudioDrawerMode
import com.smartsales.prism.ui.shell.BaseRuntimeConnectivitySurface
import com.smartsales.prism.ui.shell.BaseRuntimeDrawerType
import com.smartsales.prism.ui.shell.BaseRuntimeShellCoreState
import com.smartsales.prism.ui.shell.closeBaseRuntimeConnectivitySurface
import com.smartsales.prism.ui.shell.closeBaseRuntimeOverlays
import com.smartsales.prism.ui.shell.closeBaseRuntimeSettings
import com.smartsales.prism.ui.shell.openBaseRuntimeAudioDrawer
import com.smartsales.prism.ui.shell.openBaseRuntimeConnectivityManager
import com.smartsales.prism.ui.shell.openBaseRuntimeConnectivityModal
import com.smartsales.prism.ui.shell.openBaseRuntimeConnectivitySetup
import com.smartsales.prism.ui.shell.openBaseRuntimeHistory
import com.smartsales.prism.ui.shell.openBaseRuntimeScheduler
import com.smartsales.prism.ui.shell.openBaseRuntimeSettings
import com.smartsales.prism.ui.shell.resolveBaseRuntimeScrimAlpha
import com.smartsales.prism.ui.shell.shouldShowBaseRuntimeIdleComposerHint
import com.smartsales.prism.ui.shell.shouldShowBaseRuntimeScrim

internal fun deriveRuntimeFollowUpSurface(state: RuntimeShellState): SimBadgeFollowUpSurface {
    return when {
        state.showHistory -> SimBadgeFollowUpSurface.HISTORY
        state.showSettings -> SimBadgeFollowUpSurface.SETTINGS
        state.activeConnectivitySurface != null -> SimBadgeFollowUpSurface.CONNECTIVITY
        state.activeDrawer == RuntimeDrawerType.SCHEDULER -> SimBadgeFollowUpSurface.SCHEDULER
        else -> SimBadgeFollowUpSurface.CHAT
    }
}

internal fun initialRuntimeShellState(
    forceSetupOnLaunch: Boolean
): RuntimeShellState = if (forceSetupOnLaunch) {
    RuntimeShellState(
        activeConnectivitySurface = openBaseRuntimeConnectivitySetup(BaseRuntimeShellCoreState())
            .toRuntimeConnectivitySurface(),
        isForcedFirstLaunchOnboarding = true
    )
} else {
    RuntimeShellState()
}

internal fun startRuntimeForcedFirstLaunchOnboarding(state: RuntimeShellState): RuntimeShellState =
    openRuntimeConnectivitySetup(state).copy(isForcedFirstLaunchOnboarding = true)

internal fun closeRuntimeOverlays(state: RuntimeShellState): RuntimeShellState =
    closeBaseRuntimeOverlays(state.toBaseRuntimeShellCoreState()).mergeInto(state)

internal fun openRuntimeScheduler(state: RuntimeShellState): RuntimeShellState =
    openBaseRuntimeScheduler(state.toBaseRuntimeShellCoreState()).mergeInto(state)

internal fun openRuntimeHistory(state: RuntimeShellState): RuntimeShellState =
    openBaseRuntimeHistory(state.toBaseRuntimeShellCoreState()).mergeInto(state)

internal fun openRuntimeSettings(state: RuntimeShellState): RuntimeShellState =
    openBaseRuntimeSettings(state.toBaseRuntimeShellCoreState()).mergeInto(state)

internal fun closeRuntimeSettings(state: RuntimeShellState): RuntimeShellState =
    closeBaseRuntimeSettings(state.toBaseRuntimeShellCoreState()).mergeInto(state)

internal fun handleRuntimeHistoryEntryRequest(
    state: RuntimeShellState,
    source: String,
    emitTelemetry: (String, String) -> Unit = { summary, detail ->
        emitSimHistoryRouteTelemetry(summary, detail)
    }
): RuntimeShellState {
    emitTelemetry(SIM_HISTORY_DRAWER_OPENED_SUMMARY, "source=$source")
    return openRuntimeHistory(state)
}

internal fun openRuntimeAudioDrawer(
    state: RuntimeShellState,
    mode: RuntimeAudioDrawerMode
): RuntimeShellState = openBaseRuntimeAudioDrawer(
    state.toBaseRuntimeShellCoreState(),
    mode.toBaseRuntimeAudioDrawerMode()
).mergeInto(state)

internal fun openRuntimeConnectivityModal(
    state: RuntimeShellState,
    autoOpened: Boolean = false
): RuntimeShellState =
    openBaseRuntimeConnectivityModal(state.toBaseRuntimeShellCoreState())
        .mergeInto(state)
        .copy(connectivityModalAutoOpened = autoOpened)

internal fun openRuntimeConnectivitySetup(state: RuntimeShellState): RuntimeShellState =
    openBaseRuntimeConnectivitySetup(state.toBaseRuntimeShellCoreState()).mergeInto(state)

internal fun openRuntimeConnectivityManager(state: RuntimeShellState): RuntimeShellState =
    openBaseRuntimeConnectivityManager(state.toBaseRuntimeShellCoreState()).mergeInto(state)

internal fun resolveRuntimeConnectivityEntrySurface(
    connectionState: ConnectionState
): RuntimeConnectivitySurface = when (connectionState) {
    ConnectionState.NEEDS_SETUP -> RuntimeConnectivitySurface.MODAL
    else -> RuntimeConnectivitySurface.MANAGER
}

internal fun openRuntimeConnectivityEntry(
    state: RuntimeShellState,
    connectionState: ConnectionState
): RuntimeShellState = when (resolveRuntimeConnectivityEntrySurface(connectionState)) {
    RuntimeConnectivitySurface.MODAL -> openRuntimeConnectivityModal(state)
    RuntimeConnectivitySurface.SETUP -> openRuntimeConnectivitySetup(state)
    RuntimeConnectivitySurface.MANAGER -> openRuntimeConnectivityManager(state)
    RuntimeConnectivitySurface.ADD_DEVICE -> openRuntimeConnectivitySetup(state)
}

internal fun closeRuntimeConnectivitySurface(state: RuntimeShellState): RuntimeShellState =
    closeBaseRuntimeConnectivitySurface(state.toBaseRuntimeShellCoreState()).mergeInto(state)

internal fun handleRuntimeConnectivityEntryRequest(
    state: RuntimeShellState,
    connectionState: ConnectionState,
    source: String,
    emitTelemetry: (String, String) -> Unit = { summary, detail ->
        emitSimConnectivityRouteTelemetry(summary, detail)
    }
): RuntimeShellState {
    val target = resolveRuntimeConnectivityEntrySurface(connectionState)
    val summary = if (target == RuntimeConnectivitySurface.MODAL) {
        SIM_CONNECTIVITY_ENTRY_OPENED_SUMMARY
    } else {
        SIM_CONNECTIVITY_MANAGER_DIRECT_ENTRY_OPENED_SUMMARY
    }
    emitTelemetry(summary, "source=$source state=$connectionState target=$target")
    return openRuntimeConnectivityEntry(state, connectionState)
}

internal fun handleRuntimeConnectivitySetupStart(
    state: RuntimeShellState,
    source: String,
    emitTelemetry: (String, String) -> Unit = { summary, detail ->
        emitSimConnectivityRouteTelemetry(summary, detail)
    }
): RuntimeShellState {
    emitTelemetry(
        SIM_CONNECTIVITY_SETUP_STARTED_SUMMARY,
        "source=$source target=${RuntimeConnectivitySurface.SETUP}"
    )
    return openRuntimeConnectivitySetup(state)
}

internal fun handleRuntimeAddDeviceStart(
    state: RuntimeShellState,
    source: String,
    emitTelemetry: (String, String) -> Unit = { summary, detail ->
        emitSimConnectivityRouteTelemetry(summary, detail)
    }
): RuntimeShellState {
    emitTelemetry(
        SIM_CONNECTIVITY_SETUP_STARTED_SUMMARY,
        "source=$source target=${RuntimeConnectivitySurface.ADD_DEVICE}"
    )
    return closeRuntimeOverlays(state).copy(
        activeConnectivitySurface = RuntimeConnectivitySurface.ADD_DEVICE
    )
}

internal fun handleRuntimeConnectivitySetupCompleted(
    state: RuntimeShellState,
    emitTelemetry: (String, String) -> Unit = { summary, detail ->
        emitSimConnectivityRouteTelemetry(summary, detail)
    }
): RuntimeShellState {
    emitTelemetry(
        SIM_CONNECTIVITY_SETUP_COMPLETED_SUMMARY,
        "source=pairing_success target=HOME"
    )
    return closeRuntimeOverlays(state).copy(isForcedFirstLaunchOnboarding = false)
}

internal fun handleRuntimeConnectivitySetupSkipped(
    state: RuntimeShellState,
    source: String,
    emitTelemetry: (String, String) -> Unit = { summary, detail ->
        emitSimConnectivityRouteTelemetry(summary, detail)
    }
): RuntimeShellState {
    emitTelemetry(
        SIM_CONNECTIVITY_SETUP_SKIPPED_SUMMARY,
        "source=$source forced=${state.isForcedFirstLaunchOnboarding}"
    )
    return closeRuntimeOverlays(state).copy(isForcedFirstLaunchOnboarding = false)
}

internal fun shouldShowRuntimeShellScrim(state: RuntimeShellState): Boolean =
    shouldShowBaseRuntimeScrim(state.toBaseRuntimeShellCoreState())

internal fun resolveRuntimeShellScrimAlpha(state: RuntimeShellState): Float =
    resolveBaseRuntimeScrimAlpha(state.toBaseRuntimeShellCoreState())

internal fun shouldShowRuntimeIdleComposerHint(
    state: RuntimeShellState,
    isImeVisible: Boolean
): Boolean = shouldShowBaseRuntimeIdleComposerHint(state.toBaseRuntimeShellCoreState(), isImeVisible)

internal fun shouldAutoOpenRuntimeSchedulerStartupTeaser(
    state: RuntimeShellState,
    isImeVisible: Boolean,
    teaserPending: Boolean
): Boolean = shouldAutoOpenRuntimeSchedulerWhenShellIsClear(
    state = state,
    isImeVisible = isImeVisible,
    pending = teaserPending
)

internal fun shouldAutoOpenRuntimeSchedulerPostOnboardingHandoff(
    state: RuntimeShellState,
    isImeVisible: Boolean,
    handoffPending: Boolean
): Boolean = shouldAutoOpenRuntimeSchedulerWhenShellIsClear(
    state = state,
    isImeVisible = isImeVisible,
    pending = handoffPending
)


internal fun dismissRuntimeSchedulerIslandHint(state: RuntimeShellState): RuntimeShellState =
    if (state.showSchedulerIslandHint) {
        state.copy(showSchedulerIslandHint = false)
    } else {
        state
    }

private fun RuntimeShellState.toBaseRuntimeShellCoreState(): BaseRuntimeShellCoreState =
    BaseRuntimeShellCoreState(
        activeDrawer = when {
            showHistory -> BaseRuntimeDrawerType.HISTORY
            activeDrawer == RuntimeDrawerType.SCHEDULER -> BaseRuntimeDrawerType.SCHEDULER
            activeDrawer == RuntimeDrawerType.AUDIO -> BaseRuntimeDrawerType.AUDIO
            else -> null
        },
        audioDrawerMode = audioDrawerMode.toBaseRuntimeAudioDrawerMode(),
        activeConnectivitySurface = activeConnectivitySurface.toBaseRuntimeConnectivitySurface(),
        showSettings = showSettings
    )

private fun BaseRuntimeShellCoreState.mergeInto(state: RuntimeShellState): RuntimeShellState = state.copy(
    activeDrawer = toRuntimeDrawerType(),
    audioDrawerMode = toRuntimeAudioDrawerMode(),
    activeConnectivitySurface = toRuntimeConnectivitySurface(),
    showHistory = activeDrawer == BaseRuntimeDrawerType.HISTORY,
    showSettings = showSettings,
    connectivityModalAutoOpened = if (activeConnectivitySurface == BaseRuntimeConnectivitySurface.MODAL) {
        state.connectivityModalAutoOpened
    } else {
        false
    }
)

private fun RuntimeAudioDrawerMode.toBaseRuntimeAudioDrawerMode(): BaseRuntimeAudioDrawerMode =
    when (this) {
        RuntimeAudioDrawerMode.BROWSE -> BaseRuntimeAudioDrawerMode.BROWSE
        RuntimeAudioDrawerMode.CHAT_RESELECT -> BaseRuntimeAudioDrawerMode.CHAT_RESELECT
    }

private fun BaseRuntimeShellCoreState.toRuntimeAudioDrawerMode(): RuntimeAudioDrawerMode =
    when (audioDrawerMode) {
        BaseRuntimeAudioDrawerMode.BROWSE -> RuntimeAudioDrawerMode.BROWSE
        BaseRuntimeAudioDrawerMode.CHAT_RESELECT -> RuntimeAudioDrawerMode.CHAT_RESELECT
    }

private fun RuntimeConnectivitySurface?.toBaseRuntimeConnectivitySurface(): BaseRuntimeConnectivitySurface? =
    when (this) {
        RuntimeConnectivitySurface.MODAL -> BaseRuntimeConnectivitySurface.MODAL
        RuntimeConnectivitySurface.SETUP -> BaseRuntimeConnectivitySurface.SETUP
        RuntimeConnectivitySurface.MANAGER -> BaseRuntimeConnectivitySurface.MANAGER
        RuntimeConnectivitySurface.ADD_DEVICE -> BaseRuntimeConnectivitySurface.SETUP
        null -> null
    }

private fun BaseRuntimeShellCoreState.toRuntimeConnectivitySurface(): RuntimeConnectivitySurface? =
    when (activeConnectivitySurface) {
        BaseRuntimeConnectivitySurface.MODAL -> RuntimeConnectivitySurface.MODAL
        BaseRuntimeConnectivitySurface.SETUP -> RuntimeConnectivitySurface.SETUP
        BaseRuntimeConnectivitySurface.MANAGER -> RuntimeConnectivitySurface.MANAGER
        null -> null
    }

private fun BaseRuntimeShellCoreState.toRuntimeDrawerType(): RuntimeDrawerType? =
    when (activeDrawer) {
        BaseRuntimeDrawerType.SCHEDULER -> RuntimeDrawerType.SCHEDULER
        BaseRuntimeDrawerType.AUDIO -> RuntimeDrawerType.AUDIO
        BaseRuntimeDrawerType.HISTORY,
        null -> null
    }

private fun shouldAutoOpenRuntimeSchedulerWhenShellIsClear(
    state: RuntimeShellState,
    isImeVisible: Boolean,
    pending: Boolean
): Boolean = pending &&
    shouldShowRuntimeIdleComposerHint(state, isImeVisible)
