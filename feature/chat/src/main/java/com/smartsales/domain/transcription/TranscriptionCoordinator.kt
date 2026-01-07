// File: feature/chat/src/main/java/com/smartsales/domain/transcription/TranscriptionCoordinator.kt
// Module: :feature:chat
// Summary: Transcription task coordination interface - batch gate and window filtering
// Author: created on 2026-01-05

package com.smartsales.domain.transcription

import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.chat.home.transcription.ProcessedBatch
import com.smartsales.feature.chat.home.transcription.TranscriptionUiState
import kotlinx.coroutines.flow.*

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
interface TranscriptionCoordinator {

    val state: StateFlow<TranscriptionUiState>

    /**
     * Observe transcription job state.
     */
    fun observeJob(jobId: String): Flow<AudioTranscriptionJobState>

    /**
     * Observe processed batches (after gate and window filtering).
     *
     * This flow automatically handles:
     * - Batch gate (ensure ordered release)
     * - Window filtering (if enabled)
     * - Tingwu trace recording
     */
    fun observeProcessedBatches(jobId: String): Flow<ProcessedBatch>

    /**
     * Process batch data: apply window filtering, return effective markdown chunk.
     */
    fun processChunk(event: AudioTranscriptionBatchEvent.BatchReleased): String

    /**
     * Reset transcription state (call when starting new transcription).
     */
    fun reset()

    /**
     * Mark transcription as final.
     */
    fun markFinal()

    /**
     * Set current message ID being processed.
     */
    fun setCurrentMessageId(messageId: String?)

    /**
     * Start transcription task.
     */
    fun startTranscription(jobId: String)

    /**
     * Merge transcription chunks for streaming display.
     */
    fun mergeChunks(existing: String, incoming: String): String
}
