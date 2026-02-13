package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.memory.DurationSource
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * Scheduler Repository — Tasks, Inspirations, Conflicts
 * @see prism-ui-ux-contract.md §1.3
 */
interface ScheduledTaskRepository {
    /**
     * 获取指定日期偏移的时间线项目 (0 = 今天)
     */
    fun getTimelineItems(dayOffset: Int): Flow<List<TimelineItemModel>>

    /**
     * 按日期范围查询任务
     */
    fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<TimelineItemModel>>

    /**
     * 插入新任务
     */
    suspend fun insertTask(task: TimelineItemModel.Task): String

    /**
     * 获取单个任务
     */
    suspend fun getTask(id: String): TimelineItemModel.Task?

    /**
     * 更新任务
     */
    suspend fun updateTask(task: TimelineItemModel.Task)

    /**
     * 删除时间线项目
     */
    suspend fun deleteItem(id: String)

    /**
     * 最近已完成的任务（hero dashboard 用）
     */
    suspend fun getRecentCompleted(limit: Int): List<TimelineItemModel.Task>
}


/**
 * Domain Model for Timeline Items (Mirrors UI State but cleaner)
 * In a full Clean Arch, we might map Domain -> UI, but for now we share the sealed class 
 * or duplicate if UI needs specific state.
 * 
 * Given the project uses 'TimelineItem' in UI heavily, let's define a Domain version 
 * to keep layers clean, or import the UI state if 'TimelineItem' is actually a Domain entity.
 * 
 * Checking existing AudioRepository: it uses 'AudioFile' (Domain) which maps to 'AudioItemState' (UI).
 * So we should define domain models here.
 */
sealed class TimelineItemModel {
    abstract val id: String
    abstract val timeDisplay: String

    data class Task(
        override val id: String,
        override val timeDisplay: String,
        val title: String,
        val urgencyLevel: UrgencyLevel = UrgencyLevel.L3_NORMAL, // Wave 4.2: LLM Urgency
        val isDone: Boolean = false,
        val hasAlarm: Boolean = false,
        val isSmartAlarm: Boolean = false, // "智能提醒"
        val startTime: Instant, // Raw data for persistence
        val endTime: Instant? = null, // Raw data for persistence
        val durationMinutes: Int = 0, // 持续时间 (fire-off 无时长)
        val durationSource: DurationSource = DurationSource.DEFAULT, // 持续时间来源
        val conflictPolicy: ConflictPolicy = ConflictPolicy.EXCLUSIVE, // 冲突策略
        val dateRange: String = "",
        val location: String? = null,
        val notes: String? = null,
        val keyPerson: String? = null,
        val keyPersonEntityId: String? = null,  // Wave 9: Entity ID for tip generation
        val highlights: String? = null,
        val tips: List<String> = emptyList(),   // Wave 9: LLM-generated context tips
        val tipsLoading: Boolean = false,       // Wave 9: Generation animation state
        val alarmCascade: List<String> = emptyList() // e.g. ["-1h", "-15m", "-5m"]
    ) : TimelineItemModel()

    data class Inspiration(
        override val id: String,
        override val timeDisplay: String,
        val title: String
    ) : TimelineItemModel()

    data class Conflict(
        override val id: String,
        override val timeDisplay: String,
        val conflictText: String,
        val taskA: com.smartsales.prism.domain.memory.ScheduleItem,
        val taskB: com.smartsales.prism.domain.memory.ScheduleItem
    ) : TimelineItemModel()
}

