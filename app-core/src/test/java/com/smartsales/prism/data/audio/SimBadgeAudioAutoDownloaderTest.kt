package com.smartsales.prism.data.audio

import android.content.Context
import com.smartsales.core.util.Result
import com.smartsales.data.oss.OssUploader
import com.smartsales.prism.domain.audio.AudioLocalAvailability
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.RecordingNotification
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import com.smartsales.prism.service.DownloadServiceOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SimBadgeAudioAutoDownloaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var connectivityBridge: FakeConnectivityBridge
    private lateinit var runtime: SimAudioRepositoryRuntime
    private lateinit var orchestrator: DownloadServiceOrchestrator

    @Before
    fun setup() {
        context = mock()
        whenever(context.filesDir).thenReturn(tempFolder.root)
        connectivityBridge = FakeConnectivityBridge()
        runtime = SimAudioRepositoryRuntime(
            context = context,
            connectivityBridge = connectivityBridge,
            ossUploader = mock<OssUploader>(),
            tingwuPipeline = mock<TingwuPipeline>()
        )
        orchestrator = mock()
    }

    @Test
    fun `rec notification starts foreground service and upgrades placeholder after download`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        val downloader = SimBadgeAudioAutoDownloader(connectivityBridge, runtime, orchestrator)
        val localFile = tempFolder.newFile("rec.wav").apply { writeText("audio-content") }
        connectivityBridge.downloadResults["rec_20260416_120000.wav"] = WavDownloadResult.Success(
            localFile = localFile,
            originalFilename = "rec_20260416_120000.wav",
            sizeBytes = 2048L
        )

        advanceUntilIdle()
        connectivityBridge.audioNotifications.emit(
            RecordingNotification.AudioRecordingReady("rec_20260416_120000.wav")
        )
        advanceUntilIdle()

        verify(orchestrator).notifyDownloadStarting()
        val entry = runtime.audioFiles.value.single { it.filename == "rec_20260416_120000.wav" }
        assertEquals(AudioLocalAvailability.READY, entry.localAvailability)
        assertTrue(entry.id.isNotBlank())
        @Suppress("UNUSED_VARIABLE")
        val keepReference = downloader
    }

    @Test
    fun `blank rec notification is ignored without starting foreground service`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        SimBadgeAudioAutoDownloader(connectivityBridge, runtime, orchestrator)

        advanceUntilIdle()
        connectivityBridge.audioNotifications.emit(
            RecordingNotification.AudioRecordingReady("   ")
        )
        advanceUntilIdle()

        assertTrue(runtime.audioFiles.value.isEmpty())
        org.mockito.kotlin.verifyNoInteractions(orchestrator)
    }

    private fun bindRuntimeToTestScheduler(scheduler: TestCoroutineScheduler) {
        val dispatcher = StandardTestDispatcher(scheduler)
        runtime.overrideConcurrencyForTests(
            dispatcher = dispatcher,
            scope = CoroutineScope(SupervisorJob() + dispatcher)
        )
    }

    private class FakeConnectivityBridge : ConnectivityBridge {
        override val connectionState = MutableStateFlow<BadgeConnectionState>(
            BadgeConnectionState.Disconnected
        )
        override val managerStatus = MutableStateFlow<BadgeManagerStatus>(
            BadgeManagerStatus.Disconnected
        )
        val audioNotifications = MutableSharedFlow<RecordingNotification.AudioRecordingReady>(
            extraBufferCapacity = 4
        )
        val downloadResults = mutableMapOf<String, WavDownloadResult>()

        override suspend fun downloadRecording(
            filename: String,
            onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
        ): WavDownloadResult {
            return downloadResults[filename]
                ?: WavDownloadResult.Error(
                    code = WavDownloadResult.ErrorCode.DOWNLOAD_FAILED,
                    message = "missing stub"
                )
        }

        override suspend fun listRecordings(): Result<List<String>> = Result.Success(emptyList())

        override fun recordingNotifications(): Flow<RecordingNotification> = emptyFlow()

        override fun audioRecordingNotifications(): Flow<RecordingNotification.AudioRecordingReady> {
            return audioNotifications
        }

        override suspend fun isReady(): Boolean = true

        override suspend fun deleteRecording(filename: String): Boolean = true
    }
}
