package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.entities.ScheduledTask
import com.smartsales.domain.prism.core.repositories.ScheduledTaskRepository

class FakeScheduledTaskRepository : ScheduledTaskRepository {
    private val store = mutableMapOf<String, ScheduledTask>()
    
    override suspend fun getAll() = store.values.sortedBy { it.scheduledAt }
    
    override suspend fun getById(id: String) = store[id]
    
    override suspend fun getForDateRange(startMs: Long, endMs: Long) =
        store.values.filter { it.scheduledAt in startMs..endMs }
    
    override suspend fun getUpcoming(limit: Int) =
        store.values
            .filter { it.scheduledAt > System.currentTimeMillis() }
            .sortedBy { it.scheduledAt }
            .take(limit)
    
    override suspend fun insert(task: ScheduledTask) {
        store[task.id] = task
    }
    
    override suspend fun update(task: ScheduledTask) {
        store[task.id] = task
    }
    
    override suspend fun delete(id: String) {
        store.remove(id)
    }
}
