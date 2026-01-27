package com.smartsales.data.prismlib.linters

import com.smartsales.domain.prism.core.linters.Linter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Linter 注册表 — 根据输出类型路由到对应的 Linter
 * @see Prism-V1.md §2.2 #6
 */
@Singleton
class LinterRegistry @Inject constructor(
    private val entityLinter: RealEntityLinter,
    private val planLinter: RealPlanLinter,
    private val schedulerLinter: RealSchedulerLinter,
    private val relevancyLinter: RealRelevancyLinter
) {

    /**
     * 根据输出类型获取 Linter
     */
    fun getLinter(outputType: OutputType): Linter {
        return when (outputType) {
            OutputType.EntityExtraction -> entityLinter
            OutputType.ExecutionPlan -> planLinter
            OutputType.SchedulerCommand -> schedulerLinter
            OutputType.RelevancyUpdate -> relevancyLinter
        }
    }

    /**
     * 验证指定类型的内容
     */
    fun validate(outputType: OutputType, content: String) = getLinter(outputType).validate(content)
}

/**
 * 输出类型枚举
 */
sealed class OutputType {
    object EntityExtraction : OutputType()
    object ExecutionPlan : OutputType()
    object SchedulerCommand : OutputType()
    object RelevancyUpdate : OutputType()
}
