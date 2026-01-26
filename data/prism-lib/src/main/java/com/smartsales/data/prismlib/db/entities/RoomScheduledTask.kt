package com.smartsales.data.prismlib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsales.domain.prism.core.entities.ReminderConfig
import com.smartsales.domain.prism.core.entities.ScheduledTask
import com.smartsales.domain.prism.core.entities.TaskPriority
import com.smartsales.domain.prism.core.entities.TaskStatus

@Entity(tableName = "scheduled_tasks")
data class RoomScheduledTask(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val scheduledAt: Long,
    val priority: TaskPriority,
    val status: TaskStatus,
    val isAllDay: Boolean,
    val reminderConfig: ReminderConfig? // Requires TypeConverter
) {
    fun toDomain(): ScheduledTask = ScheduledTask(
        id = id,
        title = title,
        description = description,
        scheduledAt = scheduledAt,
        priority = priority,
        status = status,
        isAllDay = isAllDay,
        reminderConfig = reminderConfig
    )

    companion object {
        fun fromDomain(domain: ScheduledTask): RoomScheduledTask = RoomScheduledTask(
            id = domain.id,
            title = domain.title,
            description = domain.description,
            scheduledAt = domain.scheduledAt,
            priority = domain.priority,
            status = domain.status,
            isAllDay = domain.isAllDay,
            reminderConfig = domain.reminderConfig
        )
    }
}
