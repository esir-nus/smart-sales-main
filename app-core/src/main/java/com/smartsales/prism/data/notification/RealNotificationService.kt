package com.smartsales.prism.data.notification

import android.Manifest
import android.app.NotificationChannel as AndroidNotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smartsales.prism.R
import com.smartsales.prism.domain.notification.NotificationAction
import com.smartsales.prism.domain.notification.NotificationPriority
import com.smartsales.prism.domain.notification.NotificationService
import com.smartsales.prism.domain.notification.PrismNotificationChannel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android NotificationManager 封装
 *
 * 统一管理通知渠道创建、权限检查和通知显示。
 * 所有功能模块通过此实现来发送系统通知。
 */
@Singleton
class RealNotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationService {

    companion object {
        private const val TAG = "NotificationService"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    // 渠道是否已初始化（幂等操作，但避免重复调用）
    private var channelsCreated = false

    // 持续振动器 — URGENT 通知专用
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    init {
        ensureChannels()
    }

    override fun show(
        id: String,
        title: String,
        body: String,
        channel: PrismNotificationChannel,
        priority: NotificationPriority,
        action: NotificationAction
    ) {
        if (!hasPermission()) {
            Log.w(TAG, "通知权限未授予，跳过: id=$id, title=$title")
            return
        }

        ensureChannels()

        val androidPriority = when (priority) {
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
            NotificationPriority.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.URGENT -> NotificationCompat.PRIORITY_MAX
        }

        val parsedContentIntent: PendingIntent? = when (action) {
            is NotificationAction.None -> null
            is NotificationAction.OpenApp -> {
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    action.deepLinkParam?.let { param ->
                        putExtra("deep_link_param", param)
                    }
                }
                intent?.let {
                    PendingIntent.getActivity(
                        context,
                        id.hashCode(),
                        it,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            }
        }

        val isUrgent = priority == NotificationPriority.URGENT

        // URGENT 使用独立 Vibrator（持续振动），非 URGENT 使用通知自带振动
        val vibrationPattern = if (isUrgent) {
            null // 不用通知振动，用 Vibrator
        } else {
            when (channel) {
                PrismNotificationChannel.TASK_REMINDER_EARLY -> longArrayOf(0, 250, 250, 250)
                PrismNotificationChannel.TASK_REMINDER_DEADLINE -> longArrayOf(0, 500, 300, 500)
                PrismNotificationChannel.COACH_NUDGE -> longArrayOf(0, 150)
                PrismNotificationChannel.BADGE_STATUS -> null
                PrismNotificationChannel.BADGE_DOWNLOAD_PROGRESS -> null
                PrismNotificationChannel.MEMORY_UPDATE -> null
            }
        }

        val notification = NotificationCompat.Builder(context, channel.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(androidPriority)
            .setCategory(if (isUrgent) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                if (isUrgent) {
                    // URGENT: 不自动取消，由 AlarmDismissReceiver 统一处理
                    setAutoCancel(false)
                } else {
                    setAutoCancel(true)
                }
                vibrationPattern?.let { setVibrate(it) }
                parsedContentIntent?.let { setContentIntent(it) }
            }
            .build()

        val notificationId = id.hashCode()

        try {
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "通知已显示: id=$id ($notificationId), channel=${channel.channelId}, priority=$priority")
        } catch (e: SecurityException) {
            Log.w(TAG, "通知发送失败 (权限): ${e.message}")
        }
    }

    override fun cancel(id: String) {
        val notificationId = id.hashCode()
        notificationManager.cancel(notificationId)
        Log.d(TAG, "通知已取消: id=$id ($notificationId)")
    }

    override fun hasPermission(): Boolean {
        // API < 33 无需运行时权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun stopVibration() {
        vibrator.cancel()
        Log.d(TAG, "持续振动已停止")
    }

    override fun startPersistentVibration() {
        // 闹钟节奏: 3连振 + 停顿 → 循环
        // [等0ms, 振400ms, 停200ms, 振400ms, 停200ms, 振400ms, 停1000ms] → 从头循环
        val pattern = longArrayOf(0, 400, 200, 400, 200, 400, 1000)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0=循环
        Log.d(TAG, "持续振动已启动")
    }

    /**
     * 创建所有通知渠道 (Android 8+)
     * 幂等操作 — Android 对已存在的渠道不做任何修改
     */
    private fun ensureChannels() {
        if (channelsCreated) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            channelsCreated = true
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        PrismNotificationChannel.entries.forEach { prismChannel ->
            val importance = when (prismChannel) {
                PrismNotificationChannel.TASK_REMINDER_EARLY -> NotificationManager.IMPORTANCE_HIGH
                PrismNotificationChannel.TASK_REMINDER_DEADLINE -> NotificationManager.IMPORTANCE_HIGH
                PrismNotificationChannel.COACH_NUDGE -> NotificationManager.IMPORTANCE_DEFAULT
                PrismNotificationChannel.BADGE_STATUS -> NotificationManager.IMPORTANCE_LOW
                PrismNotificationChannel.BADGE_DOWNLOAD_PROGRESS -> NotificationManager.IMPORTANCE_LOW
                PrismNotificationChannel.MEMORY_UPDATE -> NotificationManager.IMPORTANCE_DEFAULT
            }

            val androidChannel = AndroidNotificationChannel(
                prismChannel.channelId,
                prismChannel.displayName,
                importance
            ).apply {
                when (prismChannel) {
                    // EARLY: 尊重 DND，短振动
                    PrismNotificationChannel.TASK_REMINDER_EARLY -> {
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 250)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                        // 不设 setBypassDnd — 尊重勿扰模式
                    }
                    // DEADLINE: 绕过 DND，闹钟铃声 + 强振动
                    PrismNotificationChannel.TASK_REMINDER_DEADLINE -> {
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 500, 300, 500)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                        setBypassDnd(true)  // 绕过勿扰模式
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    }
                    else -> { /* 其他渠道默认配置 */ }
                }
            }
            manager.createNotificationChannel(androidChannel)
        }

        // 删除旧版渠道（v1 无振动，v2 无闹钟铃声，v3 未分Early/Deadline）
        manager.deleteNotificationChannel("prism_task_reminders")
        manager.deleteNotificationChannel("prism_task_reminders_v2")
        manager.deleteNotificationChannel("prism_task_reminders_v3")

        channelsCreated = true
        Log.d(TAG, "通知渠道已创建: ${PrismNotificationChannel.entries.size} 个")
    }
}
