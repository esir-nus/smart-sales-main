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
    val sessionId: String? = null,
    val speakerMap: Map<String, SpeakerMeta> = emptyMap(),
    val source: TranscriptSource = TranscriptSource.UNKNOWN,
    val createdAt: Long = System.currentTimeMillis(),
    val diarizedSegmentsCount: Int? = null,
    val mainPerson: String? = null,
    val shortSummary: String? = null,
    val summaryTitle6Chars: String? = null,
    val location: String? = null,
    val stage: SessionStage? = null,
    val riskLevel: RiskLevel? = null,

    val chapters: List<ChapterMeta> = emptyList(),
    val keyPoints: List<KeyPointMeta> = emptyList(),
    val extra: Map<String, Any?> = emptyMap()
) {
    /**
     * 非空合并：speakerMap 以高置信度/非空覆盖，其他字段仅用新值覆盖旧值。
     */
    fun mergeWith(other: TranscriptMetadata): TranscriptMetadata = TranscriptMetadata(
        transcriptId = transcriptId,
        sessionId = other.sessionId ?: sessionId,
        speakerMap = mergeSpeakers(speakerMap, other.speakerMap),
        source = other.source.takeIf { it != TranscriptSource.UNKNOWN } ?: source,
        createdAt = maxOf(createdAt, other.createdAt),
        diarizedSegmentsCount = other.diarizedSegmentsCount ?: diarizedSegmentsCount,
        mainPerson = other.mainPerson ?: mainPerson,
        shortSummary = other.shortSummary ?: shortSummary,
        summaryTitle6Chars = other.summaryTitle6Chars ?: summaryTitle6Chars,
        location = other.location ?: location,

        stage = other.stage ?: stage,
        riskLevel = other.riskLevel ?: riskLevel,
        // List fields: replace if new list is not empty, otherwise keep old
        chapters = if (other.chapters.isNotEmpty()) other.chapters else chapters,
        keyPoints = if (other.keyPoints.isNotEmpty()) other.keyPoints else keyPoints,
        extra = extra + other.extra
    )

    private fun mergeSpeakers(
        current: Map<String, SpeakerMeta>,
        incoming: Map<String, SpeakerMeta>
    ): Map<String, SpeakerMeta> {
        if (incoming.isEmpty()) return current
        val mutable = current.toMutableMap()
        incoming.forEach { (id, meta) ->
            val existing = mutable[id]
            val merged = if (existing == null) {
                // 新增 speaker：直接采用 incoming 的字段
                meta
            } else {
                // 已有 speaker：非空字段覆盖
                SpeakerMeta(
                    displayName = meta.displayName ?: existing.displayName,
                    role = meta.role ?: existing.role,
                    confidence = meta.confidence ?: existing.confidence
                )
            }
            // ✅ 统一在这里 clamp，保证所有 speaker 的 confidence ∈ [0, 1]
            mutable[id] = merged.copy(
                confidence = merged.confidence?.coerceIn(0f, 1f)
            )
        }
        return mutable
    }
}

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
    TINGWU_LLM,
    UNKNOWN
}

/**
 * M2B Chapter structure (M1 Deferred implementation)
 */
data class ChapterMeta(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val summary: String? = null
)

/**
 * M2B KeyPoint structure (M1 Deferred implementation)
 */
data class KeyPointMeta(
    val text: String,
    val timeRange: TimeRange? = null
)

data class TimeRange(
    val startMs: Long,
    val endMs: Long
)
