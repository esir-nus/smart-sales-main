package com.smartsales.prism.data.real.session

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatResponse
import com.smartsales.data.aicore.AiChatService
import com.smartsales.prism.domain.pipeline.ChatTurn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito
import org.mockito.ArgumentMatchers

class LlmSessionTitleGeneratorTest {

    private val aiChatService: AiChatService = Mockito.mock(AiChatService::class.java)
    private val generator = LlmSessionTitleGenerator(aiChatService)

    // Helper for safe any() in Kotlin to avoid IllegalStateException
    private fun <T> any(type: Class<T>): T = ArgumentMatchers.any(type)
    private fun <T> any(): T = ArgumentMatchers.any()

    @Test
    fun `generateTitle parses valid JSON response correctly`() = runTest {
        // Arrange
        val history = listOf(ChatTurn("user", "你好"))
        val jsonResponse = """
            Thinking...
            ```json
            {"clientName": "王总", "summary": "预算审查"}
            ```
        """.trimIndent()
        
        Mockito.`when`(aiChatService.sendMessage(any()))
            .thenReturn(Result.Success(AiChatResponse(jsonResponse, null)))

        // Act
        val result = generator.generateTitle(history)

        // Assert
        assertEquals("王总", result.clientName)
        assertEquals("预算审查", result.summary)
        Mockito.verify(aiChatService).sendMessage(any())
    }

    @Test
    fun `generateTitle handles missing JSON gracefully`() = runTest {
        // Arrange
        val history = listOf(ChatTurn("user", "你好"))
        // Creating the Result object first to avoid overload ambiguity
        val response = Result.Success(AiChatResponse("Not JSON", null))
        
        Mockito.`when`(aiChatService.sendMessage(any()))
            .thenReturn(response)

        // Act
        val result = generator.generateTitle(history)

        // Assert
        assertEquals("客户", result.clientName) // Fallback
        assertEquals("对话摘要", result.summary) // Fallback
    }

    @Test
    fun `generateTitle handles service error gracefully`() = runTest {
        // Arrange
        val history = listOf(ChatTurn("user", "你好"))
        val errorResponse = Result.Error(Exception("Network error"))
        
        Mockito.`when`(aiChatService.sendMessage(any()))
            .thenReturn(errorResponse)

        // Act
        val result = generator.generateTitle(history)

        // Assert
        assertEquals("客户", result.clientName) // Fallback
        assertEquals("对话摘要", result.summary) // Fallback
    }
}
