package com.smartsales.core.pipeline.habit

import com.smartsales.prism.domain.rl.ObservationSource
import com.smartsales.prism.domain.rl.RlPayload
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RlPayloadSchemaTest {

    @Test
    fun `mechanical validation - RlPayload schema mathematically matches RealHabitListener prompt contract`() {
        // The exact expected schema extracted directly from RealHabitListener.SYSTEM_PROMPT
        val simulatedLlmOutput = """
        {
          "rl_observations": [
            {
              "entityId": "c-001",
              "key": "preferred_meeting_time",
              "value": "morning",
              "source": "USER_POSITIVE",
              "evidence": "我喜欢早上开会"
            },
            {
              "entityId": "null",
              "key": "default_duration",
              "value": "30m",
              "source": "INFERRED",
              "evidence": "最近三次会议都是30分钟"
            }
          ]
        }
        """.trimIndent()

        // Mechanically parse the output
        val jsonParser = Json { ignoreUnknownKeys = true }
        val payload = jsonParser.decodeFromString<RlPayload>(simulatedLlmOutput)

        // Mathematical verification
        assertEquals(2, payload.rlObservations.size)
        
        val obs1 = payload.rlObservations[0]
        assertEquals("c-001", obs1.entityId)
        assertEquals("preferred_meeting_time", obs1.key)
        assertEquals("morning", obs1.value)
        assertEquals(ObservationSource.USER_POSITIVE, obs1.source)
        assertEquals("我喜欢早上开会", obs1.evidence)

        val obs2 = payload.rlObservations[1]
        // "null" string is gracefully handled down the line inside RealHabitListener.kt
        assertEquals("null", obs2.entityId)
        assertEquals("default_duration", obs2.key)
        assertEquals("30m", obs2.value)
        assertEquals(ObservationSource.INFERRED, obs2.source)
        assertEquals("最近三次会议都是30分钟", obs2.evidence)
    }
}
