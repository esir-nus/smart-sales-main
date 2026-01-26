package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.entities.MemoryEntryEntity
import com.smartsales.domain.prism.core.repositories.MemoryEntryRepository

class FakeMemoryEntryRepository : MemoryEntryRepository {
    private val store = mutableMapOf<String, MemoryEntryEntity>()
    
    override suspend fun getById(id: String) = store[id]
    
    override suspend fun getHotZone() = store.values.filter { !it.isArchived }
    
    override suspend fun getCementZone() = store.values.filter { it.isArchived }
    
    override suspend fun getBySessionId(sessionId: String) = 
        store.values.filter { it.sessionId == sessionId }
    
    override suspend fun insert(entry: MemoryEntryEntity) {
        store[entry.id] = entry
    }
    
    override suspend fun update(entry: MemoryEntryEntity) {
        store[entry.id] = entry
    }
    
    override suspend fun archive(id: String) {
        store[id]?.let { store[id] = it.copy(isArchived = true) }
    }
    
    override suspend fun delete(id: String) {
        store.remove(id)
    }
}
