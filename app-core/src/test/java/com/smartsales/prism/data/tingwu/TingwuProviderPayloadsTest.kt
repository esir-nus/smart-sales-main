package com.smartsales.prism.data.tingwu

import com.smartsales.prism.domain.tingwu.DiarizedSegment
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TingwuProviderPayloadsTest {

    @Test
    fun `parseIdentityRecognitionSpeakerLabels supports nested identity object`() {
        val labels = parseIdentityRecognitionSpeakerLabels(
            """
                {
                  "IdentityRecognition": [
                    {
                      "SpeakerId": "spk_1",
                      "Identity": {"Name": "销售顾问"}
                    },
                    {
                      "SpeakerId": "spk_2",
                      "Identity": {"Name": "客户"}
                    }
                  ]
                }
            """.trimIndent()
        )

        assertEquals(
            mapOf("spk_1" to "销售顾问", "spk_2" to "客户"),
            labels
        )
    }

    @Test
    fun `mergeTingwuSpeakerLabels prefers identity recognition labels`() {
        val merged = mergeTingwuSpeakerLabels(
            baseSpeakerLabels = mapOf("spk_1" to "发言人1"),
            diarizedSegments = listOf(
                DiarizedSegment(
                    speakerId = "spk_1",
                    speakerIndex = 0,
                    startMs = 0L,
                    endMs = 1000L,
                    text = "你好"
                ),
                DiarizedSegment(
                    speakerId = "spk_2",
                    speakerIndex = 1,
                    startMs = 1000L,
                    endMs = 2000L,
                    text = "欢迎"
                )
            ),
            identityRecognitionLabels = mapOf("spk_1" to "销售顾问", "spk_2" to "客户")
        )

        assertEquals(
            mapOf("spk_1" to "销售顾问", "spk_2" to "客户"),
            merged
        )
    }

    @Test
    fun `parseTranscriptionSpeakerLabels reads speakers array`() {
        val root = Json.parseToJsonElement(
            """
                {
                  "Speakers": [
                    {"Id": "spk_1", "Name": "罗总"},
                    {"Id": "spk_2", "Name": "销售"}
                  ]
                }
            """.trimIndent()
        )

        assertEquals(
            mapOf("spk_1" to "罗总", "spk_2" to "销售"),
            parseTranscriptionSpeakerLabels(root as kotlinx.serialization.json.JsonObject)
        )
    }

    @Test
    fun `parseTranscriptionDiarizedSegments reads paragraph word speaker ids`() {
        val root = Json.parseToJsonElement(
            """
                {
                  "Paragraphs": [
                    {
                      "Words": [
                        {"SentenceId": 1, "Text": "你好", "Start": 0, "End": 250, "SpeakerId": "spk_1"},
                        {"SentenceId": 1, "Text": "罗总", "Start": 250, "End": 600, "SpeakerId": "spk_1"},
                        {"SentenceId": 2, "Text": "欢迎", "Start": 800, "End": 1100, "SpeakerId": "spk_2"}
                      ]
                    }
                  ]
                }
            """.trimIndent()
        ) as kotlinx.serialization.json.JsonObject

        val segments = parseTranscriptionDiarizedSegments(root)

        assertEquals(2, segments.size)
        assertEquals("spk_1", segments[0].speakerId)
        assertEquals(0, segments[0].speakerIndex)
        assertEquals(0L, segments[0].startMs)
        assertEquals(600L, segments[0].endMs)
        assertEquals("你好罗总", segments[0].text)
        assertEquals("spk_2", segments[1].speakerId)
        assertEquals(1, segments[1].speakerIndex)
        assertEquals(800L, segments[1].startMs)
        assertEquals(1100L, segments[1].endMs)
        assertEquals("欢迎", segments[1].text)
    }

    @Test
    fun `parseTranscriptionDiarizedSegments uses paragraph speaker fallback`() {
        val root = Json.parseToJsonElement(
            """
                {
                  "Paragraphs": [
                    {
                      "SpeakerId": "spk_customer",
                      "Words": [
                        {"SentenceId": 7, "Text": "预算", "Start": 1000, "End": 1300},
                        {"SentenceId": 7, "Text": "四百万", "Start": 1300, "End": 1900}
                      ]
                    }
                  ]
                }
            """.trimIndent()
        ) as kotlinx.serialization.json.JsonObject

        val segments = parseTranscriptionDiarizedSegments(root)

        assertEquals(1, segments.size)
        assertEquals("spk_customer", segments[0].speakerId)
        assertEquals(0, segments[0].speakerIndex)
        assertEquals(1000L, segments[0].startMs)
        assertEquals(1900L, segments[0].endMs)
        assertEquals("预算四百万", segments[0].text)
    }
}
