package com.smartsales.data.aicore

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt
// 模块：:data:ai-core
// 说明：转写元数据协同器，封装说话人标签等结构化写入
// 作者：创建于 2025-12-06

import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.RiskLevel
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionStage
import com.smartsales.core.metahub.SpeakerMeta
import com.smartsales.core.metahub.SpeakerRole
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.core.metahub.TranscriptSource
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.core.util.LogTags
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class TranscriptMetadataRequest(
    val transcriptId: String,
    val sessionId: String?,
    val diarizedSegments: List<DiarizedSegment>,
    val speakerLabels: Map<String, String>,
    val createdAt: Long = System.currentTimeMillis(),
    val force: Boolean = false
)

/**
 * 负责根据转写片段生成转写元数据，并写入 MetaHub。
 */
interface TranscriptOrchestrator {
    suspend fun inferTranscriptMetadata(request: TranscriptMetadataRequest): TranscriptMetadata?
}

@Singleton
class RealTranscriptOrchestrator @Inject constructor(
    private val metaHub: MetaHub,
    private val dispatchers: DispatcherProvider,
    private val aiChatService: AiChatService
) : TranscriptOrchestrator {

    override suspend fun inferTranscriptMetadata(
        request: TranscriptMetadataRequest
    ): TranscriptMetadata? = withContext(dispatchers.default) {
        val cached = readCachedMetadata(request)
        if (cached != null) return@withContext cached

        val sampledSegments = sampleSegments(request.diarizedSegments)
        if (sampledSegments.isEmpty()) {
            return@withContext null
        }
        val prompt = buildPrompt(sampledSegments, request.speakerLabels)
        val rawText = when (val result = aiChatService.sendMessage(AiChatRequest(prompt = prompt))) {
            is Result.Success -> result.data.displayText
            is Result.Error -> {
                AiCoreLogger.w(TAG, "转写元数据推理失败：${result.throwable.message}")
                return@withContext null
            }
        }
        val jsonText = extractJsonBlock(rawText) ?: return@withContext null
        val parsed = parseMetadata(jsonText, request, sampledSegments.size) ?: return@withContext null
        persistMetadata(parsed)
    }

    private suspend fun readCachedMetadata(request: TranscriptMetadataRequest): TranscriptMetadata? {
        val sessionId = request.sessionId ?: return null
        val existing = metaHub.getTranscriptBySession(sessionId) ?: return null
        val hasReadableSpeaker = existing.speakerMap.values.any { meta ->
            val name = meta.displayName?.trim().orEmpty()
            if (name.isBlank()) return@any false
            val normalized = name.lowercase(Locale.getDefault())
            !(normalized.startsWith("发言人") || normalized.startsWith("speaker"))
        }
        if (request.force || !hasReadableSpeaker) return null
        val merged = existing.copy(extra = existing.extra + ("speakerInferCacheHit" to true))
        runCatching { metaHub.upsertTranscript(merged) }
        return merged
    }

    private fun sampleSegments(
        segments: List<DiarizedSegment>,
        maxSegments: Int = 30,
        maxCharsPerSegment: Int = 200
    ): List<DiarizedSegment> {
        if (segments.isEmpty()) return emptyList()
        val sorted = segments.sortedBy { it.startMs }
        val earlyContext = sorted.take(6).toMutableList()
        val perSpeaker = linkedMapOf<String, MutableList<DiarizedSegment>>()
        sorted.forEach { segment ->
            val key = segment.speakerId ?: "unknown"
            val bucket = perSpeaker.getOrPut(key) { mutableListOf() }
            if (bucket.size < 2) {
                bucket += segment
            }
        }
        val selected = mutableListOf<DiarizedSegment>()
        selected += earlyContext
        perSpeaker.values.forEach { items ->
            items.forEach { if (selected.size < maxSegments) selected += it }
        }
        if (selected.isEmpty()) {
            selected += sorted.take(maxSegments)
        }
        return selected
            .distinctBy { Triple(it.speakerId, it.startMs, it.text) }
            .take(maxSegments)
            .sortedBy { it.startMs }
            .map { segment ->
                val limited = segment.text.take(maxCharsPerSegment)
                segment.copy(text = limited)
            }
    }

    private fun buildPrompt(
        sampled: List<DiarizedSegment>,
        speakerLabels: Map<String, String>
    ): String {
        val existingLabels = speakerLabels.entries.joinToString("\n") { (id, name) ->
            "- $id: $name"
        }.takeIf { it.isNotBlank() }
        return buildString {
            appendLine("你是一名销售通话的角色识别助手。根据带 speakerId 的片段，推测可读展示名（如“罗总/销售顾问”）和角色，并补充会话元数据。")
            appendLine("请只输出 JSON，推荐结构：")
            appendLine(
                """
{
  "speaker_map": {
    "spk_1": {"display_name": "罗总", "role": "客户", "confidence": 0.9}
  },
  "main_person": "",
  "short_summary": "",
  "summary_title_6chars": "",
  "location": "",
  "stage": "DISCOVERY|NEGOTIATION|PROPOSAL|CLOSING|POST_SALE|UNKNOWN",
  "risk_level": "LOW|MEDIUM|HIGH|UNKNOWN",
  "highlights": [],
  "actionable_tips": []
}"""
            )
            existingLabels?.let {
                appendLine()
                appendLine("已有的说话人标签（可参考）：")
                appendLine(it)
            }
            appendLine()
            appendLine("对话片段：")
            sampled.forEach { segment ->
                val speaker = segment.speakerId ?: "unknown"
                append("- [").append(speaker).append("] ")
                appendLine(segment.text)
            }
        }.trim()
    }

    private fun extractJsonBlock(text: String): String? {
        val fenced = Regex("```json\\s*(\\{[\\s\\S]*?})\\s*```", RegexOption.IGNORE_CASE)
        fenced.find(text)?.let { match ->
            return match.groupValues.getOrNull(1)?.trim()
        }
        val anyFence = Regex("```\\s*(\\{[\\s\\S]*?})\\s*```")
        anyFence.find(text)?.let { match ->
            return match.groupValues.getOrNull(1)?.trim()
        }
        val start = text.indexOf('{')
        if (start == -1) return null
        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return text.substring(start, min(i + 1, text.length)).trim()
                    }
                }
            }
        }
        return null
    }

    private fun parseMetadata(
        jsonText: String,
        request: TranscriptMetadataRequest,
        sampledCount: Int
    ): ParsedMetadata? {
        val obj = runCatching { JSONObject(jsonText) }.getOrElse {
            AiCoreLogger.w(TAG, "转写 JSON 解析失败：${it.message}")
            return null
        }
        val speakerMap = parseSpeakerMap(obj.optJSONObject("speaker_map"))
        val mainPerson = obj.optString("main_person").takeIf { it.isNotBlank() }
        val shortSummary = obj.optString("short_summary").takeIf { it.isNotBlank() }
        val summaryTitle = obj.optString("summary_title_6chars").takeIf { it.isNotBlank() }
        val location = obj.optString("location").takeIf { it.isNotBlank() }
        val stage = obj.optString("stage").takeIf { it.isNotBlank() }?.let { toStage(it) }
        val risk = obj.optString("risk_level").takeIf { it.isNotBlank() }?.let { toRisk(it) }
        val highlights = obj.optJSONArray("highlights")?.let { array ->
            (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
        }.orEmpty()
        val actionable = obj.optJSONArray("actionable_tips")?.let { array ->
            (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
        }.orEmpty()
        val tags = (highlights + actionable).filter { it.isNotBlank() }.toSet()

        val transcript = TranscriptMetadata(
            transcriptId = request.transcriptId,
            sessionId = request.sessionId,
            speakerMap = speakerMap,
            source = TranscriptSource.TINGWU_LLM,
            createdAt = request.createdAt,
            diarizedSegmentsCount = request.diarizedSegments.size,
            mainPerson = mainPerson,
            shortSummary = shortSummary,
            summaryTitle6Chars = summaryTitle,
            location = location,
            stage = stage,
            riskLevel = risk,
            extra = mapOf("sampledSegments" to sampledCount)
        )
        val sessionMeta = request.sessionId?.let { sessionId ->
            if (mainPerson == null &&
                shortSummary == null &&
                summaryTitle == null &&
                location == null &&
                tags.isEmpty()
            ) {
                null
            } else {
                SessionMetadata(
                    sessionId = sessionId,
                    mainPerson = mainPerson,
                    shortSummary = shortSummary,
                    summaryTitle6Chars = summaryTitle,
                    location = location,
                    stage = stage,
                    riskLevel = risk,
                    tags = tags,
                    lastUpdatedAt = System.currentTimeMillis()
                )
            }
        }
        return ParsedMetadata(transcript = transcript, session = sessionMeta)
    }

    private fun parseSpeakerMap(obj: JSONObject?): Map<String, SpeakerMeta> {
        if (obj == null) return emptyMap()
        val map = mutableMapOf<String, SpeakerMeta>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val metaObj = obj.optJSONObject(id) ?: continue
            val displayName = metaObj.optString("display_name").takeIf { it.isNotBlank() }
            val role = metaObj.optString("role").takeIf { it.isNotBlank() }?.let { mapSpeakerRole(it) }
            val confidence = if (metaObj.has("confidence")) {
                val value = metaObj.optDouble("confidence")
                if (value.isNaN()) null else value.toFloat().coerceIn(0f, 1f)
            } else null
            if (displayName != null || role != null || confidence != null) {
                map[id] = SpeakerMeta(
                    displayName = displayName,
                    role = role,
                    confidence = confidence
                )
            }
        }
        return map
    }

    private suspend fun persistMetadata(parsed: ParsedMetadata): TranscriptMetadata {
        val transcript = parsed.transcript
        val existingTranscript = transcript.sessionId?.let { metaHub.getTranscriptBySession(it) }
        val mergedTranscript = existingTranscript?.mergeWith(transcript) ?: transcript
        runCatching { metaHub.upsertTranscript(mergedTranscript) }
        parsed.session?.let { sessionMeta ->
            val existingSession = metaHub.getSession(sessionMeta.sessionId)
            val mergedSession = existingSession?.mergeWith(sessionMeta) ?: sessionMeta
            runCatching { metaHub.upsertSession(mergedSession) }
        }
        return mergedTranscript
    }

    private fun mapSpeakerRole(raw: String?): SpeakerRole? {
        if (raw.isNullOrBlank()) return null
        val normalized = raw.lowercase(Locale.getDefault())
        return when {
            normalized.contains("客") || normalized.contains("client") || normalized.contains("buyer") -> SpeakerRole.CUSTOMER
            normalized.contains("销") || normalized.contains("sales") || normalized.contains("顾问") -> SpeakerRole.SALES
            normalized.contains("other") || normalized.contains("其他") -> SpeakerRole.OTHER
            else -> SpeakerRole.UNKNOWN
        }
    }

    private fun toStage(value: String): SessionStage? = when (value.uppercase(Locale.getDefault())) {
        "DISCOVERY" -> SessionStage.DISCOVERY
        "NEGOTIATION" -> SessionStage.NEGOTIATION
        "PROPOSAL" -> SessionStage.PROPOSAL
        "CLOSING" -> SessionStage.CLOSING
        "POST_SALE", "POSTSALE", "POST-SALE" -> SessionStage.POST_SALE
        "UNKNOWN" -> SessionStage.UNKNOWN
        else -> null
    }

    private fun toRisk(value: String): RiskLevel? = when (value.uppercase(Locale.getDefault())) {
        "LOW" -> RiskLevel.LOW
        "MEDIUM" -> RiskLevel.MEDIUM
        "HIGH" -> RiskLevel.HIGH
        "UNKNOWN" -> RiskLevel.UNKNOWN
        else -> null
    }

    private data class ParsedMetadata(
        val transcript: TranscriptMetadata,
        val session: SessionMetadata?
    )

    companion object {
        private val TAG = "${LogTags.AI_CORE}/TranscriptOrchestrator"
    }
}
