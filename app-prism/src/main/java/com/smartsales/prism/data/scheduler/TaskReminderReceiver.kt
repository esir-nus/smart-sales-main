package com.smartsales.prism.data.scheduler

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartsales.prism.R
import com.smartsales.prism.data.notification.AlarmDismissReceiver
import com.smartsales.prism.domain.notification.NotificationService
import com.smartsales.prism.domain.notification.PrismNotificationChannel
import com.smartsales.prism.domain.scheduler.CascadeTier
import com.smartsales.prism.ui.alarm.AlarmActivity
import dagger.hilt.android.EntryPointAccessors

/**
 * 任务提醒广播接收器
 *
 * 接收 AlarmManager 发送的提醒广播。
 * 根据 CascadeTier（EARLY / FINAL / DEADLINE）区分行为：
 * - EARLY: ⏰ 标准横幅通知，无全屏，无持续振动
 * - FINAL: ⚠️ 横幅通知 + 贪睡按钮，无全屏，无持续振动
 * - DEADLINE: 🚨 全屏 AlarmActivity + 持续振动 + 知道了/贪睡按钮
 */
class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TaskReminderReceiver"
    }

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface NotificationServiceEntryPoint {
        fun notificationService(): NotificationService
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RealAlarmScheduler.ACTION_TASK_REMINDER) return

        val taskId = intent.getStringExtra(RealAlarmScheduler.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(RealAlarmScheduler.EXTRA_TASK_TITLE) ?: "任务提醒"
        val offsetMinutes = intent.getIntExtra(RealAlarmScheduler.EXTRA_OFFSET_MINUTES, 15)
        val tier = CascadeTier.from(offsetMinutes)

        Log.d(TAG, "收到任务提醒: taskId=$taskId, title=$taskTitle, offset=${offsetMinutes}min, tier=$tier")

        // DEADLINE 需要 WakeLock 确保 Activity 启动
        // 如果 fullScreenIntent 权限未授予（中国 OEM 常见），
        // 降级为 SCREEN_BRIGHT_WAKE_LOCK 强制亮屏
        val canUseFullScreen = if (tier == CascadeTier.DEADLINE &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.canUseFullScreenIntent()
        } else {
            true // EARLY/FINAL 不关心, 或 Android 14 以下默认支持
        }

        if (tier == CascadeTier.DEADLINE) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = if (!canUseFullScreen) {
                // 降级: SCREEN_BRIGHT 强制亮屏（deprecated 但对中国 ROM 有效）
                Log.w(TAG, "fullScreenIntent 未授予，使用 SCREEN_BRIGHT_WAKE_LOCK 亮屏")
                powerManager.newWakeLock(
                    android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                            android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "Prism:TaskReminderScreenWake"
                )
            } else {
                powerManager.newWakeLock(
                    android.os.PowerManager.PARTIAL_WAKE_LOCK,
                    "Prism:TaskReminderWakeLock"
                )
            }
            wakeLock.acquire(10 * 1000L)
        }

        val timeText = buildTimeText(offsetMinutes)
        val notificationId = "$taskId-$offsetMinutes"
        val titlePrefix = when (tier) {
            CascadeTier.EARLY    -> "⏰"
            CascadeTier.FINAL    -> "⚠️"
            CascadeTier.DEADLINE -> "🚨"
        }

        // === PendingIntents ===

        // contentIntent: 点击通知 → 停止振动 + 打开应用
        val contentDismiss = AlarmDismissReceiver.createIntent(
            context, notificationId, taskId,
            openApp = true, requestCode = "$taskId-$offsetMinutes-content".hashCode()
        )

        // deleteIntent: 滑动关闭 → 停止振动
        val deleteDismiss = AlarmDismissReceiver.createIntent(
            context, notificationId, taskId,
            openApp = false, requestCode = "$taskId-$offsetMinutes-delete".hashCode()
        )

        // === 构建通知 ===
        val priority = if (tier == CascadeTier.DEADLINE) {
            NotificationCompat.PRIORITY_MAX
        } else {
            NotificationCompat.PRIORITY_HIGH
        }

        val builder = NotificationCompat.Builder(context, PrismNotificationChannel.TASK_REMINDER.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$titlePrefix $taskTitle")
            .setContentText(timeText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(timeText))
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(tier != CascadeTier.DEADLINE)
            .setContentIntent(contentDismiss)
            .setDeleteIntent(deleteDismiss)

        // FINAL / DEADLINE: "知道了" 按钮
        if (tier != CascadeTier.EARLY) {
            val actionDismiss = AlarmDismissReceiver.createIntent(
                context, notificationId, taskId,
                openApp = false, requestCode = "$taskId-$offsetMinutes-action".hashCode()
            )
            builder.addAction(0, "知道了", actionDismiss)
        }


        // DEADLINE: fullScreenIntent → AlarmActivity
        if (tier == CascadeTier.DEADLINE) {
            val fullScreenActivity = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(AlarmActivity.EXTRA_TASK_TITLE, taskTitle)
                putExtra(AlarmActivity.EXTRA_TIME_TEXT, timeText)
                putExtra(AlarmActivity.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(AlarmActivity.EXTRA_TASK_ID, taskId)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                "$taskId-$offsetMinutes-fullscreen".hashCode(),
                fullScreenActivity,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // canUseFullScreen 已在上方 WakeLock 块计算
            if (canUseFullScreen) {
                builder.setFullScreenIntent(fullScreenPendingIntent, true)
                Log.d(TAG, "fullScreenIntent 已设置 (DEADLINE)")
            } else {
                Log.w(TAG, "fullScreenIntent 权限未授予，降级为横幅")
            }
        }

        val notification = builder.build()

        try {
            NotificationManagerCompat.from(context)
                .notify(notificationId.hashCode(), notification)
            Log.d(TAG, "通知已显示: id=$notificationId, tier=$tier")
        } catch (e: SecurityException) {
            Log.w(TAG, "通知发送失败: ${e.message}")
        }

        // DEADLINE: 持续振动
        if (tier == CascadeTier.DEADLINE) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                NotificationServiceEntryPoint::class.java
            )
            entryPoint.notificationService().startPersistentVibration()
            Log.d(TAG, "持续振动已启动 (DEADLINE)")
        }
    }

    private fun buildTimeText(offsetMinutes: Int): String = when (offsetMinutes) {
        0 -> "现在开始！"
        1 -> "1分钟后开始"
        5 -> "5分钟后开始"
        15 -> "15分钟后开始"
        30 -> "30分钟后开始"
        60 -> "1小时后开始"
        120 -> "2小时后开始"
        else -> "${offsetMinutes}分钟后开始"
    }
}
