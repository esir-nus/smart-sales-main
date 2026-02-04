package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 假灵感仓库 — 内存实现，用于测试和预览
 */
@Singleton
class FakeInspirationRepository @Inject constructor() : InspirationRepository {
    
    private val _items = MutableStateFlow<List<TimelineItemModel.Inspiration>>(emptyList())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    
    override suspend fun insert(text: String): String {
        val id = UUID.randomUUID().toString()
        val inspiration = TimelineItemModel.Inspiration(
            id = id,
            timeDisplay = timeFormatter.format(Instant.now()),
            title = text
        )
        _items.value = listOf(inspiration) + _items.value
        return id
    }
    
    override fun getAll(): Flow<List<TimelineItemModel.Inspiration>> {
        return _items.asStateFlow()
    }
    
    override suspend fun delete(id: String) {
        _items.value = _items.value.filter { it.id != id }
    }
}
