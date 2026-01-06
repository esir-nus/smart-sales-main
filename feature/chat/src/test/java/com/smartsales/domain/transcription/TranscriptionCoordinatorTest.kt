package com.smartsales.domain.transcription

// File: feature/chat/src/test/java/com/smartsales/domain/transcription/TranscriptionCoordinatorTest.kt
// Module: :feature:chat
// Summary: Unit tests for TranscriptionCoordinator - batch gate, window filtering, job state
// Author: created on 2026-01-06
// Status: 5/10 passing. TODO: Fix flow collection tests (UncompletedCoroutinesError with SharedFlow)

import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.feature.chat.home.transcription.ProcessedBatch
import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Optional

@OptIn(ExperimentalCoroutinesApi::class)
class TranscriptionCoordinatorTest {

    private val fakeCoordinator = FakeAudioTranscriptionCoordinator()
    private val traceStore = TingwuTraceStore()
    private val coordinator = TranscriptionCoordinator(
        transcriptionCoordinator = fakeCoordinator,
        tingwuTraceStore = traceStore,
        optionalConfig = Optional.of(AiCoreConfig(enableV1TingwuMacroWindowFilter = false))
    )

    @Test
    fun observeJob_returnsFlowFromUnderlyingCoordinator() = runTest {
        // Given: Job state flow emits InProgress
        val jobState = AudioTranscriptionJobState.InProgress("job1", 50)
        fakeCoordinator.jobStateFlow.emit(jobState)

        // When: Observe job
        val result = coordinator.observeJob("job1").first()

        // Then: Should get the job state
        assertEquals(jobState, result)
    }

    @Test
    fun observeProcessedBatches_orderedBatches_emitsImmediately() = runTest {
        // Given: Ordered batches
        coordinator.reset()
        
        // When: Emit batches 1, 2, 3 in order
        fakeCoordinator.batchFlow.emit(createBatchEvent(1, "Batch 1"))
        fakeCoordinator.batchFlow.emit(createBatchEvent(2, "Batch 2"))
        fakeCoordinator.batchFlow.emit(createBatchEvent(3, "Batch 3", isFinal = true))

        // Then: Should emit all batches immediately
        val results = coordinator.observeProcessedBatches("job1").take(3).toList()
        assertEquals(3, results.size)
        assertEquals(1, results[0].batchIndex)
        assertEquals(2, results[1].batchIndex)
        assertEquals(3, results[2].batchIndex)
    }

    @Test
    fun observeProcessedBatches_outOfOrder_buffersUntilPrefix() = runTest {
        // Given: Out-of-order batches
        coordinator.reset()

        // When: Emit batch 2 first, then batch 1
        fakeCoordinator.batchFlow.emit(createBatchEvent(2, "Batch 2"))
        fakeCoordinator.batchFlow.emit(createBatchEvent(1, "Batch 1"))
        fakeCoordinator.batchFlow.emit(createBatchEvent(3, "Batch 3", isFinal = true))

        // Then: Should buffer batch 2 until batch 1 arrives, then emit both
        val results = coordinator.observeProcessedBatches("job1").take(3).toList()
        assertEquals(3, results.size)
        assertEquals(1, results[0].batchIndex)
        assertEquals(2, results[1].batchIndex)
        assertEquals(3, results[2].batchIndex)
    }

    @Test
    fun observeProcessedBatches_afterFinal_ignoresBatches() = runTest {
        // Given: Final batch emitted
        coordinator.reset()

        // When: Emit final batch, then another batch
        fakeCoordinator.batchFlow.emit(createBatchEvent(1, "Batch 1", isFinal = true))
        
        // Mark as final
        val firstBatch = coordinator.observeProcessedBatches("job1").first()
        assertEquals(true, firstBatch.isFinal)

        fakeCoordinator.batchFlow.emit(createBatchEvent(2, "Batch 2 after final"))

        // Then: Second batch should be ignored (flow completes after first)
        val results = coordinator.observeProcessedBatches("job1").take(1).toList()
        assertEquals(1, results.size)
    }

