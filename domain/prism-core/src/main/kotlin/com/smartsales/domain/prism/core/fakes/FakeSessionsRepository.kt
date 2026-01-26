package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.entities.Session
import com.smartsales.domain.prism.core.repositories.SessionsRepository

class FakeSessionsRepository : SessionsRepository {
    private val store = mutableMapOf<String, Session>()
    
    override suspend fun getAll() = store.values.sortedByDescending { it.updatedAt }
    
    override suspend fun getById(id: String) = store[id]
    
    override suspend fun getPinned() = store.values.filter { it.isPinned }
    
    override suspend fun insert(session: Session) {
        store[session.id] = session
    }
    
    override suspend fun update(session: Session) {
        store[session.id] = session
    }
    
    override suspend fun delete(id: String) {
        store.remove(id)
    }
    
    override suspend fun pin(id: String, isPinned: Boolean) {
        store[id]?.let { store[id] = it.copy(isPinned = isPinned) }
    }
}
