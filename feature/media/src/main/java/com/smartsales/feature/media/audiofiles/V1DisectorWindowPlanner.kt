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
        val fullBatches = audioDurationMs / batchDurationMs
        val rem = audioDurationMs % batchDurationMs
        val windows = mutableListOf<Pair<Long, Long>>()
        if (fullBatches == 0L) {
            windows += 0L to audioDurationMs
        } else {
            for (i in 0 until fullBatches) {
                val absStartMs = i * batchDurationMs
                windows += absStartMs to (absStartMs + batchDurationMs)
            }
            if (rem != 0L) {
                // 余数合并规则：rem < 7分钟则合并到最后一段，否则新建余数批次。
                if (rem < 420_000L) {
                    val last = windows.last()
                    windows[windows.lastIndex] = last.first to (last.second + rem)
                } else {
                    val absStartMs = fullBatches * batchDurationMs
                    windows += absStartMs to (absStartMs + rem)
                }
            }
        }
        return windows.mapIndexed { index, window ->
            val absStartMs = window.first
            val absEndMs = min(window.second, audioDurationMs)
            // overlapMs 仅作为 pre-roll，第二批开始回溯录音起点用于对齐 Tingwu 相对时间。
            val captureStartMs = if (index == 0) {
                absStartMs
            } else {
                (absStartMs - effectiveOverlapMs).coerceAtLeast(0)
            }
            // 半开区间 [absStartMs, absEndMs)，批次顺序严格按 batchIndex 递增。
            V1TranscriptionBatchWindow(
                batchIndex = index + 1,
                absStartMs = absStartMs,
                absEndMs = absEndMs,
                overlapMs = effectiveOverlapMs,
                captureStartMs = captureStartMs
            )
        }
    }
}
