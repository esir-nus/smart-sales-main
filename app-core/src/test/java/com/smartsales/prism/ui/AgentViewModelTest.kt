package com.smartsales.prism.ui

import com.smartsales.core.pipeline.*
import com.smartsales.core.test.fakes.*
import com.smartsales.prism.data.fakes.FakeAudioRepository
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AgentViewModelTest {

    private lateinit var fakeHistoryRepo: FakeHistoryRepository
    private lateinit var fakeUserProfileRepo: FakeUserProfileRepository
    private lateinit var fakeScheduledTaskRepo: FakeScheduledTaskRepository
    private lateinit var activityController: AgentActivityController // Real object
    private lateinit var fakeMascotService: FakeMascotService
    private lateinit var fakeEventBus: FakeSystemEventBus
    private lateinit var fakeAudioRepo: FakeAudioRepository
    private lateinit var fakeContextBuilder: FakeContextBuilder
    private lateinit var fakeToolRegistry: FakeToolRegistry
    private lateinit var fakeLightningRouter: FakeLightningRouter
    private lateinit var fakeUnifiedPipeline: FakeUnifiedPipeline
    private lateinit var fakeEntityWriter: FakeEntityWriter
    private lateinit var fakeAliasCache: FakeAliasCache
    private lateinit var intentOrchestrator: IntentOrchestrator

    private lateinit var viewModel: AgentViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        fakeHistoryRepo = FakeHistoryRepository()
        fakeUserProfileRepo = FakeUserProfileRepository()
        fakeScheduledTaskRepo = FakeScheduledTaskRepository()
        activityController = AgentActivityController()
        fakeMascotService = FakeMascotService()
        fakeEventBus = FakeSystemEventBus()
        fakeAudioRepo = FakeAudioRepository()
        fakeContextBuilder = FakeContextBuilder()
        fakeToolRegistry = FakeToolRegistry()
        fakeLightningRouter = FakeLightningRouter()
        fakeUnifiedPipeline = FakeUnifiedPipeline()
        fakeEntityWriter = FakeEntityWriter()
        fakeAliasCache = FakeAliasCache()

        val fakeTaskRepository = object : com.smartsales.prism.domain.scheduler.ScheduledTaskRepository {
            override suspend fun batchInsertTasks(rules: List<com.smartsales.prism.domain.scheduler.ScheduledTask>): List<String> = emptyList()
            override suspend fun upsertTask(task: com.smartsales.prism.domain.scheduler.ScheduledTask): String = ""
            override suspend fun insertTask(task: com.smartsales.prism.domain.scheduler.ScheduledTask): String = ""
            override suspend fun updateTask(task: com.smartsales.prism.domain.scheduler.ScheduledTask) {}
            override suspend fun getTask(id: String): com.smartsales.prism.domain.scheduler.ScheduledTask? = null
            override fun queryByDateRange(start: java.time.LocalDate, end: java.time.LocalDate): kotlinx.coroutines.flow.Flow<List<com.smartsales.prism.domain.scheduler.ScheduledTask>> = kotlinx.coroutines.flow.emptyFlow()
            override fun getTimelineItems(dayOffset: Int): kotlinx.coroutines.flow.Flow<List<com.smartsales.prism.domain.scheduler.SchedulerTimelineItem>> = kotlinx.coroutines.flow.emptyFlow()
            override suspend fun getRecentCompleted(limit: Int): List<com.smartsales.prism.domain.scheduler.ScheduledTask> = emptyList()
            override suspend fun getTopUrgentActiveForEntity(entityId: String): com.smartsales.prism.domain.scheduler.ScheduledTask? = null
            override fun observeByEntityId(entityId: String): kotlinx.coroutines.flow.Flow<List<com.smartsales.prism.domain.scheduler.ScheduledTask>> = kotlinx.coroutines.flow.emptyFlow()
            override suspend fun deleteItem(id: String) {}
            override suspend fun rescheduleTask(oldTaskId: String, newTask: com.smartsales.prism.domain.scheduler.ScheduledTask) {}
        }

        val testTimeProvider = object : com.smartsales.prism.domain.time.TimeProvider {
            override val now: Instant = Instant.now()
            override val currentTime: java.time.LocalTime = java.time.LocalTime.now()
            override val today: java.time.LocalDate = java.time.LocalDate.now()
            override val zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()
            override fun formatForLlm(): String = ""
        }

        intentOrchestrator = IntentOrchestrator(
            contextBuilder = fakeContextBuilder,
            lightningRouter = fakeLightningRouter,
            mascotService = fakeMascotService,
            unifiedPipeline = fakeUnifiedPipeline,
            entityWriter = fakeEntityWriter,
            aliasCache = fakeAliasCache,
            uniAExtractionService = RealUniAExtractionService(
                executor = FakeExecutor(),
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter()
            ),
            uniBExtractionService = RealUniBExtractionService(
                executor = FakeExecutor(),
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter()
            ),
            uniCExtractionService = RealUniCExtractionService(
                executor = FakeExecutor(),
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
            toolRegistry = fakeToolRegistry,
            timeProvider = testTimeProvider,
            appScope = testScope
        )

        viewModel = AgentViewModel(
            intentOrchestrator = intentOrchestrator,
            historyRepository = fakeHistoryRepo,
            userProfileRepository = fakeUserProfileRepo,
            scheduledTaskRepository = fakeScheduledTaskRepo,
            activityController = activityController,
            mascotService = fakeMascotService,
            eventBus = fakeEventBus,
            audioRepository = fakeAudioRepo,
            contextBuilder = fakeContextBuilder,
            toolRegistry = fakeToolRegistry
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty input leaves state IDLE and does not route`() = runTest {
        viewModel.updateInput("   ")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(UiState.Idle, viewModel.uiState.value)
        assertEquals(0, fakeUnifiedPipeline.processedInputs.size)
    }

    @Test
    fun `noise intent triggers mascot intercept from IntentOrchestrator`() = runTest {
        // Enqueue NOISE result from LightningRouter
        fakeLightningRouter.enqueueResult(
            RouterResult(QueryQuality.NOISE, false, "输入为空")
        )

        viewModel.updateInput("测试噪音")
        viewModel.send()
        advanceUntilIdle()

        // Assert no delegates to UnifiedPipeline because L3 LightningRouter intercepted it
        assertEquals(0, fakeUnifiedPipeline.processedInputs.size)
        
        // Ensure ViewModel dropped to Idle without emitting an AI response
        val lastMsg = viewModel.history.value.last()
        assertTrue("Expected User message, got Ai", lastMsg is com.smartsales.prism.domain.model.ChatMessage.User)
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `deep analysis routes to UnifiedPipeline and emits its conversational reply`() = runTest {
        fakeLightningRouter.enqueueResult(
            RouterResult(QueryQuality.DEEP_ANALYSIS, false, "")
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(
            PipelineResult.ConversationalReply("分析已完成")
        )

        viewModel.updateInput("分析销售报表")
        viewModel.send()
        advanceUntilIdle()

        // It successfully bypassed the router interception
        assertEquals(1, fakeUnifiedPipeline.processedInputs.size)
        // Ensure the input intent was set perfectly
        assertEquals("分析销售报表", fakeUnifiedPipeline.processedInputs[0].rawText)
        assertEquals(QueryQuality.DEEP_ANALYSIS, fakeUnifiedPipeline.processedInputs[0].intent)
        
        // Ensure ViewModel surfaced the response to history
        val historyMsg = viewModel.history.value.last() as com.smartsales.prism.domain.model.ChatMessage.Ai
        assertTrue("Expected Response state, got ${historyMsg.uiState.javaClass.simpleName}", historyMsg.uiState is UiState.Response)
        assertEquals("分析已完成", (historyMsg.uiState as UiState.Response).content)
        assertEquals(UiState.Idle, viewModel.uiState.value)
        assertEquals(
            listOf(
                com.smartsales.core.context.ChatTurn("user", "分析销售报表"),
                com.smartsales.core.context.ChatTurn("assistant", "分析已完成")
            ),
            fakeContextBuilder.getSessionHistory()
        )
    }

    @Test
    fun `clarification follow-up resumes through kernel session memory`() = runTest {
        fakeLightningRouter.enqueueResult(
            RouterResult(QueryQuality.DEEP_ANALYSIS, false, "")
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(
            PipelineResult.ClarificationNeeded("你指的是 Tom 吗？")
        )

        viewModel.updateInput("他说了什么")
        viewModel.send()
        advanceUntilIdle()

        fakeLightningRouter.enqueueResult(
            RouterResult(QueryQuality.DEEP_ANALYSIS, false, "")
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(
            PipelineResult.ConversationalReply("Tom 说先推进报价。")
        )

        viewModel.updateInput("Tom")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(2, fakeLightningRouter.evaluatedContexts.size)
        assertEquals(
            listOf(
                com.smartsales.core.context.ChatTurn("user", "他说了什么"),
                com.smartsales.core.context.ChatTurn("assistant", "你指的是 Tom 吗？"),
                com.smartsales.core.context.ChatTurn("user", "Tom")
            ),
            fakeLightningRouter.evaluatedContexts[1].sessionHistory
        )
        assertEquals(
            listOf(
                com.smartsales.core.context.ChatTurn("user", "他说了什么"),
                com.smartsales.core.context.ChatTurn("assistant", "你指的是 Tom 吗？"),
                com.smartsales.core.context.ChatTurn("user", "Tom"),
                com.smartsales.core.context.ChatTurn("assistant", "Tom 说先推进报价。")
            ),
            fakeContextBuilder.getSessionHistory()
        )
    }

    @Test
    fun `selectTaskBoardItem submits correct payload to ToolRegistry`() = runTest {
        // Arrange
        val expectedToolId = "test_tool_123"
        val expectedInputText = "some context text"
        
        // Use reflection to mock the UI state since it's populated from an external source not easily faked here
        val field = AgentViewModel::class.java.getDeclaredField("_taskBoardItems")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as MutableStateFlow<List<com.smartsales.prism.domain.analyst.TaskBoardItem>>
        stateFlow.value = listOf(
            com.smartsales.prism.domain.analyst.TaskBoardItem(
                id = expectedToolId, title = "Test Tool", description = "", icon = ""
            )
        )
        
        viewModel.updateInput(expectedInputText)
        
        // Act - Call the method directly rather than relying on LLM auto-dispatch
        viewModel.selectTaskBoardItem(expectedToolId)
        advanceUntilIdle()
        
        // Assert
        assertEquals("ToolRegistry should have been called once", 1, fakeToolRegistry.executedRequests.size)
        val request = fakeToolRegistry.executedRequests.first()
        
        assertEquals("The extracted text context should match the ViewModel's input text", expectedInputText, request.rawInput)
        assertEquals("The parameters should be empty when invoked directly from UI", emptyMap<String, Any>(), request.parameters)
    }

    @Test
    fun `selectTaskBoardItem builds runtime plugin gateway with bounded session-read capability`() = runTest {
        val expectedToolId = "test_tool_456"

        val field = AgentViewModel::class.java.getDeclaredField("_taskBoardItems")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as MutableStateFlow<List<com.smartsales.prism.domain.analyst.TaskBoardItem>>
        stateFlow.value = listOf(
            com.smartsales.prism.domain.analyst.TaskBoardItem(
                id = expectedToolId, title = "Test Tool", description = "", icon = ""
            )
        )

        fakeContextBuilder.loadSession(
            "session-1",
            listOf(
                com.smartsales.core.context.ChatTurn("user", "跟 Tom 跟进报价"),
                com.smartsales.core.context.ChatTurn("assistant", "好的")
            )
        )
        fakeToolRegistry.executeFlow = emptyFlow()

        viewModel.selectTaskBoardItem(expectedToolId)
        advanceUntilIdle()

        assertEquals(1, fakeToolRegistry.executedGateways.size)
        val gateway = fakeToolRegistry.executedGateways.single()
        assertTrue(gateway.grantedPermissions().contains(CoreModulePermission.READ_SESSION_HISTORY))

        val history = gateway.getSessionHistory(2)
        assertTrue(history.contains("跟 Tom 跟进报价"))
    }
}
