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
