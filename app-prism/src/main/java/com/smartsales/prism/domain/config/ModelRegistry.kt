package com.smartsales.prism.domain.config

/**
 * 模型注册表 — 中央配置
 * 
 * 所有 LLM 模型选择都从这里引用，确保与 Prism-V1.md Model Registry 对齐。
 * @see implementation_plan.md §Model Registry
 */
object ModelRegistry {
    
    // === Mode Models (Executor Strategy) ===
    
    /** Coach 模式 — 快速对话，1M tokens */
    const val COACH = "qwen-plus"
    
    /** Analyst 模式 — 推理、工具调用，32k tokens */
    const val ANALYST = "qwen3-max"
    
    /** Scheduler 模式 — 结构化输出 (使用与 Analyst 相同的模型) */
    const val SCHEDULER = "qwen3-max"
    
    /** Vision 模式 — 图像 OCR/描述 */
    const val VISION = "qwen3-vl-plus"
    
    // === Memory Layer Models ===
    
    /** Relevancy Library — 工具调用查询结构化实体，32k tokens */
    const val RELEVANCY = "qwen3-max"
    
    /** Hot Zone — 快速读取近期上下文，1M tokens */
    const val HOT_ZONE = "qwen-plus"
    
    /** Cement Zone — 深层历史检索，10M tokens */
    const val CEMENT = "qwen-long"
    
    // === Batch Processing Models ===
    
    /** Memory Writer (Hot/Cold/Cement 整合) — 10M tokens 长上下文 */
    const val MEMORY_CONSOLIDATION = "qwen-long"
}
