// 文件：feature/media/src/main/java/com/smartsales/feature/media/audiofiles/TranscriptionBatchPlanner.kt
// 模块：:feature:media
// 说明：将转写 Markdown 按固定行数切分为伪流式批次
// 作者：创建于 2025-12-22
package com.smartsales.feature.media.audiofiles

data class TranscriptionBatchPlan(
    val ruleLabel: String,
    val batchSize: Int,
    val totalBatches: Int,
    val batches: List<TranscriptionBatchChunk>
)

data class TranscriptionBatchPlanWithWindows(
    val plan: TranscriptionBatchPlan,
    val windows: List<V1TranscriptionBatchWindow>
)

data class TranscriptionBatchChunk(
    val batchIndex: Int,
    val totalBatches: Int,
    val markdownChunk: String,
    val lineCount: Int
)

/**
 * 说明：伪流式切分规则为固定行数，确保每次输入相同 Markdown 得到一致批次。
 */
object TranscriptionBatchPlanner {
    const val RULE_LABEL = "fixed_lines_per_batch"
    private const val DEFAULT_BATCH_SIZE = 20

    fun plan(
        markdown: String,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): TranscriptionBatchPlan {
        val normalized = markdown.replace("\r\n", "\n").trimEnd()
        if (normalized.isBlank()) {
            return TranscriptionBatchPlan(
                ruleLabel = RULE_LABEL,
                batchSize = batchSize.coerceAtLeast(1),
                totalBatches = 0,
                batches = emptyList()
            )
        }
        val lines = normalized.split("\n")
        val effectiveBatchSize = batchSize.coerceAtLeast(1)
        val chunks = lines.chunked(effectiveBatchSize)
        val total = chunks.size
        val batches = chunks.mapIndexed { index, chunk ->
            TranscriptionBatchChunk(
                batchIndex = index + 1,
                totalBatches = total,
                markdownChunk = chunk.joinToString("\n"),
                lineCount = chunk.size
            )
        }
        return TranscriptionBatchPlan(
            ruleLabel = RULE_LABEL,
            batchSize = effectiveBatchSize,
            totalBatches = total,
            batches = batches
        )
    }

    /**
     * 说明：V1 时间窗口仅用于后续 Tingwu anchor/macro-window 过滤，不影响现有行切分行为。
     */
    fun planWithWindows(
        markdown: String,
        audioDurationMs: Long,
        batchDurationMs: Long,
        overlapMs: Long,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): TranscriptionBatchPlanWithWindows {
        // 说明：保持原有批次切分行为不变，仅增加窗口计划用于后续 V1 接入。
        val plan = plan(markdown, batchSize)
        val windows = V1DisectorWindowPlanner(
            batchDurationMs = batchDurationMs,
            overlapMs = overlapMs
        ).plan(audioDurationMs)
        return TranscriptionBatchPlanWithWindows(
            plan = plan,
            windows = windows
        )
    }
}
