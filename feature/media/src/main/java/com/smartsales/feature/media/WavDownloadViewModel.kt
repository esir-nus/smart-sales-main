package com.smartsales.feature.media

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// 文件：feature/media/src/main/java/com/smartsales/feature/media/WavDownloadViewModel.kt
// 模块：:feature:media
// 说明：WAV录音下载的UI状态管理
// 作者：创建于 2026-01-10

/**
 * ViewModel for WAV file download from ESP32 badge.
 * 
 * Two-phase flow:
 * 1. Scan for available files
 * 2. Download selected files
 * 
 * Follows ux-experience.md spec for state machine and microcopy.
 */
@HiltViewModel
class WavDownloadViewModel @Inject constructor(
    private val coordinator: WavDownloadCoordinator,
    private val connectionManager: DeviceConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WavDownloadUiState())
    val uiState: StateFlow<WavDownloadUiState> = _uiState.asStateFlow()

    private val currentSession: BleSession?
        get() = when (val state = connectionManager.state.value) {
            is ConnectionState.Connected -> state.session
            is ConnectionState.WifiProvisioned -> state.session
            is ConnectionState.Syncing -> state.session
            else -> null
        }

    fun startScan() {
        val session = currentSession ?: run {
            _uiState.value = _uiState.value.copy(
                step = WavDownloadStep.Error,
                errorMessage = "徽章未连接"
            )
            return
        }

        viewModelScope.launch {
            coordinator.listFiles(session).collect { state ->
                _uiState.value = mapListState(state)
            }
        }
    }

    fun toggleFileSelection(filename: String) {
        val current = _uiState.value
        val newSelection = current.selectedFiles.toMutableSet()
        if (filename in newSelection) {
            newSelection.remove(filename)
        } else {
            newSelection.add(filename)
        }
        _uiState.value = current.copy(
            selectedFiles = newSelection,
            isDownloadEnabled = newSelection.isNotEmpty()
        )
    }

    fun selectAll() {
        val current = _uiState.value
        _uiState.value = current.copy(
            selectedFiles = current.availableFiles.toSet(),
            isDownloadEnabled = current.availableFiles.isNotEmpty()
        )
    }

    fun deselectAll() {
        _uiState.value = _uiState.value.copy(
            selectedFiles = emptySet(),
            isDownloadEnabled = false
        )
    }

    fun startDownload() {
        val session = currentSession ?: run {
            _uiState.value = _uiState.value.copy(
                step = WavDownloadStep.Error,
                errorMessage = "徽章未连接"
            )
            return
        }

        val files = _uiState.value.selectedFiles.toList()
        if (files.isEmpty()) return

        // Download to app's external files directory
        val destDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "SmartSales/recordings"
        )

        viewModelScope.launch {
            coordinator.downloadFiles(session, files, destDir).collect { state ->
                _uiState.value = mapDownloadState(state)
            }
        }
    }

    fun retry() {
        _uiState.value = WavDownloadUiState()
        startScan()
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(
            step = WavDownloadStep.Idle,
            errorMessage = null
        )
    }

    fun reset() {
        _uiState.value = WavDownloadUiState()
    }

    private fun mapListState(state: WavListState): WavDownloadUiState {
        val current = _uiState.value
        return when (state) {
            is WavListState.Scanning -> current.copy(
                step = WavDownloadStep.Scanning,
                statusMessage = "正在扫描录音文件..."
            )
            is WavListState.Found -> current.copy(
                step = WavDownloadStep.FilesFound,
                availableFiles = state.files,
                statusMessage = "${state.files.size} 个录音文件"
            )
            is WavListState.Empty -> current.copy(
                step = WavDownloadStep.Empty,
                statusMessage = "暂无录音"
            )
            is WavListState.Error -> current.copy(
                step = WavDownloadStep.Error,
                errorMessage = state.message
            )
        }
    }

    private fun mapDownloadState(state: WavDownloadState): WavDownloadUiState {
        val current = _uiState.value
        return when (state) {
            is WavDownloadState.Downloading -> current.copy(
                step = WavDownloadStep.Downloading,
                currentFileName = state.filename,
                progress = state.current,
                total = state.total,
                statusMessage = "正在下载 ${state.filename}..."
            )
            is WavDownloadState.Complete -> current.copy(
                step = WavDownloadStep.Complete,
                statusMessage = "下载完成"
            )
            is WavDownloadState.Error -> current.copy(
                step = WavDownloadStep.Error,
                errorMessage = state.message
            )
        }
    }
}

data class WavDownloadUiState(
    val step: WavDownloadStep = WavDownloadStep.Idle,
    val availableFiles: List<String> = emptyList(),
    val selectedFiles: Set<String> = emptySet(),
    val currentFileName: String? = null,
    val progress: Int = 0,
    val total: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val isDownloadEnabled: Boolean = false
)

enum class WavDownloadStep {
    Idle,           // "获取录音" button
    Scanning,       // "正在扫描录音文件..."
    FilesFound,     // File list with checkboxes
    Empty,          // "暂无录音"
    Downloading,    // "正在下载 {filename}..."
    Complete,       // "下载完成"
    Error           // Error card
}
