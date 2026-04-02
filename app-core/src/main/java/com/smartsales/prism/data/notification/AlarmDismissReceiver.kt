package com.smartsales.prism.data.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.smartsales.prism.MainActivity
import com.smartsales.prism.domain.notification.NotificationService
import dagger.hilt.android.EntryPointAccessors

/**
 * 闹钟关闭广播接收器
 *
 * 统一处理所有关闭路径：滑动关闭、"知道了"按钮、点击通知。
 * 每条路径都会：停止振动 → 取消通知。
 * 若 EXTRA_OPEN_APP=true，额外启动 MainActivity。
 */
class AlarmDismissReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmDismissReceiver"
        const val ACTION_DISMISS = "com.smartsales.prism.ALARM_DISMISS"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_OPEN_APP = "open_app"
        const val EXTRA_TASK_ID = "task_id"

        /**
         * 创建指向此 Receiver 的 PendingIntent
         */
        fun createIntent(
            context: Context,
            notificationId: String,
            taskId: String = "",
            openApp: Boolean = false,
            requestCode: Int = 0
        ): PendingIntent {
            val intent = Intent(context, AlarmDismissReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_OPEN_APP, openApp)
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface NotificationEntryPoint {
        fun notificationService(): NotificationService
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISMISS) return

        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: return
        val openApp = intent.getBooleanExtra(EXTRA_OPEN_APP, false)
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: ""

        Log.d(TAG, "关闭闹钟: notificationId=$notificationId, openApp=$openApp")

        // 1. 停止持续振动
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationEntryPoint::class.java
        )
        val notificationService = entryPoint.notificationService()
        notificationService.stopVibration()

        // 2. 取消通知
        NotificationManagerCompat.from(context).cancel(notificationId.hashCode())

        // 3. 关闭全屏 AlarmActivity（如果正在显示）
        context.sendBroadcast(Intent("com.smartsales.prism.FINISH_ALARM").apply {
            setPackage(context.packageName)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        })

        // 4. 若需要打开应用
        if (openApp) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "scheduler")
                putExtra("task_id", taskId)
            }
            context.startActivity(launchIntent)
        }
    }
}
