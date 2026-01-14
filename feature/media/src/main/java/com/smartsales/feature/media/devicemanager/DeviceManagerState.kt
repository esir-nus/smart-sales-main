package com.smartsales.feature.media.devicemanager

// 文件：feature/media/src/main/java/com/smartsales/feature/media/devicemanager/DeviceManagerState.kt
// 模块：:feature:media
// 说明：设备管理器的数据类定义（从 DeviceManagerViewModel 提取）
// 作者：提取于 2026-01-06

import android.net.Uri
import com.smartsales.feature.media.GifTransferState
import com.smartsales.feature.media.WavListState
import com.smartsales.feature.media.WavDownloadState

internal const val DEFAULT_MEDIA_SERVER_PORT = 8088
internal const val DEFAULT_MEDIA_SERVER_BASE_URL = "http://10.0.2.2:$DEFAULT_MEDIA_SERVER_PORT"
internal const val AUTO_DETECT_WAITING_MESSAGE = "等待设备联网..."

/**
 * 设备管理器 UI 状态
 */
data class DeviceManagerUiState(
    val connectionStatus: DeviceConnectionUiState = DeviceConnectionUiState.Disconnected(),
    val isConnected: Boolean = false,
    val canRetryConnect: Boolean = true,
    val canStartSetup: Boolean = false,
    val baseUrl: String = DEFAULT_MEDIA_SERVER_BASE_URL,
    val autoDetectedBaseUrl: String? = null,
    val isAutoDetectingBaseUrl: Boolean = false,
    val autoDetectStatus: String = AUTO_DETECT_WAITING_MESSAGE,
    val baseUrlWasManual: Boolean = false,
    val hasResolvedBaseUrl: Boolean = false,
    val files: List<DeviceFileUi> = emptyList(),
    val visibleFiles: List<DeviceFileUi> = emptyList(),
    val selectedFile: DeviceFileUi? = null,
    val viewerFile: DeviceFileUi? = null,
    val applyInProgressId: String? = null,
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val errorMessage: String? = null,
    val loadErrorMessage: String? = null,
    
    // New coordinator states
    val gifTransferState: GifTransferState? = null,
    val wavListState: WavListState? = null,
    val wavDownloadState: WavDownloadState? = null
)

/**
 * 设备文件 UI 模型
 */
data class DeviceFileUi(
    val id: String,
    val displayName: String,
    val sizeText: String,
    val mimeType: String,
    val mediaType: DeviceMediaTab,
    val mediaLabel: String,
    val modifiedAtText: String,
    val mediaUrl: String,
    val downloadUrl: String,
    val isApplied: Boolean = false,
    val durationText: String? = null,
    val thumbnailUrl: String? = null
)

/**
 * 设备媒体类型标签
 */
enum class DeviceMediaTab {
    Videos,
    Gifs,
    Images,
    Audio;
}

/**
 * 设备媒体文件（来自网关）
 */
data class DeviceMediaFile(
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val modifiedAtMillis: Long,
    val mediaUrl: String,
    val downloadUrl: String,
    val durationMillis: Long? = null,
    val location: String? = null,
    val source: String? = null
)

/**
 * 设备上传源
 */
sealed interface DeviceUploadSource {
    data class AndroidUri(val uri: Uri) : DeviceUploadSource
}

/**
 * 设备管理器事件
 */
sealed interface DeviceManagerEvent {
    data object NavigateToDeviceSetup : DeviceManagerEvent
}

/**
 * 设备连接 UI 状态
 */
sealed class DeviceConnectionUiState {
    data class Disconnected(val reason: String? = null) : DeviceConnectionUiState()
    data class Connecting(val detail: String? = null) : DeviceConnectionUiState()
    data class Connected(val deviceName: String? = null) : DeviceConnectionUiState()

    fun isReadyForFiles(): Boolean = this is Connected
}
