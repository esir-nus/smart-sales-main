package com.smartsales.domain.prism.core.payloads

import com.smartsales.domain.prism.core.entities.TaskPriority

/**
 * Scheduler 模式载荷 — 存储在 MemoryEntryEntity.payloadJson
 * @see Prism-V1.md §5.7
 */
data class SchedulerPayload(
    val scheduledAt: Long,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val status: SchedulerStatus = SchedulerStatus.PENDING,
    val reminder: ReminderConfig? = null
)

enum class SchedulerStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}

/**
 * 提醒配置
 */
data class ReminderConfig(
    val enabled: Boolean = true,
    val advanceMinutes: Int = 15,  // 提前多少分钟提醒
    val reminderType: ReminderType = ReminderType.SMART
)

enum class ReminderType {
    MANUAL,     // 用户设置
    SMART       // AI 智能推荐
}
