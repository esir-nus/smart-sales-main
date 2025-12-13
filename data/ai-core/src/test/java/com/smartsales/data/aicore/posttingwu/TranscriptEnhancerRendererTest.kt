// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/posttingwu/TranscriptEnhancerRendererTest.kt
// 模块：:data:ai-core
// 说明：验证后置转写增强的补丁应用与渲染规则
// 作者：创建于 2025-12-13
package com.smartsales.data.aicore.posttingwu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptEnhancerRendererTest {

    @Test
    fun splitLines_onlyFirstKeepsTimestamp() {
        val utterances = listOf(
            EnhancerUtterance(
                index = 0,
                startMs = 1_000L,
                endMs = 2_000L,
                speakerId = "1",
                text = "嗯你好，欢迎光临"
            )
        )
        val output = EnhancerOutput(
            speakerRoster = listOf(SpeakerLabel("1", "销售顾问", 0.9)),
            utteranceEdits = listOf(
                UtteranceEdit(
                    index = 0,
                    split = listOf(
                        SplitLine("销售顾问", "你好"),
                        SplitLine("客户", "欢迎光临")
                    )
                )
            )
        )

        val lines = applyEnhancerOutput(
            utterances = utterances,
            output = output,
            baseSpeakerLabels = mapOf("1" to "销售")
        )

        assertEquals(2, lines.size)
        assertEquals(utterances.first().startMs, lines[0].timestampMs)
        assertEquals(null, lines[1].timestampMs)
        val markdown = renderEnhancedMarkdown(lines)
        assertTrue(markdown.contains("- [00:01] 销售顾问：你好"))
        assertTrue(markdown.contains("  客户：欢迎光临"))
    }

    @Test
    fun lowConfidenceRoster_fallsBackToBaseLabel() {
        val utterances = listOf(
            EnhancerUtterance(
                index = 0,
                startMs = 0,
                endMs = 0,
                speakerId = "2",
                text = "测试一句"
            )
        )
        val output = EnhancerOutput(
            speakerRoster = listOf(SpeakerLabel("2", "访客", 0.2)),
            utteranceEdits = emptyList()
        )

        val lines = applyEnhancerOutput(
            utterances = utterances,
            output = output,
            baseSpeakerLabels = mapOf("2" to "客户")
        )

        assertEquals(1, lines.size)
        assertEquals("客户", lines.first().speaker)
    }

    @Test
    fun nullOutput_returnsOriginalOrderWithFillerCleaned() {
        val utterances = listOf(
            EnhancerUtterance(
                index = 0,
                startMs = null,
                endMs = null,
                speakerId = "3",
                text = "嗯 然后 我想测试"
            )
        )

        val lines = applyEnhancerOutput(
            utterances = utterances,
            output = null,
            baseSpeakerLabels = emptyMap()
        )

        assertEquals(1, lines.size)
        assertTrue(lines.first().text.contains("我想测试"))
    }
}
