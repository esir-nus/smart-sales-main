package com.smartsales.prism.data.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import java.time.Instant

/**
 * Room 实体 — 调度任务存储表
 * 
 * 索引策略:
 * - startTimeMillis: 按日期范围查询
 */
@Entity(
    tableName = "scheduled_tasks",
    indices = [
        Index(value = ["startTimeMillis"])
    ]
)
data class ScheduledTaskEntity(
    @PrimaryKey
    val taskId: String,
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val durationMinutes: Int,
    val durationSource: String,  // Enum stored as String
    val conflictPolicy: String,  // Enum stored as String
    val location: String?,
    val notes: String?,
    val keyPerson: String?,
    val keyPersonEntityId: String?,  // Wave 9: Entity ID
    val highlights: String?,
    val isDone: Boolean,
    val hasAlarm: Boolean,
    val isSmartAlarm: Boolean,
    val alarmCascadeJson: String?,  // JSON serialized List<String>
    val urgencyLevel: String = "L3_NORMAL",  // UrgencyLevel enum stored as String
    val hasConflict: Boolean = false,
    val isVague: Boolean = false
)
