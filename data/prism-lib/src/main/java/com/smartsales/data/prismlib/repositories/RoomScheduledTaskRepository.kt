package com.smartsales.data.prismlib.repositories

import com.smartsales.data.prismlib.db.dao.ScheduledTaskDao
import com.smartsales.data.prismlib.db.entities.RoomScheduledTask
import com.smartsales.domain.prism.core.entities.ScheduledTask
import com.smartsales.domain.prism.core.repositories.ScheduledTaskRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomScheduledTaskRepository @Inject constructor(
    private val dao: ScheduledTaskDao
) : ScheduledTaskRepository {

    override suspend fun insert(task: ScheduledTask) {
        dao.insert(RoomScheduledTask.fromDomain(task))
    }

    override suspend fun getById(id: String): ScheduledTask? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getUpcoming(limit: Int): List<ScheduledTask> {
        return dao.getUpcoming(System.currentTimeMillis(), limit).map { it.toDomain() }
    }

    override suspend fun getForDateRange(start: Long, end: Long): List<ScheduledTask> {
        return dao.getForDateRange(start, end).map { it.toDomain() }
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}
