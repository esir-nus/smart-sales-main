package com.smartsales.data.aicore

// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/TingwuSessionMetadataMapperTest.kt
// 模块：:data:ai-core
// 说明：验证 Tingwu 元数据映射为 SessionMetadata 的安全子集
// 作者：创建于 2025-12-09

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TingwuSessionMetadataMapperTest {

    @Test
    fun toMetadataPatch_allNull_returnsNull() {
        val patch = TingwuSessionMetadataMapper.toMetadataPatch(
            TingwuMetadataInput(
                sessionId = "s1",
                callSummary = null,
                shortTitleHint = null,
                mainPersonName = null,
                tags = null
            )
        )
        assertNull(patch)
    }

    @Test
    fun toMetadataPatch_summaryOnly_populatesSummaryAndTitle() {
        val patch = TingwuSessionMetadataMapper.toMetadataPatch(
            TingwuMetadataInput(
                sessionId = "s1",
                callSummary = "会议概览：客户确认下季度采购计划",
                shortTitleHint = null,
                mainPersonName = null,
                tags = null
            )
        )
        requireNotNull(patch)
        assertEquals("会议概览：客户确认下季度采购计划", patch.shortSummary)
        // title 应从 summary 推导并截断（8 字以内）
        assertEquals("会议概览：客户确", patch.summaryTitle6Chars)
    }

    @Test
    fun toMetadataPatch_withTitleHint_prefersHint() {
        val patch = TingwuSessionMetadataMapper.toMetadataPatch(
            TingwuMetadataInput(
                sessionId = "s1",
                callSummary = "长摘要不会被当做标题",
                shortTitleHint = "短标题",
                mainPersonName = null,
                tags = null
            )
        )
        requireNotNull(patch)
        assertEquals("短标题", patch.summaryTitle6Chars)
    }

    @Test
    fun toMetadataPatch_setsMainPersonAndTags() {
        val patch = TingwuSessionMetadataMapper.toMetadataPatch(
            TingwuMetadataInput(
                sessionId = "s1",
                callSummary = null,
                shortTitleHint = null,
                mainPersonName = "罗总",
                tags = setOf("试驾", "报价")
            )
        )
        requireNotNull(patch)
        assertEquals("罗总", patch.mainPerson)
        assertEquals(setOf("试驾", "报价"), patch.tags)
    }
}
