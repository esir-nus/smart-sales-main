package com.smartsales.core.test.fakes

import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.LlmProfile
import com.smartsales.core.llm.TokenUsage

/**
 * A fake LLM Executor that allows injecting deterministic JSON payloads for E2E tests.
 * 
 * Used to eliminate the "Testing Illusion" by forcing the pipeline to parse 
 * realistically structured JSON strings without burning real tokens.
 */
class FakeExecutor : Executor {
    
    // Default fallback if no specific response is set
    var defaultResponse: ExecutorResult = ExecutorResult.Success(
        content = """{"query_quality": "vague", "info_sufficient": false, "response": "Default fake response"}""",
        tokenUsage = TokenUsage(10, 10)
    )

    // Store responses mapped by expected prompt keywords or profiles
    private val responseQueue = mutableListOf<ExecutorResult>()
    
    val executedPrompts = mutableListOf<String>()

    /**
     * Enqueue a specific result to be returned by the next `execute` call.
     */
    fun enqueueResponse(result: ExecutorResult) {
        responseQueue.add(result)
    }

    override suspend fun execute(profile: LlmProfile, prompt: String): ExecutorResult {
        executedPrompts.add(prompt)
        return if (responseQueue.isNotEmpty()) {
            responseQueue.removeAt(0)
        } else {
            defaultResponse
        }
    }
}
