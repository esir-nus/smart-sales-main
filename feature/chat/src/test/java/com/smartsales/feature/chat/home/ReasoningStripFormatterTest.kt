package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/ReasoningStripFormatterTest.kt
// 模块：:feature:chat
// 说明：验证 SMART 分析“分析依据”文案的格式化逻辑与回退
// 作者：创建于 2025-12-09

import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.RiskLevel
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionStage
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReasoningStripFormatterTest {

    private fun build(meta: SessionMetadata?) = ReasoningStripFormatter.build(
        metadata = meta,
        formatStage = { "阶段-${it.name}" },
        formatRisk = { "风险-${it.name}" },
        formatSource = { "来源-${it.name}" },
        formatTime = { "时间-$it" }
    )

    @Test
    fun build_whenMetadataNull_returnsNull() {
        assertNull(build(null))
    }

    @Test
    fun build_withPartialMetadata_usesFallbacks() {
        val meta = SessionMetadata(
            sessionId = "s1",
            stage = SessionStage.DISCOVERY,
            riskLevel = null,
            tags = emptySet()
        )
        val text = build(meta)
        requireNotNull(text)
        assertTrue(text.contains("阶段 阶段-DISCOVERY"))
        assertTrue(text.contains("风险 未标注"))
        assertTrue(text.contains("标签 无标签"))
        assertTrue(text.contains("来源 未知来源"))
    }

    @Test
    fun build_withFullMetadata_formatsTagsAndSourceTime() {
        val meta = SessionMetadata(
            sessionId = "s2",
            stage = SessionStage.NEGOTIATION,
            riskLevel = RiskLevel.HIGH,
            tags = setOf("试驾", "报价", "跟进", "多余"),
            latestMajorAnalysisSource = AnalysisSource.SMART_ANALYSIS_USER,
            latestMajorAnalysisAt = 1700000000000L
        )
        val text = build(meta)
        requireNotNull(text)
        assertTrue(text.contains("阶段 阶段-NEGOTIATION"))
        assertTrue(text.contains("风险 风险-HIGH"))
        assertTrue(text.contains("试驾、报价、跟进"))
        assertTrue(text.contains("来源 来源-SMART_ANALYSIS_USER（时间-1700000000000）"))
    }
}
