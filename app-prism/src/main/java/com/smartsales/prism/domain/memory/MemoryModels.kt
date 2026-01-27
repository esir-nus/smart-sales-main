package com.smartsales.prism.domain.memory

/**
 * 记忆条目实体 — Hot/Cement Zone 存储单元
 * @see Prism-V1.md §5.1 Two-Zone Model
 */
data class MemoryEntry(
    val entryId: String,
    val sessionId: String,
    val content: String,
    val entryType: MemoryEntryType,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false,
    val scheduledAt: Long? = null,
    val structuredJson: String? = null
)

/**
 * 记忆条目类型
 */
enum class MemoryEntryType {
    USER_MESSAGE,
    ASSISTANT_RESPONSE,
    TASK_RECORD,
    SCHEDULE_ITEM,
    INSPIRATION
}

/**
 * 关联性条目 — Relevancy Library 索引单元
 * @see Prism-V1.md §5.2 Relevancy Library Schema
 */
data class RelevancyEntry(
    val entityId: String,          // e.g., "z-001" (Person), "p-042" (Product)
    val entityType: EntityType,
    val displayName: String,       // 规范名称
    val aliasesJson: String = "[]",
    val demeanorJson: String = "{}",
    val attributesJson: String = "{}",
    val metricsHistoryJson: String = "{}",
    val relatedEntitiesJson: String = "[]",
    val decisionLogJson: String = "[]",
    val lastUpdatedAt: Long,
    val createdAt: Long
)

/**
 * 实体类型
 */
enum class EntityType {
    PERSON,
    COMPANY,
    PRODUCT,
    LOCATION,
    EVENT,
    CUSTOM
}
