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

    @Test
    fun `unknown task type defaults to 60min (PERSONAL)`() {
        val unknownTaskJson = """
            {
                "title": "其他事情",
                "startTime": "2026-02-03 14:00"
            }
        """.trimIndent()

        val result = linter.lint(unknownTaskJson)

        // Unknown type defaults to PERSONAL -> 60min
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(60, success.task.durationMinutes)
    }

    @Test
    fun `meeting title infers 60min duration`() {
        val meetingJson = """
            {
                "title": "开会",
                "startTime": "2026-02-03 14:00"
            }
        """.trimIndent()

        val result = linter.lint(meetingJson)

        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(60, success.task.durationMinutes)
    }

    @Test
    fun `call title infers 15min duration`() {
        val callJson = """
            {
                "title": "电话",
                "startTime": "2026-02-03 14:00"
            }
        """.trimIndent()

        val result = linter.lint(callJson)

        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(15, success.task.durationMinutes)
    }

    @Test
    fun `explicit duration string is parsed`() {
        val durationJson = """
            {
                "title": "测试",
                "startTime": "2026-02-03 14:00",
                "duration": "45m"
            }
        """.trimIndent()

        val result = linter.lint(durationJson)

        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(45, success.task.durationMinutes)
    }
    
    @Test
    fun `endTime calculates duration correctly`() {
        val endTimeJson = """
            {
                "title": "测试",
                "startTime": "2026-02-03 14:00",
                "endTime": "2026-02-03 15:30"
            }
        """.trimIndent()
        
        val result = linter.lint(endTimeJson)
        
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(90, success.task.durationMinutes)
    }
    
    // Wave 3: Smart Reminder Inference tests
    
    @Test
    fun `single reminder sets ReminderType SINGLE`() {
        val singleReminderJson = """
            {
                "title": "给张总打电话",
                "startTime": "2026-02-03 14:00",
                "reminder": "single"
            }
        """.trimIndent()
        
        val result = linter.lint(singleReminderJson)
        
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(ReminderType.SINGLE, success.reminderType)
        assertEquals(TaskTypeHint.CALL, success.taskTypeHint)
        assertEquals(listOf("-15m"), success.task.alarmCascade)
    }
    
    @Test
    fun `no reminder field sets no alarm`() {
        val noReminderJson = """
            {
                "title": "买牛奶",
                "startTime": "2026-02-03 14:00"
            }
        """.trimIndent()
        
        val result = linter.lint(noReminderJson)
        
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals(null, success.reminderType)
        assertEquals(false, success.task.hasAlarm)
    }
    
    // Wave 4.0: Input Classification tests
    
    @Test
    fun `classification non_intent returns NonIntent`() {
        val nonIntentJson = """
            {
                "classification": "non_intent"
            }
        """.trimIndent()
        
        val result = linter.lint(nonIntentJson)
        
        assertTrue("Expected LintResult.NonIntent, got: $result", result is LintResult.NonIntent)
    }
    
    @Test
    fun `classification inspiration returns Inspiration with content`() {
        val inspirationJson = """
            {
                "classification": "inspiration",
                "inspirationText": "以后想学吉他"
            }
        """.trimIndent()
        
        val result = linter.lint(inspirationJson)
        
        assertTrue("Expected LintResult.Inspiration, got: $result", result is LintResult.Inspiration)
        val inspiration = result as LintResult.Inspiration
        assertEquals("以后想学吉他", inspiration.content)
    }
    
    // Wave 4.1: Multi-Task Splitting tests
    
    @Test
    fun `tasks array with single item returns Success`() {
        val singleTaskArrayJson = """
            {
                "classification": "schedulable",
                "tasks": [
                    {
                        "title": "开会",
                        "startTime": "2026-02-03 14:00"
                    }
                ]
            }
        """.trimIndent()
        
        val result = linter.lint(singleTaskArrayJson)
        
        assertTrue("Expected LintResult.Success, got: $result", result is LintResult.Success)
        val success = result as LintResult.Success
        assertEquals("开会", success.task.title)
    }
    
    @Test
    fun `tasks array with multiple items returns MultiTask`() {
        val multiTaskJson = """
            {
                "classification": "schedulable",
                "tasks": [
                    {
                        "title": "吃面",
                        "startTime": "2026-02-03 08:00"
                    },
                    {
                        "title": "开会",
                        "startTime": "2026-02-03 09:00"
                    }
                ]
            }
        """.trimIndent()
        
        val result = linter.lint(multiTaskJson)
        
        assertTrue("Expected LintResult.MultiTask, got: $result", result is LintResult.MultiTask)
        val multi = result as LintResult.MultiTask
        assertEquals(2, multi.tasks.size)
        assertEquals("吃面", multi.tasks[0].title)
        assertEquals("开会", multi.tasks[1].title)
    }
}
