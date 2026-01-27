package com.smartsales.prism.domain.memory

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake RelevancyRepository — 内存中存储
 * Phase 1 占位实现
 */
@Singleton
class FakeRelevancyRepository @Inject constructor() : RelevancyRepository {
    
    private val entries = mutableMapOf<String, RelevancyEntry>()
    
    override suspend fun getById(entityId: String): RelevancyEntry? {
        return entries[entityId]
    }
    
    override suspend fun findByAlias(alias: String): List<RelevancyEntry> {
        return entries.values.filter { entry ->
            entry.aliasesJson.contains(alias, ignoreCase = true)
        }
    }
    
    override suspend fun getByType(entityType: EntityType): List<RelevancyEntry> {
        return entries.values.filter { it.entityType == entityType }
    }
    
    override suspend fun save(entry: RelevancyEntry) {
        entries[entry.entityId] = entry
    }
    
    override suspend fun search(query: String, limit: Int): List<RelevancyEntry> {
        return entries.values
            .filter { 
                it.displayName.contains(query, ignoreCase = true) ||
                it.aliasesJson.contains(query, ignoreCase = true)
            }
            .take(limit)
    }
}
