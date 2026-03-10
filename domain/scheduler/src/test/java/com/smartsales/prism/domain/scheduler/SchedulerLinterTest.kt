package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.data.fakes.FakeTimeProvider
import com.smartsales.prism.domain.memory.ConflictPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SchedulerLinter 单元测试
 * 验证 LLM 输出解析 (UrgencyLevel) 和日期验证逻辑
 */
class SchedulerLinterTest {
    
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var linter: SchedulerLinter
    
    @Before
    fun setup() {
        // 固定时间: 2026-02-02 10:00 UTC+8
        timeProvider = FakeTimeProvider()
        timeProvider.setDateTime(2026, 2, 2, 10, 0)
        linter = SchedulerLinter(timeProvider)
    }
    
    @Test
    fun `valid JSON returns Success`() {
        val validJson = """
            {
                "title": "赶飞机",
                "startTime": "2026-02-03 03:00",
                "endTime": "2026-02-03 04:00",
                "urgency": "L1"
            }
        """.trimIndent()
        
        val result = linter.lint(validJson)
        
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals("赶飞机", success.task.title)
        assertEquals(UrgencyLevel.L1_CRITICAL, success.urgencyLevel)
        assertTrue(success.task.isSmartAlarm)
        assertEquals(6, success.task.alarmCascade.size) // L1 = 6 alarms (including 0m)
    }
    
    @Test
    fun `missing urgency defaults to L3`() {
        val noUrgencyJson = """
            {
                "title": "买牛奶",
                "startTime": "2026-02-03 14:00"
            }
        """.trimIndent()
        
        val result = linter.lint(noUrgencyJson)
        
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(UrgencyLevel.L3_NORMAL, success.urgencyLevel)
        assertEquals(3, success.task.alarmCascade.size) // L3 = 3 alarms (-15m, -1m, 0m)
    }
    
    @Test
    fun `FIRE_OFF urgency sets COEXISTING policy and single 0m alarm`() {
        val fireOffJson = """
            {
                "title": "喝水",
                "startTime": "2026-02-03 10:15",
                "urgency": "FIRE_OFF"
            }
        """.trimIndent()
        
        val result = linter.lint(fireOffJson)
        
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(UrgencyLevel.FIRE_OFF, success.urgencyLevel)
        assertEquals(ConflictPolicy.COEXISTING, success.task.conflictPolicy)
        assertEquals(listOf("0m"), success.task.alarmCascade)
        assertEquals(true, success.task.hasAlarm)
    }
    
    @Test
    fun `L1 urgency sets EXCLUSIVE policy and smart alarm`() {
        val l1Json = """
            {
                "title": "重要面试",
                "startTime": "2026-02-03 14:00",
                "urgency": "L1"
            }
        """.trimIndent()
        
        val result = linter.lint(l1Json)
        
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(UrgencyLevel.L1_CRITICAL, success.urgencyLevel)
        assertEquals(ConflictPolicy.EXCLUSIVE, success.task.conflictPolicy)
        assertTrue(success.task.isSmartAlarm)
    }
    
    @Test
    fun `lowercase urgency is normalized`() {
        val lowerCaseJson = """
            {
                "title": "测试",
                "startTime": "2026-02-03 14:00",
                "urgency": "l2"
            }
        """.trimIndent()
        
        val result = linter.lint(lowerCaseJson)
        
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(UrgencyLevel.L2_IMPORTANT, success.urgencyLevel)
    }
    
    @Test
    fun `unknown urgency string falls back to L3`() {
        val unknownJson = """
            {
                "title": "测试",
                "startTime": "2026-02-03 14:00",
                "urgency": "SOMETHING_ELSE"
            }
        """.trimIndent()
        
        val result = linter.lint(unknownJson)
        
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(UrgencyLevel.L3_NORMAL, success.urgencyLevel)
    }
    
    @Test
    fun `explicit duration overrides default`() {
        val durationJson = """
            {
                "title": "短会",
                "startTime": "2026-02-03 14:00",
                "duration": "10m",
                "urgency": "L2"
            }
        """.trimIndent()
        
        val result = linter.lint(durationJson)
        
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(10, success.task.durationMinutes)
    }
    
    // Existing validation tests maintained below
    
    @Test
    fun `missing title returns Error`() {
        val noTitleJson = """
            {
                "startTime": "2026-02-03 03:00",
                "endTime": "2026-02-03 04:00"
            }
        """.trimIndent()
        val result = linter.lint(noTitleJson)
        assertTrue(result is LintResult.Error)
    }
    
    @Test
    fun `past date returns Error`() {
        val pastDateJson = """
            {
                "title": "过去",
                "startTime": "2020-01-01 10:00"
            }
        """.trimIndent()
        val result = linter.lint(pastDateJson)
        assertTrue(result is LintResult.Error)
    }
    
    @Test
    fun `classification non_intent returns NonIntent`() {
        val json = """{"classification": "non_intent"}"""
        val result = linter.lint(json)
        assertTrue(result is LintResult.NonIntent)
    }
    
    @Test
    fun `classification inspiration returns Inspiration`() {
        val json = """
            {
                "classification": "inspiration",
                "inspirationText": "Idea"
            }
        """.trimIndent()
        val result = linter.lint(json)
        assertTrue(result is LintResult.Inspiration)
    }
}
