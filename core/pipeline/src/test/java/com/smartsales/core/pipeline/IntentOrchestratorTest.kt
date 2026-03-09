package com.smartsales.core.pipeline

import com.smartsales.core.context.ContextDepth
import com.smartsales.core.context.EnhancedContext
import com.smartsales.prism.domain.model.Mode
import com.smartsales.core.test.fakes.FakeContextBuilder
import com.smartsales.core.test.fakes.FakeLightningRouter
import com.smartsales.core.test.fakes.FakeMascotService
import com.smartsales.core.test.fakes.FakeUnifiedPipeline
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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
    fun `when input is NOISE, routes to Mascot and emits nothing`() = runBlocking {
        // Arrange
        val input = "嗯嗯"
        
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.NOISE, true, ""))

        // Act
        val results = orchestrator.processInput(input).toList()

        // Assert
        assertTrue("NOISE should emit nothing to the pipeline", results.isEmpty())
        assertEquals(1, fakeMascotService.interactions.size)
        assertTrue(fakeMascotService.interactions[0] is MascotInteraction.Text)
        assertEquals(input, (fakeMascotService.interactions[0] as MascotInteraction.Text).content)
        assertTrue(fakeUnifiedPipeline.processedInputs.isEmpty())
    }

    @Test
    fun `when input is GREETING, routes to Mascot and emits nothing`() = runBlocking {
        // Arrange
        val input = "你好啊"
        
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.GREETING, true, ""))

        // Act
        val results = orchestrator.processInput(input).toList()

        // Assert
        assertTrue("GREETING should emit nothing to the pipeline", results.isEmpty())
        assertEquals(1, fakeMascotService.interactions.size)
        assertTrue(fakeMascotService.interactions[0] is MascotInteraction.Text)
        assertEquals(input, (fakeMascotService.interactions[0] as MascotInteraction.Text).content)
        assertTrue(fakeUnifiedPipeline.processedInputs.isEmpty())
    }

    @Test
    fun `when input is VAGUE, emits ConversationalReply for clarification`() = runBlocking {
        // Arrange
        val input = "那个功能"
        val clarification = "你指的是具体哪个功能？"
        
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.VAGUE, false, clarification))

        // Act
        val results = orchestrator.processInput(input).toList()

        // Assert
        assertEquals(1, results.size)
        assertTrue(results[0] is PipelineResult.ConversationalReply)
        assertEquals(clarification, (results[0] as PipelineResult.ConversationalReply).text)
        assertTrue(fakeMascotService.interactions.isEmpty())
        assertTrue(fakeUnifiedPipeline.processedInputs.isEmpty())
    }

    @Test
    fun `when input is DEEP_ANALYSIS, routes to UnifiedPipeline`() = runBlocking {
        // Arrange
        val input = "帮我分析一下华为的最新进展"
        
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        
        val expectedPipelineResult = PipelineResult.ConversationalReply("这是分析结果")
        fakeUnifiedPipeline.nextResultFlow = flowOf(expectedPipelineResult)

        // Act
        val results = orchestrator.processInput(input).toList()

        // Assert
        assertEquals(1, results.size)
        assertEquals(expectedPipelineResult, results[0])
        assertTrue(fakeMascotService.interactions.isEmpty())
        assertEquals(1, fakeUnifiedPipeline.processedInputs.size)
        assertEquals(input, fakeUnifiedPipeline.processedInputs[0].userText)
    }
}
