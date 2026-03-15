package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeInspirationRepository : InspirationRepository {
    private val items = mutableListOf<SchedulerTimelineItem.Inspiration>()
    private val itemsFlow = MutableStateFlow<List<SchedulerTimelineItem.Inspiration>>(emptyList())
    
    override suspend fun insert(text: String): String {
        val id = "insp-${items.size + 1}"
        items.add(SchedulerTimelineItem.Inspiration(id, "Just now", text))
        itemsFlow.value = items.toList()
        return id
    }
    
    override fun getAll(): Flow<List<SchedulerTimelineItem.Inspiration>> {
        return itemsFlow
    }
    
    override suspend fun delete(id: String) {
        items.removeAll { it.id == id }
        itemsFlow.value = items.toList()
    }
}
