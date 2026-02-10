package com.smartsales.prism.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.ReminderType
import com.smartsales.prism.domain.scheduler.TaskTypeHint
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实闹钟调度器 — 使用 AlarmManager 实现级联提醒
 * 
 * 级联模式: 在事件前 -60min, -15min, -5min 触发三次提醒
 * 单次模式: 在事件前 -15min 触发一次提醒
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
        
        // 级联提醒偏移量 (分钟) — 1min 是每个任务的最终提醒
        private val CASCADE_OFFSETS = listOf(60, 15, 5, 1)  // 1h, 15m, 5m, 1m before
        private val SINGLE_OFFSET = listOf(15, 1)            // 15m + 1m before
    }

    override suspend fun scheduleReminder(taskId: String, taskTitle: String, triggerAt: Instant, type: ReminderType) {
        val offsets = when (type) {
            ReminderType.SMART_CASCADE -> CASCADE_OFFSETS
            ReminderType.SINGLE -> SINGLE_OFFSET
        }
        
        offsets.forEach { offsetMinutes ->
            val alarmTime = triggerAt.minusSeconds(offsetMinutes * 60L)
            if (alarmTime.isAfter(timeProvider.now)) {
                scheduleExactAlarm(taskId, taskTitle, offsetMinutes, alarmTime)
                Log.d(TAG, "已设置提醒: taskId=$taskId, title=$taskTitle, offset=-${offsetMinutes}min, at=$alarmTime")
            } else {
                Log.d(TAG, "跳过过期提醒: taskId=$taskId, offset=-${offsetMinutes}min")
            }
        }
    }

    override suspend fun cancelReminder(taskId: String) {
        // 取消所有可能的级联提醒
        (CASCADE_OFFSETS + SINGLE_OFFSET).distinct().forEach { offsetMinutes ->
            // title 对取消无影响，PendingIntent 匹配基于 requestCode
            val intent = createCascadePendingIntent(taskId, "", offsetMinutes)
            alarmManager.cancel(intent)
        }
        Log.d(TAG, "已取消提醒: taskId=$taskId")
    }

    override suspend fun scheduleSmartCascade(taskId: String, taskTitle: String, eventTime: Instant, taskType: TaskTypeHint) {
        val reminderType = when (taskType) {
            TaskTypeHint.MEETING -> ReminderType.SMART_CASCADE  // 会议用级联
            TaskTypeHint.CALL -> ReminderType.SINGLE            // 电话用单次
            TaskTypeHint.PERSONAL -> ReminderType.SINGLE        // 个人用单次
            TaskTypeHint.URGENT -> ReminderType.SMART_CASCADE   // 紧急用级联
        }
        scheduleReminder(taskId, taskTitle, eventTime, reminderType)
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
                // 回退到非精确闹钟
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
     * 
     * 关键: 每个偏移量使用不同的 requestCode，确保多个提醒不会互相覆盖
     */
    private fun createCascadePendingIntent(taskId: String, taskTitle: String, offsetMinutes: Int): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_OFFSET_MINUTES, offsetMinutes)
        }
        
        // 关键: 使用 taskId + offset 组合生成唯一 requestCode
        val requestCode = "$taskId-$offsetMinutes".hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
