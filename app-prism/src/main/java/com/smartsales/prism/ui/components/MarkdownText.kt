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
    color: Color = Color.White,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 20.sp
) {
    val annotated = remember(text) { parseMarkdown(text, color) }
    Text(
        text = annotated,
        modifier = modifier,
        fontSize = fontSize,
        lineHeight = lineHeight
    )
}

/**
 * 解析 markdown 文本 → AnnotatedString
 * 
 * 当前仅处理 **加粗**，后续可扩展斜体、链接等。
 */
private fun parseMarkdown(text: String, defaultColor: Color): AnnotatedString {
    // 匹配 **bold** 模式
    val boldPattern = Regex("\\*\\*(.+?)\\*\\*")
    
    return buildAnnotatedString {
        var lastIndex = 0
        
        boldPattern.findAll(text).forEach { match ->
            // 追加匹配前的普通文本
            if (match.range.first > lastIndex) {
                withStyle(SpanStyle(color = defaultColor)) {
                    append(text.substring(lastIndex, match.range.first))
                }
            }
            
            // 追加加粗文本
            withStyle(SpanStyle(color = defaultColor, fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            
            lastIndex = match.range.last + 1
        }
        
        // 追加剩余文本
        if (lastIndex < text.length) {
            withStyle(SpanStyle(color = defaultColor)) {
                append(text.substring(lastIndex))
            }
        }
    }
}
