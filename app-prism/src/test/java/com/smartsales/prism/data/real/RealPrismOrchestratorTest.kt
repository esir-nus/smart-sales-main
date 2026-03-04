package com.smartsales.prism.data.real

import com.smartsales.prism.domain.analyst.AnalystPipeline
import com.smartsales.prism.domain.analyst.LightningRouter
import com.smartsales.prism.domain.mascot.MascotService
import com.smartsales.prism.domain.activity.AgentActivityController
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.EntityWriter
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.model.ClarificationType
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.parser.InputParserService
import com.smartsales.prism.domain.parser.ParseResult
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.EntityRef
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ModeMetadata
import com.smartsales.prism.domain.pipeline.ToolArtifact
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RealPrismOrchestratorTest {

    private lateinit var orchestrator: PrismOrchestrator
    private lateinit var mockContextBuilder: ContextBuilder
    private lateinit var mockExecutor: Executor
    private lateinit var mockActivityController: AgentActivityController
    private lateinit var mockScheduledTaskRepo: ScheduledTaskRepository
    private lateinit var mockAlarmScheduler: AlarmScheduler
    private lateinit var mockSchedulerLinter: SchedulerLinter
    private lateinit var mockScheduleBoard: ScheduleBoard
    private lateinit var mockInspirationRepo: InspirationRepository
    private lateinit var mockReinforcementLearner: ReinforcementLearner
    private lateinit var mockEntityWriter: EntityWriter
    private lateinit var mockEntityRepo: EntityRepository
    private lateinit var mockInputParserService: InputParserService
    private lateinit var mockEntityDisambiguationService: com.smartsales.prism.domain.disambiguation.EntityDisambiguationService
    private lateinit var mockLightningRouter: LightningRouter
    private lateinit var mockMascotService: MascotService
    private lateinit var mockAnalystPipeline: AnalystPipeline
    private lateinit var telemetry: com.smartsales.prism.data.fakes.FakePipelineTelemetry

    @Before
    fun setup() {
        mockContextBuilder = mock()
        mockExecutor = mock()
        mockActivityController = mock()
        mockScheduledTaskRepo = mock()
        mockAlarmScheduler = mock()
        mockSchedulerLinter = mock()
        mockScheduleBoard = mock()
        mockInspirationRepo = mock()
        mockReinforcementLearner = mock()
        mockEntityWriter = mock()
        mockEntityRepo = mock()
        mockInputParserService = mock()
        mockEntityDisambiguationService = mock()
        mockLightningRouter = mock()
        mockMascotService = mock()
        mockAnalystPipeline = mock()
        telemetry = com.smartsales.prism.data.fakes.FakePipelineTelemetry()

        kotlinx.coroutines.runBlocking {
            whenever(mockEntityDisambiguationService.process(any())).doReturn(com.smartsales.prism.domain.disambiguation.DisambiguationResult.PassThrough)
            
            val fakeStateFlow = MutableStateFlow(com.smartsales.prism.domain.analyst.AnalystState.IDLE)
            whenever(mockAnalystPipeline.state).doReturn(fakeStateFlow)
        }

        orchestrator = PrismOrchestrator(
            contextBuilder = mockContextBuilder,
            executor = mockExecutor,
            activityController = mockActivityController,
            scheduledTaskRepository = mockScheduledTaskRepo,
            alarmScheduler = mockAlarmScheduler,
            schedulerLinter = mockSchedulerLinter,
            scheduleBoard = mockScheduleBoard,
            inspirationRepository = mockInspirationRepo,
            reinforcementLearner = mockReinforcementLearner,
            entityWriter = mockEntityWriter,
            entityRepository = mockEntityRepo,
            inputParserService = mockInputParserService,
            entityDisambiguationService = mockEntityDisambiguationService,
            telemetry = telemetry,
            lightningRouter = mockLightningRouter,
            mascotService = mockMascotService,
            analystPipeline = mockAnalystPipeline
        )
    }

    @Test
    fun `when InputParser returns NeedsClarification, Orchestrator halts and returns AwaitingClarification`() = runTest {
        // Arrange
        val ambiguousResult = ParseResult.NeedsClarification(
            ambiguousName = "新客户",
            suggestedMatches = emptyList(),
            clarificationPrompt = "系统发现 '新客户' 似乎不在通讯录中，您是想提及新客户还是拼写有误？"
        )
        whenever(mockInputParserService.parseIntent("查一下新客户")).doReturn(ambiguousResult)
        
        val expectedClarification = UiState.AwaitingClarification(
            question = "系统发现 '新客户' 似乎不在通讯录中，您是想提及新客户还是拼写有误？",
            clarificationType = ClarificationType.AMBIGUOUS_PERSON,
            candidates = emptyList()
        )
        whenever(
            mockEntityDisambiguationService.startDisambiguation(
                originalInput = any(),
                originalMode = any(),
                ambiguousName = any(),
                candidates = any()
            )
        ).doReturn(expectedClarification)
        
        whenever(mockContextBuilder.build(any(), any(), any(), any())).doReturn(
            EnhancedContext(userText = "查一下新客户", modeMetadata = ModeMetadata(currentMode = com.smartsales.prism.domain.model.Mode.ANALYST))
        )
        // Router mock since processInput now checks lightningRouter
        whenever(mockLightningRouter.evaluateIntent(any())).doReturn(
            com.smartsales.prism.domain.analyst.RouterResult(
                queryQuality = com.smartsales.prism.domain.analyst.QueryQuality.CRM_TASK,
                infoSufficient = true,
                response = ""
            )
        )
        // Act
        val result = orchestrator.processInput("查一下新客户")

        // Assert
        assertTrue("Expected AwaitingClarification but got $result", result is UiState.AwaitingClarification)
        val clarification = result as UiState.AwaitingClarification
        assertEquals("系统发现 '新客户' 似乎不在通讯录中，您是想提及新客户还是拼写有误？", clarification.question)
        assertEquals(ClarificationType.AMBIGUOUS_PERSON, clarification.clarificationType)
        assertTrue(clarification.candidates.isEmpty())
    }

    @Test
    fun `when InputParser returns Success, Orchestrator delegates correctly and returns Idle for Mascot`() = runTest {
        // Arrange
        val successResult = ParseResult.Success(
            resolvedEntityIds = listOf("person-1"),
            temporalIntent = null,
            rawParsedJson = "{}"
        )
        whenever(mockInputParserService.parseIntent("给张三打个电话")).doReturn(successResult)
        whenever(mockContextBuilder.getSessionHistory()).doReturn(emptyList())
        


        whenever(mockContextBuilder.build(any(), any(), any(), any())).doReturn(
            EnhancedContext(userText = "给张三打个电话", modeMetadata = ModeMetadata(currentMode = com.smartsales.prism.domain.model.Mode.ANALYST))
        )
        whenever(mockLightningRouter.evaluateIntent(any())).doReturn(
            com.smartsales.prism.domain.analyst.RouterResult(
                queryQuality = com.smartsales.prism.domain.analyst.QueryQuality.NOISE,
                infoSufficient = false,
                response = ""
            )
        )
        // Act
        val result = orchestrator.processInput("给张三打个电话")

        // Assert
        assertTrue(result is UiState.Idle)
    }
}
