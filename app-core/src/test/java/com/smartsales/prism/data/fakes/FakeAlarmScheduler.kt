package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 测试用闹钟调度器
 *
 * 记录所有调度操作，不实际设置系统闹钟。
 * 支持级联偏移量调度方式。
 */
@Singleton
class FakeAlarmScheduler @Inject constructor() : AlarmScheduler {
    
    data class ScheduledAlarm(
        val taskId: String,
        val triggerAt: Instant,
        val offsetMinutes: Int
    )
    
    private val _scheduledAlarms = mutableListOf<ScheduledAlarm>()
    val scheduledAlarms: List<ScheduledAlarm> get() = _scheduledAlarms.toList()
    
    private val _cancelledTaskIds = mutableListOf<String>()
    val cancelledTaskIds: List<String> get() = _cancelledTaskIds.toList()
    
    override suspend fun scheduleCascade(
        taskId: String,
        taskTitle: String,
        eventTime: Instant,
        cascade: List<String>
    ) {
        cascade.forEach { offset ->
            val offsetMs = UrgencyLevel.parseCascadeOffset(offset)
            val offsetMinutes = (offsetMs / 60_000).toInt()
            _scheduledAlarms.add(
                ScheduledAlarm(
                    taskId = taskId,
                    triggerAt = eventTime.minusMillis(offsetMs),
                    offsetMinutes = offsetMinutes
                )
            )
        }
    }

    override suspend fun cancelReminder(taskId: String) {
        _cancelledTaskIds.add(taskId)
        _scheduledAlarms.removeAll { it.taskId == taskId }
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
