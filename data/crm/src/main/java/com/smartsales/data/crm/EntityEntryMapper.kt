package com.smartsales.data.crm

import com.smartsales.prism.data.persistence.EntityEntryEntity
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType

fun EntityEntry.toEntity(): EntityEntryEntity = EntityEntryEntity(
    entityId = entityId,
    entityType = entityType.name,
    displayName = displayName,
    aliasesJson = aliasesJson,
    demeanorJson = demeanorJson,
    attributesJson = attributesJson,
    metricsHistoryJson = metricsHistoryJson,
    relatedEntitiesJson = relatedEntitiesJson,
    decisionLogJson = decisionLogJson,
    lastUpdatedAt = lastUpdatedAt,
    createdAt = createdAt,
    accountId = accountId,
    primaryContactId = primaryContactId,
    jobTitle = jobTitle,
    buyingRole = buyingRole,
    dealStage = dealStage,
    dealValue = dealValue,
    closeDate = closeDate,
    nextAction = nextAction
)

fun EntityEntryEntity.toDomain(): EntityEntry = EntityEntry(
    entityId = entityId,
    entityType = EntityType.valueOf(entityType),
    displayName = displayName,
    aliasesJson = aliasesJson,
    demeanorJson = demeanorJson,
    attributesJson = attributesJson,
    metricsHistoryJson = metricsHistoryJson,
    relatedEntitiesJson = relatedEntitiesJson,
    decisionLogJson = decisionLogJson,
    lastUpdatedAt = lastUpdatedAt,
    createdAt = createdAt,
    accountId = accountId,
    primaryContactId = primaryContactId,
    jobTitle = jobTitle,
    buyingRole = buyingRole,
    dealStage = dealStage,
    dealValue = dealValue,
    closeDate = closeDate,
    nextAction = nextAction
)
