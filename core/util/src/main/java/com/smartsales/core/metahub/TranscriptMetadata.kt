package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/TranscriptMetadata.kt
// 模块：:core:util
// 说明：定义转写相关元数据，保持说话人映射等结构化信息
// 作者：创建于 2025-12-04

/**
 * 转写元数据，仅存说话人和来源等结构化信息，不含完整逐字稿。
 */
data class TranscriptMetadata(
    val transcriptId: String,
    val sessionId: String?,
    val speakerMap: Map<String, SpeakerMeta>,
    val source: TranscriptSource,
    val createdAt: Long
)

/**
 * 说话人元数据，描述展示名和角色。
 */
data class SpeakerMeta(
    val displayName: String?,
    val role: SpeakerRole?,
    val confidence: Float?
)

enum class SpeakerRole {
    CUSTOMER,
    SALES,
    OTHER,
    UNKNOWN
}

enum class TranscriptSource {
    TINGWU,
    UNKNOWN
}
