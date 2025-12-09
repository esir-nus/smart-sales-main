package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt
// 模块：:core:util
// 说明：定义会话级元数据，存储客户、摘要等结构化信息
// 作者：创建于 2025-12-04

/**
 * 会话级元数据，仅存结构化事实，不存完整聊天内容。
 */
data class SessionMetadata(
    val sessionId: String,
    val mainPerson: String? = null,
    val shortSummary: String? = null,
    val summaryTitle6Chars: String? = null,
    val location: String? = null,
    val stage: SessionStage? = null,
    val riskLevel: RiskLevel? = null,
    val tags: Set<String> = emptySet(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val latestMajorAnalysisMessageId: String? = null,
    val latestMajorAnalysisAt: Long? = null,
    val latestMajorAnalysisSource: AnalysisSource? = null,
    val crmRows: List<CrmRow> = emptyList()
) {
    /**
     * 非空合并：仅用新值覆盖旧值，tags/CrmRows 合并去重。
     */
    fun mergeWith(other: SessionMetadata): SessionMetadata = SessionMetadata(
        sessionId = sessionId,
        mainPerson = other.mainPerson ?: mainPerson,
        shortSummary = other.shortSummary ?: shortSummary,
        summaryTitle6Chars = other.summaryTitle6Chars ?: summaryTitle6Chars,
        location = other.location ?: location,
        stage = other.stage ?: stage,
        riskLevel = other.riskLevel ?: riskLevel,
        tags = (tags + other.tags).filter { it.isNotBlank() }.toSet(),
        lastUpdatedAt = maxOf(lastUpdatedAt, other.lastUpdatedAt),
        latestMajorAnalysisMessageId = other.latestMajorAnalysisMessageId ?: latestMajorAnalysisMessageId,
        latestMajorAnalysisAt = other.latestMajorAnalysisAt ?: latestMajorAnalysisAt,
        latestMajorAnalysisSource = other.latestMajorAnalysisSource ?: latestMajorAnalysisSource,
        crmRows = mergeCrmRows(crmRows, other.crmRows)
    )

    private fun mergeCrmRows(oldRows: List<CrmRow>, newRows: List<CrmRow>): List<CrmRow> {
        if (newRows.isEmpty()) return oldRows
        if (oldRows.isEmpty()) return newRows
        val combined = (oldRows + newRows).filter { it.client.isNotBlank() || it.owner.isNotBlank() }
        return combined.distinctBy { it.client.trim() + "|" + it.owner.trim() }
    }
}

/**
 * 分析来源，标记最新重大分析的触发渠道。
 */
enum class AnalysisSource {
    GENERAL_FIRST_REPLY,
    SMART_ANALYSIS_USER,
    SMART_ANALYSIS_AUTO,
    TINGWU
}

/**
 * CRM 行，导出 CSV 及 UI 列表使用。
 */
data class CrmRow(
    val client: String = "",
    val region: String = "",
    val stage: String = "",
    val progress: String = "",
    val nextStep: String = "",
    val owner: String = ""
)

/**
 * 销售阶段，仅用于元数据分类。
 */
enum class SessionStage {
    DISCOVERY,
    NEGOTIATION,
    PROPOSAL,
    CLOSING,
    POST_SALE,
    UNKNOWN
}

/**
 * 风险等级，仅用于元数据分类。
 */
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN
}
