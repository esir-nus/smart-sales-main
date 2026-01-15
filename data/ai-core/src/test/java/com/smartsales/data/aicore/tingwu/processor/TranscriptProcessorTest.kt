package com.smartsales.data.aicore.tingwu.processor

import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.data.aicore.TingwuChapter
import com.smartsales.data.aicore.TingwuJobArtifacts
import com.smartsales.data.aicore.tingwu.api.TingwuTranscription
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for TranscriptProcessor interface behavior.
 * 
 * Uses FakeTranscriptProcessor to verify Fake contract and usage patterns.
 * Real implementation tested via integration tests (TingwuRunnerTest).
 */
class TranscriptProcessorTest {

    private lateinit var processor: FakeTranscriptProcessor

    @Before
    fun setup() {
        processor = FakeTranscriptProcessor()
    }

    @Test
    fun `fetchTranscript returns stubResult when provided`() = runTest {
        val expectedResult = TranscriptResult(
            markdown = "Custom markdown",
            artifacts = TingwuJobArtifacts(),
            chapters = listOf(TingwuChapter("Test", 0, null, "Summary")),
            diarizedSegments = listOf(DiarizedSegment("s1", 1, 0, 1000, "Hello"))
        )
        processor.stubResult = expectedResult

        val result = processor.fetchTranscript(
            jobId = "job123",
            resultLinks = null,
            fallbackArtifacts = null,
            runEnhancer = { _, _, _, fallback -> fallback },
            composeFinalMarkdown = { md, _, _ -> md }
        )

        assertEquals(expectedResult, result)
    }

    @Test
    fun `fetchTranscript returns default result when no stub`() = runTest {
        val result = processor.fetchTranscript(
            jobId = "testJob",
            resultLinks = null,
            fallbackArtifacts = null,
            runEnhancer = { _, _, _, fallback -> fallback },
            composeFinalMarkdown = { md, _, _ -> md }
        )

        assertNotNull(result)
        assertTrue(result.markdown.contains("testJob"))
    }

    @Test
    fun `fetchTranscript throws stubError when provided`() = runTest {
        processor.stubError = RuntimeException("Test error")

        val result = runCatching {
            processor.fetchTranscript(
                jobId = "job123",
                resultLinks = null,
                fallbackArtifacts = null,
                runEnhancer = { _, _, _, fallback -> fallback },
                composeFinalMarkdown = { md, _, _ -> md }
            )
        }

        assertTrue(result.isFailure)
        assertEquals("Test error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fetchTranscript tracks call history`() = runTest {
        processor.fetchTranscript("job1", null, null, { _, _, _, f -> f }, { m, _, _ -> m })
        processor.fetchTranscript("job2", null, null, { _, _, _, f -> f }, { m, _, _ -> m })
        processor.fetchTranscript("job3", null, null, { _, _, _, f -> f }, { m, _, _ -> m })

        assertEquals(listOf("job1", "job2", "job3"), processor.calls)
    }

    @Test
    fun `reset clears all state`() = runTest {
        processor.stubResult = TranscriptResult("md", null, null, null)
        processor.stubError = RuntimeException("error")
        
        // Add to calls manually since we can't call fetchTranscript with stubError set
        processor.calls.add("job1")

        processor.reset()

        assertEquals(null, processor.stubResult)
        assertEquals(null, processor.stubError)
        assertTrue(processor.calls.isEmpty())
    }
}
