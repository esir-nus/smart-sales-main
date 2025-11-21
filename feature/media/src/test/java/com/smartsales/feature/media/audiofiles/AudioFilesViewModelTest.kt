package com.smartsales.feature.media.audiofiles

// 文件：feature/media/src/test/java/com/smartsales/feature/media/audiofiles/AudioFilesViewModelTest.kt
// 模块：:feature:media
// 说明：验证 AudioFilesViewModel 的刷新、删除与转写状态逻辑
// 作者：创建于 2025-11-21

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import com.smartsales.feature.media.devicemanager.DeviceMediaGateway
import com.smartsales.feature.media.devicemanager.DeviceUploadSource
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioFilesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var gateway: FakeDeviceMediaGateway
    private lateinit var transcription: FakeAudioTranscriptionCoordinator
    private lateinit var playbackController: FakeAudioPlaybackController
    private lateinit var endpointProvider: FakeDeviceHttpEndpointProvider
    private lateinit var viewModel: AudioFilesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        gateway = FakeDeviceMediaGateway()
        transcription = FakeAudioTranscriptionCoordinator()
        playbackController = FakeAudioPlaybackController()
        endpointProvider = FakeDeviceHttpEndpointProvider()
        viewModel = AudioFilesViewModel(
            mediaGateway = gateway,
            transcriptionCoordinator = transcription,
            playbackController = playbackController,
            endpointProvider = endpointProvider,
            dispatcherProvider = FakeDispatcherProvider(dispatcher)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `auto refresh populates recordings when endpoint available`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("audio-1.wav", 1024, "audio/wav", 1_000L, "media/1", "dl/1")
        )

        endpointProvider.emit("http://192.168.0.10:8000")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.recordings.size)
        assertEquals("audio-1.wav", state.recordings.first().fileName)
    }

    @Test
    fun `delete removes item`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("audio-2.wav", 512, "audio/wav", 2_000L, "media/2", "dl/2")
        )
        endpointProvider.emit("http://192.168.0.20:8000")
        advanceUntilIdle()

        viewModel.onDelete("audio-2.wav")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.recordings.size)
    }

    @Test
    fun `error surfaces message`() = runTest(dispatcher) {
        gateway.fetchResult = Result.Error(IllegalStateException("网络异常"))

        endpointProvider.emit("http://192.168.0.30:8000")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("网络异常", state.errorMessage)
    }

    @Test
    fun `sync does not trigger transcription automatically`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("audio-auto.wav", 2048, "audio/wav", 3_500L, "media/a", "dl/a")
        )

        endpointProvider.emit("http://192.168.0.35:8000")
        advanceUntilIdle()

        viewModel.onSyncClicked()
        advanceUntilIdle()

        assertEquals(AudioRecordingStatus.Idle, viewModel.uiState.value.recordings.first().status)
    }

    @Test
    fun `tingwu completion updates status`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("audio-3.wav", 2048, "audio/wav", 3_000L, "media/3", "dl/3")
        )

        endpointProvider.emit("http://192.168.0.40:8000")
        advanceUntilIdle()

        viewModel.onTranscribe("audio-3.wav")
        advanceUntilIdle()

        val jobId = transcription.lastJobId!!
        transcription.emit(
            jobId,
            AudioTranscriptionJobState.Completed(
                jobId = jobId,
                transcriptMarkdown = "## 完整转写"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AudioRecordingStatus.Transcribed, state.recordings.first().status)
    }

    @Test
    fun `recordings cleared when endpoint lost`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("audio-4.wav", 1024, "audio/wav", 4_000L, "media/4", "dl/4")
        )
        endpointProvider.emit("http://192.168.0.50:8000")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.recordings.size)

        endpointProvider.emit(null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.recordings.isEmpty())
        assertEquals(null, state.baseUrl)
    }

    @Test
    fun `non audio files are filtered out`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("audio-5.wav", 1234, "audio/wav", 5_000L, "media/5", "dl/5"),
            DeviceMediaFile("photo-1.jpg", 888, "image/jpeg", 5_100L, "media/img", "dl/img")
        )

        endpointProvider.emit("http://192.168.0.60:8000")
        advanceUntilIdle()

        val names = viewModel.uiState.value.recordings.map { it.fileName }
        assertEquals(listOf("audio-5.wav"), names)
    }

    @Test
    fun `transcribe emits navigation for specific recording`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("audio-6.wav", 2048, "audio/wav", 6_000L, "media/6", "dl/6")
        )
        endpointProvider.emit("http://192.168.0.70:8000")
        advanceUntilIdle()

        val events = mutableListOf<AudioFilesNavigation>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        viewModel.onTranscribe("audio-6.wav")
        advanceUntilIdle()

        val nav = events.first() as AudioFilesNavigation.TranscribeToChat
        assertEquals("audio-6.wav", nav.fileName)
        assertEquals("audio-6.wav", transcription.lastSubmittedAsset)
        assertEquals(AudioRecordingStatus.Transcribing, viewModel.uiState.value.recordings.first().status)

        job.cancel()
    }

    private class FakeDeviceMediaGateway : DeviceMediaGateway {
        var files: List<DeviceMediaFile> = emptyList()
        var fetchResult: Result<List<DeviceMediaFile>>? = null

        override suspend fun fetchFiles(baseUrl: String): Result<List<DeviceMediaFile>> {
            return fetchResult ?: Result.Success(files)
        }

        override suspend fun uploadFile(baseUrl: String, source: DeviceUploadSource): Result<Unit> =
            Result.Success(Unit)

        override suspend fun applyFile(baseUrl: String, fileName: String): Result<Unit> =
            Result.Success(Unit)

        override suspend fun deleteFile(baseUrl: String, fileName: String): Result<Unit> {
            files = files.filterNot { it.name == fileName }
            return Result.Success(Unit)
        }

        override suspend fun downloadFile(baseUrl: String, file: DeviceMediaFile): Result<File> {
            return Result.Success(File.createTempFile("audio", ".tmp"))
        }
    }

    private class FakeAudioTranscriptionCoordinator : AudioTranscriptionCoordinator {
        private val states = mutableMapOf<String, MutableStateFlow<AudioTranscriptionJobState>>()
        var lastJobId: String? = null
        var lastSubmittedAsset: String? = null

        override suspend fun uploadAudio(file: File): Result<AudioUploadPayload> {
            return Result.Success(
                AudioUploadPayload(
                    objectKey = "oss/${file.name}",
                    presignedUrl = "https://oss.example/${file.name}"
                )
            )
        }

        override suspend fun submitTranscription(
            audioAssetName: String,
            language: String,
            uploadPayload: AudioUploadPayload,
        ): Result<String> {
            val id = "job-${states.size + 1}"
            lastSubmittedAsset = audioAssetName
            lastJobId = id
            states[id] = MutableStateFlow(AudioTranscriptionJobState.InProgress(id, 10))
            return Result.Success(id)
        }

        override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> =
            states.getOrPut(jobId) { MutableStateFlow(AudioTranscriptionJobState.Idle) }

        fun emit(jobId: String, state: AudioTranscriptionJobState) {
            states[jobId]?.value = state
        }
    }

    private class FakeAudioPlaybackController : AudioPlaybackController {
        var playCalls = 0
        override suspend fun play(file: File): Result<Unit> {
            playCalls++
            return Result.Success(Unit)
        }

        override suspend fun pause(): Result<Unit> = Result.Success(Unit)
    }

    private class FakeDeviceHttpEndpointProvider(
        initialUrl: String? = null
    ) : DeviceHttpEndpointProvider {
        private val flow = MutableStateFlow(initialUrl)
        override val deviceBaseUrl: Flow<String?> = flow
        fun emit(value: String?) {
            flow.value = value
        }
    }
}
