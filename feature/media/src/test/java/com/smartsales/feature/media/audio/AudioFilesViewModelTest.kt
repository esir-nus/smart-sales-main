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
import com.smartsales.feature.media.audiofiles.DeviceHttpEndpointProvider
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import com.smartsales.feature.media.devicemanager.DeviceMediaGateway
import com.smartsales.feature.media.devicemanager.DeviceUploadSource
import com.smartsales.feature.media.audio.TranscriptionStatus
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.StateFlow
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
    private lateinit var viewModel: AudioFilesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        gateway = FakeDeviceMediaGateway()
        syncCoordinator = FakeMediaSyncCoordinator()
        endpointProvider = FakeDeviceHttpEndpointProvider()
        viewModel = AudioFilesViewModel(
            mediaGateway = gateway,
            mediaSyncCoordinator = syncCoordinator,
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

    private class FakeDeviceMediaGateway : DeviceMediaGateway {
        var files: List<DeviceMediaFile> = emptyList()
        var fetchResult: Result<List<DeviceMediaFile>>? = null

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

        override suspend fun downloadFile(baseUrl: String, file: DeviceMediaFile): Result<File> =
            Result.Success(File("tmp"))
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
}
