package com.smartsales.prism.data.real

import com.smartsales.core.context.RealContextBuilder
import com.smartsales.core.pipeline.RealUnifiedPipeline

import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

/** Shared Fake for Component Testing */
class TestScheduledTaskRepository : ScheduledTaskRepository {
    val items = MutableStateFlow<List<TimelineItemModel>>(emptyList())

    override fun queryByDateRange(start: LocalDate, end: LocalDate) = items
    override fun getTimelineItems(dayOffset: Int) = items
    
    override suspend fun insertTask(task: TimelineItemModel.Task): String = "id"
    override suspend fun getTask(id: String): TimelineItemModel.Task? = null
    override suspend fun updateTask(task: TimelineItemModel.Task) {}
    override suspend fun deleteItem(id: String) {}
    override suspend fun getRecentCompleted(limit: Int): List<TimelineItemModel.Task> = emptyList()
    override suspend fun getTopUrgentActiveForEntity(entityId: String): TimelineItemModel.Task? = null
}
