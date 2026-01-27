package com.smartsales.domain.prism.core

/**
 * 规划器 — Analyst 模式专用，生成可见的执行计划
 * @see Prism-V1.md §4.5, §4.6
 */
interface Planner {
    /**
     * 生成执行计划，用于 Plan Card 展示
     */
    suspend fun generatePlan(context: EnhancedContext): ExecutionPlan
}
