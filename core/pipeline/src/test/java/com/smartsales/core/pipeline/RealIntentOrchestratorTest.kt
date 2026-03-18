package com.smartsales.core.pipeline

import com.smartsales.core.test.fakes.FakeUnifiedPipeline
import com.smartsales.core.test.fakes.FakeMascotService
import com.smartsales.core.test.fakes.FakeContextBuilder
import com.smartsales.core.test.fakes.FakeExecutor
import com.smartsales.core.test.fakes.FakeEntityWriter
import com.smartsales.core.test.fakes.FakeAliasCache
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.pipeline.ToolRegistry
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.Flow
import com.smartsales.core.test.fakes.FakeToolRegistry
import com.smartsales.core.test.fakes.FakeInspirationRepository
import com.smartsales.core.test.fakes.FakeScheduleBoard

/**
 * L2 Simulated Test for IntentOrchestrator utilizing Mock Eviction.
 * This test verifies the Phase 0 Gateway routing based on LightningRouter's LLM outputs.
 */
class RealIntentOrchestratorTest {

    private lateinit var orchestrator: IntentOrchestrator
    private lateinit var fakeContextBuilder: FakeContextBuilder
    private lateinit var realLightningRouter: RealLightningRouter
    private lateinit var fakeExecutor: FakeExecutor
    private lateinit var promptCompiler: PromptCompiler
    private lateinit var fakeMascotService: FakeMascotService
    private lateinit var fakeUnifiedPipeline: FakeUnifiedPipeline
    private lateinit var fakeEntityWriter: FakeEntityWriter
    private lateinit var fakeAliasCache: FakeAliasCache
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        fakeContextBuilder = FakeContextBuilder()
        fakeExecutor = FakeExecutor()
        promptCompiler = PromptCompiler()
        realLightningRouter = RealLightningRouter(fakeExecutor, promptCompiler)
        fakeMascotService = FakeMascotService()
        fakeUnifiedPipeline = FakeUnifiedPipeline()
        fakeEntityWriter = FakeEntityWriter()
        fakeAliasCache = FakeAliasCache()

        val fakeTaskRepository = object : ScheduledTaskRepository {
            override suspend fun batchInsertTasks(rules: List<ScheduledTask>): List<String> = emptyList()
            override suspend fun upsertTask(task: ScheduledTask): String = ""
            override suspend fun insertTask(task: ScheduledTask): String = ""
            override suspend fun updateTask(task: ScheduledTask) {}
            override suspend fun getTask(id: String): ScheduledTask? = null
            override fun queryByDateRange(start: java.time.LocalDate, end: java.time.LocalDate): Flow<List<ScheduledTask>> = emptyFlow()
            override fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>> = emptyFlow()
            override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> = emptyList()
            override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? = null
            override fun observeByEntityId(entityId: String): Flow<List<ScheduledTask>> = emptyFlow()
            override suspend fun deleteItem(id: String) {}
            override suspend fun rescheduleTask(oldTaskId: String, newTask: ScheduledTask) {}
        }

        orchestrator = IntentOrchestrator(
            contextBuilder = fakeContextBuilder,
            lightningRouter = realLightningRouter,
            mascotService = fakeMascotService,
            unifiedPipeline = fakeUnifiedPipeline,
            entityWriter = fakeEntityWriter,
            aliasCache = fakeAliasCache,
            uniAExtractionService = RealUniAExtractionService(
                executor = fakeExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter()
            ),
            fastTrackMutationEngine = FastTrackMutationEngine(
                taskRepository = fakeTaskRepository,
                scheduleBoard = FakeScheduleBoard(),
                inspirationRepository = FakeInspirationRepository()
            ),
            taskRepository = fakeTaskRepository,
            scheduleBoard = FakeScheduleBoard(),
            toolRegistry = FakeToolRegistry(),
            timeProvider = object : TimeProvider { 
                override val now: Instant = Instant.now() 
                override val currentTime: java.time.LocalTime = java.time.LocalTime.now()
                override val today: java.time.LocalDate = java.time.LocalDate.now()
                override val zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()
                override fun formatForLlm(): String = ""
            },
            appScope = testScope
        )
    }

    @Test
    fun `test all intent routing scenarios sequentially`() = runTest {
        // --- SCENARIO 1: NOISE ---
        setup()
        var input = "嗯嗯"
        fakeExecutor.enqueueResponse(ExecutorResult.Success(
            """{"query_quality": "noise", "response": "..."}"""
        ))
        var result = orchestrator.processInput(input).firstOrNull()

        assertEquals("NOISE should emit MascotIntercepted", PipelineResult.MascotIntercepted, result)
        assertEquals(1, fakeMascotService.interactions.size)
        assertTrue(fakeUnifiedPipeline.processedInputs.isEmpty())

        // --- SCENARIO 2: GREETING ---
        setup()
        input = "你好啊"
        fakeExecutor.enqueueResponse(ExecutorResult.Success(
            """{"query_quality": "greeting", "response": "我在"}"""
        ))
        result = orchestrator.processInput(input).firstOrNull()

        assertEquals("GREETING should emit MascotIntercepted", PipelineResult.MascotIntercepted, result)
        assertEquals(1, fakeMascotService.interactions.size)
        assertTrue(fakeUnifiedPipeline.processedInputs.isEmpty())


        // --- SCENARIO 4: DEEP_ANALYSIS ---
        setup()
        input = "帮我分析一下华为的最新进展"
        fakeExecutor.enqueueResponse(ExecutorResult.Success(
            """{"query_quality": "deep_analysis", "response": ""}"""
        ))
        val expectedPipelineResult = PipelineResult.ConversationalReply("这是分析结果")
        fakeUnifiedPipeline.nextResultFlow = kotlinx.coroutines.flow.flowOf(expectedPipelineResult)
        
        result = orchestrator.processInput(input).firstOrNull()

        assertNotNull(result)
        assertEquals(expectedPipelineResult, result)
        assertEquals(1, fakeUnifiedPipeline.processedInputs.size)
        assertEquals(input, fakeUnifiedPipeline.processedInputs[0].rawText)

        // --- SCENARIO 5: SIMPLE_QA ---
        setup()
        input = "谁是苹果的CEO"
        val answer = "库克"
        fakeExecutor.enqueueResponse(ExecutorResult.Success(
            """{"query_quality": "simple_qa", "response": "$answer"}"""
        ))
        val expectedQaResult = PipelineResult.ConversationalReply(answer)
        fakeUnifiedPipeline.nextResultFlow = kotlinx.coroutines.flow.flowOf(expectedQaResult)
        
        result = orchestrator.processInput(input).firstOrNull()

        assertNotNull(result)
        assertEquals(expectedQaResult, result)
        assertEquals(1, fakeUnifiedPipeline.processedInputs.size)
        assertEquals(input, fakeUnifiedPipeline.processedInputs[0].rawText)
        assertTrue(fakeMascotService.interactions.isEmpty())
    }
}
