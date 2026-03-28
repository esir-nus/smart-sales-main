package com.smartsales.prism.domain.scheduler

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Fake 任务仓库 — 用于开发和测试
 * 注意: 生产环境使用 RealScheduledTaskRepository
 */
class FakeScheduledTaskRepository @Inject constructor() : ScheduledTaskRepository {

    // Initial Mock Data
    // Initial Mock Data (Cleaned per Clean Slate Protocol)
    private val _items = MutableStateFlow<List<SchedulerTimelineItem>>(emptyList())

    override fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>> {
        return _items
    }

    override fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<SchedulerTimelineItem>> {
        // Fake: 返回所有项目，不按日期过滤
        return _items
    }

    override suspend fun insertTask(task: ScheduledTask): String {
        delay(200) // Fake write
        val newId = task.id.ifBlank { (System.currentTimeMillis() % 100000).toString() }
        val newTask = task.copy(id = newId)
        val current = _items.value.toMutableList()
        current.add(newTask)
        _items.value = current
        return newId
    }

    override suspend fun getTask(id: String): ScheduledTask? {
        return _items.value.filterIsInstance<ScheduledTask>().find { it.id == id }
    }

    override suspend fun getActiveTasks(): List<ScheduledTask> {
        return _items.value
            .filterIsInstance<ScheduledTask>()
            .filter { !it.isDone }
            .sortedWith(compareBy<ScheduledTask> { it.urgencyLevel.ordinal }.thenBy { it.startTime })
    }

    override suspend fun updateTask(task: ScheduledTask) {
        delay(200) // Fake write
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            current[index] = task
            _items.value = current
        }
    }

    override suspend fun upsertTask(task: ScheduledTask): String {
        val existing = getTask(task.id)
        return if (existing != null) {
            updateTask(task)
            task.id
        } else {
            insertTask(task)
        }
    }

    override suspend fun deleteItem(id: String) {
        delay(300) // Fake network
        val current = _items.value.toMutableList()
        current.removeAll { it.id == id }
        _items.value = current
    }

    override suspend fun batchInsertTasks(tasks: List<ScheduledTask>): List<String> {
        delay(200) // Fake transaction
        val ids = mutableListOf<String>()
        val current = _items.value.toMutableList()
        for (task in tasks) {
            val newId = task.id.ifBlank { (System.currentTimeMillis() % 100000 + ids.size).toString() }
            current.add(task.copy(id = newId))
            ids.add(newId)
        }
        _items.value = current
        return ids
    }

    override suspend fun rescheduleTask(oldTaskId: String, newTask: ScheduledTask) {
        delay(200) // Fake transaction
        val current = _items.value.toMutableList()
        val oldIndex = current.indexOfFirst { it.id == oldTaskId }
        if (oldIndex >= 0) {
            current.removeAt(oldIndex)
            // Forcefully inherit GUID as per Path A contract
            current.add(newTask.copy(id = oldTaskId))
            _items.value = current
        }
    }

    override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> {
        return _items.value
            .filterIsInstance<ScheduledTask>()
            .filter { it.isDone }
            .take(limit)
    }

    override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? {
        return _items.value
            .filterIsInstance<ScheduledTask>()
            .filter { 
                it.keyPersonEntityId == entityId && !it.isDone &&
                (it.urgencyLevel == UrgencyLevel.L1_CRITICAL || it.urgencyLevel == UrgencyLevel.L2_IMPORTANT)
            }
            .sortedWith(compareBy<ScheduledTask> { it.urgencyLevel.ordinal }.thenBy { it.startTime })
            .firstOrNull()
    }

    override fun observeByEntityId(entityId: String): Flow<List<ScheduledTask>> {
        return _items.map { items ->
            items.filterIsInstance<ScheduledTask>().filter { it.keyPersonEntityId == entityId }
        }
    }
}
