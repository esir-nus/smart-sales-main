package com.smartsales.feature.media.audiofiles

// 文件：feature/media/src/test/java/com/smartsales/feature/media/audiofiles/AudioFilesViewModelTest.kt
// 模块：:feature:media
// 说明：验证 AudioFilesViewModel 基于本地存储的同步、上传与转写流程
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private lateinit var storageRepository: FakeAudioStorageRepository
    private lateinit var viewModel: AudioFilesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        gateway = FakeDeviceMediaGateway()
        transcription = FakeAudioTranscriptionCoordinator()
        playbackController = FakeAudioPlaybackController()
        endpointProvider = FakeDeviceHttpEndpointProvider()
        storageRepository = FakeAudioStorageRepository()
        viewModel = AudioFilesViewModel(
            mediaGateway = gateway,
            transcriptionCoordinator = transcription,
            playbackController = playbackController,
            endpointProvider = endpointProvider,
            audioStorageRepository = storageRepository,
            dispatcherProvider = FakeDispatcherProvider(dispatcher)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sync imports new device audio into storage`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("dev-1.wav", 1024, "audio/wav", 1_000L, "media/1", "http://d/1")
        )
        endpointProvider.emit("http://192.168.0.10:8000")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.recordings.size)
        assertEquals(AudioOrigin.DEVICE, viewModel.uiState.value.recordings.first().origin)
    }

    @Test
    fun `import from phone adds phone origin audio`() = runTest(dispatcher) {
        endpointProvider.emit("http://192.168.0.11:8000")
        advanceUntilIdle()

        val uri = android.net.Uri.fromFile(File.createTempFile("phone", ".mp3"))
        viewModel.onImportFromPhone(uri)
        advanceUntilIdle()

        val recording = viewModel.uiState.value.recordings.first()
        assertEquals(AudioOrigin.PHONE, recording.origin)
    }

    @Test
    fun `transcribe emits navigation event for stored audio`() = runTest(dispatcher) {
        val local = File.createTempFile("trans", ".wav")
        storageRepository.add(
            StoredAudio(
                id = "audio-t.wav",
                displayName = "audio-t.wav",
                sizeBytes = 2048,
                durationMillis = null,
                timestampMillis = 2_000L,
                origin = AudioOrigin.DEVICE,
                localUri = android.net.Uri.fromFile(local)
            )
        )
        advanceUntilIdle()

        val events = mutableListOf<AudioFilesNavigation>()
        val job = backgroundScope.launch { viewModel.events.collect { events.add(it) } }

        viewModel.onTranscribe("audio-t.wav")
        advanceUntilIdle()

        assertTrue(events.first() is AudioFilesNavigation.TranscribeToChat)
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

        override suspend fun deleteFile(baseUrl: String, fileName: String): Result<Unit> =
            Result.Success(Unit)

        override suspend fun downloadFile(baseUrl: String, file: DeviceMediaFile): Result<File> =
            Result.Success(File.createTempFile("dl", ".wav"))
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
    }

    private class FakeAudioPlaybackController : AudioPlaybackController {
        override suspend fun play(file: File): Result<Unit> = Result.Success(Unit)
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

    private class FakeAudioStorageRepository : AudioStorageRepository {
        private val listFlow = MutableStateFlow<List<StoredAudio>>(emptyList())
        override val audios: Flow<List<StoredAudio>> = listFlow.asStateFlow()

        fun add(audio: StoredAudio) {
            listFlow.update { it + audio }
        }

        override suspend fun importFromDevice(baseUrl: String, file: DeviceMediaFile): StoredAudio {
            val stored = StoredAudio(
                id = file.name,
                displayName = file.name,
                sizeBytes = file.sizeBytes,
                durationMillis = null,
                timestampMillis = file.modifiedAtMillis,
                origin = AudioOrigin.DEVICE,
                localUri = android.net.Uri.fromFile(File.createTempFile("dev", ".wav"))
            )
            listFlow.update { it + stored }
            return stored
        }

        override suspend fun importFromPhone(uri: android.net.Uri): StoredAudio {
            val stored = StoredAudio(
                id = "phone-${listFlow.value.size + 1}.wav",
                displayName = "phone-${listFlow.value.size + 1}.wav",
                sizeBytes = 100,
                durationMillis = null,
                timestampMillis = System.currentTimeMillis(),
                origin = AudioOrigin.PHONE,
                localUri = uri
            )
            listFlow.update { it + stored }
            return stored
        }

        override suspend fun delete(audioId: String) {
            listFlow.update { it.filterNot { audio -> audio.id == audioId } }
        }
    }
}
