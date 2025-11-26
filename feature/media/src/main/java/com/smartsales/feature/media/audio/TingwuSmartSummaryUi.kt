package com.smartsales.feature.media.audio

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audio/TingwuSmartSummaryUi.kt
// 模块：:feature:media
// 说明：音频模块 UI 使用的听悟智能总结模型
// 作者：创建于 2025-11-26
data class TingwuSmartSummaryUi(
    val summary: String? = null,
    val keyPoints: List<String> = emptyList(),
    val actionItems: List<String> = emptyList()
)
