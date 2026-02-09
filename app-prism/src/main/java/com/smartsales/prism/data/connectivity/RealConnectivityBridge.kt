package com.smartsales.prism.data.connectivity

import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BadgeHttpClient
import com.smartsales.feature.connectivity.BadgeHttpException
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.RecordingNotification
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private val httpClient: BadgeHttpClient
) : ConnectivityBridge {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    
    override val connectionState: StateFlow<BadgeConnectionState> = deviceManager.state
        .map { legacyState -> mapToPrismState(legacyState) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = BadgeConnectionState.Disconnected
        )
    
    override suspend fun downloadRecording(filename: String): WavDownloadResult {
        val baseUrl = resolveBaseUrl() ?: return WavDownloadResult.Error(
            code = WavDownloadResult.ErrorCode.NOT_CONNECTED,
            message = "无法获取设备IP"
        )
        val tempFile = File.createTempFile("badge_recording_", ".wav")
        
        return when (val result = httpClient.downloadWav(baseUrl, filename, tempFile)) {
            is Result.Success -> WavDownloadResult.Success(
                localFile = tempFile,
                originalFilename = filename,
                sizeBytes = tempFile.length()
            )
            is Result.Error -> mapHttpError(result.throwable)
        }
    }
    
    
    override fun recordingNotifications(): Flow<RecordingNotification> =
        deviceManager.recordingReadyEvents.map { filename ->
            // BLE sends "20260209_142605" (no extension)
            // HTTP download expects "20260209_142605.wav"
            RecordingNotification.RecordingReady("$filename.wav")
        }
    
    
    override suspend fun isReady(): Boolean {
        val baseUrl = resolveBaseUrl() ?: return false
        return httpClient.isReachable(baseUrl)
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
            is ConnectionState.AutoReconnecting -> BadgeConnectionState.Connecting
            
            is ConnectionState.Connected,
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
        val networkStatus = when (val result = deviceManager.queryNetworkStatus()) {
            is Result.Success -> result.data
            is Result.Error -> return null
        }
        return "http://${networkStatus.ipAddress}:8088"
    }
}
