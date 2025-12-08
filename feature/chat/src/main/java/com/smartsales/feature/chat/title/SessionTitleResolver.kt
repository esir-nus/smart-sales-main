package com.smartsales.feature.chat.title

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/title/SessionTitleResolver.kt
// 模块：:feature:chat
// 说明：结合 MetaHub 元数据与启发式生成会话标题
// 作者：创建于 2025-12-04

import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionTitlePolicy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionTitleResolver @Inject constructor(
    private val metaHub: MetaHub
) {
    /**
     * 优先使用 MetaHub 元数据生成标题，缺失时回退到旧启发式。
     */
    suspend fun resolveTitle(
        sessionId: String,
        updatedAtMillis: Long,
        firstUserMessage: String,
        firstAssistantMessage: String?
    ): String {
        val meta = metaHub.getSession(sessionId)
        SessionTitlePolicy.buildSuggestedTitle(meta, updatedAtMillis)?.let { return it }
        return SessionTitleGenerator.deriveSessionTitle(
            updatedAtMillis = updatedAtMillis,
            firstUserMessage = firstUserMessage,
            firstAssistantMessage = firstAssistantMessage
        )
    }
}
