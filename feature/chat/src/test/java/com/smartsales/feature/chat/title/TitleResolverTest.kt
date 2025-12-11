package com.smartsales.feature.chat.title

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/title/TitleResolverTest.kt
// 模块：:feature:chat
// 说明：验证改名候选与占位/手动改名的决策规则
// 作者：创建于 2025-12-11

import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.feature.chat.AiSessionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TitleResolverTest {

    @Test
    fun `uses rename candidate when placeholder`() {
        val summary = AiSessionSummary(
            id = "s1",
            title = "新的聊天",
            updatedAtMillis = System.currentTimeMillis(),
            isTranscription = false,
            isTitleUserEdited = false,
            pinned = false
        )
        val candidate = TitleCandidate(
            name = "李总",
            title6 = "预算沟通",
            source = TitleSource.GENERAL,
            createdAt = System.currentTimeMillis()
        )

        val resolved = TitleResolver.resolveTitle(summary, candidate, null)
        assertEquals("李总 - 预算沟通", resolved)
    }

    @Test
    fun `respects manual rename`() {
        val summary = AiSessionSummary(
            id = "s1",
            title = "自定义标题",
            updatedAtMillis = System.currentTimeMillis(),
            isTranscription = false,
            isTitleUserEdited = true,
            pinned = false
        )
        val candidate = TitleCandidate(
            name = "王总",
            title6 = "拜访",
            source = TitleSource.GENERAL,
            createdAt = System.currentTimeMillis()
        )

        val resolved = TitleResolver.resolveTitle(summary, candidate, null)
        assertNull(resolved)
    }

    @Test
    fun `falls back to metadata when no candidate`() {
        val summary = AiSessionSummary(
            id = "s1",
            title = "新的聊天",
            updatedAtMillis = System.currentTimeMillis(),
            isTranscription = false,
            isTitleUserEdited = false,
            pinned = false
        )
        val meta = SessionMetadata(
            sessionId = "s1",
            mainPerson = "赵总",
            summaryTitle6Chars = "首谈"
        )
        val resolved = TitleResolver.resolveTitle(summary, null, meta)
        assertEquals("赵总 - 首谈", resolved)
    }
}
