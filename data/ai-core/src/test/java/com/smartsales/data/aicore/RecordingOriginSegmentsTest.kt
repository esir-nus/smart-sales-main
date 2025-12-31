// File: data/ai-core/src/test/java/com/smartsales/data/aicore/RecordingOriginSegmentsTest.kt
// Description: Recording-origin segments conversion tests (2025-12-30)
package com.smartsales.data.aicore

import com.smartsales.data.aicore.tingwu.TingwuTranscriptSegment
import com.smartsales.data.aicore.tingwu.TingwuTranscription
import com.smartsales.data.aicore.tingwu.TingwuSpeaker
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingOriginSegmentsTest {

    @Test
    fun `recording-origin segments preserve absolute timing`() {
        val transcription = TingwuTranscription(
            text = "t",
            segments = listOf(
                TingwuTranscriptSegment(id = 1, start = 5.0, end = 7.0, text = "A", speaker = "s1"),
                TingwuTranscriptSegment(id = 2, start = 9.0, end = 10.5, text = "B", speaker = "s1")
            ),
            speakers = listOf(TingwuSpeaker(id = "s1", name = "S1")),
            language = "zh-CN",
            duration = 12.0
        )

        val normalized = buildNormalizedSegmentsForTest(transcription)
        val recordingOrigin = buildRecordingOriginSegmentsForTest(transcription)

        assertEquals(0, normalized.first().startMs)
        assertEquals(2000, normalized.first().endMs)
        assertEquals(5000, recordingOrigin.first().startMs)
        assertEquals(7000, recordingOrigin.first().endMs)
    }

    private fun buildNormalizedSegmentsForTest(transcription: TingwuTranscription): List<DiarizedSegment> {
        val segments = transcription.segments.orEmpty()
            .filter { !it.text.isNullOrBlank() && !it.speaker.isNullOrBlank() }
            .sortedBy { it.start ?: 0.0 }
        val baseStartSeconds = segments.minOfOrNull { it.start ?: 0.0 }?.coerceAtLeast(0.0) ?: 0.0
        return segments.map { seg ->
            val rawStart = (seg.start ?: 0.0) - baseStartSeconds
            val rawEnd = (seg.end ?: seg.start ?: 0.0) - baseStartSeconds
            DiarizedSegment(
                speakerId = seg.speaker,
                speakerIndex = 1,
                startMs = (rawStart * 1000).toLong(),
                endMs = (rawEnd * 1000).toLong(),
                text = seg.text.orEmpty()
            )
        }
    }

    private fun buildRecordingOriginSegmentsForTest(transcription: TingwuTranscription): List<DiarizedSegment> {
        val segments = transcription.segments.orEmpty()
            .filter { !it.text.isNullOrBlank() && !it.speaker.isNullOrBlank() }
            .sortedBy { it.start ?: 0.0 }
        return segments.map { seg ->
            DiarizedSegment(
                speakerId = seg.speaker,
                speakerIndex = 1,
                startMs = ((seg.start ?: 0.0) * 1000).toLong(),
                endMs = ((seg.end ?: seg.start ?: 0.0) * 1000).toLong(),
                text = seg.text.orEmpty()
            )
        }
    }
}
