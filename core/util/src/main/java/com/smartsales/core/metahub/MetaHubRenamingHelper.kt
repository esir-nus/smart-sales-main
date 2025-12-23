package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/MetaHubRenamingHelper.kt
// 模块：:core:util
// 说明：MetaHub 命名辅助函数，提供 M3 accepted/candidate 写入与有效名解析
// 作者：创建于 2025-12-23

/**
 * 设置候选名称（candidate）。
 * 注意：candidate 不能覆盖 accepted，仅更新候选值与溯源。
 */
suspend fun MetaHub.setM3CandidateName(
    sessionId: String,
    target: RenamingTarget,
    name: String,
    prov: Provenance
) {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return
    val existing = getSession(sessionId) ?: SessionMetadata(sessionId = sessionId)
    val updatedRenaming = when (target) {
        RenamingTarget.SESSION_TITLE -> existing.renaming.copy(
            sessionTitle = existing.renaming.sessionTitle.copy(
                candidate = trimmed,
                candidateProv = prov
            )
        )
        RenamingTarget.EXPORT_TITLE -> existing.renaming.copy(
            exportTitle = existing.renaming.exportTitle.copy(
                candidate = trimmed,
                candidateProv = prov
            )
        )
    }
    val updated = existing.copy(renaming = updatedRenaming)
    upsertSession(updated)
}

/**
 * 设置用户确认名称（accepted）。
 * 规则：accepted 优先级最高；用户确认后不可被 candidate 覆盖。
 */
suspend fun MetaHub.setM3AcceptedName(
    sessionId: String,
    target: RenamingTarget,
    name: String,
    prov: Provenance
) {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return
    val existing = getSession(sessionId) ?: SessionMetadata(sessionId = sessionId)
    val updatedRenaming = when (target) {
        RenamingTarget.SESSION_TITLE -> existing.renaming.copy(
            sessionTitle = existing.renaming.sessionTitle.copy(accepted = trimmed),
            userRenamedAt = prov.updatedAt
        )
        RenamingTarget.EXPORT_TITLE -> existing.renaming.copy(
            exportTitle = existing.renaming.exportTitle.copy(accepted = trimmed),
            exportTitleUserEditedAt = prov.updatedAt
        )
    }
    val updated = existing.copy(renaming = updatedRenaming)
    upsertSession(updated)
}

/**
 * 读取有效名称：accepted > candidate > fallback。
 */
suspend fun MetaHub.getM3EffectiveName(
    sessionId: String,
    target: RenamingTarget,
    fallback: String? = null
): String? {
    val renaming = getSession(sessionId)?.renaming ?: RenamingMetadata()
    val effective = when (target) {
        RenamingTarget.SESSION_TITLE -> renaming.effectiveSessionTitle()
        RenamingTarget.EXPORT_TITLE -> renaming.effectiveExportTitle()
    }
    return effective ?: fallback?.trim()?.takeIf { it.isNotBlank() }
}
