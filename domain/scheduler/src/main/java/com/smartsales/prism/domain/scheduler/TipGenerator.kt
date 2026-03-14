package com.smartsales.prism.domain.scheduler

/**
 * 智能提示生成器 — 为任务生成上下文相关的行动提示
 * 
 * Wave 9: Smart Tips
 * 来源: docs/cerb/scheduler/spec.md L439-508
 */
interface TipGenerator {
    /**
     * 为给定任务生成 2-5 条智能提示
     * 
     * @param task 任务模型（必须包含 keyPersonEntityId）
     * @return 提示列表；如果无法生成或不适用则返回空列表
     */
    suspend fun generate(task: ScheduledTask): List<String>
}
