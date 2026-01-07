// File: feature/chat/src/main/java/com/smartsales/domain/transcription/TranscriptionCoordinator.kt
// Module: :feature:chat
// Summary: Transcription task coordination, batch gate, and window filtering
// Author: created on 2026-01-05

package com.smartsales.domain.transcription

import android.util.Log
import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.feature.chat.core.transcription.V1BatchIndexPrefixGate
import com.smartsales.feature.chat.core.transcription.V1TingwuWindowedChunkBuilder
import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.feature.chat.home.transcription.ProcessedBatch
import com.smartsales.feature.chat.home.transcription.TranscriptionUiState
import kotlinx.coroutines.flow.*
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TranscriptionCoordinator"

/**
 * Transcription Coordinator: handles Tingwu transcription task coordination, batch gate, and window filtering.
 *
 * Responsibilities:
 * - Manage batch gate (ensure ordered release)
 * - Execute window filtering (macro window range filtering)
 * - Expose Job state flow and processed batch flow
 * - Record Tingwu trace data
 *
 * Design:
 * - Pure coordinator, no UI message creation
 * - UI message creation handled by HomeViewModel
 * - Exposes processed data flows for caller consumption
 */
@Singleton
class TranscriptionCoordinator @Inject constructor(
    private val transcriptionCoordinator: AudioTranscriptionCoordinator,
    private val tingwuTraceStore: TingwuTraceStore,
    optionalConfig: Optional<AiCoreConfig>
) {

    private val _state = MutableStateFlow(TranscriptionUiState())
    val state: StateFlow<TranscriptionUiState> = _state.asStateFlow()

    // Batch gate: ensure batches released in order
    private val batchGate = V1BatchIndexPrefixGate<AudioTranscriptionBatchEvent.BatchReleased>()

    // Window filter switch
    private val enableV1TingwuMacroWindowFilter =
        optionalConfig.orElse(AiCoreConfig()).enableV1TingwuMacroWindowFilter

    /**
     * Observe transcription job state.
     */
    fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> {
        return transcriptionCoordinator.observeJob(jobId)
    }

    /**
     * Observe processed batches (after gate and window filtering).
     *
     * This flow automatically handles:
     * - Batch gate (ensure ordered release)
     * - Window filtering (if enabled)
     * - Tingwu trace recording
     */
    fun observeProcessedBatches(jobId: String): Flow<ProcessedBatch> = flow {
        transcriptionCoordinator.observeBatches(jobId).collect { event ->
            when (event) {
                is AudioTranscriptionBatchEvent.BatchReleased -> {
                    // Check if already marked final
                    if (_state.value.isFinal) {
                        Log.w(
                            TAG,
                            "event=transcription_batch_after_final " +
                                "jobId=${event.jobId} batchIndex=${event.batchIndex}"
                        )
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

    /**
     * Process batch data: apply window filtering, return effective markdown chunk.
     */
    fun processChunk(event: AudioTranscriptionBatchEvent.BatchReleased): String {
        val window = event.v1Window
        val timedSegments = event.timedSegments

        // Apply macro window filtering (if enabled)
        return if (enableV1TingwuMacroWindowFilter && window != null && timedSegments != null) {
            val result = V1TingwuWindowedChunkBuilder.buildWindowedMarkdownChunkResult(
                absStartMs = window.absStartMs,
                absEndMs = window.absEndMs,
                timedSegments = timedSegments
            )

            Log.d(
                TAG,
                "event=v1_tingwu_window_filter_applied " +
                    "batchIndex=${event.batchIndex} " +
                    "absStartMs=${window.absStartMs} " +
                    "absEndMs=${window.absEndMs} " +
                    "segmentsIn=${result.segmentsInCount} " +
                    "segmentsOut=${result.segmentsOutCount} " +
                    "chunkChars=${result.chunk.length}"
            )

            result.chunk
        } else {
            // Fallback to original markdown
            event.markdownChunk
        }
    }

    /**
     * Reset transcription state (call when starting new transcription).
     */
    fun reset() {
        batchGate.reset()
        _state.update {
            TranscriptionUiState()
        }
    }

    /**
     * Mark transcription as final.
     */
    fun markFinal() {
        _state.update { it.copy(isFinal = true) }
    }

    /**
     * Set current message ID being processed.
     */
    fun setCurrentMessageId(messageId: String?) {
        _state.update { it.copy(currentMessageId = messageId) }
    }

    /**
     * Start transcription task.
     */
    fun startTranscription(jobId: String) {
        _state.update {
            it.copy(
                jobId = jobId,
                isActive = true,
                isFinal = false
            )
        }
    }

    /**
     * Merge transcription chunks for streaming display.
     */
    fun mergeChunks(existing: String, incoming: String): String {
        if (existing.isBlank()) return incoming
        if (incoming.isBlank()) return existing
        // Streaming append only: avoid duplication or out-of-order
        val separator = when {
            existing.endsWith("\n") || incoming.startsWith("\n") -> ""
            else -> "\n"
        }
        return existing + separator + incoming
    }
}
