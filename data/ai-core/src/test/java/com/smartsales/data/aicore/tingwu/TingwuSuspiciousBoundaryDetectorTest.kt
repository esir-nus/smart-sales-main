// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/tingwu/TingwuSuspiciousBoundaryDetectorTest.kt
// 模块：:data:ai-core
// 说明：验证 Tingwu 可疑边界检测的解析与确定性
// 作者：创建于 2025-12-24
package com.smartsales.data.aicore.tingwu

import kotlin.test.assertEquals
import org.junit.Test

class TingwuSuspiciousBoundaryDetectorTest {
    @Test
    fun `detects gap boundary with range timestamp`() {
        val markdown = """
            ## 逐字稿
            - [01:02:03 - 01:02:05] 1：你好
            - [01:02:06] 1：继续
        """.trimIndent()

        val boundaries = TingwuSuspiciousBoundaryDetector.detect(
            transcriptMarkdown = markdown,
            gapThresholdMs = 500
        )

        assertEquals(1, boundaries.size)
        assertEquals(1, boundaries.first().index)
        assertEquals("gap", boundaries.first().reason)
    }

    @Test
    fun `detects speaker change when gap is small`() {
        val markdown = """
            ## 逐字稿
            - [00:00] 客户：好的
            - [00:00] 销售：继续
        """.trimIndent()

        val boundaries = TingwuSuspiciousBoundaryDetector.detect(
            transcriptMarkdown = markdown,
            gapThresholdMs = 1000
        )

        assertEquals(1, boundaries.size)
        assertEquals(1, boundaries.first().index)
        assertEquals("speaker-change", boundaries.first().reason)
    }

    @Test
    fun `prefers gap and stays deterministic`() {
        val markdown = """
            ## 逐字稿
            - [00:00] A：开始
            - [00:02] B：继续
        """.trimIndent()

        val first = TingwuSuspiciousBoundaryDetector.detect(
            transcriptMarkdown = markdown,
            gapThresholdMs = 0
        )
        val second = TingwuSuspiciousBoundaryDetector.detect(
            transcriptMarkdown = markdown,
            gapThresholdMs = 0
        )

        assertEquals(1, first.size)
        assertEquals("gap", first.first().reason)
        assertEquals(first, second)
    }
}
