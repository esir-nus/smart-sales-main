package com.smartsales.prism.ui.sim

import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandLane
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.components.DynamicIslandUiState
import com.smartsales.prism.ui.components.DynamicIslandVisualState
import com.smartsales.prism.ui.components.connectivity.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val SIM_DYNAMIC_ISLAND_SCHEDULER_ROTATION_MILLIS = 5_000L
private const val SIM_DYNAMIC_ISLAND_INTERRUPT_LOCK_MILLIS = 3_000L
private const val SIM_DYNAMIC_ISLAND_HEARTBEAT_INTERVAL_MILLIS = 30_000L
private const val SIM_DYNAMIC_ISLAND_HEARTBEAT_DWELL_MILLIS = 2_500L

internal data class SimShellDynamicIslandPresentation(
    val uiState: DynamicIslandUiState = DynamicIslandUiState.Hidden
) {
    val visibleItem: DynamicIslandItem?
        get() = (uiState as? DynamicIslandUiState.Visible)?.item
}

internal class SimShellDynamicIslandCoordinator(
    parentScope: CoroutineScope,
    schedulerItems: StateFlow<List<DynamicIslandItem>>,
    connectivityState: StateFlow<ConnectionState>,
    batteryLevel: StateFlow<Int>,
    takeoverSuppressed: StateFlow<Boolean>
) : AutoCloseable {

    private val coordinatorJob = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + coordinatorJob)

    private val _presentation = MutableStateFlow(SimShellDynamicIslandPresentation())
    val presentation: StateFlow<SimShellDynamicIslandPresentation> = _presentation.asStateFlow()

    private var currentSchedulerItems: List<DynamicIslandItem> = schedulerItems.value
    private var currentSchedulerItemKey: String? = currentSchedulerItems.firstOrNull()?.stableKey
    private var currentConnectivityState: ConnectionState = connectivityState.value
    private var currentBatteryLevel: Int = batteryLevel.value
    private var isTakeoverSuppressed: Boolean = takeoverSuppressed.value
    private var activeConnectivityLane: ConnectionState? = resolvePersistentConnectivityLane(currentConnectivityState)
    private var activeConnectivityLanePersistent: Boolean = activeConnectivityLane != null
    private var activeConnectivityLaneToken: Long = 0L
    private var transientConnectivityJob: Job? = null
    private var heartbeatJob: Job? = null

    init {
        updatePresentation()
        restartHeartbeatLoop()

        scope.launch {
            schedulerItems.collect { items ->
                currentSchedulerItems = items
                currentSchedulerItemKey = when {
                    currentSchedulerItems.isEmpty() -> null
                    currentSchedulerItemKey == null -> currentSchedulerItems.first().stableKey
                    currentSchedulerItems.none { it.stableKey == currentSchedulerItemKey } -> currentSchedulerItems.first().stableKey
                    else -> currentSchedulerItemKey
                }
                updatePresentation()
            }
        }

        scope.launch {
            connectivityState.drop(1).collect { state ->
                currentConnectivityState = state
                handleConnectivityStateChanged(state)
            }
        }

        scope.launch {
            batteryLevel.collect { level ->
                currentBatteryLevel = level
                updatePresentation()
            }
        }

        scope.launch {
            takeoverSuppressed.collect { suppressed ->
                isTakeoverSuppressed = suppressed
                updatePresentation()
            }
        }

        scope.launch {
            while (isActive) {
                delay(SIM_DYNAMIC_ISLAND_SCHEDULER_ROTATION_MILLIS)
                rotateSchedulerLaneIfVisible()
            }
        }
    }

    override fun close() {
        scope.cancel()
    }

    private fun handleConnectivityStateChanged(state: ConnectionState) {
        restartHeartbeatLoop()
        val persistentLane = resolvePersistentConnectivityLane(state)
        if (persistentLane != null) {
            showPersistentConnectivityLane(persistentLane)
            return
        }

        clearPersistentConnectivityLane()
        if (state == ConnectionState.CONNECTED || state == ConnectionState.DISCONNECTED) {
            startTransientConnectivityLane(
                state = state,
                durationMillis = SIM_DYNAMIC_ISLAND_INTERRUPT_LOCK_MILLIS
            )
        } else {
            updatePresentation()
        }
    }

    private fun rotateSchedulerLaneIfVisible() {
        val visibleItem = _presentation.value.visibleItem ?: return
        if (visibleItem.lane != DynamicIslandLane.SCHEDULER || currentSchedulerItems.size <= 1) {
            return
        }
        val currentIndex = resolveSimDynamicIslandIndex(
            items = currentSchedulerItems,
            currentItemKey = currentSchedulerItemKey
        )
        val nextIndex = (currentIndex + 1) % currentSchedulerItems.size
        currentSchedulerItemKey = currentSchedulerItems[nextIndex].stableKey
        updatePresentation()
    }

    private fun showPersistentConnectivityLane(state: ConnectionState) {
        transientConnectivityJob?.cancel()
        transientConnectivityJob = null
        activeConnectivityLane = state
        activeConnectivityLanePersistent = true
        updatePresentation()
    }

    private fun clearPersistentConnectivityLane() {
        if (!activeConnectivityLanePersistent) {
            return
        }
        activeConnectivityLanePersistent = false
        activeConnectivityLane = null
    }

    private fun startTransientConnectivityLane(
        state: ConnectionState,
        durationMillis: Long
    ) {
        if (isTakeoverSuppressed) {
            return
        }
        transientConnectivityJob?.cancel()
        activeConnectivityLanePersistent = false
        activeConnectivityLane = state
        activeConnectivityLaneToken += 1L
        val token = activeConnectivityLaneToken
        updatePresentation()
        transientConnectivityJob = scope.launch {
            delay(durationMillis)
            if (!activeConnectivityLanePersistent && activeConnectivityLaneToken == token) {
                activeConnectivityLane = null
                updatePresentation()
            }
        }
    }

    private fun restartHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(SIM_DYNAMIC_ISLAND_HEARTBEAT_INTERVAL_MILLIS)
                if (canShowConnectivityHeartbeat()) {
                    startTransientConnectivityLane(
                        state = currentConnectivityState,
                        durationMillis = SIM_DYNAMIC_ISLAND_HEARTBEAT_DWELL_MILLIS
                    )
                }
            }
        }
    }

    private fun canShowConnectivityHeartbeat(): Boolean {
        return !isTakeoverSuppressed &&
            activeConnectivityLane == null &&
            (currentConnectivityState == ConnectionState.CONNECTED ||
                currentConnectivityState == ConnectionState.DISCONNECTED)
    }

    private fun updatePresentation() {
        val visibleItem = when {
            !isTakeoverSuppressed && activeConnectivityLane != null -> {
                buildConnectivityLaneItem(activeConnectivityLane!!, currentBatteryLevel)
            }
            currentSchedulerItems.isNotEmpty() -> {
                val currentIndex = resolveSimDynamicIslandIndex(
                    items = currentSchedulerItems,
                    currentItemKey = currentSchedulerItemKey
                )
                currentSchedulerItems[currentIndex].also { item ->
                    currentSchedulerItemKey = item.stableKey
                }
            }
            else -> null
        }
        _presentation.value = SimShellDynamicIslandPresentation(
            uiState = visibleItem?.let(DynamicIslandUiState::Visible) ?: DynamicIslandUiState.Hidden
        )
    }
}

