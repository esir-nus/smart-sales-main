package com.smartsales.prism.domain.model

/**
 * Prism 运行模式
 * @see Prism-V1.md §2.1
 */
enum class Mode {
    COACH,      // 销售教练模式
    ANALYST,    // 数据分析模式
    SCHEDULER   // 日程规划模式
}
