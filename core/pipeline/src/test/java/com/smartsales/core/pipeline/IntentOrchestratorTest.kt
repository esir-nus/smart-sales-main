package com.smartsales.core.pipeline

import com.smartsales.core.context.ContextDepth
import com.smartsales.core.context.EnhancedContext
import com.smartsales.prism.domain.model.Mode
import com.smartsales.core.test.fakes.FakeContextBuilder
import com.smartsales.core.test.fakes.FakeLightningRouter
import com.smartsales.core.test.fakes.FakeMascotService
import com.smartsales.core.test.fakes.FakeUnifiedPipeline
import com.smartsales.core.test.fakes.FakeEntityWriter
import com.smartsales.core.test.fakes.FakeAliasCache
import com.smartsales.core.test.fakes.FakeExecutor
import com.smartsales.core.test.fakes.FakeInspirationRepository
import com.smartsales.core.test.fakes.FakeScheduleBoard
import com.smartsales.prism.domain.memory.CacheResult
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
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
import com.smartsales.core.llm.ExecutorResult

/**
 * L1 Logic Verification Test - Intent Orchestrator
 * 
 * Verifies that inputs are correctly routed to the MascotService or the UnifiedPipeline
 * based on the LightningRouter evaluation.
 * 
 * Anti-Illusion Protocol Compliant (No Mockito).
 */
class IntentOrchestratorTest {

    private lateinit var fakeContextBuilder: FakeContextBuilder
    private lateinit var fakeLightningRouter: FakeLightningRouter
    private lateinit var fakeMascotService: FakeMascotService
    private lateinit var fakeUnifiedPipeline: FakeUnifiedPipeline
    private lateinit var fakeEntityWriter: FakeEntityWriter
    private lateinit var fakeAliasCache: FakeAliasCache
    private lateinit var fakeUniAExecutor: FakeExecutor
    private lateinit var fakeTaskRepository: ScheduledTaskRepository
    private lateinit var fakeScheduleBoard: FakeScheduleBoard
    private lateinit var storedTasks: MutableMap<String, ScheduledTask>
    private val testScope = TestScope(UnconfinedTestDispatcher())
    
    private lateinit var orchestrator: IntentOrchestrator

    @Before
    fun setup() {
        fakeContextBuilder = FakeContextBuilder()
        fakeLightningRouter = FakeLightningRouter()
        fakeMascotService = FakeMascotService()
        fakeUnifiedPipeline = FakeUnifiedPipeline()
        fakeEntityWriter = FakeEntityWriter()
        fakeAliasCache = FakeAliasCache()
        fakeUniAExecutor = FakeExecutor()
        fakeScheduleBoard = FakeScheduleBoard()
        storedTasks = mutableMapOf()
        fakeTaskRepository = object : ScheduledTaskRepository {
            override suspend fun batchInsertTasks(rules: List<ScheduledTask>): List<String> {
                rules.forEachIndexed { index, task ->
                    val id = task.id.ifBlank { "task-$index" }
                    storedTasks[id] = task.copy(id = id)
                }
                return storedTasks.keys.toList()
            }
            override suspend fun upsertTask(task: ScheduledTask): String {
                storedTasks[task.id] = task
                return task.id
            }
            override suspend fun insertTask(task: ScheduledTask): String {
                storedTasks[task.id] = task
                return task.id
            }
            override suspend fun updateTask(task: ScheduledTask) {
                storedTasks[task.id] = task
            }
            override suspend fun getTask(id: String): ScheduledTask? = storedTasks[id]
            override fun queryByDateRange(start: java.time.LocalDate, end: java.time.LocalDate): Flow<List<SchedulerTimelineItem>> = emptyFlow()
            override fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>> = emptyFlow()
            override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> = emptyList()
            override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? = null
            override fun observeByEntityId(entityId: String): Flow<List<ScheduledTask>> = emptyFlow()
            override suspend fun deleteItem(id: String) {
                storedTasks.remove(id)
            }
            override suspend fun rescheduleTask(oldTaskId: String, newTask: ScheduledTask) {
                storedTasks.remove(oldTaskId)
                storedTasks[oldTaskId] = newTask.copy(id = oldTaskId)
            }
        }
        
        orchestrator = IntentOrchestrator(
            contextBuilder = fakeContextBuilder,
            lightningRouter = fakeLightningRouter,
            mascotService = fakeMascotService,
            unifiedPipeline = fakeUnifiedPipeline,
            entityWriter = fakeEntityWriter,
            aliasCache = fakeAliasCache,
            uniAExtractionService = RealUniAExtractionService(
                executor = fakeUniAExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter()
            ),
            fastTrackMutationEngine = FastTrackMutationEngine(
                taskRepository = fakeTaskRepository,
                scheduleBoard = fakeScheduleBoard,
                inspirationRepository = FakeInspirationRepository()
            ),
            taskRepository = fakeTaskRepository,
            scheduleBoard = fakeScheduleBoard,
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
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.NOISE, true, ""))
        var result = orchestrator.processInput(input).firstOrNull()
        
