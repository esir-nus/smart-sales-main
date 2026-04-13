package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.core.util.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal class DeviceConnectionManagerReconnectSupport(
    private val dispatchers: DispatcherProvider,
    private val scope: CoroutineScope,
    private val runtime: DeviceConnectionManagerRuntime,
    private val connectionSupport: DeviceConnectionManagerConnectionSupport,
    private val hasBlePermission: () -> Boolean
) {

    fun scheduleAutoReconnectIfNeeded() {
        val credsReady = connectionSupport.hasStoredSession()
        if (!credsReady) {
            runtime.state.value = ConnectionState.NeedsSetup
            return
        }
        if (!hasBlePermission()) {
            ConnectivityLogger.w("🔄 Auto-reconnect skipped: BLUETOOTH_CONNECT permission not granted")
            return
        }
        when (runtime.state.value) {
            is ConnectionState.AutoReconnecting,
            is ConnectionState.Pairing -> return
            else -> Unit
        }
        val now = System.currentTimeMillis()
        val requiredInterval = requiredIntervalFor(runtime.reconnectMeta.failureCount)
        val elapsed = now - runtime.reconnectMeta.lastAttemptMillis
        if (elapsed < requiredInterval) {
            ConnectivityLogger.d("🔄 Backoff: ${elapsed}ms < ${requiredInterval}ms")
            return
        }
        launchReconnect()
    }

    fun forceReconnectNow() {
        if (!connectionSupport.hasStoredSession()) {
            runtime.state.value = ConnectionState.NeedsSetup
            return
        }
        if (!hasBlePermission()) {
            ConnectivityLogger.w("🔄 Force reconnect skipped: BLUETOOTH_CONNECT permission not granted")
            return
        }
        launchReconnect(ignoreBackoff = true)
    }

    suspend fun reconnectAndWait(): ConnectionState = withContext(dispatchers.io) {
        if (!hasBlePermission()) {
            ConnectivityLogger.w("🔄 reconnectAndWait skipped: BLUETOOTH_CONNECT permission not granted")
            return@withContext ConnectionState.Disconnected
        }
        val sessionSnapshot = connectionSupport.currentSessionOrNull()
        if (sessionSnapshot == null) {
            runtime.state.value = ConnectionState.NeedsSetup
            return@withContext ConnectionState.NeedsSetup
        }
        runtime.state.value = ConnectionState.AutoReconnecting(1)
        val outcome = try {
            withTimeout(RECONNECT_TIMEOUT_MS) {
                connectionSupport.connectUsingSession(sessionSnapshot)
            }
        } catch (_: TimeoutCancellationException) {
            ConnectivityLogger.w("🔄 reconnectAndWait timed out after ${RECONNECT_TIMEOUT_MS}ms")
            ConnectionState.Error(ConnectivityError.Timeout(RECONNECT_TIMEOUT_MS))
        }
        when (outcome) {
            is ConnectionState.WifiProvisioned -> {
                runtime.state.value = outcome
                connectionSupport.startHeartbeat(outcome.session, outcome.status)
                runtime.reconnectMeta = AutoReconnectMeta()
            }
            else -> {
                runtime.state.value = ConnectionState.Disconnected
            }
        }
        outcome
    }

    private fun launchReconnect(ignoreBackoff: Boolean = false) {
        if (ignoreBackoff) {
            runtime.reconnectMeta = runtime.reconnectMeta.copy(lastAttemptMillis = 0L)
        }
        val attempt = runtime.reconnectMeta.failureCount + 1
        runtime.state.value = ConnectionState.AutoReconnecting(attempt)
        scope.launch(dispatchers.io) {
            runtime.reconnectMeta = runtime.reconnectMeta.copy(lastAttemptMillis = System.currentTimeMillis())
            val sessionSnapshot = connectionSupport.currentSessionOrNull()
            if (sessionSnapshot == null) {
                runtime.state.value = ConnectionState.NeedsSetup
                return@launch
            }
            val outcome = try {
                withTimeout(RECONNECT_TIMEOUT_MS) {
                    connectionSupport.connectUsingSession(sessionSnapshot)
                }
            } catch (_: TimeoutCancellationException) {
                ConnectivityLogger.w("🔄 launchReconnect timed out after ${RECONNECT_TIMEOUT_MS}ms")
                ConnectionState.Error(ConnectivityError.Timeout(RECONNECT_TIMEOUT_MS))
            }
            when (outcome) {
                is ConnectionState.WifiProvisioned -> {
                    runtime.state.value = outcome
                    connectionSupport.startHeartbeat(outcome.session, outcome.status)
                    runtime.reconnectMeta = AutoReconnectMeta()
                }
                is ConnectionState.Error -> {
                    val nextFailure = runtime.reconnectMeta.failureCount + 1
                    runtime.reconnectMeta = runtime.reconnectMeta.copy(failureCount = nextFailure)
                    runtime.state.value = outcome
                    runtime.state.value = ConnectionState.Disconnected
                }
                else -> {
                    val nextFailure = runtime.reconnectMeta.failureCount + 1
                    runtime.reconnectMeta = runtime.reconnectMeta.copy(failureCount = nextFailure)
                    runtime.state.value = ConnectionState.Disconnected
                }
            }
        }
    }
}

internal data class AutoReconnectMeta(
    val lastAttemptMillis: Long = 0L,
    val failureCount: Int = 0
)

internal fun requiredIntervalFor(failureCount: Int): Long = when (failureCount) {
    0 -> 0L
    1 -> 5_000L
    2 -> 10_000L
    3 -> 20_000L
    4 -> 40_000L
    else -> 60_000L
}

private const val RECONNECT_TIMEOUT_MS = 35_000L
