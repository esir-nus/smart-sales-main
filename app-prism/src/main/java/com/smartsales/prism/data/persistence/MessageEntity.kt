package com.smartsales.prism.data.persistence

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 会话消息实体 — 持久化聊天记录
 *
 * 外键关联 sessions 表，级联删除
 */
@Entity(
    tableName = "session_messages",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["sessionId"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val isUser: Boolean,       // true=用户消息, false=AI消息
    val content: String,       // 文本内容
    val timestamp: Long,
    val orderIndex: Int        // 消息排序
)
