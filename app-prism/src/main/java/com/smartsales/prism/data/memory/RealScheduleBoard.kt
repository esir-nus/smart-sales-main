package com.smartsales.prism.data.memory

import com.smartsales.prism.domain.memory.*
import com.smartsales.prism.domain.scheduler.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日程看板实现 — 封装 ScheduledTaskRepository 提供快速冲突检测
 * 
 * 使用硬编码 Kotlin 逻辑，无需 LLM。
 */
@Singleton
class RealScheduleBoard @Inject constructor(
    private val taskRepository: ScheduledTaskRepository
) : ScheduleBoard {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _upcomingItems = MutableStateFlow<List<ScheduleItem>>(emptyList())
    override val upcomingItems: StateFlow<List<ScheduleItem>> = _upcomingItems.asStateFlow()
    
    init {
        // 订阅任务变化，投射到 ScheduleItem
        scope.launch {
            // 查询未来7天的任务
            val today = LocalDate.now()
            val endDate = today.plusDays(7)
            
            taskRepository.queryByDateRange(today, endDate).collect { items ->
                _upcomingItems.value = items
                    .filterIsInstance<TimelineItemModel.Task>()
                    .map { it.toScheduleItem() }
            }
        }
    }
    
    /**
     * 冲突检测 — 硬编码 Kotlin 逻辑
     * 
     * 时间重叠判断: s1 < e2 && s2 < e1
     */
    override suspend fun checkConflict(
        proposedStart: Long,
        durationMinutes: Int
    ): ConflictResult {
        val proposedEnd = proposedStart + (durationMinutes * 60_000L)
        
        val overlaps = _upcomingItems.value.filter { slot ->
            // 只检查 EXCLUSIVE 策略的项目
            slot.conflictPolicy == ConflictPolicy.EXCLUSIVE &&
            // 时间重叠判断
            slot.scheduledAt < proposedEnd && proposedStart < slot.endAt
        }
        
        return if (overlaps.isEmpty()) {
            ConflictResult.Clear
        } else {
            ConflictResult.Conflict(overlaps)
        }
    }
    
    override suspend fun refresh() {
        // 触发重新收集
        val today = LocalDate.now()
        val endDate = today.plusDays(7)
        
        taskRepository.queryByDateRange(today, endDate).collect { items ->
            _upcomingItems.value = items
                .filterIsInstance<TimelineItemModel.Task>()
                .map { it.toScheduleItem() }
        }
    }
    
    /**
     * Task -> ScheduleItem 转换
     */
    private fun TimelineItemModel.Task.toScheduleItem(): ScheduleItem {
        return ScheduleItem(
            entryId = id,
            title = title,
            scheduledAt = startTime.toEpochMilli(),
            durationMinutes = durationMinutes,
            durationSource = durationSource,
            conflictPolicy = conflictPolicy,
            participants = keyPerson?.let { listOf(it) } ?: emptyList(),
            location = location
        )
    }
}
