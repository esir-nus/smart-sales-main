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
    val mainPerson: String?,
    val shortSummary: String?,
    val summaryTitle6Chars: String?,
    val location: String?,
    val stage: SessionStage?,
    val riskLevel: RiskLevel?,
    val tags: Set<String> = emptySet(),
    val lastUpdatedAt: Long
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
