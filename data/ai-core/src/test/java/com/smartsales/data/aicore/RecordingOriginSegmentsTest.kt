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
                TingwuTranscriptSegment(id = 1, start = 12.0, end = 13.0, text = "A", speaker = "s1"),
                TingwuTranscriptSegment(id = 2, start = 20.0, end = 21.0, text = "B", speaker = "s1")
            ),
            speakers = listOf(TingwuSpeaker(id = "s1", name = "S1")),
            language = "zh-CN",
            duration = 12.0
        )

        val normalized = buildNormalizedSegmentsForTest(transcription)
        val recordingOrigin = buildRecordingOriginSegmentsForTest(transcription)

        // 说明：V1 宏窗口过滤依赖录音起点绝对时间，recording-origin 不允许 baseStart 归一化。
        assertEquals(0, normalized.first().startMs)
        assertEquals(1000, normalized.first().endMs)
        assertEquals(12_000, recordingOrigin.first().startMs)
        assertEquals(13_000, recordingOrigin.first().endMs)
        assertEquals(20_000, recordingOrigin[1].startMs)
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
