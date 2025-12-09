package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/SessionMetadataLabelProvider.kt
// 模块：:core:util
// 说明：统一格式化 SessionMetadata 的阶段/风险/标签/来源/时间文案，供 HUD、导出与分析依据复用
// 作者：创建于 2025-12-09

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 统一后的标签集合，减少各端自行拼接时的分歧。
 */
data class MetadataLabels(
    val stageLabel: String,
    val riskLabel: String,
    val tagsLabel: String,
    val sourceLabel: String,
    val timeLabel: String
)

/**
 * 将 SessionMetadata 字段映射为展示友好的标签，避免 HUD/导出/分析依据各自维护一套文案。
 */
object SessionMetadataLabelProvider {

    private val timeFormatter by lazy {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    }

    /**
     * 返回完整标签集合；若元数据为空或不含任何关键字段则返回 null。
     */
    fun labelsFrom(metadata: SessionMetadata?): MetadataLabels? {
        if (metadata == null) return null
        val hasMeaningful = metadata.stage != null ||
            metadata.riskLevel != null ||
            metadata.tags.isNotEmpty() ||
            metadata.latestMajorAnalysisSource != null ||
            metadata.latestMajorAnalysisAt != null
        if (!hasMeaningful) return null
        return MetadataLabels(
            stageLabel = stageLabel(metadata.stage),
            riskLabel = riskLabel(metadata.riskLevel),
            tagsLabel = tagsLabel(metadata.tags),
            sourceLabel = sourceLabel(metadata.latestMajorAnalysisSource),
            timeLabel = timeLabel(metadata.latestMajorAnalysisAt)
        )
    }

    fun stageLabel(stage: SessionStage?): String = when (stage) {
        SessionStage.DISCOVERY -> "探索阶段"
        SessionStage.NEGOTIATION -> "谈判阶段"
        SessionStage.PROPOSAL -> "方案阶段"
        SessionStage.CLOSING -> "成交推进"
        SessionStage.POST_SALE -> "售后阶段"
        SessionStage.UNKNOWN -> "未知阶段"
        null -> "未标注"
    }

    fun riskLabel(risk: RiskLevel?): String = when (risk) {
        RiskLevel.LOW -> "低风险"
        RiskLevel.MEDIUM -> "中风险"
        RiskLevel.HIGH -> "高风险"
        RiskLevel.UNKNOWN -> "风险未知"
        null -> "未标注"
    }

    /**
     * 统一标签串格式，默认取前三个、用顿号分隔并在超长时截断。
     */
    fun tagsLabel(
        tags: Set<String>?,
        limit: Int = 3,
        delimiter: String = "、",
        maxLength: Int = 18,
        sort: Boolean = false
    ): String {
        val filtered = tags.orEmpty().filter { it.isNotBlank() }
        val normalized = (if (sort) filtered.sorted() else filtered).distinct()
        if (normalized.isEmpty()) return "无标签"
        val limited = normalized.take(limit)
        val joined = limited.joinToString(delimiter)
        if (joined.length <= maxLength) return joined
        return joined.take(maxLength) + "..."
    }

    fun sourceLabel(source: AnalysisSource?): String = when (source) {
        AnalysisSource.SMART_ANALYSIS_USER,
        AnalysisSource.SMART_ANALYSIS_AUTO -> "来自智能分析"
        AnalysisSource.GENERAL_FIRST_REPLY -> "来自首次回复"
        AnalysisSource.TINGWU -> "来自通话转写"
        null -> "未知来源"
    }

    fun timeLabel(timestampMillis: Long?): String {
        val millis = timestampMillis ?: return ""
        return synchronized(timeFormatter) {
            timeFormatter.format(Date(millis))
        }
    }
}
