package com.smartsales.prism.data.real

import com.smartsales.prism.domain.activity.AgentActivityController
import com.smartsales.prism.domain.coach.CoachPipeline
import com.smartsales.prism.domain.coach.CoachResponse
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
    private lateinit var mockCoachPipeline: CoachPipeline
    private lateinit var mockEntityWriter: EntityWriter
    private lateinit var mockEntityRepo: EntityRepository
    private lateinit var mockInputParserService: InputParserService
    private lateinit var mockEntityDisambiguationService: com.smartsales.prism.domain.disambiguation.EntityDisambiguationService
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
        mockCoachPipeline = mock()
        mockEntityWriter = mock()
        mockEntityRepo = mock()
        mockInputParserService = mock()
        mockEntityDisambiguationService = mock()
        telemetry = com.smartsales.prism.data.fakes.FakePipelineTelemetry()

        kotlinx.coroutines.runBlocking {
            whenever(mockEntityDisambiguationService.process(any())).doReturn(com.smartsales.prism.domain.disambiguation.DisambiguationResult.PassThrough)
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
            coachPipeline = mockCoachPipeline,
            entityWriter = mockEntityWriter,
            entityRepository = mockEntityRepo,
            inputParserService = mockInputParserService,
            entityDisambiguationService = mockEntityDisambiguationService,
            telemetry = telemetry
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
        
        // Ensure Mode is set to COACH (default) or whatever processInput delegates to
        orchestrator.switchMode(com.smartsales.prism.domain.model.Mode.COACH)

        // Act
        val result = orchestrator.processInput("查一下新客户")

        // Assert
        assertTrue(result is UiState.AwaitingClarification)
        val clarification = result as UiState.AwaitingClarification
        assertEquals("系统发现 '新客户' 似乎不在通讯录中，您是想提及新客户还是拼写有误？", clarification.question)
        assertEquals(ClarificationType.AMBIGUOUS_PERSON, clarification.clarificationType)
        assertTrue(clarification.candidates.isEmpty())
    }

    @Test
    fun `when InputParser returns Success, Orchestrator delegates to Pipeline with resolved IDs`() = runTest {
        // Arrange
        val successResult = ParseResult.Success(
            resolvedEntityIds = listOf("person-1"),
            temporalIntent = null,
            rawParsedJson = "{}"
        )
        whenever(mockInputParserService.parseIntent("给张三打个电话")).doReturn(successResult)
        whenever(mockContextBuilder.getSessionHistory()).doReturn(emptyList())
        val mockResponse = CoachResponse.Chat(content = "好的", suggestAnalyst = false)
        whenever(mockCoachPipeline.process("给张三打个电话", emptyList(), listOf("person-1"))).doReturn(mockResponse)
        
        orchestrator.switchMode(com.smartsales.prism.domain.model.Mode.COACH)

        // Act
        val result = orchestrator.processInput("给张三打个电话")

        // Assert
        assertTrue(result is UiState.Response)
        val response = result as UiState.Response
        assertEquals("好的", response.content)
    }
}
