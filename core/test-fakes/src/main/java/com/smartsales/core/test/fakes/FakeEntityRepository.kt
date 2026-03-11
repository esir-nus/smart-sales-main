package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake EntityRepository — 内存中存储
 * 用于骨架开发和测试
 */
@Singleton
class FakeEntityRepository @Inject constructor() : EntityRepository {
    
    private val mutex = Mutex()
    private val entries = mutableMapOf<String, EntityEntry>()
    
    var getByIdCount = 0
        private set
    
    // NOTE: No hardcoded test data. Tests should seed their own data.
    
    override suspend fun getById(entityId: String): EntityEntry? = mutex.withLock {
        getByIdCount++
        return@withLock entries[entityId]
    }
    
    override suspend fun findByAlias(alias: String): List<EntityEntry> = mutex.withLock {
        return@withLock entries.values.filter { entry ->
            entry.aliasesJson.contains(alias, ignoreCase = true)
        }
    }
    
    override suspend fun findByDisplayName(name: String): List<EntityEntry> = mutex.withLock {
        return@withLock entries.values
            .filter { it.displayName == name }
            .sortedByDescending { it.lastUpdatedAt }
    }
    
    override suspend fun getByType(entityType: EntityType): List<EntityEntry> = mutex.withLock {
        return@withLock entries.values.filter { it.entityType == entityType }
    }
    
    override suspend fun save(entry: EntityEntry) = mutex.withLock {
        entries[entry.entityId] = entry
    }
    
    override suspend fun search(query: String, limit: Int): List<EntityEntry> = mutex.withLock {
        return@withLock entries.values
            .filter { 
                it.displayName.contains(query, ignoreCase = true) ||
                it.aliasesJson.contains(query, ignoreCase = true)
            }
            .take(limit)
    }
    
    override suspend fun getByAccountId(accountId: String): List<EntityEntry> = mutex.withLock {
        return@withLock entries.values.filter { it.accountId == accountId }
    }
    
    override suspend fun delete(entityId: String) {
        mutex.withLock { entries.remove(entityId) }
    }
    
    override suspend fun getAll(limit: Int): List<EntityEntry> = mutex.withLock {
        return@withLock entries.values.toList().take(limit)
    }
}

// Backwards compatibility alias (deprecated, will be removed)
@Deprecated("Use FakeEntityRepository instead", ReplaceWith("FakeEntityRepository"))
typealias FakeRelevancyRepository = FakeEntityRepository
