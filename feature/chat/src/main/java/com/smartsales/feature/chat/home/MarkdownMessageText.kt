package com.smartsales.feature.chat.home

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/MarkdownMessageText.kt
// 模块：:feature:chat
// 说明：为 GENERAL 助手气泡渲染受控的 Markdown 子集（标题/列表/粗体/斜体）
// 作者：创建于 2025-12-11

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

internal sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class OrderedItem(val index: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

/** 受控 Markdown 渲染：仅支持标题、列表、粗体/斜体，避免引入完整解析器。 */
@Composable
fun MarkdownMessageText(
    text: String,
    modifier: Modifier = Modifier
) {
    val blocks = parseMarkdownBlocks(text)
    Column(modifier = modifier) {
        blocks.forEachIndexed { idx, block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style = if (block.level <= 1) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    }
                    Text(
                        text = buildAnnotated(block.text),
                        style = style,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row {
                        Text(text = "•", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildAnnotated(block.text),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                is MarkdownBlock.OrderedItem -> {
                    Row {
                        Text(
                            text = "${block.index}.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildAnnotated(block.text),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildAnnotated(block.text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        overflow = TextOverflow.Visible
                    )
                }
            }
            if (idx != blocks.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

// 仅支持 #/## 标题、-/* 列表、数字序号、粗体(**)/斜体(_)
internal fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    if (text.isBlank()) return emptyList()
    val lines = text.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    var orderIndex = 1
    lines.forEach { line ->
        val trimmed = line.trimEnd()
        if (trimmed.isBlank()) return@forEach
        when {
            trimmed.startsWith("## ") -> {
                blocks += MarkdownBlock.Heading(level = 2, text = trimmed.removePrefix("## ").trim())
            }

            trimmed.startsWith("# ") -> {
                blocks += MarkdownBlock.Heading(level = 1, text = trimmed.removePrefix("# ").trim())
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                blocks += MarkdownBlock.BulletItem(text = trimmed.drop(2).trim())
            }

            Regex("^\\d+\\.\\s+.*").matches(trimmed) -> {
                val idxText = trimmed.takeWhile { it.isDigit() }
                val body = trimmed.dropWhile { it.isDigit() || it == '.' || it.isWhitespace() }
                val num = idxText.toIntOrNull() ?: orderIndex
                orderIndex = num + 1
                blocks += MarkdownBlock.OrderedItem(index = num, text = body.trim())
            }

            else -> {
                blocks += MarkdownBlock.Paragraph(text = trimmed)
            }
        }
    }
    return blocks
}

internal fun buildAnnotated(text: String): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", startIndex = i + 2)
                if (end > i + 2) {
                    val content = text.substring(i + 2, end)
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(content)
                    }
                    i = end + 2
                } else {
                    builder.append(text[i])
                    i++
                }
            }

            text.startsWith("_", i) -> {
                val end = text.indexOf("_", startIndex = i + 1)
                if (end > i + 1) {
                    val content = text.substring(i + 1, end)
                    builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(content)
                    }
                    i = end + 1
                } else {
                    builder.append(text[i])
                    i++
                }
            }

            else -> {
                builder.append(text[i])
                i++
            }
        }
    }
    return builder.toAnnotatedString()
}
