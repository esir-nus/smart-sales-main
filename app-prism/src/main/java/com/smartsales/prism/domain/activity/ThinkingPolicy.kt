package com.smartsales.prism.domain.activity

import com.smartsales.prism.domain.model.Mode

/**
 * 思考痕迹显示策略 — 根据模式控制 UI 截断
 * 
 * 所有模型均返回完整思考痕迹，此策略仅控制 UI 显示行数。
 * 
 * @see Prism-V1.md §4.6.3
 */
object ThinkingPolicy {
    
    /**
     * 获取指定模式下的最大显示行数
     */
    fun maxTraceLines(mode: Mode): Int = when (mode) {
        Mode.COACH -> 3       // 快速截断 — "感觉被理解"
        Mode.ANALYST -> 20    // 完整痕迹 — 透明度优先
        Mode.SCHEDULER -> 5   // 适度显示
    }
    
    /**
     * 是否自动折叠思考痕迹
     * 
     * Analyst 模式保持展开直到完成，让用户看到完整的思考过程。
     */
    fun shouldAutoCollapse(mode: Mode): Boolean = when (mode) {
        Mode.COACH -> true       // 快速折叠 — 不打扰用户
        Mode.ANALYST -> false    // 保持展开 — 透明度优先
        Mode.SCHEDULER -> true   // 适度折叠
    }
    
    /**
     * 截断思考痕迹
     */
    fun truncate(trace: List<String>, mode: Mode): List<String> {
        val max = maxTraceLines(mode)
        return if (trace.size <= max) trace else trace.take(max)
    }
}
