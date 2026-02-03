package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.data.fakes.FakeTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SchedulerLinter 单元测试
 * 验证 LLM 输出解析和日期验证逻辑
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
    fun `valid JSON returns Success with task`() {
        // 注意: 格式是 "yyyy-MM-dd HH:mm" (空格分隔，不是T)
        val validJson = """
            {
                "title": "赶飞机",
                "startTime": "2026-02-03 03:00",
                "endTime": "2026-02-03 04:00",
                "reminder": "smart"
            }
        """.trimIndent()
        
        val result = linter.lint(validJson)
        
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals("赶飞机", success.task.title)
        assertEquals("03:00", success.task.timeDisplay)
        assertTrue(success.task.isSmartAlarm)
        assertEquals(ReminderType.SMART_CASCADE, success.reminderType)
    }
    
    @Test
    fun `missing title returns Error`() {
        val noTitleJson = """
            {
                "startTime": "2026-02-03 03:00",
                "endTime": "2026-02-03 04:00"
            }
        """.trimIndent()
        
        val result = linter.lint(noTitleJson)
        
        assertTrue("Expected LintResult.Error, got: $result", result is LintResult.Error)
        assertTrue((result as LintResult.Error).message.contains("标题"))
    }
    
    @Test
    fun `end before start returns Error`() {
        val invalidRangeJson = """
            {
                "title": "开会",
                "startTime": "2026-02-03 10:00",
                "endTime": "2026-02-03 09:00"
            }
        """.trimIndent()
        
        val result = linter.lint(invalidRangeJson)
        
        assertTrue("Expected LintResult.Error, got: $result", result is LintResult.Error)
        assertTrue((result as LintResult.Error).message.contains("结束时间"))
    }
    
    @Test
    fun `past date returns Error`() {
        val pastDateJson = """
            {
                "title": "过去的任务",
                "startTime": "2020-01-01 10:00",
                "endTime": "2020-01-01 11:00"
            }
        """.trimIndent()
        
        val result = linter.lint(pastDateJson)
        
        assertTrue("Expected LintResult.Error, got: $result", result is LintResult.Error)
        assertTrue((result as LintResult.Error).message.contains("过去"))
    }
    
    @Test
    fun `smart reminder sets ReminderType SMART_CASCADE`() {
        val smartReminderJson = """
            {
                "title": "紧急会议",
                "startTime": "2026-02-03 14:00",
                "endTime": "2026-02-03 15:00",
                "reminder": "smart"
            }
        """.trimIndent()
        
        val result = linter.lint(smartReminderJson)
        
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(ReminderType.SMART_CASCADE, success.reminderType)
        // 注意: "紧急会议" contains "会议" which matches first, so it's MEETING not URGENT
        assertEquals(TaskTypeHint.MEETING, success.taskTypeHint)
    }
    
    @Test
    fun `malformed JSON returns Error`() {
        val malformedJson = "{not valid json}"
        
        val result = linter.lint(malformedJson)
        
        assertTrue("Expected LintResult.Error, got: $result", result is LintResult.Error)
        assertTrue((result as LintResult.Error).message.contains("JSON"))
    }
}
