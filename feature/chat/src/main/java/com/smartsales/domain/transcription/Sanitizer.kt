// File: feature/chat/src/main/java/com/smartsales/domain/transcription/Sanitizer.kt
// Module: :feature:chat
// Summary: Transcription display cleanup interface per Orchestrator-V1 Section 3.2
// Author: created on 2026-01-05

package com.smartsales.domain.transcription

import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.data.aicore.tingwu.TingwuTranscription

/**
 * Sanitizer: transcription display cleanup (Orchestrator-V1 Section 3.2).
 * 
 * Input: Tingwu raw transcription + batch context
 * Output: Publisher-ready display transcription data
 * 
 * Note: Sanitizer ensures display safety and consistency; no language polishing.
 */
interface Sanitizer {
    /**
     * Build markdown transcript from Tingwu transcription data.
     * Priority: diarized segments > raw text > individual segments
     */
    fun buildMarkdown(
        transcription: TingwuTranscription?,
        diarizedSegments: List<DiarizedSegment> = emptyList(),
        speakerLabels: Map<String, String> = emptyMap(),
    ): String

    /**
     * Build diarized segments with speaker separation and subtitle merging.
     */
    fun buildDiarizedSegments(transcription: TingwuTranscription?): List<DiarizedSegment>

    /**
     * Build speaker labels from Tingwu transcription data.
     */
    fun buildSpeakerLabels(
        transcription: TingwuTranscription?,
        segments: List<DiarizedSegment>,
    ): Map<String, String>
}
