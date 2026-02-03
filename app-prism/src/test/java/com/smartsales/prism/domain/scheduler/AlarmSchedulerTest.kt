package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.data.fakes.FakeAlarmScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * AlarmScheduler 单元测试
 * 
 * 测试级联提醒的调度逻辑
 */
class AlarmSchedulerTest {
    
    private lateinit var alarmScheduler: FakeAlarmScheduler
    
    @Before
    fun setup() {
        alarmScheduler = FakeAlarmScheduler()
    }
    
    @Test
    fun `scheduleReminder with SMART_CASCADE creates 3 alarms`() = runTest {
        // Given
        val taskId = "task-123"
        val eventTime = Instant.parse("2026-02-03T15:00:00Z")
        
        // When
        alarmScheduler.scheduleReminder(taskId, eventTime, ReminderType.SMART_CASCADE)
        
        // Then
        val alarms = alarmScheduler.getAlarmsForTask(taskId)
        assertEquals(3, alarms.size)
        
        // 验证偏移量: -60m, -15m, -5m
        val offsets = alarms.mapNotNull { it.offsetMinutes }.sortedDescending()
        assertEquals(listOf(60, 15, 5), offsets)
    }
    
    @Test
    fun `scheduleReminder with SINGLE creates 1 alarm at -15m`() = runTest {
        // Given
        val taskId = "task-456"
        val eventTime = Instant.parse("2026-02-03T15:00:00Z")
        
        // When
        alarmScheduler.scheduleReminder(taskId, eventTime, ReminderType.SINGLE)
        
        // Then
        val alarms = alarmScheduler.getAlarmsForTask(taskId)
        assertEquals(1, alarms.size)
        assertEquals(15, alarms.first().offsetMinutes)
    }
    
    @Test
    fun `scheduleSmartCascade for MEETING uses cascade`() = runTest {
        // Given
        val taskId = "meeting-789"
        val eventTime = Instant.parse("2026-02-03T15:00:00Z")
        
        // When
        alarmScheduler.scheduleSmartCascade(taskId, eventTime, TaskTypeHint.MEETING)
        
        // Then
        val alarms = alarmScheduler.getAlarmsForTask(taskId)
        assertEquals(3, alarms.size) // Cascade = 3 alarms
    }
    
    @Test
    fun `scheduleSmartCascade for CALL uses single`() = runTest {
        // Given
        val taskId = "call-101"
        val eventTime = Instant.parse("2026-02-03T15:00:00Z")
        
        // When
        alarmScheduler.scheduleSmartCascade(taskId, eventTime, TaskTypeHint.CALL)
        
        // Then
        val alarms = alarmScheduler.getAlarmsForTask(taskId)
        assertEquals(1, alarms.size) // Single = 1 alarm
    }
    
    @Test
    fun `cancelReminder removes all alarms for task`() = runTest {
        // Given
        val taskId = "task-cancel"
        val eventTime = Instant.parse("2026-02-03T15:00:00Z")
        alarmScheduler.scheduleReminder(taskId, eventTime, ReminderType.SMART_CASCADE)
        assertEquals(3, alarmScheduler.getAlarmsForTask(taskId).size)
        
        // When
        alarmScheduler.cancelReminder(taskId)
        
        // Then
        assertTrue(alarmScheduler.getAlarmsForTask(taskId).isEmpty())
        assertTrue(alarmScheduler.cancelledTaskIds.contains(taskId))
    }
    
    @Test
    fun `alarm trigger times are correctly calculated`() = runTest {
        // Given
        val taskId = "task-times"
        val eventTime = Instant.parse("2026-02-03T15:00:00Z")
        
        // When
        alarmScheduler.scheduleReminder(taskId, eventTime, ReminderType.SMART_CASCADE)
        
        // Then
        val alarms = alarmScheduler.scheduledAlarms
        
        // 验证每个提醒的触发时间
        val alarm60m = alarms.find { it.offsetMinutes == 60 }!!
        assertEquals(Instant.parse("2026-02-03T14:00:00Z"), alarm60m.triggerAt)
        
        val alarm15m = alarms.find { it.offsetMinutes == 15 }!!
        assertEquals(Instant.parse("2026-02-03T14:45:00Z"), alarm15m.triggerAt)
        
        val alarm5m = alarms.find { it.offsetMinutes == 5 }!!
        assertEquals(Instant.parse("2026-02-03T14:55:00Z"), alarm5m.triggerAt)
    }
}
