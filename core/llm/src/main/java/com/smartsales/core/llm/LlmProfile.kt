package com.smartsales.core.llm

/**
 * LLM 策略模版 — 配置单个任务的执行参数
 */
data class LlmProfile(
    val modelId: String,
    val temperature: Float = 0.5f,
    val skillTags: Set<String> = emptySet()
)
