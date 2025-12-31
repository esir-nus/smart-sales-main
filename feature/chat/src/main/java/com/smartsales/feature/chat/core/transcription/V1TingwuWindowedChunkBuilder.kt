package com.smartsales.feature.chat.core.transcription

import com.smartsales.feature.media.audiofiles.V1TimedTextSegment

object V1TingwuWindowedChunkBuilder {
    data class BuildResult(
        val chunk: String,
        val segmentsInCount: Int,
        val segmentsOutCount: Int
    )

    /**
     * 按 V1 宏窗口 [absStartMs, absEndMs) 做确定性范围过滤（半开区间）。
     * timedSegments 的时间基准是“录音起点(0ms)”的绝对时间，直接与 absStart/absEnd 比较。
     * 这里只做范围过滤，不做文本相似度去重，也不做“补边/润色”，统计信息不包含文本内容。
     */
    fun buildWindowedMarkdownChunk(
        absStartMs: Long,
        absEndMs: Long,
        timedSegments: List<V1TimedTextSegment>
    ): String {
        return buildWindowedMarkdownChunkResult(
            absStartMs = absStartMs,
            absEndMs = absEndMs,
            timedSegments = timedSegments
        ).chunk
    }

    fun buildWindowedMarkdownChunkResult(
        absStartMs: Long,
        absEndMs: Long,
        timedSegments: List<V1TimedTextSegment>
    ): BuildResult {
        if (absEndMs <= absStartMs || timedSegments.isEmpty()) {
            return BuildResult(chunk = "", segmentsInCount = timedSegments.size, segmentsOutCount = 0)
        }
        val absSegments = timedSegments.map { segment ->
            AbsSegment(
                absStartMs = segment.startMs,
                absEndMs = segment.endMs,
                text = segment.text
            )
        }
        val filtered = V1TingwuMacroWindowFilter.filterToMacroWindow(
            absStartMs = absStartMs,
            absEndMs = absEndMs,
            absSegments = absSegments
        )
        val chunk = if (filtered.isEmpty()) "" else filtered.joinToString("\n") { it.text }
        return BuildResult(
            chunk = chunk,
            segmentsInCount = timedSegments.size,
            segmentsOutCount = filtered.size
        )
    }
}
