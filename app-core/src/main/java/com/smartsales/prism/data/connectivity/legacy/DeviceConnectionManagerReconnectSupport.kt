package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.core.util.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DeviceConnectionManagerReconnectSupport(
    private val dispatchers: DispatcherProvider,
    private val scope: CoroutineScope,
    private val runtime: DeviceConnectionManagerRuntime,
    private val connectionSupport: DeviceConnectionManagerConnectionSupport
) {

    fun scheduleAutoReconnectIfNeeded() {
        val credsReady = connectionSupport.hasStoredSession()
        if (!credsReady) {
            runtime.state.value = ConnectionState.NeedsSetup
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
        launchReconnect(ignoreBackoff = true)
    }

    suspend fun reconnectAndWait(): ConnectionState = withContext(dispatchers.io) {
        val sessionSnapshot = connectionSupport.currentSessionOrNull()
        if (sessionSnapshot == null) {
            runtime.state.value = ConnectionState.NeedsSetup
            return@withContext ConnectionState.NeedsSetup
        }
        runtime.state.value = ConnectionState.AutoReconnecting(1)
        val outcome = connectionSupport.connectUsingSession(sessionSnapshot)
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
        val sessionSnapshot = connectionSupport.currentSessionOrNull()
        scope.launch(dispatchers.io) {
            runtime.reconnectMeta = runtime.reconnectMeta.copy(lastAttemptMillis = System.currentTimeMillis())
            if (sessionSnapshot == null) {
                runtime.state.value = ConnectionState.NeedsSetup
                return@launch
            }
            when (val outcome = connectionSupport.connectUsingSession(sessionSnapshot)) {
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
    3 -> 30_000L
    else -> 60_000L
}
