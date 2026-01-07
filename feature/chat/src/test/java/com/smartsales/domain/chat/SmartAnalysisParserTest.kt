package com.smartsales.domain.chat

// File: feature/chat/src/test/java/com/smartsales/domain/chat/SmartAnalysisParserTest.kt
// Module: :feature:chat
// Summary: Unit tests for SmartAnalysisParser domain class
// Author: created on 2026-01-06

import com.smartsales.core.metahub.RiskLevel
import com.smartsales.core.metahub.SessionStage
import com.smartsales.domain.analysis.ParsedSmartAnalysis
import com.smartsales.domain.analysis.SmartAnalysisParser
import org.junit.Assert.*
import org.junit.Test

class SmartAnalysisParserTest {

    // ===== parse() - Valid JSON tests =====

    @Test
    fun parse_validJson_extractsAllFields() {
        val jsonText = """
            ```json
            {
                "main_person": "张总",
                "short_summary": "讨论了新产品需求",
                "summary_title_6chars": "产品需求",
                "location": "北京",
                "stage": "DISCOVERY",
                "risk_level": "LOW",
                "highlights": ["价格敏感", "需要定制"],
                "actionable_tips": ["准备方案", "跟进沟通"],
                "core_insight": "客户对价格非常关注",
                "sharp_line": "你可以强调性价比优势"
            }
            ```
        """.trimIndent()

        val result = SmartAnalysisParser.parse(jsonText)

        assertNotNull(result)
        assertEquals("张总", result?.mainPerson)
        assertEquals("讨论了新产品需求", result?.shortSummary)
        assertEquals("产品需求", result?.summaryTitle6Chars)
        assertEquals("北京", result?.location)
        assertEquals(SessionStage.DISCOVERY, result?.stage)
        assertEquals(RiskLevel.LOW, result?.riskLevel)
        assertEquals(listOf("价格敏感", "需要定制"), result?.highlights)
        assertEquals(listOf("准备方案", "跟进沟通"), result?.actionableTips)
        assertEquals("客户对价格非常关注", result?.coreInsight)
        assertEquals("你可以强调性价比优势", result?.sharpLine)
    }

    @Test
    fun parse_minimalJson_extractsPresentFields() {
        val jsonText = """
            {"short_summary": "简短概要"}
        """.trimIndent()

        val result = SmartAnalysisParser.parse(jsonText)

        assertNotNull(result)
        assertEquals("简短概要", result?.shortSummary)
        assertNull(result?.mainPerson)
        assertNull(result?.stage)
    }

    @Test
    fun parse_lastValueWins_forShortSummary() {
        val text = """
            "short_summary": "第一个"
            "short_summary": "第二个"
        """.trimIndent()

        val result = SmartAnalysisParser.parse(text)

        assertEquals("第二个", result?.shortSummary)
    }

    // ===== parse() - Fallback to text parsing =====

    @Test
    fun parse_malformedJson_fallsBackToText() {
        val text = """
            这是一些噪声
            "short_summary": "文本提取的概要"
            "core_insight": "文本提取的洞察"
        """.trimIndent()

        val result = SmartAnalysisParser.parse(text)

        assertNotNull(result)
        assertEquals("文本提取的概要", result?.shortSummary)
        assertEquals("文本提取的洞察", result?.coreInsight)
    }

    @Test
    fun parse_noContent_returnsNull() {
        val emptyText = "{}"
        val result = SmartAnalysisParser.parse(emptyText)
        assertNull(result)
    }

    // ===== normalizePov() tests =====

    @Test
    fun normalizePov_我在_transformsTo你在() {
        val input = "我在跟进这个客户"
        // Access via reflection since normalizePov is private
        // For now, test via buildMarkdown which uses it
        val parsed = ParsedSmartAnalysis(
            mainPerson = null,
            shortSummary = input,
            summaryTitle6Chars = null,
            location = null,
            stage = null,
            riskLevel = null,
            highlights = emptyList(),
            actionableTips = emptyList(),
            coreInsight = null,
            sharpLine = null
        )

        val markdown = SmartAnalysisParser::class.java
            .getDeclaredMethod("buildMarkdown", ParsedSmartAnalysis::class.java)
            .apply { isAccessible = true }
            .invoke(SmartAnalysisParser, parsed) as String

        assertTrue(markdown.contains("你在跟进这个客户"))
        assertFalse(markdown.contains("我在"))
    }

