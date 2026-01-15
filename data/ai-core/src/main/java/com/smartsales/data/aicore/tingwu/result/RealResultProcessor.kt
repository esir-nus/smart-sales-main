package com.smartsales.data.aicore.tingwu.result

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.data.aicore.AiCoreLogger
import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.core.metahub.SpeakerMeta
import com.smartsales.data.aicore.TingwuChapter
import com.smartsales.data.aicore.TingwuJobArtifacts
import com.smartsales.data.aicore.TingwuResultLink
import com.smartsales.data.aicore.TingwuSmartSummary
import com.smartsales.data.aicore.TranscriptMetadataRequest
import com.smartsales.data.aicore.TranscriptOrchestrator
import com.smartsales.data.aicore.tingwu.api.TingwuResultData
import com.smartsales.data.aicore.tingwu.api.TingwuStatusData
import com.smartsales.data.aicore.tingwu.artifact.TingwuArtifactFetcher
import com.smartsales.data.aicore.tingwu.util.TingwuPayloadParser
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ResultProcessor"
private const val CUSTOM_PROMPT_KEY = "CustomPrompt"

/**
 * RealResultProcessor: Production implementation of ResultProcessor.
 *
 * Extracted from TingwuRunner to create a pure Lattice box.
 */
@Singleton
class RealResultProcessor @Inject constructor(
    private val artifactFetcher: TingwuArtifactFetcher,
    private val transcriptOrchestrator: TranscriptOrchestrator,
    private val gson: Gson
) : ResultProcessor {

    override fun buildArtifactsFromStatus(status: TingwuStatusData): TingwuJobArtifacts? =
        buildArtifacts(
            mp3 = status.outputMp3Path,
            mp4 = status.outputMp4Path,
            thumb = status.outputThumbnailPath,
            spectrum = status.outputSpectrumPath,
            links = status.resultLinks,
            customPromptUrl = status.resultLinks?.get(CUSTOM_PROMPT_KEY)
        )

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
    ): TingwuJobArtifacts? =
        buildArtifacts(
            mp3 = result.outputMp3Path ?: fallbackArtifacts?.outputMp3Path,
            mp4 = result.outputMp4Path ?: fallbackArtifacts?.outputMp4Path,
            thumb = result.outputThumbnailPath ?: fallbackArtifacts?.outputThumbnailPath,
            spectrum = result.outputSpectrumPath ?: fallbackArtifacts?.outputSpectrumPath,
            links = result.resultLinks ?: fallbackArtifacts?.resultLinks?.associate { it.label to it.url },
            transcriptionUrl = transcriptionUrl ?: fallbackArtifacts?.transcriptionUrl,
            autoChaptersUrl = autoChaptersUrl ?: fallbackArtifacts?.autoChaptersUrl,
            customPromptUrl = customPromptUrl ?: fallbackArtifacts?.customPromptUrl,
            extraResultUrls = if (extraResultUrls.isNotEmpty()) extraResultUrls else fallbackArtifacts?.extraResultUrls.orEmpty(),
            chapters = chapters ?: fallbackArtifacts?.chapters,
            smartSummary = smartSummary ?: fallbackArtifacts?.smartSummary,
            diarizedSegments = diarizedSegments ?: fallbackArtifacts?.diarizedSegments,
            recordingOriginDiarizedSegments = recordingOriginDiarizedSegments
                ?: fallbackArtifacts?.recordingOriginDiarizedSegments,
            speakerLabels = if (speakerLabels.isNotEmpty()) speakerLabels else fallbackArtifacts?.speakerLabels.orEmpty(),
        )

    override suspend fun refineSpeakerLabels(
        transcriptId: String,
        sessionId: String?,
        diarizedSegments: List<DiarizedSegment>,
        speakerLabels: Map<String, String>
    ): TranscriptMetadata? {
        val request = TranscriptMetadataRequest(
            transcriptId = transcriptId,
            sessionId = sessionId,
            diarizedSegments = diarizedSegments,
            speakerLabels = speakerLabels,
            force = true
        )
        return runCatching { transcriptOrchestrator.inferTranscriptMetadata(request) }
            .onFailure { AiCoreLogger.w(TAG, "写入转写元数据失败：${it.message}") }
            .getOrNull()
    }

    override fun mergeSpeakerLabels(
        base: Map<String, String>,
        incoming: Map<String, SpeakerMeta>,
        minConfidence: Float
    ): Map<String, String> {
        if (incoming.isEmpty()) return base
        val merged = LinkedHashMap(base)
        incoming.forEach { (id, meta) ->
            val name = meta.displayName?.takeIf { it.isNotBlank() }
            val confidence = meta.confidence ?: 0f
            if (name != null && confidence >= minConfidence) {
                merged[id] = name
            } else if (name != null && !merged.containsKey(id)) {
                merged[id] = name
            }
        }
        return merged
    }

    override fun fetchSummarizationText(resultLinks: Map<String, String>?): String? {
        val url = resultLinks?.entries
            ?.firstOrNull { it.key.equals("Summarization", ignoreCase = true) }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val raw = artifactFetcher.fetchText(url) ?: return null
        return runCatching {
            val json = JsonParser.parseString(raw)
            if (!json.isJsonObject) return@runCatching null
            val obj = json.asJsonObject
            val summaryObj = obj.getAsJsonObject("Summarization") ?: obj
            val paragraphTitle = getPrimitiveString(summaryObj, "ParagraphTitle")
            val paragraphSummary = getPrimitiveString(summaryObj, "ParagraphSummary")
            val conversational = summaryObj.getAsJsonArray("ConversationalSummary")
                ?.mapNotNull { element ->
                    element.asJsonObjectOrNull()?.let { item ->
                        val speaker = getPrimitiveString(item, "SpeakerName")
                            ?: getPrimitiveString(item, "SpeakerId")
                        val summary = getPrimitiveString(item, "Summary")
                        if (summary.isNullOrBlank()) null else speaker to summary
                    }
                }.orEmpty()
            val qa = summaryObj.getAsJsonArray("QuestionsAnsweringSummary")
                ?.mapNotNull { element ->
                    element.asJsonObjectOrNull()?.let { item ->
                        val q = getPrimitiveString(item, "Question")?.takeIf { it.isNotBlank() }
                        val a = getPrimitiveString(item, "Answer")?.takeIf { it.isNotBlank() }
                        if (q == null && a == null) null else q to a
                    }
                }.orEmpty()
            if (
                paragraphTitle.isNullOrBlank() &&
                paragraphSummary.isNullOrBlank() &&
                conversational.isEmpty() &&
                qa.isEmpty()
            ) null
            else {
                buildString {
                    if (!paragraphTitle.isNullOrBlank() || !paragraphSummary.isNullOrBlank()) {
                        appendLine("### 段落摘要")
                        paragraphTitle?.let { appendLine("**$it**") }
                        paragraphSummary?.let { appendLine(it) }
                        appendLine()
                    }
                    if (conversational.isNotEmpty()) {
                        appendLine("### 发言人总结")
                        conversational.forEach { (speaker, summary) ->
                            val label = speaker ?: "说话人"
                            appendLine("- **$label**：$summary")
                        }
                        appendLine()
                    }
                    if (qa.isNotEmpty()) {
                        appendLine("### 问答回顾")
                        qa.forEach { (q, a) ->
                            q?.let { appendLine("- **Q：** $it") }
                            a?.let { appendLine("  **A：** $it") }
                        }
                        appendLine()
                    }
                }.trim()
            }
        }.getOrNull()
    }

    override fun fetchAutoChapters(resultLinks: Map<String, String>?): List<ChapterDisplay>? {
        val url = resultLinks?.entries
            ?.firstOrNull { it.key.equals("AutoChapters", ignoreCase = true) }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val raw = artifactFetcher.fetchText(url) ?: return null
        return runCatching {
            val root = JsonParser.parseString(raw)
            val arr = when {
                root.isJsonObject -> root.asJsonObject.getAsJsonArray("AutoChapters")
                root.isJsonArray -> root.asJsonArray
                else -> null
            } ?: return@runCatching null
            arr.mapNotNull { element ->
                val obj = element.asJsonObjectOrNull() ?: return@mapNotNull null
                val headline = getPrimitiveString(obj, "Headline") ?: getPrimitiveString(obj, "Title")
                val summary = getPrimitiveString(obj, "Summary")
                val start = obj.get("Start")?.asLongOrNull()
                    ?: obj.get("StartTime")?.asLongOrNull()
                    ?: obj.get("StartMs")?.asLongOrNull()
                if (headline.isNullOrBlank()) null
                else ChapterDisplay(startMs = start ?: 0, headline = headline, summary = summary)
            }.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    override fun buildChaptersText(artifacts: TingwuJobArtifacts?, resultLinks: Map<String, String>?): String? {
        val parsedChapters = artifacts?.chapters
        val chapters = if (!parsedChapters.isNullOrEmpty()) {
            parsedChapters.map {
                ChapterDisplay(
                    startMs = it.startMs,
                    headline = it.displayTitle(),
                    summary = it.summary
                )
            }
        } else {
            fetchAutoChapters(resultLinks)
        }
        if (chapters.isNullOrEmpty()) return null
        return buildString {
            chapters.forEach { chapter ->
                val start = TingwuPayloadParser.formatTimeMs(chapter.startMs ?: 0)
                appendLine("- [$start] ${chapter.headline}")
                chapter.summary?.takeIf { it.isNotBlank() }?.let { appendLine("  - $it") }
            }
        }.trim()
    }

    // Helpers

    private fun buildArtifacts(
        mp3: String?,
        mp4: String?,
        thumb: String?,
        spectrum: String?,
        links: Map<String, String>?,
        transcriptionUrl: String? = null,
        autoChaptersUrl: String? = null,
        customPromptUrl: String? = null,
        extraResultUrls: Map<String, String> = emptyMap(),
        chapters: List<TingwuChapter>? = null,
        smartSummary: TingwuSmartSummary? = null,
        diarizedSegments: List<DiarizedSegment>? = null,
        recordingOriginDiarizedSegments: List<DiarizedSegment>? = null,
        speakerLabels: Map<String, String> = emptyMap(),
    ): TingwuJobArtifacts? {
        if (
            mp3.isNullOrBlank() &&
            mp4.isNullOrBlank() &&
            thumb.isNullOrBlank() &&
            spectrum.isNullOrBlank() &&
            links.isNullOrEmpty() &&
            transcriptionUrl.isNullOrBlank() &&
            autoChaptersUrl.isNullOrBlank() &&
            customPromptUrl.isNullOrBlank() &&
            extraResultUrls.isEmpty() &&
            chapters.isNullOrEmpty() &&
            smartSummary == null &&
            diarizedSegments.isNullOrEmpty() &&
            recordingOriginDiarizedSegments.isNullOrEmpty() &&
            speakerLabels.isEmpty()
        ) {
            return null
        }
        val resultLinks = links.orEmpty()
            .mapNotNull { (label, url) ->
                url.takeIf { it.isNotBlank() }?.let { TingwuResultLink(label, it) }
            }
        return TingwuJobArtifacts(
            outputMp3Path = mp3,
            outputMp4Path = mp4,
            outputThumbnailPath = thumb,
            outputSpectrumPath = spectrum,
            resultLinks = resultLinks,
            transcriptionUrl = transcriptionUrl,
            autoChaptersUrl = autoChaptersUrl,
            customPromptUrl = customPromptUrl ?: links?.get(CUSTOM_PROMPT_KEY),
            extraResultUrls = extraResultUrls,
            chapters = chapters,
            smartSummary = smartSummary,
            diarizedSegments = diarizedSegments,
            recordingOriginDiarizedSegments = recordingOriginDiarizedSegments,
            speakerLabels = speakerLabels,
        )
    }

    private fun com.google.gson.JsonElement.asJsonObjectOrNull(): com.google.gson.JsonObject? =
        takeIf { it.isJsonObject }?.asJsonObject

    private fun com.google.gson.JsonElement.asLongOrNull(): Long? =
        runCatching { asLong }.getOrNull()
        
    private fun getPrimitiveString(obj: com.google.gson.JsonObject, key: String): String? {
        return obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString
    }
}
