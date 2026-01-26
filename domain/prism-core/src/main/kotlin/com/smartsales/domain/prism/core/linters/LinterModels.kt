package com.smartsales.domain.prism.core.linters

import com.smartsales.domain.prism.core.entities.EntityType

/**
 * LLM 提取的实体结构
 * @see Prism-V1.md §5.6
 */
data class ExtractedEntity(
    val type: EntityType,
    val extractedId: String?,       // LLM 猜测的 ID
    val extractedName: String,
    val context: String?,           // 提取上下文
    val resolvedEntityId: String?,  // 消歧后的真实 ID
    val confidence: Float = 0.0f
)

/**
 * 日程命令结构
 */
data class SchedulerCommand(
    val action: SchedulerAction,
    val title: String,
    val scheduledAt: Long?,
    val priority: String? = null,
    val description: String? = null
)

enum class SchedulerAction {
    CREATE,
    UPDATE,
    DELETE,
    REMIND
}
