package com.smartsales.domain.transcription

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/transcription/V1TingwuMacroWindowFilter.kt
// Module: :feature:chat
// Summary: V1 Tingwu time anchoring + macro-window range filtering helper.
// Author: created on 2025-12-30

data class RelativeSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class AbsSegment(
    val absStartMs: Long,
    val absEndMs: Long,
    val text: String
)

object V1TingwuMacroWindowFilter {
    // 相对时间必须锚定到绝对时间轴：abs = captureStartMs + relativeMs
    fun anchorToAbs(
        captureStartMs: Long,
        segments: List<RelativeSegment>
    ): List<AbsSegment> {
        if (segments.isEmpty()) return emptyList()
        return segments.mapNotNull { segment ->
            if (segment.endMs < segment.startMs) {
                // 丢弃非法区间，保持确定性
                null
            } else {
                AbsSegment(
                    absStartMs = captureStartMs + segment.startMs,
                    absEndMs = captureStartMs + segment.endMs,
                    text = segment.text
                )
            }
        }
    }

    // 半开区间 [absStartMs, absEndMs) 的重叠判定：仅做范围过滤，不做文本相似度去重
    fun filterToMacroWindow(
        absStartMs: Long,
        absEndMs: Long,
        absSegments: List<AbsSegment>
    ): List<AbsSegment> {
        if (absEndMs <= absStartMs || absSegments.isEmpty()) return emptyList()
        return absSegments.filter { segment ->
            if (segment.absEndMs < segment.absStartMs) {
                false
            } else {
                segment.absEndMs > absStartMs && segment.absStartMs < absEndMs
            }
        }
    }
}
