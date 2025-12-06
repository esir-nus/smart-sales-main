package com.smartsales.feature.chat.home.orchestrator

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt
// 模块：:feature:chat
// 说明：Home 层 Orchestrator 实现，当前直通聊天服务并写入会话元数据
// 作者：创建于 2025-12-04

import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.CrmRow
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.RiskLevel
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionStage
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class HomeOrchestratorImpl @Inject constructor(
    private val aiChatService: AiChatService,
    private val metaHub: MetaHub
) : HomeOrchestrator {
    override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> {
        return flow {
            aiChatService.streamChat(request).collect { event ->
                if (event is ChatStreamEvent.Completed && shouldParseMetadata(request)) {
                    runCatching { maybeUpsertSessionMetadata(request, event.fullText) }
                }
                emit(event)
            }
        }
    }

    private suspend fun maybeUpsertSessionMetadata(
        request: ChatRequest,
        assistantText: String
    ) {
        val jsonBlock = extractJsonBlock(assistantText) ?: return
        val parsed = parseSessionMetadata(
            sessionId = request.sessionId,
            jsonText = jsonBlock,
            source = resolveAnalysisSource(request)
        ) ?: return
        val merged = mergeWithExisting(request.sessionId, parsed)
        runCatching { metaHub.upsertSession(merged) }
    }

    private suspend fun mergeWithExisting(
        sessionId: String,
        parsed: SessionMetadata
    ): SessionMetadata {
        val existing = metaHub.getSession(sessionId)
        return existing?.mergeWith(parsed) ?: parsed
    }

    private fun shouldParseMetadata(request: ChatRequest): Boolean {
        val mode = request.quickSkillId
        return mode == null || mode == QuickSkillId.SMART_ANALYSIS.name
    }

    private fun extractJsonBlock(text: String): String? {
        val fencedRegex = Regex("```json\\s*(\\{[\\s\\S]*?})\\s*```", RegexOption.IGNORE_CASE)
        fencedRegex.find(text)?.let { match ->
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
                        return text.substring(start, i + 1).trim()
                    }
                }
            }
        }
        return null
    }

    private fun parseSessionMetadata(
        sessionId: String,
        jsonText: String,
        source: AnalysisSource?
    ): SessionMetadata? {
        val obj = runCatching { JSONObject(jsonText) }.getOrElse { return null }
        val mainPerson = obj.optString("main_person").takeIf { it.isNotBlank() }
            ?: obj.optString("mainPerson").takeIf { it.isNotBlank() }
        val shortSummary = obj.optString("short_summary").takeIf { it.isNotBlank() }
        val summaryTitle = obj.optString("summary_title_6chars").takeIf { it.isNotBlank() }
        val location = obj.optString("location").takeIf { it.isNotBlank() }
        val stage = obj.optString("stage").takeIf { it.isNotBlank() }?.let { toStage(it) }
        val risk = obj.optString("risk_level").takeIf { it.isNotBlank() }?.let { toRisk(it) }
        val highlights = obj.optJSONArray("highlights")?.toStringList().orEmpty()
        val actionable = obj.optJSONArray("actionable_tips")?.toStringList().orEmpty()
        val crmRows = obj.optJSONArray("crm_rows")?.toCrmRows().orEmpty()
        val tags = (highlights + actionable).filter { it.isNotBlank() }.toSet()

        if (mainPerson == null &&
            shortSummary == null &&
            summaryTitle == null &&
            location == null &&
            crmRows.isEmpty() &&
            tags.isEmpty()
        ) {
            return null
        }
        return SessionMetadata(
            sessionId = sessionId,
            mainPerson = mainPerson,
            shortSummary = shortSummary,
            summaryTitle6Chars = summaryTitle,
            location = location,
            stage = stage,
            riskLevel = risk,
            tags = tags,
            lastUpdatedAt = System.currentTimeMillis(),
            latestMajorAnalysisMessageId = null,
            latestMajorAnalysisAt = System.currentTimeMillis(),
            latestMajorAnalysisSource = source,
            crmRows = crmRows
        )
    }

    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            optString(i)?.takeIf { it.isNotBlank() }?.let { list.add(it) }
        }
        return list
    }

    private fun JSONArray.toCrmRows(): List<CrmRow> {
        val rows = mutableListOf<CrmRow>()
        for (i in 0 until length()) {
            val obj = optJSONObject(i) ?: continue
            val row = CrmRow(
                client = obj.optString("client"),
                region = obj.optString("region"),
                stage = obj.optString("stage"),
                progress = obj.optString("progress"),
                nextStep = obj.optString("next_step"),
                owner = obj.optString("owner")
            )
            rows += row
        }
        return rows
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

    private fun resolveAnalysisSource(request: ChatRequest): AnalysisSource? {
        val mode = request.quickSkillId
        return when {
            mode == QuickSkillId.SMART_ANALYSIS.name -> AnalysisSource.SMART_ANALYSIS_USER
            mode == null && request.isFirstAssistantReply -> AnalysisSource.GENERAL_FIRST_REPLY
            else -> null
        }
    }
}
