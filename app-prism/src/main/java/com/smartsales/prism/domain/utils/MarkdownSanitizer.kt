package com.smartsales.prism.domain.utils

/**
 * Markdown 结构预处理器
 * 
 * 处理 markdown 的 结构 问题（换行、列表符号），保留 样式 标记（**加粗**）。
 * 样式标记由 UI 层的 MarkdownText 组件渲染。
 * 
 * 职责分工:
 * - MarkdownSanitizer: 结构（换行、列表符号、标题剥离）
 * - MarkdownText: 样式（加粗渲染）
 */
object MarkdownSanitizer {
    
    fun strip(text: String): String {
        if (text.isBlank()) return text
        
        return text
            // 标题: "## Title" → "Title"
            .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
            
            // 注意: 不剥离 **加粗** — 由 MarkdownText 渲染
            
            // 分隔线: --- → (空)
            .replace(Regex("^---+$", RegexOption.MULTILINE), "")
            
            // 确保编号列表项各占一行:
            // "...文字2. 下一条" → "...文字\n2. 下一条"
            .replace(Regex("(?<=\\D)(\\d{1,2})\\.\\s+"), "\n$1. ")
            // "...文字1.13:42" → "...文字\n1. 13:42" (时间格式)
            .replace(Regex("(?<=\\D)(\\d{1,2})\\.(\\d{1,2}:\\d{2})"), "\n$1. $2")
            
            // 无序列表: "- item" → "• item"
            .replace(Regex("^\\s*[-]\\s+", RegexOption.MULTILINE), "• ")
            
            // 清理多余空行
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}
