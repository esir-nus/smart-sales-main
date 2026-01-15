package com.smartsales.data.aicore.tingwu.publisher

import com.smartsales.data.aicore.TingwuChapter
import com.smartsales.data.aicore.TingwuSmartSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for Publisher interface behavior.
 * 
 * Uses FakeTranscriptPublisher to verify Fake contract and usage patterns.
 */
class PublisherTest {

    private lateinit var publisher: FakeTranscriptPublisher

    @Before
    fun setup() {
        publisher = FakeTranscriptPublisher()
    }

    @Test
    fun `extractTranscriptionUrl returns stub value when set`() {
        publisher.stubTranscriptionUrl = "https://test.com/transcript.json"

        val url = publisher.extractTranscriptionUrl(mapOf("Transcription" to "https://other.com"))

        assertEquals("https://test.com/transcript.json", url)
        assertTrue(publisher.extractCalls.contains("transcription"))
    }

    @Test
    fun `extractTranscriptionUrl falls back to resultLinks`() {
        val links = mapOf("Transcription" to "https://fallback.com/transcript.json")

        val url = publisher.extractTranscriptionUrl(links)

        assertEquals("https://fallback.com/transcript.json", url)
    }

    @Test
    fun `fetchChaptersSafe returns stub chapters`() {
        val chapters = listOf(
            TingwuChapter("Chapter 1", 0, null, "Summary 1"),
            TingwuChapter("Chapter 2", 60000, null, "Summary 2")
        )
        publisher.stubChapters = chapters

        val result = publisher.fetchChaptersSafe("https://test.com/chapters.json", "job123")

        assertEquals(chapters, result)
        assertTrue(publisher.downloadCalls.contains("chapters:job123"))
    }

    @Test
    fun `fetchSmartSummarySafe returns stub summary`() {
        val summary = TingwuSmartSummary(
            summary = "Meeting about Q1 goals",
            keyPoints = listOf("Budget review", "Timeline"),
            actionItems = listOf("Send report")
        )
        publisher.stubSmartSummary = summary

        val result = publisher.fetchSmartSummarySafe(null, "job456")

        assertEquals(summary, result)
        assertTrue(publisher.downloadCalls.contains("smartSummary:job456"))
    }

    @Test
    fun `reset clears all state`() {
        publisher.stubTranscriptionUrl = "http://test"
        publisher.stubChapters = listOf(TingwuChapter("Test", 0, null, null))
        publisher.extractTranscriptionUrl(null)
        publisher.fetchChaptersSafe("", "job1")

        publisher.reset()

        assertNull(publisher.stubTranscriptionUrl)
        assertNull(publisher.stubChapters)
        assertTrue(publisher.extractCalls.isEmpty())
        assertTrue(publisher.downloadCalls.isEmpty())
    }
}
