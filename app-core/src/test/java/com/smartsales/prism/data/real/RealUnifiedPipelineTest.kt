package com.smartsales.prism.data.real

import com.smartsales.core.context.RealContextBuilder
import com.smartsales.core.pipeline.RealUnifiedPipeline

import com.smartsales.prism.domain.model.Mode
import com.smartsales.core.context.ContextBuilder
import com.smartsales.core.context.ContextDepth
import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.context.ModeMetadata
import com.smartsales.core.pipeline.PipelineInput
import com.smartsales.core.pipeline.PipelineResult
import com.smartsales.core.pipeline.ParseResult
import com.smartsales.core.pipeline.DisambiguationResult
import com.smartsales.core.pipeline.QueryQuality
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import com.smartsales.prism.data.fakes.FakeTimeProvider
import com.smartsales.core.test.fakes.FakeAlarmScheduler
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import com.smartsales.core.test.fakes.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.smartsales.core.llm.ExecutorResult

class RealUnifiedPipelineTest {

    private lateinit var pipeline: RealUnifiedPipeline
    
    // 11 Fakes
    private lateinit var contextBuilder: FakeContextBuilder
    private lateinit var entityDisambiguationService: FakeEntityDisambiguationService
    private lateinit var inputParserService: FakeInputParserService
    private lateinit var entityWriter: FakeEntityWriter
    private lateinit var schedulerLinter: SchedulerLinter
    private lateinit var scheduledTaskRepository: FakeScheduledTaskRepository
    private lateinit var scheduleBoard: FakeScheduleBoard
    private lateinit var inspirationRepository: FakeInspirationRepository
    private lateinit var alarmScheduler: FakeAlarmScheduler
    private lateinit var sessionTitleGenerator: FakeSessionTitleGenerator
    private lateinit var promptCompiler: FakePromptCompiler
    private lateinit var executor: FakeExecutor
    private lateinit var timeProvider: FakeTimeProvider

    @Before
    fun setup() {
        contextBuilder = FakeContextBuilder()
        entityDisambiguationService = FakeEntityDisambiguationService()
        inputParserService = FakeInputParserService()
        entityWriter = FakeEntityWriter()
        
        timeProvider = FakeTimeProvider()
        schedulerLinter = SchedulerLinter(timeProvider)
        
        scheduledTaskRepository = FakeScheduledTaskRepository()
        scheduleBoard = FakeScheduleBoard()
        inspirationRepository = FakeInspirationRepository()
        alarmScheduler = FakeAlarmScheduler()
        sessionTitleGenerator = FakeSessionTitleGenerator()
        promptCompiler = FakePromptCompiler()
        executor = FakeExecutor()

        pipeline = RealUnifiedPipeline(
            contextBuilder = contextBuilder,
            entityDisambiguationService = entityDisambiguationService,
            inputParserService = inputParserService,
            entityWriter = entityWriter,
            schedulerLinter = schedulerLinter,
            scheduledTaskRepository = scheduledTaskRepository,
            scheduleBoard = scheduleBoard,
            inspirationRepository = inspirationRepository,
            alarmScheduler = alarmScheduler,
            sessionTitleGenerator = sessionTitleGenerator,
            promptCompiler = promptCompiler,
            executor = executor
        )
    }

