// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/params/XfyunVoiceprintSettingsTest.kt
// 模块：:data:ai-core
// 说明：验证讯飞声纹配置的规范化与 fail-soft 逻辑（featureIds/roleNum/disabledReason）
// 作者：创建于 2025-12-19
package com.smartsales.data.aicore.params

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XfyunVoiceprintSettingsTest {

    @Test
    fun `resolveEffective disabled when setting off`() {
        val effective = XfyunVoiceprintSettings(
            enabled = false,
            featureIds = listOf("  vp_a ", "vp_a", "vp_b"),
            roleNum = 0,
        ).resolveEffective()

        assertFalse(effective.effectiveEnabled)
        assertEquals(false, effective.enabledSetting)
        assertEquals(listOf("vp_a", "vp_b"), effective.featureIds)
        assertEquals("VOICEPRINT_DISABLED_SETTING_OFF", effective.disabledReason)
    }

    @Test
    fun `resolveEffective disabled when featureIds empty`() {
        val effective = XfyunVoiceprintSettings(
            enabled = true,
            featureIds = listOf("  ", "\n"),
            roleNum = 0,
        ).resolveEffective()

        assertFalse(effective.effectiveEnabled)
        assertEquals(true, effective.enabledSetting)
        assertTrue(effective.featureIds.isEmpty())
        assertEquals("VOICEPRINT_DISABLED_EMPTY_FEATURE_IDS", effective.disabledReason)
    }

    @Test
    fun `resolveEffective disabled when roleNum invalid`() {
        val effective = XfyunVoiceprintSettings(
            enabled = true,
            featureIds = listOf("vp_a"),
            roleNum = 11,
        ).resolveEffective()

        assertFalse(effective.effectiveEnabled)
        assertEquals(true, effective.enabledSetting)
        assertEquals(listOf("vp_a"), effective.featureIds)
        assertNull(effective.roleNum)
        assertEquals("VOICEPRINT_DISABLED_INVALID_ROLE_NUM", effective.disabledReason)
    }

    @Test
    fun `resolveEffective caps featureIds to 64 and keeps order`() {
        val raw = buildList {
            add("  vp_001 ")
            add("vp_001")
            repeat(80) { index ->
                add("vp_${(index + 2).toString().padStart(3, '0')}")
            }
        }
        val effective = XfyunVoiceprintSettings(
            enabled = true,
            featureIds = raw,
            roleNum = 0,
        ).resolveEffective()

        assertTrue(effective.effectiveEnabled)
        assertEquals(true, effective.featureIdsTruncated)
        assertEquals(64, effective.featureIds.size)
        assertEquals("vp_001", effective.featureIds.first())
    }

    @Test
    fun `repository helper enables voiceprint and appends featureId`() {
        val repo = InMemoryAiParaSettingsRepository()

        repo.enableVoiceprintAndAddFeatureId("  vp_a  ")
        repo.enableVoiceprintAndAddFeatureId("vp_a")
        repo.enableVoiceprintAndAddFeatureId("vp_b")

        val voiceprint = repo.snapshot().transcription.xfyun.voiceprint
        assertTrue(voiceprint.enabled)
        assertEquals(listOf("vp_a", "vp_b"), voiceprint.featureIds)
    }

    @Test
    fun `defaults are fail-soft`() {
        val snapshot = AiParaSettingsSnapshot()
        assertFalse(snapshot.transcription.xfyun.voiceprint.enabled)
        assertFalse(snapshot.transcription.xfyun.postXfyun.enabled)
        assertEquals(1, snapshot.transcription.xfyun.upload.roleType)
        assertEquals("autodialect", snapshot.transcription.xfyun.upload.language)
    }
}
