package com.smartsales.data.aicore

// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/ExportNameResolverTest.kt
// 模块：:data:ai-core
// 说明：验证导出命名解析的优先级（accepted/candidate/fallback）
// 作者：创建于 2025-12-22

import com.smartsales.core.metahub.ExportNameResolver
import com.smartsales.core.metahub.ExportNameSource
import com.smartsales.core.metahub.AcceptedAndCandidate
import com.smartsales.core.metahub.RenamingMetadata
import com.smartsales.core.metahub.SessionMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportNameResolverTest {

    @Test
    fun resolve_acceptedTitleWins() {
        val resolution = ExportNameResolver.resolve(
            sessionId = "s-1",
            sessionTitle = "客户复盘",
            isTitleUserEdited = true,
            meta = SessionMetadata(sessionId = "s-1")
        )

        assertEquals(ExportNameSource.ACCEPTED, resolution.source)
        assertEquals("客户复盘", resolution.baseName)
    }

    @Test
    fun resolve_candidateTitleWhenNotUserEdited() {
        val resolution = ExportNameResolver.resolve(
            sessionId = "s-2",
            sessionTitle = "自动标题",
            isTitleUserEdited = false,
            meta = SessionMetadata(sessionId = "s-2")
        )

        assertEquals(ExportNameSource.CANDIDATE, resolution.source)
        assertEquals("自动标题", resolution.baseName)
    }

    @Test
    fun resolve_prefersM3RenamingWhenPresent() {
        val meta = SessionMetadata(
            sessionId = "s-3",
            renaming = RenamingMetadata(
                sessionTitle = AcceptedAndCandidate(
                    accepted = "用户确认名",
                    candidate = "候选名"
                ),
                exportTitle = AcceptedAndCandidate(candidate = "导出候选名")
            )
        )
        val resolution = ExportNameResolver.resolve(
            sessionId = "s-3",
            sessionTitle = "旧标题",
            isTitleUserEdited = true,
            meta = meta
        )

        assertEquals(ExportNameSource.ACCEPTED, resolution.source)
        assertEquals("用户确认名", resolution.baseName)
    }

    @Test
    fun resolve_fallbackWhenNoTitleOrMeta() {
        val resolution = ExportNameResolver.resolve(
            sessionId = "session-xyz",
            sessionTitle = null,
            isTitleUserEdited = null,
            meta = null
        )

        assertEquals(ExportNameSource.FALLBACK, resolution.source)
        assertTrue(resolution.baseName.startsWith("session_session-xyz"))
    }
}
