package com.smartsales.feature.media.audio

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audio/TingwuChapterUi.kt
// 模块：:feature:media
// 说明：AudioFiles 层使用的章节模型
// 作者：创建于 2025-11-26
data class TingwuChapterUi(
    val title: String,
    val startMs: Long,
    val endMs: Long? = null
)
