package com.smartsales.data.memory

import com.smartsales.prism.data.persistence.MemoryEntryEntity
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType

fun MemoryEntry.toEntity(): MemoryEntryEntity = MemoryEntryEntity(
    entryId = entryId,
    sessionId = sessionId,
    content = content,
    entryType = entryType.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isArchived = isArchived,
    scheduledAt = scheduledAt,
    structuredJson = structuredJson,
    workflow = workflow,
    title = title,
    completedAt = completedAt,
    outcomeStatus = outcomeStatus,
    outcomeJson = outcomeJson,
    payloadJson = payloadJson,
    displayContent = displayContent,
    artifactsJson = artifactsJson
)

fun MemoryEntryEntity.toDomain(): MemoryEntry = MemoryEntry(
    entryId = entryId,
    sessionId = sessionId,
    content = content,
    entryType = MemoryEntryType.valueOf(entryType),
    createdAt = createdAt,
    updatedAt = updatedAt,
    isArchived = isArchived,
    scheduledAt = scheduledAt,
    structuredJson = structuredJson,
    workflow = workflow,
    title = title,
    completedAt = completedAt,
    outcomeStatus = outcomeStatus,
    outcomeJson = outcomeJson,
    payloadJson = payloadJson,
    displayContent = displayContent,
    artifactsJson = artifactsJson
)
