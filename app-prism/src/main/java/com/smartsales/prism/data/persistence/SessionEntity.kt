package com.smartsales.prism.data.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsales.prism.domain.model.SessionPreview

/**
 * 会话元数据实体 — 持久化到 Room
 *
 * 与 SessionPreview 1:1 映射，字段名差异: sessionId (PK) → id
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val clientName: String,
    val summary: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val linkedAudioId: String? = null
)

// === 映射函数 ===

fun SessionEntity.toDomain(): SessionPreview = SessionPreview(
    id = sessionId,
    clientName = clientName,
    summary = summary,
    timestamp = timestamp,
    isPinned = isPinned,
    linkedAudioId = linkedAudioId
)

fun SessionPreview.toEntity(): SessionEntity = SessionEntity(
    sessionId = id,
    clientName = clientName,
    summary = summary,
    timestamp = timestamp,
    isPinned = isPinned,
    linkedAudioId = linkedAudioId
)
