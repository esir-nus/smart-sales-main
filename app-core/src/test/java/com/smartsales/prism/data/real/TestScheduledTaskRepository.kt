package com.smartsales.prism.data.real

import com.smartsales.core.context.RealContextBuilder
import com.smartsales.core.pipeline.RealUnifiedPipeline

import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

/** Shared Fake for Component Testing */
class TestScheduledTaskRepository : ScheduledTaskRepository {
    val items = MutableStateFlow<List<SchedulerTimelineItem>>(emptyList())

    override fun queryByDateRange(start: LocalDate, end: LocalDate) = items
    override fun getTimelineItems(dayOffset: Int) = items
    
    override suspend fun insertTask(task: ScheduledTask): String = "id"
    override suspend fun getTask(id: String): ScheduledTask? = null
    override suspend fun updateTask(task: ScheduledTask) {}
    override suspend fun upsertTask(task: ScheduledTask): String = "id"
            override suspend fun deleteItem(id: String) {}
    override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> = emptyList()
    override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? = null
    override fun observeByEntityId(entityId: String): kotlinx.coroutines.flow.Flow<List<ScheduledTask>> = kotlinx.coroutines.flow.emptyFlow()
}
