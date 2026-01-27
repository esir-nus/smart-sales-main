package com.smartsales.domain.prism.core.entities

/**
 * 日程任务
 * @see Prism-V1.md §5.7 Scheduler Payload
 */
data class ScheduledTask(
    val id: String,
    val title: String,
    val description: String? = null,
    val scheduledAt: Long,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val status: TaskStatus = TaskStatus.PENDING,
    val hasAlarm: Boolean = false,
    val alarmType: AlarmType = AlarmType.MANUAL,
    val relatedEntityIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class TaskPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

enum class TaskStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

enum class AlarmType {
    MANUAL,     // 用户手动设置
    SMART       // AI 智能提醒
}
