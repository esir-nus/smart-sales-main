package com.smartsales.feature.chat.title

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/title/SessionTitleGeneratorTest.kt
// 模块：:feature:chat
// 说明：验证会话标题生成逻辑
// 作者：创建于 2025-12-09

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionTitleGeneratorTest {

    private val fixedDate = 1_735_000_000_000L // arbitrary fixed millis for stable MM/dd

    @Test
    fun derive_title_with_honorific_and_scene() {
        val title = SessionTitleGenerator.deriveSessionTitle(
            updatedAtMillis = fixedDate,
            firstUserMessage = "罗总，中国区奥迪主管。展会项目",
            firstAssistantMessage = null
        )
        assertEquals("11/14_罗总_展会项目", title)
    }

    @Test
    fun derive_title_with_company_and_email() {
        val title = SessionTitleGenerator.deriveSessionTitle(
            updatedAtMillis = fixedDate,
            firstUserMessage = "给阿里巴巴写一封报价跟进邮件",
            firstAssistantMessage = null
        )
        assertEquals("11/14_阿里巴巴_报价跟进邮件", title)
    }

    @Test
    fun derive_title_generic_when_vague() {
        val title = SessionTitleGenerator.deriveSessionTitle(
            updatedAtMillis = fixedDate,
            firstUserMessage = "帮我优化一下销售话术",
            firstAssistantMessage = null
        )
        assertEquals("11/14_未知客户_销售话术优化", title)
    }
}
