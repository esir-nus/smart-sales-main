package com.smartsales.core.pipeline

import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.context.ModeMetadata
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.prism.domain.model.Mode
import com.smartsales.core.test.fakes.FakeExecutor
import com.smartsales.core.test.fakes.FakePromptCompiler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * L1 Logic Verification Test - Lightning Router
 * 
 * Verifies that the JSON payload from the Extractor model is correctly mapped
 * to the PipelineIntent (QueryQuality) enums, guaranteeing no parsing crashes.
 * 
 * Anti-Illusion Protocol Compliant (No Mockito).
 */
class RealLightningRouterTest {

    private lateinit var lightningRouter: RealLightningRouter
    private lateinit var fakeExecutor: FakeExecutor
    private lateinit var fakePromptCompiler: FakePromptCompiler

    @Before
    fun setup() {
        fakeExecutor = FakeExecutor()
        fakePromptCompiler = FakePromptCompiler()
        fakePromptCompiler.compileOutput = "Compiled Prompt"
        lightningRouter = RealLightningRouter(fakeExecutor, fakePromptCompiler)
    }

    private fun buildMockContext(): EnhancedContext = EnhancedContext(
        userText = "Test input",
        modeMetadata = ModeMetadata(currentMode = Mode.ANALYST, sessionId = "test", turnIndex = 1),
        sessionHistory = emptyList(),
        currentDate = "2026-03-09",
        currentInstant = 0L,
        executedTools = emptySet()
    )

    @Test
    fun `GREETING maps correctly from LLM JSON to RouterResult`() = runTest {
        // Arrange
        val jsonPayload = """
            ```json
            {
                "query_quality": "greeting",
                "analysis": {
                    "info_sufficient": false
                },
                "response": "Hello there!"
            }
            ```
        """.trimIndent()
        
        fakeExecutor.enqueueResponse(ExecutorResult.Success(jsonPayload))
        
        // Act
        val result = lightningRouter.evaluateIntent(buildMockContext())
        
        // Assert
        assertNotNull("RouterResult should not be null", result)
        assertEquals(QueryQuality.GREETING, result?.queryQuality)
        assertEquals("Hello there!", result?.response)
        assertTrue("Greeting should clear missing entities list to prevent disambiguation loop", result?.missingEntities?.isEmpty() == true)
    }

    @Test
    fun `NOISE maps correctly and gracefully drops missing entities`() = runTest {
        // Arrange
        val jsonPayload = """
            {
                "query_quality": "noise",
                "info_sufficient": false,
                "missing_entities": ["something ignored"],
                "response": "..."
            }
        """.trimIndent()
        
        fakeExecutor.enqueueResponse(ExecutorResult.Success(jsonPayload))
        
        // Act
        val result = lightningRouter.evaluateIntent(buildMockContext())
        
        // Assert
        assertNotNull(result)
        assertEquals(QueryQuality.NOISE, result?.queryQuality)
        assertTrue("Noise should clear missing entities list to prevent disambiguation loop", result?.missingEntities?.isEmpty() == true)
    }

    @Test
    fun `DEEP_ANALYSIS maps as fallback from arbitrary unsupported strings`() = runTest {
        // Arrange
        val jsonPayload = """
            {
                "query_quality": "some_unknown_weird_string",
                "info_sufficient": false,
                "response": "Can you clarify?"
            }
        """.trimIndent()
        
        fakeExecutor.enqueueResponse(ExecutorResult.Success(jsonPayload))
        
        // Act
        val result = lightningRouter.evaluateIntent(buildMockContext())
        
        // Assert
        assertNotNull(result)
        assertEquals(QueryQuality.DEEP_ANALYSIS, result?.queryQuality)
    }
}
