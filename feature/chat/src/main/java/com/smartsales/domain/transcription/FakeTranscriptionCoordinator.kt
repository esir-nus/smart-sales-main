package com.smartsales.domain.transcription

import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.chat.home.transcription.ProcessedBatch
import com.smartsales.feature.chat.home.transcription.TranscriptionUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Fake implementation for testing. Supports stubbing and call tracking.
 */
class FakeTranscriptionCoordinator : TranscriptionCoordinator {
    private val _state = MutableStateFlow(TranscriptionUiState())
    override val state: StateFlow<TranscriptionUiState> = _state

    var stubProcessedChunk: String = "fake chunk"
    var stubMergeResult: String = "merged"
    
    val observeJobCalls = mutableListOf<String>()
    val observeBatchCalls = mutableListOf<String>()
    val processChunkCalls = mutableListOf<AudioTranscriptionBatchEvent.BatchReleased>()
    var resetCalls = 0
    var markFinalCalls = 0
    val setMessageIdCalls = mutableListOf<String?>()
    val startTranscriptionCalls = mutableListOf<String>()

    override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> {
        observeJobCalls.add(jobId)
        return emptyFlow()
    }

    override fun observeProcessedBatches(jobId: String): Flow<ProcessedBatch> {
        observeBatchCalls.add(jobId)
        return emptyFlow()
    }

    override fun processChunk(event: AudioTranscriptionBatchEvent.BatchReleased): String {
        processChunkCalls.add(event)
        return stubProcessedChunk
    }

    override fun reset() {
        resetCalls++
    }

    override fun markFinal() {
        markFinalCalls++
    }

    override fun setCurrentMessageId(messageId: String?) {
        setMessageIdCalls.add(messageId)
    }

    override fun startTranscription(jobId: String) {
        startTranscriptionCalls.add(jobId)
    }

    override fun mergeChunks(existing: String, incoming: String): String {
        return stubMergeResult
    }

    override suspend fun runTranscription(
        jobId: String,
        fileName: String,
        progressMessageId: String,
        onProgressUpdate: (percent: Int, messageId: String) -> Unit,
        onBatchReceived: (ProcessedBatch) -> Unit,
        onCompleted: (transcriptMarkdown: String, messageId: String) -> Unit,
        onFailed: (reason: String, messageId: String) -> Unit
    ) {
        // No-op in fake
    }

    fun resetTracking() {
        observeJobCalls.clear()
        observeBatchCalls.clear()
        processChunkCalls.clear()
        resetCalls = 0
        markFinalCalls = 0
        setMessageIdCalls.clear()
        startTranscriptionCalls.clear()
    }
}
