package com.smartsales.domain.prism.core.payloads

/**
 * Analyst 模式章节
 * @see Prism-V1.md §5.7
 */
data class AnalysisChapter(
    val id: String,
    val title: String,
    val content: String,
    val order: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Analyst 模式载荷 — 存储在 MemoryEntryEntity.payloadJson
 */
data class AnalystPayload(
    val chapters: List<AnalysisChapter>,
    val keyInsights: List<String> = emptyList(),
    val isComplete: Boolean = false
)
