package com.smartsales.feature.chat.title

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/title/TitleResolver.kt
// 模块：:feature:chat
// 说明：统一会话改名候选解析与决策，支持多来源（GENERAL/SMART/TINGWU）
// 作者：创建于 2025-12-11

import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionTitlePolicy
import com.smartsales.domain.chat.TitleCandidate
import com.smartsales.domain.chat.TitleSource
import com.smartsales.feature.chat.AiSessionSummary

// Extension to check if candidate has content (moved from deleted data class)
private fun TitleCandidate.hasContent(): Boolean = !name.isNullOrBlank() || !title6.isNullOrBlank()

/**
 * 统一处理改名决策：
 * - 手动改名（isTitleUserEdited）后不再自动改名。
 * - 仅在占位标题下应用自动改名。
 * - 当前阶段仅 GENERAL 产生候选，预留 source 以便 SMART/TINGWU 未来接入。
 */
object TitleResolver {

    fun resolveTitle(
        currentSummary: AiSessionSummary?,
        candidate: TitleCandidate?,
        metaFallback: SessionMetadata?
    ): String? {
        if (currentSummary?.isTitleUserEdited == true) return null
        val currentTitle = currentSummary?.title
        if (currentTitle != null && !SessionTitlePolicy.isPlaceholder(currentTitle)) return null

        // 优先使用 Rename 渠道
        val rename = candidate?.takeIf { it.hasContent() }?.let { buildTitleFromCandidate(it) }
        if (!rename.isNullOrBlank()) return rename

        // 回退：沿用元数据改名逻辑
        return SessionTitlePolicy.buildSuggestedTitle(metaFallback, System.currentTimeMillis())
    }

    private fun buildTitleFromCandidate(candidate: TitleCandidate): String? {
        val person = SessionTitlePolicy.resolvePerson(candidate.name) ?: SessionTitlePolicy.fallbackPerson()
        val summary = SessionTitlePolicy.resolveSummary(candidate.title6) ?: SessionTitlePolicy.fallbackSummary()
        return when {
            person != null && summary != null -> "$person - $summary"
            summary != null -> summary
            person != null -> person
            else -> null
        }
    }
}
