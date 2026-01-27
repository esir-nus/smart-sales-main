package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.entities.*
import com.smartsales.domain.prism.core.repositories.RelevancyRepository

class FakeRelevancyRepository : RelevancyRepository {
    private val store = mutableMapOf<String, RelevancyEntry>()
    
    override suspend fun getByEntityId(id: String) = store[id]
    
    override suspend fun findByAlias(alias: String) = store.values.filter { entry ->
        entry.aliases.any { it.alias == alias }
    }
    
    override suspend fun getAll() = store.values.toList()
    
    override suspend fun upsert(entry: RelevancyEntry) {
        store[entry.entityId] = entry
    }
    
    override suspend fun delete(entityId: String) {
        store.remove(entityId)
    }
}
