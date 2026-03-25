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
}
