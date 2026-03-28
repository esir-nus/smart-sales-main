package com.smartsales.prism.data.connectivity.legacy.badge

import com.smartsales.prism.data.connectivity.legacy.BleSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake implementation of [BadgeStateMonitor] for testing.
 * Records method calls for verification.
 */
class FakeBadgeStateMonitor : BadgeStateMonitor {
    private val _status = MutableStateFlow(BadgeStatus.UNKNOWN)
    override val status: StateFlow<BadgeStatus> = _status

    /** Records all sessions that triggered onBleConnected */
    val connectedSessions = mutableListOf<String>()

    /** Number of times onBleDisconnected was called */
    var disconnectCount = 0
        private set

    /** Number of times foreground query failure was reported */
    var queryFailureCount = 0
        private set

    /** Whether startPolling was called */
    var pollingStarted = false
        private set

    /** Whether stopPolling was called */
    var pollingStopped = false
        private set

    override fun onBleConnected(session: BleSession) {
        connectedSessions.add(session.peripheralId)
        _status.value = BadgeStatus(
            state = BadgeState.PAIRED,
            bleConnected = true,
            lastCheckMs = System.currentTimeMillis()
        )
    }

    override fun onBleDisconnected() {
        disconnectCount++
        _status.value = BadgeStatus.UNKNOWN
    }

    override fun onNetworkStatusObserved(networkStatus: com.smartsales.prism.data.connectivity.legacy.DeviceNetworkStatus) {
        val ip = networkStatus.ipAddress
        val isOffline = ip.isBlank() || ip == "0.0.0.0" || ip.startsWith("0.")
        _status.value = BadgeStatus(
            state = if (isOffline) BadgeState.OFFLINE else BadgeState.CONNECTED,
            ipAddress = if (isOffline) null else ip,
            wifiName = networkStatus.deviceWifiName.takeIf { it.isNotBlank() },
            bleConnected = true,
            consecutiveFailures = 0,
            lastCheckMs = System.currentTimeMillis()
        )
    }

    override fun onNetworkStatusQueryFailed() {
        queryFailureCount++
        _status.value = _status.value.copy(
            bleConnected = true,
            consecutiveFailures = _status.value.consecutiveFailures + 1,
            lastCheckMs = System.currentTimeMillis()
        )
    }

    override fun startPolling() {
        pollingStarted = true
    }

    override fun stopPolling() {
        pollingStopped = true
    }

    // Test helpers

    /** Set status directly for testing */
    fun setStatus(newStatus: BadgeStatus) {
        _status.value = newStatus
    }

    /** Simulate transition to CONNECTED state */
    fun simulateConnected(ip: String, wifiName: String? = null) {
        _status.value = BadgeStatus(
            state = BadgeState.CONNECTED,
            ipAddress = ip,
            wifiName = wifiName,
            lastCheckMs = System.currentTimeMillis(),
            bleConnected = true
        )
    }

    /** Simulate transition to OFFLINE state */
    fun simulateOffline() {
        _status.value = BadgeStatus(
            state = BadgeState.OFFLINE,
            bleConnected = true,
            lastCheckMs = System.currentTimeMillis()
        )
    }

    /** Reset all recorded state */
    fun reset() {
        connectedSessions.clear()
        disconnectCount = 0
        queryFailureCount = 0
        pollingStarted = false
        pollingStopped = false
        _status.value = BadgeStatus.UNKNOWN
    }
}
