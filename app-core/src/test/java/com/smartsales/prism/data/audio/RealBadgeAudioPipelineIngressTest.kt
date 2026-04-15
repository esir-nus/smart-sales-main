package com.smartsales.prism.data.audio

import com.smartsales.prism.domain.asr.AsrService
import com.smartsales.prism.domain.audio.PipelineEvent
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.RecordingNotification
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import com.smartsales.core.pipeline.IntentOrchestrator
import com.smartsales.core.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class RealBadgeAudioPipelineIngressTest {

    @Test
    fun `bridge notification triggers processFile through init collector`() = runTest {
        val bridge = FakeConnectivityBridge()
        val pipeline = RealBadgeAudioPipeline(
            connectivityBridge = bridge,
            asrService = mock<AsrService>(),
            intentOrchestrator = mock<IntentOrchestrator>(),
            simBadgeAudioPipelineIngestSupport = mock<SimBadgeAudioPipelineIngestSupport>()
        )

        bridge.emit(RecordingNotification.RecordingReady("log_20260322_170000.wav"))
        waitUntil { bridge.downloadCalls.isNotEmpty() && pipeline.events.replayCache.size >= 2 }
        val events = pipeline.events.replayCache

        assertEquals(listOf("log_20260322_170000.wav"), bridge.downloadCalls)
        assertTrue(events.first() is PipelineEvent.Downloading)
        assertTrue(events.last() is PipelineEvent.Error)
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

        override val connectionState: StateFlow<BadgeConnectionState> = _connectionState.asStateFlow()
        override val managerStatus: StateFlow<BadgeManagerStatus> =
            MutableStateFlow<BadgeManagerStatus>(BadgeManagerStatus.Ready("192.168.0.9", "MstRobot")).asStateFlow()

        override suspend fun downloadRecording(
            filename: String,
            onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
        ): WavDownloadResult {
            downloadCalls += filename
            return WavDownloadResult.Error(
                code = WavDownloadResult.ErrorCode.DOWNLOAD_FAILED,
                message = "forced failure"
            )
        }

        override suspend fun listRecordings(): Result<List<String>> = Result.Success(emptyList())

        override fun recordingNotifications(): Flow<RecordingNotification> = notifications

        override suspend fun isReady(): Boolean = true

        override suspend fun deleteRecording(filename: String): Boolean = true

        suspend fun emit(notification: RecordingNotification) {
            notifications.emit(notification)
        }
    }
}
