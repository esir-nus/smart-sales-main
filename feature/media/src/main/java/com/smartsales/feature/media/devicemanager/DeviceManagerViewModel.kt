package com.smartsales.feature.media.devicemanager

// 文件：feature/media/src/main/java/com/smartsales/feature/media/devicemanager/DeviceManagerViewModel.kt
// 模块：:feature:media
// 说明：管理设备文件列表、上传与应用操作的 ViewModel
// 作者：创建于 2025-11-20

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_MEDIA_SERVER_BASE_URL = "http://10.0.2.2:8000"

@HiltViewModel
class DeviceManagerViewModel @Inject constructor(
    private val mediaGateway: DeviceMediaGateway,
    private val connectionManager: DeviceConnectionManager,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceManagerUiState())
    val uiState: StateFlow<DeviceManagerUiState> = _uiState.asStateFlow()

    private var cachedFiles: List<DeviceFileUi> = emptyList()

    init {
        observeConnection()
    }

    fun onRefreshFiles() {
        val connection = _uiState.value.connectionState
        if (!connection.isReadyForFiles()) {
            _uiState.update {
                it.copy(errorMessage = "设备未连接，无法刷新文件。")
            }
            return
        }
        viewModelScope.launch(dispatcherProvider.io) {
            fetchFilesInternal(_uiState.value.baseUrl)
        }
    }

    fun onSelectTab(tab: DeviceMediaTab) {
        val files = cachedFiles
        _uiState.update { state ->
            val filtered = tab.filter(files)
            state.copy(
                activeTab = tab,
                visibleFiles = filtered,
                selectedFile = filtered.firstOrNull { it.id == state.selectedFile?.id }
            )
        }
    }

    fun onSelectFile(fileId: String) {
        val candidate = cachedFiles.firstOrNull { it.id == fileId } ?: return
        _uiState.update {
            it.copy(selectedFile = candidate)
        }
    }

    fun onUploadFile(source: DeviceUploadSource) {
        if (_uiState.value.isUploading) return
        if (!_uiState.value.connectionState.isReadyForFiles()) {
            _uiState.update { it.copy(errorMessage = "设备未连接，无法上传。") }
            return
        }
        val baseUrl = _uiState.value.baseUrl
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.update { it.copy(isUploading = true, errorMessage = null) }
            when (val result = mediaGateway.uploadFile(baseUrl, source)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isUploading = false) }
                    fetchFilesInternal(baseUrl)
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = result.throwable.message ?: "上传失败"
                        )
                    }
                }
            }
        }
    }

    fun onApplyFile(fileId: String) {
        val file = cachedFiles.firstOrNull { it.id == fileId }
        if (file == null) {
            _uiState.update { it.copy(errorMessage = "找不到该文件") }
            return
        }
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = mediaGateway.applyFile(_uiState.value.baseUrl, fileId)) {
                is Result.Success -> updateAppliedFile(fileId, applied = true)
                is Result.Error -> _uiState.update {
                    it.copy(errorMessage = result.throwable.message ?: "应用失败")
                }
            }
        }
    }

    fun onDeleteFile(fileId: String) {
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
                        val filtered = state.activeTab.filter(cachedFiles)
                        state.copy(
                            files = cachedFiles,
                            visibleFiles = filtered,
                            selectedFile = state.selectedFile?.takeIf { it.id != fileId }
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
        _uiState.update { it.copy(baseUrl = value.trim()) }
    }

    fun onClearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun fetchFilesInternal(baseUrl: String) {
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        when (val result = mediaGateway.fetchFiles(baseUrl)) {
            is Result.Success -> {
                cachedFiles = result.data.map { it.toUi() }
                _uiState.update { state ->
                    val filtered = state.activeTab.filter(cachedFiles)
                    state.copy(
                        files = cachedFiles,
                        visibleFiles = filtered,
                        selectedFile = state.selectedFile?.let { selected ->
                            filtered.firstOrNull { it.id == selected.id }
                        },
                        isRefreshing = false
                    )
                }
            }

            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = result.throwable.message ?: "获取文件失败"
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
            val filtered = state.activeTab.filter(cachedFiles)
            state.copy(
                files = cachedFiles,
                visibleFiles = filtered,
                selectedFile = state.selectedFile?.let { selected ->
                    filtered.firstOrNull { it.id == selected.id }
                        ?: filtered.firstOrNull { it.id == fileId }
                }
            )
        }
    }

    private fun observeConnection() {
        viewModelScope.launch(dispatcherProvider.default) {
            connectionManager.state.collectLatest { connection ->
                _uiState.update { it.copy(connectionState = connection) }
            }
        }
    }

    private fun ConnectionState.isReadyForFiles(): Boolean =
        this is ConnectionState.WifiProvisioned || this is ConnectionState.Syncing
}

data class DeviceManagerUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val baseUrl: String = DEFAULT_MEDIA_SERVER_BASE_URL,
    val files: List<DeviceFileUi> = emptyList(),
    val visibleFiles: List<DeviceFileUi> = emptyList(),
    val activeTab: DeviceMediaTab = DeviceMediaTab.Images,
    val selectedFile: DeviceFileUi? = null,
    val isRefreshing: Boolean = false,
    val isUploading: Boolean = false,
    val errorMessage: String? = null
)

data class DeviceFileUi(
    val id: String,
    val displayName: String,
    val sizeText: String,
    val mimeType: String,
    val mediaType: DeviceMediaTab,
    val modifiedAtText: String,
    val mediaUrl: String,
    val downloadUrl: String,
    val isApplied: Boolean = false
)

enum class DeviceMediaTab {
    Images,
    Videos,
    Other;

    fun matches(mimeType: String): Boolean = when (this) {
        Images -> mimeType.startsWith("image", ignoreCase = true)
        Videos -> mimeType.startsWith("video", ignoreCase = true)
        Other -> !(mimeType.startsWith("image", true) || mimeType.startsWith("video", true))
    }

    fun filter(files: List<DeviceFileUi>): List<DeviceFileUi> =
        files.filter { matches(it.mimeType) }
}

data class DeviceMediaFile(
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val modifiedAtMillis: Long,
    val mediaUrl: String,
    val downloadUrl: String
)

sealed interface DeviceUploadSource {
    data class AndroidUri(val uri: Uri) : DeviceUploadSource
}

interface DeviceMediaGateway {
    suspend fun fetchFiles(baseUrl: String): Result<List<DeviceMediaFile>>
    suspend fun uploadFile(baseUrl: String, source: DeviceUploadSource): Result<Unit>
    suspend fun applyFile(baseUrl: String, fileName: String): Result<Unit>
    suspend fun deleteFile(baseUrl: String, fileName: String): Result<Unit>
}

private fun DeviceMediaFile.toUi(): DeviceFileUi {
    val tab = when {
        mimeType.startsWith("image", ignoreCase = true) -> DeviceMediaTab.Images
        mimeType.startsWith("video", ignoreCase = true) -> DeviceMediaTab.Videos
        else -> DeviceMediaTab.Other
    }
    return DeviceFileUi(
        id = name,
        displayName = name,
        sizeText = formatSize(sizeBytes),
        mimeType = mimeType,
        mediaType = tab,
        modifiedAtText = formatTimestamp(modifiedAtMillis),
        mediaUrl = mediaUrl,
        downloadUrl = downloadUrl
    )
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return String.format(Locale.CHINA, "%.1f%s", value, units[index])
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return "未知时间"
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    return formatter.format(Date(timestamp))
}
