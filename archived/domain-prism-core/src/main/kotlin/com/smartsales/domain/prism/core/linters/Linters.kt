package com.smartsales.domain.prism.core.linters

import com.smartsales.domain.prism.core.LintResult

/**
 * 实体 Linter — 验证 ExtractedEntity JSON
 * Phase 1: 直接返回 Pass
 */
class EntityLinter : Linter {
    override fun validate(content: String): LintResult = LintResult.Pass
}

/**
 * 计划 Linter — 验证 ExecutionPlan JSON
 * Phase 1: 直接返回 Pass
 */
class PlanLinter : Linter {
    override fun validate(content: String): LintResult = LintResult.Pass
}

/**
 * 日程 Linter — 验证 SchedulerCommand JSON
 * Phase 1: 直接返回 Pass
 */
class SchedulerLinter : Linter {
    override fun validate(content: String): LintResult = LintResult.Pass
}

/**
 * Relevancy Linter — 验证 RelevancyEntry 更新
 * Phase 1: 直接返回 Pass
 */
class RelevancyLinter : Linter {
    override fun validate(content: String): LintResult = LintResult.Pass
}
