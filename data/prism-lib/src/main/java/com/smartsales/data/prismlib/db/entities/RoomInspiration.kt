package com.smartsales.data.prismlib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsales.domain.prism.core.entities.Inspiration
import com.smartsales.domain.prism.core.entities.InspirationSource

@Entity(tableName = "inspirations")
data class RoomInspiration(
    @PrimaryKey
    val id: String,
    val content: String,
    val source: InspirationSource,
    val relatedEntityIds: List<String>,
    val isPromoted: Boolean,
    val promotedTaskId: String?,
    val createdAt: Long
) {
    fun toDomain(): Inspiration = Inspiration(
        id = id,
        content = content,
        source = source,
        relatedEntityIds = relatedEntityIds,
        isPromoted = isPromoted,
        promotedTaskId = promotedTaskId,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: Inspiration): RoomInspiration = RoomInspiration(
            id = domain.id,
            content = domain.content,
            source = domain.source,
            relatedEntityIds = domain.relatedEntityIds,
            isPromoted = domain.isPromoted,
            promotedTaskId = domain.promotedTaskId,
            createdAt = domain.createdAt
        )
    }
}
