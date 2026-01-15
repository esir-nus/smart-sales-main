package com.smartsales.data.aicore.tingwu.result

import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.core.metahub.SpeakerMeta
import com.smartsales.data.aicore.TingwuChapter
import com.smartsales.data.aicore.TingwuJobArtifacts
import com.smartsales.data.aicore.TingwuSmartSummary
import com.smartsales.data.aicore.tingwu.api.TingwuResultData
import com.smartsales.data.aicore.tingwu.api.TingwuStatusData
import com.smartsales.core.metahub.TranscriptMetadata

// ============================================================================
// ResultProcessor: Lattice Box for processing Tingwu result artifacts
// Per Orchestrator-Lattice.md §2: DTOs + Interface + Fake in one file
// ============================================================================

/**
 * Chapter display DTO for result processing.
 */
data class ChapterDisplay(
    val startMs: Long?,
    val headline: String,
    val summary: String?
)

/**
 * ResultProcessor: Lattice interface for processing Tingwu results.
 *
 * Responsibilities:
 * - Build TingwuJobArtifacts from API responses
 * - Merge speaker labels from LLM refinement
 * - Fetch and parse summarization/chapters from result URLs
 */
interface ResultProcessor {
    /**
     * Builds artifacts from TingwuStatusData.
     */
    fun buildArtifactsFromStatus(status: TingwuStatusData): TingwuJobArtifacts?

    /**
     * Builds artifacts from TingwuResultData with fallback.
     */
    fun buildArtifactsFromResult(
        result: TingwuResultData,
        fallbackArtifacts: TingwuJobArtifacts? = null,
        transcriptionUrl: String? = null,
        autoChaptersUrl: String? = null,
        customPromptUrl: String? = null,
        extraResultUrls: Map<String, String> = emptyMap(),
        chapters: List<TingwuChapter>? = null,
        smartSummary: TingwuSmartSummary? = null,
        diarizedSegments: List<DiarizedSegment>? = null,
        recordingOriginDiarizedSegments: List<DiarizedSegment>? = null,
        speakerLabels: Map<String, String> = emptyMap()
    ): TingwuJobArtifacts?

    /**
     * Refines speaker labels using TranscriptOrchestrator inference.
     */
    suspend fun refineSpeakerLabels(
        transcriptId: String,
        sessionId: String?,
        diarizedSegments: List<DiarizedSegment>,
        speakerLabels: Map<String, String>
    ): TranscriptMetadata?

    /**
     * Merges base speaker labels with incoming LLM-refined labels.
     */
    fun mergeSpeakerLabels(
        base: Map<String, String>,
        incoming: Map<String, SpeakerMeta>,
        minConfidence: Float = 0.6f
    ): Map<String, String>

    /**
     * Fetches and parses summarization text from result links.
     */
    fun fetchSummarizationText(resultLinks: Map<String, String>?): String?

    /**
     * Fetches and parses auto chapters from result links.
     */
    fun fetchAutoChapters(resultLinks: Map<String, String>?): List<ChapterDisplay>?

    /**
     * Builds chapter display text from artifacts or result links.
     */
    fun buildChaptersText(artifacts: TingwuJobArtifacts?, resultLinks: Map<String, String>?): String?
}

/**
 * FakeResultProcessor: Test double for ResultProcessor.
 */
class FakeResultProcessor : ResultProcessor {

    var artifactsFromStatus: TingwuJobArtifacts? = null
    var artifactsFromResult: TingwuJobArtifacts? = null
    var refinedMetadata: TranscriptMetadata? = null
    var mergedLabels: Map<String, String> = emptyMap()
    var summarizationText: String? = null
    var autoChapters: List<ChapterDisplay>? = null
    var chaptersText: String? = null

    override fun buildArtifactsFromStatus(status: TingwuStatusData): TingwuJobArtifacts? = artifactsFromStatus

    override fun buildArtifactsFromResult(
        result: TingwuResultData,
        fallbackArtifacts: TingwuJobArtifacts?,
        transcriptionUrl: String?,
        autoChaptersUrl: String?,
        customPromptUrl: String?,
        extraResultUrls: Map<String, String>,
        chapters: List<TingwuChapter>?,
        smartSummary: TingwuSmartSummary?,
        diarizedSegments: List<DiarizedSegment>?,
        recordingOriginDiarizedSegments: List<DiarizedSegment>?,
        speakerLabels: Map<String, String>
    ): TingwuJobArtifacts? = artifactsFromResult

    override suspend fun refineSpeakerLabels(
        transcriptId: String,
        sessionId: String?,
        diarizedSegments: List<DiarizedSegment>,
        speakerLabels: Map<String, String>
    ): TranscriptMetadata? = refinedMetadata

    override fun mergeSpeakerLabels(
        base: Map<String, String>,
        incoming: Map<String, SpeakerMeta>,
        minConfidence: Float
    ): Map<String, String> = mergedLabels

    override fun fetchSummarizationText(resultLinks: Map<String, String>?): String? = summarizationText

    override fun fetchAutoChapters(resultLinks: Map<String, String>?): List<ChapterDisplay>? = autoChapters

    override fun buildChaptersText(artifacts: TingwuJobArtifacts?, resultLinks: Map<String, String>?): String? = chaptersText

    fun reset() {
        artifactsFromStatus = null
        artifactsFromResult = null
        mergedLabels = emptyMap()
        summarizationText = null
        autoChapters = null
        chaptersText = null
    }
}
