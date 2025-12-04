package com.smartsales.feature.chat.home.orchestrator

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/OrchestratorInput.kt
// 模块：:feature:chat
// 说明：Orchestrator 输入模型，占位后续扩展元数据编排
// 作者：创建于 2025-12-04

/**
 * Home Orchestrator 的输入，仅存结构化参数，当前未实际使用。
 */
data class OrchestratorInput(
    val sessionId: String,
    val mode: OrchestratorMode = OrchestratorMode.GENERAL_CHAT,
    val userText: String? = null,
    val quickSkillId: String? = null,
    val extra: Map<String, Any?> = emptyMap()
)
