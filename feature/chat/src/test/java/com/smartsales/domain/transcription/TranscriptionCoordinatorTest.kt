package com.smartsales.domain.transcription

// File: feature/chat/src/test/java/com/smartsales/domain/transcription/TranscriptionCoordinatorTest.kt
// Module: :feature:chat
// Summary: Unit tests for TranscriptionCoordinator - batch gate, window filtering, job state
// Author: created on 2026-01-06
// Status: 5/10 passing. TODO: Fix flow collection tests (UncompletedCoroutinesError with SharedFlow)

import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Optional


class TranscriptionCoordinatorTest {

    private val fakeCoordinator = FakeAudioTranscriptionCoordinator()
    private val traceStore = TingwuTraceStore()
    private val coordinator = TranscriptionCoordinatorImpl(
        transcriptionCoordinator = fakeCoordinator,
        tingwuTraceStore = traceStore,
        optionalConfig = Optional.of(AiCoreConfig(enableV1TingwuMacroWindowFilter = false))
    )

    @Test
    fun observeJob_returnsFlowFromUnderlyingCoordinator() = runTest(UnconfinedTestDispatcher()) {
        // Given/When: Start collecting then emit
        val deferred = async {
            coordinator.observeJob("job1").first()
        }

        // Emit after collector is waiting
        val jobState = AudioTranscriptionJobState.InProgress("job1", 50)
        fakeCoordinator.jobStateFlow.emit(jobState)

        // Then: Should get the job state
        assertEquals(jobState, deferred.await())
    }



    @Test
    fun reset_clearsGateAndState() = runTest {
        // Given: State has been modified
        coordinator.startTranscription("job1")
        
        // When: Reset
        coordinator.reset()

        // Then: State should be cleared (default TranscriptionUiState values)
        val state = coordinator.state.first()
        assertEquals(null, state.jobId)  // TranscriptionUiState default is null
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



    // ===== Fake Implementations =====

    private class FakeAudioTranscriptionCoordinator : AudioTranscriptionCoordinator {
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


    }
}
