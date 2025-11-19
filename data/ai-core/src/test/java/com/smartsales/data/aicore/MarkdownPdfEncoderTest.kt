// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/MarkdownPdfEncoderTest.kt
// 模块：:data:ai-core
// 说明：验证 MarkdownPdfEncoder 的 JVM 渲染路径
// 作者：创建于 2025-11-16
package com.smartsales.data.aicore

import java.nio.charset.StandardCharsets
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownPdfEncoderTest {

    @Test
    fun `生成的PDF包含标题与正文行`() {
        val bytes = MarkdownPdfEncoder.encode(
            """
            # Title
            - First
            - Second
            """.trimIndent()
        )
        val pdfText = bytes.toPdfAscii()

        assertTrue(pdfText.startsWith("%PDF-1.4"))
        assertTrue(pdfText.contains("Title"))
        assertTrue(pdfText.contains("First"))
        assertTrue(pdfText.contains("Second"))
    }

    @Test
    fun `特殊字符会被转义避免破坏PDF语法`() {
        val bytes = MarkdownPdfEncoder.encode(
            """
            inline(test)
            path with slash: C:\demo\file.md
            """.trimIndent()
        )
        val pdfText = bytes.toPdfAscii()

        assertTrue(pdfText.contains("\\(test\\)"))
        assertTrue(pdfText.contains("C:\\\\demo\\\\file.md"))
    }

    private fun ByteArray.toPdfAscii(): String =
        String(this, StandardCharsets.US_ASCII)
}
