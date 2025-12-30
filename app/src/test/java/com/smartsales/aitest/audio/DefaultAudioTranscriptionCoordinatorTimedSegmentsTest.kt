// File: app/src/test/java/com/smartsales/aitest/audio/DefaultAudioTranscriptionCoordinatorTimedSegmentsTest.kt
// Description: BatchReleased timedSegments propagation tests (2025-12-30)
package com.smartsales.aitest.audio

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.data.aicore.OssUploadClient
import com.smartsales.data.aicore.OssUploadRequest
import com.smartsales.data.aicore.OssUploadResult
import com.smartsales.data.aicore.TingwuCoordinator
import com.smartsales.data.aicore.TingwuJobArtifacts
import com.smartsales.data.aicore.TingwuJobState
import com.smartsales.data.aicore.TingwuRequest
import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent
import com.smartsales.feature.media.audiofiles.V1TimedTextSegment
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultAudioTranscriptionCoordinatorTimedSegmentsTest {

    @Test
    fun `batch release carries timed segments when available`() = runTest {
        val segments = listOf(
            DiarizedSegment(speakerId = "1", speakerIndex = 0, startMs = 100, endMs = 200, text = "A"),
            DiarizedSegment(speakerId = "2", speakerIndex = 1, startMs = 300, endMs = 450, text = "B")
        )
        val state = TingwuJobState.Completed(
            jobId = "job-1",
            transcriptMarkdown = "line1",
            artifacts = TingwuJobArtifacts(diarizedSegments = segments)
        )
        val coordinator = DefaultAudioTranscriptionCoordinator(
            tingwuCoordinator = FakeTingwuCoordinator(state),
            ossUploadClient = FakeOssUploadClient()
        )

        val event = coordinator.observeBatches("job-1").first() as AudioTranscriptionBatchEvent.BatchReleased

        assertEquals(
            listOf(
                V1TimedTextSegment(100, 200, "A"),
                V1TimedTextSegment(300, 450, "B")
            ),
            event.timedSegments
        )
    }

    @Test
    fun `batch release keeps timed segments null when missing`() = runTest {
        val state = TingwuJobState.Completed(
            jobId = "job-2",
            transcriptMarkdown = "line1",
            artifacts = null
        )
        val coordinator = DefaultAudioTranscriptionCoordinator(
            tingwuCoordinator = FakeTingwuCoordinator(state),
            ossUploadClient = FakeOssUploadClient()
        )

        val event = coordinator.observeBatches("job-2").first() as AudioTranscriptionBatchEvent.BatchReleased

        assertNull(event.timedSegments)
    }

    private class FakeTingwuCoordinator(
        private val state: TingwuJobState.Completed
    ) : TingwuCoordinator {
        override suspend fun submit(request: TingwuRequest): Result<String> = Result.Success(state.jobId)
        override fun observeJob(jobId: String): Flow<TingwuJobState> = flowOf(state)
    }

    private class FakeOssUploadClient : OssUploadClient {
        override suspend fun uploadAudio(request: OssUploadRequest): Result<OssUploadResult> {
            return Result.Error(IllegalStateException("unused"))
        }
    }
}
