package com.smartsales.feature.media

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.feature.connectivity.DeviceConnectionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 文件：feature/media/src/main/java/com/smartsales/feature/media/MediaSyncCoordinator.kt
// 模块：:feature:media
// 说明：定义媒体同步协调器并提供依赖连接状态的模拟实现
// 作者：创建于 2025-11-15
data class MediaClip(
    val id: String,
    val title: String,
    val customer: String,
    val recordedAtMillis: Long,
    val durationSeconds: Int,
    val sourceDeviceName: String,
    val status: MediaClipStatus,
    val transcriptSource: String?,
    val mediaFileName: String? = null
)

enum class MediaClipStatus {
    Ready,
    Uploading,
    Failed
}

data class MediaSyncState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val syncing: Boolean = false,
    val lastSyncedAtMillis: Long? = null,
    val items: List<MediaClip> = emptyList(),
    val errorMessage: String? = null
)

interface MediaSyncCoordinator {
    val state: StateFlow<MediaSyncState>
    suspend fun triggerSync(): Result<Unit>
}

@Singleton
class FakeMediaSyncCoordinator @Inject constructor(
    private val connectionManager: DeviceConnectionManager,
    private val dispatchers: DispatcherProvider
) : MediaSyncCoordinator {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private var clipCounter = 0

    private val internalState = MutableStateFlow(defaultState())
    override val state: StateFlow<MediaSyncState> = internalState.asStateFlow()

    init {
        scope.launch {
            connectionManager.state.collect { connection ->
                internalState.update { current ->
                    current.copy(
                        connectionState = connection,
                        errorMessage = if (connection.isReadyForMedia()) null else current.errorMessage
                    )
                }
            }
        }
    }

    override suspend fun triggerSync(): Result<Unit> = withContext(dispatchers.default) {
        val connection = connectionManager.state.value
        if (!connection.isReadyForMedia()) {
            val reason = connection.blockedReason()
            internalState.update { it.copy(errorMessage = reason) }
            return@withContext Result.Error(IllegalStateException(reason))
        }
        if (internalState.value.syncing) {
            return@withContext Result.Error(IllegalStateException("媒体同步正在进行中"))
        }

        internalState.update { it.copy(syncing = true, errorMessage = null) }
        delay(SYNC_DELAY_MS)
        val newClip = buildNewClip(connection)
        internalState.update { current ->
            val updatedItems = (listOf(newClip) + current.items)
                .distinctBy { it.id }
                .take(MAX_CLIP_COUNT)
            current.copy(
                items = updatedItems,
                syncing = false,
                lastSyncedAtMillis = System.currentTimeMillis()
            )
        }
        Result.Success(Unit)
    }

    private fun defaultState(): MediaSyncState {
        val initialClips = seedClips()
        return MediaSyncState(
            connectionState = connectionManager.state.value,
            items = initialClips
        )
    }

    private fun seedClips(): List<MediaClip> =
        listOf(
            SeedClip("门店巡访语音", "Acme 华东", SAMPLE_TINGWU_SOURCE, SAMPLE_LOCAL_MEDIA),
            SeedClip("新品宣导音频", "Beta 华北", null, null)
        ).map { clip ->
            clipCounter += 1
            MediaClip(
                id = "clip-$clipCounter",
                title = clip.title,
                customer = clip.customer,
                recordedAtMillis = System.currentTimeMillis() - clipCounter * 45 * 60_000L,
                durationSeconds = 60 + clipCounter * 20,
                sourceDeviceName = "演示设备",
                status = MediaClipStatus.Ready,
                transcriptSource = clip.transcriptSource,
                mediaFileName = clip.mediaFileName
            )
        }

    private fun buildNewClip(connection: ConnectionState): MediaClip {
        clipCounter += 1
        val now = System.currentTimeMillis()
        val customer = SAMPLE_CUSTOMERS[clipCounter % SAMPLE_CUSTOMERS.size]
        val duration = 40 + (clipCounter * 15 % 120)
        val title = "巡访录音 #$clipCounter"
        val deviceName = when (connection) {
            is ConnectionState.WifiProvisioned -> connection.session.peripheralName
            is ConnectionState.Syncing -> connection.session.peripheralName
            else -> "设备"
        }
        return MediaClip(
            id = "clip-$clipCounter",
            title = title,
            customer = customer,
            recordedAtMillis = now - clipCounter * 20 * 60_000L,
            durationSeconds = duration,
            sourceDeviceName = deviceName,
            status = MediaClipStatus.Ready,
            transcriptSource = null,
            mediaFileName = null
        )
    }

    private fun ConnectionState.isReadyForMedia(): Boolean =
        this is ConnectionState.WifiProvisioned || this is ConnectionState.Syncing

    private fun ConnectionState.blockedReason(): String = when (this) {
        ConnectionState.Disconnected -> "设备未连接，无法同步媒体。"
        is ConnectionState.Connected -> "BLE 已连接，等待设备加入 Wi-Fi 网络。"
        is ConnectionState.Pairing -> "正在配对 ${deviceName}，请稍后再试。"
        is ConnectionState.Error -> when (val err = error) {
            is ConnectivityError.PairingInProgress -> "配对冲突：${err.deviceName} 已在使用。"
            is ConnectivityError.ProvisioningFailed -> err.reason
            is ConnectivityError.PermissionDenied -> "权限不足：${err.permissions.joinToString()}。"
            is ConnectivityError.Timeout -> "连接超时，等待自动重试。"
            is ConnectivityError.Transport -> err.reason
            ConnectivityError.MissingSession -> "当前没有有效的配对会话。"
        }

        is ConnectionState.WifiProvisioned,
        is ConnectionState.Syncing -> "连接已就绪，可同步媒体。"
    }

    private companion object {
        private const val SYNC_DELAY_MS = 600L
        private const val MAX_CLIP_COUNT = 6
        private val SAMPLE_CUSTOMERS = listOf(
            "Acme 华东",
            "Beta 旗舰店",
            "Ceta 南区",
            "Delta 社区店"
        )
        private const val SAMPLE_TINGWU_SOURCE =
            "audio_files/20251028_110444_20251028_110444_Recording_8.mp3"
        private const val SAMPLE_LOCAL_MEDIA = "Recording (8).mp3"

        private data class SeedClip(
            val title: String,
            val customer: String,
            val transcriptSource: String?,
            val mediaFileName: String?
        )
    }
}
