package com.smartsales.prism.ui.sim

import android.util.Log
import com.smartsales.prism.data.notification.ExactAlarmPermissionGate
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.normalizedReminderCascade

internal class SimSchedulerReminderSupport(
    private val alarmScheduler: AlarmScheduler,
    private val exactAlarmPermissionGate: ExactAlarmPermissionGate,
    private val bridge: SimSchedulerUiBridge
) {

    fun emitReminderReliabilityPromptIfNeeded() {
        if (exactAlarmPermissionGate.shouldPromptForExactAlarm()) {
            bridge.emitExactAlarmPermissionNeeded()
        }
    }

    suspend fun scheduleReminderIfExact(task: ScheduledTask) {
        if (task.isVague || task.isDone) return

        val cascade = task.normalizedReminderCascade()
        if (cascade.isEmpty()) return

        runCatching {
            alarmScheduler.scheduleCascade(
                taskId = task.id,
                taskTitle = task.title,
                eventTime = task.startTime,
                cascade = cascade
            )
        }.onFailure { error ->
            Log.w("SimSchedulerAlarm", "schedule reminder failed for task=${task.id}: ${error.message}")
        }
    }

    suspend fun cancelReminderSafely(taskId: String) {
        runCatching {
            alarmScheduler.cancelReminder(taskId)
        }.onFailure { error ->
            Log.w("SimSchedulerAlarm", "cancel reminder failed for task=$taskId: ${error.message}")
        }
    }
}
