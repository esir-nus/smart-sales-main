// File: feature/chat/src/main/java/com/smartsales/domain/transcription/TranscriptionCoordinatorImpl.kt
// Module: :feature:chat
// Summary: Transcription task coordination implementation - batch gate and window filtering
// Author: created on 2026-01-07

package com.smartsales.domain.transcription

import android.util.Log
import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.domain.transcription.V1BatchIndexPrefixGate
import com.smartsales.domain.transcription.V1TingwuWindowedChunkBuilder
import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.feature.chat.home.transcription.ProcessedBatch
import com.smartsales.feature.chat.home.transcription.TranscriptionUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Transcription Coordinator implementation.
 */
@Singleton
class TranscriptionCoordinatorImpl @Inject constructor(
    private val transcriptionCoordinator: AudioTranscriptionCoordinator,
    private val tingwuTraceStore: TingwuTraceStore,
    optionalConfig: Optional<AiCoreConfig>
) : TranscriptionCoordinator {

    private val _state = MutableStateFlow(TranscriptionUiState())
    override val state: StateFlow<TranscriptionUiState> = _state.asStateFlow()

    // Batch gate: ensure batches released in order
    private val batchGate = V1BatchIndexPrefixGate<AudioTranscriptionBatchEvent.BatchReleased>()

    // Window filter switch
    private val enableV1TingwuMacroWindowFilter =
        optionalConfig.orElse(AiCoreConfig()).enableV1TingwuMacroWindowFilter

    companion object {
        private const val TAG = "TranscriptionCoordinatorImpl"
    }

    override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> {
        return transcriptionCoordinator.observeJob(jobId)
    }

    override fun observeProcessedBatches(jobId: String): Flow<ProcessedBatch> = flow {
        Log.d(TAG, "observeProcessedBatches: starting collection for jobId=$jobId")
        transcriptionCoordinator.observeBatches(jobId).collect { event ->
            Log.d(TAG, "observeProcessedBatches: received event type=${event::class.simpleName}")
            when (event) {
                is AudioTranscriptionBatchEvent.BatchReleased -> {
                    Log.d(TAG, "observeProcessedBatches: BatchReleased index=${event.batchIndex}/${event.totalBatches} chunk长度=${event.markdownChunk.length}")
                    // Check if already marked final
                    if (_state.value.isFinal) {
                        // Batch received after final - skip
                        Log.d(TAG, "observeProcessedBatches: skipping - already marked final")
                        return@collect
                    }

                    // Record Tingwu trace data
                    tingwuTraceStore.record(
                        batchPlanRule = event.ruleLabel,
                        batchPlanBatchSize = event.batchSize,
                        batchPlanTotalBatches = event.totalBatches,
                        batchPlanCurrentBatchIndex = event.batchIndex,
                        v1BatchPlanRule = event.v1BatchPlanRule,
                        v1BatchDurationMs = event.v1BatchDurationMs,
                        v1OverlapMs = event.v1OverlapMs,
                        v1BatchPlanTotalBatches = event.v1TotalBatches,
                        v1BatchPlanCurrentBatchIndex = event.v1CurrentBatchIndex
                    )

                    // Release batches through gate
                    val releasables = batchGate.offer(event.batchIndex, event)

                    // Process all releasable batches
                    for (released in releasables) {
                        val effectiveChunk = processChunk(released)
                        val isFinal = released.isFinal

                        // Emit processed batch
                        emit(
                            ProcessedBatch(
                                batchIndex = released.batchIndex,
                                effectiveChunk = effectiveChunk,
                                isFinal = isFinal,
                                event = released
                            )
                        )

                        // Mark final if this is the final batch
                        if (isFinal) {
                            _state.update { it.copy(isFinal = true) }
                        }
                    }
                }
            }
        }
    }

    override fun processChunk(event: AudioTranscriptionBatchEvent.BatchReleased): String {
        val window = event.v1Window
        val timedSegments = event.timedSegments

        // Apply macro window filtering (if enabled)
        return if (enableV1TingwuMacroWindowFilter && window != null && timedSegments != null) {
            val result = V1TingwuWindowedChunkBuilder.buildWindowedMarkdownChunkResult(
                absStartMs = window.absStartMs,
                absEndMs = window.absEndMs,
                timedSegments = timedSegments
            )

            // V1 window filter applied
            result.chunk
        } else {
            // Fallback to original markdown
            event.markdownChunk
        }
    }

    override fun reset() {
        batchGate.reset()
        _state.update {
            TranscriptionUiState()
        }
    }

    override fun markFinal() {
        _state.update { it.copy(isFinal = true) }
    }

    override fun setCurrentMessageId(messageId: String?) {
        _state.update { it.copy(currentMessageId = messageId) }
    }

    override fun startTranscription(jobId: String) {
        _state.update {
            it.copy(
                jobId = jobId,
                isActive = true,
                isFinal = false
            )
        }
    }

    override fun mergeChunks(existing: String, incoming: String): String {
        if (existing.isBlank()) return incoming
        if (incoming.isBlank()) return existing
        // Streaming append only: avoid duplication or out-of-order
        val separator = when {
            existing.endsWith("\n") || incoming.startsWith("\n") -> ""
            else -> "\n"
        }
        return existing + separator + incoming
    }

    override suspend fun runTranscription(
        jobId: String,
        fileName: String,
        progressMessageId: String,
        onProgressUpdate: (percent: Int, messageId: String) -> Unit,
        onBatchReceived: (ProcessedBatch) -> Unit,
        onCompleted: (messageId: String) -> Unit,
        onFailed: (reason: String, messageId: String) -> Unit
    ) {
        Log.d(TAG, "runTranscription: starting for jobId=$jobId fileName=$fileName")
        // Observe job state until terminal state (Completed/Failed)
        val terminalState = observeJob(jobId)
            .onEach { state ->
                Log.d(TAG, "runTranscription: observeJob emitted state=${state::class.simpleName}")
                when (state) {
                    is AudioTranscriptionJobState.InProgress -> {
                        onProgressUpdate(state.progressPercent, progressMessageId)
                    }
                    else -> Unit
                }
            }
            .first { it is AudioTranscriptionJobState.Completed || it is AudioTranscriptionJobState.Failed }

        Log.d(TAG, "runTranscription: terminal state reached = ${terminalState::class.simpleName}")
        // Now process all batches (they're available after Completed)
        when (terminalState) {
            is AudioTranscriptionJobState.Completed -> {
                Log.d(TAG, "runTranscription: Completed state, starting batch collection")
                // Collect all batches first
                observeProcessedBatches(jobId).collect { batch ->
                    Log.d(TAG, "runTranscription: batch received index=${batch.batchIndex} chunk长度=${batch.effectiveChunk.length}")
                    onBatchReceived(batch)
                }
                Log.d(TAG, "runTranscription: batch collection complete, calling onCompleted")
                // Then signal completion
                onCompleted(progressMessageId)
            }
            is AudioTranscriptionJobState.Failed -> {
                onFailed(terminalState.reason.ifBlank { "转写失败" }, progressMessageId)
            }
            else -> Unit // Idle - shouldn't happen as terminal
        }
    }
}