        assertEquals("NOISE should emit MascotIntercepted", PipelineResult.MascotIntercepted, result)
        assertEquals(1, fakeMascotService.interactions.size)
        assertTrue(fakeMascotService.interactions[0] is MascotInteraction.Text)
        assertEquals(input, (fakeMascotService.interactions[0] as MascotInteraction.Text).content)
        assertTrue(fakeUnifiedPipeline.processedInputs.isEmpty())

        // --- SCENARIO 2: GREETING ---
        setup()
        input = "你好啊"
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.GREETING, true, ""))
        result = orchestrator.processInput(input).firstOrNull()

        assertEquals("GREETING should emit MascotIntercepted", PipelineResult.MascotIntercepted, result)
        assertEquals(1, fakeMascotService.interactions.size)
        assertEquals(input, (fakeMascotService.interactions[0] as MascotInteraction.Text).content)
        assertTrue(fakeUnifiedPipeline.processedInputs.isEmpty())


        // --- SCENARIO 4: DEEP_ANALYSIS ---
        setup()
        input = "帮我分析一下华为的最新进展"
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        val expectedPipelineResult = PipelineResult.ConversationalReply("这是分析结果")
        fakeUnifiedPipeline.nextResultFlow = flowOf(expectedPipelineResult)
        
        result = orchestrator.processInput(input).firstOrNull()

        assertNotNull(result)
        assertEquals(expectedPipelineResult, result)
        assertEquals(1, fakeUnifiedPipeline.processedInputs.size)
        assertEquals(input, fakeUnifiedPipeline.processedInputs[0].rawText)

        // --- SCENARIO 5: SIMPLE_QA ---
        setup()
        input = "谁是苹果的CEO"
        val answer = "库克"
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.SIMPLE_QA, true, answer))
        val expectedQaResult = PipelineResult.ConversationalReply(answer)
        fakeUnifiedPipeline.nextResultFlow = flowOf(expectedQaResult)
        
        result = orchestrator.processInput(input).firstOrNull()

        assertNotNull(result)
        assertEquals(expectedQaResult, result)
        assertEquals(1, fakeUnifiedPipeline.processedInputs.size)
        assertEquals(input, fakeUnifiedPipeline.processedInputs[0].rawText)
        // --- SCENARIO 6: WAVE 5 T1 SYNC LOOP (AMBIGUOUS CACHE CAUSES FAST-FAIL) ---
        setup()
        input = "张总怎么说"
        // Lightning router extracts "张总"
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, "", listOf("张总")))
        
        // Cache says ambiguous (2 hits)
        val dummyEntity1 = EntityEntry("e1", EntityType.PERSON, "张伟", "[]", "{}", "{}", "{}", "{}", "{}", 0, 0)
        val dummyEntity2 = EntityEntry("e2", EntityType.PERSON, "张三", "[]", "{}", "{}", "{}", "{}", "{}", 0, 0)
        fakeAliasCache.nextResult = CacheResult.Ambiguous(listOf(dummyEntity1, dummyEntity2))
        
        result = orchestrator.processInput(input).firstOrNull()
        
        // Must emit DisambiguationIntercepted immediately
        assertNotNull(result)
        assertTrue(result is PipelineResult.DisambiguationIntercepted)
        val interceptResult = result as PipelineResult.DisambiguationIntercepted
        val uiState = interceptResult.uiState as UiState.AwaitingClarification
        assertEquals(2, uiState.candidates.size)
        // Must NOT call UnifiedPipeline
        assertTrue("UnifiedPipeline should be bypassed on AliasCache.Ambiguous", fakeUnifiedPipeline.processedInputs.isEmpty())

        // --- SCENARIO 7: WAVE 5 T1 SYNC LOOP (EXACT MATCH PASSES DOWNSTREAM) ---
        setup()
        input = "雷军的总结"
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, "", listOf("雷军")))
        
        // Cache says exact match
        fakeAliasCache.nextResult = CacheResult.ExactMatch("lei-001")
        val expectedNormalResult = PipelineResult.ConversationalReply("分析中")
        fakeUnifiedPipeline.nextResultFlow = flowOf(expectedNormalResult)
        
        result = orchestrator.processInput(input).firstOrNull()
        
        assertEquals(expectedNormalResult, result)
        assertEquals(1, fakeUnifiedPipeline.processedInputs.size)
        // PipelineInput MUST carry the resolved ID
        assertEquals("lei-001", fakeUnifiedPipeline.processedInputs[0].resolvedEntityId)
    }

    @Test
    fun `verify unifiedID propagates into PipelineInput`() = runTest {
        // According to Wave 14 Shard 1 Specs, the orchestrator must mint a non-null unifiedId
        // and attach it to the PipelineInput for Dual-Path architecture sync.
        setup()
        val input = "Schedule a meeting for tomorrow"
        
        // Route it as a standard scheduler intent
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.CRM_TASK, true, ""))
        val expectedResult = PipelineResult.ConversationalReply("Scheduled.")
        fakeUnifiedPipeline.nextResultFlow = flowOf(expectedResult)
        
        val result = orchestrator.processInput(input).firstOrNull()
        
        // Verify orchestrator execution completes successfully
        assertNotNull(result)
        assertEquals(expectedResult, result)
        
        // Check UnifiedPipeline hook for mechanical verification
        assertEquals(1, fakeUnifiedPipeline.processedInputs.size)
        val pipelineInput = fakeUnifiedPipeline.processedInputs[0]
        
        // Assert intent is preserved
        assertEquals(QueryQuality.CRM_TASK, pipelineInput.intent)
        
        // Mechanical Verification: unifiedId must be explicitly present and non-empty
        assertNotNull("unifiedId must not be null", pipelineInput.unifiedId)
        assertTrue("unifiedId must be a populated UUID token", pipelineInput.unifiedId.isNotBlank())
    }

    @Test
    fun `confirmation executes cached profile mutation through entity writer`() = runTest {
        setup()
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUnifiedPipeline.nextResultFlow = flowOf(
            PipelineResult.MutationProposal(
                profileMutations = listOf(
                    PipelineResult.ProfileMutation(
                        entityId = "entity-1",
                        field = "notes",
                        value = "new value"
                    )
                )
            )
        )

        val firstTurn = orchestrator.processInput("更新Tom的备注").toList()
        assertTrue(firstTurn.any { it is PipelineResult.MutationProposal })

        val confirmTurn = orchestrator.processInput("确认执行").toList()
        assertTrue(confirmTurn.any { it is PipelineResult.ConversationalReply })
        assertEquals(
            listOf(Triple("entity-1", "notes", "new value")),
            fakeEntityWriter.updatedAttributes
        )
    }

    @Test
    fun `confirmation executes typed scheduler task command through scheduler owner`() = runTest {
        setup()
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.CRM_TASK, true, ""))
        fakeUnifiedPipeline.nextResultFlow = flowOf(
            PipelineResult.TaskCommandProposal(
                SchedulerTaskCommand.CreateTasks(
                    com.smartsales.prism.domain.scheduler.CreateTasksParams(
                        tasks = listOf(
                            com.smartsales.prism.domain.scheduler.TaskDefinition(
                                title = "Typed Task",
                                startTimeIso = "2026-03-18T10:00:00Z",
                                durationMinutes = 30,
                                urgency = com.smartsales.prism.domain.scheduler.UrgencyEnum.L2_IMPORTANT
                            )
                        )
                    )
                )
            )
        )

        val firstTurn = orchestrator.processInput("明天十点安排Typed Task").toList()
        assertTrue(firstTurn.any { it is PipelineResult.TaskCommandProposal })

        val confirmTurn = orchestrator.processInput("确认执行").toList()
        assertTrue(confirmTurn.any { it is PipelineResult.ConversationalReply })
        assertEquals(1, storedTasks.size)
        assertEquals("Typed Task", storedTasks.values.first().title)
    }

    @Test
    fun `voice scheduler path emits PathACommitted before downstream results`() = runTest {
        setup()
        val input = "明天上午十点开会"

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "EXACT_CREATE",
                  "task": {
                    "title": "开会",
                    "startTimeIso": "2026-03-18T02:00:00Z",
                    "durationMinutes": 60,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )
        val expectedReply = PipelineResult.ConversationalReply("Scheduled.")
        fakeUnifiedPipeline.nextResultFlow = flowOf(expectedReply)

        val results = orchestrator.processInput(input, isVoice = true).toList()

        assertTrue(results.first() is PipelineResult.PathACommitted)
        val pathAResult = results.first() as PipelineResult.PathACommitted
        assertEquals("开会", pathAResult.task.title)
        assertEquals(pathAResult.task.id, fakeUnifiedPipeline.processedInputs.first().unifiedId)
        assertEquals(QueryQuality.DEEP_ANALYSIS, fakeUnifiedPipeline.processedInputs.first().intent)
        assertEquals(Instant.parse("2026-03-18T02:00:00Z"), pathAResult.task.startTime)
        assertFalse(pathAResult.task.isVague)
        assertFalse(pathAResult.task.hasConflict)
        assertNotNull(fakeTaskRepository.getTask(pathAResult.task.id))
        assertTrue(results.contains(expectedReply))
    }

    @Test
    fun `voice scheduler path does not fake PathA commit when Uni-A is not exact`() = runTest {
        setup()
        val input = "明天开会"

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.SIMPLE_QA, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "缺少明确时间"
                }
                """.trimIndent()
            )
        )
        val expectedReply = PipelineResult.ConversationalReply("Scheduled.")
        fakeUnifiedPipeline.nextResultFlow = flowOf(expectedReply)

        val results = orchestrator.processInput(input, isVoice = true).toList()

        assertTrue(results.none { it is PipelineResult.PathACommitted })
        assertTrue(results.contains(expectedReply))
        assertEquals(1, fakeUnifiedPipeline.processedInputs.size)
        assertEquals(QueryQuality.SIMPLE_QA, fakeUnifiedPipeline.processedInputs.first().intent)
    }

    @Test
    fun `voice scheduler path does not fake PathA commit when Uni-A exact task conflicts`() = runTest {
        setup()
        val input = "明天上午十点开会"

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "EXACT_CREATE",
                  "task": {
                    "title": "开会",
                    "startTimeIso": "2026-03-18T02:00:00Z",
                    "durationMinutes": 60,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )
        fakeScheduleBoard.nextConflictResult = ConflictResult.Conflict(
            overlaps = emptyList()
        )
        val expectedReply = PipelineResult.ConversationalReply("Scheduled.")
        fakeUnifiedPipeline.nextResultFlow = flowOf(expectedReply)

        val results = orchestrator.processInput(input, isVoice = true).toList()

        assertTrue(results.none { it is PipelineResult.PathACommitted })
        assertNull(fakeTaskRepository.getTask(fakeUnifiedPipeline.processedInputs.first().unifiedId))
        assertTrue(results.contains(expectedReply))
    }
}
