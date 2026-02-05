package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake EntityRepository — 内存中存储
 * 用于骨架开发和测试
 */
@Singleton
class FakeEntityRepository @Inject constructor() : EntityRepository {
    
    private val entries = mutableMapOf<String, EntityEntry>()
    
    // NOTE: No hardcoded test data. Tests should seed their own data.
    
    override suspend fun getById(entityId: String): EntityEntry? {
        return entries[entityId]
    }
    
    override suspend fun findByAlias(alias: String): List<EntityEntry> {
        return entries.values.filter { entry ->
            entry.aliasesJson.contains(alias, ignoreCase = true)
        }
    }
    
    override suspend fun getByType(entityType: EntityType): List<EntityEntry> {
        return entries.values.filter { it.entityType == entityType }
    }
    
    override suspend fun save(entry: EntityEntry) {
        entries[entry.entityId] = entry
    }
    
    override suspend fun search(query: String, limit: Int): List<EntityEntry> {
        return entries.values
            .filter { 
                it.displayName.contains(query, ignoreCase = true) ||
                it.aliasesJson.contains(query, ignoreCase = true)
            }
            .take(limit)
    }
}

// Backwards compatibility alias (deprecated, will be removed)
@Deprecated("Use FakeEntityRepository instead", ReplaceWith("FakeEntityRepository"))
typealias FakeRelevancyRepository = FakeEntityRepository
