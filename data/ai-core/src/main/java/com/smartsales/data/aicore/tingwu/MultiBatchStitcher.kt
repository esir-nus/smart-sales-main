package com.smartsales.data.aicore.tingwu

import com.smartsales.data.aicore.disector.DisectorBatch
import com.smartsales.data.aicore.tingwu.api.TingwuTranscription
import com.smartsales.data.aicore.tingwu.api.TingwuTranscriptSegment
import com.smartsales.data.aicore.tingwu.api.TingwuSpeaker
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MultiBatchStitcher: Pure domain logic for stitching multi-batch transcriptions.
 *
 * Implements V1 Spec logic:
 * 1. Offset correction: Tingwu returns timestamps relative to slice start (0s).
 *    We must add `batch.captureStartMs` to get absolute recording timeline.
 * 2. Overlap filtering: Filter segments to keep only those within [absStart, absEnd).
 */
@Singleton
class MultiBatchStitcher @Inject constructor() {

    fun stitch(
        batches: List<DisectorBatch>,
        transcriptions: List<TingwuTranscription>
    ): TingwuTranscription {
        require(batches.size == transcriptions.size) {
            "Mismatch between batches count (${batches.size}) and transcriptions count (${transcriptions.size})"
        }

        val stitchedSegments = mutableListOf<TingwuTranscriptSegment>()
        val allSpeakers = mutableListOf<TingwuSpeaker>()
        val joinedTextBuilder = StringBuilder()

        batches.zip(transcriptions).forEach { (batch, transcription) ->
            // Offset in seconds (Double)
            val offsetSec = batch.captureStartMs / 1000.0

            // 1. Accumulate Text
            val batchText = transcription.text
            if (!batchText.isNullOrBlank()) {
                if (joinedTextBuilder.isNotEmpty()) joinedTextBuilder.append("\n")
                joinedTextBuilder.append(batchText)
            }

            // 2. Accumulate Speakers (Deduplication done later)
            transcription.speakers?.let { allSpeakers.addAll(it) }

            // 3. Process Segments
            transcription.segments?.forEach { segment ->
                val segStart = segment.start ?: 0.0
                val segEnd = segment.end ?: 0.0
                
                val absStart = segStart + offsetSec
                val absEnd = segEnd + offsetSec

                // Filter logic:
                // Convert absolute seconds back to millis for comparison with batch constraints
                val absStartMs = (absStart * 1000).toLong()
                
                // Strict window filtering
                if (absStartMs >= batch.absStartMs && absStartMs < batch.absEndMs) {
                    stitchedSegments.add(
                        segment.copy(
                            start = absStart,
                            end = absEnd
                            // 'words' field removed as it does not exist in TingwuTranscriptSegment definition
                        )
                    )
                }
            }
        }

        // Deduplicate speakers by ID
        val uniqueSpeakers = allSpeakers.distinctBy { it.id }

        // Determine total duration from last batch's absolute end time
        val totalDuration = batches.lastOrNull()?.let { it.absEndMs / 1000.0 } ?: 0.0

        return TingwuTranscription(
            text = joinedTextBuilder.toString(),
            segments = stitchedSegments.sortedBy { it.start ?: 0.0 },
            speakers = uniqueSpeakers,
            language = transcriptions.firstOrNull()?.language ?: "zh",
            duration = totalDuration,
            url = null // Virtual result has no single URL
        )
    }

    /**
     * Option C: Stitch DiarizedSegment lists directly (simpler than full TingwuTranscription).
     * 
     * @param batchResults List of (batch, segments) pairs from completed jobs.
     * @return Merged list of DiarizedSegments with corrected timestamps.
     */
    fun stitchSegments(
        batchResults: List<Pair<DisectorBatch, List<com.smartsales.data.aicore.DiarizedSegment>>>
    ): List<com.smartsales.data.aicore.DiarizedSegment> {
        val stitched = mutableListOf<com.smartsales.data.aicore.DiarizedSegment>()

        for ((batch, segments) in batchResults) {
            val offsetMs = batch.captureStartMs

            for (segment in segments) {
                val absStartMs = segment.startMs + offsetMs
                val absEndMs = segment.endMs + offsetMs

                // Filter: only keep segments within batch window
                if (absStartMs >= batch.absStartMs && absStartMs < batch.absEndMs) {
                    stitched.add(
                        segment.copy(
                            startMs = absStartMs,
                            endMs = absEndMs
                        )
                    )
                }
            }
        }

        return stitched.sortedBy { it.startMs }
    }
}
