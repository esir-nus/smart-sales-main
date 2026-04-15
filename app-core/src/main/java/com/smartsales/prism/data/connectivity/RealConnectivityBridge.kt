package com.smartsales.prism.data.connectivity

import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.BadgeHttpClient
import com.smartsales.prism.data.connectivity.legacy.BadgeHttpException
import com.smartsales.prism.data.connectivity.legacy.ConnectionState
import com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.PhoneWifiProvider
import com.smartsales.prism.data.connectivity.legacy.PhoneWifiSnapshot
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeState
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeStateMonitor
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeStatus
import com.smartsales.prism.data.connectivity.legacy.hasUsableBadgeIp
import com.smartsales.prism.data.connectivity.legacy.normalizeWifiSsid
import com.smartsales.prism.data.connectivity.legacy.toBadgeDownloadFilename
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.RecordingNotification
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real ConnectivityBridge — 包装 legacy DeviceConnectionManager 和 BadgeHttpClient
 * 
 * 端点处理策略：当前连接期内复用内存中的活动端点快照
 * - BLE 前台查询只在连接/重连/修复/快照失效时触发
 * - HTTP `/list` `/download` `/delete` 复用同一运行期端点
 * - 避免在 ESP32 上重复触发 `wifi#address#ip#name`
 * 
 * @see docs/cerb/connectivity-bridge/spec.md Wave 2
 */
