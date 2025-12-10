package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/GeneralOutputGuardsTest.kt
// 模块：:feature:chat
// 说明：验证 GENERAL 输出守护策略：去重与长度限制
// 作者：创建于 2025-12-10

import org.junit.Assert.assertTrue
import org.junit.Test

class GeneralOutputGuardsTest {

    @Test
    fun `drops over-repeated sentences and caps length`() {
        val text = "罗总关注价格。罗总关注价格。罗总关注价格。需要明确交付周期。需要明确交付周期。还有其他补充吗？"
        val guarded = applyGeneralOutputGuards(text, maxLength = 60, maxRepeatPerSentence = 2)

        assertTrue(guarded.contains("罗总关注价格。"))
        // 同一句最多 2 次
        val count = Regex("罗总关注价格。").findAll(guarded).count()
        assertTrue("repeat count=$count", count <= 2)
        assertTrue("length=${guarded.length}", guarded.length <= 60)
    }

    @Test
    fun `normalization treats punctuation-free sentences as duplicates`() {
        val text = "客户预算有限，需给到折扣!客户预算有限需给到折扣!请确认交付周期。"
        val guarded = applyGeneralOutputGuards(text, maxLength = 80, maxRepeatPerSentence = 1)

        val duplicates = Regex("预算有限").findAll(guarded).count()
        assertTrue("duplicates=$duplicates", duplicates <= 1)
    }
}
