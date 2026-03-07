package com.smartsales.core.llm

import com.smartsales.core.llm.LlmProfile

/**
 * 模型注册中心 (Static Hub) — 中央配置
 * 
 * 所有服务根据自己的任务边界，**直接、手动**选择对应的 Profile 即可。严禁在这里写 if/else "Smart Routing" 逻辑。
 * @see Prism-V1.md §Model Registry
 */
object ModelRegistry {
    
    // === Deterministic Pipelines ===
    
    /** 提取器 (Extractor) — 快速解析，实体识别。禁止发散推理。 */
    val EXTRACTOR = LlmProfile(
        modelId = "qwen-turbo",
        temperature = 0.0f
    )
    
    /** 执行器 (Executor) — 严格执行工具调用，输出稳定的结构化 JSON。 */
    val EXECUTOR = LlmProfile(
        modelId = "qwen3-max-2026-01-23",
        temperature = 0.0f,
        skillTags = setOf("structured_output")
    )
    
    // === Generative & Reasoning Pipelines ===
    
    /** 策划者 (Planner) — 大文本理解、策略制定。 */
    val PLANNER = LlmProfile(
        modelId = "qwen-plus",
        temperature = 0.5f,
        skillTags = setOf("reasoning")
    )
    
    /** 教练 (Coach) — 直接对话，生成最终的自然语言话术给用户听。 */
    val COACH = LlmProfile(
        modelId = "qwen-plus",
        temperature = 0.5f,
        skillTags = setOf("sales_coach", "conversational")
    )
    
    /** 视觉 (Vision) — 图像 OCR。 */
    val VISION = LlmProfile(
        modelId = "qwen-vl-plus",
        temperature = 0.5f
    )
    
    // === Batch Processing Pipelines ===
    
    /** 记忆压缩核 (Memory Consolidation) — 深夜/离线 归档冷数据的超长上下文。 */
    val MEMORY_CONSOLIDATION = LlmProfile(
        modelId = "qwen-long",
        temperature = 0.2f
    )
}
