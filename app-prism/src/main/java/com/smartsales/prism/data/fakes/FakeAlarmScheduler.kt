package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.ReminderType
import com.smartsales.prism.domain.scheduler.TaskTypeHint
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FakeAlarmScheduler — 用于测试的闹钟调度器
 * 
 * 记录所有调度操作，不实际设置系统闹钟
 */
@Singleton
class FakeAlarmScheduler @Inject constructor() : AlarmScheduler {
    
    data class ScheduledAlarm(
        val taskId: String,
        val triggerAt: Instant,
        val type: ReminderType,
        val offsetMinutes: Int? = null
    )
    
    private val _scheduledAlarms = mutableListOf<ScheduledAlarm>()
    val scheduledAlarms: List<ScheduledAlarm> get() = _scheduledAlarms.toList()
    
    private val _cancelledTaskIds = mutableListOf<String>()
    val cancelledTaskIds: List<String> get() = _cancelledTaskIds.toList()
    
    /** 用于测试的级联偏移量 */
    private val cascadeOffsets = listOf(60, 15, 5, 1)
    private val singleOffset = listOf(15, 1)
    
    override suspend fun scheduleReminder(taskId: String, taskTitle: String, triggerAt: Instant, type: ReminderType) {
        val offsets = when (type) {
            ReminderType.SMART_CASCADE -> cascadeOffsets
            ReminderType.SINGLE -> singleOffset
        }
        
        offsets.forEach { offset ->
            _scheduledAlarms.add(
                ScheduledAlarm(
                    taskId = taskId,
                    triggerAt = triggerAt.minusSeconds(offset * 60L),
                    type = type,
                    offsetMinutes = offset
                )
            )
        }
    }

    override suspend fun cancelReminder(taskId: String) {
        _cancelledTaskIds.add(taskId)
        _scheduledAlarms.removeAll { it.taskId == taskId }
    }

    override suspend fun scheduleSmartCascade(taskId: String, taskTitle: String, eventTime: Instant, taskType: TaskTypeHint) {
        val reminderType = when (taskType) {
            TaskTypeHint.MEETING -> ReminderType.SMART_CASCADE
            TaskTypeHint.CALL -> ReminderType.SINGLE
            TaskTypeHint.PERSONAL -> ReminderType.SINGLE
            TaskTypeHint.URGENT -> ReminderType.SMART_CASCADE
        }
        scheduleReminder(taskId, taskTitle, eventTime, reminderType)
    }
    
    /** 清空测试状态 */
    fun reset() {
        _scheduledAlarms.clear()
        _cancelledTaskIds.clear()
    }
    
    /** 获取某个任务的所有提醒 */
    fun getAlarmsForTask(taskId: String): List<ScheduledAlarm> {
        return _scheduledAlarms.filter { it.taskId == taskId }
    }
}
