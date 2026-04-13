package com.smartsales.prism.ui

import com.smartsales.core.pipeline.*
import com.smartsales.core.test.fakes.*
import com.smartsales.prism.data.fakes.FakeAudioRepository
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.domain.tingwu.TingwuSmartSummary
import kotlinx.coroutines.cancel
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
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
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
        testScope.cancel()
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
    fun `non voice plugin dispatch waits for confirm and then executes through real registry`() = runTest {
        val scenarioPlugin = artifactGeneratePlugin()
        val realToolRegistry = RealToolRegistry(setOf(scenarioPlugin))

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
            override val now: Instant = Instant.parse("2026-03-18T00:00:00Z")
            override val currentTime: java.time.LocalTime = java.time.LocalTime.NOON
            override val today: java.time.LocalDate = java.time.LocalDate.parse("2026-03-18")
            override val zoneId: java.time.ZoneId = java.time.ZoneId.of("UTC")
            override fun formatForLlm(): String = "2026-03-18"
        }

        val orchestrator = IntentOrchestrator(
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
            toolRegistry = realToolRegistry,
            timeProvider = testTimeProvider,
            appScope = testScope
        )

        val vm = AgentViewModel(
            intentOrchestrator = orchestrator,
            historyRepository = fakeHistoryRepo,
            userProfileRepository = fakeUserProfileRepo,
            scheduledTaskRepository = fakeScheduledTaskRepo,
            activityController = activityController,
            mascotService = fakeMascotService,
            eventBus = fakeEventBus,
            audioRepository = fakeAudioRepo,
            contextBuilder = fakeContextBuilder,
            toolRegistry = realToolRegistry
        )

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUnifiedPipeline.nextResultFlow = flowOf(
            PipelineResult.ToolDispatch(
                toolId = "artifact.generate",
                params = mapOf("ruleId" to "executive_report")
            )
        )

        vm.updateInput("生成华为汇报")
        vm.send()
        advanceUntilIdle()

        assertTrue(scenarioPlugin.executedRequests.isEmpty())
        val proposalMsg = vm.history.value.last() as com.smartsales.prism.domain.model.ChatMessage.Ai
        val proposalText = (proposalMsg.uiState as UiState.Response).content
        assertTrue(proposalText.contains("确认执行"))

        vm.updateInput("确认执行")
        vm.send()
        advanceUntilIdle()

        assertEquals(1, scenarioPlugin.executedRequests.size)
        val finalMsg = vm.history.value.last() as com.smartsales.prism.domain.model.ChatMessage.Ai
        val finalText = (finalMsg.uiState as UiState.Response).content
        assertTrue(finalText.contains("artifact.generate completed"))
        assertTrue(finalText.contains("executive_report"))
    }

    @Test
    fun `legacy workflow recommendation is normalized to canonical task board item`() = runTest {
        fakeToolRegistry.tools.add(
            AnalystTool(
                id = PluginToolIds.ARTIFACT_GENERATE,
                icon = "pdf",
                label = "PDF Report",
                description = "Generate a report"
            )
        )
        fakeLightningRouter.enqueueResult(
            RouterResult(QueryQuality.DEEP_ANALYSIS, true, "")
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(
            PipelineResult.ToolRecommendation(
                listOf(
                    com.smartsales.prism.domain.core.WorkflowRecommendation(
                        workflowId = "GENERATE_PDF",
                        reason = "legacy alias",
                        parameters = emptyMap()
                    )
                )
            )
        )

        viewModel.updateInput("帮我生成 PDF")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(1, viewModel.taskBoardItems.value.size)
        assertEquals(PluginToolIds.ARTIFACT_GENERATE, viewModel.taskBoardItems.value.single().id)
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

    @Test
    fun `startNewSession resets transient ui state and activity`() = runTest {
        viewModel.updateInput("待清空输入")
        activityController.startPhase(ActivityPhase.PLANNING)
        viewModel.debugRunScenario("MARKDOWN_BUBBLE")

        val taskBoardField = AgentViewModel::class.java.getDeclaredField("_taskBoardItems")
        taskBoardField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val taskBoardState =
            taskBoardField.get(viewModel) as MutableStateFlow<List<com.smartsales.prism.domain.analyst.TaskBoardItem>>
        taskBoardState.value = listOf(
            com.smartsales.prism.domain.analyst.TaskBoardItem(
                id = "tool-1",
                icon = "icon",
                title = "Tool",
                description = "Desc"
            )
        )

        val errorField = AgentViewModel::class.java.getDeclaredField("_errorMessage")
        errorField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val errorState = errorField.get(viewModel) as MutableStateFlow<String?>
        errorState.value = "旧错误"

        viewModel.startNewSession()
        advanceUntilIdle()

        assertEquals("", viewModel.inputText.value)
        assertEquals(UiState.Idle, viewModel.uiState.value)
        assertEquals("新对话", viewModel.sessionTitle.value)
        assertTrue(viewModel.history.value.isEmpty())
        assertTrue(viewModel.taskBoardItems.value.isEmpty())
        assertNull(viewModel.errorMessage.value)
        assertNull(viewModel.agentActivity.value)
        assertTrue(fakeContextBuilder.getSessionHistory().isEmpty())
    }

    @Test
    fun `switchSession rehydrates history and linked audio document context`() = runTest {
        val sessionId = fakeHistoryRepo.createSession("Acme", "摘要", linkedAudioId = "audio-1")
        fakeHistoryRepo.saveMessage(sessionId, isUser = true, content = "你好", orderIndex = 0)
        fakeHistoryRepo.saveMessage(sessionId, isUser = false, content = "您好", orderIndex = 1)
        fakeAudioRepo.artifacts = TingwuJobArtifacts(
            transcriptMarkdown = "逐字稿内容",
            smartSummary = TingwuSmartSummary(summary = "总结内容")
        )
        activityController.startPhase(ActivityPhase.PLANNING)

        viewModel.switchSession(sessionId)
        advanceUntilIdle()

        assertEquals("Acme", viewModel.sessionTitle.value)
        assertEquals("", viewModel.inputText.value)
        assertEquals(UiState.Idle, viewModel.uiState.value)
        assertNull(viewModel.agentActivity.value)
        assertEquals(2, viewModel.history.value.size)
        assertEquals(
            listOf(
                com.smartsales.core.context.ChatTurn("user", "你好"),
                com.smartsales.core.context.ChatTurn("assistant", "您好")
            ),
            fakeContextBuilder.getSessionHistory()
        )
        assertTrue(fakeContextBuilder.lastDocumentContextPayload?.contains("总结内容") == true)
        assertTrue(fakeContextBuilder.lastDocumentContextPayload?.contains("逐字稿内容") == true)
    }

    @Test
    fun `updateSessionTitle persists rename to active session`() = runTest {
        advanceUntilIdle()

        viewModel.updateSessionTitle("新标题")
        advanceUntilIdle()

        assertEquals("新标题", viewModel.sessionTitle.value)
        assertEquals("新标题", fakeHistoryRepo.getSession("session-1")?.clientName)
    }

}
