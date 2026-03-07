package com.smartsales.prism.data.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


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

