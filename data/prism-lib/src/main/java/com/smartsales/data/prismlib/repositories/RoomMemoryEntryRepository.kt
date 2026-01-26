package com.smartsales.data.prismlib.repositories

import com.smartsales.data.prismlib.db.dao.MemoryEntryDao
import com.smartsales.data.prismlib.db.entities.RoomMemoryEntry
import com.smartsales.domain.prism.core.entities.MemoryEntryEntity
import com.smartsales.domain.prism.core.repositories.MemoryEntryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomMemoryEntryRepository @Inject constructor(
    private val dao: MemoryEntryDao
) : MemoryEntryRepository {

    override suspend fun insert(entry: MemoryEntryEntity) {
        dao.insert(RoomMemoryEntry.fromDomain(entry))
    }

    override suspend fun getById(id: String): MemoryEntryEntity? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getHotZone(): List<MemoryEntryEntity> {
        return dao.getHotZone().map { it.toDomain() }
    }

    override suspend fun getCementZone(): List<MemoryEntryEntity> {
        return dao.getCementZone().map { it.toDomain() }
    }

    override suspend fun archive(id: String) {
        dao.archive(id)
    }

    override suspend fun getBySession(sessionId: String): List<MemoryEntryEntity> {
        return dao.getBySession(sessionId).map { it.toDomain() }
    }
}
