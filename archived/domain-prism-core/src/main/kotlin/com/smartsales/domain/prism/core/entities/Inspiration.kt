package com.smartsales.domain.prism.core.entities

/**
 * 灵感卡片
 * @see Prism-V1.md §5.7
 */
data class Inspiration(
    val id: String,
    val content: String,
    val source: InspirationSource = InspirationSource.VOICE,
    val relatedEntityIds: List<String> = emptyList(),
    val isPromoted: Boolean = false,  // 是否已转为任务
    val promotedTaskId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class InspirationSource {
    VOICE,      // 语音记录
    MANUAL,     // 手动输入
    AI_SUGGEST  // AI 建议
}