@Singleton
class RealConnectivityBridge @Inject constructor(
    private val deviceManager: DeviceConnectionManager,
    private val httpClient: BadgeHttpClient,
    private val badgeStateMonitor: BadgeStateMonitor,
    private val phoneWifiProvider: PhoneWifiProvider
) : ConnectivityBridge {
    
    companion object {
        private const val TAG = "AudioPipeline"
    }
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val endpointMutex = Mutex()
    private var activeEndpointSnapshot: ActiveEndpointSnapshot? = null
    private var endpointRefreshRequired = false
    private val recordingNotificationsFlow = MutableSharedFlow<RecordingNotification>(
        replay = 0,
        extraBufferCapacity = 1
    )
    private val audioRecordingNotificationsFlow = MutableSharedFlow<RecordingNotification.AudioRecordingReady>(
        replay = 0,
        extraBufferCapacity = 3
    )

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            deviceManager.recordingReadyEvents.collect { filename ->
                if (!isTransportReadyForRecordingNotifications()) {
                    android.util.Log.w(
                        TAG,
                        "🚫 Dropping recording notification while transport not ready: $filename"
                    )
                    return@collect
                }
                recordingNotificationsFlow.emit(
                    RecordingNotification.RecordingReady(filename.toBadgeDownloadFilename())
                )
            }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            deviceManager.audioRecordingReadyEvents.collect { filename ->
                if (!isTransportReadyForRecordingNotifications()) {
                    android.util.Log.w(
                        TAG,
                        "🚫 Dropping audio recording notification while transport not ready: $filename"
                    )
                    return@collect
                }
                audioRecordingNotificationsFlow.emit(
                    RecordingNotification.AudioRecordingReady(filename)
                )
            }
        }
    }
    
    
    override val connectionState: StateFlow<BadgeConnectionState> = combine(
        deviceManager.state,
        badgeStateMonitor.status
    ) { legacyState, badgeStatus ->
        mapToPrismState(legacyState, badgeStatus)
    }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = mapToPrismState(
                legacy = deviceManager.state.value,
                badgeStatus = badgeStateMonitor.status.value
            )
        )

    override val managerStatus: StateFlow<BadgeManagerStatus> = combine(
        deviceManager.state,
        badgeStateMonitor.status
    ) { legacyState, badgeStatus ->
        mapToManagerStatus(legacyState, badgeStatus)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = mapToManagerStatus(
            legacy = deviceManager.state.value,
            badgeStatus = badgeStateMonitor.status.value
        )
    )
    
    override suspend fun downloadRecording(
        filename: String,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
    ): WavDownloadResult {
        val baseUrl = resolveBaseUrl() ?: return WavDownloadResult.Error(
            code = WavDownloadResult.ErrorCode.NOT_CONNECTED,
            message = "无法获取设备IP — 请确认 Badge 已连接 WiFi"
        )
        android.util.Log.d(TAG, "⬇️ Downloading: $baseUrl/download?file=$filename")
        val tempFile = File.createTempFile("badge_recording_", ".wav")

        return when (val result = httpClient.downloadWav(baseUrl, filename, tempFile, onProgress)) {
            is Result.Success -> WavDownloadResult.Success(
                localFile = tempFile,
                originalFilename = filename,
                sizeBytes = tempFile.length()
            )
            is Result.Error -> {
                invalidateActiveEndpoint("download failed")
                android.util.Log.w(TAG, "❌ Download failed from $baseUrl: ${result.throwable.message}")
                mapHttpError(result.throwable)
            }
        }
    }
    
    override suspend fun listRecordings(): Result<List<String>> {
        val baseUrl = resolveBaseUrl() ?: return Result.Error(
            Exception("无法获取设备IP — 请确认 Badge 已连接 WiFi")
        )
        return httpClient.listWavFiles(baseUrl).also { result ->
            if (result is Result.Error) {
                invalidateActiveEndpoint("list failed")
            }
        }
    }
    
    
    override fun recordingNotifications(): Flow<RecordingNotification> =
        recordingNotificationsFlow.asSharedFlow()

    override fun audioRecordingNotifications(): Flow<RecordingNotification.AudioRecordingReady> =
        audioRecordingNotificationsFlow.asSharedFlow()
    
    
    override suspend fun isReady(): Boolean {
        android.util.Log.d(TAG, "🔎 isReady preflight: start")
        val baseUrl = resolveBaseUrl() ?: run {
            android.util.Log.d(TAG, "🔎 isReady preflight: result=not-ready reason=base-url-unresolved")
            return false
        }
        android.util.Log.d(TAG, "🔎 isReady preflight: checking reachability baseUrl=$baseUrl")
        val reachable = httpClient.isReachable(baseUrl)
        if (!reachable) {
            invalidateActiveEndpoint("http reachability failed")
        }
        android.util.Log.d(
            TAG,
            "🔎 isReady preflight: result=${if (reachable) "ready" else "not-ready"} baseUrl=$baseUrl"
        )
        return reachable
    }
    
    override suspend fun deleteRecording(filename: String): Boolean {
        val baseUrl = resolveBaseUrl() ?: return false
        return when (httpClient.deleteWav(baseUrl, filename)) {
            is Result.Success -> true
            is Result.Error -> {
                invalidateActiveEndpoint("delete failed")
                false
            }
        }
    }
    
    private fun mapToPrismState(
        legacy: ConnectionState,
        badgeStatus: BadgeStatus
    ): BadgeConnectionState {
        return when (legacy) {
            is ConnectionState.Disconnected -> BadgeConnectionState.Disconnected
            is ConnectionState.NeedsSetup -> BadgeConnectionState.NeedsSetup

            is ConnectionState.Pairing -> BadgeConnectionState.Connecting

            is ConnectionState.Connected,
            is ConnectionState.AutoReconnecting -> {
                if (badgeStatus.hasSharedTransportReadiness()) {
                    BadgeConnectionState.Connected(
                        badgeIp = badgeStatus.ipAddress ?: "pending",
                        ssid = badgeStatus.wifiName ?: extractSsid(legacy)
                    )
                } else {
                    BadgeConnectionState.Connecting
                }
            }

            is ConnectionState.WifiProvisioned,
            is ConnectionState.Syncing -> {
                BadgeConnectionState.Connected(
                    badgeIp = badgeStatus.ipAddress ?: "pending",
                    ssid = badgeStatus.wifiName ?: extractSsid(legacy)
                )
            }

            is ConnectionState.Error -> BadgeConnectionState.Error(legacy.error.toString())
        }
    }

    private fun mapToManagerStatus(
        legacy: ConnectionState,
        badgeStatus: BadgeStatus
    ): BadgeManagerStatus {
        return when (legacy) {
            is ConnectionState.NeedsSetup -> BadgeManagerStatus.NeedsSetup
            is ConnectionState.Disconnected -> when {
                badgeStatus.bleConnected && badgeStatus.state == BadgeState.OFFLINE ->
                    BadgeManagerStatus.BlePairedNetworkOffline
                badgeStatus.bleConnected && badgeStatus.state == BadgeState.PAIRED ->
                    BadgeManagerStatus.BlePairedNetworkUnknown
                badgeStatus.state == BadgeState.UNKNOWN ->
                    BadgeManagerStatus.Unknown
                else -> BadgeManagerStatus.Disconnected
            }
            is ConnectionState.Pairing,
            is ConnectionState.Connected,
            is ConnectionState.AutoReconnecting -> BadgeManagerStatus.Connecting
            is ConnectionState.WifiProvisioned -> BadgeManagerStatus.Ready(
                ssid = legacy.status.wifiSsid
            )
            is ConnectionState.Syncing -> BadgeManagerStatus.Ready(
                ssid = legacy.status.wifiSsid
            )
            is ConnectionState.Error -> BadgeManagerStatus.Error(legacy.error.toString())
        }
    }
    
    private fun extractSsid(state: ConnectionState): String {
        return when (state) {
            is ConnectionState.Connected -> state.session.peripheralName
            is ConnectionState.WifiProvisioned -> state.status.wifiSsid
            is ConnectionState.Syncing -> state.status.wifiSsid
            else -> "Unknown"
        }
    }

    private fun BadgeStatus.hasSharedTransportReadiness(): Boolean {
        return bleConnected &&
            state == BadgeState.CONNECTED &&
            hasUsableBadgeIp(ipAddress)
    }

    private fun isTransportReadyForRecordingNotifications(): Boolean {
        return when (deviceManager.state.value) {
            is ConnectionState.WifiProvisioned,
            is ConnectionState.Syncing -> true
            is ConnectionState.Connected,
            is ConnectionState.AutoReconnecting -> badgeStateMonitor.status.value.hasSharedTransportReadiness()
            else -> false
        }
    }
    
    private fun mapHttpError(throwable: Throwable): WavDownloadResult {
        return when (throwable) {
            is BadgeHttpException.NotFoundException -> WavDownloadResult.Error(
                code = WavDownloadResult.ErrorCode.FILE_NOT_FOUND,
                message = throwable.message ?: "文件不存在"
            )
            is BadgeHttpException.NetworkException -> WavDownloadResult.Error(
                code = WavDownloadResult.ErrorCode.DOWNLOAD_FAILED,
                message = throwable.message ?: "网络错误"
            )
            else -> WavDownloadResult.Error(
                code = WavDownloadResult.ErrorCode.DOWNLOAD_FAILED,
                message = throwable.message ?: "下载失败"
            )
        }
    }
    
    /**
     * Reuses the active runtime endpoint whenever possible so HTTP sync work
     * does not repeatedly trigger BLE Wi‑Fi status queries on ESP32.
     */
    private suspend fun resolveBaseUrl(): String? {
        return endpointMutex.withLock {
            val phoneSsid = currentPhoneSsid()
            activeEndpointSnapshot
                ?.takeIf { snapshot ->
                    snapshot.matches(
                        runtimeKey = currentRuntimeKey(),
                        phoneSsid = phoneSsid,
                        refreshRequired = endpointRefreshRequired
                    )
                }
                ?.let { snapshot ->
                    android.util.Log.d(TAG, "🌐 resolveBaseUrl: reusing active endpoint ${snapshot.baseUrl}")
                    return@withLock snapshot.baseUrl
                }

            if (activeEndpointSnapshot != null) {
                endpointRefreshRequired = true
                activeEndpointSnapshot = null
            }

            monitorSnapshot(phoneSsid)
                ?.takeUnless { endpointRefreshRequired }
                ?.let { snapshot ->
                    activeEndpointSnapshot = snapshot
                    android.util.Log.d(TAG, "🌐 resolveBaseUrl: seeded active endpoint ${snapshot.baseUrl}")
                    return@withLock snapshot.baseUrl
                }

            android.util.Log.d(TAG, "🌐 resolveBaseUrl: querying badge network status")
            val networkStatus = when (val result = deviceManager.queryNetworkStatus()) {
                is Result.Success -> {
                    android.util.Log.d(
                        TAG,
                        "🌐 resolveBaseUrl: ip=${result.data.ipAddress} badgeSsid=${result.data.deviceWifiName} phoneSsid=${result.data.phoneWifiName}"
                    )
                    result.data
                }
                is Result.Error -> {
                    android.util.Log.w(TAG, "❌ resolveBaseUrl: 无法查询设备网络状态 — ${result.throwable.message}")
                    return@withLock null
                }
            }
            if (!hasUsableBadgeIp(networkStatus.ipAddress)) {
                android.util.Log.w(
                    TAG,
                    "❌ resolveBaseUrl: Badge IP 无效 (${networkStatus.ipAddress}) — Badge 未连 WiFi 或不在同一网络"
                )
                return@withLock null
            }
            if (!isPhoneAlignedWithBadge(phoneSsid, networkStatus.deviceWifiName)) {
                android.util.Log.w(
                    TAG,
                    "❌ resolveBaseUrl: badge ssid=${networkStatus.deviceWifiName} does not match phone ssid=$phoneSsid"
                )
                endpointRefreshRequired = true
                return@withLock null
            }

            val runtimeKey = currentRuntimeKey()
                ?: ActiveEndpointRuntimeKey(
                    peripheralId = "untracked",
                    secureToken = networkStatus.rawResponse
                )
            val snapshot = ActiveEndpointSnapshot(
                runtimeKey = runtimeKey,
                phoneSsid = phoneSsid,
                badgeIp = networkStatus.ipAddress,
                badgeSsid = networkStatus.deviceWifiName.takeIf { it.isNotBlank() },
                baseUrl = "http://${networkStatus.ipAddress}:8088"
            )
            activeEndpointSnapshot = snapshot
            endpointRefreshRequired = false
            android.util.Log.d(TAG, "🌐 resolveBaseUrl: refreshed ${snapshot.baseUrl}")
            snapshot.baseUrl
        }
    }

    private fun monitorSnapshot(phoneSsid: String?): ActiveEndpointSnapshot? {
        val runtimeKey = currentRuntimeKey() ?: return null
        val badgeStatus = badgeStateMonitor.status.value
        val ip = badgeStatus.ipAddress
        if (badgeStatus.state != BadgeState.CONNECTED || !hasUsableBadgeIp(ip)) {
            return null
        }
        if (!isPhoneAlignedWithBadge(phoneSsid, badgeStatus.wifiName)) {
            return null
        }
        return ActiveEndpointSnapshot(
            runtimeKey = runtimeKey,
            phoneSsid = phoneSsid,
            badgeIp = ip ?: return null,
            badgeSsid = badgeStatus.wifiName,
            baseUrl = "http://${ip}:8088"
        )
    }

    private fun currentRuntimeKey(): ActiveEndpointRuntimeKey? {
        return when (val state = deviceManager.state.value) {
            is ConnectionState.WifiProvisioned -> ActiveEndpointRuntimeKey(
                peripheralId = state.session.peripheralId,
                secureToken = state.session.secureToken
            )
            is ConnectionState.Syncing -> ActiveEndpointRuntimeKey(
                peripheralId = state.session.peripheralId,
                secureToken = state.session.secureToken
            )
            else -> null
        }
    }

    private fun currentPhoneSsid(): String? {
        return when (val snapshot = phoneWifiProvider.currentWifiSnapshot()) {
            PhoneWifiSnapshot.Unavailable -> null
            is PhoneWifiSnapshot.Connected -> snapshot.normalizedSsid
        }
    }

    private fun invalidateActiveEndpoint(reason: String) {
        android.util.Log.w(TAG, "🌐 invalidate active endpoint reason=$reason")
        activeEndpointSnapshot = null
        endpointRefreshRequired = true
    }

    private fun isPhoneAlignedWithBadge(
        phoneSsid: String?,
        badgeSsidRaw: String?
    ): Boolean {
        val badgeSsid = normalizeWifiSsid(badgeSsidRaw)
        return phoneSsid == null || badgeSsid == null || badgeSsid == phoneSsid
    }

    private data class ActiveEndpointRuntimeKey(
        val peripheralId: String,
        val secureToken: String
    )

    private data class ActiveEndpointSnapshot(
        val runtimeKey: ActiveEndpointRuntimeKey,
        val phoneSsid: String?,
        val badgeIp: String,
        val badgeSsid: String?,
        val baseUrl: String
    ) {
        fun matches(
            runtimeKey: ActiveEndpointRuntimeKey?,
            phoneSsid: String?,
            refreshRequired: Boolean
        ): Boolean {
            if (refreshRequired || runtimeKey == null || this.runtimeKey != runtimeKey) {
                return false
            }
            if (phoneSsid != null && this.phoneSsid != null && phoneSsid != this.phoneSsid) {
                return false
            }
            return hasUsableBadgeIp(badgeIp)
        }
    }
}
