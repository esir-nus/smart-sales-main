package com.smartsales.domain.prism.core.repositories

import com.smartsales.domain.prism.core.entities.ScheduledTask

/**
 * 日程任务仓库
 */
interface ScheduledTaskRepository {
    suspend fun getAll(): List<ScheduledTask>
    suspend fun getById(id: String): ScheduledTask?
    suspend fun getForDateRange(startMs: Long, endMs: Long): List<ScheduledTask>
    suspend fun getUpcoming(limit: Int = 10): List<ScheduledTask>
    suspend fun insert(task: ScheduledTask)
    suspend fun update(task: ScheduledTask)
    suspend fun delete(id: String)
}
