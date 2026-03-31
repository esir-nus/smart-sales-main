package com.smartsales.prism.data.persistence

import com.smartsales.prism.domain.core.safeEnumValueOf
import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.memory.DurationSource
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.scheduler.withNormalizedReminderMetadata
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.habit.UserHabit
import com.smartsales.prism.domain.model.SessionPreview
import org.json.JSONArray
import java.time.Instant

// === ScheduledTaskEntity Mappers ===

fun ScheduledTask.toEntity(): ScheduledTaskEntity {
    val normalizedTask = withNormalizedReminderMetadata()
    return ScheduledTaskEntity(
        taskId = normalizedTask.id,
        title = normalizedTask.title,
        startTimeMillis = normalizedTask.startTime.toEpochMilli(),
        endTimeMillis = normalizedTask.endTime?.toEpochMilli(),
        durationMinutes = normalizedTask.durationMinutes,
        durationSource = normalizedTask.durationSource.name,
        conflictPolicy = normalizedTask.conflictPolicy.name,
        location = normalizedTask.location,
        notes = normalizedTask.notes,
        keyPerson = normalizedTask.keyPerson,
        keyPersonEntityId = normalizedTask.keyPersonEntityId,
        highlights = normalizedTask.highlights,
        isDone = normalizedTask.isDone,
        hasAlarm = normalizedTask.hasAlarm,
        isSmartAlarm = normalizedTask.isSmartAlarm,
        alarmCascadeJson = if (normalizedTask.alarmCascade.isEmpty()) null else JSONArray(normalizedTask.alarmCascade).toString(),
        urgencyLevel = normalizedTask.urgencyLevel.name,
        hasConflict = normalizedTask.hasConflict,
        conflictWithTaskId = normalizedTask.conflictWithTaskId,
        conflictSummary = normalizedTask.conflictSummary,
        isVague = normalizedTask.isVague
    )
}

fun ScheduledTaskEntity.toDomain(): ScheduledTask {
    val startInstant = Instant.ofEpochMilli(startTimeMillis)
    val endInstant = endTimeMillis?.let { Instant.ofEpochMilli(it) }
    val zone = java.time.ZoneId.systemDefault()
    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    // 重建 timeDisplay（左侧时间标签）
    val startZoned = startInstant.atZone(zone)
    val endZoned = endInstant?.atZone(zone)
    val timeDisplay = if (isVague) {
        "待定"
    } else if (endZoned != null) {
        "${startZoned.format(timeFormatter)} - ${endZoned.format(timeFormatter)}"
    } else {
        "${startZoned.format(timeFormatter)} - ..."
    }
    
    // 重建 dateRange（展开卡片的 📅 行）
    val dateRange = when {
        isVague -> "${startZoned.format(dateFormatter)} · 时间待定"
        endZoned == null -> "${startZoned.format(dateFormatter)} ${startZoned.format(timeFormatter)} - ..."
        startZoned.toLocalDate() == endZoned.toLocalDate() -> "${startZoned.format(timeFormatter)} - ${endZoned.format(timeFormatter)}"
        else -> "${startZoned.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))} ~ ${endZoned.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}"
    }
    
    // Parse alarmCascade
    val cascade = alarmCascadeJson?.let { json ->
        val array = JSONArray(json)
        List(array.length()) { i -> array.getString(i) }
    } ?: emptyList()

    // If the DB has the legacy 'ESTIMATED' string, fallback handles it
    val durationSourceEnum = safeEnumValueOf(durationSource, fallback = DurationSource.DEFAULT).let {
        if (durationSource == "ESTIMATED") DurationSource.INFERRED else it
    }
    
    return ScheduledTask(
        id = taskId,
        timeDisplay = timeDisplay,
        title = title,
        startTime = startInstant,
        endTime = endInstant,
        durationMinutes = durationMinutes,
        durationSource = durationSourceEnum,
        conflictPolicy = safeEnumValueOf(conflictPolicy, fallback = ConflictPolicy.EXCLUSIVE),
        location = location,
        notes = notes,
        keyPerson = keyPerson,
        keyPersonEntityId = keyPersonEntityId,
        highlights = highlights,
        isDone = isDone,
        hasAlarm = hasAlarm,
        isSmartAlarm = isSmartAlarm,
        alarmCascade = cascade,
        urgencyLevel = safeEnumValueOf(urgencyLevel, fallback = UrgencyLevel.L3_NORMAL),
        dateRange = dateRange,
        hasConflict = hasConflict,
        conflictWithTaskId = conflictWithTaskId,
        conflictSummary = conflictSummary,
        isVague = isVague
    ).withNormalizedReminderMetadata()
}
