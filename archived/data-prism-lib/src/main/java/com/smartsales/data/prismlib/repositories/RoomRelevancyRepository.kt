package com.smartsales.data.prismlib.repositories

import com.smartsales.data.prismlib.db.dao.RelevancyDao
import com.smartsales.data.prismlib.db.entities.RoomRelevancyEntry
import com.smartsales.domain.prism.core.entities.RelevancyEntry
import com.smartsales.domain.prism.core.repositories.RelevancyRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRelevancyRepository @Inject constructor(
    private val dao: RelevancyDao
) : RelevancyRepository {

    override suspend fun upsert(entry: RelevancyEntry) {
        dao.upsert(RoomRelevancyEntry.fromDomain(entry))
    }

    override suspend fun getByEntityId(entityId: String): RelevancyEntry? {
        return dao.getById(entityId)?.toDomain()
    }

    override suspend fun findByAlias(alias: String): List<RelevancyEntry> {
        // In-memory filter for Phase 2 as discussed in DAO
        return dao.getAll()
            .map { it.toDomain() }
            .filter { entry -> 
                entry.aliases.any { it.alias == alias } || entry.displayName == alias
            }
    }

    override suspend fun delete(entityId: String) {
        dao.delete(entityId)
    }

    override suspend fun getAll(): List<RelevancyEntry> {
        return dao.getAll().map { it.toDomain() }
    }
}