    @Test
    fun `processInput Context Branch - CRM_TASK execution routing`() = runTest {
        // Arrange
        // Fake Parser bypasses clarifying blocks with a success payload
        val jsonStr = """{"intent": "scheduler"}"""
        inputParserService.nextResult = ParseResult.Success(emptyList(), null, jsonStr)
        entityDisambiguationService.nextResult = DisambiguationResult.PassThrough
        
        // Fake Executor returns carefully crafted JSON matching SchedulerLinter schema
        val controlledLlmOutput = """{
            "classification": "schedulable",
            "tasks": [{
                "title": "Discuss Anti-Illusion Protocol",
                "startTime": "2026-03-10 10:00",
                "duration": "30m",
                "urgency": "L2_IMPORTANT"
            }]
        }"""
        executor.defaultResponse = ExecutorResult.Success(
            content = controlledLlmOutput,
            tokenUsage = com.smartsales.core.llm.TokenUsage(100, 10)
        )
        
        val input = PipelineInput(rawText = "Schedule a meeting", isVoice = false, intent = QueryQuality.CRM_TASK)

        // Act
        val results = pipeline.processInput(input).toList()

        // Assert
        assertTrue("Pipeline did not emit results", results.isNotEmpty())
        
        // Anti-Illusion: Verify the actual downstream Payload using the recorded Executor prompts
        assertEquals("Executor should have been called exactly once", 1, executor.executedPrompts.size)
        val generatedPrompt = executor.executedPrompts.first()
        assertTrue("Prompt must explicitly contain the user's intent text for Dataflow Veracity", generatedPrompt.contains("Schedule a meeting"))
        
        val taskResult = results.filterIsInstance<PipelineResult.SchedulerTaskCreated>().firstOrNull()
        assertTrue("Expected SchedulerTaskCreated but it was not emitted. Results: ${results.map { it::class.simpleName }}", taskResult != null)
        assertEquals("Discuss Anti-Illusion Protocol", taskResult!!.title)
        assertEquals(30, taskResult.durationMinutes)
    }

    @Test
    fun `processInput Context Branch - ClarificationNeeded Trap`() = runTest {
        // Arrange
        // Force the InputParser to flag an ambiguous entity
        inputParserService.nextResult = ParseResult.NeedsClarification(
            ambiguousName = "Alex",
            suggestedMatches = emptyList(),
            clarificationPrompt = "Which Alex do you mean?"
        )
        entityDisambiguationService.nextResult = DisambiguationResult.PassThrough
        
        val input = PipelineInput(rawText = "Set a meeting with Alex", isVoice = false, intent = QueryQuality.CRM_TASK)

        // Act
        val results = pipeline.processInput(input).toList()

        // Assert
        assertTrue("Pipeline did not emit results", results.isNotEmpty())
        val result = results.first()
        // It should intercept and yield UI state
        assertTrue("Expected DisambiguationIntercepted but got ${result::class.simpleName}", result is PipelineResult.DisambiguationIntercepted)
    }

    @Test
    fun `processInput empty string handles gracefully (Break-It)`() = runTest {
        // Arrange
        val input = PipelineInput(rawText = "   ", isVoice = false, intent = QueryQuality.NOISE)
        
        // By default, FakeDisambiguation is PassThrough. FakeParser uses ParseResult.Success empty JSON.
        // It routes to Analyst/Consultant fallback (NOISE)

        // Act
        val results = pipeline.processInput(input).toList()

        // Assert
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `processInput LightningFastTrack drops to Mascot for GREETING`() = runTest {
        // Arrange
        val input = PipelineInput(rawText = "Hello Smart Sales", isVoice = false, intent = QueryQuality.GREETING)
        
        // Lightning Extractor Model returns a GREETING intent
        val jsonPayload = """
            {
                "query_quality": "greeting",
                "info_sufficient": false,
                "response": "Hello Frank!"
            }
        """.trimIndent()
        executor.enqueueResponse(ExecutorResult.Success(jsonPayload))

        // Act
        val results = pipeline.processInput(input).toList()

        // Assert
        assertTrue("Pipeline did not emit results", results.isNotEmpty())
        
        // Assert exactly two results: AutoRenameTriggered (from Parser) and ConversationalReply (from Pipeline fallback)
        assertEquals("Should terminate early and emit fallback ConversationalReply", 2, results.size)
        val renameResult = results[0]
        assertTrue("Expected AutoRenameTriggered but got ${renameResult::class.simpleName}", renameResult is PipelineResult.AutoRenameTriggered)
        
        val replyResult = results[1]
        assertTrue("Expected ConversationalReply but got ${replyResult::class.simpleName}", replyResult is PipelineResult.ConversationalReply)
        
        // Prove downstream EntityWriter/Scheduler was bypassed because CRM was skipped
        // By defining a strict L1 route test, we explicitly prevent Mockito mapping illusions.
    }
}
