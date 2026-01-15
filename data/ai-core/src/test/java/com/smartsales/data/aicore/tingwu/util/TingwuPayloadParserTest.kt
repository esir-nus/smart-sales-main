package com.smartsales.data.aicore.tingwu.util

import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.data.aicore.TingwuChapter
import com.smartsales.data.aicore.TingwuSmartSummary
import com.smartsales.data.aicore.tingwu.api.TingwuSpeaker
import com.smartsales.data.aicore.tingwu.api.TingwuTranscription
import com.smartsales.data.aicore.tingwu.api.TingwuTranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TingwuPayloadParserTest {

    @Test
    fun parseAutoChapters_handlesStandardFormat() {
        val json = """
            {
              "AutoChapters": [
                {"Headline": "开场", "Start": 0, "End": 12.5},
                {"Headline": "报价讨论", "Start": 42, "End": 85}
              ]
            }
        """.trimIndent()
        
        val chapters = TingwuPayloadParser.parseAutoChapters(json)
        
        assertEquals(2, chapters.size)
        assertEquals("开场", chapters[0].title)
        assertEquals(0L, chapters[0].startMs)
        assertEquals(12500L, chapters[0].endMs)
    }

    @Test
    fun parseAutoChapters_handlesMultipleFormats() {
        val jsonWithChapters = """{"Chapters": [{"Title": "开场", "StartTime": 0}]}"""
        val jsonWithItems = """{"Items": [{"Name": "开场", "StartMs": 0}]}"""
        val jsonAsArray = """[{"Headline": "开场", "Start": 0}]"""
        
        assertTrue(TingwuPayloadParser.parseAutoChapters(jsonWithChapters).isNotEmpty())
        assertTrue(TingwuPayloadParser.parseAutoChapters(jsonWithItems).isNotEmpty())
        assertTrue(TingwuPayloadParser.parseAutoChapters(jsonAsArray).isNotEmpty())
    }

    @Test
    fun parseAutoChapters_handlesMalformedJson() {
        assertEquals(emptyList<TingwuChapter>(), TingwuPayloadParser.parseAutoChapters(""))
        assertEquals(emptyList<TingwuChapter>(), TingwuPayloadParser.parseAutoChapters("invalid json"))
        assertEquals(emptyList<TingwuChapter>(), TingwuPayloadParser.parseAutoChapters("{}"))
        assertEquals(emptyList<TingwuChapter>(), TingwuPayloadParser.parseAutoChapters("[]"))
    }

    @Test
    fun parseAutoChapters_skipsMissingRequiredFields() {
        val json = """
            {
              "AutoChapters": [
                {"Start": 0},
                {"Headline": "有效章节", "Start": 1000}
              ]
            }
        """.trimIndent()
        
        val chapters = TingwuPayloadParser.parseAutoChapters(json)
        
        assertEquals(1, chapters.size)
        assertEquals("有效章节", chapters[0].title)
    }

    @Test
    fun parseSmartSummary_handlesAllFields() {
        val json = """
            {
              "Summary": "会议概览",
              "KeyPoints": ["要点1", "要点2"],
              "ActionItems": ["行动A"]
            }
        """.trimIndent()
        
        val summary = TingwuPayloadParser.parseSmartSummary(json)
        
        assertNotNull(summary)
        assertEquals("会议概览", summary?.summary)
        assertEquals(listOf("要点1", "要点2"), summary?.keyPoints)
        assertEquals(listOf("行动A"), summary?.actionItems)
    }

    @Test
    fun parseSmartSummary_handlesPartialFields() {
        val onlySummary = """{"Summary": "概览"}"""
        val onlyKeyPoints = """{"KeyPoints": ["要点1"]}"""
        
        assertNotNull(TingwuPayloadParser.parseSmartSummary(onlySummary))
        assertNotNull(TingwuPayloadParser.parseSmartSummary(onlyKeyPoints))
    }

    @Test
    fun parseSmartSummary_handlesAlternativeFieldNames() {
        val abstractField = """{"Abstract": "摘要"}"""
        val highlightsField = """{"Highlights": ["亮点"]}"""
        
        val summary1 = TingwuPayloadParser.parseSmartSummary(abstractField)
        val summary2 = TingwuPayloadParser.parseSmartSummary(highlightsField)
        
        assertEquals("摘要", summary1?.summary)
        assertEquals(listOf("亮点"), summary2?.keyPoints)
    }

    @Test
    fun parseSmartSummary_returnsNullForEmptyContent() {
        assertNull(TingwuPayloadParser.parseSmartSummary("{}"))
        assertNull(TingwuPayloadParser.parseSmartSummary("invalid"))
        assertNull(TingwuPayloadParser.parseSmartSummary("""{"Summary": "", "KeyPoints": [], "ActionItems": []}"""))
    }

    @Test
    fun buildDiarizedSegments_appliesTimeOffset() {
        val transcription = TingwuTranscription(
            text = "原始文本",
            segments = listOf(
                TingwuTranscriptSegment(id = 0, text = "你好", speaker = "spk1", start = 0.0, end = 1.0),
                TingwuTranscriptSegment(id = 1, text = "世界", speaker = "spk1", start = 1.0, end = 2.0)
            ),
            speakers = listOf(TingwuSpeaker(id = "spk1", name = "说话人1")),
            language = "cn",
            duration = 100.0
        )
        
        val baseOffsetMs = 5000L
        val segments = TingwuPayloadParser.buildDiarizedSegments(
            transcription = transcription,
            baseOffsetMs = baseOffsetMs,
            shouldMerge = { _, _ -> false }
        )
        
        assertEquals(2, segments.size)
        assertEquals(5000L, segments[0].startMs) // 0 + 5000
        assertEquals(6000L, segments[0].endMs)   // 1000 + 5000
        assertEquals(6000L, segments[1].startMs) // 1000 + 5000
        assertEquals(7000L, segments[1].endMs)   // 2000 + 5000
    }

    @Test
    fun buildDiarizedSegments_mergesAdjacentSameSpeaker() {
        val transcription = TingwuTranscription(
            text = null,
            segments = listOf(
                TingwuTranscriptSegment(id = 0, text = "你好", speaker = "spk1", start = 0.0, end = 1.0),
                TingwuTranscriptSegment(id = 1, text = "世界", speaker = "spk1", start = 1.0, end = 2.0)
            ),
            speakers = listOf(TingwuSpeaker(id = "spk1")),
            language = "cn",
            duration = 100.0
        )
        
        val segments = TingwuPayloadParser.buildDiarizedSegments(
            transcription = transcription,
            baseOffsetMs = 0,
            shouldMerge = { last, next -> last.speakerIndex == next.speakerIndex }
        )
        
        assertEquals(1, segments.size)
        assertEquals("你好 世界", segments[0].text)
        assertEquals(0L, segments[0].startMs)
        assertEquals(2000L, segments[0].endMs)
    }

    @Test
    fun buildDiarizedSegments_handlesEmptyOrNullTranscription() {
        assertEquals(emptyList<DiarizedSegment>(), 
            TingwuPayloadParser.buildDiarizedSegments(null, 0, { _, _ -> false }))
        
        val emptyTranscription = TingwuTranscription(
            text = "", 
            segments = emptyList(), 
            speakers = null,
            language = null,
            duration = null
        )
        assertEquals(emptyList<DiarizedSegment>(), 
            TingwuPayloadParser.buildDiarizedSegments(emptyTranscription, 0, { _, _ -> false }))
    }

    @Test
    fun formatTimeMs_handlesZeroAndNegative() {
        assertEquals("00:00", TingwuPayloadParser.formatTimeMs(0))
        assertEquals("00:00", TingwuPayloadParser.formatTimeMs(-100))
    }

    @Test
    fun formatTimeMs_formatsSecondsOnly() {
        assertEquals("00:05", TingwuPayloadParser.formatTimeMs(5000))
        assertEquals("01:30", TingwuPayloadParser.formatTimeMs(90000))
    }

    @Test
    fun formatTimeMs_formatsHoursMinutesSeconds() {
        assertEquals("01:00:00", TingwuPayloadParser.formatTimeMs(3600000))
        assertEquals("01:23:45", TingwuPayloadParser.formatTimeMs(5025000))
    }
}
