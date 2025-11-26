package com.smartsales.data.aicore

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/TingwuChapter.kt
// 模块：:data:ai-core
// 说明：表示 Tingwu 章节速览的章节信息
// 作者：创建于 2025-11-26
data class TingwuChapter(
    val title: String,
    val startMs: Long,
    val endMs: Long? = null
)
