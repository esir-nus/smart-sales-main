package com.smartsales.feature.media.audio

// 文件：feature/media/src/test/java/com/smartsales/feature/media/audio/AudioFilesViewModelTest.kt
// 模块：:feature:media
// 说明：验证 AudioFilesViewModel 的加载、同步与错误处理逻辑
// 作者：创建于 2025-11-21

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.media.MediaClip
import com.smartsales.feature.media.MediaClipStatus
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import com.smartsales.feature.media.audiofiles.DeviceHttpEndpointProvider
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import com.smartsales.feature.media.devicemanager.DeviceMediaGateway
import com.smartsales.feature.media.devicemanager.DeviceUploadSource
import com.smartsales.feature.media.audio.TranscriptionStatus
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioFilesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var gateway: FakeDeviceMediaGateway
    private lateinit var syncCoordinator: FakeMediaSyncCoordinator
    private lateinit var endpointProvider: FakeDeviceHttpEndpointProvider
    private lateinit var transcriptionCoordinator: FakeTranscriptionCoordinator
    private lateinit var viewModel: AudioFilesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        gateway = FakeDeviceMediaGateway()
        syncCoordinator = FakeMediaSyncCoordinator()
        endpointProvider = FakeDeviceHttpEndpointProvider()
        transcriptionCoordinator = FakeTranscriptionCoordinator()
        viewModel = AudioFilesViewModel(
            mediaGateway = gateway,
            mediaSyncCoordinator = syncCoordinator,
            transcriptionCoordinator = transcriptionCoordinator,
            endpointProvider = endpointProvider,
            dispatchers = FakeDispatcherProvider(dispatcher)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load shows loading then populates audio files`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("clip.wav", 100, "audio/wav", 1_000L, "http://m/1", "http://d/1"),
            DeviceMediaFile("video.mp4", 200, "video/mp4", 2_000L, "http://m/2", "http://d/2")
        )

        endpointProvider.emit("http://10.0.0.1:8000")
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.recordings.size)
        assertEquals("clip", state.recordings.first().title)
        assertEquals(TranscriptionStatus.NONE, state.recordings.first().transcriptionStatus)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun `initial load surfaces error when fetch fails`() = runTest(dispatcher) {
        gateway.fetchResult = Result.Error(IllegalStateException("offline"))

        endpointProvider.emit("http://10.0.0.2:8000")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("offline", state.errorMessage)
        assertTrue(state.recordings.isEmpty())
        assertEquals("offline", state.loadErrorMessage)
    }

    @Test
    fun `sync toggles flag and refreshes items`() = runTest(dispatcher) {
        endpointProvider.emit("http://10.0.0.3:8000")
        gateway.files = listOf(
            DeviceMediaFile("old.mp3", 1, "audio/mpeg", 10L, "m1", "d1")
        )
        advanceUntilIdle()

        gateway.files = listOf(
            DeviceMediaFile("new.mp3", 1, "audio/mpeg", 20L, "m2", "d2")
        )

        viewModel.onSyncClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSyncing)
        assertEquals(1, state.recordings.size)
        assertEquals("new", state.recordings.first().title)
        assertTrue(syncCoordinator.triggered)
    }

    @Test
    fun `delete removes item and reloads list`() = runTest(dispatcher) {
        endpointProvider.emit("http://10.0.0.4:8000")
        gateway.files = listOf(
            DeviceMediaFile("keep.mp3", 1, "audio/mpeg", 10L, "m2", "d2"),
            DeviceMediaFile("drop.mp3", 1, "audio/mpeg", 20L, "m3", "d3")
        )
        advanceUntilIdle()

        viewModel.onDeleteClicked("drop.mp3")
        advanceUntilIdle()

        val names = viewModel.uiState.value.recordings.map { it.fileName }
        assertEquals(listOf("keep.mp3"), names)
    }

    @Test
    fun `errors populate errorMessage and can be dismissed`() = runTest(dispatcher) {
        gateway.fetchResult = Result.Error(IllegalStateException("boom"))

        endpointProvider.emit("http://10.0.0.5:8000")
        advanceUntilIdle()

        assertEquals("boom", viewModel.uiState.value.errorMessage)
        viewModel.onErrorDismissed()
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `transcribe click marks recording in progress`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("voice.mp3", 1, "audio/mpeg", 10L, "m2", "d2")
        )
        endpointProvider.emit("http://10.0.0.6:8000")
        advanceUntilIdle()

        viewModel.onTranscribeClicked("voice.mp3")
        val state = viewModel.uiState.value
        val recording = state.recordings.first()
        assertEquals(TranscriptionStatus.IN_PROGRESS, recording.transcriptionStatus)
        assertEquals(null, state.transcriptPreviewRecording)
        assertFalse(recording.transcriptPreview.isNullOrBlank())
    }

    @Test
    fun `transcript dialog opens and dismisses`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("voice2.mp3", 1, "audio/mpeg", 10L, "m2", "d2")
        )
        endpointProvider.emit("http://10.0.0.7:8000")
        advanceUntilIdle()
        viewModel.onTranscribeClicked("voice2.mp3")

        viewModel.onTranscriptClicked("voice2.mp3")
        assertEquals("voice2.mp3", viewModel.uiState.value.transcriptPreviewRecording?.id)

        viewModel.onTranscriptDismissed()
        assertEquals(null, viewModel.uiState.value.transcriptPreviewRecording)
    }

    @Test
    fun `transcribe triggers download upload submit and stores task id`() = runTest(dispatcher) {
        val file = DeviceMediaFile("clip.mp3", 1, "audio/mpeg", 10L, "m", "d")
        gateway.files = listOf(file)
        endpointProvider.emit("http://10.0.0.8:8000")
        advanceUntilIdle()

        viewModel.onTranscribeClicked("clip.mp3")
        advanceUntilIdle()

        assertTrue(gateway.downloadCalled)
        assertEquals(file.name, transcriptionCoordinator.lastUploadedFile?.name)
        assertEquals(TranscriptionStatus.IN_PROGRESS, viewModel.uiState.value.recordings.first().transcriptionStatus)
        assertEquals("task-1", viewModel.uiState.value.tingwuTaskIds["clip.mp3"])
    }

    @Test
    fun `job completion updates status and preview`() = runTest(dispatcher) {
        val file = DeviceMediaFile("clip3.mp3", 1, "audio/mpeg", 10L, "m", "d")
        gateway.files = listOf(file)
        endpointProvider.emit("http://10.0.0.10:8000")
        advanceUntilIdle()

        viewModel.onTranscribeClicked("clip3.mp3")
        transcriptionCoordinator.emitState("task-1", AudioTranscriptionJobState.InProgress("task-1", 10))
        transcriptionCoordinator.emitState(
            "task-1",
            AudioTranscriptionJobState.Completed(
                jobId = "task-1",
                transcriptMarkdown = "第一行内容\n更多",
                transcriptionUrl = "https://example.com/transcription.json",
                autoChaptersUrl = "https://example.com/chapters.json",
                chapters = listOf(com.smartsales.feature.media.audio.TingwuChapterUi("开场", 1000, 5000)),
                smartSummary = com.smartsales.feature.media.audio.TingwuSmartSummaryUi(
                    summary = "概览",
                    keyPoints = listOf("要点1"),
                    actionItems = listOf("行动A")
                )
            )
        )
        advanceUntilIdle()

        val recording = viewModel.uiState.value.recordings.first()
        assertEquals(TranscriptionStatus.DONE, recording.transcriptionStatus)
        assertEquals("第一行内容", recording.transcriptPreview)
        assertEquals("第一行内容\n更多", recording.fullTranscriptMarkdown)
        assertEquals("https://example.com/transcription.json", recording.transcriptionUrl)
        assertEquals("https://example.com/chapters.json", recording.autoChaptersUrl)
        assertEquals(1, recording.chapters?.size)
        assertEquals("概览", recording.smartSummary?.summary)
        assertEquals(listOf("要点1"), recording.smartSummary?.keyPoints)
        assertEquals(listOf("行动A"), recording.smartSummary?.actionItems)
    }

    @Test
    fun `job completion emits transcript event`() = runTest(dispatcher) {
        val file = DeviceMediaFile("clip5.mp3", 1, "audio/mpeg", 10L, "m", "d")
        gateway.files = listOf(file)
        endpointProvider.emit("http://10.0.0.12:8000")

        val received = mutableListOf<AudioFilesEvent>()
        val collectJob = backgroundScope.launch {
            viewModel.events.collect { received += it }
        }

        advanceUntilIdle()

        viewModel.onTranscribeClicked("clip5.mp3")
        transcriptionCoordinator.emitState(
            "task-1",
            AudioTranscriptionJobState.Completed(
                jobId = "task-1",
                transcriptMarkdown = "转写完成",
                transcriptionUrl = "https://example.com/transcription.json"
            )
        )
        advanceUntilIdle()

        val ready = received.filterIsInstance<AudioFilesEvent.TranscriptReady>().firstOrNull()
        assertEquals("clip5.mp3", ready?.recordingId)
        assertEquals("task-1", ready?.jobId)
        assertEquals("转写完成", ready?.fullTranscriptMarkdown)
        collectJob.cancel()
    }

    @Test
    fun `job failure updates status to error`() = runTest(dispatcher) {
        val file = DeviceMediaFile("clip4.mp3", 1, "audio/mpeg", 10L, "m", "d")
        gateway.files = listOf(file)
        endpointProvider.emit("http://10.0.0.11:8000")
        advanceUntilIdle()

        viewModel.onTranscribeClicked("clip4.mp3")
        transcriptionCoordinator.emitState("task-1", AudioTranscriptionJobState.Failed("task-1", "fail"))
        advanceUntilIdle()

        val recording = viewModel.uiState.value.recordings.first()
        assertEquals(TranscriptionStatus.ERROR, recording.transcriptionStatus)
    }

    @Test
    fun `transcribe failure sets error status`() = runTest(dispatcher) {
        val file = DeviceMediaFile("clip2.mp3", 1, "audio/mpeg", 10L, "m", "d")
        gateway.files = listOf(file)
        gateway.downloadResult = Result.Error(IllegalStateException("no download"))
        endpointProvider.emit("http://10.0.0.9:8000")
        advanceUntilIdle()

        viewModel.onTranscribeClicked("clip2.mp3")
        advanceUntilIdle()

        assertEquals(TranscriptionStatus.ERROR, viewModel.uiState.value.recordings.first().transcriptionStatus)
        assertTrue(viewModel.uiState.value.tingwuTaskIds.isEmpty())
        assertEquals("no download", viewModel.uiState.value.errorMessage)
    }

    private class FakeDeviceMediaGateway : DeviceMediaGateway {
        var files: List<DeviceMediaFile> = emptyList()
        var fetchResult: Result<List<DeviceMediaFile>>? = null
        var downloadResult: Result<File>? = null
        var downloadCalled = false

        override suspend fun fetchFiles(baseUrl: String): Result<List<DeviceMediaFile>> =
            fetchResult ?: Result.Success(files)

        override suspend fun uploadFile(baseUrl: String, source: DeviceUploadSource): Result<Unit> =
            Result.Success(Unit)

        override suspend fun applyFile(baseUrl: String, fileName: String): Result<Unit> =
            Result.Success(Unit)

        override suspend fun deleteFile(baseUrl: String, fileName: String): Result<Unit> {
            files = files.filterNot { it.name == fileName }
            return Result.Success(Unit)
        }

        override suspend fun downloadFile(baseUrl: String, file: DeviceMediaFile): Result<File> {
            downloadCalled = true
            return downloadResult ?: Result.Success(File(file.name))
        }
    }

    private class FakeMediaSyncCoordinator : MediaSyncCoordinator {
        private val _state = MutableStateFlow(
            MediaSyncState(
                connectionState = ConnectionState.Disconnected,
                syncing = false,
                items = emptyList()
            )
        )
        var triggered = false
        override val state: StateFlow<MediaSyncState> = _state.asStateFlow()

        override suspend fun triggerSync(): Result<Unit> {
            triggered = true
            _state.update {
                it.copy(
                    syncing = false,
                    items = listOf(
                        MediaClip(
                            id = "c1",
                            title = "clip",
                            customer = "",
                            recordedAtMillis = 0L,
                            durationSeconds = 0,
                            sourceDeviceName = "",
                            status = MediaClipStatus.Ready,
                            transcriptSource = null
                        )
                    )
                )
            }
            return Result.Success(Unit)
        }
    }

    private class FakeDeviceHttpEndpointProvider(
        initial: String? = null
    ) : DeviceHttpEndpointProvider {
        private val flow = MutableStateFlow(initial)
        override val deviceBaseUrl: Flow<String?> = flow.asStateFlow()
        fun emit(value: String?) {
            flow.value = value
        }
    }

    private class FakeTranscriptionCoordinator : AudioTranscriptionCoordinator {
        var uploadResult: Result<AudioUploadPayload> = Result.Success(
            AudioUploadPayload("object-key", "presigned")
        )
        var submitResult: Result<String> = Result.Success("task-1")
        var lastUploadedFile: File? = null
        var lastSubmitAssetName: String? = null
        private val jobStates = mutableMapOf<String, MutableStateFlow<AudioTranscriptionJobState>>()

        override suspend fun uploadAudio(file: File): Result<AudioUploadPayload> {
            lastUploadedFile = file
            return uploadResult
        }

        override suspend fun submitTranscription(
            audioAssetName: String,
            language: String,
            uploadPayload: AudioUploadPayload
        ): Result<String> {
            lastSubmitAssetName = audioAssetName
            return submitResult
        }

        override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> {
            return jobStates.getOrPut(jobId) { MutableStateFlow<AudioTranscriptionJobState>(AudioTranscriptionJobState.Idle) }
        }

        fun emitState(jobId: String, state: AudioTranscriptionJobState) {
            val flow = jobStates.getOrPut(jobId) { MutableStateFlow<AudioTranscriptionJobState>(AudioTranscriptionJobState.Idle) }
            flow.value = state
        }
    }
}
