package com.smartsales.prism.data.connectivity.legacy.badge

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.ConnectivityLogger
import com.smartsales.prism.data.connectivity.legacy.ConnectivityScope
import com.smartsales.prism.data.connectivity.legacy.DeviceNetworkStatus
import com.smartsales.prism.data.connectivity.legacy.WifiProvisioner
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Monitors badge connectivity state with rate-limited polling.
 *
 * Key features:
 * - Rate limiting: min 5s between queries to protect ESP32
 * - Explicit offline detection (0.0.0.0 response)
 * - Automatic stop after consecutive failures
 * - Passive observer pattern (IoC compliant)
 */
interface BadgeStateMonitor {
    /** Observable badge status */
    val status: StateFlow<BadgeStatus>

    /** Called when BLE connection is established */
    fun onBleConnected(session: BleSession)

    /** Called when BLE connection is lost */
    fun onBleDisconnected()

    /** Start rate-limited polling loop */
    fun startPolling()

    /** Stop the polling loop */
    fun stopPolling()

    companion object {
        /** Normal polling interval (10 seconds) */
        const val POLL_INTERVAL_MS = 10_000L

        /** Minimum gap between polls (5 seconds) - protects ESP32 */
        const val MIN_POLL_GAP_MS = 5_000L

        /** Stop polling after this many consecutive failures */
        const val MAX_CONSECUTIVE_FAILURES = 3
    }
}

/**
 * Real implementation of [BadgeStateMonitor].
 */
@Singleton
class RealBadgeStateMonitor @Inject constructor(
    private val provisioner: WifiProvisioner,
    private val dispatchers: DispatcherProvider,
    @ConnectivityScope private val scope: CoroutineScope
) : BadgeStateMonitor {

    private val _status = MutableStateFlow(BadgeStatus.UNKNOWN)
    override val status: StateFlow<BadgeStatus> = _status.asStateFlow()

    private var pollingJob: Job? = null
    private var lastPollMs = 0L
    private var currentSession: BleSession? = null

    override fun onBleConnected(session: BleSession) {
        currentSession = session
        _status.value = BadgeStatus(
            state = BadgeState.PAIRED,
            bleConnected = true,
            lastCheckMs = System.currentTimeMillis()
        )
        ConnectivityLogger.i("📶 BLE connected, starting poll")
        startPolling()
    }

    override fun onBleDisconnected() {
        currentSession = null
        stopPolling()
        _status.value = BadgeStatus.UNKNOWN
        ConnectivityLogger.i("📶 BLE disconnected")
    }

    override fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch(dispatchers.io) {
            while (isActive) {
                val now = System.currentTimeMillis()

                // Rate limiting: enforce minimum gap
                val elapsed = now - lastPollMs
                if (elapsed < BadgeStateMonitor.MIN_POLL_GAP_MS) {
                    delay(BadgeStateMonitor.MIN_POLL_GAP_MS - elapsed)
                }

                val session = currentSession
                if (session == null) {
                    ConnectivityLogger.d("📶 No session, stopping poll")
                    break
                }

                lastPollMs = System.currentTimeMillis()
                pollNetworkStatus(session)

                // Stop polling after too many consecutive failures
                if (_status.value.consecutiveFailures >= BadgeStateMonitor.MAX_CONSECUTIVE_FAILURES) {
                    ConnectivityLogger.w("⚠️ Max failures reached, stopping poll")
                    break
                }

                delay(BadgeStateMonitor.POLL_INTERVAL_MS)
            }
        }
    }

    override fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        lastPollMs = 0L
    }

    private suspend fun pollNetworkStatus(session: BleSession) {
        when (val result = provisioner.queryNetworkStatus(session)) {
            is Result.Success -> handleSuccess(result.data)
            is Result.Error -> handleError()
        }
    }

    private fun handleSuccess(networkStatus: DeviceNetworkStatus) {
        val ip = networkStatus.ipAddress
        val isOffline = ip.isBlank() || ip == "0.0.0.0"
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
        ConnectivityLogger.w("⚠️ poll failed, count=${_status.value.consecutiveFailures}")
    }
}
