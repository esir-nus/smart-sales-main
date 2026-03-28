package com.smartsales.prism.data.connectivity

import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.BadgeHttpClient
import com.smartsales.prism.data.connectivity.legacy.BadgeHttpException
import com.smartsales.prism.data.connectivity.legacy.ConnectionState
import com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeState
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeStateMonitor
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeStatus
import com.smartsales.prism.data.connectivity.legacy.toBadgeDownloadFilename
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.RecordingNotification
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real ConnectivityBridge — 包装 legacy DeviceConnectionManager 和 BadgeHttpClient
 * 
 * IP 处理策略：按需查询（on-demand query）
 * - ConnectionState 不包含 IP
 * - 通过 DeviceManager.queryNetworkStatus() 获取 DeviceNetworkStatus.ipAddress
 * - 每次操作前查询，确保 IP 始终最新
 * 
 * @see docs/cerb/connectivity-bridge/spec.md Wave 2
 */
@Singleton
class RealConnectivityBridge @Inject constructor(
    private val deviceManager: DeviceConnectionManager,
    private val httpClient: BadgeHttpClient,
    private val badgeStateMonitor: BadgeStateMonitor
) : ConnectivityBridge {
    
    companion object {
        private const val TAG = "AudioPipeline"
    }
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    
    override val connectionState: StateFlow<BadgeConnectionState> = deviceManager.state
        .map { legacyState -> mapToPrismState(legacyState) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = mapToPrismState(deviceManager.state.value)
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
    
    override suspend fun downloadRecording(filename: String): WavDownloadResult {
        val baseUrl = resolveBaseUrl() ?: return WavDownloadResult.Error(
            code = WavDownloadResult.ErrorCode.NOT_CONNECTED,
            message = "无法获取设备IP — 请确认 Badge 已连接 WiFi"
        )
        android.util.Log.d(TAG, "⬇️ Downloading: $baseUrl/download?file=$filename")
        val tempFile = File.createTempFile("badge_recording_", ".wav")
        
        return when (val result = httpClient.downloadWav(baseUrl, filename, tempFile)) {
            is Result.Success -> WavDownloadResult.Success(
                localFile = tempFile,
                originalFilename = filename,
                sizeBytes = tempFile.length()
            )
            is Result.Error -> {
                android.util.Log.w(TAG, "❌ Download failed from $baseUrl: ${result.throwable.message}")
                mapHttpError(result.throwable)
            }
        }
    }
    
    override suspend fun listRecordings(): Result<List<String>> {
        val baseUrl = resolveBaseUrl() ?: return Result.Error(
            Exception("无法获取设备IP — 请确认 Badge 已连接 WiFi")
        )
        return httpClient.listWavFiles(baseUrl)
    }
    
    
    override fun recordingNotifications(): Flow<RecordingNotification> =
        deviceManager.recordingReadyEvents.map { filename ->
            RecordingNotification.RecordingReady(filename.toBadgeDownloadFilename())
        }
    
    
    override suspend fun isReady(): Boolean {
        android.util.Log.d(TAG, "🔎 isReady preflight: start")
        val baseUrl = resolveBaseUrl() ?: run {
            android.util.Log.d(TAG, "🔎 isReady preflight: result=not-ready reason=base-url-unresolved")
            return false
        }
        android.util.Log.d(TAG, "🔎 isReady preflight: checking reachability baseUrl=$baseUrl")
        val reachable = httpClient.isReachable(baseUrl)
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
            is Result.Error -> false
        }
    }
    
    private fun mapToPrismState(legacy: ConnectionState): BadgeConnectionState {
        return when (legacy) {
            is ConnectionState.Disconnected -> BadgeConnectionState.Disconnected
            is ConnectionState.NeedsSetup -> BadgeConnectionState.NeedsSetup
            
            is ConnectionState.Pairing,
            is ConnectionState.Connected,
            is ConnectionState.AutoReconnecting -> BadgeConnectionState.Connecting
            
            is ConnectionState.WifiProvisioned,
            is ConnectionState.Syncing -> {
                // NOTE: badgeIp = "pending" is a placeholder
                // Real IP is queried on-demand by downloadRecording/deleteRecording/isReady
                // This avoids stale IP and extra network calls in state mapping
                BadgeConnectionState.Connected(
                    badgeIp = "pending",
                    ssid = extractSsid(legacy)
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
     * Extract duplicated pattern: query network status → build base URL.
     * Eliminates 3× copy-paste in downloadRecording/isReady/deleteRecording.
     */
    private suspend fun resolveBaseUrl(): String? {
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
                return null
            }
        }
        if (networkStatus.ipAddress == "0.0.0.0" || networkStatus.ipAddress.isBlank()) {
            android.util.Log.w(TAG, "❌ resolveBaseUrl: Badge IP 无效 (${networkStatus.ipAddress}) — Badge 未连 WiFi 或不在同一网络")
            return null
        }
        return "http://${networkStatus.ipAddress}:8088".also { baseUrl ->
            android.util.Log.d(TAG, "🌐 resolveBaseUrl: resolved $baseUrl")
        }
    }
}
