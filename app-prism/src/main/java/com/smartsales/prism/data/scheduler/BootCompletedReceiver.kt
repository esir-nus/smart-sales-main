package com.smartsales.prism.data.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smartsales.prism.data.persistence.ScheduledTaskDao
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.time.TimeProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * 开机恢复闹钟
 *
 * 设备重启后 AlarmManager 所有闹钟丢失。
 * 此 Receiver 从 Room 中查询所有 hasAlarm=true 的未来任务，
 * 逐一调用 AlarmScheduler.scheduleCascade() 重新注册。
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootEntryPoint {
        fun alarmScheduler(): AlarmScheduler
        fun timeProvider(): TimeProvider
        fun scheduledTaskDao(): ScheduledTaskDao
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "收到 BOOT_COMPLETED，开始恢复闹钟")

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BootEntryPoint::class.java
        )

        val pendingResult = goAsync()
        val scheduler = entryPoint.alarmScheduler()
        val timeProvider = entryPoint.timeProvider()
        val dao = entryPoint.scheduledTaskDao()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nowMs = timeProvider.now.toEpochMilli()
                val tasks = dao.getFutureTasksWithAlarm(nowMs)

                Log.d(TAG, "需要恢复 ${tasks.size} 个闹钟")

                for (task in tasks) {
                    val cascade = task.alarmCascadeJson?.let { json ->
                        val array = JSONArray(json)
                        List(array.length()) { i -> array.getString(i) }
                    } ?: listOf("0m")

                    scheduler.scheduleCascade(
                        taskId = task.taskId,
                        taskTitle = task.title,
                        eventTime = java.time.Instant.ofEpochMilli(task.startTimeMillis),
                        cascade = cascade
                    )
                    Log.d(TAG, "已恢复: ${task.title} (${cascade.size} 级)")
                }

                Log.d(TAG, "闹钟恢复完成: ${tasks.size} 个任务")
            } catch (e: Exception) {
                Log.e(TAG, "闹钟恢复失败: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
