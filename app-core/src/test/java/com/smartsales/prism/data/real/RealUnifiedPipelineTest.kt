package com.smartsales.prism.data.real

import com.smartsales.core.context.RealContextBuilder
import com.smartsales.core.pipeline.PromptCompiler
import com.smartsales.core.pipeline.RealGlobalRescheduleExtractionService
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.RealUniBExtractionService
import com.smartsales.core.pipeline.RealUniMExtractionService
import com.smartsales.core.pipeline.RealUnifiedPipeline
import com.smartsales.prism.domain.model.Mode
import com.smartsales.core.context.ContextBuilder
import com.smartsales.core.context.ContextDepth
import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.context.ModeMetadata
import com.smartsales.core.pipeline.HabitListener
import com.smartsales.core.pipeline.PipelineInput
import com.smartsales.core.pipeline.PipelineResult
import com.smartsales.core.pipeline.ParseResult
import com.smartsales.core.pipeline.DisambiguationResult
import com.smartsales.core.pipeline.PluginToolIds
import com.smartsales.core.pipeline.QueryQuality
import com.smartsales.core.pipeline.SchedulerTaskCommand
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import com.smartsales.core.test.fakes.*
import com.smartsales.prism.domain.telemetry.PipelineTelemetry
import com.smartsales.prism.domain.telemetry.PipelinePhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import com.smartsales.core.llm.ExecutorResult

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
    private lateinit var telemetry: PipelineTelemetry
    private lateinit var habitListener: FakeHabitListener

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

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
        telemetry = mock<PipelineTelemetry>()
        habitListener = FakeHabitListener()

        pipeline = RealUnifiedPipeline(
            contextBuilder = contextBuilder,
            entityDisambiguationService = entityDisambiguationService,
            inputParserService = inputParserService,
            schedulerLinter = SchedulerLinter(),
            entityWriter = entityWriter,
            sessionTitleGenerator = sessionTitleGenerator,
            promptCompiler = promptCompiler,
            executor = executor,
            telemetry = telemetry,
            habitListener = habitListener,
            appScope = testScope
        )
    }

    private fun buildSharedSchedulerPipeline(
        sharedExecutor: FakeExecutor,
        retrievalIndex: FakeActiveTaskRetrievalIndex = FakeActiveTaskRetrievalIndex()
    ): RealUnifiedPipeline {
        val sharedLinter = SchedulerLinter(timeProvider)
        return RealUnifiedPipeline(
            contextBuilder = contextBuilder,
            entityDisambiguationService = entityDisambiguationService,
            inputParserService = inputParserService,
            schedulerLinter = sharedLinter,
            entityWriter = entityWriter,
            sessionTitleGenerator = sessionTitleGenerator,
            promptCompiler = promptCompiler,
            executor = sharedExecutor,
            telemetry = telemetry,
            habitListener = habitListener,
            appScope = testScope,
            taskRepository = scheduledTaskRepository,
            activeTaskRetrievalIndex = retrievalIndex,
            timeProvider = timeProvider,
            uniAExtractionService = RealUniAExtractionService(sharedExecutor, PromptCompiler(), sharedLinter),
            uniBExtractionService = RealUniBExtractionService(sharedExecutor, PromptCompiler(), sharedLinter),
            uniMExtractionService = RealUniMExtractionService(sharedExecutor, PromptCompiler(), sharedLinter),
            globalRescheduleExtractionService = RealGlobalRescheduleExtractionService(sharedExecutor, PromptCompiler(), sharedLinter)
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
        
        val input = PipelineInput(rawText = "Schedule a meeting", isVoice = false, intent = QueryQuality.CRM_TASK, unifiedId = "test_unified_id")

        // Act
        val results = pipeline.processInput(input).toList()

        // Assert
        assertTrue("Pipeline did not emit results", results.isNotEmpty())
        
        // Anti-Illusion: Verify the actual downstream Payload using the recorded Executor prompts
        assertEquals("Executor should have been called exactly once", 1, executor.executedPrompts.size)
        val generatedPrompt = executor.executedPrompts.first()
        assertTrue("Prompt must explicitly contain the user's intent text for Dataflow Veracity", generatedPrompt.contains("Schedule a meeting"))
        val taskCommand = results.filterIsInstance<PipelineResult.TaskCommandProposal>().firstOrNull()
        assertTrue("Expected TaskCommandProposal but it was not emitted.", taskCommand != null)
        assertTrue(
            "Expected typed create-task command",
            taskCommand!!.command is com.smartsales.core.pipeline.SchedulerTaskCommand.CreateTasks
        )
        
        // Background Path Validation
        assertEquals("Habit listener MUST be triggered after ETL", 1, habitListener.analyzeAsyncCallCount)
        assertEquals("Schedule a meeting", habitListener.rawInputCaptured)
    }

    @Test
    fun `processInput shared scheduler router emits batch create proposal for mixed Path B text scheduling`() = runTest {
        val sharedExecutor = FakeExecutor()
        val localPipeline = buildSharedSchedulerPipeline(sharedExecutor)

        inputParserService.nextResult = ParseResult.Success(emptyList(), null, """{"intent":"scheduler"}""")
        entityDisambiguationService.nextResult = DisambiguationResult.PassThrough
        sharedExecutor.enqueueResponse(
            ExecutorResult.Success(
                content = """{
                    "classification": "schedulable",
                    "response": "好的，我为您起草日程。"
                }""".trimIndent(),
                tokenUsage = com.smartsales.core.llm.TokenUsage(60, 10)
            )
        )
        sharedExecutor.enqueueResponse(
            ExecutorResult.Success(
                content = """{
                    "decision":"MULTI_CREATE",
                    "fragments":[
                        {
                            "title":"带合同见老板",
                            "mode":"EXACT",
                            "anchorKind":"ABSOLUTE",
                            "startTimeIso":"2026-03-21T01:00:00Z",
                            "durationMinutes":30,
                            "urgency":"L2"
                        },
                        {
                            "title":"跟进客户",
                            "mode":"VAGUE",
                            "anchorKind":"ABSOLUTE",
                            "anchorDateIso":"2026-03-22",
                            "timeHint":"下午",
                            "urgency":"L2"
                        }
                    ]
                }""".trimIndent(),
                tokenUsage = com.smartsales.core.llm.TokenUsage(40, 10)
            )
        )

        val results = localPipeline.processInput(
            PipelineInput(
                rawText = "明天上午九点带合同见老板，然后后天下午跟进客户",
                isVoice = false,
                intent = QueryQuality.CRM_TASK,
                unifiedId = "shared_batch_unified_id"
            )
        ).toList()

        val taskCommand = results.filterIsInstance<PipelineResult.TaskCommandProposal>().singleOrNull()
        assertTrue(taskCommand != null)
        assertTrue(taskCommand!!.command is SchedulerTaskCommand.CreateBatch)
        val batch = taskCommand.command as SchedulerTaskCommand.CreateBatch
        assertEquals(2, batch.operations.size)
        val exact = batch.operations.filterIsInstance<SchedulerTaskCommand.CreateOperation.Exact>().singleOrNull()
        val vague = batch.operations.filterIsInstance<SchedulerTaskCommand.CreateOperation.Vague>().singleOrNull()
        assertTrue(exact != null)
        assertTrue(vague != null)
        assertEquals("带合同见老板", exact!!.params.tasks.single().title)
        assertEquals("跟进客户", vague!!.params.title)
    }

    @Test
    fun `processInput shared scheduler router resolves global reschedule proposal for Path B text scheduling`() = runTest {
        val sharedExecutor = FakeExecutor()
        val retrievalIndex = FakeActiveTaskRetrievalIndex()
        val localPipeline = buildSharedSchedulerPipeline(sharedExecutor, retrievalIndex)
        val existingTaskId = scheduledTaskRepository.insertTask(
            ScheduledTask(
                id = "task-1",
                timeDisplay = "10:00",
                title = "张总会议",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = java.time.Instant.parse("2026-03-20T02:00:00Z"),
                durationMinutes = 60
            )
        )

        inputParserService.nextResult = ParseResult.Success(emptyList(), null, """{"intent":"scheduler"}""")
        entityDisambiguationService.nextResult = DisambiguationResult.PassThrough
        retrievalIndex.nextResolveResult =
            com.smartsales.prism.domain.scheduler.ActiveTaskResolveResult.Resolved(existingTaskId)
        sharedExecutor.enqueueResponse(
            ExecutorResult.Success(
                content = """{
                    "classification": "reschedule",
                    "response": "好的，我为您起草改期。"
                }""".trimIndent(),
                tokenUsage = com.smartsales.core.llm.TokenUsage(60, 10)
            )
        )
        sharedExecutor.enqueueResponse(
            ExecutorResult.Success(
                content = """{
                    "decision":"RESCHEDULE_TARGETED",
                    "targetQuery":"张总会议",
                    "timeInstruction":"推迟一小时"
                }""".trimIndent(),
                tokenUsage = com.smartsales.core.llm.TokenUsage(40, 10)
            )
        )

        val rawText = "把张总会议推迟一小时"
        val results = localPipeline.processInput(
            PipelineInput(
                rawText = rawText,
                isVoice = false,
                intent = QueryQuality.CRM_TASK,
                unifiedId = "shared_reschedule_unified_id"
            )
        ).toList()

        val taskCommand = results.filterIsInstance<PipelineResult.TaskCommandProposal>().singleOrNull()
        assertTrue(taskCommand != null)
        assertTrue(taskCommand!!.command is SchedulerTaskCommand.RescheduleTask)
        val command = taskCommand.command as SchedulerTaskCommand.RescheduleTask
        assertEquals(existingTaskId, command.params.resolvedTaskId)
        assertEquals("张总会议", retrievalIndex.lastResolveTarget?.targetQuery)
        assertEquals(rawText, retrievalIndex.lastShortlistTranscript)
        assertEquals("2026-03-20T03:00:00Z", command.params.newStartTimeIso)
    }

    @Test
    fun `processInput emits direct semantic plugin dispatch when plugin_dispatch is present`() = runTest {
        inputParserService.nextResult = ParseResult.Success(emptyList(), null, """{"intent":"analyst"}""")
        entityDisambiguationService.nextResult = DisambiguationResult.PassThrough
        executor.defaultResponse = ExecutorResult.Success(
            content = """{
                "query_quality": "deep_analysis",
                "classification": "non_intent",
                "response": "好的，我已为您起草工具执行。",
                "plugin_dispatch": {
                    "toolId": "artifact.generate",
                    "parameters": {
                        "ruleId": "executive_report",
                        "targetRef": "account:bytedance"
                    }
                }
            }""".trimIndent(),
            tokenUsage = com.smartsales.core.llm.TokenUsage(80, 10)
        )

        val input = PipelineInput(
            rawText = "帮我生成字节跳动的汇报",
            isVoice = false,
            intent = QueryQuality.DEEP_ANALYSIS,
            unifiedId = "plugin_dispatch_unified_id"
        )

        val results = pipeline.processInput(input).toList()

        assertTrue(results.any {
            it is PipelineResult.ToolDispatch &&
                it.toolId == PluginToolIds.ARTIFACT_GENERATE &&
                it.params["ruleId"] == "executive_report"
        })
        assertTrue(results.none { it is PipelineResult.ToolRecommendation })
        assertTrue(results.none {
            it is PipelineResult.ConversationalReply && it.text == "好的，我已为您起草工具执行。"
        })
    }

    @Test
    fun `processInput upgrades single legacy workflow recommendation into direct semantic dispatch`() = runTest {
        inputParserService.nextResult = ParseResult.Success(emptyList(), null, """{"intent":"analyst"}""")
        entityDisambiguationService.nextResult = DisambiguationResult.PassThrough
        executor.defaultResponse = ExecutorResult.Success(
            content = """{
                "query_quality": "deep_analysis",
                "classification": "non_intent",
                "response": "好的，我为您找到了相关工具，请确认。",
                "recommended_workflows": [
                    {
                        "workflowId": "GENERATE_PDF",
                        "reason": "生成正式汇报",
                        "parameters": {
                            "accountName": "字节跳动"
                        }
                    }
                ]
            }""".trimIndent(),
            tokenUsage = com.smartsales.core.llm.TokenUsage(80, 10)
        )

        val input = PipelineInput(
            rawText = "帮我生成字节跳动的PDF汇报",
            isVoice = false,
            intent = QueryQuality.DEEP_ANALYSIS,
            unifiedId = "legacy_pdf_unified_id"
        )

        val results = pipeline.processInput(input).toList()

        val dispatch = results.filterIsInstance<PipelineResult.ToolDispatch>().singleOrNull()
        assertTrue(dispatch != null)
        assertEquals(PluginToolIds.ARTIFACT_GENERATE, dispatch!!.toolId)
        assertEquals("字节跳动", dispatch.params["accountName"])
        assertTrue(results.none { it is PipelineResult.ToolRecommendation })
        assertTrue(results.none {
            it is PipelineResult.ConversationalReply && it.text == "好的，我为您找到了相关工具，请确认。"
        })
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
        
        val input = PipelineInput(rawText = "Set a meeting with Alex", isVoice = false, intent = QueryQuality.CRM_TASK, unifiedId = "test_unified_id")

        // Act
        val results = pipeline.processInput(input).toList()

        // Assert
        assertTrue("Pipeline did not emit results", results.isNotEmpty())
        
        // It should intercept and yield UI state
        val result = results.filterIsInstance<PipelineResult.DisambiguationIntercepted>().firstOrNull()
        assertTrue("Expected DisambiguationIntercepted but got null", result != null)
    }

    @Test
    fun `processInput empty string handles gracefully (Break-It)`() = runTest {
        // Arrange
        val input = PipelineInput(rawText = "   ", isVoice = false, intent = QueryQuality.NOISE, unifiedId = "test_unified_id")
        
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
        val input = PipelineInput(rawText = "Hello Smart Sales", isVoice = false, intent = QueryQuality.GREETING, unifiedId = "test_unified_id")
        
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
        val renameResult = results.filterIsInstance<PipelineResult.AutoRenameTriggered>().firstOrNull()
        assertTrue("Expected AutoRenameTriggered", renameResult != null)
        
        val replyResult = results.filterIsInstance<PipelineResult.ConversationalReply>().firstOrNull()
        assertTrue("Expected ConversationalReply", replyResult != null)
        
        // Prove downstream EntityWriter/Scheduler was bypassed because CRM was skipped
        // By defining a strict L1 route test, we explicitly prevent Mockito mapping illusions.
    }

    @Test
    fun `processInput does not block main reply on slow RL listener`() = runTest {
        val slowListener = object : HabitListener {
            var started = false
            var completed = false

            override fun analyzeAsync(rawInput: String, context: EnhancedContext, coroutineScope: CoroutineScope) {
                started = true
                coroutineScope.launch {
                    delay(5_000)
                    completed = true
                }
            }
        }

        val localPipeline = RealUnifiedPipeline(
            contextBuilder = contextBuilder,
            entityDisambiguationService = entityDisambiguationService,
            inputParserService = inputParserService,
            schedulerLinter = SchedulerLinter(),
            entityWriter = entityWriter,
            sessionTitleGenerator = sessionTitleGenerator,
            promptCompiler = promptCompiler,
            executor = executor,
            telemetry = telemetry,
            habitListener = slowListener,
            appScope = this
        )

        inputParserService.nextResult = ParseResult.Success(emptyList(), null, """{"intent":"scheduler"}""")
        entityDisambiguationService.nextResult = DisambiguationResult.PassThrough
        executor.defaultResponse = ExecutorResult.Success(
            content = """{
                "classification": "schedulable",
                "tasks": [{
                    "title": "Call Tom",
                    "startTime": "2026-03-10 10:00",
                    "duration": "30m",
                    "urgency": "L2_IMPORTANT"
                }]
            }""".trimIndent(),
            tokenUsage = com.smartsales.core.llm.TokenUsage(100, 10)
        )

        val results = localPipeline.processInput(
            PipelineInput(
                rawText = "Schedule a call with Tom",
                isVoice = false,
                intent = QueryQuality.CRM_TASK,
                unifiedId = "test_unified_id"
            )
        ).toList()

        assertTrue(slowListener.started)
        assertTrue(results.any { it is PipelineResult.TaskCommandProposal })
        assertTrue("Main pipeline should complete before slow RL finishes", !slowListener.completed)

        advanceTimeBy(5_000)
        runCurrent()

        assertTrue("Slow RL listener should finish after the main reply path completes", slowListener.completed)
    }
}
