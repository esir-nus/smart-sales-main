package com.smartsales.data.prismlib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsales.domain.prism.core.entities.Inspiration
import com.smartsales.domain.prism.core.entities.InspirationSource

@Entity(tableName = "inspirations")
data class RoomInspiration(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val source: InspirationSource,
    val sourceRefId: String?,
    val tags: List<String>,
    val createdAt: Long,
    val isDismissed: Boolean // If we want to track dismissed inspirations
) {
    fun toDomain(): Inspiration = Inspiration(
        id = id,
        title = title,
        description = description,
        source = source,
        sourceRefId = sourceRefId,
        tags = tags,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: Inspiration): RoomInspiration = RoomInspiration(
            id = domain.id,
            title = domain.title,
            description = domain.description,
            source = domain.source,
            sourceRefId = domain.sourceRefId,
            tags = domain.tags,
            createdAt = domain.createdAt,
            isDismissed = false // Default
        )
    }
}
