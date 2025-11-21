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
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        Dispatchers.setMain(UnconfinedTestDispatcher())
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
    fun `initial load fetches audio list when endpoint is ready`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile(
                name = "a1.wav",
                sizeBytes = 100,
                mimeType = "audio/wav",
                modifiedAtMillis = 1_000L,
                mediaUrl = "http://d/1",
                downloadUrl = "http://d/1"
            )
        )

        endpointProvider.emit("http://10.0.0.1:8000")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.recordings.size)
        assertEquals("a1", state.recordings.first().title)
    }

    @Test
    fun `sync toggles flag and refreshes items`() = runTest(dispatcher) {
        endpointProvider.emit("http://10.0.0.2:8000")
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
        endpointProvider.emit("http://10.0.0.3:8000")
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
        endpointProvider.emit("http://10.0.0.4:8000")
        advanceUntilIdle()

        assertEquals("boom", viewModel.uiState.value.errorMessage)
        viewModel.onErrorDismissed()
        assertEquals(null, viewModel.uiState.value.errorMessage)
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
