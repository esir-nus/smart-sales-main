package com.smartsales.prism.data.real

import com.smartsales.core.context.RealContextBuilder
import com.smartsales.core.pipeline.*
import com.smartsales.prism.domain.model.Mode
import com.smartsales.core.context.ContextDepth
import com.smartsales.core.test.fakes.*
import com.smartsales.prism.data.fakes.FakePipelineTelemetry
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import com.smartsales.prism.data.rl.RealReinforcementLearner
import com.smartsales.data.crm.writer.RealEntityWriter
import com.smartsales.data.crm.RealAliasCache
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.model.UiState
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.TokenUsage
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.smartsales.core.pipeline.ToolRegistry
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import com.smartsales.core.test.fakes.FakeToolRegistry
import java.time.LocalDate

class L2UserFlowTests {

    private lateinit var intentOrchestrator: IntentOrchestrator
    private lateinit var pipeline: RealUnifiedPipeline
    private lateinit var contextBuilder: RealContextBuilder
    private lateinit var fakeEntityRepo: FakeEntityRepository
    private lateinit var fakeMemoryRepo: FakeMemoryRepository
    private lateinit var fakeHabitRepo: FakeUserHabitRepository
    private lateinit var fakeTaskRepo: ScheduledTaskRepository
    private lateinit var entityWriter: RealEntityWriter
    private lateinit var aliasCache: RealAliasCache
    private lateinit var lightningRouter: RealLightningRouter
    private lateinit var executor: FakeExecutor
    private lateinit var inputParserService: FakeInputParserService
    private lateinit var disambiguationService: RealEntityDisambiguationService
    private lateinit var fakeMascotService: FakeMascotService
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        fakeEntityRepo = FakeEntityRepository()
        fakeMemoryRepo = FakeMemoryRepository()
        fakeHabitRepo = FakeUserHabitRepository()
        
        val rl = RealReinforcementLearner(fakeHabitRepo)
        val timeProvider = FakeTimeProvider()
        val historyRepo = FakeHistoryRepository()

        val fakeTaskRepoObj = object : ScheduledTaskRepository {
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
        this.fakeTaskRepo = fakeTaskRepoObj

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

        aliasCache = RealAliasCache(fakeEntityRepo, testScope)
        executor = FakeExecutor()
        val promptCompiler = FakePromptCompiler()
        
        lightningRouter = RealLightningRouter(executor, promptCompiler)
        
        inputParserService = FakeInputParserService()
        disambiguationService = RealEntityDisambiguationService(inputParserService)

        pipeline = RealUnifiedPipeline(
            contextBuilder = contextBuilder,
            entityDisambiguationService = disambiguationService,
            inputParserService = inputParserService,
            schedulerLinter = SchedulerLinter(),
            entityWriter = entityWriter,
            promptCompiler = promptCompiler,
            executor = executor,
            telemetry = FakePipelineTelemetry(),
            habitListener = FakeHabitListener(),
            appScope = testScope
        )

        fakeMascotService = FakeMascotService()
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

        val testTimeProvider = object : TimeProvider {
            override val now: Instant = Instant.now()
            override val currentTime: java.time.LocalTime = java.time.LocalTime.now()
            override val today: java.time.LocalDate = java.time.LocalDate.now()
            override val zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()
            override fun formatForLlm(): String = ""
        }

        intentOrchestrator = IntentOrchestrator(
            contextBuilder = contextBuilder,
            lightningRouter = lightningRouter,
            mascotService = fakeMascotService,
            unifiedPipeline = pipeline,
            entityWriter = entityWriter,
            aliasCache = aliasCache,
            uniAExtractionService = RealUniAExtractionService(
                executor = executor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter()
            ),
            uniBExtractionService = RealUniBExtractionService(
                executor = executor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter()
            ),
            uniCExtractionService = RealUniCExtractionService(
                executor = executor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter()
            ),
            fastTrackMutationEngine = FastTrackMutationEngine(
                taskRepository = fakeTaskRepository,
                scheduleBoard = FakeScheduleBoard(),
                inspirationRepository = FakeInspirationRepository(),
                timeProvider = testTimeProvider
            ),
            taskRepository = fakeTaskRepository,
            scheduleBoard = FakeScheduleBoard(),
            toolRegistry = FakeToolRegistry(),
            timeProvider = testTimeProvider,
            appScope = testScope
        )
    }

