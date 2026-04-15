package com.smartsales.prism.data.real

import com.smartsales.core.pipeline.RealUnifiedPipeline
import com.smartsales.core.pipeline.PipelineInput
import com.smartsales.core.pipeline.PipelineResult
import com.smartsales.core.pipeline.ParseResult
import com.smartsales.core.pipeline.QueryQuality
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.TokenUsage
import com.smartsales.core.context.RealContextBuilder
import com.smartsales.core.test.fakes.*
import com.smartsales.prism.data.fakes.FakePipelineTelemetry
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.rl.RlObservation
import com.smartsales.prism.domain.rl.HabitContext
import java.time.LocalDate
import com.smartsales.data.crm.writer.RealEntityWriter
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class L2EfficiencyOverloadTest {

    private lateinit var pipeline: RealUnifiedPipeline
    private lateinit var fakeTaskRepo: ScheduledTaskRepository
    private lateinit var fakeScheduleBoard: FakeScheduleBoard
    private lateinit var fakeAlarmScheduler: FakeAlarmScheduler
    private lateinit var fakeInputParserService: FakeInputParserService
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeExecutor: FakeExecutor
    private lateinit var fakeHabitListener: FakeHabitListener
    
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @Before
    fun setup() {
        fakeTimeProvider = FakeTimeProvider()
        fakeTimeProvider.fixedInstant = Instant.parse("2026-03-11T09:00:00Z")
        
        fakeTaskRepo = object : ScheduledTaskRepository {
            override fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>> = emptyFlow()
            override fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<SchedulerTimelineItem>> = emptyFlow()
            override suspend fun insertTask(task: ScheduledTask): String = "fake-task-${System.currentTimeMillis()}"
            override suspend fun batchInsertTasks(tasks: List<ScheduledTask>): List<String> = emptyList()
            override suspend fun rescheduleTask(oldTaskId: String, newTask: ScheduledTask) {}
            override suspend fun getTask(id: String): ScheduledTask? = null
            override suspend fun updateTask(task: ScheduledTask) {}
            override suspend fun upsertTask(task: com.smartsales.prism.domain.scheduler.ScheduledTask): String = task.id
            override suspend fun deleteItem(id: String) {}
            override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> = emptyList()
            override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? = null
            override fun observeByEntityId(entityId: String): kotlinx.coroutines.flow.Flow<List<ScheduledTask>> = kotlinx.coroutines.flow.emptyFlow()
        }
        
        val fakeRL = object : ReinforcementLearner {
            override suspend fun processObservations(observations: List<RlObservation>) {}
            override suspend fun loadUserHabits(): HabitContext = HabitContext(emptyList(), emptyList(), emptyMap())
            override suspend fun loadClientHabits(entityIds: List<String>): HabitContext = HabitContext(emptyList(), emptyList(), emptyMap())
        }
        fakeScheduleBoard = FakeScheduleBoard()
        fakeAlarmScheduler = FakeAlarmScheduler()
        fakeExecutor = FakeExecutor()
        fakeInputParserService = FakeInputParserService()
        fakeHabitListener = FakeHabitListener()

        val contextBuilder = RealContextBuilder(
            timeProvider = fakeTimeProvider,
            reinforcementLearner = fakeRL,
            memoryRepository = FakeMemoryRepository(),
            entityRepository = FakeEntityRepository(),
            scheduledTaskRepository = fakeTaskRepo,
            historyRepository = FakeHistoryRepository(),
            telemetry = FakePipelineTelemetry()
        )

        val entityWriter = RealEntityWriter(
            entityRepository = FakeEntityRepository(),
            timeProvider = fakeTimeProvider,
            kernelWriteBack = contextBuilder,
            appScope = testScope
        )

        pipeline = RealUnifiedPipeline(
            contextBuilder = contextBuilder,
            entityDisambiguationService = FakeEntityDisambiguationService(),
            inputParserService = fakeInputParserService,
            schedulerLinter = SchedulerLinter(),
            entityWriter = entityWriter,
            promptCompiler = FakePromptCompiler(),
            executor = fakeExecutor,
            telemetry = FakePipelineTelemetry(),
            habitListener = fakeHabitListener,
            appScope = testScope
        )
    }

    @Test
    fun `Scenario 1 - The Multi-Task Burst`() = runTest {
        // Senior Reviewer Compliance: Inject raw JSON array so the SchedulerLinter does the actual breakdown.
        val rawJson = """
            {
              "classification": "schedulable",
              "tasks": [
                {
                  "title": "Task One",
                  "startTime": "2026-03-12 10:00",
                  "duration": "30m"
                },
                {
                  "title": "Task Two",
                  "startTime": "2026-03-12 14:00",
                  "duration": "45m"
                },
                {
                  "title": "Task Three",
                  "startTime": "2026-03-13 09:00",
                  "duration": "1h"
                }
              ]
            }
        """.trimIndent()

        fakeInputParserService.nextResult = ParseResult.Success(emptyList(), null, rawJson) // keep for title generation
        fakeExecutor.enqueueResponse(ExecutorResult.Success(rawJson, TokenUsage(10, 10)))

        val results = pipeline.processInput(PipelineInput("schedule 3 items", intent = QueryQuality.CRM_TASK, unifiedId = "test_unified_id")).toList()
        
        val taskCommand = results.filterIsInstance<PipelineResult.TaskCommandProposal>().firstOrNull()
        println("DEBUG SCENE 1 RESULTS: $results")
        assertNotNull("Must emit typed scheduler command", taskCommand)
        assertTrue(taskCommand!!.command is com.smartsales.core.pipeline.SchedulerTaskCommand.CreateTasks)
    }

    @Test
    fun `Scenario 2 - The Conflict Cascade`() = runTest {
        // We simulate a board collision natively via the fake schedule board
        fakeScheduleBoard.nextConflictResult = com.smartsales.prism.domain.memory.ConflictResult.Conflict(emptyList())

        val rawJson = """
            {
              "classification": "schedulable",
              "tasks": [
                {
                  "title": "Clean Task",
                  "startTime": "2026-03-13 10:00",
                  "duration": "30m"
                },
                {
                  "title": "Conflicting Task",
                  "startTime": "2026-03-12 10:00",
                  "duration": "30m"
                }
              ]
            }
        """.trimIndent()
        
        fakeInputParserService.nextResult = ParseResult.Success(emptyList(), null, rawJson)
        fakeExecutor.enqueueResponse(ExecutorResult.Success(rawJson, TokenUsage(10, 10)))
        val results = pipeline.processInput(PipelineInput("schedule overlap", intent = QueryQuality.CRM_TASK, unifiedId = "test_unified_id")).toList()
        
        val taskCommand = results.filterIsInstance<PipelineResult.TaskCommandProposal>().firstOrNull()
        println("DEBUG SCENE 2 RESULTS: $results")
        assertNotNull("Must emit typed scheduler command", taskCommand)
        assertTrue(taskCommand!!.command is com.smartsales.core.pipeline.SchedulerTaskCommand.CreateTasks)
    }

    @Test
    fun `Scenario 3 - The Alarm Array`() = runTest {
        val rawJson = """
            {
              "classification": "schedulable",
              "tasks": [
                {
                  "title": "Sync A",
                  "startTime": "2026-03-14 10:00",
                  "duration": "30m"
                },
                {
                  "title": "Sync B",
                  "startTime": "2026-03-14 11:00",
                  "duration": "30m"
                }
              ]
            }
        """.trimIndent()

        fakeAlarmScheduler.reset()

        fakeInputParserService.nextResult = ParseResult.Success(emptyList(), null, rawJson)
        fakeExecutor.enqueueResponse(ExecutorResult.Success(rawJson, TokenUsage(10, 10)))
        val results = pipeline.processInput(PipelineInput("schedule alarms", intent = QueryQuality.CRM_TASK, unifiedId = "test_unified_id")).toList()
        println("DEBUG SCENE 3 RESULTS: $results")
        
        // Assert: tasks naturally fell down the cascade queue and were proposed with alarms natively
        val taskCommand = results.filterIsInstance<PipelineResult.TaskCommandProposal>().firstOrNull()
        assertNotNull("Must emit typed scheduler command", taskCommand)
        assertTrue(taskCommand!!.command is com.smartsales.core.pipeline.SchedulerTaskCommand.CreateTasks)
    }
}
