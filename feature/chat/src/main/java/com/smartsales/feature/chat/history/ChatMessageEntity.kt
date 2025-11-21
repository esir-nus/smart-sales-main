package com.smartsales.feature.chat.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/history/ChatMessageEntity.kt
// 模块：:feature:chat
// 说明：Home 聊天记录的持久化实体
// 作者：创建于 2025-11-22
@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["session_id"])]
)
data class ChatMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "timestamp")
    val timestampMillis: Long
)
