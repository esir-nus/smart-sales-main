package com.smartsales.prism.data.scheduler

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日历仓库 — 封装 CalendarContract 操作
 * 
 * 使用系统日历作为单一数据源
 */
@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver
        get() = context.contentResolver

    /**
     * 查询指定日期范围内的事件
     */
    fun queryEvents(start: LocalDate, end: LocalDate): Flow<List<CalendarEvent>> = flow {
        val events = withContext(Dispatchers.IO) {
            val startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.DESCRIPTION
            )

            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} < ?"
            val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())

            val cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            val result = mutableListOf<CalendarEvent>()
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val title = it.getString(1) ?: ""
                    val dtStart = it.getLong(2)
                    val dtEnd = it.getLong(3)
                    val location = it.getString(4)
                    val description = it.getString(5)

                    result.add(
                        CalendarEvent(
                            id = id.toString(),
                            title = title,
                            startTime = Instant.ofEpochMilli(dtStart),
                            endTime = Instant.ofEpochMilli(dtEnd),
                            location = location,
                            notes = description
                        )
                    )
                }
            }
            result
        }
        emit(events)
    }

    /**
     * 插入新事件
     * @return 新事件的ID
     */
    suspend fun insertEvent(event: CalendarEvent): String = withContext(Dispatchers.IO) {
        val calendarId = getPrimaryCalendarId() ?: throw IllegalStateException("无法找到可用日历")

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DTSTART, event.startTime.toEpochMilli())
            put(CalendarContract.Events.DTEND, event.endTime.toEpochMilli())
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            event.location?.let { put(CalendarContract.Events.EVENT_LOCATION, it) }
            event.notes?.let { put(CalendarContract.Events.DESCRIPTION, it) }
        }

        val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        ContentUris.parseId(uri!!).toString()
    }

    /**
     * 获取单个事件
     */
    suspend fun getEvent(eventId: String): CalendarEvent? = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION
        )

        val eventUri = ContentUris.withAppendedId(
            CalendarContract.Events.CONTENT_URI,
            eventId.toLong()
        )

        contentResolver.query(eventUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return@withContext CalendarEvent(
                    id = cursor.getLong(0).toString(),
                    title = cursor.getString(1) ?: "",
                    startTime = Instant.ofEpochMilli(cursor.getLong(2)),
                    endTime = Instant.ofEpochMilli(cursor.getLong(3)),
                    location = cursor.getString(4),
                    notes = cursor.getString(5)
                )
            }
        }
        null
    }

    /**
     * 更新事件
     */
    suspend fun updateEvent(event: CalendarEvent) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DTSTART, event.startTime.toEpochMilli())
            put(CalendarContract.Events.DTEND, event.endTime.toEpochMilli())
            event.location?.let { put(CalendarContract.Events.EVENT_LOCATION, it) }
            event.notes?.let { put(CalendarContract.Events.DESCRIPTION, it) }
        }

        val updateUri = ContentUris.withAppendedId(
            CalendarContract.Events.CONTENT_URI,
            event.id.toLong()
        )
        contentResolver.update(updateUri, values, null, null)
    }

    /**
     * 删除事件
     */
    suspend fun deleteEvent(eventId: String) = withContext(Dispatchers.IO) {
        val deleteUri = ContentUris.withAppendedId(
            CalendarContract.Events.CONTENT_URI,
            eventId.toLong()
        )
        contentResolver.delete(deleteUri, null, null)
    }

    /**
     * 获取主要日历ID (用户的第一个可写日历)
     */
    private fun getPrimaryCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

        contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }
}

/**
 * 日历事件数据类
 */
data class CalendarEvent(
    val id: String = "",
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val location: String? = null,
    val notes: String? = null
)
