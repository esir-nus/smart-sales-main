package com.smartsales.core.pipeline

import com.smartsales.core.test.fakes.FakeUnifiedPipeline
import com.smartsales.core.test.fakes.FakeMascotService
import com.smartsales.core.test.fakes.FakeContextBuilder
import com.smartsales.core.test.fakes.FakeExecutor
import com.smartsales.core.test.fakes.FakeScheduleBoard
import com.smartsales.core.test.fakes.FakeEntityWriter
import com.smartsales.core.test.fakes.FakeAliasCache
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.smartsales.core.llm.ExecutorResult

/**
 * L2 Simulated Test for IntentOrchestrator utilizing Mock Eviction.
 * This test verifies the Phase 0 Gateway routing based on LightningRouter's LLM outputs.
 */
class RealIntentOrchestratorTest {

    private lateinit var orchestrator: IntentOrchestrator
    private lateinit var fakeContextBuilder: FakeContextBuilder
    private lateinit var realLightningRouter: RealLightningRouter
    private lateinit var fakeExecutor: FakeExecutor
    private lateinit var promptCompiler: PromptCompiler
    private lateinit var fakeMascotService: FakeMascotService
    private lateinit var fakeUnifiedPipeline: FakeUnifiedPipeline
    private lateinit var fakeScheduledTaskRepository: FakeScheduledTaskRepository
    private lateinit var fakeScheduleBoard: FakeScheduleBoard
    private lateinit var fakeEntityWriter: FakeEntityWriter
    private lateinit var fakeAliasCache: FakeAliasCache
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        fakeContextBuilder = FakeContextBuilder()
        fakeExecutor = FakeExecutor()
        promptCompiler = PromptCompiler()
        realLightningRouter = RealLightningRouter(fakeExecutor, promptCompiler)
        fakeMascotService = FakeMascotService()
        fakeUnifiedPipeline = FakeUnifiedPipeline()
        fakeScheduledTaskRepository = FakeScheduledTaskRepository()
        fakeScheduleBoard = FakeScheduleBoard()
        fakeEntityWriter = FakeEntityWriter()
        fakeAliasCache = FakeAliasCache()

        orchestrator = IntentOrchestrator(
            contextBuilder = fakeContextBuilder,
            lightningRouter = realLightningRouter,
            mascotService = fakeMascotService,
            unifiedPipeline = fakeUnifiedPipeline,
            scheduledTaskRepository = fakeScheduledTaskRepository,
            scheduleBoard = fakeScheduleBoard,
            entityWriter = fakeEntityWriter,
            aliasCache = fakeAliasCache,
            appScope = testScope
        )
    }

    @Test
    fun `test all intent routing scenarios sequentially`() = runTest {
        // --- SCENARIO 1: NOISE ---
        setup()
        var input = "嗯嗯"
        fakeExecutor.enqueueResponse(ExecutorResult.Success(
            """{"query_quality": "noise", "response": "..."}"""
        ))
        var result = orchestrator.processInput(input).firstOrNull()

        assertEquals("NOISE should emit MascotIntercepted", PipelineResult.MascotIntercepted, result)
        assertEquals(1, fakeMascotService.interactions.size)
        assertTrue(fakeUnifiedPipeline.processedInputs.isEmpty())

        // --- SCENARIO 2: GREETING ---
        setup()
        input = "你好啊"
        fakeExecutor.enqueueResponse(ExecutorResult.Success(
            """{"query_quality": "greeting", "response": "我在"}"""
        ))
        result = orchestrator.processInput(input).firstOrNull()

        assertEquals("GREETING should emit MascotIntercepted", PipelineResult.MascotIntercepted, result)
        assertEquals(1, fakeMascotService.interactions.size)
        assertTrue(fakeUnifiedPipeline.processedInputs.isEmpty())

        // --- SCENARIO 3: VAGUE ---
        setup()
        input = "那个功能"
        val clarification = "你指的是具体哪个功能？"
        fakeExecutor.enqueueResponse(ExecutorResult.Success(
            """{"query_quality": "vague", "response": "$clarification"}"""
        ))
        result = orchestrator.processInput(input).firstOrNull()

        assertNotNull(result)
        assertTrue(result is PipelineResult.ConversationalReply)
        assertEquals(clarification, (result as PipelineResult.ConversationalReply).text)
        assertTrue(fakeMascotService.interactions.isEmpty())
        assertTrue(fakeUnifiedPipeline.processedInputs.isEmpty())

        // --- SCENARIO 4: DEEP_ANALYSIS ---
        setup()
        input = "帮我分析一下华为的最新进展"
        fakeExecutor.enqueueResponse(ExecutorResult.Success(
            """{"query_quality": "deep_analysis", "response": ""}"""
        ))
        val expectedPipelineResult = PipelineResult.ConversationalReply("这是分析结果")
        fakeUnifiedPipeline.nextResultFlow = kotlinx.coroutines.flow.flowOf(expectedPipelineResult)
        
        result = orchestrator.processInput(input).firstOrNull()

        assertNotNull(result)
        assertEquals(expectedPipelineResult, result)
        assertEquals(1, fakeUnifiedPipeline.processedInputs.size)
        assertEquals(input, fakeUnifiedPipeline.processedInputs[0].rawText)

        // --- SCENARIO 5: SIMPLE_QA ---
        setup()
        input = "谁是苹果的CEO"
        val answer = "库克"
        fakeExecutor.enqueueResponse(ExecutorResult.Success(
            """{"query_quality": "simple_qa", "response": "$answer"}"""
        ))
        val expectedQaResult = PipelineResult.ConversationalReply(answer)
        fakeUnifiedPipeline.nextResultFlow = kotlinx.coroutines.flow.flowOf(expectedQaResult)
        
        result = orchestrator.processInput(input).firstOrNull()

        assertNotNull(result)
        assertEquals(expectedQaResult, result)
        assertEquals(1, fakeUnifiedPipeline.processedInputs.size)
        assertEquals(input, fakeUnifiedPipeline.processedInputs[0].rawText)
        assertTrue(fakeMascotService.interactions.isEmpty())
    }
}
