package com.smartsales.prism.data.real

import com.smartsales.core.pipeline.RealUnifiedPipeline
import com.smartsales.core.pipeline.PipelineInput
import com.smartsales.core.pipeline.PipelineResult
import com.smartsales.core.pipeline.ParseResult
import com.smartsales.core.pipeline.DisambiguationResult
import com.smartsales.core.pipeline.QueryQuality
import com.smartsales.core.context.ContextDepth
import com.smartsales.core.context.RealContextBuilder
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.test.fakes.*
import com.smartsales.prism.data.fakes.FakePipelineTelemetry
import com.smartsales.prism.data.fakes.FakeTimeProvider
import com.smartsales.prism.data.rl.RealReinforcementLearner
import com.smartsales.data.crm.writer.RealEntityWriter
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class L2StrictInterfaceIntegrityTest {

    private lateinit var pipeline: RealUnifiedPipeline
    private lateinit var contextBuilder: RealContextBuilder
    private lateinit var fakeEntityRepo: FakeEntityRepository
    private lateinit var fakeDisambiguationService: FakeEntityDisambiguationService
    private lateinit var fakeInputParserService: FakeInputParserService
    private lateinit var fakeMemoryRepo: FakeMemoryRepository
    private lateinit var fakeExecutor: FakeExecutor
    private lateinit var fakeTaskRepo: ScheduledTaskRepository
    private lateinit var fakeHabitListener: FakeHabitListener
    
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        fakeEntityRepo = FakeEntityRepository()
        fakeMemoryRepo = FakeMemoryRepository()
        val habitRepo = FakeUserHabitRepository()
        val rl = RealReinforcementLearner(habitRepo)
        val timeProvider = FakeTimeProvider()
        val historyRepo = FakeHistoryRepository()

        fakeTaskRepo = object : ScheduledTaskRepository {
            override fun getTimelineItems(dayOffset: Int): Flow<List<TimelineItemModel>> = emptyFlow()
            override fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<TimelineItemModel>> = emptyFlow()
            override suspend fun insertTask(task: TimelineItemModel.Task): String = "fake-task"
            override suspend fun getTask(id: String): TimelineItemModel.Task? = null
            override suspend fun updateTask(task: TimelineItemModel.Task) {}
            override suspend fun deleteItem(id: String) {}
            override suspend fun getRecentCompleted(limit: Int): List<TimelineItemModel.Task> = emptyList()
            override suspend fun getTopUrgentActiveForEntity(entityId: String): TimelineItemModel.Task? = null
            override fun observeByEntityId(entityId: String): kotlinx.coroutines.flow.Flow<List<TimelineItemModel.Task>> = kotlinx.coroutines.flow.flowOf(emptyList())
        }

        contextBuilder = RealContextBuilder(
            timeProvider = timeProvider,
            reinforcementLearner = rl,
            memoryRepository = fakeMemoryRepo,
            entityRepository = fakeEntityRepo,
            scheduledTaskRepository = fakeTaskRepo,
            historyRepository = historyRepo,
            telemetry = FakePipelineTelemetry()
        )

        val entityWriter = RealEntityWriter(
            entityRepository = fakeEntityRepo,
            timeProvider = timeProvider,
            kernelWriteBack = contextBuilder,
            appScope = testScope
        )

        fakeDisambiguationService = FakeEntityDisambiguationService()
        fakeInputParserService = FakeInputParserService()
        fakeExecutor = FakeExecutor()
        fakeHabitListener = FakeHabitListener()

        val fakeLinter = SchedulerLinter(timeProvider)

        pipeline = RealUnifiedPipeline(
            contextBuilder = contextBuilder,
            entityDisambiguationService = fakeDisambiguationService,
            inputParserService = fakeInputParserService,
            entityWriter = entityWriter,
            schedulerLinter = fakeLinter,
            scheduledTaskRepository = fakeTaskRepo,
            scheduleBoard = FakeScheduleBoard(),
            inspirationRepository = FakeInspirationRepository(),
            alarmScheduler = FakeAlarmScheduler(),
            sessionTitleGenerator = FakeSessionTitleGenerator(),
            promptCompiler = FakePromptCompiler(),
            executor = fakeExecutor,
            telemetry = FakePipelineTelemetry(),
            habitListener = fakeHabitListener,
            appScope = testScope
        )
    }

    @Test
    fun `Scenario 1 - The Semantic Ambiguity Intercept`() = runTest {
        // Inject ParseResult.NeedsClarification
        fakeDisambiguationService.nextResult = DisambiguationResult.PassThrough
        fakeInputParserService.nextResult = ParseResult.NeedsClarification(
            ambiguousName = "order",
            suggestedMatches = emptyList(),
            clarificationPrompt = "Which order do you mean?"
        )
        
        val results = pipeline.processInput(
            PipelineInput("Update the order quantity", intent = QueryQuality.DEEP_ANALYSIS, requestedDepth = ContextDepth.FULL)
        ).toList()

        // Assert Pipeline suspended gracefully and emitted Intercepted result
        val disambiguationResult = results.filterIsInstance<PipelineResult.DisambiguationIntercepted>().firstOrNull()
        assertNotNull("Must emit DisambiguationIntercepted", disambiguationResult)
        
        // Assert no SSD writes
        assertEquals("Clarification requests should not touch SSD", 0, fakeEntityRepo.getAll(10).size)
    }

    @Test
    fun `Scenario 2 - The Linter Semantic Recovery`() = runTest {
        // We inject the pipeline with a schedule intent, but the FakeExecutor returns malformed JSON structure
        // Let's ensure the InputParser passes through normally, but we route to CRM_TASK (scheduler) route
        fakeDisambiguationService.nextResult = DisambiguationResult.PassThrough
        fakeInputParserService.nextResult = ParseResult.Success(emptyList(), null, "{}")
        
        // This JSON is structurally valid but has a blank title, which triggers LintResult.Error
        val badJson = """
            {
              "classification": "schedulable",
              "tasks": [
                {
                  "title": ""
                }
              ]
            }
        """.trimIndent()
        
        fakeExecutor.enqueueResponse(ExecutorResult.Success(badJson))

        val results = pipeline.processInput(
            PipelineInput("Schedule something broken", intent = QueryQuality.CRM_TASK, requestedDepth = ContextDepth.FULL)
        ).toList()

        // Assert pipeline did not crash, but correctly returned the linter's rejection
        val replyStr = results.filterIsInstance<PipelineResult.ConversationalReply>().firstOrNull()?.text
        assertNotNull("Must return ConversationalReply containing error", replyStr)
        assertTrue("Must contain failure phrase", replyStr!!.contains("操作解析失败"))
        
        assertEquals("Linter failure should not touch entity SSD", 0, fakeEntityRepo.getAll(10).size)
    }

    @Test
    fun `Scenario 3 - The Unresolved Context Pass-Through`() = runTest {
        // Simulate a disambiguation service that organically resumes the normal pipeline
        fakeDisambiguationService.nextResult = DisambiguationResult.Resumed("Just log a note that I called", Mode.ANALYST)
        fakeInputParserService.nextResult = ParseResult.Success(emptyList(), null, "{}")
        
        val results = pipeline.processInput(
            PipelineInput("Whatever, Just log a note that I called", intent = QueryQuality.DEEP_ANALYSIS, requestedDepth = ContextDepth.FULL)
        ).toList()
        
        val reply = results.filterIsInstance<PipelineResult.ConversationalReply>().firstOrNull()
        assertNotNull("Must resume normal ETL and return reply", reply)
    }
}
