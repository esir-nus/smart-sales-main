package com.smartsales.prism.ui.sim

import com.smartsales.prism.domain.tingwu.DiarizedSegment
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import org.junit.Assert.assertEquals
import org.junit.Test

class TingwuTranscriptPresentationTest {

    @Test
    fun `buildSpeakerAwareTranscript renders speaker-prefixed diarized lines`() {
        val artifacts = TingwuJobArtifacts(
            transcriptMarkdown = "原始逐字稿",
            diarizedSegments = listOf(
                DiarizedSegment("spk_2", 1, 1_000, 2_000, "可以下周启动。"),
                DiarizedSegment("spk_1", 0, 0, 900, "我们先确认预算。")
            ),
            speakerLabels = mapOf(
                "spk_1" to "销售顾问",
                "spk_2" to "客户"
            )
        )

        assertEquals(
            "销售顾问：我们先确认预算。\n客户：可以下周启动。",
            buildSpeakerAwareTranscript(artifacts)
        )
    }

    @Test
    fun `buildSpeakerAwareTranscript falls back to raw transcript when diarized segments are absent`() {
        val artifacts = TingwuJobArtifacts(transcriptMarkdown = "# 标题\n- 第一行 **内容**")

        assertEquals("# 标题\n- 第一行 **内容**", buildSpeakerAwareTranscript(artifacts))
    }

    @Test
    fun `buildSpeakerAwareTranscript renders numeric speaker ids as named speakers`() {
        val artifacts = TingwuJobArtifacts(
            diarizedSegments = listOf(
                DiarizedSegment("1", 0, 0, 900, "在路虎能买什么车？"),
                DiarizedSegment("2", 1, 1_000, 1_900, "您好，罗总。"),
                DiarizedSegment("3", 2, 2_000, 2_900, "欢迎来到捷豹路虎。")
            )
        )

        assertEquals(
            "发言人1：在路虎能买什么车？\n发言人2：您好，罗总。\n发言人3：欢迎来到捷豹路虎。",
            buildSpeakerAwareTranscript(artifacts)
        )
    }

    @Test
    fun `buildSpeakerAwareTranscriptPreview strips markdown and preserves speaker-readable text`() {
        val artifacts = TingwuJobArtifacts(
            transcriptMarkdown = "# 标题\n- 第一行 **内容**",
            diarizedSegments = listOf(
                DiarizedSegment("spk_1", 0, 0, 900, "**报价**可以调整。")
            ),
            speakerLabels = mapOf("spk_1" to "客户")
        )

        assertEquals(
            "客户： 报价 可以调整。",
            buildSpeakerAwareTranscriptPreview(artifacts)
        )
    }
}
