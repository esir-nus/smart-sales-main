package com.smartsales.data.aicore

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/TingwuSmartSummary.kt
// 模块：:data:ai-core
// 说明：表示 Tingwu 智能纪要/摘要结果
// 作者：创建于 2025-11-26
data class TingwuSmartSummary(
    val summary: String? = null,
    val keyPoints: List<String> = emptyList(),
    val actionItems: List<String> = emptyList()
)
