package com.smartsales.prism.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
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

        // HyperOS 后台通知权限诊断 — 仅日志，不打断用户
        if (com.smartsales.prism.data.notification.OemCompat.isXiaomi &&
            !com.smartsales.prism.data.notification.OemCompat.canSendBackgroundNotification(context)) {
            Log.w(TAG, "⚠️ 后台发送本地通知权限未授予 — 通知可能被 HyperOS 拦截")
        }

        cascade.forEach { offset ->
            val offsetMs = UrgencyLevel.parseCascadeOffset(offset)
            val offsetMinutes = (offsetMs / 60_000).toInt()
            val alarmTime = eventTime.minusMillis(offsetMs)
            
            if (alarmTime.isAfter(timeProvider.now)) {
                // 未来闹钟 → 正常调度
                scheduleExactAlarm(taskId, taskTitle, offsetMinutes, alarmTime)
                Log.d(TAG, "已设置提醒: taskId=$taskId, title=$taskTitle, offset=$offset, at=$alarmTime")
            } else {
                // 过期闹钟 — 判断是否刚过期（FIRE_OFF 的 0m 典型场景）
                val stalenessMs = java.time.Duration.between(alarmTime, timeProvider.now).toMillis()
                if (stalenessMs <= 60_000) {
                    // 60 秒内：直接发送广播，走同一条通知管线
                    fireImmediately(taskId, taskTitle, offsetMinutes)
                    Log.d(TAG, "立即触发提醒: taskId=$taskId, offset=$offset (过期 ${stalenessMs}ms)")
                } else {
                    Log.d(TAG, "跳过过期提醒: taskId=$taskId, offset=$offset (过期 ${stalenessMs}ms)")
                }
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
     *
     * DEADLINE 级别 (offsetMinutes == 0) 使用 setAlarmClock()：
     * - 这是 MIUI 唯一不会掐死的 AlarmManager API
     * - 状态栏显示闹钟图标，点击跳转 App（showIntent）
     * - 系统保证即使在 Doze 模式下也准时触发
     *
     * 其他级别使用 setExactAndAllowWhileIdle()：
     * - 提前提醒允许 best-effort 延迟，不值得占状态栏闹钟图标
     */
    private fun scheduleExactAlarm(taskId: String, taskTitle: String, offsetMinutes: Int, triggerAt: Instant) {
        val pendingIntent = createCascadePendingIntent(taskId, taskTitle, offsetMinutes)
        val triggerMillis = triggerAt.toEpochMilli()

        // DEADLINE: 优先 setAlarmClock() — MIUI 保送
        if (offsetMinutes == 0) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                try {
                    val showIntent = createAppLaunchPendingIntent()
                    val alarmInfo = AlarmManager.AlarmClockInfo(triggerMillis, showIntent)
                    alarmManager.setAlarmClock(alarmInfo, pendingIntent)
                    Log.d(TAG, "DEADLINE 闹钟已设置 (setAlarmClock): $triggerMillis")
                    return
                } catch (e: SecurityException) {
                    Log.w(TAG, "setAlarmClock 失败: ${e.message}, 降级为 setAndAllowWhileIdle")
                }
            } else {
                Log.w(TAG, "精确闹钟权限未授予，DEADLINE 降级为非精确闹钟")
            }
            // 降级: 仍然调度，只是可能被 MIUI 延迟
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
            return
        }

        // 非 DEADLINE: setExactAndAllowWhileIdle() — best-effort
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
     * 创建点击状态栏闹钟图标时的跳转 PendingIntent
     * setAlarmClock() 的 showIntent — 用户看到闹钟图标点击后打开 App
     */
    private fun createAppLaunchPendingIntent(): PendingIntent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        return PendingIntent.getActivity(
            context,
            "alarm-clock-show".hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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

    /**
     * 立即触发提醒 — 绕过 AlarmManager，直接发送广播
     *
     * 用于刚过期的闹钟（典型场景：FIRE_OFF 的 0m 提醒）。
     * 发送与 AlarmManager 完全相同的 Intent，
     * TaskReminderReceiver 无法区分来源 — 走同一条通知管线。
     *
     * 持 PARTIAL_WAKE_LOCK (2秒超时) 确保 CPU 不在广播传递期间休眠。
     * 超时自动释放，防泄漏。
     */
    private fun fireImmediately(taskId: String, taskTitle: String, offsetMinutes: Int) {
        // WakeLock 确保 CPU 在锁屏时不睡
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Prism:FireImmediateWakeLock"
        )
        wakeLock.acquire(2_000L) // 2秒超时，防泄漏

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_OFFSET_MINUTES, offsetMinutes)
        }
        context.sendBroadcast(intent)
        // wakeLock 超时自动释放，无需手动 release
    }
}
