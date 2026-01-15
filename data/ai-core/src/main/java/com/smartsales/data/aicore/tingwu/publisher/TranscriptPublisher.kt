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
 * Publisher: Lattice interface for transcript artifact extraction and download.
 */
interface Publisher {
    fun extractTranscriptionUrl(resultLinks: Map<String, String>?): String?
    fun extractAutoChaptersUrl(resultLinks: Map<String, String>?): String?
    fun extractSmartSummaryUrl(resultLinks: Map<String, String>?): String?
    fun fetchChaptersSafe(url: String, jobId: String): List<TingwuChapter>?
    fun downloadChapters(url: String, jobId: String): List<TingwuChapter>
    fun fetchSmartSummarySafe(resultLinks: Map<String, String>?, jobId: String): TingwuSmartSummary?
    fun downloadSmartSummary(url: String, jobId: String): TingwuSmartSummary?
}

/**
 * TranscriptPublisher: Real implementation of Publisher.
 */
@Singleton
class TranscriptPublisher @Inject constructor(
    private val config: AiCoreConfig,
    private val pipelineTracer: PipelineTracer,
    private val artifactFetcher: com.smartsales.data.aicore.tingwu.artifact.TingwuArtifactFetcher,
) : Publisher {

    override fun extractTranscriptionUrl(resultLinks: Map<String, String>?): String? {
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

    override fun extractAutoChaptersUrl(resultLinks: Map<String, String>?): String? {
        if (resultLinks.isNullOrEmpty()) return null
        return resultLinks.entries.firstOrNull { it.key.equals("AutoChapters", ignoreCase = true) }?.value
    }

    override fun extractSmartSummaryUrl(resultLinks: Map<String, String>?): String? {
        if (resultLinks.isNullOrEmpty()) return null
        val keys = listOf("MeetingAssistance", "Summarization", "SmartSummary", "Summary")
        return resultLinks.entries.firstOrNull { entry ->
            keys.any { key -> entry.key.equals(key, ignoreCase = true) }
        }?.value
    }

    override fun fetchChaptersSafe(url: String, jobId: String): List<TingwuChapter>? =
        runCatching { downloadChapters(url, jobId) }.onFailure {
            AiCoreLogger.w(TAG, "下载章节失败，将忽略：jobId=$jobId url=${url.take(80)} error=${it.message}")
        }.getOrNull()

    override fun downloadChapters(url: String, jobId: String): List<TingwuChapter> {
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

    override fun fetchSmartSummarySafe(resultLinks: Map<String, String>?, jobId: String): TingwuSmartSummary? {
        val url = extractSmartSummaryUrl(resultLinks) ?: return null
        return runCatching { downloadSmartSummary(url, jobId) }.onFailure {
            AiCoreLogger.w(TAG, "下载智能摘要失败，将忽略：jobId=$jobId url=${url.take(80)} error=${it.message}")
        }.getOrNull()
    }

    override fun downloadSmartSummary(url: String, jobId: String): TingwuSmartSummary? {
        AiCoreLogger.d(TAG, "开始下载智能摘要：jobId=$jobId url=${url.take(100)}...")
        val payload = artifactFetcher.fetchText(url, timeoutMs = config.tingwuReadTimeoutMillis.toInt(), maxChars = 500_000)
            ?: run {
                AiCoreLogger.w(TAG, "下载智能摘要失败：jobId=$jobId error=ArtifactFetcher 返回 null")
                return null
            }
        return parseSmartSummaryPayload(payload)
    }

    private fun parseAutoChaptersPayload(payload: String): List<TingwuChapter> =
        com.smartsales.data.aicore.tingwu.util.TingwuPayloadParser.parseAutoChapters(payload)

    private fun parseSmartSummaryPayload(payload: String): TingwuSmartSummary? =
        com.smartsales.data.aicore.tingwu.util.TingwuPayloadParser.parseSmartSummary(payload)

    companion object {
        private const val TAG = "TranscriptPublisher"
    }
}
