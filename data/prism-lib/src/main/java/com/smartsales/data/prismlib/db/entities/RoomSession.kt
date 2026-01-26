package com.smartsales.data.prismlib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsales.domain.prism.core.Mode
import com.smartsales.domain.prism.core.entities.Session

@Entity(tableName = "sessions")
data class RoomSession(
    @PrimaryKey
    val id: String,
    val title: String,
    val mode: Mode,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean,
    val previewText: String?
) {
    fun toDomain(): Session = Session(
        id = id,
        title = title,
        mode = mode,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        previewText = previewText
    )

    companion object {
        fun fromDomain(domain: Session): RoomSession = RoomSession(
            id = domain.id,
            title = domain.title,
            mode = domain.mode,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            isPinned = domain.isPinned,
            previewText = domain.previewText
        )
    }
}
