package com.smartsales.data.aicore.tingwu.result

import com.google.gson.Gson
import com.smartsales.core.metahub.SpeakerMeta
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.data.aicore.TingwuJobArtifacts
import com.smartsales.data.aicore.TranscriptMetadataRequest
import com.smartsales.data.aicore.TranscriptOrchestrator
import com.smartsales.data.aicore.tingwu.api.TingwuResultData
import com.smartsales.data.aicore.tingwu.artifact.FakeTingwuArtifactFetcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RealResultProcessor.
 *
 * Uses Fakes for dependencies following project test conventions.
 */
class RealResultProcessorTest {

    private lateinit var processor: RealResultProcessor
    private lateinit var fakeArtifactFetcher: FakeTingwuArtifactFetcher
    private lateinit var fakeTranscriptOrchestrator: FakeTranscriptOrchestrator
    private val gson = Gson()

    @Before
    fun setup() {
        fakeArtifactFetcher = FakeTingwuArtifactFetcher()
        fakeTranscriptOrchestrator = FakeTranscriptOrchestrator()
        processor = RealResultProcessor(
            artifactFetcher = fakeArtifactFetcher,
            transcriptOrchestrator = fakeTranscriptOrchestrator,
            gson = gson
        )
    }

    // =========================================================================
    // mergeSpeakerLabels tests
    // =========================================================================

    @Test
    fun `mergeSpeakerLabels returns base when incoming is empty`() {
        val base = mapOf("s1" to "Alice", "s2" to "Bob")
        val incoming = emptyMap<String, SpeakerMeta>()

        val result = processor.mergeSpeakerLabels(base, incoming)

        assertEquals(base, result)
    }

    @Test
    fun `mergeSpeakerLabels merges high confidence labels`() {
        val base = mapOf("s1" to "Speaker 1")
        val incoming = mapOf(
            "s1" to SpeakerMeta(displayName = "Alice", role = null, confidence = 0.9f),
            "s2" to SpeakerMeta(displayName = "Bob", role = null, confidence = 0.8f)
        )

        val result = processor.mergeSpeakerLabels(base, incoming, minConfidence = 0.6f)

        assertEquals("Alice", result["s1"])
        assertEquals("Bob", result["s2"])
    }

    @Test
    fun `mergeSpeakerLabels filters low confidence labels but adds if key missing`() {
        val base = mapOf("s1" to "Alice")
        val incoming = mapOf(
            "s1" to SpeakerMeta(displayName = "Bob", role = null, confidence = 0.3f),
            "s2" to SpeakerMeta(displayName = "Carol", role = null, confidence = 0.4f)
        )

        val result = processor.mergeSpeakerLabels(base, incoming, minConfidence = 0.6f)

        // s1 keeps Alice (low confidence incoming doesn't override)
        assertEquals("Alice", result["s1"])
        // s2 gets Carol (key didn't exist, so low confidence is added)
        assertEquals("Carol", result["s2"])
    }

    // =========================================================================
    // buildArtifactsFromResult tests
    // =========================================================================

    @Test
    fun `buildArtifactsFromResult returns artifacts with provided data`() {
        val resultData = TingwuResultData(
            taskId = "task123",
            transcription = null,
            resultLinks = null,
            outputMp3Path = "https://example.com/audio.mp3",
            outputMp4Path = null,
            outputThumbnailPath = null,
            outputSpectrumPath = null
        )
        val segments = listOf(
            DiarizedSegment("s1", 1, 0, 1000, "Hello")
        )

        val artifacts = processor.buildArtifactsFromResult(
            result = resultData,
            diarizedSegments = segments,
            speakerLabels = mapOf("s1" to "Alice")
        )

        assertEquals("https://example.com/audio.mp3", artifacts?.outputMp3Path)
        assertEquals(1, artifacts?.diarizedSegments?.size)
        assertEquals("Alice", artifacts?.speakerLabels?.get("s1"))
    }

    @Test
    fun `buildArtifactsFromResult uses fallback when result fields are null`() {
        val resultData = TingwuResultData(
            taskId = "task123",
            transcription = null,
            resultLinks = null,
            outputMp3Path = null,
            outputMp4Path = null,
            outputThumbnailPath = null,
            outputSpectrumPath = null
        )
        val fallback = TingwuJobArtifacts(
            outputMp3Path = "https://fallback.com/audio.mp3",
            speakerLabels = mapOf("s1" to "FallbackName")
        )

        val artifacts = processor.buildArtifactsFromResult(
            result = resultData,
            fallbackArtifacts = fallback
        )

        assertEquals("https://fallback.com/audio.mp3", artifacts?.outputMp3Path)
        assertEquals("FallbackName", artifacts?.speakerLabels?.get("s1"))
    }

    @Test
    fun `buildArtifactsFromResult handles null diarizedSegments gracefully`() {
        val resultData = TingwuResultData(
            taskId = "task123",
            transcription = null,
            resultLinks = null,
            outputMp3Path = "https://example.com/audio.mp3",
            outputMp4Path = null,
            outputThumbnailPath = null,
            outputSpectrumPath = null
        )

        val artifacts = processor.buildArtifactsFromResult(
            result = resultData,
            diarizedSegments = null
        )

        assertNull(artifacts?.diarizedSegments)
    }

    // =========================================================================
    // Fake for TranscriptOrchestrator
    // =========================================================================

    private class FakeTranscriptOrchestrator : TranscriptOrchestrator {
        override suspend fun inferTranscriptMetadata(
            request: TranscriptMetadataRequest
        ): TranscriptMetadata? = null
    }
}
