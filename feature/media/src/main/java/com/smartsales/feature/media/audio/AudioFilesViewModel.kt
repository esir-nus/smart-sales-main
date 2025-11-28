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
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import com.smartsales.feature.media.devicemanager.DeviceMediaGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AudioFilesViewModel @Inject constructor(
    private val mediaGateway: DeviceMediaGateway,
    private val mediaSyncCoordinator: MediaSyncCoordinator,
    private val transcriptionCoordinator: AudioTranscriptionCoordinator,
    private val endpointProvider: DeviceHttpEndpointProvider,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioFilesUiState(isLoading = true))
    val uiState: StateFlow<AudioFilesUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<AudioFilesEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AudioFilesEvent> = _events.asSharedFlow()

    private var currentBaseUrl: String? = null
    private var playingId: String? = null
    private val recordingSources = mutableMapOf<String, DeviceMediaFile>()
    private val observingJobs = mutableSetOf<String>()

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
        val base = currentBaseUrl ?: run {
            markRecordingStatus(id, TranscriptionStatus.ERROR)
            return emitError("设备未连接")
        }
        val deviceFile = recordingSources[id] ?: run {
            markRecordingStatus(id, TranscriptionStatus.ERROR)
            return emitError("未找到音频文件")
        }
        viewModelScope.launch(dispatchers.io) {
            markRecordingStatus(id, TranscriptionStatus.IN_PROGRESS)
            val localFile = when (val download = mediaGateway.downloadFile(base, deviceFile)) {
                is Result.Success -> download.data
                is Result.Error -> {
                    markRecordingStatus(id, TranscriptionStatus.ERROR)
                    emitError(download.throwable.message)
                    return@launch
                }
            }
            val uploadPayload = when (val upload = transcriptionCoordinator.uploadAudio(localFile)) {
                is Result.Success -> upload.data
                is Result.Error -> {
                    markRecordingStatus(id, TranscriptionStatus.ERROR)
                    emitError(upload.throwable.message)
                    return@launch
                }
            }
            when (
                val submit = transcriptionCoordinator.submitTranscription(
                    audioAssetName = deviceFile.name,
                    language = DEFAULT_TINGWU_LANGUAGE,
                    uploadPayload = uploadPayload
                )
            ) {
                is Result.Success -> {
                    val taskId = submit.data
                    _uiState.update { state ->
                        state.copy(
                            recordings = state.recordings.map { recording ->
                                if (recording.id == id) {
                                    recording.copy(transcriptionStatus = TranscriptionStatus.IN_PROGRESS)
                                } else recording
                            },
                            transcriptPreviewRecording = null,
                            tingwuTaskIds = state.tingwuTaskIds + (id to taskId)
                        )
                    }
                    observeJob(id, taskId)
                }

                is Result.Error -> {
                    markRecordingStatus(id, TranscriptionStatus.ERROR)
                    emitError(submit.throwable.message)
                }
            }
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
                recordingSources.clear()
                val items = result.data
                    .filter { it.isAudio() }
                    .onEach { recordingSources[it.name] = it }
                    .map { it.toUi(playingId) }
                _uiState.update {
                    it.copy(
                        recordings = items,
                        transcriptPreviewRecording = null,
                        tingwuTaskIds = it.tingwuTaskIds.filterKeys { key ->
                            items.any { recording -> recording.id == key }
                        },
                        isLoading = false,
                        errorMessage = null,
                        loadErrorMessage = null
                    )
                }
            }

            is Result.Error -> {
                emitError(result.throwable.message)
                _uiState.update { it.copy(isLoading = false, loadErrorMessage = result.throwable.message) }
            }
        }
    }

    private fun performWithBaseUrl(block: suspend (String) -> Unit) {
        val base = currentBaseUrl ?: return emitError("设备未连接")
        viewModelScope.launch(dispatchers.io) {
            block(base)
        }
    }

    private fun markRecordingStatus(id: String, status: TranscriptionStatus) {
        _uiState.update { state ->
            state.copy(
                recordings = state.recordings.map { recording ->
                    if (recording.id == id) {
                        recording.copy(transcriptionStatus = status)
                    } else recording
                }
            )
        }
    }

    private fun observeJob(recordingId: String, jobId: String) {
        if (observingJobs.contains(jobId)) return
        observingJobs.add(jobId)
        viewModelScope.launch(dispatchers.default) {
            transcriptionCoordinator.observeJob(jobId).collectLatest { state ->
                when (state) {
                    is com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState.InProgress -> {
                        markRecordingStatus(recordingId, TranscriptionStatus.IN_PROGRESS)
                    }

                    is com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState.Completed -> {
                        val preview = buildPreview(state.transcriptMarkdown)
                        _uiState.update { ui ->
                            ui.copy(
                                recordings = ui.recordings.map { recording ->
                                    if (recording.id == recordingId) {
                                        recording.copy(
                                            transcriptionStatus = TranscriptionStatus.DONE,
                                            transcriptPreview = preview,
                                            fullTranscriptMarkdown = state.transcriptMarkdown,
                                            transcriptionUrl = state.transcriptionUrl,
                                            autoChaptersUrl = state.autoChaptersUrl,
                                            chapters = state.chapters,
                                            smartSummary = state.smartSummary
                                        )
                                    } else recording
                                }
                            )
                        }
                        val fileName = recordingSources[recordingId]?.name ?: recordingId
                        val transcript = state.transcriptMarkdown
                        if (transcript.isNotBlank()) {
                            _events.tryEmit(
                                AudioFilesEvent.TranscriptReady(
                                    recordingId = recordingId,
                                    fileName = fileName,
                                    jobId = jobId,
                                    transcriptPreview = preview,
                                    fullTranscriptMarkdown = transcript,
                                    transcriptionUrl = state.transcriptionUrl
                                )
                            )
                        }
                        observingJobs.remove(jobId)
                    }

                    is com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState.Failed -> {
                        markRecordingStatus(recordingId, TranscriptionStatus.ERROR)
                        observingJobs.remove(jobId)
                    }

                    else -> {
                        // Idle or unknown – no-op
                    }
                }
            }
        }
    }

    private fun buildPreview(markdown: String): String {
        if (markdown.isBlank()) return "暂无内容"
        val firstLine = markdown.lineSequence().firstOrNull { it.isNotBlank() }
        val text = firstLine ?: markdown
        return text.take(MAX_PREVIEW_LENGTH)
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
        private const val DEFAULT_TINGWU_LANGUAGE = "zh-CN"
        private const val MAX_PREVIEW_LENGTH = 120

        private fun formatTimestamp(timestamp: Long?): String {
            if (timestamp == null || timestamp <= 0) return "未知时间"
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            return formatter.format(Date(timestamp))
        }
    }
}

sealed interface AudioFilesEvent {
    data class TranscriptReady(
        val recordingId: String,
        val fileName: String,
        val jobId: String,
        val transcriptPreview: String?,
        val fullTranscriptMarkdown: String?,
        val transcriptionUrl: String?
    ) : AudioFilesEvent
}
