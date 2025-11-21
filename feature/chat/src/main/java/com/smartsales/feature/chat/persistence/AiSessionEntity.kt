package com.smartsales.feature.chat.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/persistence/AiSessionEntity.kt
// 模块：:feature:chat
// 说明：存储聊天会话摘要的 Room 实体
// 作者：创建于 2025-11-16
@Entity(tableName = "ai_session")
data class AiSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "preview")
    val preview: String,
    @ColumnInfo(name = "updated_at")
    val updatedAtMillis: Long,
    @ColumnInfo(name = "pinned", defaultValue = "0")
    val pinned: Boolean = false
)
