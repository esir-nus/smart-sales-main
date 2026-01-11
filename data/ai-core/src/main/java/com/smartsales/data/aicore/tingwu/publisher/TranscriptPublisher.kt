// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TranscriptPublisher.kt
// Module: :data:ai-core
// Summary: Transcript URL extraction and download helpers
// Author: created on 2026-01-05

package com.smartsales.data.aicore.tingwu.publisher

import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.AiCoreLogger
import com.smartsales.data.aicore.TingwuChapter
import com.smartsales.data.aicore.TingwuSmartSummary
import com.smartsales.data.aicore.debug.PipelineStage
import com.smartsales.data.aicore.debug.PipelineTracer
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Transcript URL extraction and download utilities.
 */
@Singleton
class TranscriptPublisher @Inject constructor(
    private val config: AiCoreConfig,
    private val pipelineTracer: PipelineTracer,
    private val artifactFetcher: com.smartsales.data.aicore.tingwu.artifact.TingwuArtifactFetcher,
) {

    fun extractTranscriptionUrl(resultLinks: Map<String, String>?): String? {
        if (resultLinks == null) {
            AiCoreLogger.w(TAG, "extractTranscriptionUrl: resultLinks 为 null")
            return null
        }
        if (resultLinks.isEmpty()) {
            AiCoreLogger.w(TAG, "extractTranscriptionUrl: resultLinks 为空")
            return null
        }
        val availableKeys = resultLinks.keys.joinToString(", ")
        AiCoreLogger.d(TAG, "extractTranscriptionUrl: 可用键 [$availableKeys]")
        val transcriptionUrl = resultLinks
            .entries
            .firstOrNull { it.key.equals("Transcription", ignoreCase = true) }
            ?.value
            ?.takeIf { it.isNotBlank() }
        if (transcriptionUrl != null) {
            AiCoreLogger.d(TAG, "extractTranscriptionUrl: 找到 Transcription URL (长度=${transcriptionUrl.length})")
        } else {
            AiCoreLogger.w(TAG, "extractTranscriptionUrl: 未找到 Transcription 键，可用键: [$availableKeys]")
        }
        return transcriptionUrl
    }

    fun extractAutoChaptersUrl(resultLinks: Map<String, String>?): String? {
        if (resultLinks.isNullOrEmpty()) return null
        return resultLinks.entries.firstOrNull { it.key.equals("AutoChapters", ignoreCase = true) }?.value
    }

    fun extractSmartSummaryUrl(resultLinks: Map<String, String>?): String? {
        if (resultLinks.isNullOrEmpty()) return null
        val keys = listOf("MeetingAssistance", "Summarization", "SmartSummary", "Summary")
        return resultLinks.entries.firstOrNull { entry ->
            keys.any { key -> entry.key.equals(key, ignoreCase = true) }
        }?.value
    }

    fun fetchChaptersSafe(url: String, jobId: String): List<TingwuChapter>? =
        runCatching { downloadChapters(url, jobId) }.onFailure {
            AiCoreLogger.w(TAG, "下载章节失败，将忽略：jobId=$jobId url=${url.take(80)} error=${it.message}")
        }.getOrNull()

    fun downloadChapters(url: String, jobId: String): List<TingwuChapter> {
        AiCoreLogger.d(TAG, "开始下载章节 JSON：jobId=$jobId url=${url.take(100)}...")
        pipelineTracer.emit(PipelineStage.TRANSCRIPT_PUBLISH, "STARTED", "jobId=$jobId type=chapters")
        val payload = artifactFetcher.fetchText(url, timeoutMs = config.tingwuReadTimeoutMillis.toInt(), maxChars = 1_000_000)
            ?: run {
                pipelineTracer.emit(PipelineStage.TRANSCRIPT_PUBLISH, "FAILED", "jobId=$jobId type=chapters error=null payload")
                throw AiCoreException(
                    source = AiCoreErrorSource.TINGWU,
                    reason = AiCoreErrorReason.NETWORK,
                    message = "下载章节失败：ArtifactFetcher 返回 null"
                )
            }
        val chapters = parseAutoChaptersPayload(payload)
        pipelineTracer.emit(PipelineStage.TRANSCRIPT_PUBLISH, "COMPLETED", "jobId=$jobId type=chapters count=${chapters.size}")
        return chapters
    }

    fun fetchSmartSummarySafe(resultLinks: Map<String, String>?, jobId: String): TingwuSmartSummary? {
        val url = extractSmartSummaryUrl(resultLinks) ?: return null
        return runCatching { downloadSmartSummary(url, jobId) }.onFailure {
            AiCoreLogger.w(TAG, "下载智能摘要失败，将忽略：jobId=$jobId url=${url.take(80)} error=${it.message}")
        }.getOrNull()
    }

    fun downloadSmartSummary(url: String, jobId: String): TingwuSmartSummary? {
        AiCoreLogger.d(TAG, "开始下载智能摘要：jobId=$jobId url=${url.take(100)}...")
        val payload = artifactFetcher.fetchText(url, timeoutMs = config.tingwuReadTimeoutMillis.toInt(), maxChars = 500_000)
            ?: run {
                AiCoreLogger.w(TAG, "下载智能摘要失败：jobId=$jobId error=ArtifactFetcher 返回 null")
                return null
            }
        return parseSmartSummaryPayload(payload)
    }

    private fun parseAutoChaptersPayload(payload: String): List<TingwuChapter> {
        val gson = Gson()
        return try {
            val root = JsonParser.parseString(payload).asJsonObject
            val chapters = root.getAsJsonArray("AutoChapters")
                ?.mapNotNull { it.asJsonObject }
                ?.map { chapter ->
                    val headline = chapter.getPrimitiveString("Headline")
                    val title = chapter.getPrimitiveString("Title") ?: ""
                    val summary = chapter.getPrimitiveString("Summary")
                    val startMs = chapter.get("Start")?.asDouble?.toLong()
                        ?: chapter.get("StartTime")?.asDouble?.toLong()
                        ?: 0L
                    val endMs = chapter.get("End")?.asDouble?.toLong()
                        ?: chapter.get("EndTime")?.asDouble?.toLong()
                    TingwuChapter(
                        title = headline ?: title,
                        startMs = startMs,
                        endMs = endMs,
                        headline = headline,
                        summary = summary
                    )
                }
                ?: emptyList()
            AiCoreLogger.d(TAG, "解析章节成功：共 ${chapters.size} 个")
            chapters
        } catch (e: Exception) {
            AiCoreLogger.w(TAG, "解析章节失败：${e.message}")
            emptyList()
        }
    }

    private fun parseSmartSummaryPayload(payload: String): TingwuSmartSummary? {
        return try {
            val obj = JsonParser.parseString(payload).asJsonObject
            val summary = obj.getPrimitiveString("Summary")
                ?: obj.getPrimitiveString("Abstract")
                ?: obj.getPrimitiveString("Summarization")
            val keyPoints = obj.getAsJsonArray("KeyPoints")
                ?: obj.getAsJsonArray("Highlights")
                ?: obj.getAsJsonArray("Keypoints")
            val actionItems = obj.getAsJsonArray("ActionItems")
                ?: obj.getAsJsonArray("Todos")
                ?: obj.getAsJsonArray("Tasks")
            val keys = keyPoints?.mapNotNull { it.asStringOrNull() } ?: emptyList()
            val actions = actionItems?.mapNotNull { it.asStringOrNull() } ?: emptyList()
            if (summary.isNullOrBlank() && keys.isEmpty() && actions.isEmpty()) return null
            TingwuSmartSummary(
                summary = summary,
                keyPoints = keys,
                actionItems = actions
            )
        } catch (e: Exception) {
            AiCoreLogger.w(TAG, "解析智能摘要失败：${e.message}")
            null
        }
    }

    private fun JsonObject.getPrimitiveString(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun JsonElement.asStringOrNull(): String? =
        takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    companion object {
        private const val TAG = "TranscriptPublisher"
    }
}
