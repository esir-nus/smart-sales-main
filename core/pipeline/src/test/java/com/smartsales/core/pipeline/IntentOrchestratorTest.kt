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
import com.smartsales.core.test.fakes.FakeActiveTaskRetrievalIndex
import com.smartsales.prism.domain.memory.CacheResult
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.smartsales.core.pipeline.ToolRegistry
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.Flow
import com.smartsales.core.test.fakes.FakeToolRegistry
import com.smartsales.core.llm.ExecutorResult
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * L1 Logic Verification Test - Intent Orchestrator
 * 
 * Verifies that inputs are correctly routed to the MascotService or the UnifiedPipeline
 * based on the LightningRouter evaluation.
 * 
 * Anti-Illusion Protocol Compliant (No Mockito).
 */
class IntentOrchestratorTest {

    private val fixedZoneId: ZoneId = ZoneId.of("Asia/Shanghai")
    private val fixedNow: Instant = Instant.parse("2026-03-18T00:00:00Z")
    private val fixedToday: LocalDate = LocalDate.of(2026, 3, 18)
    private val fixedCurrentTime: LocalTime = LocalTime.of(8, 0)

    private lateinit var fakeContextBuilder: FakeContextBuilder
    private lateinit var fakeLightningRouter: FakeLightningRouter
    private lateinit var fakeMascotService: FakeMascotService
    private lateinit var fakeUnifiedPipeline: FakeUnifiedPipeline
    private lateinit var fakeEntityWriter: FakeEntityWriter
    private lateinit var fakeAliasCache: FakeAliasCache
    private lateinit var fakeUniAExecutor: FakeExecutor
    private lateinit var fakeTaskRepository: ScheduledTaskRepository
    private lateinit var fakeScheduleBoard: FakeScheduleBoard
    private lateinit var fakeInspirationRepository: FakeInspirationRepository
    private lateinit var fakeActiveTaskRetrievalIndex: FakeActiveTaskRetrievalIndex
    private lateinit var testTimeProvider: TimeProvider
    private lateinit var storedTasks: MutableMap<String, ScheduledTask>
    private lateinit var fakeTaskCreationBadgeSignal: FakeTaskCreationBadgeSignal
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
        fakeInspirationRepository = FakeInspirationRepository()
        fakeActiveTaskRetrievalIndex = FakeActiveTaskRetrievalIndex()
        fakeTaskCreationBadgeSignal = FakeTaskCreationBadgeSignal()
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
        
        testTimeProvider = object : TimeProvider {
            override val now: Instant = fixedNow
            override val currentTime: LocalTime = fixedCurrentTime
            override val today: LocalDate = fixedToday
            override val zoneId: ZoneId = fixedZoneId
            override fun formatForLlm(): String = ""
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
            uniBExtractionService = RealUniBExtractionService(
                executor = fakeUniAExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter()
            ),
            uniCExtractionService = RealUniCExtractionService(
                executor = fakeUniAExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter()
            ),
            fastTrackMutationEngine = FastTrackMutationEngine(
                taskRepository = fakeTaskRepository,
                scheduleBoard = fakeScheduleBoard,
                inspirationRepository = fakeInspirationRepository,
                timeProvider = testTimeProvider
            ),
            taskRepository = fakeTaskRepository,
            scheduleBoard = fakeScheduleBoard,
            toolRegistry = FakeToolRegistry(),
            timeProvider = testTimeProvider,
            appScope = testScope,
            taskCreationBadgeSignal = fakeTaskCreationBadgeSignal
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
                    "startTimeIso": "2026-03-19T02:00:00Z",
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
        assertEquals(Instant.parse("2026-03-19T02:00:00Z"), pathAResult.task.startTime)
        assertFalse(pathAResult.task.isVague)
        assertFalse(pathAResult.task.hasConflict)
        assertNotNull(fakeTaskRepository.getTask(pathAResult.task.id))
        assertTrue(results.contains(expectedReply))
        assertEquals(1, fakeTaskCreationBadgeSignal.calls)
    }

