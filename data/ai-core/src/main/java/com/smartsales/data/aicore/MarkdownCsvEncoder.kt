package com.smartsales.data.aicore

import java.nio.charset.StandardCharsets

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/MarkdownCsvEncoder.kt
// 模块：:data:ai-core
// 说明：将Markdown结构转换为易读的CSV格式
// 作者：创建于 2025-11-16
internal object MarkdownCsvEncoder {
    private val keyValuePattern = Regex("^(.+?):\\s*(.+)$")

    fun encode(markdown: String): ByteArray {
        val rows = buildRows(markdown)
        val builder = StringBuilder()
        builder.append("section,content\n")
        rows.forEachIndexed { index, row ->
            if (index > 0) builder.append("\n")
            builder.append(row.section.csvEscape())
                .append(",")
                .append(row.content.csvEscape())
        }
        return builder.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun buildRows(markdown: String): List<Row> {
        val rows = mutableListOf<Row>()
        var currentSection = ""
        markdown
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    return@forEach
                }
                when {
                    trimmed.startsWith("#") -> {
                        currentSection = trimmed.trimStart('#', ' ', '\t')
                    }
                    trimmed.startsWith("- ") -> {
                        rows += Row(currentSection, trimmed.drop(2).trim())
                    }
                    keyValuePattern.matches(trimmed) -> {
                        val match = keyValuePattern.find(trimmed)!!
                        val key = match.groupValues[1].trim()
                        val value = match.groupValues[2].trim()
                        rows += Row(key.ifBlank { currentSection }, value)
                    }
                    else -> rows += Row(currentSection, trimmed)
                }
            }
        if (rows.isEmpty()) {
            rows += Row("content", markdown.trim())
        }
        return rows
    }

    private fun String.csvEscape(): String {
        val escaped = replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private data class Row(val section: String, val content: String)
}
