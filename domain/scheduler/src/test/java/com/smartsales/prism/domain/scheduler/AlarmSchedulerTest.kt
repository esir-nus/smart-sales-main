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
    fun `scheduleCascade with L1 critical offsets creates 7 alarms`() = runTest {
        // Given
        val taskId = "task-flight"
        val eventTime = Instant.parse("2026-02-03T15:00:00Z")
        val cascade = UrgencyLevel.buildCascade(UrgencyLevel.L1_CRITICAL)
        
        // When
        alarmScheduler.scheduleCascade(taskId, "Catch Flight", eventTime, cascade)
        
        // Then
        val alarms = alarmScheduler.getAlarmsForTask(taskId)
        assertEquals(6, alarms.size)
        
        // 验证偏移量: -2h, -1h, -30m, -15m, -5m, 0m
        val offsets = alarms.mapNotNull { it.offsetMinutes }.sortedDescending()
        assertEquals(listOf(120, 60, 30, 15, 5, 0), offsets)
    }
    
    @Test
    fun `scheduleCascade with L3 normal offsets creates 2 alarms`() = runTest {
        // Given
        val taskId = "task-email"
        val eventTime = Instant.parse("2026-02-03T15:00:00Z")
        val cascade = UrgencyLevel.buildCascade(UrgencyLevel.L3_NORMAL)
        
        // When
        alarmScheduler.scheduleCascade(taskId, "Reply Email", eventTime, cascade)
        
        // Then
        val alarms = alarmScheduler.getAlarmsForTask(taskId)
        assertEquals(3, alarms.size) // -15m, -5m, 0m
        assertEquals(listOf(15, 5, 0), alarms.map { it.offsetMinutes }.sortedDescending())
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
        assertEquals(6, alarmScheduler.getAlarmsForTask(taskId).size)
        
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
        val cascade = UrgencyLevel.buildCascade(UrgencyLevel.L2_IMPORTANT) // -1h, -15m, -5m, -1m
        
        // When
        alarmScheduler.scheduleCascade(taskId, "Performance Review", eventTime, cascade)
        
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
