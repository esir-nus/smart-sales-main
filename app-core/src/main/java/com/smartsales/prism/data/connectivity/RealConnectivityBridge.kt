package com.smartsales.prism.data.connectivity

import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.BadgeHttpClient
import com.smartsales.prism.data.connectivity.legacy.BadgeHttpException
import com.smartsales.prism.data.connectivity.legacy.ConnectionState
import com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.PhoneWifiProvider
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeState
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeStateMonitor
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeStatus
import com.smartsales.prism.data.connectivity.legacy.hasUsableBadgeIp
import com.smartsales.prism.data.connectivity.legacy.toBadgeDownloadFilename
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.ConnectivityPrompt
import com.smartsales.prism.domain.connectivity.IsolationTriggerContext
import com.smartsales.prism.domain.connectivity.RecordingNotification
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import com.smartsales.prism.domain.connectivity.WifiRepairEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    private val endpointRecoveryCoordinator: BadgeEndpointRecoveryCoordinator,
    private val phoneWifiProvider: PhoneWifiProvider,
    private val connectivityPrompt: ConnectivityPrompt,
) : ConnectivityBridge {
    
    companion object {
        private const val TAG = "AudioPipeline"
        private const val POST_CREDENTIAL_GRACE_ATTEMPTS = 3
    }
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val endpointMutex = Mutex()
    private var activeEndpointSnapshot: ActiveEndpointSnapshot? = null
    private var endpointRefreshRequired = false
    // 保存最后已知的徽章 IP，用于断开时隔离探测
    @Volatile private var lastKnownBadgeIp: String? = null
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
            deviceManager.state
                .map(::runtimeKeyForState)
                .distinctUntilChanged()
                .collect { runtimeKey ->
                    endpointRecoveryCoordinator.noteCurrentRuntimeKey(runtimeKey)
                }
        }
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

        // 断开时隔离探测：当徽章从已配网状态意外断开时，延迟 1s 检查是否为 AP 客户端隔离
        // 探测仅使用单次 HTTP 请求，不触发 BLE 查询，满足 ESP32 280K RAM 约束
        scope.launch {
            var prevLegacyState: ConnectionState = deviceManager.state.value
            deviceManager.state.collect { current ->
                val wasProvisioned = prevLegacyState is ConnectionState.WifiProvisioned ||
                    prevLegacyState is ConnectionState.Syncing
                val isNowDisconnected = current is ConnectionState.Disconnected
                prevLegacyState = current

                if (wasProvisioned && isNowDisconnected) {
                    val ip = lastKnownBadgeIp
                    lastKnownBadgeIp = null
                    if (ip != null) {
                        delay(1_000L) // 等待断开状态稳定后再探测
                        runIsolationProbeIfSuspected(
                            badgeIp = ip,
                            httpClient = httpClient,
                            phoneWifiProvider = phoneWifiProvider,
                            connectivityPrompt = connectivityPrompt,
                            triggerContext = IsolationTriggerContext.ON_DISCONNECT
                        )
                    }
                }
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

    override fun batteryNotifications(): Flow<Int> = deviceManager.batteryEvents

    override fun firmwareVersionNotifications(): Flow<String> = deviceManager.firmwareVersionEvents

    override fun sdCardSpaceNotifications(): Flow<String> = deviceManager.sdCardSpaceEvents

    override suspend fun requestFirmwareVersion(): Boolean = deviceManager.requestFirmwareVersion()

    override suspend fun requestSdCardSpace(): Boolean = deviceManager.requestSdCardSpace()

    override suspend fun notifyCommandEnd() = deviceManager.notifyCommandEnd()

    
    override suspend fun isReady(): Boolean {
        android.util.Log.d(TAG, "🔎 isReady preflight: start")
        val runtimeKey = currentRuntimeKey()
        val hasGrace = endpointRecoveryCoordinator.hasPendingPostCredentialGrace(runtimeKey)
        if (!hasGrace) {
            return probeReadyOnce(allowRefreshRetry = true)
        }
        val activeRuntimeKey = runtimeKey ?: return probeReadyOnce()

        repeat(POST_CREDENTIAL_GRACE_ATTEMPTS) { attempt ->
            val delayMs = endpointRecoveryCoordinator.consumePostCredentialProbeDelayMs(
                runtimeKey = activeRuntimeKey,
                allowImplicitArm = false
            ) ?: return@repeat
            android.util.Log.i(
                TAG,
                "🛜 post-credential readiness grace wait=${delayMs}ms attempt=${attempt + 1} runtime=${activeRuntimeKey.toLogString()}"
            )
            delay(delayMs)
            if (probeReadyOnce(allowRefreshRetry = false)) {
                endpointRecoveryCoordinator.clearPostCredentialGrace(activeRuntimeKey)
                return true
            }
        }

        endpointRecoveryCoordinator.clearPostCredentialGrace(activeRuntimeKey)
        return false
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
            is ConnectionState.WifiProvisionedHttpDelayed,
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
            is ConnectionState.WifiProvisionedHttpDelayed -> BadgeManagerStatus.Ready(
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
            activeEndpointSnapshot
                ?.takeIf { snapshot ->
                    snapshot.matches(
                        runtimeKey = currentRuntimeKey(),
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

            monitorSnapshot()
                ?.takeUnless { endpointRefreshRequired }
                ?.let { snapshot ->
                    activeEndpointSnapshot = snapshot
                    lastKnownBadgeIp = snapshot.badgeIp
                    endpointRecoveryCoordinator.noteResolvedEndpoint(
                        BadgeEndpointSnapshot(
                            runtimeKey = snapshot.runtimeKey,
                            badgeIp = snapshot.badgeIp,
                            baseUrl = snapshot.baseUrl
                        )
                    )
                    android.util.Log.d(TAG, "🌐 resolveBaseUrl: seeded active endpoint ${snapshot.baseUrl}")
                    return@withLock snapshot.baseUrl
                }

            android.util.Log.d(TAG, "🌐 resolveBaseUrl: querying badge network status")
            val networkStatus = when (val result = deviceManager.queryNetworkStatus()) {
                is Result.Success -> {
                    android.util.Log.d(
                        TAG,
                        "🌐 resolveBaseUrl: ip=${result.data.ipAddress} badgeSsid=${result.data.deviceWifiName} phoneSsidForDiag=${result.data.phoneWifiName}"
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

            val runtimeKey = currentRuntimeKey()
                ?: BadgeRuntimeKey(
                    peripheralId = "untracked",
                    secureToken = networkStatus.rawResponse
                )
            val snapshot = ActiveEndpointSnapshot(
                runtimeKey = runtimeKey,
                badgeIp = networkStatus.ipAddress,
                badgeSsid = networkStatus.deviceWifiName.takeIf { it.isNotBlank() },
                baseUrl = "http://${networkStatus.ipAddress}:8088"
            )
            activeEndpointSnapshot = snapshot
            lastKnownBadgeIp = snapshot.badgeIp
            endpointRefreshRequired = false
            endpointRecoveryCoordinator.noteResolvedEndpoint(
                BadgeEndpointSnapshot(
                    runtimeKey = snapshot.runtimeKey,
                    badgeIp = snapshot.badgeIp,
                    baseUrl = snapshot.baseUrl
                )
            )
            android.util.Log.d(TAG, "🌐 resolveBaseUrl: refreshed ${snapshot.baseUrl}")
            snapshot.baseUrl
        }
    }

    private fun monitorSnapshot(): ActiveEndpointSnapshot? {
        val runtimeKey = currentRuntimeKey() ?: return null
        val badgeStatus = badgeStateMonitor.status.value
        val ip = badgeStatus.ipAddress
        if (badgeStatus.state != BadgeState.CONNECTED || !hasUsableBadgeIp(ip)) {
            return null
        }
        return ActiveEndpointSnapshot(
            runtimeKey = runtimeKey,
            badgeIp = ip ?: return null,
            badgeSsid = badgeStatus.wifiName,
            baseUrl = "http://${ip}:8088"
        )
    }

    private fun currentRuntimeKey(): BadgeRuntimeKey? {
        return runtimeKeyForState(deviceManager.state.value)
    }

    private fun runtimeKeyForState(state: ConnectionState): BadgeRuntimeKey? {
        return when (state) {
            is ConnectionState.WifiProvisioned -> BadgeRuntimeKey(
                peripheralId = state.session.peripheralId,
                secureToken = state.session.secureToken
            )
            is ConnectionState.Syncing -> BadgeRuntimeKey(
                peripheralId = state.session.peripheralId,
                secureToken = state.session.secureToken
            )
            else -> null
        }
    }

    private suspend fun probeReadyOnce(allowRefreshRetry: Boolean = false): Boolean {
        val baseUrl = resolveBaseUrl() ?: run {
            android.util.Log.d(TAG, "🔎 isReady preflight: result=not-ready reason=base-url-unresolved")
            return false
        }
        android.util.Log.d(TAG, "🔎 isReady preflight: checking reachability baseUrl=$baseUrl")
        val reachable = httpClient.isReachable(baseUrl)
        if (!reachable) {
            invalidateActiveEndpoint("http reachability failed")
            if (allowRefreshRetry) {
                android.util.Log.d(TAG, "🔎 isReady preflight: retrying after endpoint refresh")
                return probeReadyOnce(allowRefreshRetry = false)
            }
        }
        android.util.Log.d(
            TAG,
            "🔎 isReady preflight: result=${if (reachable) "ready" else "not-ready"} baseUrl=$baseUrl"
        )
        return reachable
    }

    override fun wifiRepairEvents(): Flow<WifiRepairEvent> = deviceManager.wifiRepairEvents

    private fun invalidateActiveEndpoint(reason: String) {
        android.util.Log.w(TAG, "🌐 invalidate active endpoint reason=$reason")
        activeEndpointSnapshot = null
        endpointRefreshRequired = true
    }

    private data class ActiveEndpointSnapshot(
        val runtimeKey: BadgeRuntimeKey,
        val badgeIp: String,
        val badgeSsid: String?,
        val baseUrl: String
    ) {
        fun matches(
            runtimeKey: BadgeRuntimeKey?,
            refreshRequired: Boolean
        ): Boolean {
            if (refreshRequired || runtimeKey == null || this.runtimeKey != runtimeKey) {
                return false
            }
            return hasUsableBadgeIp(badgeIp)
        }
    }

}
