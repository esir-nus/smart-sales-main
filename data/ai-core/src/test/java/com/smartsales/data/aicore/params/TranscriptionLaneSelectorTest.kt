// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/params/TranscriptionLaneSelectorTest.kt
// 模块：:data:ai-core
// 说明：验证转写提供方选择与禁用回退规则
// 作者：创建于 2025-12-22
package com.smartsales.data.aicore.params

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranscriptionLaneSelectorTest {

    @Test
    fun `default provider uses tingwu`() {
        val decision = TranscriptionLaneSelector.resolve(AiParaSettingsSnapshot())
        assertEquals(TRANSCRIPTION_PROVIDER_TINGWU, decision.requestedProvider)
        assertEquals(TRANSCRIPTION_PROVIDER_TINGWU, decision.selectedProvider)
        assertNull(decision.disabledReason)
    }

    @Test
    fun `xfyun requested but disabled falls back to tingwu`() {
        val snapshot = AiParaSettingsSnapshot(
            transcription = TranscriptionSettings(
                provider = TRANSCRIPTION_PROVIDER_XFYUN,
                xfyunEnabled = false,
            )
        )
        val decision = TranscriptionLaneSelector.resolve(snapshot)
        assertEquals(TRANSCRIPTION_PROVIDER_XFYUN, decision.requestedProvider)
        assertEquals(TRANSCRIPTION_PROVIDER_TINGWU, decision.selectedProvider)
        assertEquals("XFYUN_DISABLED_BY_SETTING", decision.disabledReason)
    }

    @Test
    fun `xfyun enabled allows xfyun`() {
        val snapshot = AiParaSettingsSnapshot(
            transcription = TranscriptionSettings(
                provider = TRANSCRIPTION_PROVIDER_XFYUN,
                xfyunEnabled = true,
            )
        )
        val decision = TranscriptionLaneSelector.resolve(snapshot)
        assertEquals(TRANSCRIPTION_PROVIDER_XFYUN, decision.requestedProvider)
        assertEquals(TRANSCRIPTION_PROVIDER_XFYUN, decision.selectedProvider)
        assertNull(decision.disabledReason)
    }

    @Test
    fun `unknown provider falls back to tingwu`() {
        val snapshot = AiParaSettingsSnapshot(
            transcription = TranscriptionSettings(
                provider = "UNKNOWN_PROVIDER",
            )
        )
        val decision = TranscriptionLaneSelector.resolve(snapshot)
        assertEquals("UNKNOWN_PROVIDER", decision.requestedProvider)
        assertEquals(TRANSCRIPTION_PROVIDER_TINGWU, decision.selectedProvider)
        assertEquals("PROVIDER_UNKNOWN:UNKNOWN_PROVIDER", decision.disabledReason)
    }
}
