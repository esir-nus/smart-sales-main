package com.smartsales.prism.data.fakes.plugins

import com.smartsales.prism.domain.analyst.AnalystTool
import com.smartsales.prism.domain.analyst.PluginRequest
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeConfigurablePluginBreakItTest {

    private val testTool = AnalystTool(
        id = "TEST_TOOL_01",
        icon = "🛠️",
        label = "Test Tool",
        description = "A tool for testing",
        requiredParams = mapOf("param1" to "string")
    )
    
    private val plugin = FakeConfigurablePlugin(testTool)

    @Test
    fun `execute with empty parameters returns semantic placeholder`() = runTest {
        val request = PluginRequest(rawInput = "do it", parameters = emptyMap())
        val states = plugin.execute(request).toList()
        
        assertEquals(4, states.size)
        // State 0: Starting
        // State 1: Parsing
        // State 2: Processing (Shows "No parameters")
        assertTrue((states[2] as UiState.ExecutingTool).toolName.contains("No parameters"))
        // State 3: Response
        assertTrue((states[3] as UiState.Response).content.contains("No parameters"))
    }

    @Test
    fun `execute with multiple complex parameters parses them into string`() = runTest {
        val request = PluginRequest(
            rawInput = "do it", 
            parameters = mapOf(
                "reportType" to "annual_summary",
                "year" to 2026,
                "nullParam" to "null"
            )
        )
        val states = plugin.execute(request).toList()
        
        assertEquals(4, states.size)
        val finalResponse = states.last() as UiState.Response
        assertTrue(finalResponse.content.contains("reportType=annual_summary"))
        assertTrue(finalResponse.content.contains("year=2026"))
        assertTrue(finalResponse.content.contains("nullParam=null"))
    }
}
