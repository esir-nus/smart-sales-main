package com.smartsales.domain.prism.core

/**
 * Schema Linter 结果 — 验证 AI 结构化输出
 * @see Prism-V1.md §2.2 #6
 */
sealed class LintResult {
    object Pass : LintResult()
    data class Fail(val errors: List<LintError>) : LintResult()
    
    val isPass: Boolean get() = this is Pass
    val isFail: Boolean get() = this is Fail
}

/**
 * Lint 错误详情
 */
data class LintError(
    val field: String,
    val message: String,
    val severity: LintSeverity = LintSeverity.ERROR
)

/**
 * Lint 错误严重程度
 */
enum class LintSeverity {
    WARNING,  // 警告，不阻止写入
    ERROR     // 错误，阻止写入
}
