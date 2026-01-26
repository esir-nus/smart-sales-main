package com.smartsales.domain.prism.core.payloads

/**
 * Rethink UI 冲突卡片
 * @see Prism-V1.md §5.5
 */
data class ConflictCard(
    val id: String,
    val conflictDescription: String,
    val option1: ConflictOption,
    val option2: ConflictOption,
    val contextSnippet: String,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val userChoice: Int? = null  // 1 or 2
)

/**
 * 冲突选项
 */
data class ConflictOption(
    val label: String,
    val description: String,
    val entityId: String?,
    val confidence: Float = 0.0f
)
