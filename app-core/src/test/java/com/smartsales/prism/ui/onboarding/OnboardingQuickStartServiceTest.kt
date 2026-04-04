package com.smartsales.prism.ui.onboarding

import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.LlmProfile
import com.smartsales.core.pipeline.PromptCompiler
import com.smartsales.core.pipeline.RealGlobalRescheduleExtractionService
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.RealUniBExtractionService
import com.smartsales.core.pipeline.RealUniMExtractionService
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingQuickStartServiceTest {

    private val executor = ScriptedExecutor()
    private val promptCompiler = PromptCompiler()
    private val schedulerLinter = SchedulerLinter()
    private val timeProvider = FakeTimeProvider()
    private val service = RealOnboardingQuickStartService(
        uniAExtractionService = RealUniAExtractionService(executor, promptCompiler, schedulerLinter),
        uniBExtractionService = RealUniBExtractionService(executor, promptCompiler, schedulerLinter),
        uniMExtractionService = RealUniMExtractionService(executor, promptCompiler, schedulerLinter),
        globalRescheduleExtractionService = RealGlobalRescheduleExtractionService(executor, promptCompiler, schedulerLinter),
        sandboxResolver = RealOnboardingQuickStartSandboxResolver(),
        timeProvider = timeProvider
    )

    @Test
    fun `single exact create skips uni m and returns exact preview item`() = runTest {
        executor.uniAResponse = ExecutorResult.Success(
            """
            {
              "decision":"EXACT_CREATE",
              "task":{
                "title":"带合同见老板",
                "startTimeIso":"2026-04-04T09:00:00+08:00",
                "durationMinutes":0,
                "urgency":"L2"
              }
            }
            """.trimIndent()
        )

        val result = service.applyTranscript(
            transcript = "明天早上九点带合同见老板",
            currentItems = emptyList()
        ) as OnboardingQuickStartServiceResult.Success

        assertEquals(1, result.items.size)
        assertEquals("带合同见老板", result.items.first().title)
        assertEquals("09:00", result.items.first().timeLabel)
        assertEquals("明天", result.items.first().dateLabel)
        assertTrue(result.touchedExactTask)
        assertEquals(1, executor.uniMCalls)
        assertEquals(1, executor.uniACalls)
        assertEquals(0, executor.uniBCalls)
    }

    @Test
    fun `single vague create skips uni m and falls through uni b`() = runTest {
        executor.uniAResponse = ExecutorResult.Success(
            """
            {
              "decision":"NOT_EXACT",
              "reason":"date only"
            }
            """.trimIndent()
        )
        executor.uniBResponse = ExecutorResult.Success(
            """
            {
              "decision":"VAGUE_CREATE",
              "task":{
                "title":"联系王经理",
                "anchorDateIso":"2026-04-05",
                "timeHint":"白天",
                "urgency":"L2"
              }
            }
            """.trimIndent()
        )

        val result = service.applyTranscript(
            transcript = "后天联系王经理",
            currentItems = emptyList()
        ) as OnboardingQuickStartServiceResult.Success

        assertEquals(1, result.items.size)
        assertEquals("联系王经理", result.items.first().title)
        assertEquals("后天", result.items.first().dateLabel)
        assertFalse(result.touchedExactTask)
        assertEquals(1, executor.uniMCalls)
        assertEquals(1, executor.uniACalls)
        assertEquals(1, executor.uniBCalls)
    }

    @Test
    fun `clear multi create transcript invokes uni m before single task fallbacks`() = runTest {
        executor.uniMResponse = ExecutorResult.Success(
            """
            {
              "decision":"MULTI_CREATE",
              "fragments":[
                {
                  "title":"带合同见老板",
                  "mode":"EXACT",
                  "anchorKind":"ABSOLUTE",
                  "startTimeIso":"2026-04-04T09:00:00+08:00",
                  "durationMinutes":0,
                  "urgency":"L2"
                },
                {
                  "title":"联系王经理",
                  "mode":"VAGUE",
                  "anchorKind":"ABSOLUTE",
                  "anchorDateIso":"2026-04-05",
                  "timeHint":"下午",
                  "urgency":"L2"
                }
              ]
            }
            """.trimIndent()
        )

        val result = service.applyTranscript(
            transcript = "明天早上九点带合同见老板，然后后天下午联系王经理",
            currentItems = emptyList()
        ) as OnboardingQuickStartServiceResult.Success

        assertEquals(2, result.items.size)
        assertEquals(1, executor.uniMCalls)
        assertEquals(0, executor.uniACalls)
        assertEquals(0, executor.uniBCalls)
    }

    @Test
    fun `chained day clock quick start creates same day exact tasks before uni fallbacks`() = runTest {
        val result = service.applyTranscript(
            transcript = "明天早上7点叫我起床，9点要带合同去见老板",
            currentItems = emptyList()
        ) as OnboardingQuickStartServiceResult.Success

        assertEquals(2, result.items.size)
        assertEquals("叫我起床", result.items[0].title)
        assertEquals("07:00", result.items[0].timeLabel)
        assertEquals("明天", result.items[0].dateLabel)
        assertEquals("带合同去见老板", result.items[1].title)
        assertEquals("09:00", result.items[1].timeLabel)
        assertEquals("明天", result.items[1].dateLabel)
        assertTrue(result.touchedExactTask)
        assertEquals(0, executor.uniMCalls)
        assertEquals(0, executor.uniACalls)
        assertEquals(0, executor.uniBCalls)
    }


    @Test
    fun `uni m timeout falls through to shared single task parsing`() = runTest {
        executor.uniMDelayMs = 2_000L
        executor.uniAResponse = ExecutorResult.Success(
            """
            {
              "decision":"EXACT_CREATE",
              "task":{
                "title":"带合同见老板",
                "startTimeIso":"2026-04-04T09:00:00+08:00",
                "durationMinutes":0,
                "urgency":"L2"
              }
            }
            """.trimIndent()
        )

        val result = service.applyTranscript(
            transcript = "明天早上九点带合同见老板",
            currentItems = emptyList()
        ) as OnboardingQuickStartServiceResult.Success

        assertEquals(1, result.items.size)
        assertEquals(1, executor.uniMCalls)
        assertEquals(1, executor.uniACalls)
        assertTrue(executor.executedPrompts.any { it.contains("displayed_date_iso: null") })
    }

    @Test
    fun `reschedule transcript updates staged vague last item through sandbox target resolution`() = runTest {
        executor.globalRescheduleResponse = ExecutorResult.Success("""
            {
              "decision":"RESCHEDULE_TARGETED",
              "suggestedTaskId":"task-1",
              "targetQuery":"最后一项",
              "timeInstruction":"大后天"
            }
        """.trimIndent())
        val result = service.applyTranscript(
            transcript = "把最后一项推迟到大后天",
            currentItems = listOf(
                OnboardingQuickStartItem(
                    stableId = "task-1",
                    title = "赶飞机",
                    timeLabel = "待定",
                    dateLabel = "后天",
                    dateIso = "2026-04-05",
                    urgencyLevel = UrgencyLevel.L2_IMPORTANT
                )
            )
        ) as OnboardingQuickStartServiceResult.Success

        assertEquals(OnboardingQuickStartServiceResult.Success.MutationKind.UPDATE, result.mutationKind)
        assertEquals("大后天", result.items.first().dateLabel)
        assertEquals("2026-04-06", result.items.first().dateIso)
        assertEquals(1, result.items.first().highlightToken)
    }

    @Test
    fun `actually no longer forces onboarding quick start into reschedule lane`() = runTest {
        executor.uniAResponse = ExecutorResult.Success(
            """
            {
              "decision":"EXACT_CREATE",
              "task":{
                "title":"带合同见老板",
                "startTimeIso":"2026-04-04T09:00:00+08:00",
                "durationMinutes":0,
                "urgency":"L2"
              }
            }
            """.trimIndent()
        )

        val result = service.applyTranscript(
            transcript = "actually 明天早上九点带合同见老板",
            currentItems = listOf(
                OnboardingQuickStartItem(
                    stableId = "task-1",
                    title = "赶飞机",
                    timeLabel = "待定",
                    dateLabel = "后天",
                    dateIso = "2026-04-05",
                    urgencyLevel = UrgencyLevel.L2_IMPORTANT
                )
            )
        ) as OnboardingQuickStartServiceResult.Success

        assertEquals(OnboardingQuickStartServiceResult.Success.MutationKind.CREATE, result.mutationKind)
        assertEquals(2, result.items.size)
        assertEquals(0, executor.globalRescheduleCalls)
        assertEquals(1, executor.uniACalls)
        assertEquals("带合同见老板", result.items.last().title)
    }

    private class ScriptedExecutor : Executor {
        var uniMResponse: ExecutorResult = ExecutorResult.Failure("missing Uni-M response")
        var uniAResponse: ExecutorResult = ExecutorResult.Failure("missing Uni-A response")
        var uniBResponse: ExecutorResult = ExecutorResult.Failure("missing Uni-B response")
        var globalRescheduleResponse: ExecutorResult = ExecutorResult.Failure("missing global response")
        var uniMCalls: Int = 0
        var uniACalls: Int = 0
        var uniBCalls: Int = 0
        var globalRescheduleCalls: Int = 0
        var uniMDelayMs: Long = 0L
        val executedPrompts = mutableListOf<String>()

        override suspend fun execute(profile: LlmProfile, prompt: String): ExecutorResult {
            executedPrompts += prompt
            return when {
                prompt.contains("多任务拆解器") -> {
                    uniMCalls += 1
                    if (uniMDelayMs > 0) delay(uniMDelayMs)
                    uniMResponse
                }
                prompt.contains("轻量精确提取器") -> {
                    uniACalls += 1
                    uniAResponse
                }
                prompt.contains("轻量模糊提取器") -> {
                    uniBCalls += 1
                    uniBResponse
                }
                prompt.contains("全局改期提取器") -> {
                    globalRescheduleCalls += 1
                    globalRescheduleResponse
                }
                else -> ExecutorResult.Failure("unexpected prompt")
            }
        }
    }

    private class FakeTimeProvider : TimeProvider {
        override val now: Instant = Instant.parse("2026-04-03T08:00:00Z")
        override val today: LocalDate = LocalDate.parse("2026-04-03")
        override val currentTime: LocalTime = LocalTime.of(16, 0)
        override val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")

        override fun formatForLlm(): String = "2026年4月3日（周五）16:00"
    }
}
