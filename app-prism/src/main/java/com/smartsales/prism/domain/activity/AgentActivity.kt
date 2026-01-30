package com.smartsales.prism.domain.activity

/**
 * 活动阶段 — 高层级任务状态（始终可见）
 * 
 * @see Prism-V1.md §4.6.2
 */
enum class ActivityPhase {
    PLANNING,      // 📝 规划分析步骤
    EXECUTING,     // ⚙️ 执行工具
    RESPONDING,    // 💬 生成回复
    ERROR          // ⚠️ 发生错误
}

/**
 * 活动动作 — 具体操作（可选）
 */
enum class ActivityAction {
    THINKING,      // 🧠 思考中... (Qwen CoT)
    PARSING,       // 📄 解析中... (Qwen-VL)
    TRANSCRIBING,  // 🎙️ 转写中... (Tingwu)
    RETRIEVING,    // 📚 检索记忆... (Relevancy)
    ASSEMBLING,    // 📋 整理上下文...
    STREAMING      // ✨ 生成中...
}

/**
 * 代理活动状态 — 用于 AgentActivityBanner 显示
 * 
 * @param phase 当前阶段（必需，始终显示）
 * @param action 当前动作（可选，显示为副标题）
 * @param trace 思考痕迹（可选，显示为可折叠内容）
 */
data class AgentActivity(
    val phase: ActivityPhase,
    val action: ActivityAction? = null,
    val trace: List<String> = emptyList()
)
