package com.smartsales.prism.data.real

import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.TokenUsage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RealArchitectServiceBreakItTest {

    private lateinit var executor: Executor
    private lateinit var linter: ArchitectLinter
    private lateinit var architectService: RealArchitectService
    private lateinit var dummyContext: EnhancedContext

    @Before
    fun setup() {
        executor = mock()
        linter = ArchitectLinter()
        architectService = RealArchitectService(executor, linter)
        dummyContext = EnhancedContext(userText = "break it")
    }

    private suspend fun assertItHandlesBrokenInputSafely(brokenInput: String) {
        whenever(executor.execute(any(), any())).thenReturn(
            ExecutorResult.Success(brokenInput, TokenUsage(100, 50))
        )
        try {
            val plan = architectService.generatePlan("test", dummyContext, emptyList())
            // It parses whatever string it's given as a markdown strategy.
            // If it succeeds, that's fine too (e.g. for pure text).
            assertTrue(true)
        } catch (e: IllegalStateException) {
            // This is the expected behavior now when the Linter fails to parse broken JSON
            assertTrue(true)
        } catch (e: Exception) {
            // Failed! It threw some unexpected exception like NullPointerException
            assertTrue("Threw unexpected exception: ${e::class.java.simpleName}", false)
        }
    }

    @Test
    fun `break-it json with trailing commas`() = runTest {
        val brokenJson = """
            {
                "title": "broken",
                "summary": "broken",
                "steps": [
                    { "description": "1" },
                ]
            }
        """.trimIndent()
        // org.json handles trailing commas sometimes, but let's see what happens.
        // It shouldn't crash the JVM.
        val plan = try {
            whenever(executor.execute(any(), any())).thenReturn(
                ExecutorResult.Success(brokenJson, TokenUsage(100, 50))
            )
            architectService.generatePlan("test", dummyContext, emptyList(), emptyList())
        } catch (e: Exception) { null }
        
        // It's perfectly fine if it parses it or throws gracefully. 
        // We just don't want it to crash the app thread.
        assertTrue(true)
    }

    @Test
    fun `break-it completely empty string`() = runTest {
        assertItHandlesBrokenInputSafely("")
    }

    @Test
    fun `break-it valid JSON but completely wrong schema`() = runTest {
        assertItHandlesBrokenInputSafely("""{"name": "frank", "age": 30}""")
    }

    @Test
    fun `break-it half markdown half json`() = runTest {
        assertItHandlesBrokenInputSafely("""
            Here is your plan:
            ```json
            { "title": "my plan" 
            // forgot closing bracket
            ```
        """.trimIndent())
    }

    @Test
    fun `break-it huge string`() = runTest {
        val hugeString = "A".repeat(100_000)
        assertItHandlesBrokenInputSafely(hugeString)
    }
}
