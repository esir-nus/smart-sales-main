package com.smartsales.core.pipeline

import com.smartsales.core.context.EnhancedContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptCompilerBadgeTest {

    private val compiler = PromptCompiler()

    @Test
    fun `when isBadge is false, prompt must strictly forbid generation of tasks array`() {
        val context = EnhancedContext(
            userText = "明天下午2点开会",
            isBadge = false
        )
        val prompt = compiler.compile(context)
        
        assertTrue("Prompt should contain the badge_delegation strict rule", prompt.contains("badge_delegation"))
        assertTrue("Prompt should explicitly forbid tasks array", prompt.contains("绝对不能**在 JSON 中生成 `tasks` 数组"))
    }

    @Test
    fun `when isBadge is true, prompt must allow generation of tasks array`() {
        val context = EnhancedContext(
            userText = "明天下午2点开会",
            isBadge = true
        )
        val prompt = compiler.compile(context)
        
        assertFalse("Prompt should NOT contain the badge_delegation strict rule", prompt.contains("badge_delegation"))
        assertTrue("Prompt should instruct to ask for confirmation in UI", prompt.contains("请点击下方的卡片进行确认"))
    }
}
