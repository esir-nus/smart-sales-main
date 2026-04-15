package com.smartsales.prism.domain.model

enum class SessionKind {
    GENERAL,
    AUDIO_GROUNDED,
    SCHEDULER_FOLLOW_UP
}

data class SchedulerFollowUpTaskSummary(
    val taskId: String,
    val title: String,
    val dayOffset: Int,
    val scheduledAtMillis: Long,
    val durationMinutes: Int
)

data class SchedulerFollowUpContext(
    val sourceBadgeThreadId: String,
    val boundTaskIds: List<String>,
    val batchId: String? = null,
    val taskSummaries: List<SchedulerFollowUpTaskSummary> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 会话预览 — 用于 History Drawer 展示
 * 格式: [ClientName/Title]_[Summary (max 6 chars)]
 */
data class SessionPreview(
    val id: String,
    val clientName: String,
    val summary: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val linkedAudioId: String? = null,  // 关联的音频文件ID（用于分析模式）
    val hasAudioContextHistory: Boolean = false,
    val sessionKind: SessionKind = SessionKind.GENERAL,
    val schedulerFollowUpContext: SchedulerFollowUpContext? = null
) {
    /** 显示标题 - 单行格式 */
    val displayTitle: String
        get() = "${clientName}_${summary.take(6)}"
}
