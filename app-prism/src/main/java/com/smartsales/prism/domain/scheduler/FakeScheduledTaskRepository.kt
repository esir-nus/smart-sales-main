package com.smartsales.prism.domain.scheduler

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Fake 任务仓库 — 用于开发和测试
 * 注意: 生产环境使用 RealScheduledTaskRepository
 */
class FakeScheduledTaskRepository @Inject constructor() : ScheduledTaskRepository {

    // Initial Mock Data
    private val _items = MutableStateFlow(
        listOf(
            TimelineItemModel.Task(
                "1", "08:00", "与张总会议 (A3项目)",
                hasAlarm = true, dateRange = "08:00 - 09:00", location = "会议室 A",
                notes = "讨论Q4预算审核细节，确认最终报价范围。",
                startTime = java.time.Instant.now(), // Fake
                endTime = java.time.Instant.now().plusSeconds(3600)
            ),
            TimelineItemModel.Inspiration("2", "10:30", "研究竞品报价策略"),
            TimelineItemModel.Conflict("3", "12:00", "李总电话 vs 午餐会议"),
            TimelineItemModel.Task(
                "4", "14:00", "提交季度报告 (已完成)",
                isDone = true, dateRange = "14:00 - 15:30", location = "工位",
                notes = "已通过邮件发送给运营总监。",
                startTime = java.time.Instant.now(),
                endTime = java.time.Instant.now().plusSeconds(5400)
            ),
            TimelineItemModel.Task(
                "5", "16:00", "跟进上周客户反馈",
                dateRange = "16:00 - 17:00", location = "远程",
                notes = "将此任务改期至上周三。",
                startTime = java.time.Instant.now(),
                endTime = java.time.Instant.now().plusSeconds(3600)
            )
        )
    )

    override fun getTimelineItems(dayOffset: Int): Flow<List<TimelineItemModel>> {
        return _items
    }

    override fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<TimelineItemModel>> {
        // Fake: 返回所有项目，不按日期过滤
        return _items
    }

    override suspend fun insertTask(task: TimelineItemModel.Task): String {
        delay(200) // Fake write
        val newId = (System.currentTimeMillis() % 100000).toString()
        val newTask = task.copy(id = newId)
        val current = _items.value.toMutableList()
        current.add(newTask)
        _items.value = current
        return newId
    }

    override suspend fun updateTask(task: TimelineItemModel.Task) {
        delay(200) // Fake write
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            current[index] = task
            _items.value = current
        }
    }

    override suspend fun deleteItem(id: String) {
        delay(300) // Fake network
        val current = _items.value.toMutableList()
        current.removeAll { it.id == id }
        _items.value = current
    }
}
