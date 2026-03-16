package com.smartsales.prism.domain.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SchedulerLinter 单元测试 (Path A Wave 17 DTO Mapping)
 * 验证 LLM 输出解析 (UrgencyLevel) 和日期验证逻辑，使用严苛的 kotlinx.serialization
 * 并确保产出的是纯粹的 DTO，不带有业务领域逻辑污染
 */
class SchedulerLinterTest {
    
    private lateinit var linter: SchedulerLinter
    
    @Before
    fun setup() {
        linter = SchedulerLinter()
    }
    
    @Test
    fun `valid JSON returns CreateTasks DTO`() {
        val validJson = """
            {
                "classification": "schedulable",
                "tasks": [
                    {
                        "title": "赶飞机",
                        "startTime": "2026-02-03T03:00:00Z",
                        "urgency": "L1"
                    }
                ]
            }
        """.trimIndent()
        
        val result = linter.parseFastTrackIntent(validJson)
        
        assertTrue("Expected CreateTasks, got: $result", result is FastTrackResult.CreateTasks)
        val success = result as FastTrackResult.CreateTasks
        assertEquals(1, success.params.tasks.size)
        val task = success.params.tasks.first()
        assertEquals("赶飞机", task.title)
        assertEquals("2026-02-03T03:00:00Z", task.startTimeIso)
        assertEquals(UrgencyEnum.L1_CRITICAL, task.urgency)
    }
    
    @Test
    fun `missing urgency defaults to L3`() {
        val noUrgencyJson = """
            {
                "classification": "schedulable",
                "tasks": [
                    {
                        "title": "买牛奶",
                        "startTime": "2026-02-03 14:00"
                    }
                ]
            }
        """.trimIndent()
        
        val result = linter.parseFastTrackIntent(noUrgencyJson)
        
        assertTrue("Expected CreateTasks, got: $result", result is FastTrackResult.CreateTasks)
        val success = result as FastTrackResult.CreateTasks
        assertEquals(UrgencyEnum.L3_NORMAL, success.params.tasks.first().urgency)
    }
    
    @Test
    fun `FIRE_OFF urgency mapped correctly`() {
        val fireOffJson = """
            {
                "classification": "schedulable",
                "tasks": [
                    {
                        "title": "喝水",
                        "startTime": "2026-02-03 10:15",
                        "urgency": "FIRE_OFF"
                    }
                ]
            }
        """.trimIndent()
        
        val result = linter.parseFastTrackIntent(fireOffJson)
        
        assertTrue("Expected CreateTasks, got: $result", result is FastTrackResult.CreateTasks)
        val success = result as FastTrackResult.CreateTasks
        assertEquals(UrgencyEnum.FIRE_OFF, success.params.tasks.first().urgency)
    }
    
    @Test
    fun `lowercase urgency is normalized`() {
        val lowerCaseJson = """
            {
                "classification": "schedulable",
                "tasks": [
                    {
                        "title": "测试",
                        "startTime": "2026-02-03 14:00",
                        "urgency": "l2"
                    }
                ]
            }
        """.trimIndent()
        
        val result = linter.parseFastTrackIntent(lowerCaseJson)
        
        assertTrue("Expected CreateTasks, got: $result", result is FastTrackResult.CreateTasks)
        val success = result as FastTrackResult.CreateTasks
        assertEquals(UrgencyEnum.L2_IMPORTANT, success.params.tasks.first().urgency)
    }
    
    @Test
    fun `explicit duration overrides default`() {
        val durationJson = """
            {
                "classification": "schedulable",
                "tasks": [
                    {
                        "title": "短会",
                        "startTime": "2026-02-03 14:00",
                        "duration": "10m",
                        "urgency": "L2"
                    }
                ]
            }
        """.trimIndent()
        
        val result = linter.parseFastTrackIntent(durationJson)
        
        assertTrue("Expected CreateTasks, got: $result", result is FastTrackResult.CreateTasks)
        val success = result as FastTrackResult.CreateTasks
        assertEquals(10, success.params.tasks.first().durationMinutes)
    }
    
    @Test
    fun `missing title returns NoMatch`() {
        val noTitleJson = """
            {
                "classification": "schedulable",
                "tasks": [
                    {
                        "startTime": "2026-02-03 03:00",
                        "endTime": "2026-02-03 04:00"
                    }
                ]
            }
        """.trimIndent()
        val result = linter.parseFastTrackIntent(noTitleJson)
        assertTrue(result is FastTrackResult.NoMatch)
    }
    
    @Test
    fun `classification non_intent returns NoMatch`() {
        val json = """{"classification": "non_intent", "reason": "Just chatting", "tasks": []}"""
        val result = linter.parseFastTrackIntent(json)
        assertTrue(result is FastTrackResult.NoMatch)
    }

    @Test
    fun `classification deletion returns NoMatch due to Path A constraints`() {
        val json = """{"classification": "deletion", "targetTitle": "the task"}"""
        val result = linter.parseFastTrackIntent(json)
        assertTrue(result is FastTrackResult.NoMatch)
    }
    
    @Test
    fun `classification inspiration with tasks returns CreateInspiration`() {
        val json = """
            {
                "classification": "inspiration", 
                "thought": "This is a thought", 
                "tasks": [
                    {"title": "Note", "startTime": "", "notes": "Learn guitar"}
                ]
            }
        """
        val result = linter.parseFastTrackIntent(json)
        assertTrue("Expected CreateInspiration, got $result", result is FastTrackResult.CreateInspiration)
        assertEquals("Learn guitar", (result as FastTrackResult.CreateInspiration).params.content)
    }

    @Test
    fun `invalid json degrades gracefully without crashing to NoMatch`() {
        val json = """
            {
                "classification": "schedulable",
                "recommended_workflows": [
                    {
                        "workflowId": null,
                        "parameters": "string_instead_of_object"
                    }
                ],
                "tasks": []
            }
        """.trimIndent()
        val result = linter.parseFastTrackIntent(json)
        // kotlinx.serialization will throw an exception on schema mismatch (null for non-null String, String for Map),
        // which the linter catches and returns as FastTrackResult.NoMatch. We expect it NOT to crash.
        assertTrue("Expected NoMatch due to SerializationException", result is FastTrackResult.NoMatch)
    }
}
