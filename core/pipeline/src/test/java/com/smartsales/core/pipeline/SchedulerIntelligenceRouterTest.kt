package com.smartsales.core.pipeline

import com.smartsales.core.test.fakes.FakeExecutor
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
}
