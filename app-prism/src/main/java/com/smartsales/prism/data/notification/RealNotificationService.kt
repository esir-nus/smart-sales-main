package com.smartsales.prism.data.notification

import android.Manifest
import android.app.NotificationChannel as AndroidNotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smartsales.prism.R
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

    init {
        ensureChannels()
    }

    override fun show(
        id: String,
        title: String,
        body: String,
        channel: PrismNotificationChannel,
        priority: NotificationPriority,
        contentIntent: PendingIntent?
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
        }

        val vibrationPattern = when (channel) {
            PrismNotificationChannel.TASK_REMINDER -> longArrayOf(0, 250, 250, 250)
            PrismNotificationChannel.COACH_NUDGE -> longArrayOf(0, 150)
            PrismNotificationChannel.BADGE_STATUS -> null
            PrismNotificationChannel.MEMORY_UPDATE -> null
        }

        val notification = NotificationCompat.Builder(context, channel.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(androidPriority)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .apply {
                vibrationPattern?.let { setVibrate(it) }
                contentIntent?.let { setContentIntent(it) }
            }
            .build()

        val notificationId = id.hashCode()

        try {
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "通知已显示: id=$id (${notificationId}), channel=${channel.channelId}, title=$title")
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
                PrismNotificationChannel.TASK_REMINDER -> NotificationManager.IMPORTANCE_HIGH
                PrismNotificationChannel.COACH_NUDGE -> NotificationManager.IMPORTANCE_DEFAULT
                PrismNotificationChannel.BADGE_STATUS -> NotificationManager.IMPORTANCE_LOW
                PrismNotificationChannel.MEMORY_UPDATE -> NotificationManager.IMPORTANCE_DEFAULT
            }

            val androidChannel = AndroidNotificationChannel(
                prismChannel.channelId,
                prismChannel.displayName,
                importance
            )
            manager.createNotificationChannel(androidChannel)
        }

        channelsCreated = true
        Log.d(TAG, "通知渠道已创建: ${PrismNotificationChannel.entries.size} 个")
    }
}
