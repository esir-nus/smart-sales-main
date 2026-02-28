package com.smartsales.prism.data.parser

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatResponse
import com.smartsales.data.aicore.AiChatService
import com.smartsales.prism.domain.parser.AliasIndex
import com.smartsales.prism.domain.parser.ParseResult
import com.smartsales.prism.domain.pipeline.EntityRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
class RealInputParserServiceTest {

    private lateinit var aiChatService: AiChatService
    private lateinit var aliasIndex: AliasIndex
    private lateinit var parserService: RealInputParserService

    @Before
    fun setup() {
        aiChatService = mock(AiChatService::class.java)
        aliasIndex = mock(AliasIndex::class.java)
        parserService = RealInputParserService(aiChatService, aliasIndex)
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

    @Test
    fun `test success parse with exact match`() = runTest {
        val llmJson = """
            ```json
            {
              "temporal_intent": "明天",
              "mentioned_names": ["孙工"]
            }
            ```
        """.trimIndent()
        mockLlmResponse(llmJson)

        val ref = EntityRef("1", "孙工", "PERSON")
        `when`(aliasIndex.resolveAlias("孙工")).thenReturn(listOf(ref))

        val result = parserService.parseIntent("明天提醒我联系孙工")
        
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertEquals(1, success.resolvedEntityIds.size)
        assertEquals("1", success.resolvedEntityIds[0])
        assertEquals("明天", success.temporalIntent)
    }

    @Test
    fun `test needs clarification on ambiguous match`() = runTest {
        val llmJson = """
            {
              "temporal_intent": null,
              "mentioned_names": ["苹果"]
            }
        """.trimIndent()
        mockLlmResponse(llmJson)

        val ref1 = EntityRef("1", "苹果公司", "ACCOUNT")
        val ref2 = EntityRef("2", "苹果电脑贸易公司", "ACCOUNT")
        `when`(aliasIndex.resolveAlias("苹果")).thenReturn(listOf(ref1, ref2))

        val result = parserService.parseIntent("查一下苹果的订单")
        
        assertTrue(result is ParseResult.NeedsClarification)
        val clarification = result as ParseResult.NeedsClarification
        assertEquals("苹果", clarification.ambiguousName)
        assertEquals(2, clarification.suggestedMatches.size)
        assertTrue(clarification.clarificationPrompt.contains("苹果公司"))
    }

    @Test
    fun `test needs clarification on missing entity`() = runTest {
        val llmJson = """
            {
              "temporal_intent": null,
              "mentioned_names": ["马总"]
            }
        """.trimIndent()
        mockLlmResponse(llmJson)

        `when`(aliasIndex.resolveAlias("马总")).thenReturn(emptyList())

        val result = parserService.parseIntent("给马总打电话")
        
        assertTrue(result is ParseResult.NeedsClarification)
        val clarification = result as ParseResult.NeedsClarification
        assertEquals(0, clarification.suggestedMatches.size)
        assertTrue(clarification.clarificationPrompt.contains("未找到"))
    }

    @Test
    fun `test resilient parsing on empty names`() = runTest {
        val llmJson = """
            {
              "temporal_intent": "下周二",
              "mentioned_names": []
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
        val invalidJson = "I am an AI and I don't follow rules."
        mockLlmResponse(invalidJson)

        val result = parserService.parseIntent("下周二开会")
        
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertTrue(success.resolvedEntityIds.isEmpty())
        assertEquals(null, success.temporalIntent)
    }
}
