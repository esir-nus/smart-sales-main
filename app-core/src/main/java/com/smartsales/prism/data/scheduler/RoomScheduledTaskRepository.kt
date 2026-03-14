package com.smartsales.prism.data.scheduler

import com.smartsales.prism.data.persistence.ScheduledTaskDao
import com.smartsales.prism.data.persistence.toDomain
import com.smartsales.prism.data.persistence.toEntity
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room 任务仓库 — 使用 Room 数据库作为后端
 */
@Singleton
class RoomScheduledTaskRepository @Inject constructor(
    private val dao: ScheduledTaskDao,
    private val timeProvider: TimeProvider
) : ScheduledTaskRepository {

    override fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>> {
        val targetDate = timeProvider.today.plusDays(dayOffset.toLong())
        return queryByDateRange(targetDate, targetDate)
    }

    override fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<SchedulerTimelineItem>> {
        val startMillis = start.atStartOfDay(timeProvider.zoneId).toInstant().toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay(timeProvider.zoneId).toInstant().toEpochMilli()
        
        return dao.getByDateRange(startMillis, endMillis).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertTask(task: ScheduledTask): String {
        val entity = task.copy(id = UUID.randomUUID().toString()).toEntity()
        dao.insert(entity)
        return entity.taskId
    }

    override suspend fun getTask(id: String): ScheduledTask? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun updateTask(task: ScheduledTask) {
        dao.update(task.toEntity())
    }

    override suspend fun deleteItem(id: String) {
        dao.deleteById(id)
    }

    override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> {
        val sevenDaysAgo = timeProvider.today.minusDays(7)
            .atStartOfDay(timeProvider.zoneId).toInstant().toEpochMilli()
        return dao.getRecentCompleted(sevenDaysAgo, limit).map { it.toDomain() }
    }

    override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? {
        return dao.getTopUrgentActiveTask(entityId)?.toDomain()
    }

    override fun observeByEntityId(entityId: String): Flow<List<ScheduledTask>> {
        return dao.observeByEntityId(entityId).map { entities -> entities.map { it.toDomain() } }
    }
}
