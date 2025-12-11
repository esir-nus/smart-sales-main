package com.smartsales.feature.chat.core

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/core/SystemPromptBuilderTest.kt
// 模块：:feature:chat
// 说明：验证 SystemPromptBuilder 在 persona 填充下的行为
// 作者：创建于 2025-12-11

import com.smartsales.feature.usercenter.SalesPersona
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class SystemPromptBuilderTest {

    @Test
    fun `persona block uses provided values`() {
        val persona = SalesPersona(
            role = "经理",
            industry = "汽车",
            mainChannel = "微信",
            experienceLevel = "中级",
            stylePreference = "跳跃"
        )
        val prompt = SystemPromptBuilder.buildForHomeChat(
            SystemPromptContext(
                persona = persona,
                quickSkillId = null,
                isFirstGeneralAssistantReply = true
            )
        )
        assertTrue(prompt.contains("岗位：经理"))
        assertTrue(prompt.contains("行业：汽车"))
        assertTrue(prompt.contains("主要沟通渠道：微信"))
        assertTrue(prompt.contains("经验水平：中级"))
        assertTrue(prompt.contains("表达风格：跳跃"))
        assertFalse(prompt.contains("你所在的行业"))
        assertTrue(prompt.contains("<Visible2User>"))
        assertTrue(prompt.contains("<Metadata>"))
    }

    @Test
    fun `persona block falls back only for missing fields`() {
        val persona = SalesPersona(
            role = "顾问",
            industry = null,
            mainChannel = "",
            experienceLevel = "资深",
            stylePreference = null
        )
        val prompt = SystemPromptBuilder.buildForHomeChat(
            SystemPromptContext(
                persona = persona,
                quickSkillId = null,
                isFirstGeneralAssistantReply = true
            )
        )
        assertTrue(prompt.contains("岗位：顾问"))
        assertTrue(prompt.contains("行业：你所在的行业"))
        assertTrue(prompt.contains("主要沟通渠道：线上+线下混合沟通"))
        assertTrue(prompt.contains("经验水平：资深"))
        assertTrue(prompt.contains("表达风格：偏口语/像和同事聊天"))
    }
}
