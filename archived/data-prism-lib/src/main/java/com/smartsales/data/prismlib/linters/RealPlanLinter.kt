package com.smartsales.data.prismlib.linters

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.smartsales.domain.prism.core.ExecutionPlan
import com.smartsales.domain.prism.core.LintError
import com.smartsales.domain.prism.core.LintResult
import com.smartsales.domain.prism.core.LintSeverity
import com.smartsales.domain.prism.core.linters.Linter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 计划 Linter 实现 — 验证 ExecutionPlan JSON
 * @see Prism-V1.md §2.2 #6, §4.5
 */
@Singleton
class RealPlanLinter @Inject constructor() : Linter {

    private val gson = Gson()

    override fun validate(content: String): LintResult {
        val errors = mutableListOf<LintError>()

        // 1. 验证 JSON 格式
        val plan = try {
            gson.fromJson(content, ExecutionPlan::class.java)
        } catch (e: JsonSyntaxException) {
            return LintResult.Fail(listOf(
                LintError("json", "无效的 JSON 格式: ${e.message}", LintSeverity.ERROR)
            ))
        }

        if (plan == null) {
            return LintResult.Fail(listOf(
                LintError("json", "JSON 解析结果为空", LintSeverity.ERROR)
            ))
        }

        // 2. 验证 deliverables 非空
        if (plan.deliverables.isEmpty()) {
            errors.add(LintError(
                "deliverables",
                "交付物列表不能为空",
                LintSeverity.WARNING
            ))
        }

        // 3. 验证 toolsToInvoke 结构（可为空但不能为 null）
        // ExecutionPlan 的 toolsToInvoke 已有默认值，此处可跳过

        return if (errors.isEmpty()) LintResult.Pass else LintResult.Fail(errors)
    }
}
