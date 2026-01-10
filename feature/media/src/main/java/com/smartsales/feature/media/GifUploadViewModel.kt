package com.smartsales.feature.media

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 文件：feature/media/src/main/java/com/smartsales/feature/media/GifUploadViewModel.kt
// 模块：:feature:media
// 说明：GIF上传到徽章的UI状态管理
// 作者：创建于 2026-01-10

/**
 * ViewModel for GIF upload to ESP32 badge.
 * 
 * Wraps [GifTransferCoordinator] and exposes UI-friendly state.
 * Follows ux-experience.md spec for state machine and microcopy.
 */
@HiltViewModel
class GifUploadViewModel @Inject constructor(
    private val coordinator: GifTransferCoordinator,
    private val connectionManager: DeviceConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GifUploadUiState())
    val uiState: StateFlow<GifUploadUiState> = _uiState.asStateFlow()

    private var selectedUri: Uri? = null

    /** Get current session from connection manager (reads on-demand, no caching) */
    private val currentSession: BleSession?
        get() = when (val state = connectionManager.state.value) {
            is ConnectionState.Connected -> state.session
            is ConnectionState.WifiProvisioned -> state.session
            is ConnectionState.Syncing -> state.session
            else -> null
        }

    fun selectImage(uri: Uri, fileName: String) {
        selectedUri = uri
        _uiState.value = _uiState.value.copy(
            selectedFileName = fileName,
            step = GifUploadStep.Idle,
            errorMessage = null,
            isActionEnabled = currentSession != null
        )
    }

    fun clearSelection() {
        selectedUri = null
        _uiState.value = GifUploadUiState()
    }

    fun startUpload() {
        val uri = selectedUri ?: return
        val session = currentSession ?: run {
            _uiState.value = _uiState.value.copy(
                step = GifUploadStep.Error,
                errorMessage = "徽章未连接"
            )
            return
        }

        viewModelScope.launch {
            coordinator.transfer(session, uri)
                .collect { state ->
                    _uiState.value = mapTransferState(state)
                }
        }
    }

    fun retry() {
        _uiState.value = _uiState.value.copy(
            step = GifUploadStep.Idle,
            errorMessage = null
        )
        startUpload()
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(
            step = GifUploadStep.Idle,
            errorMessage = null
        )
    }

    private fun mapTransferState(state: GifTransferState): GifUploadUiState {
        val current = _uiState.value
        return when (state) {
            is GifTransferState.Preparing -> current.copy(
                step = GifUploadStep.Preparing,
                statusMessage = "处理中..."
            )
            is GifTransferState.Extracting -> current.copy(
                step = GifUploadStep.Preparing,
                statusMessage = "提取帧 ${state.current}/${state.total}..."
            )
            is GifTransferState.Connecting -> current.copy(
                step = GifUploadStep.Connecting,
                statusMessage = "正在连接徽章..."
            )
            is GifTransferState.Uploading -> current.copy(
                step = GifUploadStep.Uploading,
                progress = state.current,
                total = state.total,
                statusMessage = "正在上传 ${state.current}/${state.total}..."
            )
            is GifTransferState.Finalizing -> current.copy(
                step = GifUploadStep.Uploading,
                statusMessage = "正在完成..."
            )
            is GifTransferState.Complete -> current.copy(
                step = GifUploadStep.Complete,
                statusMessage = "发送成功！"
            )
            is GifTransferState.Error -> current.copy(
                step = GifUploadStep.Error,
                errorMessage = state.message
            )
        }
    }
}

data class GifUploadUiState(
    val step: GifUploadStep = GifUploadStep.Idle,
    val selectedFileName: String? = null,
    val progress: Int = 0,
    val total: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val isActionEnabled: Boolean = false
)

enum class GifUploadStep {
    Idle,           // "发送到徽章" button visible
    Preparing,      // "处理中..."
    Connecting,     // "正在连接徽章..."
    Uploading,      // "正在上传 {n}/{total}..."
    Complete,       // Success toast
    Error           // Error card with retry
}
