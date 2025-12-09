package com.smartsales.feature.chat.home

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/ReasoningStripFormatter.kt
// 模块：:feature:chat
// 说明：基于 SessionMetadata 生成 SMART 分析卡片下方的“分析依据”提示文案
// 作者：创建于 2025-12-09

import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.RiskLevel
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionStage

internal object ReasoningStripFormatter {

    /**
     * 将 SessionMetadata 映射为紧凑的“分析依据”行，仅在 SMART 卡片下展示。
     * 若缺少有效字段则返回 null，UI 将隐藏该条。
     */
    fun build(
        metadata: SessionMetadata?,
        formatStage: (SessionStage) -> String,
        formatRisk: (RiskLevel) -> String,
        formatSource: (AnalysisSource) -> String,
        formatTime: (Long) -> String
    ): String? {
        if (metadata == null) return null
        val hasMeaningful = metadata.stage != null ||
            metadata.riskLevel != null ||
            metadata.tags.isNotEmpty() ||
            metadata.latestMajorAnalysisSource != null ||
            metadata.latestMajorAnalysisAt != null
        if (!hasMeaningful) return null

        val stageLabel = metadata.stage?.let(formatStage) ?: "未标注"
        val riskLabel = metadata.riskLevel?.let(formatRisk) ?: "未标注"
        val tagsLabel = formatTags(metadata.tags)
        val sourceLabel = metadata.latestMajorAnalysisSource?.let(formatSource) ?: "未知来源"
        val timeSuffix = metadata.latestMajorAnalysisAt
            ?.let { formatTime(it) }
            ?.takeIf { it.isNotBlank() }
            ?.let { "（$it）" }
            .orEmpty()

        return buildString {
            append("分析依据：")
            append("阶段 ").append(stageLabel)
            append(" · 风险 ").append(riskLabel)
            append(" · 标签 ").append(tagsLabel)
            append(" · 来源 ").append(sourceLabel).append(timeSuffix)
        }
    }

    private fun formatTags(tags: Set<String>): String {
        val filtered = tags.filter { it.isNotBlank() }
        if (filtered.isEmpty()) return "无标签"
        val limited = filtered.take(3)
        val joined = limited.joinToString("、")
        return if (joined.length > 18) joined.take(18) + "..." else joined
    }
}
