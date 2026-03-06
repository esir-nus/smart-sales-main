package com.smartsales.prism.domain.session

/**
 * 会话标题生成器 — 根据对话内容自动提取客户名和摘要
 * 
 * @see docs/cerb/input-parser/spec.md Wave 4
 */
interface SessionTitleGenerator {
    /**
     * 根据 InputParserService 解析的 JSON 语义生成会话标题
     * @param rawParsedJson 解析器返回的 JSON
     * @param resolvedNames 匹配到的客户名单
     * @return 提取结果
     */
    fun generateTitle(rawParsedJson: String, resolvedNames: List<String>): TitleResult
}

/**
 * 标题生成结果
 */
data class TitleResult(
    val clientName: String, // 客户名 (或 "客户/未知")
    val summary: String     // 6字摘要 (或 "新会话")
)
