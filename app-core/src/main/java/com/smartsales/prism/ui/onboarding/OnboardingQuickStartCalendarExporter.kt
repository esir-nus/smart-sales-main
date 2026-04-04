package com.smartsales.prism.ui.onboarding

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.time.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * onboarding quick start 系统日历镜像导出器。
 */
interface OnboardingQuickStartCalendarExporter {
    suspend fun exportCommittedTaskIds(taskIds: List<String>): Boolean
}

@Singleton
class RealOnboardingQuickStartCalendarExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: ScheduledTaskRepository,
    private val timeProvider: TimeProvider
) : OnboardingQuickStartCalendarExporter {

    override suspend fun exportCommittedTaskIds(taskIds: List<String>): Boolean {
        if (taskIds.isEmpty()) return true
        val calendarId = findWritableCalendarId() ?: return false
        var exportedAny = false
        taskIds.forEach { taskId ->
            val task = taskRepository.getTask(taskId) ?: return@forEach
            if (task.isVague || task.isDone) return@forEach
            if (hasExistingMirror(taskId)) {
                exportedAny = true
                return@forEach
            }
            val startMillis = task.startTime.toEpochMilli()
            val endMillis = startMillis + maxOf(task.durationMinutes, 1) * 60_000L
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, task.title)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, timeProvider.zoneId.id)
                put(CalendarContract.Events.DESCRIPTION, buildMirrorDescription(task.id, task.notes))
            }
            val inserted = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (inserted != null) {
                exportedAny = true
            }
        }
        return exportedAny
    }

    private fun findWritableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val selection = "${CalendarContract.Calendars.VISIBLE}=1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL}>=${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR}"
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            null,
            "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars._ID} ASC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    private fun hasExistingMirror(taskId: String): Boolean {
        val projection = arrayOf(CalendarContract.Events._ID)
        val selection = "${CalendarContract.Events.DESCRIPTION} LIKE ?"
        val args = arrayOf("%${mirrorMarker(taskId)}%")
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            args,
            null
        )?.use { cursor ->
            return cursor.moveToFirst()
        }
        return false
    }

    private fun buildMirrorDescription(taskId: String, notes: String?): String {
        val marker = mirrorMarker(taskId)
        return listOfNotNull(marker, notes?.takeIf { it.isNotBlank() })
            .joinToString("\n")
    }

    private fun mirrorMarker(taskId: String): String = "smartsales:onboarding_task_id=$taskId"
}
