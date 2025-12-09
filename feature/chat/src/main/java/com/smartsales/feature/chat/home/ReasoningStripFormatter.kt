package com.smartsales.feature.chat.home

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/ReasoningStripFormatter.kt
// 模块：:feature:chat
// 说明：基于 SessionMetadata 生成 SMART 分析卡片下方的“分析依据”提示文案
// 作者：创建于 2025-12-09

import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionMetadataLabelProvider

internal object ReasoningStripFormatter {

    /**
     * 将 SessionMetadata 映射为紧凑的“分析依据”行，仅在 SMART 卡片下展示。
     * 若缺少有效字段则返回 null，UI 将隐藏该条。
     */
    fun build(
        metadata: SessionMetadata?,
        labelProvider: SessionMetadataLabelProvider = SessionMetadataLabelProvider
    ): String? {
        val labels = labelProvider.labelsFrom(metadata) ?: return null
        val tagsLabel = labels.tagsLabel
        val timeSuffix = labels.timeLabel
            .takeIf { it.isNotBlank() }
            ?.let { "（$it）" }
            .orEmpty()

        return buildString {
            append("分析依据：")
            append("阶段 ").append(labels.stageLabel)
            append(" · 风险 ").append(labels.riskLabel)
            append(" · 标签 ").append(tagsLabel)
            append(" · 来源 ").append(labels.sourceLabel).append(timeSuffix)
        }
    }
}
