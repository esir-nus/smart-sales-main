package com.smartsales.core.metahub

// 文件：core/util/src/test/java/com/smartsales/core/metahub/SessionTitlePolicyTest.kt
// 模块：:core:util
// 说明：验证会话标题与导出文件名的格式与占位规则
// 作者：创建于 2025-12-09

import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTitlePolicyTest {

    @Test
    fun placeholderDetection_handlesLegacyTranscription() {
        assertTrue(SessionTitlePolicy.isPlaceholder("新的聊天"))
        assertTrue(SessionTitlePolicy.isPlaceholder("通话分析 – demo.wav"))
        assertTrue(SessionTitlePolicy.isPlaceholder("通话分析-abc"))
        assertFalse(SessionTitlePolicy.isPlaceholder("客户总结"))
    }

    @Test
    fun buildSuggestedTitle_usesPersonSummaryDateOrder() {
        val meta = SessionMetadata(
            sessionId = "s1",
            mainPerson = "罗总",
            summaryTitle6Chars = "展会跟进"
        )
        val dateMillis = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            .parse("2025-12-09")!!
            .time
        val title = SessionTitlePolicy.buildSuggestedTitle(meta, dateMillis)
        assertEquals("罗总_展会跟进_12/09", title)
    }

    @Test
    fun buildExportBaseName_includesUserAndTimestamp() {
        val meta = SessionMetadata(
            sessionId = "s2",
            mainPerson = "张经理",
            summaryTitle6Chars = "竞争分析"
        )
        val base = SessionTitlePolicy.buildExportBaseName("Alice", meta)
        assertTrue(base.matches(Regex("Alice_张经理_竞争分析_\\d{8}_\\d{6}")))
    }
}
