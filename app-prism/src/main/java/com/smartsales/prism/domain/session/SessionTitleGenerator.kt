package com.smartsales.prism.domain.session

import com.smartsales.prism.domain.pipeline.ChatTurn

/**
 * 会话标题生成器 — 根据对话内容自动提取客户名和摘要
 * 
 * @see docs/cerb/session-history/spec.md Wave 4
 */
interface SessionTitleGenerator {
    /**
     * 生成会话标题
     * @param history 会话历史
     * @return 提取结果
     */
    suspend fun generateTitle(history: List<ChatTurn>): TitleResult
}

/**
 * 标题生成结果
 */
data class TitleResult(
    val clientName: String, // 客户名 (或 "未知客户")
    val summary: String     // 6字摘要
)
