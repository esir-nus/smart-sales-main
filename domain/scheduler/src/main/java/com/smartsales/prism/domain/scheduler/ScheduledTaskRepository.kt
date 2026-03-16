package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.memory.DurationSource
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * Scheduler Repository — Tasks, Inspirations, Conflicts
 * @see prism-ui-ux-contract.md §1.3
 */
interface ScheduledTaskRepository {
    /**
     * 获取指定日期偏移的时间线项目 (0 = 今天)
     */
    fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>>

    /**
     * 按日期范围查询任务
     */
    fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<SchedulerTimelineItem>>

    /**
     * 插入新任务
     */
    suspend fun insertTask(task: ScheduledTask): String

    /**
     * 获取单个任务
     */
    suspend fun getTask(id: String): ScheduledTask?

    /**
     * 更新任务
     */
    suspend fun updateTask(task: ScheduledTask)
    
    /**
     * Wave 14: Upsert任务 (存在则更新，不存在则插入)
     * 用于保证 Path B 和 Path A 的竞争安全
     */
    suspend fun upsertTask(task: ScheduledTask): String

    /**
     * Path A (Wave 17): Batch inserts multiple tasks atomically.
     */
    suspend fun batchInsertTasks(tasks: List<ScheduledTask>): List<String>
    
    /**
     * Path A (Wave 17): Reschedules an existing task.
     * Evaluates conflict rules, deletes old task, and inserts new task identically, forcefully inheriting GUID.
     */
    suspend fun rescheduleTask(oldTaskId: String, newTask: ScheduledTask)

    /**
     * 删除时间线项目
     */
    suspend fun deleteItem(id: String)

    /**
     * 最近已完成的任务（hero dashboard 用）
     */
    suspend fun getRecentCompleted(limit: Int): List<ScheduledTask>

    /**
     * 获取实体关联的最紧急的活跃 L1/L2 任务（nextAction 缓存用）
     */
    suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask?

    /**
     * 按实体ID观察任务
     */
    fun observeByEntityId(entityId: String): Flow<List<ScheduledTask>>
}




