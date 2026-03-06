package com.smartsales.prism.data.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType

/**
 * Room 实体 — 实体注册表存储
 * 
 * 索引策略:
 * - entityType: 按类型过滤 (PERSON, ACCOUNT, etc.)
 * - displayName: 按名称搜索
 * - accountId: CRM 层级查询 (contacts/deals by account)
 */
@Entity(
    tableName = "entity_entries",
    indices = [
        Index(value = ["entityType"]),
        Index(value = ["displayName"]),
        Index(value = ["accountId"])
    ]
)
data class EntityEntryEntity(
    @PrimaryKey
    val entityId: String,
    val entityType: String,  // Enum stored as String
    val displayName: String,
    val aliasesJson: String,
    val demeanorJson: String,
    val attributesJson: String,
    val metricsHistoryJson: String,
    val relatedEntitiesJson: String,
    val decisionLogJson: String,
    val lastUpdatedAt: Long,
    val createdAt: Long,
    
    // CRM Extension Fields (nullable)
    val accountId: String?,
    val primaryContactId: String?,
    val jobTitle: String?,
    val buyingRole: String?,
    val dealStage: String?,
    val dealValue: Long?,
    val closeDate: String?,
    val nextAction: String?
)

/**
 * 域模型映射
 */
fun EntityEntry.toEntity(): EntityEntryEntity = EntityEntryEntity(
    entityId = entityId,
    entityType = entityType.name,
    displayName = displayName,
    aliasesJson = aliasesJson,
    demeanorJson = demeanorJson,
    attributesJson = attributesJson,
    metricsHistoryJson = metricsHistoryJson,
    relatedEntitiesJson = relatedEntitiesJson,
    decisionLogJson = decisionLogJson,
    lastUpdatedAt = lastUpdatedAt,
    createdAt = createdAt,
    accountId = accountId,
    primaryContactId = primaryContactId,
    jobTitle = jobTitle,
    buyingRole = buyingRole,
    dealStage = dealStage,
    dealValue = dealValue,
    closeDate = closeDate,
    nextAction = nextAction
)

fun EntityEntryEntity.toDomain(): EntityEntry = EntityEntry(
    entityId = entityId,
    entityType = EntityType.valueOf(entityType),
    displayName = displayName,
    aliasesJson = aliasesJson,
    demeanorJson = demeanorJson,
    attributesJson = attributesJson,
    metricsHistoryJson = metricsHistoryJson,
    relatedEntitiesJson = relatedEntitiesJson,
    decisionLogJson = decisionLogJson,
    lastUpdatedAt = lastUpdatedAt,
    createdAt = createdAt,
    accountId = accountId,
    primaryContactId = primaryContactId,
    jobTitle = jobTitle,
    buyingRole = buyingRole,
    dealStage = dealStage,
    dealValue = dealValue,
    closeDate = closeDate,
    nextAction = nextAction
)
