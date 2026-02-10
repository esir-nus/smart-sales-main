package com.smartsales.prism.data.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.memory.DurationSource
import com.smartsales.prism.domain.scheduler.TimelineItemModel
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
    val alarmCascadeJson: String?  // JSON serialized List<String>
)

/**
 * 域模型映射
 */
fun TimelineItemModel.Task.toEntity(): ScheduledTaskEntity = ScheduledTaskEntity(
    taskId = id,
    title = title,
    startTimeMillis = startTime.toEpochMilli(),
    endTimeMillis = endTime?.toEpochMilli(),
    durationMinutes = durationMinutes,
    durationSource = durationSource.name,
    conflictPolicy = conflictPolicy.name,
    location = location,
    notes = notes,
    keyPerson = keyPerson,
    keyPersonEntityId = keyPersonEntityId,
    highlights = highlights,
    isDone = isDone,
    hasAlarm = hasAlarm,
    isSmartAlarm = isSmartAlarm,
    alarmCascadeJson = alarmCascade?.let { JSONArray(it).toString() }
)

fun ScheduledTaskEntity.toDomain(): TimelineItemModel.Task {
    val startInstant = Instant.ofEpochMilli(startTimeMillis)
    val endInstant = endTimeMillis?.let { Instant.ofEpochMilli(it) }
    val zone = java.time.ZoneId.systemDefault()
    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    // 重建 timeDisplay（左侧时间标签）
    val startZoned = startInstant.atZone(zone)
    val endZoned = endInstant?.atZone(zone)
    val timeDisplay = if (endZoned != null) {
        "${startZoned.format(timeFormatter)} - ${endZoned.format(timeFormatter)}"
    } else {
        "${startZoned.format(timeFormatter)} - ..."
    }
    
    // 重建 dateRange（展开卡片的 📅 行）
    val dateRange = when {
        endZoned == null -> "${startZoned.format(dateFormatter)} ${startZoned.format(timeFormatter)} - ..."
        startZoned.toLocalDate() == endZoned.toLocalDate() -> "${startZoned.format(timeFormatter)} - ${endZoned.format(timeFormatter)}"
        else -> "${startZoned.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))} ~ ${endZoned.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}"
    }
    
    // Parse alarmCascade
    val cascade = alarmCascadeJson?.let { json ->
        val array = JSONArray(json)
        List(array.length()) { i -> array.getString(i) }
    }
    
    return TimelineItemModel.Task(
        id = taskId,
        timeDisplay = timeDisplay,
        title = title,
        startTime = startInstant,
        endTime = endInstant,
        durationMinutes = durationMinutes,
        durationSource = DurationSource.valueOf(durationSource),
        conflictPolicy = ConflictPolicy.valueOf(conflictPolicy),
        location = location,
        notes = notes,
        keyPerson = keyPerson,
        keyPersonEntityId = keyPersonEntityId,
        highlights = highlights,
        isDone = isDone,
        hasAlarm = hasAlarm,
        isSmartAlarm = isSmartAlarm,
        alarmCascade = cascade,
        dateRange = dateRange
    )
}
