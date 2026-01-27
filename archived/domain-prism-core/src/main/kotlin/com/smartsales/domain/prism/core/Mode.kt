package com.smartsales.domain.prism.core

/**
 * 运行模式枚举 — 决定 Pipeline 的策略选择
 * @see Prism-V1.md §2.2 #2
 */
enum class Mode {
    COACH,      // 轻量对话，快速响应
    ANALYST,    // 深度分析，Planner驱动
    SCHEDULER   // 结构化输出，日程管理
}
