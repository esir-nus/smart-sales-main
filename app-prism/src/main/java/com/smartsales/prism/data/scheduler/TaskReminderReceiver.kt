package com.smartsales.prism.data.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 任务提醒广播接收器
 * 
 * 注意: 需要在 AndroidManifest.xml 中注册
 */
class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == RealAlarmScheduler.ACTION_TASK_REMINDER) {
            val taskId = intent.getStringExtra(RealAlarmScheduler.EXTRA_TASK_ID)
            Log.d(TAG, "收到任务提醒: $taskId")
            
            // TODO: 完善通知显示逻辑
            // 1. 查询任务详情
            // 2. 显示通知
            // 3. 可选: 振动/声音
        }
    }

    companion object {
        private const val TAG = "TaskReminderReceiver"
    }
}
