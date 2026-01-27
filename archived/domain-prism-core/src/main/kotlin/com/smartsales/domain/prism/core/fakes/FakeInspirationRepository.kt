package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.entities.Inspiration
import com.smartsales.domain.prism.core.repositories.InspirationRepository

class FakeInspirationRepository : InspirationRepository {
    private val store = mutableMapOf<String, Inspiration>()
    
    override suspend fun getAll() = store.values.sortedByDescending { it.createdAt }
    
    override suspend fun getById(id: String) = store[id]
    
    override suspend fun getUnpromoted() = store.values.filter { !it.isPromoted }
    
    override suspend fun insert(inspiration: Inspiration) {
        store[inspiration.id] = inspiration
    }
    
    override suspend fun promoteToTask(id: String, taskId: String) {
        store[id]?.let { 
            store[id] = it.copy(isPromoted = true, promotedTaskId = taskId) 
        }
    }
    
    override suspend fun delete(id: String) {
        store.remove(id)
    }
}
