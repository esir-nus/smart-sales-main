// File: feature/media/src/main/java/com/smartsales/feature/media/audiofiles/V1DisectorWindowPlanner.kt
// Description: V1 deterministic batch window planner (2025-12-30)
package com.smartsales.feature.media.audiofiles

import kotlin.math.min

data class V1TranscriptionBatchWindow(
    val batchIndex: Int,
    val absStartMs: Long,
    val absEndMs: Long,
    val overlapMs: Long,
    val captureStartMs: Long
)

class V1DisectorWindowPlanner(
    private val batchDurationMs: Long,
    overlapMs: Long
) {
    private val effectiveOverlapMs = overlapMs.coerceAtLeast(0)

    fun plan(audioDurationMs: Long): List<V1TranscriptionBatchWindow> {
        require(batchDurationMs > 0) { "batchDurationMs must be > 0" }
        if (audioDurationMs <= 0) {
            return emptyList()
        }
        val totalBatches = ((audioDurationMs + batchDurationMs - 1) / batchDurationMs).toInt()
        return (1..totalBatches).map { batchIndex ->
            val absStartMs = (batchIndex - 1) * batchDurationMs
            val absEndMs = min(batchIndex * batchDurationMs, audioDurationMs)
            // overlapMs 仅作为 pre-roll，第二批开始回溯录音起点用于对齐 Tingwu 相对时间。
            val captureStartMs = if (batchIndex == 1) {
                absStartMs
            } else {
                (absStartMs - effectiveOverlapMs).coerceAtLeast(0)
            }
            // 半开区间 [absStartMs, absEndMs)，批次顺序严格按 batchIndex 递增。
            V1TranscriptionBatchWindow(
                batchIndex = batchIndex,
                absStartMs = absStartMs,
                absEndMs = absEndMs,
                overlapMs = effectiveOverlapMs,
                captureStartMs = captureStartMs
            )
        }
    }
}
