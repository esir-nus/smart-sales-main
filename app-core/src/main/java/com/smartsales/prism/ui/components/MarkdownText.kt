package com.smartsales.prism.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Markdown 文本组件 — 渲染加粗和基础 markdown 样式
 *
 * 处理:
 * - **加粗** → FontWeight.Bold
 * 
 * 结构排版（换行、列表符号）由 MarkdownSanitizer 在数据层处理。
 * 本组件仅负责视觉样式。
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 20.sp
) {
    val annotated = remember(text, color) { parseMarkdown(text, color) }
    Text(
        text = annotated,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight
    )
}

/**
 * 解析 markdown 文本 → AnnotatedString
 * 
 * 处理了 ### 标题、* 列表项 和 **加粗**。
 */
private fun parseMarkdown(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split('\n')
        lines.forEachIndexed { index, line ->
            var currentLine = line
            var isHeader = false
            
            if (currentLine.startsWith("### ")) {
                isHeader = true
                currentLine = currentLine.removePrefix("### ")
            } else if (currentLine.startsWith("* ")) {
                currentLine = currentLine.removePrefix("* ")
                withStyle(SpanStyle(color = defaultColor)) {
                    append("• ")
                }
            }
            
            val boldPattern = Regex("\\*\\*(.+?)\\*\\*")
            var lastIndex = 0
            
            val lineStyle = if (isHeader) {
                SpanStyle(color = defaultColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                SpanStyle(color = defaultColor)
            }
            
            withStyle(lineStyle) {
                boldPattern.findAll(currentLine).forEach { match ->
                    if (match.range.first > lastIndex) {
                        append(currentLine.substring(lastIndex, match.range.first))
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groupValues[1])
                    }
                    lastIndex = match.range.last + 1
                }
                if (lastIndex < currentLine.length) {
                    append(currentLine.substring(lastIndex))
                }
            }
            
            if (index < lines.size - 1) append("\n")
        }
    }
}
