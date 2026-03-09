package com.smartsales.core.pipeline

import com.smartsales.core.context.ContextDepth
import com.smartsales.core.context.EnhancedContext
import com.smartsales.prism.domain.model.Mode
import com.smartsales.core.test.fakes.FakeContextBuilder
import com.smartsales.core.test.fakes.FakeLightningRouter
import com.smartsales.core.test.fakes.FakeMascotService
import com.smartsales.core.test.fakes.FakeUnifiedPipeline
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * L1 Logic Verification Test - Intent Orchestrator
 * 
 * Verifies that inputs are correctly routed to the MascotService or the UnifiedPipeline
 * based on the LightningRouter evaluation.
 * 
 * Anti-Illusion Protocol Compliant (No Mockito).
 */
class IntentOrchestratorTest {

    private lateinit var fakeContextBuilder: FakeContextBuilder
    private lateinit var fakeLightningRouter: FakeLightningRouter
    private lateinit var fakeMascotService: FakeMascotService
    private lateinit var fakeUnifiedPipeline: FakeUnifiedPipeline
    
    private lateinit var orchestrator: IntentOrchestrator

    @Before
    fun setup() {
        fakeContextBuilder = FakeContextBuilder()
        fakeLightningRouter = FakeLightningRouter()
        fakeMascotService = FakeMascotService()
        fakeUnifiedPipeline = FakeUnifiedPipeline()
        
        orchestrator = IntentOrchestrator(
            contextBuilder = fakeContextBuilder,
            lightningRouter = fakeLightningRouter,
            mascotService = fakeMascotService,
            unifiedPipeline = fakeUnifiedPipeline
        )
    }

    @Test
    fun `test all intent routing scenarios sequentially`() = runTest {
        // --- SCENARIO 1: NOISE ---
        setup()
        var input = "嗯嗯"
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.NOISE, true, ""))
        var result = orchestrator.processInput(input).firstOrNull()
        
        assertNull("NOISE should emit nothing", result)
        assertEquals(1, fakeMascotService.interactions.size)
        assertTrue(fakeMascotService.interactions[0] is MascotInteraction.Text)
        assertEquals(input, (fakeMascotService.interactions[0] as MascotInteraction.Text).content)
        assertTrue(fakeUnifiedPipeline.processedInputs.isEmpty())

        // --- SCENARIO 2: GREETING ---
        setup()
        input = "你好啊"
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.GREETING, true, ""))
        result = orchestrator.processInput(input).firstOrNull()

        assertNull("GREETING should emit nothing", result)
        assertEquals(1, fakeMascotService.interactions.size)
        assertEquals(input, (fakeMascotService.interactions[0] as MascotInteraction.Text).content)
        assertTrue(fakeUnifiedPipeline.processedInputs.isEmpty())

        // --- SCENARIO 3: VAGUE ---
        setup()
        input = "那个功能"
        val clarification = "你指的是具体哪个功能？"
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.VAGUE, false, clarification))
        result = orchestrator.processInput(input).firstOrNull()

        assertNotNull(result)
        assertTrue(result is PipelineResult.ConversationalReply)
        assertEquals(clarification, (result as PipelineResult.ConversationalReply).text)
        assertTrue(fakeMascotService.interactions.isEmpty())

        // --- SCENARIO 4: DEEP_ANALYSIS ---
        setup()
        input = "帮我分析一下华为的最新进展"
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        val expectedPipelineResult = PipelineResult.ConversationalReply("这是分析结果")
        fakeUnifiedPipeline.nextResultFlow = flowOf(expectedPipelineResult)
        
        result = orchestrator.processInput(input).firstOrNull()

        assertNotNull(result)
        assertEquals(expectedPipelineResult, result)
        assertEquals(1, fakeUnifiedPipeline.processedInputs.size)
        assertEquals(input, fakeUnifiedPipeline.processedInputs[0].rawText)
    }
}
