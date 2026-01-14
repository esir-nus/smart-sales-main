package com.smartsales.feature.media.devicemanager

// 文件：feature/media/src/main/java/com/smartsales/feature/media/devicemanager/DeviceManagerViewModel.kt
// 模块：:feature:media
// 说明：管理设备文件列表、上传与应用操作的 ViewModel
// 作者：创建于 2025-11-20

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.media.audiofiles.DeviceHttpEndpointProvider
import com.smartsales.feature.media.GifTransferCoordinator
import com.smartsales.feature.media.WavDownloadCoordinator
import com.smartsales.feature.media.GifTransferState
import com.smartsales.feature.media.WavListState
import com.smartsales.feature.media.WavDownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val AUTO_DETECT_LOADING_MESSAGE = "正在检测设备网络..."
private const val LOAD_ERROR_MESSAGE = "加载设备文件失败，请稍后重试。"

@HiltViewModel
class DeviceManagerViewModel @Inject constructor(
    private val mediaGateway: DeviceMediaGateway,
    private val connectionManager: DeviceConnectionManager,
    private val dispatcherProvider: DispatcherProvider,
    private val endpointProvider: DeviceHttpEndpointProvider,
    private val gifTransferCoordinator: GifTransferCoordinator,
    private val wavDownloadCoordinator: WavDownloadCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceManagerUiState())
    val uiState: StateFlow<DeviceManagerUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<DeviceManagerEvent>()
    val events: SharedFlow<DeviceManagerEvent> = _events.asSharedFlow()

    private var cachedFiles: List<DeviceFileUi> = emptyList()
    private var autoDetectJob: Job? = null

    init {
        observeConnection()
        observeEndpoint()
        connectionManager.scheduleAutoReconnectIfNeeded()
    }

    fun onRefreshFiles() {
        if (!_uiState.value.isConnected) {
            connectionManager.forceReconnectNow()
            _uiState.update {
                it.copy(
                    loadErrorMessage = "设备未连接，无法刷新文件。",
                    isLoading = false,
                    errorMessage = null
                )
            }
            return
        }
        loadFiles(_uiState.value.baseUrl)
    }

    fun onStartSetupClick() {
        viewModelScope.launch(dispatcherProvider.default) {
            _events.emit(DeviceManagerEvent.NavigateToDeviceSetup)
        }
    }

    fun onSelectFile(fileId: String) {
        val candidate = cachedFiles.firstOrNull { it.id == fileId } ?: return
        _uiState.update {
            it.copy(
                selectedFile = candidate,
                viewerFile = candidate
            )
        }
    }

    fun onCloseViewer() {
        _uiState.update { it.copy(viewerFile = null) }
    }



    fun onRetryLoad() {
        if (_uiState.value.isConnected) {
            onRefreshFiles()
        } else {
            connectionManager.forceReconnectNow()
        }
    }

    fun onApplyFile(fileId: String) {
        if (!_uiState.value.isConnected) {
            _uiState.update { it.copy(errorMessage = "设备未连接，无法操作。") }
            return
        }
        val file = cachedFiles.firstOrNull { it.id == fileId }
        if (file == null) {
            _uiState.update { it.copy(errorMessage = "找不到该文件") }
            return
        }
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.update { it.copy(applyInProgressId = fileId) }
            
            // ESP32 doesn't have /apply endpoint. Behavior depends on file type:
            // - Audio: Should use "下载录音" button which triggers WAV download flow
            // - Image: Already on badge, just update local UI state
            when (file.mediaType) {
                DeviceMediaTab.Audio -> {
                    // Audio files should be downloaded via the dedicated WAV download flow
                    // (the "下载录音" button at the top of the screen)
                    _uiState.update {
                        it.copy(
                            errorMessage = "请使用「下载录音」按钮下载音频文件",
                            applyInProgressId = null
                        )
                    }
                }
                else -> {
                    // For images: files are already on the badge (uploaded via JPG flow)
                    // Just update local UI state to mark as "applied"
                    updateAppliedFile(fileId, applied = true)
                    _uiState.update { it.copy(applyInProgressId = null) }
                }
            }
        }
    }

    fun onDeleteFile(fileId: String) {
        if (!_uiState.value.isConnected) {
            _uiState.update { it.copy(errorMessage = "设备未连接，无法删除。") }
            return
        }
        val file = cachedFiles.firstOrNull { it.id == fileId }
        if (file == null) {
            _uiState.update { it.copy(errorMessage = "找不到该文件") }
            return
        }
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = mediaGateway.deleteFile(_uiState.value.baseUrl, fileId)) {
                is Result.Success -> {
                    cachedFiles = cachedFiles.filterNot { it.id == fileId }
                    _uiState.update { state ->
                        state.copy(
                            files = cachedFiles,
                            visibleFiles = cachedFiles,
                            selectedFile = state.selectedFile?.takeIf { it.id != fileId },
                            viewerFile = state.viewerFile?.takeIf { it.id != fileId }
                        )
                    }
                }

                is Result.Error -> _uiState.update {
                    it.copy(errorMessage = result.throwable.message ?: "删除失败")
                }
            }
        }
    }

    fun onBaseUrlChanged(value: String) {
        _uiState.update {
            it.copy(
                baseUrl = value.trim(),
                baseUrlWasManual = true,
                hasResolvedBaseUrl = true
            )
        }
    }

    fun onUseAutoBaseUrl() {
        val autoBase = _uiState.value.autoDetectedBaseUrl ?: return
        _uiState.update {
            it.copy(
                baseUrl = autoBase,
                baseUrlWasManual = false,
                hasResolvedBaseUrl = true
            )
        }
    }

    fun onClearError() {
        _uiState.update { it.copy(errorMessage = null, loadErrorMessage = null) }
    }

    // === GIF Transfer ===

    fun uploadGif(uri: Uri) {
        val session = (connectionManager.state.value as? ConnectionState.Connected)?.session
            ?: (connectionManager.state.value as? ConnectionState.WifiProvisioned)?.session
            ?: (connectionManager.state.value as? ConnectionState.Syncing)?.session
        
        if (session == null) {
            _uiState.update { it.copy(errorMessage = "设备未连接，无法上传") }
            return
        }

        viewModelScope.launch(dispatcherProvider.io) {
            gifTransferCoordinator.transfer(session, uri)
                .collect { state ->
                    _uiState.update { it.copy(gifTransferState = state) }
                }
        }
    }

    fun clearGifState() {
        _uiState.update { it.copy(gifTransferState = null) }
    }

    // === WAV Operations ===

    fun listWavFiles() {
        val session = (connectionManager.state.value as? ConnectionState.Connected)?.session
            ?: (connectionManager.state.value as? ConnectionState.WifiProvisioned)?.session
            ?: (connectionManager.state.value as? ConnectionState.Syncing)?.session

        if (session == null) {
            _uiState.update { it.copy(errorMessage = "设备未连接") }
            return
        }

        viewModelScope.launch(dispatcherProvider.io) {
            wavDownloadCoordinator.listFiles(session)
                .collect { state ->
                    _uiState.update { it.copy(wavListState = state) }
                }
        }
    }

    fun downloadWavFiles(files: List<String>, destDir: File) {
         val session = (connectionManager.state.value as? ConnectionState.Connected)?.session
            ?: (connectionManager.state.value as? ConnectionState.WifiProvisioned)?.session
            ?: (connectionManager.state.value as? ConnectionState.Syncing)?.session

        if (session == null) {
            _uiState.update { it.copy(errorMessage = "设备未连接") }
            return
        }

        viewModelScope.launch(dispatcherProvider.io) {
            wavDownloadCoordinator.downloadFiles(session, files, destDir)
                .collect { state ->
                    _uiState.update { it.copy(wavDownloadState = state) }
                }
        }
    }
    
    fun clearWavState() {
        _uiState.update { it.copy(wavListState = null, wavDownloadState = null) }
    }

    private fun loadFiles(baseUrl: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch(dispatcherProvider.io) {
            fetchFilesInternal(baseUrl)
        }
    }

    private suspend fun fetchFilesInternal(baseUrl: String) {
        _uiState.update { it.copy(isLoading = true, loadErrorMessage = null, errorMessage = null) }
        when (val result = mediaGateway.fetchFiles(baseUrl)) {
            is Result.Success -> {
                if (!_uiState.value.isConnected) {
                    _uiState.update { it.copy(isLoading = false) }
                    return
                }
                cachedFiles = result.data.mapNotNull { it.toUiOrNull() }
                _uiState.update { state ->
                    state.copy(
                        files = cachedFiles,
                        visibleFiles = cachedFiles,
                        selectedFile = state.selectedFile?.let { selected ->
                            cachedFiles.firstOrNull { it.id == selected.id }
                        },
                        viewerFile = state.viewerFile?.let { viewer ->
                            cachedFiles.firstOrNull { it.id == viewer.id }
                        },
                        isLoading = false,
                        loadErrorMessage = null,
                        errorMessage = null
                    )
                }
            }

            is Result.Error -> {
                if (!_uiState.value.isConnected) {
                    _uiState.update { it.copy(isLoading = false) }
                    return
                }
                cachedFiles = emptyList()
                _uiState.update {
                    it.copy(
                        files = emptyList(),
                        visibleFiles = emptyList(),
                        selectedFile = null,
                        viewerFile = null,
                        isLoading = false,
                        loadErrorMessage = LOAD_ERROR_MESSAGE,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun updateAppliedFile(fileId: String, applied: Boolean) {
        cachedFiles = cachedFiles.map {
            if (it.id == fileId) it.copy(isApplied = applied) else it
        }
        _uiState.update { state ->
            state.copy(
                files = cachedFiles,
                visibleFiles = cachedFiles,
                selectedFile = state.selectedFile?.let { selected ->
                    cachedFiles.firstOrNull { it.id == selected.id }
                        ?: cachedFiles.firstOrNull { it.id == fileId }
                }
            )
        }
    }

    private fun observeConnection()
 {
        viewModelScope.launch(dispatcherProvider.default) {
            var lastReadyForNetwork = false
            var lastReadyForFiles = false
            connectionManager.state.collectLatest { connection ->
                val readyForNetwork = connection.canQueryNetwork()
                val readyForFiles = connection.isReadyForFiles()
                _uiState.update { state ->
                    state.copy(
                        isConnected = readyForFiles,
                        canRetryConnect = when (connection) {
                            ConnectionState.NeedsSetup -> false
                            else -> true
                        },
                        canStartSetup = when (connection) {
                            ConnectionState.NeedsSetup -> true
                            else -> false
                        },
                        connectionStatus = connection.toUiState(),
                        autoDetectStatus = if (readyForNetwork) {
                            state.autoDetectStatus
                        } else {
                            AUTO_DETECT_WAITING_MESSAGE
                        },
                        isAutoDetectingBaseUrl = if (readyForNetwork) {
                            state.isAutoDetectingBaseUrl
                        } else {
                            false
                        },
                        hasResolvedBaseUrl = state.hasResolvedBaseUrl && readyForNetwork
                    )
                }
                if (!readyForNetwork) {
                    autoDetectJob?.cancel()
                }
                if (readyForNetwork && (!lastReadyForNetwork || _uiState.value.autoDetectedBaseUrl == null)) {
                    triggerAutoDetect()
                }
                if (readyForFiles && !lastReadyForFiles) {
                    val state = _uiState.value
                    if (state.hasResolvedBaseUrl || state.baseUrlWasManual || state.baseUrl != DEFAULT_MEDIA_SERVER_BASE_URL) {
                        loadFiles(state.baseUrl)
                    }
                }
                if (!readyForFiles) {
                    cachedFiles = emptyList()
                    _uiState.update {
                        it.copy(
                            files = emptyList(),
                            visibleFiles = emptyList(),
                            selectedFile = null,
                            viewerFile = null,
                            applyInProgressId = null,
                            isLoading = false,
                            isUploading = false,
                            loadErrorMessage = null,
                            errorMessage = null,
                            hasResolvedBaseUrl = it.baseUrlWasManual
                        )
                    }
                }
                lastReadyForNetwork = readyForNetwork
                lastReadyForFiles = readyForFiles
            }
        }
    }

    private fun observeEndpoint() {
        viewModelScope.launch(dispatcherProvider.default) {
            endpointProvider.deviceBaseUrl.collectLatest { baseUrl ->
                if (baseUrl.isNullOrBlank()) return@collectLatest
                _uiState.update { state ->
                    val shouldOverride = !state.baseUrlWasManual || state.baseUrl.isBlank()
                    if (!shouldOverride) state else state.copy(
                        baseUrl = baseUrl,
                        baseUrlWasManual = false,
                        autoDetectStatus = "自动同步设备地址",
                        hasResolvedBaseUrl = true
                    )
                }
                if (_uiState.value.isConnected) {
                    loadFiles(_uiState.value.baseUrl)
                }
            }
        }
    }

    private fun triggerAutoDetect() {
        if (autoDetectJob?.isActive == true) return
        autoDetectJob = viewModelScope.launch(dispatcherProvider.io) {
            _uiState.update {
                it.copy(
                    isAutoDetectingBaseUrl = true,
                    autoDetectStatus = AUTO_DETECT_LOADING_MESSAGE
                )
            }
            when (val result = connectionManager.queryNetworkStatus()) {
                is Result.Success -> {
                    val detectedBase = buildBaseUrl(result.data.ipAddress, _uiState.value.baseUrl)
                    _uiState.update { state ->
                        val shouldOverride = !state.baseUrlWasManual
                        state.copy(
                            autoDetectedBaseUrl = detectedBase,
                            baseUrl = if (shouldOverride) detectedBase else state.baseUrl,
                            isAutoDetectingBaseUrl = false,
                            baseUrlWasManual = if (shouldOverride) false else state.baseUrlWasManual,
                            autoDetectStatus = "已检测到 ${result.data.ipAddress}",
                            hasResolvedBaseUrl = true
                        )
                    }
                    if (_uiState.value.isConnected) {
                        loadFiles(_uiState.value.baseUrl)
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isAutoDetectingBaseUrl = false,
                            autoDetectStatus = result.throwable.message ?: "无法获取设备网络"
                        )
                    }
                }
            }
        }
    }

private fun ConnectionState.isReadyForFiles(): Boolean =
    this is ConnectionState.Connected || this is ConnectionState.Syncing || this is ConnectionState.WifiProvisioned

private fun ConnectionState.canQueryNetwork(): Boolean =
    this is ConnectionState.Connected || this is ConnectionState.Syncing || this is ConnectionState.WifiProvisioned

private fun ConnectionState.toUiState(): DeviceConnectionUiState = when (this) {
    ConnectionState.NeedsSetup -> DeviceConnectionUiState.Disconnected("需要先完成设备配网")
    ConnectionState.Disconnected -> DeviceConnectionUiState.Disconnected()
    is ConnectionState.AutoReconnecting -> DeviceConnectionUiState.Connecting("正在自动重连…")
    is ConnectionState.Pairing -> DeviceConnectionUiState.Connecting(deviceName)
    is ConnectionState.Connected -> DeviceConnectionUiState.Connected(session.peripheralName)
    is ConnectionState.WifiProvisioned -> DeviceConnectionUiState.Connected(session.peripheralName)
    is ConnectionState.Syncing -> DeviceConnectionUiState.Connected(session.peripheralName)
    is ConnectionState.Error -> DeviceConnectionUiState.Disconnected(error.toReadableMessage())
}

private fun ConnectivityError.toReadableMessage(): String = when (this) {
    is ConnectivityError.PairingInProgress -> "设备 $deviceName 正在配对"
    is ConnectivityError.ProvisioningFailed -> reason.ifBlank { "配网失败" }
    is ConnectivityError.PermissionDenied -> "缺少权限：${permissions.joinToString()}"
    is ConnectivityError.Timeout -> "操作超时（${timeoutMillis}ms）"
    is ConnectivityError.Transport -> reason.ifBlank { "传输异常" }
    is ConnectivityError.EndpointUnreachable -> reason.ifBlank { "设备服务不可达" }
    is ConnectivityError.DeviceNotFound -> "未找到设备 ${deviceId}"
    ConnectivityError.MissingSession -> "未找到连接会话"
}

    private fun buildBaseUrl(host: String, fallback: String): String {
        val sanitizedHost = host
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .ifBlank { host }
        val fallbackUri = runCatching { Uri.parse(fallback) }.getOrNull()
        val scheme = fallbackUri?.scheme ?: "http"
        val port = guessPort(fallbackUri)
        return "$scheme://$sanitizedHost:$port"
    }

    private fun guessPort(uri: Uri?): Int {
        if (uri == null) return DEFAULT_MEDIA_SERVER_PORT
        val parsedPort = uri.port
        if (parsedPort != -1) return parsedPort
        return DEFAULT_MEDIA_SERVER_PORT
    }
}

interface DeviceMediaGateway {
    suspend fun fetchFiles(baseUrl: String): Result<List<DeviceMediaFile>>
    suspend fun uploadFile(baseUrl: String, source: DeviceUploadSource): Result<Unit>
    suspend fun applyFile(baseUrl: String, fileName: String): Result<Unit>
    suspend fun deleteFile(baseUrl: String, fileName: String): Result<Unit>
    suspend fun downloadFile(baseUrl: String, file: DeviceMediaFile): Result<File>
}
