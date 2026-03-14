package com.smartsales.prism.domain.memory

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
    val closeDate: String? = null,           // ISO 8601
    val nextAction: String? = null           // 下一步行动
)

/**
 * 实体类型
 */
enum class EntityType {
    // General Corrupted/Fallback State
    UNKNOWN,
    
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

/**
 * 实体引用
 */
data class EntityRef(
    val entityId: String,
    val displayName: String,
    val entityType: String
)
