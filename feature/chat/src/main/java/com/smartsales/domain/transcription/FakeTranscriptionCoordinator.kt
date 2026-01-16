package com.smartsales.domain.transcription


import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
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

    val observeJobCalls = mutableListOf<String>()
    var resetCalls = 0
    var markFinalCalls = 0
    val setMessageIdCalls = mutableListOf<String?>()
    val startTranscriptionCalls = mutableListOf<String>()

    override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> {
        observeJobCalls.add(jobId)
        return emptyFlow()
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



    override suspend fun runTranscription(
        jobId: String,
        fileName: String,
        progressMessageId: String,
        onProgressUpdate: (percent: Int, messageId: String) -> Unit,
        onCompleted: (transcriptMarkdown: String, messageId: String) -> Unit,
        onFailed: (reason: String, messageId: String) -> Unit
    ) {
        // No-op in fake
    }

    fun resetTracking() {
        observeJobCalls.clear()
        resetCalls = 0
        markFinalCalls = 0
        setMessageIdCalls.clear()
        startTranscriptionCalls.clear()
    }
}
