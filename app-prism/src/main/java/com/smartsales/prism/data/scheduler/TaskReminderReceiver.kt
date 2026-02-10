package com.smartsales.prism.data.scheduler

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smartsales.prism.PrismMainActivity
import com.smartsales.prism.domain.notification.NotificationService
import com.smartsales.prism.domain.notification.PrismNotificationChannel
import com.smartsales.prism.domain.notification.NotificationPriority
import dagger.hilt.android.EntryPointAccessors

/**
 * 任务提醒广播接收器
 * 
 * 接收 AlarmManager 发送的提醒广播，通过 NotificationService 显示通知。
 * 使用 Hilt EntryPointAccessors 获取依赖（BroadcastReceiver 不支持构造器注入）。
 */
class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TaskReminderReceiver"
    }

    /**
     * Hilt EntryPoint — 从 Application 组件获取 NotificationService
     */
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
        
        Log.d(TAG, "收到任务提醒: taskId=$taskId, title=$taskTitle, offset=-${offsetMinutes}min")

        // 构建通知内容
        val timeText = when (offsetMinutes) {
            60 -> "1小时后"
            15 -> "15分钟后"
            5 -> "5分钟后"
            1 -> "1分钟后"
            else -> "${offsetMinutes}分钟后"
        }
        
        // 点击通知打开应用并导航到 Scheduler
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

        // 通过 Hilt EntryPoint 获取 NotificationService
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationServiceEntryPoint::class.java
        )
        val notificationService = entryPoint.notificationService()

        // 每个 offset 使用不同的 id，避免级联提醒互相覆盖
        val notificationId = "$taskId-$offsetMinutes"

        notificationService.show(
            id = notificationId,
            title = "⏰ $taskTitle",
            body = "将在${timeText}开始",
            channel = PrismNotificationChannel.TASK_REMINDER,
            priority = NotificationPriority.HIGH,
            contentIntent = pendingIntent
        )
    }
}
