package com.smartsales.data.prismlib.linters

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.smartsales.domain.prism.core.LintError
import com.smartsales.domain.prism.core.LintResult
import com.smartsales.domain.prism.core.LintSeverity
import com.smartsales.domain.prism.core.linters.ExtractedEntity
import com.smartsales.domain.prism.core.linters.Linter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 实体 Linter 实现 — 验证 ExtractedEntity JSON
 * @see Prism-V1.md §2.2 #6, §5.6
 */
@Singleton
class RealEntityLinter @Inject constructor() : Linter {

    private val gson = Gson()
    
    // ID 格式: 字母-三位数字，如 z-001, p-042
    private val idPattern = Regex("^[a-z]-\\d{3}$")

    override fun validate(content: String): LintResult {
        val errors = mutableListOf<LintError>()

        // 1. 验证 JSON 格式
        val entity = try {
            gson.fromJson(content, ExtractedEntity::class.java)
        } catch (e: JsonSyntaxException) {
            return LintResult.Fail(listOf(
                LintError("json", "无效的 JSON 格式: ${e.message}", LintSeverity.ERROR)
            ))
        }

        if (entity == null) {
            return LintResult.Fail(listOf(
                LintError("json", "JSON 解析结果为空", LintSeverity.ERROR)
            ))
        }

        // 2. 验证 extractedName 非空
        if (entity.extractedName.isBlank()) {
            errors.add(LintError("extractedName", "实体名称不能为空", LintSeverity.ERROR))
        }

        // 3. 验证 extractedId 格式（如果存在）
        entity.extractedId?.let { id ->
            if (!idPattern.matches(id)) {
                errors.add(LintError(
                    "extractedId",
                    "ID 格式无效，应为 [a-z]-[0-9]{3}，如 z-001",
                    LintSeverity.ERROR
                ))
            }
        }

        // 4. 验证 confidence 范围
        if (entity.confidence < 0f || entity.confidence > 1f) {
            errors.add(LintError(
                "confidence",
                "置信度应在 0.0 到 1.0 之间",
                LintSeverity.WARNING
            ))
        }

        return if (errors.isEmpty()) LintResult.Pass else LintResult.Fail(errors)
    }
}
