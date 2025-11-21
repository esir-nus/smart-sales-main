package com.smartsales.feature.media.audiofiles

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audiofiles/AudioFilesViewModel.kt
// 模块：:feature:media
// 说明：控制音频列表、同步、播放与转写状态的 ViewModel
// 作者：创建于 2025-11-21

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import com.smartsales.feature.media.devicemanager.DeviceMediaGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.max
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val DEFAULT_TINGWU_LANGUAGE = "zh-CN"

@HiltViewModel
class AudioFilesViewModel @Inject constructor(
    private val mediaGateway: DeviceMediaGateway,
    private val transcriptionCoordinator: AudioTranscriptionCoordinator,
    private val playbackController: AudioPlaybackController,
    private val endpointProvider: DeviceHttpEndpointProvider,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val recordingsMutex = Mutex()
    private val recordings = LinkedHashMap<String, RecordingState>()
    private val jobObservers = mutableMapOf<String, Job>()

    private val _uiState = MutableStateFlow(AudioFilesUiState())
    val uiState: StateFlow<AudioFilesUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<AudioFilesNavigation>()
    val events: SharedFlow<AudioFilesNavigation> = _events

    init {
        observeEndpoint()
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onRefresh() {
        viewModelScope.launch(dispatcherProvider.io) {
            performSync()
        }
    }

    fun onSyncClicked() {
        viewModelScope.launch(dispatcherProvider.io) {
            performSync()
        }
    }

    fun onPlayPause(recordingId: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            val baseUrl = uiState.value.baseUrl ?: return@launch
            val current = uiState.value.currentlyPlayingId
            if (current == recordingId) {
                playbackController.pause()
                _uiState.update { it.copy(currentlyPlayingId = null) }
                return@launch
            }
            val file = recordingsMutex.withLock {
                recordings[recordingId]
            } ?: return@launch
            val localFile = file.localFile ?: when (val result = mediaGateway.downloadFile(baseUrl, file.file)) {
                is Result.Success -> result.data
                is Result.Error -> {
                    emitError(result.throwable.message)
                    return@launch
                }
            }
            updateRecording(recordingId) { it.copy(localFile = localFile) }
            when (val playResult = playbackController.play(localFile)) {
                is Result.Success -> _uiState.update { it.copy(currentlyPlayingId = recordingId) }
                is Result.Error -> emitError(playResult.throwable.message)
            }
        }
    }

    fun onTranscribe(recordingId: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            val baseUrl = uiState.value.baseUrl ?: return@launch
            val file = recordingsMutex.withLock { recordings[recordingId]?.file } ?: return@launch
            val current = recordingsMutex.withLock { recordings[recordingId] }
            if (current?.status == AudioRecordingStatus.Transcribing) return@launch
            updateRecording(recordingId) { it.copy(status = AudioRecordingStatus.Syncing, error = null, tingwuJobId = null) }

            val localFile = current?.localFile ?: when (val download = mediaGateway.downloadFile(baseUrl, file)) {
                is Result.Success -> download.data
                is Result.Error -> {
                    updateRecording(recordingId) {
                        it.copy(
                            status = AudioRecordingStatus.Error,
                            error = download.throwable.message
                        )
                    }
                    emitError(download.throwable.message)
                    return@launch
                }
            }
            val uploadPayload = when (val upload = transcriptionCoordinator.uploadAudio(localFile)) {
                is Result.Success -> upload.data
                is Result.Error -> {
                    updateRecording(recordingId) {
                        it.copy(
                            status = AudioRecordingStatus.Error,
                            error = upload.throwable.message,
                            localFile = localFile
                        )
                    }
                    emitError(upload.throwable.message)
                    return@launch
                }
            }
            when (val submit = transcriptionCoordinator.submitTranscription(
                audioAssetName = file.name,
                language = DEFAULT_TINGWU_LANGUAGE,
                uploadPayload = uploadPayload
            )) {
                is Result.Success -> {
                    val jobId = submit.data
                    updateRecording(recordingId) {
                        it.copy(
                            status = AudioRecordingStatus.Transcribing,
                            tingwuJobId = jobId,
                            localFile = localFile
                        )
                    }
                    observeTingwuJob(recordingId, jobId)
                    _events.emit(
                        AudioFilesNavigation.TranscribeToChat(
                            recordingId = recordingId,
                            fileName = file.name,
                            jobId = jobId
                        )
                    )
                }

                is Result.Error -> {
                    updateRecording(recordingId) {
                        it.copy(
                            status = AudioRecordingStatus.Error,
                            error = submit.throwable.message,
                            localFile = localFile
                        )
                    }
                    emitError(submit.throwable.message)
                }
            }
        }
    }

    fun onApply(recordingId: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            val baseUrl = uiState.value.baseUrl ?: return@launch
            val fileName = recordingsMutex.withLock { recordings[recordingId]?.file?.name } ?: return@launch
            when (val result = mediaGateway.applyFile(baseUrl, fileName)) {
                is Result.Success -> _uiState.update { it.copy(errorMessage = null) }
                is Result.Error -> emitError(result.throwable.message)
            }
        }
    }

    fun onDelete(recordingId: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            val baseUrl = uiState.value.baseUrl ?: return@launch
            val fileName = recordingsMutex.withLock { recordings[recordingId]?.file?.name } ?: return@launch
            when (val result = mediaGateway.deleteFile(baseUrl, fileName)) {
                is Result.Success -> {
                    recordingsMutex.withLock {
                        recordings.remove(recordingId)
                    }
                    if (uiState.value.currentlyPlayingId == recordingId) {
                        playbackController.pause()
                        _uiState.update { it.copy(currentlyPlayingId = null) }
                    }
                    publishRecordings()
                }
                is Result.Error -> emitError(result.throwable.message)
            }
        }
    }

    private suspend fun performSync() {
        val baseUrl = uiState.value.baseUrl ?: return
        _uiState.update { it.copy(errorMessage = null) }
        setSyncing(true)
        when (val result = mediaGateway.fetchFiles(baseUrl)) {
            is Result.Success -> {
                mergeFiles(result.data)
                setSyncing(false)
            }

            is Result.Error -> {
                emitError(result.throwable.message)
                setSyncing(false)
            }
        }
    }

    private fun observeTingwuJob(recordingId: String, jobId: String) {
        jobObservers[jobId]?.cancel()
        val job = viewModelScope.launch(dispatcherProvider.default) {
            transcriptionCoordinator.observeJob(jobId).collect { state ->
                when (state) {
                    is AudioTranscriptionJobState.Idle -> Unit
                    is AudioTranscriptionJobState.InProgress -> {
                        updateRecording(recordingId) {
                            it.copy(
                                status = AudioRecordingStatus.Transcribing,
                                progress = max(state.progressPercent, 0)
                            )
                        }
                    }
                    is AudioTranscriptionJobState.Completed -> {
                        updateRecording(recordingId) {
                            it.copy(
                                status = AudioRecordingStatus.Transcribed,
                                transcript = state.transcriptMarkdown
                            )
                        }
                    }
                    is AudioTranscriptionJobState.Failed -> {
                        updateRecording(recordingId) {
                            it.copy(
                                status = AudioRecordingStatus.Error,
                                error = state.reason
                            )
                        }
                        emitError(state.reason)
                    }
                }
            }
        }
        jobObservers[jobId] = job
    }

    private suspend fun mergeFiles(files: List<DeviceMediaFile>) {
        recordingsMutex.withLock {
            val currentNames = files.map { it.name }.toSet()
            val iterator = recordings.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.key !in currentNames) {
                    iterator.remove()
                }
            }
            files.forEach { file ->
                val existing = recordings[file.name]
                if (existing == null) {
                    recordings[file.name] = RecordingState(file = file)
                } else {
                    recordings[file.name] = existing.copy(file = file)
                }
            }
        }
        publishRecordings()
    }

    private suspend fun updateRecording(
        recordingId: String,
        updater: (RecordingState) -> RecordingState
    ) {
        recordingsMutex.withLock {
            val current = recordings[recordingId] ?: return
            recordings[recordingId] = updater(current)
        }
        publishRecordings()
    }

    private suspend fun publishRecordings() {
        val playingId = uiState.value.currentlyPlayingId
        val items = recordingsMutex.withLock {
            recordings.values
                .sortedByDescending { it.file.modifiedAtMillis }
                .map { it.toUi(playingId) }
        }
        _uiState.update { it.copy(recordings = items) }
    }

    private fun DeviceMediaFile.isAudioFile(): Boolean {
        val mime = mimeType.lowercase(Locale.ROOT)
        if (mime.startsWith("audio/")) return true
        val name = name.lowercase(Locale.ROOT)
        return AUDIO_EXTENSIONS.any { name.endsWith(it) }
    }

    private fun setSyncing(running: Boolean) {
        _uiState.update { it.copy(isSyncing = running) }
    }

    private fun emitError(message: String?) {
        _uiState.update { it.copy(errorMessage = message ?: "操作失败") }
    }

    private suspend fun resetForDisconnectedEndpoint() {
        recordingsMutex.withLock { recordings.clear() }
        publishRecordings()
        playbackController.pause()
        _uiState.update {
            it.copy(
                baseUrl = null,
                currentlyPlayingId = null,
                isSyncing = false,
                errorMessage = null
            )
        }
    }

    private fun observeEndpoint() {
        viewModelScope.launch(dispatcherProvider.default) {
            endpointProvider.deviceBaseUrl.collectLatest { candidate ->
                val normalized = candidate?.trim()?.takeIf { it.isNotBlank() }
                val current = uiState.value.baseUrl
                if (normalized == null) {
                    if (current != null) {
                        resetForDisconnectedEndpoint()
                    }
                } else if (normalized != current) {
                    _uiState.update { it.copy(baseUrl = normalized) }
                    withContext(dispatcherProvider.io) {
                        performSync()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        jobObservers.values.forEach { it.cancel() }
        viewModelScope.launch(dispatcherProvider.io) { playbackController.stop() }
    }
}

data class AudioFilesUiState(
    val baseUrl: String? = null,
    val recordings: List<AudioRecordingUi> = emptyList(),
    val isSyncing: Boolean = false,
    val currentlyPlayingId: String? = null,
    val errorMessage: String? = null
)

data class AudioRecordingUi(
    val id: String,
    val fileName: String,
    val sizeText: String,
    val modifiedAtText: String,
    val status: AudioRecordingStatus,
    val transcriptSummary: String? = null,
    val tingwuJobId: String? = null,
    val progressPercent: Int = 0,
    val isPlaying: Boolean = false
)

enum class AudioRecordingStatus {
    Idle,
    Syncing,
    Transcribing,
    Transcribed,
    Error
}

sealed class AudioFilesNavigation {
    data class TranscribeToChat(
        val recordingId: String,
        val fileName: String,
        val jobId: String
    ) : AudioFilesNavigation()
}

private data class RecordingState(
    val file: DeviceMediaFile,
    val status: AudioRecordingStatus = AudioRecordingStatus.Idle,
    val tingwuJobId: String? = null,
    val transcript: String? = null,
    val progress: Int = 0,
    val error: String? = null,
    val localFile: java.io.File? = null
) {
    fun toUi(currentPlayingId: String?): AudioRecordingUi {
        val summary = transcript?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
        return AudioRecordingUi(
            id = file.name,
            fileName = file.name,
            sizeText = formatSize(file.sizeBytes),
            modifiedAtText = formatTimestamp(file.modifiedAtMillis),
            status = status,
            transcriptSummary = summary,
            tingwuJobId = tingwuJobId,
            progressPercent = progress,
            isPlaying = currentPlayingId == file.name
        )
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var idx = 0
    while (value >= 1024 && idx < units.lastIndex) {
        value /= 1024
        idx++
    }
    return String.format(Locale.CHINA, "%.1f%s", value, units[idx])
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return "未知时间"
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
    return formatter.format(java.util.Date(timestamp))
}

private val AUDIO_EXTENSIONS = setOf(
    ".mp3",
    ".wav",
    ".m4a",
    ".aac",
    ".flac",
    ".ogg"
)
