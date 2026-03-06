package com.smartsales.prism.domain.memory

/**
 * 记忆条目实体 — Active/Archived Zone 存储单元
 * @see Memory Center spec §Two-Zone Model
 */
data class MemoryEntry(
    val entryId: String,
    val sessionId: String,
    val content: String,
    val entryType: MemoryEntryType,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false,
    val scheduledAt: Long? = null,
    val structuredJson: String? = null,
    val workflow: String? = null,        // COACH, ANALYST, SCHEDULER
    val title: String? = null,           // 记忆标题
    val completedAt: Long? = null,       // 完成时间戳
    val outcomeStatus: String? = null,   // ONGOING, SUCCESS, PARTIAL, FAILED
    val outcomeJson: String? = null,     // 结果详情 JSON
    val payloadJson: String? = null,     // WorkflowPayload（模式相关数据）
    val displayContent: String? = null,  // UI 展示用文本（格式化后）
    val artifactsJson: String? = null    // 生成物引用 (file refs, reports)
)

/**
 * 记忆条目类型
 */
enum class MemoryEntryType {
    USER_MESSAGE,
    ASSISTANT_RESPONSE,
    TASK_RECORD,
    SCHEDULE_ITEM,
    INSPIRATION
}

