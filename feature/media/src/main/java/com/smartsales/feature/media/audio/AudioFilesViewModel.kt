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
import com.smartsales.feature.media.audiofiles.AudioStorageRepository
import com.smartsales.feature.media.audiofiles.StoredAudio
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
    private val audioStorageRepository: AudioStorageRepository,
    private val flaggedRecordingsStore: FlaggedRecordingsStore,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioFilesUiState(isLoading = true))
    val uiState: StateFlow<AudioFilesUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<AudioFilesEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AudioFilesEvent> = _events.asSharedFlow()

    private var currentBaseUrl: String? = null
    private var playingId: String? = null
    private val jobSessionIds = mutableMapOf<String, String>()
    private val recordingSources = mutableMapOf<String, DeviceMediaFile>()
    private var deviceUiItems: List<AudioRecordingUi> = emptyList()
    private val storedAudios = mutableMapOf<String, StoredAudio>()
    private val observingJobs = mutableSetOf<String>()

    init {
        observeEndpoint()
        observeSyncState()
        observeStorage()
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

    fun onImportFromPhone(uri: android.net.Uri) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isSyncing = true) }
            runCatching { audioStorageRepository.importFromPhone(uri) }
                .onSuccess { 
                    val base = currentBaseUrl
                    if (base != null) {
                        loadRecordings(base, showLoading = false)
                    }
                }
                .onFailure { emitError(it.message ?: "导入失败") }
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

    /**
     * V17: Toggle star/flag on a recording.
     */
    fun onFlagToggle(id: String) {
        val newFlagged = !flaggedRecordingsStore.isFlagged(id)
        flaggedRecordingsStore.setFlagged(id, newFlagged)
        _uiState.update { state ->
            state.copy(
                recordings = state.recordings.map { item ->
                    if (item.id == id) item.copy(isFlagged = newFlagged) else item
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
        val recording = _uiState.value.recordings.find { it.id == id }
        // 若已有转写内容，优先复用缓存，避免重复提交与重复观察
        val cachedTranscript = recording?.fullTranscriptMarkdown ?: recording?.transcriptPreview
        if (!cachedTranscript.isNullOrBlank()) {
            val existingJob = _uiState.value.tingwuTaskIds[id]
            if (existingJob != null) {
                // 复用已完成任务，确保事件从统一的 observeJob 路径发出
                observeJob(id, existingJob, isFromCache = true)
            } else {
                emitTranscriptFromCache(
                    recordingId = id,
                    fileName = recording?.fileName ?: id,
                    transcript = cachedTranscript,
                    transcriptPreview = recording?.transcriptPreview,
                    fullTranscriptMarkdown = recording?.fullTranscriptMarkdown,
                    transcriptionUrl = recording?.transcriptionUrl
                )
            }
            return
        }
        if (recording?.transcriptionStatus == TranscriptionStatus.IN_PROGRESS) {
            // 已有任务在跑，直接复用现有 Flow，避免重复提交 Tingwu
            val existingJob = _uiState.value.tingwuTaskIds[id]
            if (existingJob != null) {
                observeJob(id, existingJob, isFromCache = true)
            }
            emitError("处理中…")
            return
        }
        val stored = storedAudios[id]
        val deviceFile = recordingSources[id]
        if (stored == null && deviceFile == null) {
            markRecordingStatus(id, TranscriptionStatus.ERROR)
            return emitError("未找到音频文件")
        }
        val sessionId = "session-${id}-${System.currentTimeMillis()}"
        // 先同步标记处理中，避免短时间重复点击导致重复提交
        _uiState.update { state ->
            state.copy(
                recordings = state.recordings.map { item ->
                    if (item.id == id) item.copy(transcriptionStatus = TranscriptionStatus.IN_PROGRESS) else item
                },
                sessionIds = state.sessionIds + (id to sessionId),
                transcriptPreviewRecording = null
            )
        }
        viewModelScope.launch(dispatchers.io) {
            val localFile = when {
                stored != null -> {
                    val path = stored.localUri.path
                    if (path.isNullOrBlank()) {
                        markRecordingStatus(id, TranscriptionStatus.ERROR)
                        emitError("找不到本地音频路径")
                        clearSessionBinding(id)
                        return@launch
                    }
                    java.io.File(path)
                }

                else -> {
                    val base = currentBaseUrl ?: run {
                        markRecordingStatus(id, TranscriptionStatus.ERROR)
                        emitError("设备未连接")
                        clearSessionBinding(id)
                        return@launch
                    }
                    val device = deviceFile ?: run {
                        markRecordingStatus(id, TranscriptionStatus.ERROR)
                        emitError("未找到音频文件")
                        clearSessionBinding(id)
                        return@launch
                    }
                    when (val download = mediaGateway.downloadFile(base, device)) {
                        is Result.Success -> download.data
                        is Result.Error -> {
                            markRecordingStatus(id, TranscriptionStatus.ERROR)
                            emitError(download.throwable.message)
                            clearSessionBinding(id)
                            return@launch
                        }
                    }
                }
            }
            val uploadPayload = when (val upload = transcriptionCoordinator.uploadAudio(localFile)) {
                is Result.Success -> upload.data
                is Result.Error -> {
                    markRecordingStatus(id, TranscriptionStatus.ERROR)
                    emitError(upload.throwable.message)
                    clearSessionBinding(id)
                    return@launch
                }
            }
            when (
                val submit = transcriptionCoordinator.submitTranscription(
                    audioAssetName = deviceFile?.name ?: stored?.displayName ?: id,
                    language = DEFAULT_TINGWU_LANGUAGE,
                    uploadPayload = uploadPayload,
                    sessionId = sessionId
                )
            ) {
                is Result.Success -> {
                    val taskId = submit.data
                    jobSessionIds[taskId] = sessionId
                    _uiState.update { state ->
                        state.copy(
                            recordings = state.recordings.map { recording ->
                                if (recording.id == id) {
                                    recording.copy(transcriptionStatus = TranscriptionStatus.IN_PROGRESS)
                                } else recording
                            },
                            sessionIds = state.sessionIds + (id to sessionId),
                            transcriptPreviewRecording = null,
                            tingwuTaskIds = state.tingwuTaskIds + (id to taskId)
                        )
                    }
                    observeJob(id, taskId)
                }

                is Result.Error -> {
                    markRecordingStatus(id, TranscriptionStatus.ERROR)
                    emitError(submit.throwable.message)
                    clearSessionBinding(id)
                }
            }
        }
    }

    private fun emitTranscriptFromCache(
        recordingId: String,
        fileName: String,
        transcript: String,
        transcriptPreview: String?,
        fullTranscriptMarkdown: String?,
        transcriptionUrl: String?
    ) {
        val sessionId = _uiState.value.sessionIds[recordingId] ?: "session-$recordingId"
        val jobId = _uiState.value.tingwuTaskIds[recordingId] ?: "tingwu-cache-$recordingId"
        _uiState.update { state ->
            state.copy(
                sessionIds = state.sessionIds + (recordingId to sessionId),
                tingwuTaskIds = state.tingwuTaskIds + (recordingId to jobId)
            )
        }
        _events.tryEmit(
            AudioFilesEvent.TranscriptReady(
                recordingId = recordingId,
                fileName = fileName,
                jobId = jobId,
                sessionId = sessionId,
                transcriptPreview = transcriptPreview ?: transcript.take(MAX_PREVIEW_LENGTH),
                fullTranscriptMarkdown = fullTranscriptMarkdown ?: transcript,
                transcriptionUrl = transcriptionUrl,
                isFromCache = true
            )
        )
    }

    fun onTranscriptClicked(id: String) {
        val target = _uiState.value.recordings.find { it.id == id } ?: return
        if (target.transcriptionStatus != TranscriptionStatus.DONE) return
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

    fun seedDemoDataIfEmpty() {
        if (_uiState.value.recordings.isNotEmpty()) return
        val now = System.currentTimeMillis()
        val demo = AudioRecordingUi(
            id = "d1",
            title = "示例通话 d1",
            fileName = "d1.wav",
            createdAtMillis = now,
            createdAtText = "刚刚",
            locationText = "上海 · 徐汇",
            durationMillis = 180_000,
            transcriptionStatus = TranscriptionStatus.DONE,
            transcriptPreview = "示例转写片段：客户需求是提升转化率。",
            fullTranscriptMarkdown = "## 通话摘要\n- 客户关注转化率\n- 需要跟进报价\n",
            sourceLabel = "演示数据"
        )
        _uiState.update {
            it.copy(
                recordings = listOf(demo),
                transcriptPreviewRecording = null,
                isLoading = false,
                errorMessage = null,
                loadErrorMessage = null
            )
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

    private fun observeStorage() {
        viewModelScope.launch(dispatchers.default) {
            audioStorageRepository.audios.collectLatest { audios ->
                storedAudios.clear()
                audios.forEach { storedAudios[it.id] = it }
                rebuildRecordings()
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
                val existing = _uiState.value.recordings.associateBy { it.id }
                val items = result.data
                    .filter { it.isAudio() }
                    .onEach { recordingSources[it.name] = it }
                    .mapNotNull { file ->
                        val previous = existing[file.name]
                        file.toUi(playingId)?.let { mergeUi(it, previous) }
                    }
                deviceUiItems = items
                rebuildRecordings(items)
                _uiState.update {
                    val retainedIds = deviceUiItems.map { it.id } + storedAudios.keys
                    it.copy(
                        transcriptPreviewRecording = null,
                        sessionIds = it.sessionIds.filterKeys { key -> retainedIds.contains(key) },
                        tingwuTaskIds = it.tingwuTaskIds.filterKeys { key -> retainedIds.contains(key) },
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

    private fun clearSessionBinding(id: String) {
        _uiState.update { state ->
            state.copy(
                sessionIds = state.sessionIds - id,
                tingwuTaskIds = state.tingwuTaskIds - id
            )
        }
    }

    private fun observeJob(
        recordingId: String,
        jobId: String,
        isFromCache: Boolean = false
    ) {
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
                                    } else {
                                        recording
                                    }
                                }
                            )
                        }
                        val fileName = recordingSources[recordingId]?.name ?: recordingId
                        val sessionId = jobSessionIds[jobId]
                            ?: _uiState.value.sessionIds[recordingId]
                            ?: "session-$jobId"
                        val transcript = state.transcriptMarkdown
                        if (transcript.isNotBlank()) {
                            _events.tryEmit(
                                AudioFilesEvent.TranscriptReady(
                                    recordingId = recordingId,
                                    fileName = storedAudios[recordingId]?.displayName ?: fileName,
                                    jobId = jobId,
                                    sessionId = sessionId,
                                    transcriptPreview = preview,
                                    fullTranscriptMarkdown = transcript,
                                    transcriptionUrl = state.transcriptionUrl,
                                    isFromCache = isFromCache
                                )
                            )
                        }
                        jobSessionIds.remove(jobId)
                        observingJobs.remove(jobId)
                    }

                    is com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState.Failed -> {
                        markRecordingStatus(recordingId, TranscriptionStatus.ERROR)
                        clearSessionBinding(recordingId)
                        jobSessionIds.remove(jobId)
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

    private fun StoredAudio.toUi(previous: AudioRecordingUi?): AudioRecordingUi {
        val title = displayName.substringBeforeLast('.', displayName)
        return AudioRecordingUi(
            id = id,
            title = title,
            fileName = displayName,
            durationMillis = durationMillis,
            createdAtMillis = timestampMillis,
            createdAtText = formatTimestamp(timestampMillis),
            locationText = null,
            transcriptionStatus = previous?.transcriptionStatus ?: TranscriptionStatus.NONE,
            transcriptPreview = previous?.transcriptPreview,
            fullTranscriptMarkdown = previous?.fullTranscriptMarkdown,
            isPlaying = previous?.isPlaying ?: false,
            hasLocalCopy = true,
            transcriptionUrl = previous?.transcriptionUrl,
            autoChaptersUrl = previous?.autoChaptersUrl,
            chapters = previous?.chapters,
            smartSummary = previous?.smartSummary,
            sourceLabel = if (origin == com.smartsales.feature.media.audiofiles.AudioOrigin.PHONE) "聊天上传" else "设备录音"
        )
    }

    private fun rebuildRecordings(
        deviceItems: List<AudioRecordingUi>? = null
    ) {
        val existing = _uiState.value.recordings.associateBy { it.id }
        val deviceList = deviceItems ?: deviceUiItems
        val storedList = storedAudios.values.mapNotNull { stored ->
            val previous = existing[stored.id]
            stored.toUi(previous)
        }
        val merged = LinkedHashMap<String, AudioRecordingUi>()
        deviceList.forEach { item ->
            val previous = existing[item.id]
            merged[item.id] = mergeUi(item, previous)
        }
        storedList.forEach { item ->
            val previous = merged[item.id] ?: existing[item.id]
            merged[item.id] = mergeUi(item, previous)
        }
        _uiState.update {
            it.copy(recordings = merged.values.toList())
        }
    }

    private fun mergeUi(
        base: AudioRecordingUi,
        previous: AudioRecordingUi?
    ): AudioRecordingUi {
        val isFlagged = flaggedRecordingsStore.isFlagged(base.id)
        if (previous == null) return base.copy(isFlagged = isFlagged)
        return base.copy(
            transcriptionStatus = previous.transcriptionStatus,
            transcriptPreview = previous.transcriptPreview ?: base.transcriptPreview,
            fullTranscriptMarkdown = previous.fullTranscriptMarkdown ?: base.fullTranscriptMarkdown,
            isPlaying = previous.isPlaying,
            hasLocalCopy = previous.hasLocalCopy || base.hasLocalCopy,
            transcriptionUrl = previous.transcriptionUrl ?: base.transcriptionUrl,
            autoChaptersUrl = previous.autoChaptersUrl ?: base.autoChaptersUrl,
            chapters = previous.chapters ?: base.chapters,
            smartSummary = previous.smartSummary ?: base.smartSummary,
            isFlagged = isFlagged
        )
    }

    private fun DeviceMediaFile.isAudio(): Boolean {
        val lower = mimeType.lowercase(Locale.ROOT)
        if (lower.startsWith("audio/")) return true
        val nameLower = name.lowercase(Locale.ROOT)
        return AUDIO_EXTENSIONS.any { nameLower.endsWith(it) }
    }

    private fun DeviceMediaFile.toUi(
        playingId: String?,
        previous: AudioRecordingUi? = null
    ): AudioRecordingUi? =
        AudioRecordingUi(
            id = name,
            title = name.substringBeforeLast(".", name),
            fileName = name,
            durationMillis = previous?.durationMillis,
            createdAtMillis = modifiedAtMillis,
            createdAtText = formatTimestamp(modifiedAtMillis),
            locationText = location?.takeIf { it.isNotBlank() },
            transcriptionStatus = previous?.transcriptionStatus ?: TranscriptionStatus.NONE,
            transcriptPreview = previous?.transcriptPreview,
            fullTranscriptMarkdown = previous?.fullTranscriptMarkdown,
            isPlaying = playingId == name,
            hasLocalCopy = previous?.hasLocalCopy ?: false,
            transcriptionUrl = previous?.transcriptionUrl,
            autoChaptersUrl = previous?.autoChaptersUrl,
            chapters = previous?.chapters,
            smartSummary = previous?.smartSummary,
            sourceLabel = mapSourceLabel(source)
        )

    companion object {
        private val AUDIO_EXTENSIONS = setOf(".mp3", ".wav", ".m4a", ".aac", ".flac", ".ogg")
        private const val DEFAULT_TINGWU_LANGUAGE = "zh-CN"
        private const val MAX_PREVIEW_LENGTH = 120
        private const val DEFAULT_SOURCE_LABEL = "设备录音"

        private fun formatTimestamp(timestamp: Long?): String {
            if (timestamp == null || timestamp <= 0) return "未知时间"
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            return formatter.format(Date(timestamp))
        }

        fun mapSourceLabel(raw: String?): String = when (raw?.trim()?.lowercase(Locale.ROOT)) {
            "phone", "mobile", "local", "local_recording" -> "本机录音"
            "device", "hardware", "gadget", "peripheral" -> "设备录音"
            "cloud", "oss", "remote" -> "云端录音"
            null, "" -> DEFAULT_SOURCE_LABEL
            else -> raw
        }
    }
}

sealed interface AudioFilesEvent {
    data class TranscriptReady(
        val recordingId: String,
        val fileName: String,
        val jobId: String,
        val sessionId: String,
        val transcriptPreview: String?,
        val fullTranscriptMarkdown: String?,
        val transcriptionUrl: String?,
        val isFromCache: Boolean = false
    ) : AudioFilesEvent
}
