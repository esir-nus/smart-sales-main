package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/SessionTitlePolicy.kt
// 模块：:core:util
// 说明：统一会话标题占位判断、元数据命名与导出文件名规则
// 作者：创建于 2025-12-09

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 标题规则：
 * - 占位标题：新的聊天 / 旧版通话分析前缀，允许一次自动改名。
 * - 正式标题：<主要人物>_<6字摘要>_<MM/dd>。
 * - 导出文件名：<用户名>_<主要人物>_<6字摘要>_<yyyyMMdd_HHmmss>.<ext>
 */
object SessionTitlePolicy {
    const val PLACEHOLDER_TITLE: String = "新的聊天"
    const val LEGACY_TRANSCRIPTION_PREFIX: String = "通话分析"
    private const val FALLBACK_PERSON: String = "未知联系人"
    private const val FALLBACK_SUMMARY: String = "销售咨询"
    private const val FALLBACK_USER: String = "用户"
    private val titleFormatter = SimpleDateFormat("MM/dd", Locale.CHINA)
    private val exportFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)

    data class TitleParts(
        val person: String,
        val summary: String,
        val date: String
    )

    fun newChatPlaceholder(): String = PLACEHOLDER_TITLE

    fun isPlaceholder(title: String?): Boolean {
        val trimmed = title?.trim().orEmpty()
        if (trimmed.isEmpty()) return true
        if (trimmed == PLACEHOLDER_TITLE) return true
        if (trimmed == LEGACY_TRANSCRIPTION_PREFIX) return true
        if (trimmed.startsWith("$LEGACY_TRANSCRIPTION_PREFIX –")) return true
        if (trimmed.startsWith("$LEGACY_TRANSCRIPTION_PREFIX-")) return true
        return false
    }

    fun resolvePerson(raw: String?): String {
        val candidate = raw?.trim().orEmpty()
        return candidate.takeIf { it.isNotBlank() } ?: FALLBACK_PERSON
    }

    fun resolveSummary(raw: String?): String {
        val candidate = raw?.trim().orEmpty()
        return candidate.takeIf { it.isNotBlank() }?.take(6) ?: FALLBACK_SUMMARY
    }

    fun buildTitleParts(
        meta: SessionMetadata?,
        updatedAtMillis: Long
    ): TitleParts? {
        meta ?: return null
        val person = resolvePerson(meta.mainPerson)
        val summary = resolveSummary(meta.summaryTitle6Chars ?: meta.shortSummary)
        val date = titleFormatter.format(Date(updatedAtMillis))
        return TitleParts(person = person, summary = summary, date = date)
    }

    fun buildSuggestedTitle(
        meta: SessionMetadata?,
        updatedAtMillis: Long
    ): String? = buildTitleParts(meta, updatedAtMillis)?.let { parts ->
        "${parts.person}_${parts.summary}_${parts.date}"
    }

    fun buildExportBaseName(
        userName: String?,
        meta: SessionMetadata?
    ): String {
        val now = System.currentTimeMillis()
        val parts = buildTitleParts(meta, meta?.lastUpdatedAt ?: now)
            ?: TitleParts(
                person = FALLBACK_PERSON,
                summary = FALLBACK_SUMMARY,
                date = titleFormatter.format(Date(now))
            )
        val user = userName?.takeIf { it.isNotBlank() } ?: FALLBACK_USER
        val timestamp = exportFormatter.format(Date(now))
        return listOf(user, parts.person, parts.summary, timestamp).joinToString("_")
    }
}
