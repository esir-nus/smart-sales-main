package com.smartsales.data.prismlib.linters

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.smartsales.domain.prism.core.LintError
import com.smartsales.domain.prism.core.LintResult
import com.smartsales.domain.prism.core.LintSeverity
import com.smartsales.domain.prism.core.entities.RelevancyEntry
import com.smartsales.domain.prism.core.linters.Linter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Relevancy Linter 实现 — 验证 RelevancyEntry 更新
 * @see Prism-V1.md §2.2 #6, §5.2
 */
@Singleton
class RealRelevancyLinter @Inject constructor() : Linter {

    private val gson = Gson()
    
    // Entity ID 格式
    private val entityIdPattern = Regex("^[a-z]-\\d{3}$")

    override fun validate(content: String): LintResult {
        val errors = mutableListOf<LintError>()

        // 1. 验证 JSON 格式
        val entry = try {
            gson.fromJson(content, RelevancyEntry::class.java)
        } catch (e: JsonSyntaxException) {
            return LintResult.Fail(listOf(
                LintError("json", "无效的 JSON 格式: ${e.message}", LintSeverity.ERROR)
            ))
        }

        if (entry == null) {
            return LintResult.Fail(listOf(
                LintError("json", "JSON 解析结果为空", LintSeverity.ERROR)
            ))
        }

        // 2. 验证 entityId 格式
        if (!entityIdPattern.matches(entry.entityId)) {
            errors.add(LintError(
                "entityId",
                "Entity ID 格式无效，应为 [a-z]-[0-9]{3}",
                LintSeverity.ERROR
            ))
        }

        // 3. 验证 displayName 非空
        if (entry.displayName.isBlank()) {
            errors.add(LintError(
                "displayName",
                "实体显示名称不能为空",
                LintSeverity.ERROR
            ))
        }

        // 4. 验证 aliases 列表（可为空，但如果有内容需要检查）
        entry.aliases.forEach { alias ->
            if (alias.alias.isBlank()) {
                errors.add(LintError(
                    "aliases",
                    "别名不能为空字符串",
                    LintSeverity.WARNING
                ))
            }
        }

        return if (errors.any { it.severity == LintSeverity.ERROR }) {
            LintResult.Fail(errors)
        } else {
            LintResult.Pass
        }
    }
}
