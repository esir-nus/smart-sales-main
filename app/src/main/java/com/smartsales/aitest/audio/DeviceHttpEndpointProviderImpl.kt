package com.smartsales.aitest.audio

// 文件：app/src/main/java/com/smartsales/aitest/audio/DeviceHttpEndpointProviderImpl.kt
// 模块：:app
// 说明：监听连接状态并提供统一的设备 HTTP BaseUrl
// 作者：创建于 2025-11-21

import android.net.Uri
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.media.audiofiles.DeviceHttpEndpointProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class DeviceHttpEndpointProviderImpl @Inject constructor(
    private val connectionManager: DeviceConnectionManager,
    private val dispatchers: DispatcherProvider,
) : DeviceHttpEndpointProvider {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val baseUrlFlow = MutableStateFlow<String?>(null)
    private val queryMutex = Mutex()

    override val deviceBaseUrl: StateFlow<String?> = baseUrlFlow.asStateFlow()

    init {
        observeConnection()
    }

    private fun observeConnection() {
        scope.launch {
            var lastReady = false
            connectionManager.state.collect { state ->
                val ready = state.isReadyForFiles()
                if (!ready) {
                    lastReady = false
                    baseUrlFlow.value = null
                } else {
                    if (!lastReady || baseUrlFlow.value == null) {
                        queryEndpoint()
                    }
                    lastReady = true
                }
            }
        }
    }

    private fun queryEndpoint() {
        scope.launch(dispatchers.io) {
            queryMutex.withLock {
                when (val result = connectionManager.queryNetworkStatus()) {
                    is Result.Success -> {
                        val normalized = buildBaseUrl(result.data.ipAddress)
                        baseUrlFlow.value = normalized
                    }

                    is Result.Error -> {
                        baseUrlFlow.value = null
                    }
                }
            }
        }
    }

    private fun ConnectionState.isReadyForFiles(): Boolean =
        this is ConnectionState.WifiProvisioned || this is ConnectionState.Syncing

    private fun buildBaseUrl(host: String?): String? {
        val trimmed = host?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        val parsed = runCatching { Uri.parse(withScheme) }.getOrNull() ?: return null
        val finalHost = parsed.host ?: return null
        val scheme = parsed.scheme ?: "http"
        val port = if (parsed.port == -1) DEFAULT_MEDIA_SERVER_PORT else parsed.port
        return "$scheme://$finalHost:$port"
    }

    private companion object {
        private const val DEFAULT_MEDIA_SERVER_PORT = 8000
    }
}