    @Test
    fun processChunk_windowFilterDisabled_fallbackToMarkdown() = runTest {
        // Given: Window filtering disabled
        val event = createBatchEvent(1, "Original markdown")

        // When: Process chunk
        val result = coordinator.processChunk(event)

        // Then: Should return original markdown
        assertEquals("Original markdown", result)
    }

    @Test
    fun processChunk_windowFilterEnabled_appliesFilter() = runTest {
        // Given: Coordinator with window filtering enabled
        val coordinatorWithFilter = TranscriptionCoordinator(
            transcriptionCoordinator = fakeCoordinator,
            tingwuTraceStore = traceStore,
            optionalConfig = Optional.of(AiCoreConfig(enableV1TingwuMacroWindowFilter = true))
        )

        val event = createBatchEvent(1, "Original markdown").copy(
            v1Window = com.smartsales.feature.media.audiofiles.V1TranscriptionBatchWindow(
                batchIndex = 1,
                absStartMs = 0L,
                absEndMs = 10000L,
                overlapMs = 0L,
                captureStartMs = 0L
            ),
            timedSegments = listOf(
                com.smartsales.feature.media.audiofiles.V1TimedTextSegment(0L, 5000L, "Segment 1"),
                com.smartsales.feature.media.audiofiles.V1TimedTextSegment(5000L, 10000L, "Segment 2")
            )
        )

        // When: Process chunk
        val result = coordinatorWithFilter.processChunk(event)

        // Then: Should apply window filtering (V1TingwuWindowedChunkBuilder)
        // Note: Without actual filtering logic, it returns filtered markdown
        assertEquals("Segment 1\nSegment 2", result)
    }

    @Test
    fun reset_clearsGateAndState() = runTest {
        // Given: State has been modified
        coordinator.startTranscription("job1")
        
        // When: Reset
        coordinator.reset()

        // Then: State should be cleared
        val state = coordinator.state.first()
        assertEquals("", state.jobId)
        assertEquals(false, state.isActive)
        assertEquals(false, state.isFinal)
    }

    @Test
    fun markFinal_setsIsFinalTrue() = runTest {
        // When: Mark final
        coordinator.markFinal()

        // Then: State should have isFinal = true
        val state = coordinator.state.first()
        assertEquals(true, state.isFinal)
    }

    @Test
    fun setCurrentMessageId_updatesState() = runTest {
        // When: Set current message ID
        coordinator.setCurrentMessageId("msg-123")

        // Then: State should update
        val state = coordinator.state.first()
        assertEquals("msg-123", state.currentMessageId)
    }

    @Test
    fun startTranscription_updatesState() = runTest {
        // When: Start transcription
        coordinator.startTranscription("job-abc")

        // Then: State should update
        val state = coordinator.state.first()
        assertEquals("job-abc", state.jobId)
        assertEquals(true, state.isActive)
        assertEquals(false, state.isFinal)
    }

    // ===== Helpers =====

    private fun createBatchEvent(
        batchIndex: Int,
        markdown: String,
        isFinal: Boolean = false
    ) = AudioTranscriptionBatchEvent.BatchReleased(
        jobId = "job1",
        batchIndex = batchIndex,
        totalBatches = 3,
        markdownChunk = markdown,
        isFinal = isFinal,
        batchSize = 100,
        lineCount = 10,
        ruleLabel = "test-rule"
    )

    // ===== Fake Implementations =====

    private class FakeAudioTranscriptionCoordinator : AudioTranscriptionCoordinator {
        val batchFlow = MutableSharedFlow<AudioTranscriptionBatchEvent>()
        val jobStateFlow = MutableSharedFlow<AudioTranscriptionJobState>()

        override suspend fun uploadAudio(file: java.io.File) =
            com.smartsales.core.util.Result.Success(
                com.smartsales.feature.media.audiofiles.AudioUploadPayload("key", "url")
            )

        override suspend fun submitTranscription(
            audioAssetName: String,
            language: String,
            uploadPayload: com.smartsales.feature.media.audiofiles.AudioUploadPayload,
            sessionId: String?
        ) = com.smartsales.core.util.Result.Success("job-id")

        override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> = jobStateFlow

        override fun observeBatches(jobId: String): Flow<AudioTranscriptionBatchEvent> = batchFlow
    }
}
