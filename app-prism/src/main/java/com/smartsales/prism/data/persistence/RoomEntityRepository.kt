package com.smartsales.prism.data.persistence

import android.util.Log
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.EntityType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room 实现 — EntityRepository
 */
@Singleton
class RoomEntityRepository @Inject constructor(
    private val db: PrismDatabase
) : EntityRepository {
    
    private val dao = db.entityDao()
    
    override suspend fun getById(entityId: String): EntityEntry? {
        return dao.getById(entityId)?.toDomain()
    }
    
    override suspend fun findByAlias(alias: String): List<EntityEntry> {
        return dao.findByAlias(alias).map { it.toDomain() }
    }
    
    override suspend fun getByType(entityType: EntityType): List<EntityEntry> {
        return dao.getByType(entityType.name).map { it.toDomain() }
    }
    
    override suspend fun save(entry: EntityEntry) {
        dao.insert(entry.toEntity())
        Log.d("RoomEntity", "💾 保存: id=${entry.entityId} name=${entry.displayName} type=${entry.entityType}")
    }
    
    override suspend fun search(query: String, limit: Int): List<EntityEntry> {
        return dao.search(query, limit).map { it.toDomain() }
    }
    
    override suspend fun getByAccountId(accountId: String): List<EntityEntry> {
        return dao.getByAccountId(accountId).map { it.toDomain() }
    }
    
    override suspend fun delete(entityId: String) {
        dao.delete(entityId)
        Log.d("RoomEntity", "🗑️ 删除: id=$entityId")
    }
}
