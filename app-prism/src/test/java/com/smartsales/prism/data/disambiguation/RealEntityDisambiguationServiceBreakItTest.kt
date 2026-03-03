package com.smartsales.prism.data.disambiguation

import com.smartsales.prism.domain.disambiguation.DisambiguationResult
import com.smartsales.prism.domain.memory.EntityWriter
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.parser.InputParserService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RealEntityDisambiguationServiceBreakItTest {

    private lateinit var service: RealEntityDisambiguationService
    private lateinit var entityWriter: EntityWriter
    private lateinit var inputParserService: InputParserService

    @Before
    fun setup() {
        entityWriter = mock()
        inputParserService = mock()
        service = RealEntityDisambiguationService(inputParserService, entityWriter)
    }

    @Test
    fun `break_it_empty_string_when_idle_does_not_crash`() = runTest {
        // Should simply return PassThrough because there's no pending intent
        val result = service.process("")
        assertTrue(result is DisambiguationResult.PassThrough)
    }

    @Test
    fun `break_it_null_chars_when_idle_does_not_crash`() = runTest {
        val maxString = "A".repeat(100000)
        val result = service.process(maxString)
        assertTrue(result is DisambiguationResult.PassThrough)
    }

    @Test
    fun `break_it_empty_string_when_waiting_for_clarification`() = runTest {
        service.startDisambiguation("查一下", Mode.COACH, "A", emptyList())
        whenever(inputParserService.parseIntent("")).doReturn(com.smartsales.prism.domain.parser.ParseResult.Success(emptyList(), null, "{}"))
        
        // This should fail to match a declaration or needsClarification, meaning it cancels the disambiguation
        val result = service.process("")
        assertTrue("Expected PassThrough when input doesn't resolve ambiguity", result is DisambiguationResult.PassThrough)
    }

    @Test
    fun `break_it_max_length_string_when_waiting_for_clarification`() = runTest {
        service.startDisambiguation("查一下", Mode.COACH, "A", emptyList())
        val maxString = "A".repeat(10000)
        whenever(inputParserService.parseIntent(maxString)).doReturn(com.smartsales.prism.domain.parser.ParseResult.Success(emptyList(), null, "{}"))
        
        val result = service.process(maxString)
        assertTrue("Expected PassThrough", result is DisambiguationResult.PassThrough)
    }
}
