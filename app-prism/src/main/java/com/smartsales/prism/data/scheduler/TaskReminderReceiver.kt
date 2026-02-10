package com.smartsales.prism.data.scheduler

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smartsales.prism.PrismMainActivity
import com.smartsales.prism.R

/**
 * 任务提醒广播接收器
 * 
 * 接收 AlarmManager 发送的提醒广播，显示通知
 */
class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TaskReminderReceiver"
        const val CHANNEL_ID = "prism_task_reminders"
        const val CHANNEL_NAME = "任务提醒"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RealAlarmScheduler.ACTION_TASK_REMINDER) return
        
        val taskId = intent.getStringExtra(RealAlarmScheduler.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(RealAlarmScheduler.EXTRA_TASK_TITLE) ?: "任务提醒"
        val offsetMinutes = intent.getIntExtra(RealAlarmScheduler.EXTRA_OFFSET_MINUTES, 15)
        
        Log.d(TAG, "收到任务提醒: taskId=$taskId, title=$taskTitle, offset=-${offsetMinutes}min")
        
        // 确保通知渠道已创建
        ensureNotificationChannel(context)
        
        // 显示通知
        showNotification(context, taskId, taskTitle, offsetMinutes)
    }

    /**
     * 确保通知渠道已创建 (Android 8.0+)
     */
    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "智能任务提醒 — 在任务开始前多次提醒"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 显示任务提醒通知
     */
    private fun showNotification(context: Context, taskId: String, taskTitle: String, offsetMinutes: Int) {
        // 检查通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                Log.w(TAG, "缺少通知权限，无法显示提醒")
                return
            }
        }
        
        // 构建通知内容
        val timeText = when (offsetMinutes) {
            60 -> "1小时后"
            15 -> "15分钟后"
            5 -> "5分钟后"
            1 -> "1分钟后"
            else -> "${offsetMinutes}分钟后"
        }
        
        // 点击通知打开应用
        val contentIntent = Intent(context, PrismMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "scheduler")
            putExtra("task_id", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ $taskTitle")
            .setContentText("将在${timeText}开始")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("将在${timeText}开始")
                .setSummaryText("任务提醒")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()
        
        // 每个 offset 使用不同的 notificationId
        val notificationId = "$taskId-$offsetMinutes".hashCode()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d(TAG, "通知已显示: id=$notificationId")
        } catch (e: SecurityException) {
            Log.e(TAG, "显示通知失败: ${e.message}")
        }
    }
}
