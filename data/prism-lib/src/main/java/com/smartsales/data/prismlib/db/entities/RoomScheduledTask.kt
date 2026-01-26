package com.smartsales.data.prismlib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsales.domain.prism.core.entities.AlarmType
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
    val hasAlarm: Boolean,
    val alarmType: AlarmType,
    val relatedEntityIds: List<String>,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): ScheduledTask = ScheduledTask(
        id = id,
        title = title,
        description = description,
        scheduledAt = scheduledAt,
        priority = priority,
        status = status,
        hasAlarm = hasAlarm,
        alarmType = alarmType,
        relatedEntityIds = relatedEntityIds,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: ScheduledTask): RoomScheduledTask = RoomScheduledTask(
            id = domain.id,
            title = domain.title,
            description = domain.description,
            scheduledAt = domain.scheduledAt,
            priority = domain.priority,
            status = domain.status,
            hasAlarm = domain.hasAlarm,
            alarmType = domain.alarmType,
            relatedEntityIds = domain.relatedEntityIds,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
