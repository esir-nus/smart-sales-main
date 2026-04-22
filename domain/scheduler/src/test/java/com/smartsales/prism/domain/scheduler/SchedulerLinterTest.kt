package com.smartsales.prism.domain.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime

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

    @Test
    fun `Uni-A exact extraction returns CreateTasks with unifiedId`() {
        val json = """
            {
              "decision": "EXACT_CREATE",
              "task": {
                "title": "开会",
                "startTimeIso": "2026-03-18T02:00:00Z",
                "durationMinutes": 45,
                "urgency": "L2"
              }
            }
        """.trimIndent()

        val result = linter.parseUniAExtraction(json, unifiedId = "uni-a-001")

        assertTrue(result is FastTrackResult.CreateTasks)
        val success = result as FastTrackResult.CreateTasks
        assertEquals("uni-a-001", success.params.unifiedId)
        assertEquals(1, success.params.tasks.size)
        assertEquals("开会", success.params.tasks.first().title)
        assertEquals("2026-03-18T02:00:00Z", success.params.tasks.first().startTimeIso)
        assertEquals(45, success.params.tasks.first().durationMinutes)
        assertEquals(UrgencyEnum.L2_IMPORTANT, success.params.tasks.first().urgency)
    }

    @Test
    fun `Uni-A not exact returns NoMatch`() {
        val json = """
            {
              "decision": "NOT_EXACT",
              "reason": "缺少明确时间"
            }
        """.trimIndent()

        val result = linter.parseUniAExtraction(json, unifiedId = "uni-a-002")

        assertTrue(result is FastTrackResult.NoMatch)
        assertEquals("缺少明确时间", (result as FastTrackResult.NoMatch).reason)
    }

    @Test
    fun `Uni-A exact extraction rejects fabricated time for date-only tomorrow input`() {
        val json = """
            {
              "decision": "EXACT_CREATE",
              "task": {
                "title": "提醒我打车去机场",
                "startTimeIso": "2026-03-19T04:44:30.211935Z",
                "durationMinutes": 0,
                "urgency": "L2"
              }
            }
        """.trimIndent()

        val result = linter.parseUniAExtraction(
            input = json,
            unifiedId = "uni-a-003",
            transcript = "明天提醒我打车去机场"
        )

        assertTrue(result is FastTrackResult.NoMatch)
        assertTrue((result as FastTrackResult.NoMatch).reason.contains("date-only input"))
    }

    @Test
    fun `Uni-A exact extraction normalizes houtian against real today not displayed page`() {
        val json = """
            {
              "decision": "EXACT_CREATE",
              "task": {
                "title": "和张总吃饭",
                "startTimeIso": "2026-03-17T20:00:00+08:00",
                "durationMinutes": 90,
                "urgency": "L2"
              }
            }
        """.trimIndent()

        val result = linter.parseUniAExtraction(
            input = json,
            unifiedId = "uni-a-004",
            transcript = "后天晚上八点和张总吃饭",
            nowIso = "2026-03-18T08:00:00+08:00",
            timezone = "Asia/Shanghai",
            displayedDateIso = "2026-03-15"
        )

        assertTrue(result is FastTrackResult.CreateTasks)
        val success = result as FastTrackResult.CreateTasks
        assertEquals(
            OffsetDateTime.parse("2026-03-20T20:00:00+08:00"),
            OffsetDateTime.parse(success.params.tasks.first().startTimeIso)
        )
    }

    @Test
    fun `Uni-A exact extraction normalizes hou yi tian against displayed page`() {
        val json = """
            {
              "decision": "EXACT_CREATE",
              "task": {
                "title": "和李总吃饭",
                "startTimeIso": "2026-03-19T18:30:00+08:00",
                "durationMinutes": 60,
                "urgency": "L2"
              }
            }
        """.trimIndent()

        val result = linter.parseUniAExtraction(
            input = json,
            unifiedId = "uni-a-005",
            transcript = "后一天晚上六点半和李总吃饭",
            nowIso = "2026-03-18T08:00:00+08:00",
            timezone = "Asia/Shanghai",
            displayedDateIso = "2026-03-19"
        )

        assertTrue(result is FastTrackResult.CreateTasks)
        val success = result as FastTrackResult.CreateTasks
        assertEquals(
            OffsetDateTime.parse("2026-03-20T18:30:00+08:00"),
            OffsetDateTime.parse(success.params.tasks.first().startTimeIso)
        )
    }

    @Test
    fun `Uni-B vague extraction returns CreateVagueTask with unifiedId`() {
        val json = """
            {
              "decision": "VAGUE_CREATE",
              "task": {
                "title": "提醒我开会",
                "anchorDateIso": "2026-03-21",
                "timeHint": "下午",
                "urgency": "L3"
              }
            }
        """.trimIndent()

        val result = linter.parseUniBExtraction(json, unifiedId = "uni-b-001")

        assertTrue(result is FastTrackResult.CreateVagueTask)
        val success = result as FastTrackResult.CreateVagueTask
        assertEquals("uni-b-001", success.params.unifiedId)
        assertEquals("提醒我开会", success.params.title)
        assertEquals("2026-03-21", success.params.anchorDateIso)
        assertEquals("下午", success.params.timeHint)
        assertEquals(UrgencyEnum.L3_NORMAL, success.params.urgency)
    }

    @Test
    fun `Uni-B not vague returns NoMatch`() {
        val json = """
            {
              "decision": "NOT_VAGUE",
              "reason": "没有日期锚点"
            }
        """.trimIndent()

        val result = linter.parseUniBExtraction(json, unifiedId = "uni-b-002")

        assertTrue(result is FastTrackResult.NoMatch)
        assertEquals("没有日期锚点", (result as FastTrackResult.NoMatch).reason)
    }

    @Test
    fun `Uni-B extraction with explicit clock cue normalizes houtian anchor and promotes to exact`() {
        val json = """
            {
              "decision": "VAGUE_CREATE",
              "task": {
                "title": "要去趟高铁站",
                "anchorDateIso": "2026-03-17",
                "timeHint": "早上八点",
                "urgency": "L3"
              }
            }
        """.trimIndent()

        val result = linter.parseUniBExtraction(
            input = json,
            unifiedId = "uni-b-003",
            transcript = "后天要去趟高铁站",
            nowIso = "2026-03-18T08:00:00+08:00",
            timezone = "Asia/Shanghai",
            displayedDateIso = "2026-03-15"
        )

        assertTrue(result is FastTrackResult.CreateTasks)
        val success = result as FastTrackResult.CreateTasks
        assertEquals(
            OffsetDateTime.parse("2026-03-20T08:00:00+08:00"),
            OffsetDateTime.parse(success.params.tasks.first().startTimeIso)
        )
    }

    @Test
    fun `Uni-B vague extraction normalizes xia yi tian anchor against displayed page`() {
        val json = """
            {
              "decision": "VAGUE_CREATE",
              "task": {
                "title": "提醒我吃饭",
                "anchorDateIso": "2026-03-19",
                "timeHint": "晚上",
                "urgency": "L3"
              }
            }
        """.trimIndent()

        val result = linter.parseUniBExtraction(
            input = json,
            unifiedId = "uni-b-004",
            transcript = "下一天提醒我吃饭",
            nowIso = "2026-03-18T08:00:00+08:00",
            timezone = "Asia/Shanghai",
            displayedDateIso = "2026-03-19"
        )

        assertTrue(result is FastTrackResult.CreateVagueTask)
        val success = result as FastTrackResult.CreateVagueTask
        assertEquals("2026-03-20", success.params.anchorDateIso)
    }

    @Test
    fun `Uni-B extraction promotes explicit clock cue into exact create`() {
        val json = """
            {
              "decision": "VAGUE_CREATE",
              "task": {
                "title": "去接李总",
                "anchorDateIso": "2026-03-20",
                "timeHint": "晚上九点",
                "urgency": "L2"
              }
            }
        """.trimIndent()

        val result = linter.parseUniBExtraction(
            input = json,
            unifiedId = "uni-b-005",
            transcript = "后天晚上九点去接李总",
            nowIso = "2026-03-18T08:00:00+08:00",
            timezone = "Asia/Shanghai",
            displayedDateIso = "2026-03-15"
        )

        assertTrue(result is FastTrackResult.CreateTasks)
        val success = result as FastTrackResult.CreateTasks
        assertEquals("uni-b-005", success.params.unifiedId)
        assertEquals("去接李总", success.params.tasks.first().title)
        assertEquals(
            OffsetDateTime.parse("2026-03-20T21:00:00+08:00"),
            OffsetDateTime.parse(success.params.tasks.first().startTimeIso)
        )
        assertEquals(UrgencyEnum.L2_IMPORTANT, success.params.tasks.first().urgency)
    }

    @Test
    fun `Uni-C inspiration extraction returns CreateInspiration with unifiedId`() {
        val json = """
            {
              "decision": "INSPIRATION_CREATE",
              "idea": {
                "content": "以后想练口语",
                "title": "口语想法"
              }
            }
        """.trimIndent()

        val result = linter.parseUniCExtraction(json, unifiedId = "uni-c-001")

        assertTrue(result is FastTrackResult.CreateInspiration)
        val success = result as FastTrackResult.CreateInspiration
        assertEquals("uni-c-001", success.params.unifiedId)
        assertEquals("以后想练口语", success.params.content)
    }

    @Test
    fun `Uni-C inspiration extraction falls back to title when content is blank`() {
        val json = """
            {
              "decision": "INSPIRATION_CREATE",
              "idea": {
                "content": "   ",
                "title": "以后想学吉他"
              }
            }
        """.trimIndent()

        val result = linter.parseUniCExtraction(
            input = json,
            unifiedId = "uni-c-002",
            transcript = "原始语音"
        )

        assertTrue(result is FastTrackResult.CreateInspiration)
        assertEquals(
            "以后想学吉他",
            (result as FastTrackResult.CreateInspiration).params.content
        )
    }

    @Test
    fun `Uni-C inspiration extraction falls back to transcript when model omits content`() {
        val json = """
            {
              "decision": "INSPIRATION_CREATE",
              "idea": {
                "content": "",
                "title": null
              }
            }
        """.trimIndent()

        val result = linter.parseUniCExtraction(
            input = json,
            unifiedId = "uni-c-003",
            transcript = "以后想学吉他"
        )

        assertTrue(result is FastTrackResult.CreateInspiration)
        assertEquals(
            "以后想学吉他",
            (result as FastTrackResult.CreateInspiration).params.content
        )
    }

    @Test
    fun `Uni-C not inspiration returns NoMatch`() {
        val json = """
            {
              "decision": "NOT_INSPIRATION",
              "reason": "仍然属于 schedulable"
            }
        """.trimIndent()

        val result = linter.parseUniCExtraction(json, unifiedId = "uni-c-002")

        assertTrue(result is FastTrackResult.NoMatch)
        assertEquals("仍然属于 schedulable", (result as FastTrackResult.NoMatch).reason)
    }

    @Test
    fun `follow-up reschedule V2 delta extraction rejects out of bounds offset`() {
        val json = """
            {
              "decision": "RESCHEDULE_EXACT",
              "timeKind": "DELTA_FROM_TARGET",
              "deltaFromTargetMinutes": 20161
            }
        """.trimIndent()

        val result = linter.parseFollowUpRescheduleExtraction(
            input = json,
            transcript = "推迟15天"
        )

        assertTrue(result is FollowUpRescheduleExtractionResult.Invalid)
        assertTrue(
            (result as FollowUpRescheduleExtractionResult.Invalid)
                .reason.contains("+/-20160")
        )
    }

    @Test
    fun `follow-up reschedule V2 relative day clock rejects page relative phrasing in first experiment`() {
        val json = """
            {
              "decision": "RESCHEDULE_EXACT",
              "timeKind": "RELATIVE_DAY_CLOCK",
              "relativeDayOffset": 1,
              "clockTime": "08:00"
            }
        """.trimIndent()

        val result = linter.parseFollowUpRescheduleExtraction(
            input = json,
            transcript = "改到后一天早上8点"
        )

        assertTrue(result is FollowUpRescheduleExtractionResult.Unsupported)
        assertTrue(
            (result as FollowUpRescheduleExtractionResult.Unsupported)
                .reason.contains("page-relative")
        )
    }

    @Test
    fun `follow-up reschedule V2 absolute extraction rejects illegal extra fields`() {
        val json = """
            {
              "decision": "RESCHEDULE_EXACT",
              "timeKind": "ABSOLUTE",
              "absoluteStartIso": "2026-03-23T08:00:00+08:00",
              "clockTime": "08:00"
            }
        """.trimIndent()

        val result = linter.parseFollowUpRescheduleExtraction(
            input = json,
            transcript = "改到2026-03-23 08:00"
        )

        assertTrue(result is FollowUpRescheduleExtractionResult.Invalid)
        assertTrue(
            (result as FollowUpRescheduleExtractionResult.Invalid)
                .reason.contains("illegal extra fields")
        )
    }

    @Test
    fun `global reschedule extraction returns supported target and time instruction`() {
        val json = """
            {
              "decision": "RESCHEDULE_TARGETED",
              "targetQuery": "和张总吃饭",
              "targetPerson": "张总",
              "timeInstruction": "明天晚上八点"
            }
        """.trimIndent()

        val result = linter.parseGlobalRescheduleExtraction(json)

        assertTrue(result is GlobalRescheduleExtractionResult.Supported)
        val supported = result as GlobalRescheduleExtractionResult.Supported
        assertEquals("和张总吃饭", supported.target.targetQuery)
        assertEquals("张总", supported.target.targetPerson)
        assertEquals("明天晚上八点", supported.timeInstruction)
    }

    @Test
    fun `global reschedule extraction rejects targeted payload without target clues`() {
        val json = """
            {
              "decision": "RESCHEDULE_TARGETED",
              "timeInstruction": "推迟一小时"
            }
        """.trimIndent()

        val result = linter.parseGlobalRescheduleExtraction(json)

        assertTrue(result is GlobalRescheduleExtractionResult.Invalid)
        assertTrue((result as GlobalRescheduleExtractionResult.Invalid).reason.contains("target clues"))
    }
}
