package com.smartsales.prism.data.connectivity.legacy.badge

import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.ConnectivityLogger
import com.smartsales.prism.data.connectivity.legacy.DeviceNetworkStatus
import com.smartsales.prism.data.connectivity.legacy.hasUsableBadgeIp
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks the latest explicit badge connectivity observation.
 *
 * Key features:
 * - Explicit offline detection (0.0.0.0 response)
 * - Passive observer pattern (IoC compliant)
 * - No autonomous polling; callers must publish foreground query results
 */
interface BadgeStateMonitor {
    /** Observable badge status */
    val status: StateFlow<BadgeStatus>

    /** Called when BLE connection is established */
    fun onBleConnected(session: BleSession)

    /** Called when BLE connection is lost */
    fun onBleDisconnected()

    /** Called when a foreground network query produced a concrete result */
    fun onNetworkStatusObserved(networkStatus: DeviceNetworkStatus)

    /** Called when a foreground network query failed while BLE remains connected */
    fun onNetworkStatusQueryFailed()

    /** Legacy no-op hook retained for compatibility */
    fun startPolling()

    /** Legacy no-op hook retained for compatibility */
    fun stopPolling()
}

/**
 * Real implementation of [BadgeStateMonitor].
 */
@Singleton
class RealBadgeStateMonitor @Inject constructor(
) : BadgeStateMonitor {

    private val _status = MutableStateFlow(BadgeStatus.UNKNOWN)
    override val status: StateFlow<BadgeStatus> = _status.asStateFlow()

    override fun onBleConnected(session: BleSession) {
        _status.value = BadgeStatus(
            state = BadgeState.PAIRED,
            bleConnected = true,
            lastCheckMs = System.currentTimeMillis()
        )
        ConnectivityLogger.i("📶 BLE connected, waiting for explicit network observation")
    }

    override fun onBleDisconnected() {
        _status.value = BadgeStatus.UNKNOWN
        ConnectivityLogger.i("📶 BLE disconnected")
    }

    override fun onNetworkStatusObserved(networkStatus: DeviceNetworkStatus) {
        handleSuccess(networkStatus)
    }

    override fun onNetworkStatusQueryFailed() {
        handleError()
    }

    override fun startPolling() = Unit

    override fun stopPolling() = Unit

    private fun handleSuccess(networkStatus: DeviceNetworkStatus) {
        val ip = networkStatus.ipAddress
        val isOffline = !hasUsableBadgeIp(ip)
        val newState = if (isOffline) BadgeState.OFFLINE else BadgeState.CONNECTED

        _status.value = BadgeStatus(
            state = newState,
            ipAddress = if (isOffline) null else ip,
            wifiName = networkStatus.deviceWifiName.takeIf { it.isNotBlank() },
            lastCheckMs = System.currentTimeMillis(),
            bleConnected = true,
            consecutiveFailures = 0
        )

        ConnectivityLogger.d("📶 state=$newState ip=$ip")
    }

    private fun handleError() {
        val current = _status.value
        _status.value = current.copy(
            consecutiveFailures = current.consecutiveFailures + 1,
            lastCheckMs = System.currentTimeMillis()
        )
        ConnectivityLogger.w("⚠️ status query failed, count=${_status.value.consecutiveFailures}")
    }
}
