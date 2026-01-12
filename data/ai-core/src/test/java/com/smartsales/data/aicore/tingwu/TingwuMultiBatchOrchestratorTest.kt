package com.smartsales.data.aicore.tingwu

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.data.aicore.OssUploadClient
import com.smartsales.data.aicore.OssUploadRequest
import com.smartsales.data.aicore.OssUploadResult
import com.smartsales.data.aicore.TingwuJobArtifacts
import com.smartsales.data.aicore.TingwuJobState
import com.smartsales.data.aicore.TingwuRequest
import com.smartsales.data.aicore.disector.DisectorBatch
import com.smartsales.data.aicore.disector.DisectorPlan
import com.smartsales.data.aicore.util.AudioSlicer
import com.smartsales.data.aicore.util.SliceError
import com.smartsales.data.aicore.util.SliceOutcome
import com.smartsales.data.aicore.util.SliceResult
import com.smartsales.data.aicore.debug.PipelineTracer
import com.smartsales.data.aicore.debug.PipelineStage
import com.smartsales.data.aicore.debug.PipelineEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [TingwuMultiBatchOrchestrator].
 * 
 * Uses fake implementations for AudioSlicer and OssUploadClient to test
 * the orchestration logic without real audio processing or network calls.
 */
class TingwuMultiBatchOrchestratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var tempDir: File
    private lateinit var sourceFile: File
    private lateinit var fakeAudioSlicer: FakeAudioSlicer
    private lateinit var fakeOssClient: FakeOssUploadClient
    private lateinit var fakePipelineTracer: FakePipelineTracer
    private lateinit var stitcher: MultiBatchStitcher
    private lateinit var orchestrator: TingwuMultiBatchOrchestrator

    @Before
    fun setup() {
        tempDir = tempFolder.newFolder("slices")
        sourceFile = tempFolder.newFile("source.m4a").apply { writeBytes(ByteArray(1024)) }
        fakeAudioSlicer = FakeAudioSlicer(tempDir)
        fakeOssClient = FakeOssUploadClient()
        fakePipelineTracer = FakePipelineTracer()
        stitcher = MultiBatchStitcher()
        orchestrator = TingwuMultiBatchOrchestrator(fakeAudioSlicer, fakeOssClient, stitcher, fakePipelineTracer)
    }

    // --- Test Helpers ---

    private fun createPlan(batchCount: Int): DisectorPlan {
        val batches = (0 until batchCount).map { i ->
            val startMs = i * 60_000L
            val endMs = (i + 1) * 60_000L
            DisectorBatch(
                batchAssetId = "batch_$i",
                batchIndex = i,
                absStartMs = startMs,
                absEndMs = endMs,
                captureStartMs = startMs,
                captureEndMs = endMs
            )
        }
        return DisectorPlan(
            disectorPlanId = "plan_123",
            audioAssetId = "audio_123",
            recordingSessionId = "session_123",
            totalMs = batchCount * 60_000L,
            batches = batches
        )
    }

    private fun createRequest() = TingwuRequest(
        audioAssetName = "Test Audio",
        language = "zh-CN"
    )

    // --- Test Cases ---

    @Test
    fun executeBatchLoop_twoBatches_allSucceed() = runBlocking {
        val plan = createPlan(2)
        val request = createRequest()
        val progressValues = mutableListOf<Int>()

        val result = orchestrator.executeBatchLoop(
            plan = plan,
            originalRequest = request,
            sourceFile = sourceFile,
            submitSingleBatch = { Result.Success("job_${it.ossObjectKey}") },
            waitForJob = { jobId -> 
                TingwuJobState.Completed(
                    jobId = jobId,
                    transcriptMarkdown = "Test",
                    artifacts = TingwuJobArtifacts(
                        recordingOriginDiarizedSegments = listOf(
                            DiarizedSegment("spk_1", 1, 10_000, 20_000, "Hello")
                        )
                    )
                )
            },
            onProgress = { progressValues.add(it) }
        )
        
        // Check trace events
        assertEquals(3, fakePipelineTracer.emittedEvents.size) // 2x STARTED + MULTI_BATCH_COMPLETE
        assertTrue(fakePipelineTracer.emittedEvents.any { it.status == "MULTI_BATCH_COMPLETE" })

        assertTrue(result is Result.Success)
        assertEquals("multi_plan_123", (result as Result.Success).data)
        assertEquals(2, fakeAudioSlicer.sliceCalls)
        assertEquals(2, fakeOssClient.uploadCalls)
        assertTrue(progressValues.contains(0))
        assertTrue(progressValues.contains(50))
        assertTrue(progressValues.contains(100))
    }

    @Test
    fun executeBatchLoop_sourceFileNotFound_returnsError() = runBlocking {
        val plan = createPlan(1)
        val nonExistentFile = File(tempDir, "nonexistent.m4a")

        val result = orchestrator.executeBatchLoop(
            plan = plan,
            originalRequest = createRequest(),
            sourceFile = nonExistentFile,
            submitSingleBatch = { Result.Success("job_1") },
            waitForJob = { TingwuJobState.Completed("job_1", "Test") },
            onProgress = {}
        )

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable as AiCoreException
        assertEquals(AiCoreErrorReason.IO, error.reason)
    }

    @Test
    fun executeBatchLoop_sliceFails_returnsError() = runBlocking {
        fakeAudioSlicer.shouldFail = true
        val plan = createPlan(1)

        val result = orchestrator.executeBatchLoop(
            plan = plan,
            originalRequest = createRequest(),
            sourceFile = sourceFile,
            submitSingleBatch = { Result.Success("job_1") },
            waitForJob = { TingwuJobState.Completed("job_1", "Test") },
            onProgress = {}
        )

        assertTrue(result is Result.Error)
    }

    @Test
    fun executeBatchLoop_uploadFails_returnsError() = runBlocking {
        fakeOssClient.shouldFail = true
        val plan = createPlan(1)

        val result = orchestrator.executeBatchLoop(
            plan = plan,
            originalRequest = createRequest(),
            sourceFile = sourceFile,
            submitSingleBatch = { Result.Success("job_1") },
            waitForJob = { TingwuJobState.Completed("job_1", "Test") },
            onProgress = {}
        )

        assertTrue(result is Result.Error)
    }

    @Test
    fun executeBatchLoop_jobFails_returnsError() = runBlocking {
        val plan = createPlan(1)
        val error = AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.REMOTE,
            message = "Job failed"
        )

        val result = orchestrator.executeBatchLoop(
            plan = plan,
            originalRequest = createRequest(),
            sourceFile = sourceFile,
            submitSingleBatch = { Result.Success("job_1") },
            waitForJob = { TingwuJobState.Failed("job_1", error) },
            onProgress = {}
        )

        assertTrue(result is Result.Error)
        assertEquals(error, (result as Result.Error).throwable)
    }

    // --- Fake Implementations ---

    /**
     * Fake AudioSlicer that doesn't use MediaCodec/MediaMuxer.
     * Creates dummy slice files without actual audio processing.
     */
    private class FakeAudioSlicer(private val fakeTempDir: File) : AudioSlicer(fakeTempDir) {
        var sliceCalls = 0
        var shouldFail = false

        override fun sliceAudio(
            source: File,
            requestedCaptureStartMs: Long,
            captureEndMs: Long,
            windowKey: String
        ): SliceOutcome {
            sliceCalls++
            if (shouldFail) {
                return SliceOutcome.Failure(SliceError.IoFailure("Fake failure"))
            }
            val sliceFile = File(fakeTempDir, "$windowKey.m4a").apply { writeBytes(ByteArray(512)) }
            return SliceOutcome.Success(
                SliceResult(
                    sliceFile = sliceFile,
                    requestedCaptureStartMs = requestedCaptureStartMs,
                    actualCaptureStartMs = requestedCaptureStartMs,
                    captureEndMs = captureEndMs,
                    durationMs = captureEndMs - requestedCaptureStartMs,
                    windowKey = windowKey
                )
            )
        }
    }

    private class FakeOssUploadClient : OssUploadClient {
        var uploadCalls = 0
        var shouldFail = false

        override suspend fun uploadAudio(request: OssUploadRequest): Result<OssUploadResult> {
            uploadCalls++
            if (shouldFail) {
                return Result.Error(
                    AiCoreException(
                        source = AiCoreErrorSource.OSS,
                        reason = AiCoreErrorReason.NETWORK,
                        message = "Fake upload failure"
                    )
                )
            }
            return Result.Success(
                OssUploadResult(
                    objectKey = request.objectKey ?: "default_key",
                    presignedUrl = "https://fake.oss.url/${request.objectKey}"
                )
            )
        }
    }

    private class FakePipelineTracer : PipelineTracer {
        val emittedEvents = mutableListOf<PipelineEvent>()
        
        override val enabled: Boolean = true
        override val events: StateFlow<List<PipelineEvent>> = MutableStateFlow(emptyList())

        override fun emit(stage: PipelineStage, status: String, message: String) {
            emittedEvents.add(PipelineEvent(stage, status, message))
        }

        override fun clear() {
            emittedEvents.clear()
        }
    }
}
