package com.smartsales.prism.ui

import com.smartsales.core.pipeline.*
import com.smartsales.core.test.fakes.*
import com.smartsales.prism.data.fakes.FakeAudioRepository
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var intentOrchestrator: IntentOrchestrator

    private lateinit var viewModel: AgentViewModel
    private val testDispatcher = StandardTestDispatcher()

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

        intentOrchestrator = IntentOrchestrator(
            contextBuilder = fakeContextBuilder,
            lightningRouter = fakeLightningRouter,
            mascotService = fakeMascotService,
            unifiedPipeline = fakeUnifiedPipeline
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
    fun `vague intent triggers clarification intercept from IntentOrchestrator`() = runTest {
        // Enqueue VAGUE result from LightningRouter
        fakeLightningRouter.enqueueResult(
            RouterResult(QueryQuality.VAGUE, false, "您想查什么数据？")
        )

        viewModel.updateInput("查数据")
        viewModel.send()
        advanceUntilIdle()

        // Assert no delegates to UnifiedPipeline because L3 LightningRouter intercepted it
        assertEquals(0, fakeUnifiedPipeline.processedInputs.size)
        
        // And it emits Response back to the UI
        val uiState = viewModel.uiState.value
        assertTrue("Expected Response state, got ${uiState.javaClass.simpleName}", uiState is UiState.Response)
        assertEquals("您想查什么数据？", (uiState as UiState.Response).content)
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
        
        // Ensure ViewModel surfaced the response
        val uiState = viewModel.uiState.value
        assertTrue("Expected Response state, got ${uiState.javaClass.simpleName}", uiState is UiState.Response)
        assertEquals("分析已完成", (uiState as UiState.Response).content)
    }
}
