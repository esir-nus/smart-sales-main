package com.smartsales.aitest.audio

// 文件：app/src/main/java/com/smartsales/aitest/audio/DeviceHttpEndpointProviderImpl.kt
// 模块：:app
// 说明：监听连接状态并提供统一的设备 HTTP BaseUrl
// 作者：创建于 2025-11-21

import android.net.Uri
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.media.audiofiles.DeviceHttpEndpointProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

@Singleton
class DeviceHttpEndpointProviderImpl @Inject constructor(
    private val connectionManager: DeviceConnectionManager,
    private val dispatchers: DispatcherProvider,
) : DeviceHttpEndpointProvider {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val baseUrlFlow = MutableStateFlow<String?>(null)
    private val queryMutex = Mutex()
    private var discoveryJob: Job? = null
    private var readyToken: String? = null
    private var hasAttemptedForToken = false

    override val deviceBaseUrl: Flow<String?> = baseUrlFlow.onSubscription {
        requestDiscovery(force = true)
    }

    init {
        observeConnection()
    }

    private fun observeConnection() {
        scope.launch {
            connectionManager.state.collect { state ->
                val token = state.readyToken()
                if (token == null) {
                    clearReadyState()
                    return@collect
                }
                handleReadyState(token)
            }
        }
    }

    private fun handleReadyState(token: String) {
        if (token != readyToken) {
            readyToken = token
            hasAttemptedForToken = false
            baseUrlFlow.value = null
            cancelDiscoveryJob()
        }
        requestDiscovery(force = false)
    }

    private fun requestDiscovery(force: Boolean) {
        if (readyToken == null) return
        if (baseUrlFlow.value != null) return
        if (discoveryJob?.isActive == true) return
        if (!force && hasAttemptedForToken) return

        hasAttemptedForToken = true
        discoveryJob = scope.launch(dispatchers.io) {
            try {
                runDiscoveryWithRetry()
            } finally {
                discoveryJob = null
            }
        }
    }

    private suspend fun runDiscoveryWithRetry() {
        var attempt = 0
        while (attempt < MAX_ATTEMPTS && coroutineContext.isActive) {
            attempt += 1
            val success = queryEndpointOnce()
            if (success) {
                return
            }
            if (attempt < MAX_ATTEMPTS) {
                val delayMillis = backoffDelay(attempt)
                delay(delayMillis)
            }
        }
        Log.w(TAG, "查询设备网络状态失败，已达到最大重试次数 ($MAX_ATTEMPTS)")
    }

    private suspend fun queryEndpointOnce(): Boolean =
        queryMutex.withLock {
            val currentToken = readyToken
            if (currentToken == null) {
                return@withLock false
            }
            when (val result = connectionManager.queryNetworkStatus()) {
                is Result.Success -> {
                    val normalized = buildBaseUrl(result.data.ipAddress)
                    if (normalized != null && currentToken == readyToken) {
                        baseUrlFlow.value = normalized
                    }
                    normalized != null
                }

                is Result.Error -> {
                    baseUrlFlow.value = null
                    false
                }
            }
        }

    private fun clearReadyState() {
        cancelDiscoveryJob()
        readyToken = null
        hasAttemptedForToken = false
        baseUrlFlow.value = null
    }

    private fun cancelDiscoveryJob() {
        discoveryJob?.cancel()
        discoveryJob = null
    }

    private fun ConnectionState.readyToken(): String? = when (this) {
        is ConnectionState.WifiProvisioned -> session.secureToken
        is ConnectionState.Syncing -> session.secureToken
        else -> null
    }

    private fun buildBaseUrl(host: String?): String? {
        val trimmed = host?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
        val parsed = runCatching { Uri.parse(withScheme) }.getOrNull()
        val finalHost = parsed?.host ?: trimmed.removePrefix("http://").removePrefix("https://").substringBefore("/")
        if (finalHost.isBlank()) return null
        val scheme = parsed?.scheme ?: "http"
        val port = parsed?.let { if (it.port == -1) DEFAULT_MEDIA_SERVER_PORT else it.port } ?: DEFAULT_MEDIA_SERVER_PORT
        return "$scheme://$finalHost:$port"
    }

    private fun backoffDelay(attempt: Int): Long =
        RETRY_DELAYS_MS.getOrElse(attempt - 1) { RETRY_DELAYS_MS.last() }

    @VisibleForTesting
    internal fun cancelForTest() {
        scope.cancel()
    }

    private companion object {
        private const val DEFAULT_MEDIA_SERVER_PORT = 8000
        private const val MAX_ATTEMPTS = 3
        // 重试节奏：首次立即，其后 1s、2s 退避，避免频繁敲打设备
        private val RETRY_DELAYS_MS = longArrayOf(1_000L, 2_000L)
        private const val TAG = "DeviceHttpEndpoint"
    }
}
