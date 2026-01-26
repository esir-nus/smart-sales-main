package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.entities.UserHabit
import com.smartsales.domain.prism.core.repositories.UserHabitRepository

class FakeUserHabitRepository : UserHabitRepository {
    private val store = mutableMapOf<String, UserHabit>()
    
    override suspend fun getAll() = store.values.toList()
    
    override suspend fun getByKey(habitKey: String) = store[habitKey]
    
    override suspend fun getForEntity(entityId: String) = 
        store.values.filter { it.entityId == entityId }
    
    override suspend fun upsert(habit: UserHabit) {
        store[habit.habitKey] = habit
    }
    
    override suspend fun delete(habitKey: String) {
        store.remove(habitKey)
    }
}
