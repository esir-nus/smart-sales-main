package com.smartsales.core.test.fakes

import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.ModelRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeExecutorTest {

    @Test
    fun `returns queued responses in order and records prompts`() = runTest {
        val executor = FakeExecutor()
        val first = ExecutorResult.Success("""{"step":"first"}""")
        val second = ExecutorResult.Success("""{"step":"second"}""")
        val fallback = ExecutorResult.Success("""{"step":"fallback"}""")

        executor.defaultResponse = fallback
        executor.enqueueResponse(first)
        executor.enqueueResponse(second)

        assertEquals(first, executor.execute(ModelRegistry.EXTRACTOR, "prompt-1"))
        assertEquals(second, executor.execute(ModelRegistry.EXTRACTOR, "prompt-2"))
        assertEquals(fallback, executor.execute(ModelRegistry.EXTRACTOR, "prompt-3"))
        assertEquals(listOf("prompt-1", "prompt-2", "prompt-3"), executor.executedPrompts)
    }
}
