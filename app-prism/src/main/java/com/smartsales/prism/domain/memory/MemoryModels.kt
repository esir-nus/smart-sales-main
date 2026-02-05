package com.smartsales.prism.domain.memory

/**
 * 记忆条目实体 — Active/Archived Zone 存储单元
 * @see Memory Center spec §Two-Zone Model
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
 * 实体条目 — Entity Registry 索引单元
 * @see Entity Registry spec §Domain Models
 */
data class EntityEntry(
    val entityId: String,          // e.g., "z-001" (Person), "a-001" (Account)
    val entityType: EntityType,
    val displayName: String,       // 规范名称
    val aliasesJson: String = "[]",
    val demeanorJson: String = "{}",
    val attributesJson: String = "{}",
    val metricsHistoryJson: String = "{}",
    val relatedEntitiesJson: String = "[]",
    val decisionLogJson: String = "[]",
    val lastUpdatedAt: Long,
    val createdAt: Long,
    
    // CRM Extension Fields (nullable for non-CRM types)
    val accountId: String? = null,           // FK to Account (for CONTACT/DEAL)
    val primaryContactId: String? = null,    // FK to Contact (for DEAL)
    val jobTitle: String? = null,            // Contact job title
    val buyingRole: String? = null,          // economic_buyer, champion, etc.
    val dealStage: String? = null,           // Pipeline stage
    val dealValue: Long? = null,             // Amount (minor units)
    val closeDate: String? = null            // ISO 8601
)

/**
 * 实体类型
 */
enum class EntityType {
    // Core Types
    PERSON,       // Individual (non-CRM)
    PRODUCT,
    LOCATION,
    EVENT,
    
    // CRM Types
    ACCOUNT,      // Company (CRM entity)
    CONTACT,      // Business contact (linked to Account)
    DEAL          // Sales opportunity
}

// Backwards compatibility alias (deprecated, will be removed)
@Deprecated("Use EntityEntry instead", ReplaceWith("EntityEntry"))
typealias RelevancyEntry = EntityEntry
