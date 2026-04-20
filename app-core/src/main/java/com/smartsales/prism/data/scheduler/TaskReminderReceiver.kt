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
import com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManager
import com.smartsales.prism.data.notification.AlarmDismissReceiver
import com.smartsales.prism.domain.notification.NotificationService
import com.smartsales.prism.domain.notification.PrismNotificationChannel
import com.smartsales.prism.domain.scheduler.CascadeTier
import com.smartsales.prism.domain.scheduler.SchedulerRefreshBus
import com.smartsales.prism.domain.scheduler.SchedulerReminderSurfaceBus
import com.smartsales.prism.domain.scheduler.SchedulerReminderSurfaceEvent
import com.smartsales.prism.ui.alarm.AlarmActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 任务提醒广播接收器
 *
 * 接收 AlarmManager 发送的提醒广播。
 * 根据 CascadeTier（EARLY / DEADLINE）区分行为：
 * - EARLY: ⏰ 标准横幅通知，无全屏，无持续振动，尊重 DND
 * - DEADLINE: 🚨 全屏 AlarmActivity + 持续振动（Activity 内启动）+ 绕过 DND
 */
class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TaskReminderReceiver"
    }

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface NotificationServiceEntryPoint {
        fun notificationService(): NotificationService
        fun deviceConnectionManager(): DeviceConnectionManager
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RealAlarmScheduler.ACTION_TASK_REMINDER) return

        // 确保通知渠道存在 — Receiver 可能在 RealNotificationService 初始化前触发
        ensureChannel(context)

        val taskId = intent.getStringExtra(RealAlarmScheduler.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(RealAlarmScheduler.EXTRA_TASK_TITLE) ?: "任务提醒"
        val offsetMinutes = intent.getIntExtra(RealAlarmScheduler.EXTRA_OFFSET_MINUTES, 15)
        val tier = CascadeTier.from(offsetMinutes)

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "系统已关闭应用通知: taskId=$taskId，Receiver 已触发但系统会拦截展示")
        }

        Log.d(TAG, "收到任务提醒: taskId=$taskId, title=$taskTitle, offset=${offsetMinutes}min, tier=$tier")

        // 每一次闹钟触发都向徽章发送一次提示音信号（与等级无关）
        // 通过 goAsync() 保证 BLE 写入能在 Receiver 返回后完成
        fireBadgeChime(context)

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

        val selectedChannel = if (tier == CascadeTier.DEADLINE) {
            PrismNotificationChannel.TASK_REMINDER_DEADLINE
        } else {
            PrismNotificationChannel.TASK_REMINDER_EARLY
        }

        val builder = NotificationCompat.Builder(context, selectedChannel.channelId)
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

        // DEADLINE: "知道了" 按钮
        if (tier == CascadeTier.DEADLINE) {
            val actionDismiss = AlarmDismissReceiver.createIntent(
                context, notificationId, taskId,
                openApp = false, requestCode = "$taskId-$offsetMinutes-action".hashCode()
            )
            builder.addAction(0, "知道了", actionDismiss)
        }


        // DEADLINE: fullScreenIntent → AlarmActivity
        if (tier == CascadeTier.DEADLINE) {
            val fullScreenActivity = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
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
            Log.d(TAG, "通知已显示: id=$notificationId, tier=$tier, channel=${selectedChannel.channelId}")
        } catch (e: SecurityException) {
            Log.w(TAG, "通知发送失败: ${e.message}")
        }

        // DEADLINE: 持续振动在 AlarmActivity.onCreate 内启动，不在 Receiver 启动（避免 Ghost Alarm）

        if (tier == CascadeTier.EARLY) {
            SchedulerReminderSurfaceBus.emit(
                SchedulerReminderSurfaceEvent(
                    taskId = taskId,
                    offsetMinutes = offsetMinutes,
                    taskTitle = taskTitle,
                    timeText = timeText
                )
            )
        }

        // DEADLINE 到期 → 通知 ViewModel 刷新（实时标记过期任务）
        if (tier == CascadeTier.DEADLINE) {
            SchedulerRefreshBus.emit()
            Log.d(TAG, "SchedulerRefreshBus emitted for taskId=$taskId")
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

    /**
     * 确保任务提醒通知渠道存在
     *
     * BroadcastReceiver 可能在 RealNotificationService 初始化前触发
     * （典型：App 被杀后 AlarmManager 唤醒），此时渠道可能不存在。
     * createNotificationChannel() 幂等 — 已有渠道不会被覆盖。
     */
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 确保两个任务提醒渠道都已创建
        listOf(
            PrismNotificationChannel.TASK_REMINDER_EARLY,
            PrismNotificationChannel.TASK_REMINDER_DEADLINE
        ).forEach { prismChannel ->
            if (nm.getNotificationChannel(prismChannel.channelId) != null) return@forEach

            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(
                prismChannel.channelId,
                prismChannel.displayName,
                importance
            ).apply {
                when (prismChannel) {
                    PrismNotificationChannel.TASK_REMINDER_EARLY -> {
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 250)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                        // 不设 setBypassDnd — 尊重勿扰
                    }
                    PrismNotificationChannel.TASK_REMINDER_DEADLINE -> {
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 500, 300, 500)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                        setBypassDnd(true)  // 绕过勿扰
                        setSound(
                            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    }
                    else -> { /* NOP */ }
                }
            }
            nm.createNotificationChannel(channel)
            Log.d(TAG, "Receiver 内补建渠道: ${prismChannel.channelId}")
        }
    }

    /**
     * 触发徽章提示音：每次闹钟无论等级都发送一次 BLE 信号。
     * BLE 未连接或写入失败时静默跳过，不影响闹钟主流程。
     */
    private fun fireBadgeChime(context: Context) {
        val entryPoint = runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                NotificationServiceEntryPoint::class.java
            )
        }.getOrNull() ?: return

        val manager = runCatching { entryPoint.deviceConnectionManager() }.getOrNull() ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                manager.notifyTaskFired()
            } catch (e: Exception) {
                Log.w(TAG, "Badge chime dispatch failed: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }
}
