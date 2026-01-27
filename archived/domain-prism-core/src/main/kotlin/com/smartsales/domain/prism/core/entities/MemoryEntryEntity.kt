package com.smartsales.domain.prism.core.entities

import com.smartsales.domain.prism.core.Mode

/**
 * 统一内存条目 — 所有工作流共享的 Room 实体
 * @see Prism-V1.md §5.7
 */
data class MemoryEntryEntity(
    val id: String,
    val workflow: Mode,
    val title: String,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val sessionId: String,
    val outcomeStatus: OutcomeStatus? = null,
    val displayContent: String,
    val structuredJson: String? = null,
    val entityIds: List<String> = emptyList(),
    val artifacts: List<ArtifactMeta> = emptyList(),
    val payloadJson: String? = null  // 工作流特定载荷
)

enum class OutcomeStatus {
    ONGOING,
    SUCCESS,
    PARTIAL,
    FAILED
}

/**
 * 文件引用元数据
 */
data class ArtifactMeta(
    val artifactId: String,
    val type: ArtifactType,
    val path: String,
    val mimeType: String,
    val sizeBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ArtifactType {
    AUDIO,
    IMAGE,
    DOCUMENT,
    CHART,
    OTHER
}
