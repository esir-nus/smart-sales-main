package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/RenamingMetadata.kt
// 模块：:core:util
// 说明：V7 会话命名元数据（accepted/candidate）与来源信息
// 作者：创建于 2025-12-23

/**
 * 证据引用：与 schema 保持一致，便于后续扩展。
 */
data class EvidenceRef(
    val kind: String,
    val value: String
)

/**
 * 溯源信息：与 schema 对齐（source/updatedAt 为必填）。
 */
data class Provenance(
    val source: String,
    val updatedAt: Long,
    val confidence: Double? = null,
    val evidenceRefs: List<EvidenceRef> = emptyList()
)

/**
 * accepted/candidate 模型：
 * - accepted：用户确认值（不可被 candidate 覆盖）
 * - candidate：候选值（可被更新，但不覆盖 accepted）
 */
data class AcceptedAndCandidate(
    val accepted: String? = null,
    val candidate: String? = null,
    val candidateProv: Provenance? = null
) {
    /**
     * 解析有效名称：accepted > candidate。
     */
    fun effectiveName(): String? {
        val acceptedValue = accepted?.trim().orEmpty()
        if (acceptedValue.isNotBlank()) return acceptedValue
        val candidateValue = candidate?.trim().orEmpty()
        return candidateValue.takeIf { it.isNotBlank() }
    }
}

/**
 * 命名字段类型：区分会话标题与导出标题。
 */
enum class RenamingTarget {
    SESSION_TITLE,
    EXPORT_TITLE
}

/**
 * V7 RenamingMetadata：
 * - sessionTitle/exportTitle 走 accepted/candidate
 * - 时间字段用于记录用户改名与自动命名状态
 */
data class RenamingMetadata(
    val sessionTitle: AcceptedAndCandidate = AcceptedAndCandidate(),
    val exportTitle: AcceptedAndCandidate = AcceptedAndCandidate(),
    val autoSessionRenameAppliedAt: Long? = null,
    val autoExportRenameAppliedAt: Long? = null,
    val userRenamedAt: Long? = null,
    val exportTitleUserEditedAt: Long? = null
) {
    /**
     * 合并规则：
     * - accepted 始终优先；candidate 不能覆盖 accepted
     * - 时间字段取最新的非空值
     */
    fun mergeWith(other: RenamingMetadata): RenamingMetadata = RenamingMetadata(
        sessionTitle = mergeAcceptedAndCandidate(sessionTitle, other.sessionTitle),
        exportTitle = mergeAcceptedAndCandidate(exportTitle, other.exportTitle),
        autoSessionRenameAppliedAt = other.autoSessionRenameAppliedAt ?: autoSessionRenameAppliedAt,
        autoExportRenameAppliedAt = other.autoExportRenameAppliedAt ?: autoExportRenameAppliedAt,
        userRenamedAt = other.userRenamedAt ?: userRenamedAt,
        exportTitleUserEditedAt = other.exportTitleUserEditedAt ?: exportTitleUserEditedAt
    )

    /**
     * 会话标题有效值：accepted > candidate。
     */
    fun effectiveSessionTitle(): String? = sessionTitle.effectiveName()

    /**
     * 导出标题有效值：accepted > candidate。
     */
    fun effectiveExportTitle(): String? = exportTitle.effectiveName()

    private fun mergeAcceptedAndCandidate(
        current: AcceptedAndCandidate,
        incoming: AcceptedAndCandidate
    ): AcceptedAndCandidate {
        // 关键规则：accepted 只有被新的 accepted 覆盖，candidate 更新不得覆盖 accepted
        val accepted = incoming.accepted ?: current.accepted
        val candidate = incoming.candidate ?: current.candidate
        val candidateProv = incoming.candidateProv ?: current.candidateProv
        return AcceptedAndCandidate(
            accepted = accepted,
            candidate = candidate,
            candidateProv = candidateProv
        )
    }
}
