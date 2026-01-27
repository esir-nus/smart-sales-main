package com.smartsales.domain.prism.core

/**
 * 执行器 — LLM 推理引擎，根据模式选择不同策略
 * @see Prism-V1.md §2.2 #3
 */
interface Executor {
    /**
     * 执行 LLM 推理，返回展示内容和结构化 JSON
     */
    suspend fun execute(context: EnhancedContext): ExecutorResult
}