    @Test
    fun `voice later-lane scheduler command is suppressed after Path A commit`() = runTest {
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
                    "startTimeIso": "2026-03-19T02:00:00Z",
                    "durationMinutes": 60,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(
            PipelineResult.TaskCommandProposal(
                SchedulerTaskCommand.CreateTasks(
                    com.smartsales.prism.domain.scheduler.CreateTasksParams(
                        unifiedId = "path-b-followup",
                        tasks = listOf(
                            com.smartsales.prism.domain.scheduler.TaskDefinition(
                                title = "Path B hallucinated follow-up",
                                startTimeIso = "2026-03-19T04:44:00Z",
                                durationMinutes = 60,
                                urgency = com.smartsales.prism.domain.scheduler.UrgencyEnum.L2_IMPORTANT
                            )
                        )
                    )
                )
            ),
            PipelineResult.ConversationalReply("Scheduled.")
        )

        val results = orchestrator.processInput(input, isVoice = true).toList()

        assertTrue(results.first() is PipelineResult.PathACommitted)
        assertEquals(1, storedTasks.size)
        assertNull(storedTasks["path-b-followup"])
        assertTrue(results.any { it is PipelineResult.ConversationalReply })
    }

    @Test
    fun `voice scheduler path forwards displayed page anchor into Uni-A prompt`() = runTest {
        setup()
        val input = "后一天一点提醒我开会"

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "测试页锚点"
                }
                """.trimIndent()
            )
        )
        fakeUnifiedPipeline.nextResultFlow = emptyFlow()

        orchestrator.processInput(
            input,
            isVoice = true,
            displayedDateIso = "2026-03-19"
        ).toList()

        assertTrue(
            fakeUniAExecutor.executedPrompts.any { it.contains("displayed_date_iso: 2026-03-19") }
        )
        assertTrue(fakeUniAExecutor.executedPrompts.any { it.contains("后一天") })
        assertTrue(fakeUniAExecutor.executedPrompts.any { it.contains("13:00") })
        assertTrue(fakeUniAExecutor.executedPrompts.any { it.contains("01:00") })
    }

    @Test
    fun `voice scheduler path emits PathACommitted for Uni-B vague create after Uni-A NotExact`() = runTest {
        setup()
        val input = "三天以后提醒我开会"

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
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
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "VAGUE_CREATE",
                  "task": {
                    "title": "提醒我开会",
                    "anchorDateIso": "2026-03-21",
                    "timeHint": "时间待定",
                    "urgency": "L3"
                  }
                }
                """.trimIndent()
            )
        )
        println(
            "DEBUG promoted linter=" + SchedulerLinter().parseUniBExtraction(
                input = """
                {
                  "decision": "VAGUE_CREATE",
                  "task": {
                    "title": "去接李总",
                    "anchorDateIso": "2026-03-20",
                    "timeHint": "晚上九点",
                    "urgency": "L2"
                  }
                }
                """.trimIndent(),
                unifiedId = "debug-promoted",
                transcript = input,
                nowIso = java.time.Instant.now().toString(),
                timezone = java.time.ZoneId.systemDefault().id,
                displayedDateIso = "2026-03-15"
            )
        )
        val expectedReply = PipelineResult.ConversationalReply("Path B reply")
        fakeUnifiedPipeline.nextResultFlow = flowOf(expectedReply)

        val results = orchestrator.processInput(input, isVoice = true).toList()

        assertTrue(results.first() is PipelineResult.PathACommitted)
        val pathAResult = results.first() as PipelineResult.PathACommitted
        assertEquals("提醒我开会", pathAResult.task.title)
        assertTrue(pathAResult.task.isVague)
        assertFalse(pathAResult.task.hasConflict)
        assertEquals(pathAResult.task.id, fakeUnifiedPipeline.processedInputs.first().unifiedId)
        assertNotNull(fakeTaskRepository.getTask(pathAResult.task.id))
        assertTrue(results.contains(expectedReply))
        assertTrue(fakeUniAExecutor.executedPrompts.last().contains("anchorDateIso"))
        assertEquals(1, fakeTaskCreationBadgeSignal.calls)
    }

    @Test
    fun `date-only tomorrow input must fall through Uni-A and commit as Uni-B vague task`() = runTest {
        setup()
        val input = "明天提醒我打车去机场"

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
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
            )
        )
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "VAGUE_CREATE",
                  "task": {
                    "title": "提醒我打车去机场",
                    "anchorDateIso": "2026-03-19",
                    "timeHint": null,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )
        val expectedReply = PipelineResult.ConversationalReply("Path B reply")
        fakeUnifiedPipeline.nextResultFlow = flowOf(expectedReply)

        val results = orchestrator.processInput(input, isVoice = true).toList()

        assertTrue(results.first() is PipelineResult.PathACommitted)
        val pathAResult = results.first() as PipelineResult.PathACommitted
        assertEquals("提醒我打车去机场", pathAResult.task.title)
        assertTrue(pathAResult.task.isVague)
        assertFalse(pathAResult.task.hasConflict)
        assertNotNull(fakeTaskRepository.getTask(pathAResult.task.id))
        assertTrue(results.contains(expectedReply))
    }

    @Test
    fun `explicit clock cue in Uni-B response must promote to exact Path A commit`() = runTest {
        setup()
        val input = "后天晚上九点去接李总"

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "测试降级"
                }
                """.trimIndent()
            )
        )
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
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
            )
        )
        val expectedReply = PipelineResult.ConversationalReply("Path B reply")
        fakeUnifiedPipeline.nextResultFlow = flowOf(expectedReply)

        val results = orchestrator.processInput(input, isVoice = true, displayedDateIso = "2026-03-15").toList()

        assertTrue(results.first() is PipelineResult.PathACommitted)
        val pathAResult = results.first() as PipelineResult.PathACommitted
        assertEquals("去接李总", pathAResult.task.title)
        assertFalse(pathAResult.task.isVague)
        assertFalse(pathAResult.task.hasConflict)
        assertEquals(OffsetDateTime.parse("2026-03-20T13:00:00Z").toInstant(), pathAResult.task.startTime)
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
    fun `voice scheduler path commits Uni-D conflict-visible exact task when overlap exists`() = runTest {
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
                    "startTimeIso": "2026-03-19T02:00:00Z",
                    "durationMinutes": 60,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )
        fakeScheduleBoard.nextConflictResult = ConflictResult.Conflict(
            overlaps = listOf(
                com.smartsales.prism.domain.memory.ScheduleItem(
                    entryId = "existing-1",
                    title = "牙医预约",
                    scheduledAt = Instant.parse("2026-03-19T02:00:00Z").toEpochMilli(),
                    durationMinutes = 60,
                    durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                    conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                )
            )
        )
        val expectedReply = PipelineResult.ConversationalReply("Scheduled.")
        fakeUnifiedPipeline.nextResultFlow = flowOf(expectedReply)

        val results = orchestrator.processInput(input, isVoice = true).toList()

        assertTrue(results.first() is PipelineResult.PathACommitted)
        val pathAResult = results.first() as PipelineResult.PathACommitted
        assertTrue(pathAResult.task.hasConflict)
        assertFalse(pathAResult.task.isVague)
        assertEquals("existing-1", pathAResult.task.conflictWithTaskId)
        assertEquals("与「牙医预约」时间冲突", pathAResult.task.conflictSummary)
        assertNotNull(fakeTaskRepository.getTask(pathAResult.task.id))
        assertTrue(results.contains(expectedReply))
    }

    @Test
    fun `voice timeless intent commits inspiration and never enters scheduler lane`() = runTest {
        setup()
        val input = "以后想练口语"

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "没有时间承诺"
                }
                """.trimIndent()
            )
        )
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_VAGUE",
                  "reason": "不是待定日程"
                }
                """.trimIndent()
            )
        )
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "INSPIRATION_CREATE",
                  "idea": {
                    "content": "以后想练口语",
                    "title": "练口语"
                  }
                }
                """.trimIndent()
            )
        )

        val results = orchestrator.processInput(input, isVoice = true).toList()

        assertTrue(results.first() is PipelineResult.InspirationCommitted)
        val inspirationResult = results.first() as PipelineResult.InspirationCommitted
        assertEquals("以后想练口语", inspirationResult.content)
        assertTrue(storedTasks.isEmpty())
        assertEquals(0, fakeUnifiedPipeline.processedInputs.size)
        val inspirations = fakeInspirationRepository.getAll().first()
        assertEquals(1, inspirations.size)
        assertEquals("以后想练口语", inspirations.first().title)
        val finalPrompt = fakeUniAExecutor.executedPrompts.last()
        assertTrue(finalPrompt.contains("INSPIRATION_CREATE"))
    }

    @Test
    fun `voice exact conflict emits Uni-D core-flow checkpoints`() = runTest {
        setup()
        val input = "明天下午三点开会"
        val checkpoints = mutableListOf<PipelineValve.Checkpoint>()
        PipelineValve.testInterceptor = { checkpoint, _, _ ->
            checkpoints += checkpoint
        }

        try {
            fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
            fakeUniAExecutor.enqueueResponse(
                ExecutorResult.Success(
                    """
                    {
                      "decision": "EXACT_CREATE",
                      "task": {
                        "title": "开会",
                        "startTimeIso": "2026-03-19T07:00:00Z",
                        "durationMinutes": 60,
                        "urgency": "L2"
                      }
                    }
                    """.trimIndent()
                )
            )
            fakeScheduleBoard.nextConflictResult = ConflictResult.Conflict(
                overlaps = listOf(
                    com.smartsales.prism.domain.memory.ScheduleItem(
                        entryId = "existing-1",
                        title = "牙医预约",
                        scheduledAt = Instant.parse("2026-03-19T07:00:00Z").toEpochMilli(),
                        durationMinutes = 60,
                        durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                        conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                    )
                )
            )
            fakeUnifiedPipeline.nextResultFlow = flowOf(PipelineResult.ConversationalReply("Scheduled."))

            orchestrator.processInput(input, isVoice = true).toList()

            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.TASK_EXTRACTED))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.CONFLICT_EVALUATED))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.PATH_A_DB_WRITTEN))
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `voice exact zero-duration task still commits Uni-D when board reports occupied point`() = runTest {
        setup()
        val input = "后天晚上九点去接李总"

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "EXACT_CREATE",
                  "task": {
                    "title": "去接李总",
                    "startTimeIso": "2026-03-21T13:00:00Z",
                    "durationMinutes": 0,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )
        fakeScheduleBoard.nextConflictResult = ConflictResult.Conflict(
            overlaps = listOf(
                com.smartsales.prism.domain.memory.ScheduleItem(
                    entryId = "existing-21",
                    title = "回家睡觉",
                    scheduledAt = Instant.parse("2026-03-21T13:00:00Z").toEpochMilli(),
                    durationMinutes = 60,
                    durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                    conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                )
            )
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(PipelineResult.ConversationalReply("Scheduled."))

        val results = orchestrator.processInput(input, isVoice = true).toList()

        assertTrue(results.first() is PipelineResult.PathACommitted)
        val pathAResult = results.first() as PipelineResult.PathACommitted
        assertEquals("去接李总", pathAResult.task.title)
        assertTrue(pathAResult.task.hasConflict)
        assertEquals("existing-21", pathAResult.task.conflictWithTaskId)
        assertEquals("与「回家睡觉」时间冲突", pathAResult.task.conflictSummary)
        assertEquals(0, pathAResult.task.durationMinutes)
    }

    @Test
    fun `shared scheduler router gives top level voice batch create parity`() = runTest {
        setup()
        val sharedExecutor = FakeExecutor()
        val sharedOrchestrator = buildSharedSchedulerOrchestrator(sharedExecutor)

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        sharedExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision":"MULTI_CREATE",
                  "fragments":[
                    {
                      "title":"开会",
                      "mode":"EXACT",
                      "anchorKind":"ABSOLUTE",
                      "startTimeIso":"2026-03-19T02:00:00Z",
                      "durationMinutes":60,
                      "urgency":"L2"
                    },
                    {
                      "title":"发报告",
                      "mode":"EXACT",
                      "anchorKind":"ABSOLUTE",
                      "startTimeIso":"2026-03-19T07:00:00Z",
                      "durationMinutes":30,
                      "urgency":"L2"
                    }
                  ]
                }
                """.trimIndent()
            )
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(PipelineResult.ConversationalReply("Path B reply"))

        val results = sharedOrchestrator.processInput(
            "明天十点开会，然后下午三点发报告",
            isVoice = true
        ).toList()

        val committed = results.filterIsInstance<PipelineResult.PathACommitted>()
        assertEquals(2, committed.size)
        assertEquals(listOf("开会", "发报告"), committed.map { it.task.title })
        assertTrue(storedTasks.values.any { it.title == "开会" })
        assertTrue(storedTasks.values.any { it.title == "发报告" })
        assertTrue(results.any { it == PipelineResult.ConversationalReply("Path B reply") })
    }

    @Test
    fun `shared scheduler router commits qualified weekday exact create before path b`() = runTest {
        setup()
        val sharedExecutor = FakeExecutor()
        val sharedOrchestrator = buildSharedSchedulerOrchestrator(sharedExecutor)

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUnifiedPipeline.nextResultFlow = flowOf(PipelineResult.ConversationalReply("Path B reply"))

        val results = sharedOrchestrator.processInput(
            "下周三早上八点钟提醒我起床",
            isVoice = true
        ).toList()

        val committed = results.filterIsInstance<PipelineResult.PathACommitted>()
        assertEquals(1, committed.size)
        assertEquals("起床", committed.first().task.title)
        assertEquals(Instant.parse("2026-03-25T00:00:00Z"), committed.first().task.startTime)
        assertTrue(results.any { it == PipelineResult.ConversationalReply("Path B reply") })
    }

    @Test
    fun `shared scheduler router resolves top level voice global reschedule before path b`() = runTest {
        setup()
        val sharedExecutor = FakeExecutor()
        val sharedOrchestrator = buildSharedSchedulerOrchestrator(sharedExecutor)
        val existing = ScheduledTask(
            id = "task-1",
            timeDisplay = "10:00",
            title = "张总会议",
            urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT,
            startTime = Instant.parse("2026-03-19T02:00:00Z"),
            durationMinutes = 60
        )
        fakeTaskRepository.upsertTask(existing)
        fakeActiveTaskRetrievalIndex.nextShortlist = listOf(
            com.smartsales.prism.domain.scheduler.ActiveTaskContext(
                taskId = "task-1",
                title = "张总会议",
                timeSummary = "明天 10:00",
                isVague = false
            )
        )
        fakeActiveTaskRetrievalIndex.nextResolveResult =
            com.smartsales.prism.domain.scheduler.ActiveTaskResolveResult.Resolved("task-1")

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        sharedExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision":"RESCHEDULE_TARGETED",
                  "suggestedTaskId":"task-1",
                  "targetQuery":"张总会议",
                  "timeInstruction":"明天上午十一点"
                }
                """.trimIndent()
            )
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(
            PipelineResult.TaskCommandProposal(
                SchedulerTaskCommand.CreateTasks(
                    com.smartsales.prism.domain.scheduler.CreateTasksParams(
                        tasks = listOf(
                            com.smartsales.prism.domain.scheduler.TaskDefinition(
                                title = "Path B hallucinated follow-up",
                                startTimeIso = "2026-03-19T04:44:00Z",
                                durationMinutes = 60,
                                urgency = com.smartsales.prism.domain.scheduler.UrgencyEnum.L2_IMPORTANT
                            )
                        )
                    )
                )
            ),
            PipelineResult.ConversationalReply("Path B reply")
        )

        val results = sharedOrchestrator.processInput(
            "把明天和张总的会改到明天上午十一点",
            isVoice = true
        ).toList()

        val committed = results.filterIsInstance<PipelineResult.PathACommitted>()
        assertEquals(1, committed.size)
        assertEquals("task-1", committed.first().task.id)
        assertEquals(Instant.parse("2026-03-19T03:00:00Z"), committed.first().task.startTime)
        assertFalse(storedTasks.values.any { it.title == "Path B hallucinated follow-up" })
        assertEquals("把明天和张总的会改到明天上午十一点", fakeActiveTaskRetrievalIndex.lastShortlistTranscript)
        assertTrue(results.any { it == PipelineResult.ConversationalReply("Path B reply") })
        assertEquals(0, fakeTaskCreationBadgeSignal.calls)
    }

    @Test
    fun `voice inspiration path does not trigger task creation badge signal`() = runTest {
        setup()
        val input = "记一下这个想法"

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "not a task"
                }
                """.trimIndent()
            )
        )
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_VAGUE",
                  "reason": "still not a task"
                }
                """.trimIndent()
            )
        )
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "INSPIRATION_CREATE",
                  "idea": {
                    "content": "记一下这个想法",
                    "title": "记一下这个想法"
                  }
                }
                """.trimIndent()
            )
        )

        val results = orchestrator.processInput(input, isVoice = true).toList()

        assertTrue(results.first() is PipelineResult.InspirationCommitted)
        assertEquals(0, fakeTaskCreationBadgeSignal.calls)
    }

    private fun buildSharedSchedulerOrchestrator(sharedExecutor: FakeExecutor): IntentOrchestrator {
        val linter = SchedulerLinter(testTimeProvider)
        return IntentOrchestrator(
            contextBuilder = fakeContextBuilder,
            lightningRouter = fakeLightningRouter,
            mascotService = fakeMascotService,
            unifiedPipeline = fakeUnifiedPipeline,
            entityWriter = fakeEntityWriter,
            aliasCache = fakeAliasCache,
            uniAExtractionService = RealUniAExtractionService(
                executor = sharedExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = linter
            ),
            uniBExtractionService = RealUniBExtractionService(
                executor = sharedExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = linter
            ),
            uniCExtractionService = RealUniCExtractionService(
                executor = sharedExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = linter
            ),
            fastTrackMutationEngine = FastTrackMutationEngine(
                taskRepository = fakeTaskRepository,
                scheduleBoard = fakeScheduleBoard,
                inspirationRepository = fakeInspirationRepository,
                timeProvider = testTimeProvider
            ),
            taskRepository = fakeTaskRepository,
            scheduleBoard = fakeScheduleBoard,
            toolRegistry = FakeToolRegistry(),
            timeProvider = testTimeProvider,
            appScope = testScope,
            taskCreationBadgeSignal = fakeTaskCreationBadgeSignal,
            activeTaskRetrievalIndex = fakeActiveTaskRetrievalIndex,
            uniMExtractionService = RealUniMExtractionService(
                executor = sharedExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = linter
            ),
            globalRescheduleExtractionService = RealGlobalRescheduleExtractionService(
                executor = sharedExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = linter
            )
        )
    }

    private class FakeTaskCreationBadgeSignal : TaskCreationBadgeSignal {
        var calls = 0

        override suspend fun onTasksCreated() {
            calls++
        }
    }
}
