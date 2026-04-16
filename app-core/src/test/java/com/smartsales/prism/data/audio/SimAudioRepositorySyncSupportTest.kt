package com.smartsales.prism.data.audio

import android.content.Context
import com.smartsales.core.util.Result
import com.smartsales.data.oss.OssUploader
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioLocalAvailability
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.RecordingNotification
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import com.smartsales.prism.service.DownloadServiceOrchestrator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SimAudioRepositorySyncSupportTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var connectivityBridge: FakeConnectivityBridge
    private lateinit var runtime: SimAudioRepositoryRuntime
    private lateinit var storeSupport: SimAudioRepositoryStoreSupport
    private lateinit var orchestrator: DownloadServiceOrchestrator
    private lateinit var syncSupport: SimAudioRepositorySyncSupport

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
        storeSupport = SimAudioRepositoryStoreSupport(runtime)
        orchestrator = mock<DownloadServiceOrchestrator>()
        syncSupport = SimAudioRepositorySyncSupport(
            runtime = runtime,
            storeSupport = storeSupport,
            orchestrator = orchestrator
        )
    }

    @Test
    fun `manual sync fails at strict preflight before list work begins`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = false

        val error = runCatching {
            syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        }.exceptionOrNull()

        assertEquals(SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE, error?.message)
        assertEquals(listOf("isReady"), connectivityBridge.calls)
    }

    @Test
    fun `auto sync skips quietly when strict preflight is not ready`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = false

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.AUTO)

        assertEquals(SimBadgeSyncSkippedReason.NOT_READY, outcome.skippedReason)
        assertEquals(listOf("isReady"), connectivityBridge.calls)
    }

    @Test
    fun `manual sync reaches list stage after ready preflight and surfaces list failure`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Error(Exception("socket timeout"))

        val error = runCatching {
            syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        }.exceptionOrNull()

        assertEquals(SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE, error?.message)
        assertEquals(listOf("isReady", "listRecordings"), connectivityBridge.calls)
    }

    @Test
    fun `manual sync reaches download stage after ready preflight and surfaces download failure`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log_20260327_135948.wav"))
        connectivityBridge.downloadResults["log_20260327_135948.wav"] = WavDownloadResult.Error(
            code = WavDownloadResult.ErrorCode.DOWNLOAD_FAILED,
            message = "timeout"
        )

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        advanceUntilIdle()

        assertEquals(SimBadgeSyncResultBranch.QUEUED, outcome.resultBranch)
        assertTrue(
            connectivityBridge.calls.containsAll(
                listOf(
                    "isReady",
                    "listRecordings",
                    "downloadRecording:log_20260327_135948.wav"
                )
            )
        )
    }

    @Test
    fun `manual sync returns device empty when badge list is empty`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(emptyList())

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.DEVICE_EMPTY, outcome.resultBranch)
        assertEquals(0, outcome.queuedCount)
        verify(orchestrator, never()).notifyDownloadStarting()
    }

    @Test
    fun `manual sync returns already present when listed files already exist locally`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log#20260327_135948"))
        runtime.audioFiles.value = listOf(
            AudioFile(
                id = "badge-1",
                filename = "log_20260327_135948.wav",
                timeDisplay = "Now",
                source = AudioSource.SMARTBADGE,
                status = TranscriptionStatus.TRANSCRIBED
            )
        )

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.ALREADY_PRESENT, outcome.resultBranch)
        assertEquals(0, outcome.queuedCount)
        assertFalse(connectivityBridge.calls.any { it.startsWith("downloadRecording:") })
    }

    @Test
    fun `manual sync creates placeholder immediately then upgrades it after background download`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log#20260327_135948"))
        connectivityBridge.downloadResults["log_20260327_135948.wav"] = WavDownloadResult.Success(
            localFile = tempFolder.newFile("downloaded.wav").apply { writeText("audio") },
            originalFilename = "log_20260327_135948.wav",
            sizeBytes = 2048L
        )

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.QUEUED, outcome.resultBranch)
        assertEquals(1, outcome.queuedCount)
        verify(orchestrator).notifyDownloadStarting()
        assertEquals(
            AudioLocalAvailability.QUEUED,
            runtime.audioFiles.value.single { it.filename == "log_20260327_135948.wav" }.localAvailability
        )

        advanceUntilIdle()

        assertEquals(
            AudioLocalAvailability.READY,
            runtime.audioFiles.value.single { it.filename == "log_20260327_135948.wav" }.localAvailability
        )
    }

    @Test
    fun `manual sync removes placeholder when background download is below 1KB threshold`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log#20260327_140000", "log#20260327_140100"))
        val emptyFile = tempFolder.newFile("empty.wav")
        val normalFile = tempFolder.newFile("normal.wav").apply { writeText("audio-content") }
        connectivityBridge.downloadResults["log_20260327_140000.wav"] = WavDownloadResult.Success(
            localFile = emptyFile,
            originalFilename = "log_20260327_140000.wav",
            sizeBytes = 44L
        )
        connectivityBridge.downloadResults["log_20260327_140100.wav"] = WavDownloadResult.Success(
            localFile = normalFile,
            originalFilename = "log_20260327_140100.wav",
            sizeBytes = 2048L
        )

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.QUEUED, outcome.resultBranch)
        assertEquals(2, outcome.queuedCount)

        advanceUntilIdle()

        assertTrue(runtime.audioFiles.value.any { it.filename == "log_20260327_140100.wav" })
        assertFalse(runtime.audioFiles.value.any { it.filename == "log_20260327_140000.wav" })
    }

    @Test
    fun `manual sync treats pipeline ingested recording as already present`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log#20260331_101500"))
        val tempAudio = tempFolder.newFile("badge_temp.wav").apply {
            writeText("audio-bytes")
        }
        SimBadgeAudioPipelineIngestSupport(runtime).ingestCompletedRecording(
            filename = "log_20260331_101500.wav",
            localFile = tempAudio,
            transcript = "客户确认下周二复盘。"
        )

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.ALREADY_PRESENT, outcome.resultBranch)
        assertEquals(0, outcome.queuedCount)
        assertFalse(connectivityBridge.calls.any { it.startsWith("downloadRecording:") })
    }

    @Test
    fun `manual sync suppresses tombstoned badge filenames instead of reimporting them`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log_20260327_135948.wav"))
        connectivityBridge.deleteRecordingResults["log_20260327_135948.wav"] = false
        runtime.pendingBadgeDeletes.value = setOf("log_20260327_135948.wav")

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.ALREADY_PRESENT, outcome.resultBranch)
        assertEquals(0, outcome.queuedCount)
        assertEquals(setOf("log_20260327_135948.wav"), storeSupport.getPendingBadgeDeletesSnapshot())
        assertTrue(connectivityBridge.calls.contains("deleteRecording:log_20260327_135948.wav"))
        assertFalse(connectivityBridge.calls.any { it.startsWith("downloadRecording:") })
    }

    @Test
    fun `manual sync clears tombstone when badge delete retry succeeds`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log_20260327_135948.wav"))
        connectivityBridge.deleteRecordingResults["log_20260327_135948.wav"] = true
        runtime.pendingBadgeDeletes.value = setOf("log_20260327_135948.wav")

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.ALREADY_PRESENT, outcome.resultBranch)
        assertEquals(0, outcome.queuedCount)
        assertTrue(storeSupport.getPendingBadgeDeletesSnapshot().isEmpty())
        assertTrue(connectivityBridge.calls.contains("deleteRecording:log_20260327_135948.wav"))
        assertFalse(connectivityBridge.calls.any { it.startsWith("downloadRecording:") })
    }

    @Test
    fun `manual sync keeps failed placeholder visible and allows later files to continue`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("a.wav", "b.wav"))
        connectivityBridge.downloadResults["a.wav"] = WavDownloadResult.Error(
            code = WavDownloadResult.ErrorCode.DOWNLOAD_FAILED,
            message = "timeout"
        )
        connectivityBridge.downloadResults["b.wav"] = WavDownloadResult.Success(
            localFile = tempFolder.newFile("b.wav").apply { writeText("audio-content") },
            originalFilename = "b.wav",
            sizeBytes = 2048L
        )

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.QUEUED, outcome.resultBranch)
        advanceUntilIdle()

        assertEquals(
            AudioLocalAvailability.FAILED,
            runtime.audioFiles.value.single { it.filename == "a.wav" }.localAvailability
        )
        assertEquals(
            AudioLocalAvailability.READY,
            runtime.audioFiles.value.single { it.filename == "b.wav" }.localAvailability
        )
    }

    @Test
    fun `selectNewSimBadgeFilenames excludes pending badge deletes`() {
        assertEquals(
            listOf("b.wav"),
            selectNewSimBadgeFilenames(
                badgeFilenames = listOf("a.wav", "b.wav"),
                existingBadgeFilenames = emptySet(),
                pendingBadgeDeleteFilenames = setOf("a.wav")
            )
        )
    }

    @Test
    fun `canceled download transitions entry from DOWNLOADING to FAILED`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log_20260401_090000.wav"))
        connectivityBridge.downloadSuspender = { kotlinx.coroutines.awaitCancellation() }

        syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        advanceUntilIdle()

        assertEquals(
            AudioLocalAvailability.DOWNLOADING,
            runtime.audioFiles.value.single { it.filename == "log_20260401_090000.wav" }.localAvailability
        )

        syncSupport.cancelBadgeDownload("log_20260401_090000.wav")
        advanceUntilIdle()

        val entry = runtime.audioFiles.value.single { it.filename == "log_20260401_090000.wav" }
        assertEquals(AudioLocalAvailability.FAILED, entry.localAvailability)
        assertEquals("下载被中断，请重试同步", entry.lastErrorMessage)
    }

    @Test
    fun `canceled download allows remaining queued downloads to proceed`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("a.wav", "b.wav"))
        connectivityBridge.downloadSuspender = { kotlinx.coroutines.awaitCancellation() }
        connectivityBridge.downloadResults["b.wav"] = WavDownloadResult.Success(
            localFile = tempFolder.newFile("b.wav").apply { writeText("audio-content") },
            originalFilename = "b.wav",
            sizeBytes = 2048L
        )

        syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        advanceUntilIdle()

        syncSupport.cancelBadgeDownload("a.wav")
        connectivityBridge.downloadSuspender = null
        advanceUntilIdle()

        assertEquals(
            AudioLocalAvailability.FAILED,
            runtime.audioFiles.value.single { it.filename == "a.wav" }.localAvailability
        )
        assertEquals(
            AudioLocalAvailability.READY,
            runtime.audioFiles.value.single { it.filename == "b.wav" }.localAvailability
        )
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

        var isReadyResult: Boolean = false
        var listResult: Result<List<String>> = Result.Success(emptyList())
        var downloadResults: MutableMap<String, WavDownloadResult> = mutableMapOf()
        var deleteRecordingResults: MutableMap<String, Boolean> = mutableMapOf()
        var downloadSuspender: (suspend () -> Unit)? = null
        val calls = mutableListOf<String>()

        override suspend fun downloadRecording(
            filename: String,
            onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
        ): WavDownloadResult {
            calls += "downloadRecording:$filename"
            downloadSuspender?.invoke()
            return downloadResults[filename]
                ?: WavDownloadResult.Error(
                    code = WavDownloadResult.ErrorCode.DOWNLOAD_FAILED,
                    message = "missing stub"
                )
        }

        override suspend fun listRecordings(): Result<List<String>> {
            calls += "listRecordings"
            return listResult
        }

        override fun recordingNotifications(): Flow<RecordingNotification> = emptyFlow()

        override fun audioRecordingNotifications(): Flow<RecordingNotification.AudioRecordingReady> = emptyFlow()

        override suspend fun isReady(): Boolean {
            calls += "isReady"
            return isReadyResult
        }

        override suspend fun deleteRecording(filename: String): Boolean {
            calls += "deleteRecording:$filename"
            return deleteRecordingResults[filename] ?: true
        }
    }
}
