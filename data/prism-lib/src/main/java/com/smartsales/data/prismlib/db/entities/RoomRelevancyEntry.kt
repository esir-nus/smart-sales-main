package com.smartsales.data.prismlib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsales.domain.prism.core.entities.*

@Entity(tableName = "relevancy_entries")
data class RoomRelevancyEntry(
    @PrimaryKey
    val entityId: String,
    val entityType: EntityType,
    val displayName: String,
    val aliases: List<AliasMapping>,
    val demeanor: Map<String, String>,
    val attributes: Map<String, String>,
    val metricsHistory: Map<String, List<MetricPoint>>,
    val relatedEntities: List<RelatedEntity>,
    val decisionLog: List<DecisionRecord>,
    val lastUpdatedAt: Long,
    val createdAt: Long
) {
    fun toDomain(): RelevancyEntry = RelevancyEntry(
        entityId = entityId,
        entityType = entityType,
        displayName = displayName,
        aliases = aliases,
        demeanor = demeanor,
        attributes = attributes,
        metricsHistory = metricsHistory,
        relatedEntities = relatedEntities,
        decisionLog = decisionLog,
        lastUpdatedAt = lastUpdatedAt,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: RelevancyEntry): RoomRelevancyEntry = RoomRelevancyEntry(
            entityId = domain.entityId,
            entityType = domain.entityType,
            displayName = domain.displayName,
            aliases = domain.aliases,
            demeanor = domain.demeanor,
            attributes = domain.attributes,
            metricsHistory = domain.metricsHistory,
            relatedEntities = domain.relatedEntities,
            decisionLog = domain.decisionLog,
            lastUpdatedAt = domain.lastUpdatedAt,
            createdAt = domain.createdAt
        )
    }
}
