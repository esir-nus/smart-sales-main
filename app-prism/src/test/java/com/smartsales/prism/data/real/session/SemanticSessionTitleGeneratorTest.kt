package com.smartsales.prism.data.real.session

import org.junit.Assert.assertEquals
import org.junit.Test

class SemanticSessionTitleGeneratorTest {

    private val generator = SemanticSessionTitleGenerator()

    @Test
    fun `generateTitle parses valid JSON with names correctly`() {
        // Arrange
        val jsonResponse = """
            {"temporal_intent": "询问Q4预算", "context_type": "Q&A"}
        """.trimIndent()
        val names = listOf("王总", "李经理")

        // Act
        val result = generator.generateTitle(jsonResponse, names)

        // Assert
        assertEquals("王总、李经理", result.clientName)
        assertEquals("询问Q4预算", result.summary)
    }

    @Test
    fun `generateTitle handles missing JSON gracefully`() {
        // Arrange
        val jsonResponse = "Not JSON format at all"
        val names = emptyList<String>()

        // Act
        val result = generator.generateTitle(jsonResponse, names)

        // Assert
        assertEquals("客户", result.clientName)
        assertEquals("新会话", result.summary)
    }

    @Test
    fun `generateTitle truncates long intents`() {
        // Arrange
        val jsonResponse = """
            {"temporal_intent": "询问Q4预算并要求提供明细报告"}
        """.trimIndent()
        
        // Act
        val result = generator.generateTitle(jsonResponse, listOf("张总"))

        // Assert
        assertEquals("张总", result.clientName)
        // 11 chars + ellipsis
        assertEquals("询问Q4预算并要求提供…", result.summary)
    }
}
