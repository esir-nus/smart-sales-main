package com.smartsales.prism.domain.model

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
    val linkedAudioId: String? = null  // 关联的音频文件ID（用于分析模式）
) {
    /** 显示标题 - 单行格式 */
    val displayTitle: String
        get() = "${clientName}_${summary.take(6)}"
}

