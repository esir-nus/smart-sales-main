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
        pollingStarted = false
        pollingStopped = false
        _status.value = BadgeStatus.UNKNOWN
    }
}
