package com.smartsales.aitest.audio

import com.smartsales.feature.media.audiofiles.V1TimedTextSegment

// 文件：app/src/main/java/com/smartsales/aitest/audio/V1WindowKey.kt
// 模块：:app
// 说明：V1 切片辅助工具（windowKey + 时间锚定），不依赖 disectorPlanId。

data class RelativeTimedSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

object V1SegmentAnchoring {
    /**
     * 将切片相对时间锚定到录音起点(0ms)绝对时间。
     * 重要：actualCaptureStartMs 以实际切片起点为准（可能因采样对齐早于请求起点）。
     */
    fun anchorToRecordingOrigin(
        actualCaptureStartMs: Long,
        relativeSegments: List<RelativeTimedSegment>
    ): List<V1TimedTextSegment> {
        return relativeSegments.map { segment ->
            V1TimedTextSegment(
                startMs = actualCaptureStartMs + segment.startMs,
                endMs = actualCaptureStartMs + segment.endMs,
                text = segment.text
            )
        }
    }
}

object V1WindowKey {
    /**
     * 说明：运行时缺少 disectorPlanId/recordingSessionId，只能用 sessionId/jobId + 窗口信息生成稳定 key。
     * 该 key 用于切片文件/OSS 对象命名，不改变 V1 的 audioAssetId 语义。
     */
    fun build(
        sessionIdOrJobId: String,
        batchIndex: Int,
        requestedCaptureStartMs: Long,
        captureEndMs: Long
    ): String {
        require(batchIndex > 0) { "batchIndex must be 1-based" }
        val safeId = sanitize(sessionIdOrJobId)
        return "rs_${safeId}_b${batchIndex}_${requestedCaptureStartMs}_${captureEndMs}"
    }

    private fun sanitize(raw: String): String {
        val trimmed = raw.trim().ifEmpty { "session" }
        return trimmed.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}
