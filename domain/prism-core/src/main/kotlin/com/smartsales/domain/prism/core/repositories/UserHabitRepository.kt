package com.smartsales.domain.prism.core.repositories

import com.smartsales.domain.prism.core.entities.UserHabit

/**
 * 用户习惯仓库
 * @see Prism-V1.md §5.9
 */
interface UserHabitRepository {
    suspend fun getAll(): List<UserHabit>
    suspend fun getByKey(habitKey: String): UserHabit?
    suspend fun getForEntity(entityId: String): List<UserHabit>
    suspend fun upsert(habit: UserHabit)
    suspend fun delete(habitKey: String)
}
