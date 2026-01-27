package com.smartsales.prism.domain.memory

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake MemoryRepository — 内存中存储
 * Phase 1 占位实现
 */
@Singleton
class FakeMemoryRepository @Inject constructor() : MemoryRepository {
    
    private val entries = MutableStateFlow<List<MemoryEntry>>(emptyList())
    
    override suspend fun getHotEntries(sessionId: String): List<MemoryEntry> {
        return entries.value.filter { 
            !it.isArchived && it.sessionId == sessionId 
        }
    }
    
    override suspend fun search(query: String, limit: Int): List<MemoryEntry> {
        return entries.value
            .filter { it.content.contains(query, ignoreCase = true) }
            .take(limit)
    }
    
    override fun observeHotEntries(sessionId: String): Flow<List<MemoryEntry>> {
        return entries.map { list ->
            list.filter { !it.isArchived && it.sessionId == sessionId }
        }
    }
    
    override suspend fun save(entry: MemoryEntry) {
        val current = entries.value.toMutableList()
        current.removeAll { it.entryId == entry.entryId }
        current.add(entry)
        entries.value = current
    }
    
    override suspend fun archive(entryId: String) {
        val current = entries.value.map { entry ->
            if (entry.entryId == entryId) entry.copy(isArchived = true)
            else entry
        }
        entries.value = current
    }
}