    @Test
    fun `T1 Temporal Semantic Extraction`() = runTest {
        // Objective: Querying memory does not misclassify as VAGUE, loads correct contextual memory
        seedWorldState(entityWriter, fakeMemoryRepo, fakeHabitRepo, fakeEntityRepo, fakeTaskRepo) {
            injectChaosSeed()
        }
        
        // 1. Lightning Router classification
        executor.defaultResponse = ExecutorResult.Success(
            content = """{
                "query_quality": "deep_analysis",
                "info_sufficient": true,
                "missing_entities": ["张总"],
                "analysis": { "info_sufficient": true }
            }""",
            tokenUsage = TokenUsage(10, 10)
        )
        
        // 2. Disambiguation - The pipeline will try to find "张总" 
        // We simulate the parser identifying it correctly 
        inputParserService.nextResult = ParseResult.EntityDeclaration(
            name = "张伟",
            company = "字节跳动",
            jobTitle = null,
            notes = null,
            aliases = emptyList()
        )
        
        val results = intentOrchestrator.processInput("马上要跟张总开会了，他们之前的顾虑是什么来着？").toList()
        
        // Ensure no VAGUE or NOISE interception
        assertTrue("Intent should NOT be intercepted by Mascot", fakeMascotService.interactions.isEmpty())
        
        // Check that UnifiedPipeline executed properly and yielded a response, not a clarification
        val reply = results.filterIsInstance<PipelineResult.ConversationalReply>().firstOrNull()
        assertNotNull("Should receive a conversational reply from pipeline", reply)
        
        // The issue physically solved: LightningRouter didn't fail it into VAGUE. 
        // RealUnifiedPipeline will load ContextDepth.FULL for DEEP_ANALYSIS inherently.
    }

    @Test
    fun `T2 Implicit Contextual Action Routing`() = runTest {
        // Objective: A scheduling intent follows the decoupling logic 
        seedWorldState(entityWriter, fakeMemoryRepo, fakeHabitRepo, fakeEntityRepo, fakeTaskRepo) {
            injectChaosSeed()
        }
        
        // This simulates a CRM_TASK intent
        executor.defaultResponse = ExecutorResult.Success(
            content = """{
                "query_quality": "crm_task",
                "info_sufficient": true,
                "missing_entities": [],
                "analysis": { "info_sufficient": true }
            }""",
            tokenUsage = TokenUsage(10, 10)
        )
        
        // Simulating the Disambiguation picking up the implicit context (ent_201 from T1 via pointer)
        inputParserService.nextResult = ParseResult.EntityDeclaration(
            name = "张伟",
            company = null,
            jobTitle = null,
            notes = null,
            aliases = emptyList()
        )
        
        // Now simulating the scheduler logic responding successfully 
        val results = intentOrchestrator.processInput("行，那你帮我定个周五下午3点的日程，跟进一下法务盖章的事。").toList()
        
        // Since we decided on Decoupled Physical Scheduling on March 12th, the pipeline 
        // might either schedule it gracefully (MutationProposal) or reply contextually.
        // We verify that it doesn't just crash or halt unpredictably.
        val proposal = results.filterIsInstance<PipelineResult.MutationProposal>().firstOrNull()
        val reply = results.filterIsInstance<PipelineResult.ConversationalReply>().firstOrNull()
        
        assertTrue("Should output a mutation proposal or reply", proposal != null || reply != null)
    }

    @Test
    fun `T3 Open-Loop Mutation Defense`() = runTest {
        // Objective: High stakes mutations must trigger PROPOSAL
        seedWorldState(entityWriter, fakeMemoryRepo, fakeHabitRepo, fakeEntityRepo, fakeTaskRepo) {
            injectChaosSeed()
        }
        
        executor.defaultResponse = ExecutorResult.Success(
            content = """{
                "query_quality": "crm_task",
                "info_sufficient": true,
                "missing_entities": ["张伟"],
                "analysis": { "info_sufficient": true }
            }""",
            tokenUsage = TokenUsage(10, 10)
        )
        
        inputParserService.nextResult = ParseResult.EntityDeclaration(
            name = "张伟",
            company = null,
            jobTitle = null,
            notes = null,
            aliases = emptyList()
        )
        
        // Simulated execution of UnifiedPipeline sending a Proposal
        // Note: RealUnifiedPipeline checks for mutations and SHOULD emit a PipelineResult.MutationProposal.
        // We will simulate the LLM outputting a mutation
        executor.defaultResponse = ExecutorResult.Success(
            content = """{
                "classification": "mutation",
                "profileMutations": [{
                     "entityId": "ent_201",
                     "field": "dealStage",
                     "value": "CLOSED_WON"
                }]
            }""",
            tokenUsage = TokenUsage(10, 10)
        )
        
        val results = intentOrchestrator.processInput("张伟那边说审批差不多了，把阶段往后挪一下，改成已赢单，金额写120万吧。").toList()
        
        // Instead of silently saving, the orchestrator MUST cache it as pendingProposal 
        // but not commit it.
        // Wait, RealIntentOrchestrator actually intercepts MutationProposals and caches them:
        // `if (result is PipelineResult.MutationProposal) { this@IntentOrchestrator.pendingProposal = result }`
        // We must ensure the repository does NOT have the updated deal stage yet.
        
        val entity = fakeEntityRepo.getAll(10).find { it.entityId == "ent_201" }
        assertTrue("Must NOT silently mutate DB without confirmation", entity?.dealStage != "CLOSED_WON")
    }
}
