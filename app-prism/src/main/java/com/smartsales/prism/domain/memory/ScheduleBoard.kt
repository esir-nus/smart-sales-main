package com.smartsales.prism.domain.memory

import kotlinx.coroutines.flow.StateFlow

/**
 * 日程看板 — 冲突检测的快速索引
 * 
 * 专用于时间冲突检测，使用硬编码 Kotlin 逻辑，无需 LLM。
 * 这是 Memory Center 的第一个专用索引。
 * 
 * @see docs/cerb/memory-center/spec.md §ScheduleBoard
 */
interface ScheduleBoard {
    /**
     * 即将到来的日程项目 (响应式，始终最新)
     */
    val upcomingItems: StateFlow<List<ScheduleItem>>
    
    /**
     * 时间冲突检测 — 硬编码 Kotlin，无需 LLM，即时返回
     * 
     * @param proposedStart 提议的开始时间 (epoch millis)
     * @param durationMinutes 持续时间 (分钟)
     * @param excludeId 排除的任务ID (用于避免新创建的任务与自己冲突)
     * @return 冲突检测结果
     */
    suspend fun checkConflict(
        proposedStart: Long,
        durationMinutes: Int,
        excludeId: String? = null
    ): ConflictResult
    
    /**
     * 强制从 ScheduledTaskRepository 刷新
     */
    suspend fun refresh()
}

/**
 * 冲突检测结果
 */
sealed class ConflictResult {
    /** 无冲突 — 时间段空闲 */
    data object Clear : ConflictResult()
    
    /** 检测到冲突 — 返回重叠的日程项 */
    data class Conflict(val overlaps: List<ScheduleItem>) : ConflictResult()
}
