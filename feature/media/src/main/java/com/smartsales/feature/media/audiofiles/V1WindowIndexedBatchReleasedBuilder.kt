package com.smartsales.feature.media.audiofiles

// 说明：按 V1 窗口顺序生成 BatchReleased，避免行分批索引干扰窗口语义。
object V1WindowIndexedBatchReleasedBuilder {
    fun build(
        jobId: String,
        windows: List<V1TranscriptionBatchWindow>,
        timedSegments: List<V1TimedTextSegment>,
        transcriptMarkdown: String,
        v1BatchPlanRule: String,
        v1BatchDurationMs: Long,
        v1OverlapMs: Long
    ): List<AudioTranscriptionBatchEvent.BatchReleased> {
        if (windows.isEmpty()) return emptyList()
        val sortedWindows = windows.sortedBy { it.batchIndex }
        val totalBatches = sortedWindows.size
        return sortedWindows.mapIndexed { index, window ->
            // 说明：batchIndex 以窗口为准（1-based），批次总数等于窗口数量。
            val batchIndex = window.batchIndex
            val markdownChunk = if (index == 0) {
                // 说明：仅用于回滚兜底（过滤关闭时不至于空白），不用于 V1 过滤逻辑。
                transcriptMarkdown
            } else {
                ""
            }
            AudioTranscriptionBatchEvent.BatchReleased(
                jobId = jobId,
                batchIndex = batchIndex,
                totalBatches = totalBatches,
                markdownChunk = markdownChunk,
                isFinal = batchIndex == totalBatches,
                batchSize = 0,
                lineCount = 0,
                ruleLabel = v1BatchPlanRule,
                v1Window = window,
                // 说明：timedSegments 只透传引用，避免重复复制占用内存。
                timedSegments = timedSegments,
                v1BatchPlanRule = v1BatchPlanRule,
                v1BatchDurationMs = v1BatchDurationMs,
                v1OverlapMs = v1OverlapMs,
                v1TotalBatches = totalBatches,
                v1CurrentBatchIndex = batchIndex
            )
        }
    }
}
