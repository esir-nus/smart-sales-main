package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeInspirationRepository : InspirationRepository {
    private val items = mutableListOf<TimelineItemModel.Inspiration>()
    private val itemsFlow = MutableStateFlow<List<TimelineItemModel.Inspiration>>(emptyList())
    
    override suspend fun insert(text: String): String {
        val id = "insp-${items.size + 1}"
        items.add(TimelineItemModel.Inspiration(id, "Just now", text))
        itemsFlow.value = items.toList()
        return id
    }
    
    override fun getAll(): Flow<List<TimelineItemModel.Inspiration>> {
        return itemsFlow
    }
    
    override suspend fun delete(id: String) {
        items.removeAll { it.id == id }
        itemsFlow.value = items.toList()
    }
}
