package com.smartsales.prism.data.scheduler

import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import com.smartsales.prism.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实任务仓库 — 使用系统日历作为后端
 */
@Singleton
class RealScheduledTaskRepository @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val timeProvider: TimeProvider
) : ScheduledTaskRepository {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun getTimelineItems(dayOffset: Int): Flow<List<TimelineItemModel>> {
        val targetDate = timeProvider.today.plusDays(dayOffset.toLong())
        return queryByDateRange(targetDate, targetDate)
    }

    override fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<TimelineItemModel>> {
        return calendarRepository.queryEvents(start, end).map { events ->
            events.map { event -> event.toTimelineItem() }
        }
    }

    override suspend fun insertTask(task: TimelineItemModel.Task): String {
        val event = task.toCalendarEvent()
        return calendarRepository.insertEvent(event)
    }

    override suspend fun updateTask(task: TimelineItemModel.Task) {
        val event = task.toCalendarEvent()
        calendarRepository.updateEvent(event)
    }

    override suspend fun deleteItem(id: String) {
        calendarRepository.deleteEvent(id)
    }

    // ========== Mapping Functions ==========

    private fun CalendarEvent.toTimelineItem(): TimelineItemModel.Task {
        val startLocal = startTime.atZone(timeProvider.zoneId).toLocalTime()
        val endLocal = endTime.atZone(timeProvider.zoneId).toLocalTime()
        
        return TimelineItemModel.Task(
            id = id,
            timeDisplay = "${startLocal.format(timeFormatter)} - ${endLocal.format(timeFormatter)}",
            title = title,
            isDone = false, // 日历事件不追踪完成状态，可扩展
            hasAlarm = true, // 假设所有日历事件都有提醒
            isSmartAlarm = false,
            startTime = startTime,
            endTime = endTime,
            dateRange = formatDateRange(startTime, endTime),
            location = location,
            notes = notes
        )
    }

    private fun TimelineItemModel.Task.toCalendarEvent(): CalendarEvent {
        // Use raw fields first, fallback to parsing if legacy/missing
        val (startTs, endTs) = if (true) { // Always use raw fields if available (they are non-nullable in definition now)
             Pair(startTime, endTime ?: startTime.plusSeconds(3600))
        } else {
             // Fallback logic kept just in case but unreachable with current Model definition
             parseDateRange(dateRange) ?: run {
                val now = timeProvider.now
                Pair(now, now.plusSeconds(3600))
            }
        }

        return CalendarEvent(
            id = id,
            title = title,
            startTime = startTs,
            endTime = endTs,
            location = location,
            notes = notes
        )
    }

    private fun formatDateRange(start: Instant, end: Instant): String {
        val dateParams = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeParams = DateTimeFormatter.ofPattern("HH:mm")

        val startZone = start.atZone(timeProvider.zoneId)
        val endZone = end.atZone(timeProvider.zoneId)

        return if (startZone.toLocalDate() == endZone.toLocalDate()) {
            // Same day: 03:00 - 04:00 (Spec aligned)
            "${startZone.format(timeParams)} - ${endZone.format(timeParams)}"
        } else {
            // Diff day: 2026-02-03 03:00 ~ 2026-02-04 09:00
            val fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            "${startZone.format(fullFormatter)} ~ ${endZone.format(fullFormatter)}"
        }
    }

    private fun parseDateRange(dateRange: String): Pair<Instant, Instant>? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            
            // 情况1: 完整日期范围 (使用 ~ 分隔)
            // 例: "2026-02-03 03:00 ~ 2026-02-03 05:00"
            if (dateRange.contains("~")) {
                val parts = dateRange.split("~").map { it.trim() }
                if (parts.size != 2) return null
                
                val start = java.time.LocalDateTime.parse(parts[0], formatter)
                    .atZone(timeProvider.zoneId).toInstant()
                val end = java.time.LocalDateTime.parse(parts[1], formatter)
                    .atZone(timeProvider.zoneId).toInstant()
                return Pair(start, end)
            }
            
            // 情况2: 开放式任务 (使用 - ... 结尾)
            // 例: "2026-02-03 03:00 - ..."
            if (dateRange.endsWith("- ...")) {
                val startStr = dateRange.removeSuffix("- ...").trim()
                val start = java.time.LocalDateTime.parse(startStr, formatter)
                    .atZone(timeProvider.zoneId).toInstant()
                // 开放式任务默认持续1小时 (日历需要结束时间)
                val end = start.plusSeconds(3600)
                return Pair(start, end)
            }
            
            // 情况3: 同一天范围 (使用 - 分隔时间)
            // 例: "2026-02-03 03:00 - 04:00"
            if (dateRange.contains(" - ")) {
                val parts = dateRange.split(" - ").map { it.trim() }
                if (parts.size == 2) {
                    val start = java.time.LocalDateTime.parse(parts[0], formatter)
                        .atZone(timeProvider.zoneId).toInstant()
                    // 结束时间只有 HH:mm，需要从开始时间提取日期
                    val startLocal = java.time.LocalDateTime.parse(parts[0], formatter)
                    val endTime = java.time.LocalTime.parse(parts[1], DateTimeFormatter.ofPattern("HH:mm"))
                    val end = startLocal.toLocalDate().atTime(endTime)
                        .atZone(timeProvider.zoneId).toInstant()
                    return Pair(start, end)
                }
            }
            
            null
        } catch (e: Exception) {
            android.util.Log.e("TaskRepo", "Failed to parse dateRange: $dateRange", e)
            null
        }
    }
}
