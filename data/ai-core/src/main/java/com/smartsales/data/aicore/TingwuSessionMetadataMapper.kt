package com.smartsales.data.aicore

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/TingwuSessionMetadataMapper.kt
// 模块：:data:ai-core
// 说明：将 Tingwu 产出的摘要等信息映射为 SessionMetadata 增量，供 MetaHub 合并
// 作者：创建于 2025-12-09

import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionTitlePolicy

data class TingwuMetadataInput(
    val sessionId: String,
    val callSummary: String?,
    val shortTitleHint: String?,
    val mainPersonName: String?,
    val tags: Set<String>?,
    val completedAt: Long = System.currentTimeMillis()
)

/**
 * Tingwu 目前只提供通用转写/摘要，这里仅输出安全子集：
 * shortSummary / summaryTitle6Chars / mainPerson / tags，不推断销售阶段或风险。
 */
object TingwuSessionMetadataMapper {

    private const val SUMMARY_LIMIT = 240

    fun toMetadataPatch(input: TingwuMetadataInput): SessionMetadata? {
        val summary = input.callSummary?.trim()?.takeIf { it.isNotBlank() }?.take(SUMMARY_LIMIT)
        val titleHint = input.shortTitleHint?.trim()?.takeIf { it.isNotBlank() }
        val summaryTitle = resolveSummaryTitle(titleHint, summary)
        val mainPerson = input.mainPersonName?.trim()?.takeIf { it.isNotBlank() }
        val tags = input.tags?.filter { it.isNotBlank() }?.toSet().orEmpty()
        val hasContent = summary != null || summaryTitle != null || mainPerson != null || tags.isNotEmpty()
        if (!hasContent) return null
        return SessionMetadata(
            sessionId = input.sessionId,
            mainPerson = mainPerson,
            shortSummary = summary,
            summaryTitle6Chars = summaryTitle,
            tags = tags,
            lastUpdatedAt = input.completedAt
        )
    }

    private fun resolveSummaryTitle(hint: String?, summary: String?): String? {
        val candidate = hint?.takeIf { it.isNotBlank() } ?: summary
        return SessionTitlePolicy.resolveSummary(candidate)
    }
}
