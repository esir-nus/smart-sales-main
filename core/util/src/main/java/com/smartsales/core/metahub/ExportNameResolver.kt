package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/ExportNameResolver.kt
// 模块：:core:util
// 说明：导出命名解析（accepted > candidate > fallback）
// 作者：创建于 2025-12-22

/**
 * 导出命名来源，用于调试与可观测。
 */
enum class ExportNameSource {
    ACCEPTED,
    CANDIDATE,
    FALLBACK
}

/**
 * 导出命名解析结果：baseName 不包含扩展名。
 */
data class ExportNameResolution(
    val baseName: String,
    val source: ExportNameSource
)

/**
 * 导出命名解析器：
 * - accepted：用户手动改名
 * - candidate：非手动标题或元数据派生标题
 * - fallback：兜底 sessionId
 */
object ExportNameResolver {

    fun resolve(
        sessionId: String,
        sessionTitle: String?,
        isTitleUserEdited: Boolean?,
        meta: SessionMetadata?,
        nowMillis: Long = System.currentTimeMillis()
    ): ExportNameResolution {
        val normalizedTitle = sessionTitle
            ?.trim()
            ?.takeIf { it.isNotBlank() && !SessionTitlePolicy.isPlaceholder(it) }
        if (isTitleUserEdited == true && !normalizedTitle.isNullOrBlank()) {
            return ExportNameResolution(
                baseName = sanitizeBaseName(normalizedTitle),
                source = ExportNameSource.ACCEPTED
            )
        }
        if (!normalizedTitle.isNullOrBlank()) {
            return ExportNameResolution(
                baseName = sanitizeBaseName(normalizedTitle),
                source = ExportNameSource.CANDIDATE
            )
        }
        // 重要：无有效标题时退回元数据候选，确保稳定且可解释。
        val metaTitle = SessionTitlePolicy.buildSuggestedTitle(meta, nowMillis)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        if (!metaTitle.isNullOrBlank()) {
            return ExportNameResolution(
                baseName = sanitizeBaseName(metaTitle),
                source = ExportNameSource.CANDIDATE
            )
        }
        val fallback = buildFallbackName(sessionId)
        return ExportNameResolution(
            baseName = sanitizeBaseName(fallback),
            source = ExportNameSource.FALLBACK
        )
    }

    private fun buildFallbackName(sessionId: String): String {
        val safeId = sessionId.trim().ifBlank { "session" }
        return "session_$safeId"
    }

    /**
     * 清理文件名中不安全字符，保证导出文件稳定落盘。
     */
    private fun sanitizeBaseName(name: String): String {
        return name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(120)
            .ifBlank { "smart-sales-export" }
    }
}
