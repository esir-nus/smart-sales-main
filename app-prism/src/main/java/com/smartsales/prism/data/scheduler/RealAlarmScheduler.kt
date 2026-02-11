package com.smartsales.prism.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实闹钟调度器 — 根据级联偏移量设置系统闹钟
 *
 * 接收 UrgencyLevel.buildCascade() 生成的偏移量列表，
 * 为每个偏移量设置一个精确闹钟。空列表 = 不设闹钟。
 */
@Singleton
class RealAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) : AlarmScheduler {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "RealAlarmScheduler"
        const val ACTION_TASK_REMINDER = "com.smartsales.prism.TASK_REMINDER"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_OFFSET_MINUTES = "offset_minutes"
        
        // 用于取消时枚举所有可能的偏移量（分钟）
        private val ALL_POSSIBLE_OFFSETS = listOf(120, 60, 30, 15, 5, 1, 0)
    }

    override suspend fun scheduleCascade(
        taskId: String,
        taskTitle: String,
        eventTime: Instant,
        cascade: List<String>
    ) {
        if (cascade.isEmpty()) {
            Log.d(TAG, "无级联提醒: taskId=$taskId")
            return
        }

        cascade.forEach { offset ->
            val offsetMs = UrgencyLevel.parseCascadeOffset(offset)
            val offsetMinutes = (offsetMs / 60_000).toInt()
            val alarmTime = eventTime.minusMillis(offsetMs)
            
            if (alarmTime.isAfter(timeProvider.now)) {
                scheduleExactAlarm(taskId, taskTitle, offsetMinutes, alarmTime)
                Log.d(TAG, "已设置提醒: taskId=$taskId, title=$taskTitle, offset=$offset, at=$alarmTime")
            } else {
                Log.d(TAG, "跳过过期提醒: taskId=$taskId, offset=$offset")
            }
        }
    }

    override suspend fun cancelReminder(taskId: String) {
        ALL_POSSIBLE_OFFSETS.forEach { offsetMinutes ->
            val intent = createCascadePendingIntent(taskId, "", offsetMinutes)
            alarmManager.cancel(intent)
        }
        Log.d(TAG, "已取消提醒: taskId=$taskId")
    }

    /**
     * 设置精确闹钟
     */
    private fun scheduleExactAlarm(taskId: String, taskTitle: String, offsetMinutes: Int, triggerAt: Instant) {
        val pendingIntent = createCascadePendingIntent(taskId, taskTitle, offsetMinutes)
        val triggerMillis = triggerAt.toEpochMilli()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
                Log.w(TAG, "精确闹钟权限未授予，使用非精确闹钟")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    /**
     * 创建级联提醒的 PendingIntent
     * 每个偏移量使用不同的 requestCode，确保多个提醒不会互相覆盖
     */
    private fun createCascadePendingIntent(taskId: String, taskTitle: String, offsetMinutes: Int): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_OFFSET_MINUTES, offsetMinutes)
        }
        
        val requestCode = "$taskId-$offsetMinutes".hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
