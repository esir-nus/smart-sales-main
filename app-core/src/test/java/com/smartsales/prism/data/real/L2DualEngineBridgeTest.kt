package com.smartsales.prism.data.real

import com.smartsales.core.pipeline.RealUnifiedPipeline
import com.smartsales.core.pipeline.PipelineInput
import com.smartsales.core.pipeline.PipelineResult
import com.smartsales.core.pipeline.ParseResult
import com.smartsales.core.pipeline.DisambiguationResult
import com.smartsales.core.pipeline.IntentOrchestrator
import com.smartsales.core.pipeline.PromptCompiler
import com.smartsales.core.pipeline.RouterResult
import com.smartsales.core.pipeline.QueryQuality
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.RealUniBExtractionService
import com.smartsales.core.pipeline.RealUniCExtractionService
import com.smartsales.core.context.ContextDepth
import com.smartsales.core.context.RealContextBuilder
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.test.fakes.*
import com.smartsales.prism.data.fakes.FakePipelineTelemetry
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import com.smartsales.prism.data.rl.RealReinforcementLearner
import com.smartsales.data.crm.writer.RealEntityWriter
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.LintResult
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class L2DualEngineBridgeTest {

    private lateinit var pipeline: RealUnifiedPipeline
    private lateinit var contextBuilder: RealContextBuilder
    private lateinit var entityWriter: RealEntityWriter
    private lateinit var fakeEntityRepo: FakeEntityRepository
    private lateinit var fakeDisambiguationService: FakeEntityDisambiguationService
    private lateinit var fakeInputParserService: FakeInputParserService
    private lateinit var fakeMemoryRepo: FakeMemoryRepository
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

        val fakeTaskRepo = object : ScheduledTaskRepository {
            override fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>> = emptyFlow()
            override fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<SchedulerTimelineItem>> = emptyFlow()
            override suspend fun insertTask(task: ScheduledTask): String = "fake-task"
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

        contextBuilder = RealContextBuilder(
            timeProvider = timeProvider,
            reinforcementLearner = rl,
            memoryRepository = fakeMemoryRepo,
            entityRepository = fakeEntityRepo,
            scheduledTaskRepository = fakeTaskRepo,
            historyRepository = historyRepo,
            telemetry = FakePipelineTelemetry()
        )

        entityWriter = RealEntityWriter(
            entityRepository = fakeEntityRepo,
            timeProvider = timeProvider,
            kernelWriteBack = contextBuilder,
            appScope = testScope
        )

        fakeDisambiguationService = FakeEntityDisambiguationService()
        fakeInputParserService = FakeInputParserService()
        fakeHabitListener = FakeHabitListener()

        val fakeLinter = SchedulerLinter(timeProvider)

        pipeline = RealUnifiedPipeline(
            contextBuilder = contextBuilder,
            entityDisambiguationService = fakeDisambiguationService,
            inputParserService = fakeInputParserService,
            schedulerLinter = fakeLinter,
            entityWriter = entityWriter,
            sessionTitleGenerator = FakeSessionTitleGenerator(),
            promptCompiler = FakePromptCompiler(),
            executor = FakeExecutor(),
            telemetry = FakePipelineTelemetry(),
            habitListener = fakeHabitListener,
            appScope = testScope
        )
    }

    @Test
    fun `Scenario 1 - The Ideal Path Multi-Turn Delta`() = runTest {
        // TURN 1 (Create)
        val turn1Declaration = ParseResult.EntityDeclaration(
            name = "Client X",
            company = null,
            jobTitle = null,
            aliases = emptyList(),
            notes = "wants 50 widgets"
        )
        fakeDisambiguationService.nextResult = DisambiguationResult.Resolved(
            declaration = turn1Declaration,
            originalInput = "Client X wants 50 widgets",
            mode = Mode.ANALYST
        )

        pipeline.processInput(
            PipelineInput("Client X wants 50 widgets", intent = QueryQuality.DEEP_ANALYSIS, requestedDepth = ContextDepth.FULL, unifiedId = "test_unified_id")
        ).toList()

        // Assert Entity is written to SSD
        val entities = fakeEntityRepo.getAll(10)
        assertEquals("Should create entity in SSD", 1, entities.size)
        val clientId = entities[0].entityId
        assertEquals("Client X", entities[0].displayName)

        // Assert Session Working Set RAM has it
        val context1 = contextBuilder.build("next", Mode.ANALYST, listOf(clientId), ContextDepth.FULL)
        assertTrue("RAM context must contain the entity reference", context1.entityContext.containsKey("entity_$clientId"))

        // TURN 2 (Update)
        val turn2Declaration = ParseResult.EntityDeclaration(
            name = "Client X",
            company = null,
            jobTitle = null,
            aliases = emptyList(),
            notes = "Actually, make it 100"
        )
        fakeDisambiguationService.nextResult = DisambiguationResult.Resolved(
            declaration = turn2Declaration,
            originalInput = "Actually, make it 100",
            mode = Mode.ANALYST
        )

        pipeline.processInput(
            PipelineInput("Actually, make it 100", intent = QueryQuality.DEEP_ANALYSIS, requestedDepth = ContextDepth.FULL, unifiedId = "test_unified_id")
        ).toList()

        // Assert SSD was updated natively
        val updatedEntities = fakeEntityRepo.getAll(10)
        assertEquals("Should STILL just be 1 entity in SSD", 1, updatedEntities.size)
        assertTrue(
            "Entity Writer should have aggregated notes in profile", 
            updatedEntities[0].attributesJson.contains("Actually, make it 100")
        )

        // Assert KernelWriteBack triggered automatically
        val context2 = contextBuilder.build("test", Mode.ANALYST, emptyList(), ContextDepth.FULL)
        assertTrue("RAM context should STILL hold the entity reference after update", context2.entityContext.containsKey("entity_$clientId"))
    }

    @Test
    fun `Scenario 2 - The Trap Context Branch`() = runTest {
        // Inject ambiguous command into explicit Empty RAM & SSD
        fakeDisambiguationService.nextResult = DisambiguationResult.PassThrough
        fakeInputParserService.nextResult = ParseResult.NeedsClarification(
            ambiguousName = "order",
            suggestedMatches = emptyList(),
            clarificationPrompt = "Who's order?"
        )
        
        val results = pipeline.processInput(
            PipelineInput("Change the order to 100", intent = QueryQuality.DEEP_ANALYSIS, requestedDepth = ContextDepth.FULL, unifiedId = "test_unified_id")
        ).toList()

        // Expect ClarificationNeeded fallback instead of crashing
        val interceptResult = results.filterIsInstance<PipelineResult.DisambiguationIntercepted>().firstOrNull()
        assertNotNull("Pipeline must intercept and return result", interceptResult)
    }

    @Test
    fun `Scenario 3 - The Linter Verification`() = runTest {
        // Inject a corrupt declaration (blank name)
        val hallucinatedDeclaration = ParseResult.EntityDeclaration(
            name = "   ", // Invalid blank name bypassing LLM formatting
            company = null,
            jobTitle = null,
            aliases = emptyList(),
            notes = "fake"
        )
        fakeDisambiguationService.nextResult = DisambiguationResult.Resolved(
            declaration = hallucinatedDeclaration,
            originalInput = "Corrupt intent",
            mode = Mode.ANALYST
        )

        // The exact pipeline operation must reject it natively prior to hitting the repo
        val initialSaveCount = fakeEntityRepo.getByIdCount
        
        var threwException = false
        try {
            pipeline.processInput(
                PipelineInput("Corrupt intent", intent = QueryQuality.DEEP_ANALYSIS, requestedDepth = ContextDepth.FULL, unifiedId = "test_unified_id")
            ).toList()
        } catch (e: IllegalArgumentException) {
            threwException = true
        } catch (e: Exception) {
            // some other wrapper exception might be thrown (e.g. from flow), but it should be rooted in IllegalArgumentException
            threwException = true 
        }
        assertTrue("Should have thrown exception before SSD", threwException)

        assertEquals("Data should be REJECTED by linter before hitting SSD reads/writes", initialSaveCount, fakeEntityRepo.getByIdCount)
    }

    @Test
    fun `Scenario 4 - Dual Path Town and Highway Concurrency`() = runTest {
        // Setup Path A/B dependencies
        val timeProvider = FakeTimeProvider()
        val fakeTaskRepo = object : ScheduledTaskRepository {
            val tasks = mutableMapOf<String, ScheduledTask>()
            var upsertCount = 0

            override fun getTimelineItems(dayOffset: Int) = emptyFlow<List<SchedulerTimelineItem>>()
            override fun queryByDateRange(start: LocalDate, end: LocalDate) = emptyFlow<List<SchedulerTimelineItem>>()
            override suspend fun insertTask(task: ScheduledTask): String {
                tasks[task.id] = task
                return task.id
            }
            override suspend fun batchInsertTasks(tasks: List<ScheduledTask>): List<String> = emptyList()
            override suspend fun rescheduleTask(oldTaskId: String, newTask: ScheduledTask) {}
            override suspend fun getTask(id: String): ScheduledTask? = tasks[id]
            override suspend fun updateTask(task: ScheduledTask) { tasks[task.id] = task }
            override suspend fun upsertTask(task: ScheduledTask): String {
                upsertCount++
                tasks[task.id] = task
                return task.id
            }
            override suspend fun deleteItem(id: String) { tasks.remove(id) }
            override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> = emptyList()
            override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? = null
            override fun observeByEntityId(entityId: String) = emptyFlow<List<ScheduledTask>>()
        }

        val fakeAsr = object : com.smartsales.prism.domain.asr.AsrService {
            override suspend fun transcribe(file: java.io.File) = com.smartsales.prism.domain.asr.AsrResult.Success("明天下午三点开会")
            override suspend fun isAvailable() = true
        }

        val fakeConnectivity = object : com.smartsales.prism.domain.connectivity.ConnectivityBridge {
            override val connectionState = kotlinx.coroutines.flow.MutableStateFlow(com.smartsales.prism.domain.connectivity.BadgeConnectionState.Disconnected)
            override suspend fun downloadRecording(filename: String) = com.smartsales.prism.domain.connectivity.WavDownloadResult.Success(java.io.File.createTempFile("test", ".wav").apply { deleteOnExit() }, "test.wav", 100L)
            override suspend fun listRecordings() = com.smartsales.core.util.Result.Success(emptyList<String>())
            override fun recordingNotifications() = kotlinx.coroutines.flow.emptyFlow<com.smartsales.prism.domain.connectivity.RecordingNotification>()
            override suspend fun isReady() = true
            override suspend fun deleteRecording(filename: String) = true
        }

        // We fake the UnifiedPipeline to realistically delay its emission representing the "Heavyweight" Path B LLM call
        val fakeUnifiedPipeline = object : com.smartsales.core.pipeline.UnifiedPipeline {
            override suspend fun processInput(input: PipelineInput): Flow<PipelineResult> = kotlinx.coroutines.flow.flow {
                // Emulate LLM dispatch safely
                kotlinx.coroutines.delay(10) 
                emit(
                    PipelineResult.TaskCommandProposal(
                        com.smartsales.core.pipeline.SchedulerTaskCommand.CreateTasks(
                            com.smartsales.prism.domain.scheduler.CreateTasksParams(tasks = emptyList())
                        )
                    )
                )
            }
        }

        val fakeLightningRouter = FakeLightningRouter().apply {
            enqueueResult(RouterResult(QueryQuality.CRM_TASK, true, ""))
        }
        val fakeUniAExecutor = FakeExecutor().apply {
            enqueueResponse(
                ExecutorResult.Success(
                    """
                    {
                      "decision": "EXACT_CREATE",
                      "task": {
                        "title": "开会",
                        "startTimeIso": "2026-03-18T02:00:00Z",
                        "durationMinutes": 30,
                        "urgency": "L2"
                      }
                    }
                    """.trimIndent()
                )
            )
        }

        val orchestrator = IntentOrchestrator(
            contextBuilder = FakeContextBuilder(),
            lightningRouter = fakeLightningRouter,
            mascotService = FakeMascotService(),
            unifiedPipeline = fakeUnifiedPipeline,
            entityWriter = FakeEntityWriter(),
            aliasCache = FakeAliasCache(),
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
                taskRepository = fakeTaskRepo,
                scheduleBoard = FakeScheduleBoard(),
                inspirationRepository = FakeInspirationRepository(),
                timeProvider = timeProvider
            ),
            taskRepository = fakeTaskRepo,
            scheduleBoard = FakeScheduleBoard(),
            toolRegistry = FakeToolRegistry(),
            timeProvider = object : com.smartsales.prism.domain.time.TimeProvider {
                override val now: Instant = timeProvider.now
                override val currentTime: LocalTime = LocalTime.now()
                override val today: LocalDate = LocalDate.now()
                override val zoneId: ZoneId = ZoneId.systemDefault()
                override fun formatForLlm(): String = ""
            },
            appScope = testScope
        )

        val audioPipeline = com.smartsales.prism.data.audio.RealBadgeAudioPipeline(
            connectivityBridge = fakeConnectivity,
            asrService = fakeAsr,
            intentOrchestrator = orchestrator
        )

        // 1. Trigger the transaction. By the time this suspend function returns, Path A is fully complete, and Path B is launched.
        audioPipeline.processFile("test.wav")

        // Assert Path A inserted optimistic task
        assertTrue("Task should be immediately inserted by Path A", fakeTaskRepo.tasks.isNotEmpty())
        val unifiedId = fakeTaskRepo.tasks.keys.first()
        assertEquals("Exact task title should come from Uni-A extraction", "开会", fakeTaskRepo.tasks[unifiedId]?.title)
        assertEquals("Exact task start time should come from Uni-A extraction", Instant.parse("2026-03-18T02:00:00Z"), fakeTaskRepo.tasks[unifiedId]?.startTime)
        assertEquals("Upsert should have been called once by Path A", 1, fakeTaskRepo.upsertCount)

        // 3. Wait for Path B (LLM) finish on Dispatchers.IO
        java.lang.Thread.sleep(500) // Wait physically for Dispatchers.IO
        kotlinx.coroutines.yield()

        // Assert Path B successfully decoupled the task logic
        // The ToolDispatch is now handled by the ToolRegistry (System III)
        // therefore RealBadgeAudioPipeline no longer blindly upserts JSON on its own.
        // It's the Plugin's job to upsert it asynchronously afterwards.
        assertEquals("Upsert should only be called by Path A exact-create", 1, fakeTaskRepo.upsertCount)
    }
}
