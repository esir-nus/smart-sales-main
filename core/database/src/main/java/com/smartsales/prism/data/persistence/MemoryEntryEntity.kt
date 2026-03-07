package com.smartsales.prism.data.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


/**
 * Room 实体 — 记忆条目存储表
 * 
 * 索引策略:
 * - sessionId: 按会话查询活跃/归档条目
 * - isArchived: 过滤活跃区/归档区
 * - createdAt: 时间窗口查询 (订阅层级)
 */
@Entity(
    tableName = "memory_entries",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["isArchived"]),
        Index(value = ["createdAt"])
    ]
)
data class MemoryEntryEntity(
    @PrimaryKey
    val entryId: String,
    val sessionId: String,
    val content: String,
    val entryType: String,  // Enum stored as String
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
    val scheduledAt: Long?,
    val structuredJson: String?,
    val workflow: String? = null,
    val title: String? = null,
    val completedAt: Long? = null,
    val outcomeStatus: String? = null,
    val outcomeJson: String? = null,
    val payloadJson: String? = null,
    val displayContent: String? = null,
    val artifactsJson: String? = null
)

