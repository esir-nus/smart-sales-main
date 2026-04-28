package com.smartsales.prism.ui.sim

import com.smartsales.prism.domain.tingwu.DiarizedSegment
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.domain.tingwu.TingwuQuestionAnswer
import com.smartsales.prism.domain.tingwu.TingwuSpeakerSummary
import com.smartsales.prism.domain.tingwu.TingwuSmartSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimArtifactContentTest {

    @Test
    fun `line threshold only marks long transcript after line 4`() {
        assertFalse(
            hasExceededTranscriptCollapseThreshold(
                renderedLineCount = 4,
                collapseAfterRenderedLines = 4
            )
        )
        assertTrue(
            hasExceededTranscriptCollapseThreshold(
                renderedLineCount = 5,
                collapseAfterRenderedLines = 4
            )
        )
    }

    @Test
    fun `remaining dwell waits until readable reveal window is satisfied`() {
        assertEquals(
            400L,
            remainingTranscriptRevealDwellMillis(
                revealStartedAtMillis = 100L,
                nowMillis = 700L,
                minRevealMillis = 1000L
            )
        )
        assertEquals(
            0L,
            remainingTranscriptRevealDwellMillis(
                revealStartedAtMillis = 100L,
                nowMillis = 1200L,
                minRevealMillis = 1000L
            )
        )
    }

    @Test
    fun `resolveSimArtifactKeywords prefers normalized artifact keywords`() {
        val artifacts = TingwuJobArtifacts(
            keywords = listOf("需求变更", "交互设计", "高优")
        )

        assertEquals(
            listOf("需求变更", "交互设计", "高优"),
            resolveSimArtifactKeywords(artifacts)
        )
    }

    @Test
    fun `resolveSimArtifactKeywords falls back to MeetingAssistance raw payload`() {
        val artifacts = TingwuJobArtifacts(
            meetingAssistanceRaw = """
                {
                  "MeetingAssistance": {
                    "Keywords": ["需求变更", "交互设计", "高优"]
                  }
                }
            """.trimIndent()
        )

        assertEquals(
            listOf("需求变更", "交互设计", "高优"),
            resolveSimArtifactKeywords(artifacts)
        )
    }

    @Test
    fun `buildSimSpeakerSummarySection renders standalone speaker recap lines`() {
        val artifacts = TingwuJobArtifacts(
            smartSummary = TingwuSmartSummary(
                speakerSummaries = listOf(
                    TingwuSpeakerSummary(name = "罗总", summary = "重点关注预算和交付节奏"),
                    TingwuSpeakerSummary(name = null, summary = "补充了试点范围")
                )
            )
        )

        assertEquals(
            "- 罗总：重点关注预算和交付节奏\n- 发言人：补充了试点范围",
            buildSimSpeakerSummarySection(artifacts)
        )
    }

    @Test
    fun `buildSimSpeakerSection normalizes numeric self labels`() {
        val artifacts = TingwuJobArtifacts(
            speakerLabels = mapOf(
                "1" to "1",
                "spk_2" to "spk_2",
                "speaker_3" to "客户"
            )
        )

        assertEquals(
            "- 发言人1 (1)\n- 发言人2 (spk_2)\n- 客户 (speaker_3)",
            buildSimSpeakerSection(artifacts)
        )
    }

    @Test
    fun `buildSimSpeakerSection normalizes diarized placeholder ids`() {
        val artifacts = TingwuJobArtifacts(
            diarizedSegments = listOf(
                DiarizedSegment("spk_1", 0, 0, 900, "预算可以确认。"),
                DiarizedSegment("spk_1", 0, 1_000, 1_900, "下周启动。"),
                DiarizedSegment(null, 1, 2_000, 2_900, "我来补充。")
            )
        )

        assertEquals(
            "- 发言人1: 2 段\n- 发言人2: 1 段",
            buildSimSpeakerSection(artifacts)
        )
    }

    @Test
    fun `buildSimQuestionAnswerSection renders standalone qa lines`() {
        val artifacts = TingwuJobArtifacts(
            smartSummary = TingwuSmartSummary(
                questionAnswers = listOf(
                    TingwuQuestionAnswer(question = "什么时候启动？", answer = "下周"),
                    TingwuQuestionAnswer(question = "预算多少？", answer = "先按季度")
                )
            )
        )

        assertEquals(
            "Q: 什么时候启动？\nA: 下周\n\nQ: 预算多少？\nA: 先按季度",
            buildSimQuestionAnswerSection(artifacts)
        )
    }

    @Test
    fun `buildSimProviderAdjacentSection does not duplicate keywords once chips are shown`() {
        val artifacts = TingwuJobArtifacts(
            meetingAssistanceRaw = """
                {
                  "MeetingAssistance": {
                    "Keywords": ["需求变更", "交互设计"],
                    "Actions": ["回传方案"],
                    "KeySentences": [{"Text": "客户要求本周给出方案。"}]
                  }
                }
            """.trimIndent()
        )

        val section = buildSimProviderAdjacentSection(artifacts)

        assertTrue(section!!.contains("待办事项"))
        assertTrue(section.contains("重点内容"))
        assertFalse(section.contains("关键词"))
        assertFalse(section.contains("需求变更"))
    }

    @Test
    fun `resolveSimArtifactOverview prefers summary over fallback preview`() {
        val artifacts = TingwuJobArtifacts(
            smartSummary = TingwuSmartSummary(summary = "**豪华体验**\n客户重点比较了配置和预算。")
        )

        assertEquals(
            "豪华体验 客户重点比较了配置和预算。",
            resolveSimArtifactOverview(artifacts, fallbackOverview = "旧预览")
        )
    }

    @Test
    fun `buildSimQuestionAnswerSection returns null when qa data is absent`() {
        assertNull(buildSimQuestionAnswerSection(TingwuJobArtifacts()))
    }
}
