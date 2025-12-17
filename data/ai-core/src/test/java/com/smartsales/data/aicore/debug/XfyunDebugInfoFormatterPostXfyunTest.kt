// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/debug/XfyunDebugInfoFormatterPostXfyunTest.kt
// 模块：:data:ai-core
// 说明：验证 XFyun DebugInfoFormatter 会输出 PostXFyun 的设置/可疑边界/仲裁结果摘要
// 作者：创建于 2025-12-16
package com.smartsales.data.aicore.debug

import org.junit.Assert.assertTrue
import org.junit.Test

class XfyunDebugInfoFormatterPostXfyunTest {

    @Test
    fun `format includes postxfyun summaries`() {
        val snapshot = XfyunTraceSnapshot(
            provider = "XFyun",
            baseUrl = "https://example.com",
            orderId = "order-1",
            postXfyunSettings = XfyunTraceSnapshot.PostXfyunSettingsDebug(
                enabled = true,
                maxRepairsPerTranscript = 2,
                suspiciousGapThresholdMs = 200,
                confidenceThreshold = 0.85,
                modelEffective = "qwen-max3",
                promptLength = 12,
                promptPreview = "TEMPLATE-XYZ",
                promptSha256 = "deadbeef",
            ),
            postXfyunSuspicious = listOf(
                XfyunTraceSnapshot.PostXfyunSuspiciousBoundary(
                    boundaryIndex = 0,
                    gapMs = 120,
                    prevSpeakerId = "1",
                    nextSpeakerId = "2",
                    prevExcerpt = "好的罗",
                    nextExcerpt = "总我们继续",
                )
            ),
            postXfyunDecisions = listOf(
                XfyunTraceSnapshot.PostXfyunDecisionDebug(
                    boundaryIndex = 0,
                    action = "NONE",
                    span = "",
                    confidence = 0.99,
                    reason = "无需修复",
                    rawResponsePreview = "{\"action\":\"NONE\"}",
                )
            ),
        )
        val text = XfyunDebugInfoFormatter.format(snapshot)
        assertTrue(text.contains("\"postXfyunSettings\""))
        assertTrue(text.contains("\"postXfyunSuspiciousCount\""))
        assertTrue(text.contains("\"postXfyunDecisionsCount\""))
        // 用于交付“格式化调试文本输出”，不包含 raw HTTP JSON。
        println(text)
    }
}
