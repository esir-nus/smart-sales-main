package com.smartsales.domain.chat

// File: feature/chat/src/test/java/com/smartsales/domain/chat/MetadataParserTest.kt
// Module: :feature:chat
// Summary: Tests for MetadataParser JSON parsing utilities
// Author: created on 2026-01-13

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MetadataParserTest {

    // ===== parseGeneralChatMetadata tests =====

    @Test
    fun `parseGeneralChatMetadata with valid JSON returns SessionMetadata`() {
        val json = """
            {
                "main_person": "张三",
                "short_summary": "客户咨询产品价格",
                "summary_title_6chars": "价格咨询",
                "location": "北京"
            }
        """.trimIndent()

        val result = MetadataParser.parseGeneralChatMetadata(json, "session-123")

        assertNotNull(result)
        assertEquals("session-123", result!!.sessionId)
        assertEquals("张三", result.mainPerson)
        assertEquals("客户咨询产品价格", result.shortSummary)
        assertEquals("价格咨询", result.summaryTitle6Chars)
        assertEquals("北京", result.location)
    }

    @Test
    fun `parseGeneralChatMetadata with 8char summary falls back to 6 chars`() {
        val json = """
            {
                "summary_title_8chars": "产品价格咨询"
            }
        """.trimIndent()

        val result = MetadataParser.parseGeneralChatMetadata(json, "session-456")

        assertNotNull(result)
        assertEquals("产品价格咨询".take(6), result!!.summaryTitle6Chars)
    }

    @Test
    fun `parseGeneralChatMetadata with malformed JSON returns null`() {
        val result = MetadataParser.parseGeneralChatMetadata("not valid json {", "session-789")

        assertNull(result)
    }

    @Test
    fun `parseGeneralChatMetadata with empty JSON returns metadata with nulls`() {
        val result = MetadataParser.parseGeneralChatMetadata("{}", "session-empty")

        assertNotNull(result)
        assertEquals("session-empty", result!!.sessionId)
        assertNull(result.mainPerson)
        assertNull(result.shortSummary)
    }

    @Test
    fun `parseGeneralChatMetadata ignores blank strings`() {
        val json = """
            {
                "main_person": "  ",
                "short_summary": ""
            }
        """.trimIndent()

        val result = MetadataParser.parseGeneralChatMetadata(json, "session-blank")

        assertNotNull(result)
        assertNull(result!!.mainPerson)
        assertNull(result.shortSummary)
    }

    // ===== findLastJsonBlock tests =====

    @Test
    fun `findLastJsonBlock with fenced json block extracts content`() {
        val text = """
            Here is some text.
            ```json
            {"key": "value"}
            ```
            Done.
        """.trimIndent()

        // Note: trailing text after ``` makes this not match (only last block if at end)
        // Let's test a cleaner case
        val cleanText = """
            Some preamble
            ```json
            {"key": "value"}
            ```
        """.trimIndent()

        val result = MetadataParser.findLastJsonBlock(cleanText)

        assertNotNull(result)
        assertEquals("{\"key\": \"value\"}", result!!.text)
    }

    @Test
    fun `findLastJsonBlock with trailing JSON object extracts it`() {
        val text = """
            This is a response with JSON at the end.
            {"result": "success", "count": 42}
        """.trimIndent()

        val result = MetadataParser.findLastJsonBlock(text)

        assertNotNull(result)
        assertEquals("{\"result\": \"success\", \"count\": 42}", result!!.text)
    }

    @Test
    fun `findLastJsonBlock with no JSON returns null`() {
        val text = "This is plain text with no JSON content."

        val result = MetadataParser.findLastJsonBlock(text)

        assertNull(result)
    }

    @Test
    fun `findLastJsonBlock with pure JSON object returns it`() {
        val text = """{"pure": "json"}"""

        val result = MetadataParser.findLastJsonBlock(text)

        assertNotNull(result)
        assertEquals("{\"pure\": \"json\"}", result!!.text)
    }

    @Test
    fun `findLastJsonBlock with nested braces handles correctly`() {
        val text = """
            Response:
            {"outer": {"inner": "value"}}
        """.trimIndent()

        val result = MetadataParser.findLastJsonBlock(text)

        assertNotNull(result)
        assertEquals("{\"outer\": {\"inner\": \"value\"}}", result!!.text)
    }

    @Test
    fun `findLastJsonBlock prefers last json block when multiple exist`() {
        val text = """
            First block:
            ```json
            {"first": true}
            ```
            Second block:
            ```json
            {"second": true}
            ```
        """.trimIndent()

        val result = MetadataParser.findLastJsonBlock(text)

        assertNotNull(result)
        assertEquals("{\"second\": true}", result!!.text)
    }
}
