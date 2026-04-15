package com.smartsales.prism.ui.sim

import com.smartsales.prism.data.audio.SimBadgeSyncIslandEvent
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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val SIM_DYNAMIC_ISLAND_SCHEDULER_ROTATION_MILLIS = 5_000L
private const val SIM_DYNAMIC_ISLAND_SESSION_TITLE_ROTATION_MILLIS = 3_000L
private const val SIM_DYNAMIC_ISLAND_CONNECTED_INTERRUPT_LOCK_MILLIS = 5_000L
private const val SIM_DYNAMIC_ISLAND_DISCONNECTED_INTERRUPT_LOCK_MILLIS = 3_000L
private const val SIM_DYNAMIC_ISLAND_HEARTBEAT_INTERVAL_MILLIS = 30_000L
private const val SIM_DYNAMIC_ISLAND_CONNECTED_HEARTBEAT_DWELL_MILLIS = 5_000L
private const val SIM_DYNAMIC_ISLAND_DISCONNECTED_HEARTBEAT_DWELL_MILLIS = 2_500L
private const val SIM_DYNAMIC_ISLAND_SYNC_DWELL_MILLIS = 3_000L

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
    takeoverSuppressed: StateFlow<Boolean>,
    syncEvents: SharedFlow<SimBadgeSyncIslandEvent>
) : AutoCloseable {

    private val coordinatorJob = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + coordinatorJob)

    private val _presentation = MutableStateFlow(SimShellDynamicIslandPresentation())
    val presentation: StateFlow<SimShellDynamicIslandPresentation> = _presentation.asStateFlow()

    private var currentSchedulerItems: List<DynamicIslandItem> = schedulerItems.value
    private var currentSchedulerItemKey: String? = currentSchedulerItems.firstOrNull()?.stableKey
    private var sessionTitleInterruptToken: Int = 0
    private var currentConnectivityState: ConnectionState = connectivityState.value
    private var currentBatteryLevel: Int = batteryLevel.value
    private var isTakeoverSuppressed: Boolean = takeoverSuppressed.value
    private var activeConnectivityLane: ConnectionState? = resolvePersistentConnectivityLane(currentConnectivityState)
    private var activeConnectivityLanePersistent: Boolean = activeConnectivityLane != null
    private var deferredTransientConnectivityLane: ConnectionState? = null
    private var activeConnectivityLaneToken: Long = 0L
    private var schedulerRotationToken: Long = 0L
    private var transientConnectivityJob: Job? = null
    private var heartbeatJob: Job? = null
    // SYNC 通道状态
    private var activeSyncItem: DynamicIslandItem? = null
    private var deferredSyncEvent: SimBadgeSyncIslandEvent? = null
    private var transientSyncJob: Job? = null

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
                schedulerRotationToken += 1L
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
                val becameUnsuppressed = isTakeoverSuppressed && !suppressed
                isTakeoverSuppressed = suppressed
                if (becameUnsuppressed) {
                    if (revealDeferredTransientConnectivityLaneIfNeeded()) return@collect
                    revealDeferredSyncEventIfNeeded()
                }
                updatePresentation()
            }
        }

        scope.launch {
            syncEvents.collect { event ->
                handleSyncEvent(event)
            }
        }

        scope.launch {
            while (isActive) {
                val token = schedulerRotationToken
                delay(resolveVisibleSchedulerDwellMillis())
                if (schedulerRotationToken == token) {
                    rotateSchedulerLaneIfVisible()
                }
            }
        }
    }

    override fun close() {
        scope.cancel()
    }

    fun updateSessionTitleInterruptToken(token: Int) {
        if (token == sessionTitleInterruptToken) return
        sessionTitleInterruptToken = token
        currentSchedulerItems.firstOrNull { it.isSessionTitleItem }?.let { titleItem ->
            currentSchedulerItemKey = titleItem.stableKey
            schedulerRotationToken += 1L
            updatePresentation()
        }
    }

    private fun handleConnectivityStateChanged(state: ConnectionState) {
        restartHeartbeatLoop()
        val persistentLane = resolvePersistentConnectivityLane(state)
        if (persistentLane != null) {
            // 持久性连接状态 → SYNC 被抢占并静默丢弃
            clearActiveSyncLane()
            clearDeferredSyncEvent()
            clearDeferredTransientConnectivityLane()
            showPersistentConnectivityLane(persistentLane)
            return
        }

        clearPersistentConnectivityLane()
        if (state == ConnectionState.CONNECTED || state == ConnectionState.DISCONNECTED) {
            if (isTakeoverSuppressed) {
                deferredTransientConnectivityLane = state
                updatePresentation()
            } else {
                // 瞬时连接事件抢占 SYNC
                clearActiveSyncLane()
                startTransientConnectivityLane(
                    state = state,
                    durationMillis = resolveInterruptDwellMillis(state)
                )
            }
        } else {
            clearDeferredTransientConnectivityLane()
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
        schedulerRotationToken += 1L
        updatePresentation()
    }

    private fun resolveVisibleSchedulerDwellMillis(): Long {
        val visibleItem = _presentation.value.visibleItem
        return if (visibleItem?.isSessionTitleItem == true) {
            SIM_DYNAMIC_ISLAND_SESSION_TITLE_ROTATION_MILLIS
        } else {
            SIM_DYNAMIC_ISLAND_SCHEDULER_ROTATION_MILLIS
        }
    }

    private fun showPersistentConnectivityLane(state: ConnectionState) {
        transientConnectivityJob?.cancel()
        transientConnectivityJob = null
        clearDeferredTransientConnectivityLane()
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
        clearDeferredTransientConnectivityLane()
        clearActiveSyncLane()
        activeConnectivityLanePersistent = false
        activeConnectivityLane = state
        activeConnectivityLaneToken += 1L
        val token = activeConnectivityLaneToken
        updatePresentation()
        transientConnectivityJob = scope.launch {
            delay(durationMillis)
            if (!activeConnectivityLanePersistent && activeConnectivityLaneToken == token) {
                activeConnectivityLane = null
                revealDeferredSyncEventIfNeeded()
                updatePresentation()
            }
        }
    }

    private fun revealDeferredTransientConnectivityLaneIfNeeded(): Boolean {
        val deferredState = deferredTransientConnectivityLane ?: return false
        if (resolvePersistentConnectivityLane(currentConnectivityState) != null ||
            currentConnectivityState != deferredState
        ) {
            clearDeferredTransientConnectivityLane()
            return false
        }
        startTransientConnectivityLane(
            state = deferredState,
            durationMillis = resolveInterruptDwellMillis(deferredState)
        )
        return true
    }

    private fun clearDeferredTransientConnectivityLane() {
        deferredTransientConnectivityLane = null
    }

    private fun handleSyncEvent(event: SimBadgeSyncIslandEvent) {
        // 持久性连接通道（RECONNECTING/NEEDS_SETUP）— SYNC 静默丢弃
        if (activeConnectivityLanePersistent) return

        // 瞬时连接通道激活时 — SYNC 延迟至通道结束后显示
        if (activeConnectivityLane != null) {
            deferredSyncEvent = event
            return
        }

        if (isTakeoverSuppressed) {
            deferredSyncEvent = event
            return
        }

        showTransientSyncLane(event)
    }

    private fun showTransientSyncLane(event: SimBadgeSyncIslandEvent) {
        val item = buildSyncLaneItem(event)
        // 最新事件替换旧事件（latest-wins）
        transientSyncJob?.cancel()
        activeSyncItem = item
        deferredSyncEvent = null
        updatePresentation()
        transientSyncJob = scope.launch {
            delay(SIM_DYNAMIC_ISLAND_SYNC_DWELL_MILLIS)
            activeSyncItem = null
            updatePresentation()
        }
    }

    private fun revealDeferredSyncEventIfNeeded() {
        val event = deferredSyncEvent ?: return
        deferredSyncEvent = null
        if (activeConnectivityLane != null || activeConnectivityLanePersistent) return
        showTransientSyncLane(event)
    }

    private fun clearActiveSyncLane() {
        transientSyncJob?.cancel()
        transientSyncJob = null
        activeSyncItem = null
    }

    private fun clearDeferredSyncEvent() {
        deferredSyncEvent = null
    }

    private fun restartHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(SIM_DYNAMIC_ISLAND_HEARTBEAT_INTERVAL_MILLIS)
                if (canShowConnectivityHeartbeat()) {
                    startTransientConnectivityLane(
                        state = currentConnectivityState,
                        durationMillis = resolveHeartbeatDwellMillis(currentConnectivityState)
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
            !isTakeoverSuppressed && activeSyncItem != null -> {
                activeSyncItem
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

private fun resolveInterruptDwellMillis(state: ConnectionState): Long {
    return when (state) {
        ConnectionState.CONNECTED -> SIM_DYNAMIC_ISLAND_CONNECTED_INTERRUPT_LOCK_MILLIS
        ConnectionState.DISCONNECTED -> SIM_DYNAMIC_ISLAND_DISCONNECTED_INTERRUPT_LOCK_MILLIS
        else -> SIM_DYNAMIC_ISLAND_DISCONNECTED_INTERRUPT_LOCK_MILLIS
    }
}

private fun resolveHeartbeatDwellMillis(state: ConnectionState): Long {
    return when (state) {
        ConnectionState.CONNECTED -> SIM_DYNAMIC_ISLAND_CONNECTED_HEARTBEAT_DWELL_MILLIS
        ConnectionState.DISCONNECTED -> SIM_DYNAMIC_ISLAND_DISCONNECTED_HEARTBEAT_DWELL_MILLIS
        else -> SIM_DYNAMIC_ISLAND_DISCONNECTED_HEARTBEAT_DWELL_MILLIS
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

private fun buildSyncLaneItem(event: SimBadgeSyncIslandEvent): DynamicIslandItem {
    return when (event) {
        is SimBadgeSyncIslandEvent.RecFileDownloaded -> DynamicIslandItem(
            displayText = "已同步：${event.filename}",
            lane = DynamicIslandLane.SYNC,
            visualState = DynamicIslandVisualState.SYNC_COMPLETE,
            tapAction = DynamicIslandTapAction.OpenSchedulerDrawer()
        )
        SimBadgeSyncIslandEvent.ManualSyncStarted -> DynamicIslandItem(
            displayText = "正在同步录音...",
            lane = DynamicIslandLane.SYNC,
            visualState = DynamicIslandVisualState.SYNC_IN_PROGRESS,
            tapAction = DynamicIslandTapAction.OpenSchedulerDrawer()
        )
        is SimBadgeSyncIslandEvent.ManualSyncComplete -> DynamicIslandItem(
            displayText = "已同步 ${event.count} 条录音",
            lane = DynamicIslandLane.SYNC,
            visualState = DynamicIslandVisualState.SYNC_COMPLETE,
            tapAction = DynamicIslandTapAction.OpenSchedulerDrawer()
        )
        SimBadgeSyncIslandEvent.AlreadyUpToDate -> DynamicIslandItem(
            displayText = "已是最新",
            lane = DynamicIslandLane.SYNC,
            visualState = DynamicIslandVisualState.SYNC_UP_TO_DATE,
            tapAction = DynamicIslandTapAction.OpenSchedulerDrawer()
        )
    }
}
