package com.smartsales.core.pipeline

import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.test.fakes.FakeExecutor
import com.smartsales.prism.domain.scheduler.ActiveTaskContext
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionRequest
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionResult
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealGlobalRescheduleExtractionServiceTest {

    @Test
    fun `extract filters suggestion and preferred ids to owned shortlist`() = runTest {
        val executor = FakeExecutor()
        val service = RealGlobalRescheduleExtractionService(
            executor = executor,
            promptCompiler = PromptCompiler(),
            schedulerLinter = SchedulerLinter()
        )
        executor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision":"RESCHEDULE_TARGETED",
                  "suggestedTaskId":"task-9",
                  "preferredTaskIds":["task-9","task-2","task-1"],
                  "targetQuery":"张总会议",
                  "timeInstruction":"明天上午十一点"
                }
                """.trimIndent()
            )
        )

        val result = service.extract(
            GlobalRescheduleExtractionRequest(
                transcript = "把张总会议改到明天上午十一点",
                nowIso = "2026-03-18T08:00:00Z",
                timezone = "Asia/Shanghai",
                activeTaskShortlist = listOf(
                    ActiveTaskContext(
                        taskId = "task-1",
                        title = "张总会议",
                        timeSummary = "明天 10:00",
                        isVague = false
                    ),
                    ActiveTaskContext(
                        taskId = "task-2",
                        title = "客户回访",
                        timeSummary = "明天 15:00",
                        isVague = false
                    )
                )
            )
        )

        assertTrue(result is GlobalRescheduleExtractionResult.Supported)
        val supported = result as GlobalRescheduleExtractionResult.Supported
        assertEquals("task-2", supported.suggestedTaskId)
        assertEquals(listOf("task-2", "task-1"), supported.preferredTaskIds)
    }
}
