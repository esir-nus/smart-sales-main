package com.smartsales.domain.prism.core.entities

/**
 * Relevancy Library 条目 — 客户/实体索引
 * @see Prism-V1.md §5.2
 */
data class RelevancyEntry(
    val entityId: String,           // e.g., "z-001" (Person), "p-042" (Product)
    val entityType: EntityType,
    val displayName: String,        // 规范名称（张伟）
    val aliases: List<AliasMapping> = emptyList(),
    val demeanor: Map<String, String> = emptyMap(),  // e.g., {"communication_style": "formal"}
    val attributes: Map<String, String> = emptyMap(), // 当前状态快照
    val metricsHistory: Map<String, List<MetricPoint>> = emptyMap(),
    val relatedEntities: List<RelatedEntity> = emptyList(),
    val decisionLog: List<DecisionRecord> = emptyList(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class EntityType {
    PERSON,
    PRODUCT,
    LOCATION,
    EVENT,
    ORGANIZATION
}

/**
 * 别名映射 — 消歧义计数器
 * @see Prism-V1.md §5.4
 */
data class AliasMapping(
    val alias: String,              // "张总"
    val confirmationCount: Int = 0,
    val lastConfirmedAt: Long = 0,
    val sessionContexts: List<String> = emptyList()
)

/**
 * 指标时间点 — 用于可视化
 */
data class MetricPoint(
    val date: String,       // ISO 8601: "2026-01-15"
    val value: Any,         // Int, String, Boolean
    val unit: String? = null,
    val source: String? = null
)

/**
 * 关联实体
 */
data class RelatedEntity(
    val entityId: String,
    val relation: String    // "interested_in", "works_with"
)

/**
 * 决策记录 — Rethink 历史
 * @see Prism-V1.md §5.5
 */
data class DecisionRecord(
    val timestamp: Long,
    val conflictDescription: String,
    val userResolution: String,
    val resultingEntryId: String? = null
)
