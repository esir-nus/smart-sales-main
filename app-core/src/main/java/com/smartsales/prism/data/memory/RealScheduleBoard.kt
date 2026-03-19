package com.smartsales.prism.data.memory

import com.smartsales.prism.domain.memory.*
import com.smartsales.prism.domain.scheduler.*
import android.util.Log
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
                    .filterIsInstance<ScheduledTask>()
                    .filter { !it.isDone }  // 只检测未完成任务
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
        durationMinutes: Int,
        excludeId: String?
    ): ConflictResult {
        val overlaps = _upcomingItems.value.filter { slot ->
            // 排除指定ID (避免新任务与自己冲突)
            slot.entryId != excludeId &&
            // 不检测模糊任务的冲突
            !slot.isVague &&
            // 只检查 EXCLUSIVE 策略的项目
            slot.conflictPolicy == ConflictPolicy.EXCLUSIVE &&
            // 精准任务即使没有显式时长，也必须参与点位占用冲突判断
            overlapsInScheduleBoard(
                proposedStart = proposedStart,
                proposedDurationMinutes = durationMinutes,
                existingStart = slot.scheduledAt,
                existingDurationMinutes = slot.durationMinutes
            )
        }
        
        return if (overlaps.isEmpty()) {
            Log.d("ScheduleBoard", "✅ checkConflict: CLEAR (proposed=${proposedStart}, dur=${durationMinutes}min)")
            ConflictResult.Clear
        } else {
            Log.d("ScheduleBoard", "⚠️ checkConflict: ${overlaps.size} conflicts (${overlaps.map { it.title }})")
            ConflictResult.Conflict(overlaps)
        }
    }
    
    override suspend fun refresh() {
        Log.d("ScheduleBoard", "🔄 refresh: triggered (no-op with Room persistence)")
        // Room sends updates automatically via the Flow collected in init {}
        // No need to manually re-query or block execution
    }
    
    override suspend fun findLexicalMatch(targetQuery: String): ScheduleItem? {
        val query = targetQuery.trim().lowercase()
        if (query.isEmpty()) return null

        val candidates = _upcomingItems.value
        val exactMatches = candidates.filter { it.title.lowercase().contains(query) }

        return if (exactMatches.size == 1) {
            exactMatches.first()
        } else {
            // Null indicates 0 or 2+ matches
            null
        }
    }
    
    /**
     * Task -> ScheduleItem 转换
     */
    private fun ScheduledTask.toScheduleItem(): ScheduleItem {
        return ScheduleItem(
            entryId = id,
            title = title,
            scheduledAt = startTime.toEpochMilli(),
            durationMinutes = durationMinutes,
            durationSource = durationSource,
            conflictPolicy = conflictPolicy,
            participants = keyPerson?.let { listOf(it) } ?: emptyList(),
            location = location,
            isVague = isVague
        )
    }
}
