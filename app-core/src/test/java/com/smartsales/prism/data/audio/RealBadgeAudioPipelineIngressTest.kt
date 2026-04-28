package com.smartsales.prism.data.audio

import com.smartsales.prism.domain.asr.AsrService
import com.smartsales.prism.domain.audio.PipelineEvent
import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.audio.SchedulerResult
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.RecordingNotification
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import com.smartsales.core.pipeline.IntentOrchestrator
import com.smartsales.core.pipeline.PipelineResult
import com.smartsales.core.pipeline.SchedulerCommitKind
import com.smartsales.core.util.Result
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RealBadgeAudioPipelineIngressTest {

    @Test
    fun `bridge notification triggers processing through init collector`() = runTest {
        val bridge = FakeConnectivityBridge()
        val pipeline = RealBadgeAudioPipeline(
            connectivityBridge = bridge,
            asrService = mock<AsrService>(),
            intentOrchestrator = mock<IntentOrchestrator>(),
            simBadgeAudioPipelineIngestSupport = mock<SimBadgeAudioPipelineIngestSupport>()
        )

        bridge.emit(RecordingNotification.RecordingReady("log_20260322_170000.wav"))
        waitUntil { bridge.downloadCalls.isNotEmpty() }

        assertEquals(listOf("log_20260322_170000.wav"), bridge.downloadCalls)
        // Pipeline should emit Downloading then Error (bridge returns failure)
        waitUntil { pipeline.events.replayCache.any { it is PipelineEvent.Error } }
        assertTrue(pipeline.events.replayCache.any { it is PipelineEvent.Downloading })
        assertTrue(pipeline.events.replayCache.any { it is PipelineEvent.Error })
    }

    @Test
    fun `processFile emits command end once on success`() = runTest {
        val bridge = FakeConnectivityBridge().apply {
            downloadResult = WavDownloadResult.Success(
                localFile = createTempAudioFile(),
                originalFilename = "log_20260322_170001.wav",
                sizeBytes = 2048L
            )
        }
        val asrService = mock<AsrService>()
        whenever(asrService.transcribe(org.mockito.kotlin.any())).thenReturn(
            AsrResult.Success("明天下午三点开会")
        )
        val orchestrator = mock<IntentOrchestrator>()
        whenever(orchestrator.processInput("明天下午三点开会", true)).thenReturn(
            flowOf(PipelineResult.InspirationCommitted("insp-1", "明天下午三点开会"))
        )
        val ingestSupport = mock<SimBadgeAudioPipelineIngestSupport>()
        whenever(
            ingestSupport.ingestCompletedRecording(
                org.mockito.kotlin.eq("log_20260322_170001.wav"),
                org.mockito.kotlin.any(),
                org.mockito.kotlin.eq("明天下午三点开会")
            )
        ).thenReturn(true)
        val pipeline = RealBadgeAudioPipeline(bridge, asrService, orchestrator, ingestSupport)

        pipeline.processFile("log_20260322_170001.wav")
        waitUntil { bridge.notifyCommandEndCalls == 1 }

        assertEquals(1, bridge.notifyCommandEndCalls)
        assertTrue(
            pipeline.events.replayCache.any {
                it is PipelineEvent.Complete && it.result == SchedulerResult.InspirationSaved("insp-1")
            }
        )
    }

    @Test
    fun `processFile maps reschedule commit to task rescheduled result`() = runTest {
        val bridge = FakeConnectivityBridge().apply {
            downloadResult = WavDownloadResult.Success(
                localFile = createTempAudioFile(),
                originalFilename = "log_20260322_170010.wav",
                sizeBytes = 2048L
            )
        }
        val asrService = mock<AsrService>()
        whenever(asrService.transcribe(org.mockito.kotlin.any())).thenReturn(
            AsrResult.Success("改成9点赶飞机")
        )
        val orchestrator = mock<IntentOrchestrator>()
        whenever(orchestrator.processInput("改成9点赶飞机", true)).thenReturn(
            flowOf(
                PipelineResult.PathACommitted(
                    task = com.smartsales.prism.domain.scheduler.ScheduledTask(
                        id = "task-9",
                        timeDisplay = "09:00",
                        title = "赶飞机",
                        startTime = java.time.Instant.parse("2026-03-22T01:00:00Z"),
                        durationMinutes = 30
                    ),
                    commitKind = SchedulerCommitKind.RESCHEDULE
                )
            )
        )
        val ingestSupport = mock<SimBadgeAudioPipelineIngestSupport>()
        whenever(
            ingestSupport.ingestCompletedRecording(
                org.mockito.kotlin.eq("log_20260322_170010.wav"),
                org.mockito.kotlin.any(),
                org.mockito.kotlin.eq("改成9点赶飞机")
            )
        ).thenReturn(true)
        val pipeline = RealBadgeAudioPipeline(bridge, asrService, orchestrator, ingestSupport)

        pipeline.processFile("log_20260322_170010.wav")
        waitUntil { pipeline.events.replayCache.any { it is PipelineEvent.Complete } }

        val complete = pipeline.events.replayCache.filterIsInstance<PipelineEvent.Complete>().last()
        assertTrue(complete.result is SchedulerResult.TaskRescheduled)
        assertEquals("赶飞机", (complete.result as SchedulerResult.TaskRescheduled).title)
    }

    @Test
    fun `processFile emits command end once on transcribe error`() = runTest {
        val bridge = FakeConnectivityBridge().apply {
            downloadResult = WavDownloadResult.Success(
                localFile = createTempAudioFile(),
                originalFilename = "log_20260322_170002.wav",
                sizeBytes = 2048L
            )
        }
        val asrService = mock<AsrService>()
        whenever(asrService.transcribe(org.mockito.kotlin.any())).thenReturn(
            AsrResult.Error(AsrResult.ErrorCode.API_ERROR, "forced asr failure")
        )
        val pipeline = RealBadgeAudioPipeline(
            connectivityBridge = bridge,
            asrService = asrService,
            intentOrchestrator = mock<IntentOrchestrator>(),
            simBadgeAudioPipelineIngestSupport = mock<SimBadgeAudioPipelineIngestSupport>()
        )

        pipeline.processFile("log_20260322_170002.wav")
        waitUntil { bridge.notifyCommandEndCalls == 1 }

        assertEquals(1, bridge.notifyCommandEndCalls)
        assertTrue(
            pipeline.events.replayCache.any {
                it is PipelineEvent.Error && it.stage == PipelineEvent.Stage.TRANSCRIBE
            }
        )
    }

    @Test
    fun `processFile emits command end once on catch all exception`() = runTest {
        val bridge = FakeConnectivityBridge().apply {
            downloadResult = WavDownloadResult.Success(
                localFile = createTempAudioFile(),
                originalFilename = "log_20260322_170003.wav",
                sizeBytes = 2048L
            )
        }
        val asrService = mock<AsrService>()
        whenever(asrService.transcribe(org.mockito.kotlin.any())).thenThrow(
            IllegalStateException("forced exception")
        )
        val pipeline = RealBadgeAudioPipeline(
            connectivityBridge = bridge,
            asrService = asrService,
            intentOrchestrator = mock<IntentOrchestrator>(),
            simBadgeAudioPipelineIngestSupport = mock<SimBadgeAudioPipelineIngestSupport>()
        )

        pipeline.processFile("log_20260322_170003.wav")
        waitUntil { bridge.notifyCommandEndCalls == 1 }

        assertEquals(1, bridge.notifyCommandEndCalls)
        assertTrue(
            pipeline.events.replayCache.any {
                it is PipelineEvent.Error && it.stage == PipelineEvent.Stage.TRANSCRIBE
            }
        )
    }

    private fun waitUntil(timeoutMs: Long = 2_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(25)
        }
        throw AssertionError("Timed out waiting for condition")
    }

    private class FakeConnectivityBridge : ConnectivityBridge {
        private val notifications = MutableSharedFlow<RecordingNotification>(
            replay = 1,
            extraBufferCapacity = 4
        )
        private val _connectionState = MutableStateFlow<BadgeConnectionState>(
            BadgeConnectionState.Connected("192.168.0.9", "MstRobot")
        )

        val downloadCalls = mutableListOf<String>()
        var downloadResult: WavDownloadResult = WavDownloadResult.Error(
            code = WavDownloadResult.ErrorCode.DOWNLOAD_FAILED,
            message = "forced failure"
        )
        var notifyCommandEndCalls = 0

        override val connectionState: StateFlow<BadgeConnectionState> = _connectionState.asStateFlow()
        override val managerStatus: StateFlow<BadgeManagerStatus> =
            MutableStateFlow<BadgeManagerStatus>(BadgeManagerStatus.Ready("192.168.0.9", "MstRobot")).asStateFlow()

        override suspend fun downloadRecording(
            filename: String,
            onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
        ): WavDownloadResult {
            downloadCalls += filename
            return downloadResult
        }

        override suspend fun listRecordings(): Result<List<String>> = Result.Success(emptyList())

        override fun recordingNotifications(): Flow<RecordingNotification> = notifications

        override fun audioRecordingNotifications(): Flow<RecordingNotification.AudioRecordingReady> = emptyFlow()

        override fun batteryNotifications(): Flow<Int> = emptyFlow()

        override fun firmwareVersionNotifications(): Flow<String> = emptyFlow()

        override fun sdCardSpaceNotifications(): Flow<String> = emptyFlow()

        override suspend fun isReady(): Boolean = true

        override suspend fun deleteRecording(filename: String): Boolean = true

        override suspend fun requestFirmwareVersion(): Boolean = false

        override suspend fun requestSdCardSpace(): Boolean = false

        override suspend fun notifyCommandEnd() {
            notifyCommandEndCalls += 1
        }

        override fun wifiRepairEvents(): Flow<com.smartsales.prism.domain.connectivity.WifiRepairEvent> =
            emptyFlow()

        suspend fun emit(notification: RecordingNotification) {
            notifications.emit(notification)
        }
    }

    private fun createTempAudioFile(): File {
        return File.createTempFile("badge-audio", ".wav").apply {
            writeText("audio-content")
            deleteOnExit()
        }
    }
}
