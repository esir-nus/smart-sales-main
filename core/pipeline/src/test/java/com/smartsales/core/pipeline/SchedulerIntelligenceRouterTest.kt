package com.smartsales.core.pipeline

import com.smartsales.core.test.fakes.FakeExecutor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.prism.domain.scheduler.ActiveTaskContext
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerIntelligenceRouterTest {

    private val timeProvider = FakeTimeProvider().apply {
        fixedInstant = java.time.Instant.parse("2026-03-20T09:00:00Z")
    }

    private val executor = FakeExecutor()
    private val linter = SchedulerLinter(timeProvider)
    private val createInterpreter = SchedulerPathACreateInterpreter(
        uniMExtractionService = RealUniMExtractionService(
            executor = executor,
            promptCompiler = PromptCompiler(),
            schedulerLinter = linter
        ),
        uniAExtractionService = RealUniAExtractionService(
            executor = executor,
            promptCompiler = PromptCompiler(),
            schedulerLinter = linter
        ),
        uniBExtractionService = RealUniBExtractionService(
            executor = executor,
            promptCompiler = PromptCompiler(),
            schedulerLinter = linter
        ),
        timeProvider = timeProvider
    )
    private val router = SchedulerIntelligenceRouter(
        timeProvider = timeProvider,
        createInterpreter = createInterpreter
    )

    @Test
    fun `routeGeneral keeps qualified weekday explicit clock in shared exact create lane`() = runTest {
        val decision = router.routeGeneral(
            SchedulerIntelligenceRouter.GeneralContext(
                transcript = "下周三早上八点钟提醒我起床",
                surface = SchedulerIntelligenceRouter.SchedulerSurface.SCHEDULER_DRAWER,
                displayedDateIso = null
            )
        )

        assertTrue(decision is SchedulerIntelligenceRouter.Decision.Create)
        val create = decision as SchedulerIntelligenceRouter.Decision.Create
        assertTrue(create.result is SchedulerPathACreateInterpreter.Result.SingleMatched)
        val matched = create.result as SchedulerPathACreateInterpreter.Result.SingleMatched
        val intent = matched.intent as FastTrackResult.CreateTasks
        val task = intent.params.tasks.single()

        assertEquals(SchedulerPathACreateInterpreter.RouteStage.DETERMINISTIC_DAY_CLOCK, matched.telemetry.routeStage)
        assertEquals("起床", task.title)
        assertEquals("2026-03-25T08:00+08:00", task.startTimeIso)
    }

    @Test
    fun `routeGeneral treats cancel wording with replacement event as global reschedule`() = runTest {
        val globalExecutor = FakeExecutor()
        val router = SchedulerIntelligenceRouter(
            timeProvider = timeProvider,
            createInterpreter = createInterpreter,
            globalRescheduleExtractionService = RealGlobalRescheduleExtractionService(
                executor = globalExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = linter
            )
        )
        globalExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision":"RESCHEDULE_TARGETED",
                  "targetQuery":"晚上8点的任务",
                  "timeInstruction":"晚上8点",
                  "newTitle":"去机场接人"
                }
                """.trimIndent()
            )
        )

        val decision = router.routeGeneral(
            SchedulerIntelligenceRouter.GeneralContext(
                transcript = "晚上8点的开会取消了，得去机场接人。",
                surface = SchedulerIntelligenceRouter.SchedulerSurface.SCHEDULER_DRAWER,
                displayedDateIso = "2026-03-20",
                activeTaskShortlist = listOf(
                    ActiveTaskContext(
                        taskId = "task-20",
                        title = "开会",
                        timeSummary = "20:00",
                        isVague = false
                    )
                )
            )
        )

        assertTrue(decision is SchedulerIntelligenceRouter.Decision.GlobalReschedule)
        val reschedule = decision as SchedulerIntelligenceRouter.Decision.GlobalReschedule
        assertEquals(SchedulerIntelligenceRouter.SchedulerIntentKind.RESCHEDULE, reschedule.metadata.intentKind)
        assertEquals("晚上8点", reschedule.extracted.timeInstruction)
        assertEquals("去机场接人", reschedule.extracted.newTitle)
    }

    @Test
    fun `routeGeneral still rejects pure cancel wording`() = runTest {
        val globalExecutor = FakeExecutor()
        val router = SchedulerIntelligenceRouter(
            timeProvider = timeProvider,
            createInterpreter = createInterpreter,
            globalRescheduleExtractionService = RealGlobalRescheduleExtractionService(
                executor = globalExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = linter
            )
        )

        val decision = router.routeGeneral(
            SchedulerIntelligenceRouter.GeneralContext(
                transcript = "取消晚上8点的开会",
                surface = SchedulerIntelligenceRouter.SchedulerSurface.SCHEDULER_DRAWER,
                displayedDateIso = "2026-03-20"
            )
        )

        assertTrue(decision is SchedulerIntelligenceRouter.Decision.Reject)
        assertEquals(
            SchedulerIntelligenceRouter.SchedulerIntentKind.DELETE_UNSUPPORTED,
            decision.metadata.intentKind
        )
        assertTrue(globalExecutor.executedPrompts.isEmpty())
    }
}
