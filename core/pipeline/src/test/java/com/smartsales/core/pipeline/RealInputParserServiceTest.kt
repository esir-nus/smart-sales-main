package com.smartsales.core.pipeline

import com.smartsales.core.llm.ExecutorResult
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.telemetry.PipelinePhase
import com.smartsales.prism.domain.telemetry.PipelineTelemetry
import com.smartsales.core.test.fakes.FakeEntityRepository
import com.smartsales.core.test.fakes.FakeExecutor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakePipelineTelemetry : PipelineTelemetry {
    override fun recordEvent(phase: PipelinePhase, message: String) {}
    override fun recordError(phase: PipelinePhase, message: String, error: Throwable?) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class RealInputParserServiceTest {

    private lateinit var fakeExecutor: FakeExecutor
    private lateinit var fakeEntityRepository: FakeEntityRepository
    private lateinit var parserService: RealInputParserService

    @Before
    fun setup() {
        fakeExecutor = FakeExecutor()
        fakeEntityRepository = FakeEntityRepository()
        parserService = RealInputParserService(
            executor = fakeExecutor,
            entityRepository = fakeEntityRepository,
            telemetry = FakePipelineTelemetry()
        )
    }

    private fun mockLlmResponse(jsonContent: String) {
        val response = ExecutorResult.Success(content = jsonContent)
        fakeExecutor.enqueueResponse(response)
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
            fakeEntityRepository.save(personRef)
            fakeEntityRepository.save(accountRef)
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
        
        assertTrue(result is ParseResult.NeedsClarification)
        val clarification = result as ParseResult.NeedsClarification
        assertEquals("未知意图", clarification.ambiguousName)
        assertEquals(0, clarification.suggestedMatches.size)
        assertTrue(clarification.clarificationPrompt.contains("系统未能理解您的语义意图"))
    }

    @Test
    fun `test entity declaration exact sealed class mapping`() = runTest {
        setupMockEntities()
        val llmJson = """
            {
              "temporal_intent": null,
              "declaration": {
                "name": "司马懿",
                "company": "曹魏集团",
                "job_title": "大都督",
                "aliases": ["仲达", "司马公"],
                "notes": "非常聪明，防守反击大师"
              }
            }
        """.trimIndent()
        mockLlmResponse(llmJson)

        val result = parserService.parseIntent("仲达，曹魏集团的大都督司马公，非常聪明，防守反击大师")
        
        assertTrue(result is ParseResult.EntityDeclaration)
        val declaration = result as ParseResult.EntityDeclaration
        
        // Mathematically prove every single json field is accurately mapped to the sealed class
        // This explicitly prevents the Entity-Domain Mapping Gap lesson learned
        assertEquals("司马懿", declaration.name)
        assertEquals("曹魏集团", declaration.company)
        assertEquals("大都督", declaration.jobTitle)
        assertEquals(2, declaration.aliases.size)
        assertTrue(declaration.aliases.contains("仲达"))
        assertTrue(declaration.aliases.contains("司马公"))
        assertEquals("非常聪明，防守反击大师", declaration.notes)
    }
}
