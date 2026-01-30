package com.smartsales.prism.domain.config

import com.smartsales.prism.domain.pipeline.EnhancedContext

/**
 * 模型路由器 — 基于任务类型选择模型
 * 
 * 路由逻辑:
 * - Vision 输入 (图片/视频) → qwen-vl-plus
 * - Tool-calling 任务 → qwen3-max  
 * - Cement 层检索 → qwen-long
 * - Hot 层 / 默认对话 → qwen-plus
 */
object ModelRouter {
    
    /**
     * 根据上下文选择执行模型
     */
    fun forContext(context: EnhancedContext): String = when {
        context.imageAnalysis.isNotEmpty() -> ModelRegistry.VISION
        context.requiresToolCalling() -> ModelRegistry.ANALYST
        else -> ModelRegistry.COACH
    }
    
    /**
     * 根据内存层选择模型
     */
    fun forMemoryLayer(layer: MemoryLayer): String = when (layer) {
        MemoryLayer.RELEVANCY -> ModelRegistry.RELEVANCY   // qwen3-max (entity tool-calling)
        MemoryLayer.HOT -> ModelRegistry.HOT_ZONE           // qwen-plus (index navigation)
        MemoryLayer.CEMENT -> ModelRegistry.CEMENT          // qwen-long (10M context)
    }
}

/**
 * 内存层枚举
 */
enum class MemoryLayer {
    RELEVANCY,  // 结构化实体库
    HOT,        // 14天热区
    CEMENT      // 历史归档
}

/**
 * EnhancedContext 扩展 — 判断是否需要 tool-calling
 */
private fun EnhancedContext.requiresToolCalling(): Boolean {
    // Analyst 模式需要规划和工具调用
    // Scheduler 模式需要解析自然语言为结构化命令
    return modeMetadata.currentMode in listOf(
        com.smartsales.prism.domain.model.Mode.ANALYST,
        com.smartsales.prism.domain.model.Mode.SCHEDULER
    )
}
