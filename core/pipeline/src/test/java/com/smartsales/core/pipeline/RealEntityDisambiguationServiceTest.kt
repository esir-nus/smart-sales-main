package com.smartsales.core.pipeline

import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.memory.EntityRef
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.core.test.fakes.FakeInputParserService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealEntityDisambiguationServiceTest {

    private lateinit var fakeParserService: FakeInputParserService
    private lateinit var disambiguationService: RealEntityDisambiguationService

    @Before
    fun setup() {
        fakeParserService = FakeInputParserService()
        disambiguationService = RealEntityDisambiguationService(fakeParserService)
    }

    @Test
    fun `test start disambiguation yields AwaitingClarification`() = runTest {
        val candidates = listOf(
            EntityRef(entityId = "1", displayName = "刘备", entityType = EntityType.PERSON.name)
        )
        val uiState = disambiguationService.startDisambiguation(
            originalInput = "打电话给刘总",
            originalMode = Mode.SCHEDULER,
            ambiguousName = "刘总",
            candidates = candidates
        )
        
        assertTrue(uiState is UiState.AwaitingClarification)
        val clarification = uiState as UiState.AwaitingClarification
        assertTrue(clarification.question.contains("刘总"))
        assertEquals(1, clarification.candidates.size)
        assertEquals("1", clarification.candidates[0].entityId)
    }

    @Test
    fun `test process resolves when declaration received`() = runTest {
        // 1. Enter disambiguation state
        disambiguationService.startDisambiguation(
            originalInput = "打电话给刘总",
            originalMode = Mode.SCHEDULER,
            ambiguousName = "刘总",
            candidates = emptyList()
        )

        // 2. User provides a cure
        val declaration = ParseResult.EntityDeclaration(
            name = "刘备",
            company = "蜀汉公司",
            jobTitle = "CEO",
            aliases = listOf("刘总"),
            notes = null
        )
        fakeParserService.nextResult = declaration

        // 3. Process the cure
        val result = disambiguationService.process("他是蜀汉公司的CEO刘备")
        
        assertTrue(result is DisambiguationResult.Resolved)
        val resolved = result as DisambiguationResult.Resolved
        assertEquals("打电话给刘总", resolved.originalInput)
        assertEquals(Mode.SCHEDULER, resolved.mode)
        assertEquals("刘备", resolved.declaration.name)
        
        // 4. Verify state is cleared by sending another input
        fakeParserService.nextResult = ParseResult.Success(emptyList(), null, "{}")
        val nextResult = disambiguationService.process("明天开会")
        assertTrue(nextResult is DisambiguationResult.PassThrough)
    }

    @Test
    fun `test process intercepts when still ambiguous`() = runTest {
        disambiguationService.startDisambiguation(
            originalInput = "打电话给刘总",
            originalMode = Mode.SCHEDULER,
            ambiguousName = "刘总",
            candidates = emptyList()
        )

        fakeParserService.nextResult = ParseResult.NeedsClarification(
            ambiguousName = "刘总经理",
            suggestedMatches = emptyList(),
            clarificationPrompt = "系统还是没找到刘总经理"
        )

        val result = disambiguationService.process("我不知道全名")
        
        assertTrue(result is DisambiguationResult.Intercepted)
        val intercepted = result as DisambiguationResult.Intercepted
        assertTrue(intercepted.uiState is UiState.AwaitingClarification)
        val uiState = intercepted.uiState as UiState.AwaitingClarification
        assertEquals("系统还是没找到刘总经理", uiState.question)
    }

    @Test
    fun `test process clears state and passes through on success without declaration`() = runTest {
        disambiguationService.startDisambiguation(
            originalInput = "打电话给刘总",
            originalMode = Mode.SCHEDULER,
            ambiguousName = "刘总",
            candidates = emptyList()
        )

        // User completely ignored the prompt and asked something else
        fakeParserService.nextResult = ParseResult.Success(emptyList(), null, "{}")

        val result = disambiguationService.process("帮我查一下另外一个事")
        
        assertTrue(result is DisambiguationResult.PassThrough)
        
        // Verify state is cleared
        val nextResult = disambiguationService.process("继续查")
        assertTrue(nextResult is DisambiguationResult.PassThrough)
    }
}
