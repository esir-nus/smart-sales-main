package com.smartsales.data.prismlib.linters

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.smartsales.domain.prism.core.LintError
import com.smartsales.domain.prism.core.LintResult
import com.smartsales.domain.prism.core.LintSeverity
import com.smartsales.domain.prism.core.linters.Linter
import com.smartsales.domain.prism.core.linters.SchedulerCommand
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日程 Linter 实现 — 验证 SchedulerCommand JSON
 * @see Prism-V1.md §2.2 #6, §4.3
 */
@Singleton
class RealSchedulerLinter @Inject constructor() : Linter {

    private val gson = Gson()

    override fun validate(content: String): LintResult {
        val errors = mutableListOf<LintError>()

        // 1. 验证 JSON 格式
        val command = try {
            gson.fromJson(content, SchedulerCommand::class.java)
        } catch (e: JsonSyntaxException) {
            return LintResult.Fail(listOf(
                LintError("json", "无效的 JSON 格式: ${e.message}", LintSeverity.ERROR)
            ))
        }

        if (command == null) {
            return LintResult.Fail(listOf(
                LintError("json", "JSON 解析结果为空", LintSeverity.ERROR)
            ))
        }

        // 2. 验证 title 非空
        if (command.title.isBlank()) {
            errors.add(LintError("title", "日程标题不能为空", LintSeverity.ERROR))
        }

        // 3. 验证 scheduledAt 时间戳有效（如果存在）
        command.scheduledAt?.let { timestamp ->
            if (timestamp < 0) {
                errors.add(LintError(
                    "scheduledAt",
                    "无效的时间戳",
                    LintSeverity.ERROR
                ))
            }
            // 警告：过去的时间
            val now = System.currentTimeMillis()
            if (timestamp < now - ONE_HOUR_MS) {
                errors.add(LintError(
                    "scheduledAt",
                    "日程时间已过去",
                    LintSeverity.WARNING
                ))
            }
        }

        return if (errors.any { it.severity == LintSeverity.ERROR }) {
            LintResult.Fail(errors)
        } else if (errors.isNotEmpty()) {
            // 仅有警告，仍然通过
            LintResult.Pass
        } else {
            LintResult.Pass
        }
    }

    companion object {
        private const val ONE_HOUR_MS = 60 * 60 * 1000L
    }
}