private fun resolvePersistentConnectivityLane(state: ConnectionState): ConnectionState? {
    return when (state) {
        ConnectionState.RECONNECTING,
        ConnectionState.NEEDS_SETUP -> state
        else -> null
    }
}

private fun buildConnectivityLaneItem(
    state: ConnectionState,
    batteryLevel: Int
): DynamicIslandItem {
    return when (state) {
        ConnectionState.CONNECTED -> DynamicIslandItem(
            displayText = "Badge 已连接",
            lane = DynamicIslandLane.CONNECTIVITY,
            visualState = DynamicIslandVisualState.CONNECTIVITY_CONNECTED,
            batteryPercentage = batteryLevel.coerceIn(0, 100),
            tapAction = DynamicIslandTapAction.OpenConnectivityEntry
        )
        ConnectionState.DISCONNECTED -> DynamicIslandItem(
            displayText = "Badge 已断开",
            lane = DynamicIslandLane.CONNECTIVITY,
            visualState = DynamicIslandVisualState.CONNECTIVITY_DISCONNECTED,
            tapAction = DynamicIslandTapAction.OpenConnectivityEntry
        )
        ConnectionState.RECONNECTING -> DynamicIslandItem(
            displayText = "Badge 重连中...",
            lane = DynamicIslandLane.CONNECTIVITY,
            visualState = DynamicIslandVisualState.CONNECTIVITY_RECONNECTING,
            tapAction = DynamicIslandTapAction.OpenConnectivityEntry
        )
        ConnectionState.NEEDS_SETUP -> DynamicIslandItem(
            displayText = "Badge 需要配网",
            lane = DynamicIslandLane.CONNECTIVITY,
            visualState = DynamicIslandVisualState.CONNECTIVITY_NEEDS_SETUP,
            tapAction = DynamicIslandTapAction.OpenConnectivityEntry
        )
        else -> DynamicIslandItem(
            displayText = "Badge 已断开",
            lane = DynamicIslandLane.CONNECTIVITY,
            visualState = DynamicIslandVisualState.CONNECTIVITY_DISCONNECTED,
            tapAction = DynamicIslandTapAction.OpenConnectivityEntry
        )
    }
}
