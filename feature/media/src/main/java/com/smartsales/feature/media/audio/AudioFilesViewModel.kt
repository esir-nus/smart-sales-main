package com.smartsales.feature.media.audio

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audio/AudioFilesViewModel.kt
// 模块：:feature:media
// 说明：负责加载、同步、播放与操作音频列表的 ViewModel
// 作者：创建于 2025-11-21

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.audiofiles.DeviceHttpEndpointProvider
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import com.smartsales.feature.media.devicemanager.DeviceMediaGateway
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
import kotlinx.coroutines.withContext

@HiltViewModel
class AudioFilesViewModel @Inject constructor(
    private val mediaGateway: DeviceMediaGateway,
    private val mediaSyncCoordinator: MediaSyncCoordinator,
    private val endpointProvider: DeviceHttpEndpointProvider,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioFilesUiState(isLoading = true))
    val uiState: StateFlow<AudioFilesUiState> = _uiState.asStateFlow()

    private var currentBaseUrl: String? = null
    private var playingId: String? = null

    init {
        observeEndpoint()
        observeSyncState()
    }

    fun onRefresh() {
        val base = currentBaseUrl ?: return emitError("设备未连接")
        viewModelScope.launch(dispatchers.io) {
            loadRecordings(base, showLoading = true)
        }
    }

    fun onSyncClicked() {
        val base = currentBaseUrl ?: return emitError("设备未连接，无法同步")
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            when (val result = mediaSyncCoordinator.triggerSync()) {
                is Result.Success -> loadRecordings(base, showLoading = false)
                is Result.Error -> emitError(result.throwable.message)
            }
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    fun onRecordingClicked(id: String) {
        _uiState.update { it.copy(selectedRecordingId = id) }
    }

    fun onPlayPauseClicked(id: String) {
        // 播放暂时只切换 UI 状态，播放集成由后续接入。
        val newPlaying = if (playingId == id) null else id
        playingId = newPlaying
        _uiState.update { state ->
            state.copy(
                selectedRecordingId = newPlaying ?: state.selectedRecordingId,
                recordings = state.recordings.map { item ->
                    item.copy(isPlaying = item.id == newPlaying)
                }
            )
        }
    }

    fun onDeleteClicked(id: String) {
        performWithBaseUrl { base ->
            when (val result = mediaGateway.deleteFile(base, id)) {
                is Result.Success -> loadRecordings(base, showLoading = false)
                is Result.Error -> emitError(result.throwable.message)
            }
        }
    }

    fun onTranscribeClicked(id: String) {
        _uiState.update { state ->
            state.copy(
                recordings = state.recordings.map { recording ->
                    if (recording.id == id) {
                        recording.copy(
                            transcriptionStatus = TranscriptionStatus.IN_PROGRESS,
                            transcriptPreview = recording.transcriptPreview ?: "转写占位内容"
                        )
                    } else {
                        recording
                    }
                },
                transcriptPreviewRecording = null
            )
        }
    }

    fun onTranscriptClicked(id: String) {
        val target = _uiState.value.recordings.find { it.id == id } ?: return
        _uiState.update { it.copy(transcriptPreviewRecording = target) }
    }

    fun onTranscriptDismissed() {
        _uiState.update { it.copy(transcriptPreviewRecording = null) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeEndpoint() {
        viewModelScope.launch(dispatchers.default) {
            endpointProvider.deviceBaseUrl.collectLatest { baseUrl ->
                currentBaseUrl = baseUrl
                if (baseUrl.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            recordings = emptyList(),
                            selectedRecordingId = null,
                            transcriptPreviewRecording = null,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    return@collectLatest
                }
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                withContext(dispatchers.io) {
                    loadRecordings(baseUrl, showLoading = false)
                }
            }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch(dispatchers.default) {
            mediaSyncCoordinator.state.collectLatest { syncState ->
                _uiState.update { it.copy(isSyncing = syncState.syncing) }
                syncState.errorMessage?.let { emitError(it) }
            }
        }
    }

    private suspend fun loadRecordings(baseUrl: String, showLoading: Boolean) {
        if (showLoading) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }
        when (val result = mediaGateway.fetchFiles(baseUrl)) {
            is Result.Success -> {
                val items = result.data
                    .filter { it.isAudio() }
                    .map { it.toUi(playingId) }
                _uiState.update {
                    it.copy(
                        recordings = items,
                        transcriptPreviewRecording = null,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }

            is Result.Error -> {
                emitError(result.throwable.message)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun performWithBaseUrl(block: suspend (String) -> Unit) {
        val base = currentBaseUrl ?: return emitError("设备未连接")
        viewModelScope.launch(dispatchers.io) {
            block(base)
        }
    }

    private fun emitError(message: String?) {
        _uiState.update { it.copy(errorMessage = message ?: "操作失败") }
    }

    private fun DeviceMediaFile.isAudio(): Boolean {
        val lower = mimeType.lowercase(Locale.ROOT)
        if (lower.startsWith("audio/")) return true
        val nameLower = name.lowercase(Locale.ROOT)
        return AUDIO_EXTENSIONS.any { nameLower.endsWith(it) }
    }

    private fun DeviceMediaFile.toUi(playingId: String?): AudioRecordingUi =
        AudioRecordingUi(
            id = name,
            title = name.substringBeforeLast(".", name),
            fileName = name,
            durationMillis = null,
            createdAtMillis = modifiedAtMillis,
            createdAtText = formatTimestamp(modifiedAtMillis),
            transcriptionStatus = TranscriptionStatus.NONE,
            isPlaying = playingId == name,
            hasLocalCopy = false
        )

    companion object {
        private val AUDIO_EXTENSIONS = setOf(".mp3", ".wav", ".m4a", ".aac", ".flac", ".ogg")

        private fun formatTimestamp(timestamp: Long?): String {
            if (timestamp == null || timestamp <= 0) return "未知时间"
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            return formatter.format(Date(timestamp))
        }
    }
}
