package com.smartsales.prism.data.parser

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatResponse
import com.smartsales.data.aicore.AiChatService
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.parser.ParseResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import com.smartsales.prism.data.fakes.FakePipelineTelemetry

@OptIn(ExperimentalCoroutinesApi::class)
class RealInputParserServiceTest {

    private lateinit var aiChatService: AiChatService
    private lateinit var entityRepository: EntityRepository
    private lateinit var parserService: RealInputParserService

    @Before
    fun setup() {
        aiChatService = mock(AiChatService::class.java)
        entityRepository = mock(EntityRepository::class.java)
        parserService = RealInputParserService(aiChatService, entityRepository, telemetry = FakePipelineTelemetry())
    }

    private fun mockLlmResponse(jsonContent: String) {
        runTest {
            val response = AiChatResponse(
                displayText = jsonContent,
                structuredMarkdown = null
            )
            `when`(aiChatService.sendMessage(any<AiChatRequest>())).thenReturn(Result.Success(response))
        }
    }

    private fun setupMockEntities() {
        runTest {
            val personRef = EntityEntry(
                entityId = "1", 
                entityType = EntityType.PERSON, 
                displayName = "孙工", 
                lastUpdatedAt = 0, 
                createdAt = 0
            )
            val accountRef = EntityEntry(
                entityId = "2", 
                entityType = EntityType.ACCOUNT, 
                displayName = "Apple Inc", 
                lastUpdatedAt = 0, 
                createdAt = 0
            )
            `when`(entityRepository.getByType(EntityType.PERSON)).thenReturn(listOf(personRef))
            `when`(entityRepository.getByType(EntityType.ACCOUNT)).thenReturn(listOf(accountRef))
            `when`(entityRepository.getByType(EntityType.CONTACT)).thenReturn(emptyList())
        }
    }

    @Test
    fun `test success parse with exact match`() = runTest {
        setupMockEntities()
        val llmJson = """
            ```json
            {
              "temporal_intent": "明天",
              "resolved_indices": [1]
            }
            ```
        """.trimIndent()
        mockLlmResponse(llmJson)

        val result = parserService.parseIntent("明天提醒我联系孙工")
        
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertEquals(1, success.resolvedEntityIds.size)
        assertEquals("1", success.resolvedEntityIds[0])
        assertEquals("明天", success.temporalIntent)
    }

    @Test
    fun `test needs clarification on unknown names`() = runTest {
        setupMockEntities()
        val llmJson = """
            {
              "temporal_intent": null,
              "unknown_names": ["新客户"]
            }
        """.trimIndent()
        mockLlmResponse(llmJson)

        val result = parserService.parseIntent("查一下新客户的订单")
        
        assertTrue(result is ParseResult.NeedsClarification)
        val clarification = result as ParseResult.NeedsClarification
        assertEquals("新客户", clarification.ambiguousName)
        assertEquals(0, clarification.suggestedMatches.size)
        assertTrue(clarification.clarificationPrompt.contains("新客户"))
    }

    @Test
    fun `test resilient parsing on empty indices`() = runTest {
        setupMockEntities()
        val llmJson = """
            {
              "temporal_intent": "下周二",
              "resolved_indices": []
            }
        """.trimIndent()
        mockLlmResponse(llmJson)

        val result = parserService.parseIntent("下周二开会")
        
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertTrue(success.resolvedEntityIds.isEmpty())
        assertEquals("下周二", success.temporalIntent)
    }

    @Test
    fun `test fallback on invalid JSON`() = runTest {
        setupMockEntities()
        val invalidJson = "I am an AI and I don't follow rules."
        mockLlmResponse(invalidJson)

        val result = parserService.parseIntent("下周二开会")
        
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertTrue(success.resolvedEntityIds.isEmpty())
        assertEquals(null, success.temporalIntent)
    }
}
