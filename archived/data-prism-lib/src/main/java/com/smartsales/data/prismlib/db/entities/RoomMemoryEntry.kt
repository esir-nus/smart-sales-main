package com.smartsales.data.prismlib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsales.domain.prism.core.Mode
import com.smartsales.domain.prism.core.entities.ArtifactMeta
import com.smartsales.domain.prism.core.entities.MemoryEntryEntity
import com.smartsales.domain.prism.core.entities.OutcomeStatus

@Entity(tableName = "memory_entries")
data class RoomMemoryEntry(
    @PrimaryKey
    val id: String,
    val workflow: Mode,
    val title: String,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val sessionId: String,
    val outcomeStatus: OutcomeStatus?,
    val displayContent: String,
    val structuredJson: String?,
    val entityIds: List<String>,
    val artifacts: List<ArtifactMeta>,
    val payloadJson: String?
) {
    fun toDomain(): MemoryEntryEntity = MemoryEntryEntity(
        id = id,
        workflow = workflow,
        title = title,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        sessionId = sessionId,
        outcomeStatus = outcomeStatus,
        displayContent = displayContent,
        structuredJson = structuredJson,
        entityIds = entityIds,
        artifacts = artifacts,
        payloadJson = payloadJson
    )

    companion object {
        fun fromDomain(domain: MemoryEntryEntity): RoomMemoryEntry = RoomMemoryEntry(
            id = domain.id,
            workflow = domain.workflow,
            title = domain.title,
            isArchived = domain.isArchived,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            completedAt = domain.completedAt,
            sessionId = domain.sessionId,
            outcomeStatus = domain.outcomeStatus,
            displayContent = domain.displayContent,
            structuredJson = domain.structuredJson,
            entityIds = domain.entityIds,
            artifacts = domain.artifacts,
            payloadJson = domain.payloadJson
        )
    }
}
