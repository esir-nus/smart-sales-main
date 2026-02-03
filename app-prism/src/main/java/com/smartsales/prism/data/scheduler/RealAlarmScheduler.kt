package com.smartsales.prism.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.ReminderType
import com.smartsales.prism.domain.scheduler.TaskTypeHint
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实闹钟调度器 — 使用 AlarmManager
 */
@Singleton
class RealAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) : AlarmScheduler {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun scheduleReminder(taskId: String, triggerAt: Instant, type: ReminderType) {
        val pendingIntent = createPendingIntent(taskId)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt.toEpochMilli(),
                    pendingIntent
                )
            } else {
                // 回退到非精确闹钟
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt.toEpochMilli(),
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt.toEpochMilli(),
                pendingIntent
            )
        }
    }

    override suspend fun cancelReminder(taskId: String) {
        val pendingIntent = createPendingIntent(taskId)
        alarmManager.cancel(pendingIntent)
    }

    override suspend fun scheduleSmartCascade(taskId: String, eventTime: Instant, taskType: TaskTypeHint) {
        val reminderOffset = when (taskType) {
            TaskTypeHint.MEETING -> 30L  // 会议提前30分钟
            TaskTypeHint.CALL -> 15L     // 电话提前15分钟
            TaskTypeHint.PERSONAL -> 10L // 个人提前10分钟
            TaskTypeHint.URGENT -> 5L    // 紧急提前5分钟
        }

        val triggerAt = eventTime.minus(reminderOffset, ChronoUnit.MINUTES)
        
        // 只有未来的提醒才设置
        if (triggerAt.isAfter(timeProvider.now)) {
            scheduleReminder(taskId, triggerAt, ReminderType.SMART_CASCADE)
        }
    }

    private fun createPendingIntent(taskId: String): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
        }
        
        val requestCode = taskId.hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    companion object {
        const val ACTION_TASK_REMINDER = "com.smartsales.prism.TASK_REMINDER"
        const val EXTRA_TASK_ID = "task_id"
    }
}
