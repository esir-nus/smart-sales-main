package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/MarkdownMessageTextTest.kt
// 模块：:feature:chat
// 说明：验证受控 Markdown 解析的块级与内联样式
// 作者：创建于 2025-12-11

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownMessageTextTest {

    @Test
    fun `parse headings bullets ordered and paragraph`() {
        val text = """
            # 一级标题
            ## 二级标题
            - 要点一
            1. 步骤一
            普通段落
        """.trimIndent()
        val blocks = parseMarkdownBlocks(text)
        assertEquals(5, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Heading)
        assertEquals("一级标题", (blocks[0] as MarkdownBlock.Heading).text)
        assertTrue(blocks[1] is MarkdownBlock.Heading)
        assertTrue(blocks[2] is MarkdownBlock.BulletItem)
        assertEquals("要点一", (blocks[2] as MarkdownBlock.BulletItem).text)
        assertTrue(blocks[3] is MarkdownBlock.OrderedItem)
        assertEquals(1, (blocks[3] as MarkdownBlock.OrderedItem).index)
        assertTrue(blocks[4] is MarkdownBlock.Paragraph)
    }

    @Test
    fun `build annotated applies bold and italic`() {
        val annotated = buildAnnotated("文本 **重点** 和 _提示_ 结束")
        val spans = annotated.spanStyles
        val hasBold = spans.any { it.item.fontWeight == FontWeight.Bold }
        val hasItalic = spans.any { it.item.fontStyle == FontStyle.Italic }
        assertTrue(hasBold)
        assertTrue(hasItalic)
    }
}