    // ===== postProcessSmartMarkdown() tests =====

    @Test
    fun postProcessMarkdown_deduplicatesAcrossSections() {
        val markdown = """
            ## 需求与痛点
            - 价格敏感
            
            ## 建议与行动
            - 价格敏感
        """.trimIndent()

        val processed = SmartAnalysisParser::class.java
            .getDeclaredMethod("postProcessSmartMarkdown", String::class.java)
            .apply { isAccessible = true }
            .invoke(SmartAnalysisParser, markdown) as String

        // Should only appear once
        assertEquals(1, processed.split("价格敏感").size - 1)
    }

    @Test
    fun postProcessMarkdown_capsBulletsAt5() {
        val markdown = """
            ## 需求与痛点
            - 第1个
            - 第2个
            - 第3个
            - 第4个
            - 第5个
            - 第6个
            - 第7个
        """.trimIndent()

        val processed = SmartAnalysisParser::class.java
            .getDeclaredMethod("postProcessSmartMarkdown", String::class.java)
            .apply { isAccessible = true }
            .invoke(SmartAnalysisParser, markdown) as String

        val bulletCount = processed.lines().count { it.trim().startsWith("-") }
        assertEquals(5, bulletCount)
    }

    @Test
    fun postProcessMarkdown_normalizesNumbering() {
        val markdown = """
            ## 建议与行动
            1. 第一步
            99. 第二步
            3） 第三步
        """.trimIndent()

        val processed = SmartAnalysisParser::class.java
            .getDeclaredMethod("postProcessSmartMarkdown", String::class.java)
            .apply { isAccessible = true }
            .invoke(SmartAnalysisParser, markdown) as String

        assertTrue(processed.contains("1. 第一步"))
        assertTrue(processed.contains("2. 第二步"))
        assertTrue(processed.contains("3. 第三步"))
    }

    @Test
    fun postProcessMarkdown_hidesEmptySections() {
        val markdown = """
            ## 会话概要
            - 有内容
            
            ## 需求与痛点
            
            ## 建议与行动
            - 也有内容
        """.trimIndent()

        val processed = SmartAnalysisParser::class.java
            .getDeclaredMethod("postProcessSmartMarkdown", String::class.java)
            .apply { isAccessible = true }
            .invoke(SmartAnalysisParser, markdown) as String

        assertTrue(processed.contains("会话概要"))
        assertFalse(processed.contains("需求与痛点"))
        assertTrue(processed.contains("建议与行动"))
    }

    //===== Edge cases =====

    @Test
    fun parse_chinesePunctuation_handledCorrectly() {
        val jsonText = """{"short_summary": "内容：测试；符号。"}"""
        val result = SmartAnalysisParser.parse(jsonText)
        assertEquals("内容：测试；符号。", result?.shortSummary)
    }

    @Test
    fun parse_stageEnumMapping_handlesAllValues() {
        listOf(
            "DISCOVERY" to SessionStage.DISCOVERY,
            "NEGOTIATION" to SessionStage.NEGOTIATION,
            "PROPOSAL" to SessionStage.PROPOSAL,
            "CLOSING" to SessionStage.CLOSING,
            "POST_SALE" to SessionStage.POST_SALE,
            "POSTSALE" to SessionStage.POST_SALE,
            "POST-SALE" to SessionStage.POST_SALE
        ).forEach { (input, expected) ->
            val json = """{"stage": "$input"}"""
            val result = SmartAnalysisParser.parse(json)
            assertEquals(expected, result?.stage)
        }
    }

    @Test
    fun parse_riskEnumMapping_handlesAllValues() {
        listOf(
            "LOW" to RiskLevel.LOW,
            "MEDIUM" to RiskLevel.MEDIUM,
            "HIGH" to RiskLevel.HIGH
        ).forEach { (input, expected) ->
            val json = """{"risk_level": "$input"}"""
            val result = SmartAnalysisParser.parse(json)
            assertEquals(expected, result?.riskLevel)
        }
    }
}
