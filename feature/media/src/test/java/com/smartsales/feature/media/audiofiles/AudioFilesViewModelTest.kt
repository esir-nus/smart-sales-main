package com.smartsales.feature.media.audiofiles

// 文件：feature/media/src/test/java/com/smartsales/feature/media/audiofiles/AudioFilesViewModelTest.kt
// 模块：:feature:media
// 说明：验证 AudioFilesViewModel 的刷新、删除与转写状态逻辑
// 作者：创建于 2025-11-21

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.OssUploadClient
import com.smartsales.data.aicore.OssUploadRequest
import com.smartsales.data.aicore.OssUploadResult
import com.smartsales.data.aicore.TingwuCoordinator
import com.smartsales.data.aicore.TingwuJobState
import com.smartsales.data.aicore.TingwuRequest
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import com.smartsales.feature.media.devicemanager.DeviceMediaGateway
import com.smartsales.feature.media.devicemanager.DeviceUploadSource
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private lateinit var tingwu: FakeTingwuCoordinator
    private lateinit var ossClient: FakeOssUploadClient
    private lateinit var playbackController: FakeAudioPlaybackController
    private lateinit var viewModel: AudioFilesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        gateway = FakeDeviceMediaGateway()
        tingwu = FakeTingwuCoordinator()
        ossClient = FakeOssUploadClient()
        playbackController = FakeAudioPlaybackController()
        viewModel = AudioFilesViewModel(
            mediaGateway = gateway,
            tingwuCoordinator = tingwu,
            ossUploadClient = ossClient,
            playbackController = playbackController,
            dispatcherProvider = FakeDispatcherProvider(dispatcher)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refresh loads recordings and toggles syncing`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("audio-1.wav", 1024, "audio/wav", 1_000L, "media/1", "dl/1")
        )

        viewModel.onRefresh()

        assertTrue(viewModel.uiState.value.isSyncing)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isSyncing)
        assertEquals(1, state.recordings.size)
        assertEquals("audio-1.wav", state.recordings.first().fileName)
    }

    @Test
    fun `delete removes item`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("audio-2.wav", 512, "audio/wav", 2_000L, "media/2", "dl/2")
        )
        viewModel.onRefresh()
        advanceUntilIdle()

        viewModel.onDelete("audio-2.wav")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.recordings.size)
    }

    @Test
    fun `error surfaces message`() = runTest(dispatcher) {
        gateway.fetchResult = Result.Error(IllegalStateException("网络异常"))

        viewModel.onRefresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("网络异常", state.errorMessage)
    }

    @Test
    fun `tingwu completion updates status`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("audio-3.wav", 2048, "audio/wav", 3_000L, "media/3", "dl/3")
        )

        viewModel.onSyncClicked()
        advanceUntilIdle()

        val jobId = tingwu.lastJobId!!
        tingwu.emit(
            jobId,
            TingwuJobState.Completed(
                jobId = jobId,
                transcriptMarkdown = "## 完整转写"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AudioRecordingStatus.Transcribed, state.recordings.first().status)
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

    private class FakeOssUploadClient : OssUploadClient {
        override suspend fun uploadAudio(request: OssUploadRequest): Result<OssUploadResult> {
            return Result.Success(
                OssUploadResult(
                    objectKey = "oss/${request.file.name}",
                    presignedUrl = "https://oss.example/${request.file.name}"
                )
            )
        }
    }

    private class FakeTingwuCoordinator : TingwuCoordinator {
        private val states = mutableMapOf<String, MutableStateFlow<TingwuJobState>>()
        var lastJobId: String? = null

        override suspend fun submit(request: TingwuRequest): Result<String> {
            val id = "job-${states.size + 1}"
            lastJobId = id
            states[id] = MutableStateFlow(TingwuJobState.InProgress(id, 10))
            return Result.Success(id)
        }

        override fun observeJob(jobId: String): StateFlow<TingwuJobState> =
            states.getOrPut(jobId) { MutableStateFlow(TingwuJobState.Idle) }

        fun emit(jobId: String, state: TingwuJobState) {
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
}
