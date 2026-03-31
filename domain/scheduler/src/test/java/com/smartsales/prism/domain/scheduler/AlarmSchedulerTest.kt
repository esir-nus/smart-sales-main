package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.scheduler.fakes.FakeAlarmScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * AlarmScheduler 单元测试
 * 
 * 测试级联提醒的调度逻辑 (UrgencyLevel based)
 */
class AlarmSchedulerTest {
    
    private lateinit var alarmScheduler: FakeAlarmScheduler
    
    @Before
    fun setup() {
        alarmScheduler = FakeAlarmScheduler()
    }
    
    @Test
    fun `scheduleCascade with L1 critical offsets creates 3 alarms`() = runTest {
        // Given
        val taskId = "task-flight"
        val eventTime = Instant.parse("2026-02-03T15:00:00Z")
        val cascade = UrgencyLevel.buildCascade(UrgencyLevel.L1_CRITICAL)
        
        // When
        alarmScheduler.scheduleCascade(taskId, "Catch Flight", eventTime, cascade)
        
        // Then
        val alarms = alarmScheduler.getAlarmsForTask(taskId)
        assertEquals(3, alarms.size)

        // 验证偏移量: -1h, -10m, 0m
        val offsets = alarms.mapNotNull { it.offsetMinutes }.sortedDescending()
        assertEquals(listOf(60, 10, 0), offsets)
    }
    
    @Test
    fun `scheduleCascade with L3 normal offsets creates single deadline alarm`() = runTest {
        // Given
        val taskId = "task-email"
        val eventTime = Instant.parse("2026-02-03T15:00:00Z")
        val cascade = UrgencyLevel.buildCascade(UrgencyLevel.L3_NORMAL)
        
        // When
        alarmScheduler.scheduleCascade(taskId, "Reply Email", eventTime, cascade)
        
        // Then
        val alarms = alarmScheduler.getAlarmsForTask(taskId)
        assertEquals(1, alarms.size)
        assertEquals(listOf(0), alarms.map { it.offsetMinutes }.sortedDescending())
    }
    
    @Test
    fun `scheduleCascade with FIRE_OFF creates single 0m alarm`() = runTest {
        // Given
        val taskId = "task-water"
        val eventTime = Instant.parse("2026-02-03T15:00:00Z")
        val cascade = UrgencyLevel.buildCascade(UrgencyLevel.FIRE_OFF)
        
        // When
        alarmScheduler.scheduleCascade(taskId, "Drink Water", eventTime, cascade)
        
        // Then
        val alarms = alarmScheduler.getAlarmsForTask(taskId)
        assertEquals(1, alarms.size) // 只有 0m 一个提醒
        assertEquals(0, alarms.first().offsetMinutes)
    }
    
    @Test
    fun `cancelReminder removes all alarms for task`() = runTest {
        // Given
        val taskId = "task-cancel"
        val eventTime = Instant.parse("2026-02-03T15:00:00Z")
        val cascade = UrgencyLevel.buildCascade(UrgencyLevel.L1_CRITICAL)
        alarmScheduler.scheduleCascade(taskId, "Sprint Review", eventTime, cascade)
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
        val cascade = UrgencyLevel.buildCascade(UrgencyLevel.L2_IMPORTANT) // -30m, 0m
        
        // When
        alarmScheduler.scheduleCascade(taskId, "Performance Review", eventTime, cascade)
        
        // Then
        val alarms = alarmScheduler.scheduledAlarms
        
        // 验证每个提醒的触发时间
        val alarm30m = alarms.find { it.offsetMinutes == 30 }!!
        assertEquals(Instant.parse("2026-02-03T14:30:00Z"), alarm30m.triggerAt)

        val alarm0m = alarms.find { it.offsetMinutes == 0 }!!
        assertEquals(Instant.parse("2026-02-03T15:00:00Z"), alarm0m.triggerAt)
    }
}
