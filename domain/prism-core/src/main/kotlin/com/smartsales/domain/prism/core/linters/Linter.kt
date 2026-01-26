package com.smartsales.domain.prism.core.linters

import com.smartsales.domain.prism.core.LintResult

/**
 * Linter 接口 — 验证 AI 结构化输出
 * @see Prism-V1.md §2.2 #6
 */
interface Linter {
    /**
     * 验证 JSON 内容
     * @param content 待验证的 JSON 字符串
     * @return 验证结果
     */
    fun validate(content: String): LintResult
}
