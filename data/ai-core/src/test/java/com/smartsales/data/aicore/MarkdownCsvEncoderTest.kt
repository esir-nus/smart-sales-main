package com.smartsales.data.aicore

import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Test

// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/MarkdownCsvEncoderTest.kt
// 模块：:data:ai-core
// 说明：验证 Markdown 转 CSV 的转换规则
// 作者：创建于 2025-11-16
class MarkdownCsvEncoderTest {

    @Test
    fun encode_buildsTwoColumnCsv() {
        val markdown = """
            ## 摘要
            - 客户同意付款
            负责人: 李雷
        """.trimIndent()

        val csv = MarkdownCsvEncoder.encode(markdown).toString(StandardCharsets.UTF_8)

        val expected = """
            section,content
            "摘要","客户同意付款"
            "负责人","李雷"
        """.trimIndent().replace("\n", System.lineSeparator())

        assertEquals(expected, csv)
    }

    @Test
    fun encode_handlesEmptyInput() {
        val csv = MarkdownCsvEncoder.encode("").toString(StandardCharsets.UTF_8)
        val expected = "section,content${System.lineSeparator()}\"content\",\"\""
        assertEquals(expected, csv)
    }

    @Test
    fun encode_preservesSubtitleStyleTranscriptLines() {
        val markdown = """
            ## 逐字稿
            - [00:01] 发言人 1：你好罗总。
            - [00:05] 发言人 2：欢迎光临。
        """.trimIndent()

        val csv = MarkdownCsvEncoder.encode(markdown).toString(StandardCharsets.UTF_8)
        val expected = """
            section,content
            "逐字稿","[00:01] 发言人 1：你好罗总。"
            "逐字稿","[00:05] 发言人 2：欢迎光临。"
        """.trimIndent().replace("\n", System.lineSeparator())

        assertEquals(expected, csv)
    }
}
