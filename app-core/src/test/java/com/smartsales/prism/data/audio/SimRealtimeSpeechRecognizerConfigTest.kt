package com.smartsales.prism.data.audio

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SimRealtimeSpeechRecognizerConfigTest {

    @Test
    fun `onboarding profile sets six second sentence silence`() {
        val params = JSONObject(buildFunAsrRealtimeParams(SimRealtimeSpeechProfile.ONBOARDING))
        val nlsConfig = params.getJSONObject("nls_config")

        assertEquals(6_000, nlsConfig.getInt("max_sentence_silence"))
        assertFalse(nlsConfig.getBoolean("semantic_punctuation_enabled"))
    }

    @Test
    fun `sim draft profile keeps default sentence silence`() {
        val params = JSONObject(buildFunAsrRealtimeParams(SimRealtimeSpeechProfile.SIM_DRAFT))
        val nlsConfig = params.getJSONObject("nls_config")

        assertFalse(nlsConfig.has("max_sentence_silence"))
        assertFalse(nlsConfig.getBoolean("semantic_punctuation_enabled"))
    }

    @Test
    fun `fun asr realtime mode maps to onboarding profile`() {
        assertEquals(
            SimRealtimeSpeechProfile.ONBOARDING,
            realtimeSpeechProfileForMode(DeviceSpeechMode.FUN_ASR_REALTIME)
        )
        assertEquals(
            SimRealtimeSpeechProfile.SIM_DRAFT,
            realtimeSpeechProfileForMode(DeviceSpeechMode.DEVICE_ONLY)
        )
    }
}
