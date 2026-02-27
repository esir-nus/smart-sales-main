package com.smartsales.prism.data.real

import com.smartsales.prism.domain.analyst.InvestigationResult
import com.smartsales.prism.domain.analyst.WorkflowSuggestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ArchitectLinterTest {

    private lateinit var linter: ArchitectLinter

    @Before
    fun setUp() {
        linter = ArchitectLinter()
    }

    @Test
    fun `lintInvestigation parses valid JSON correctly`() {
        val json = """
            ```json
            {
                "analysisContent": "# Analysis Report\n\nThis is a standard report.",
                "suggestedWorkflows": [
                    {
                        "workflowId": "EXPORT_PDF",
                        "label": "Export Report to PDF"
                    },
                    {
                        "workflowId": "DRAFT_EMAIL",
                        "label": "Draft Email Context"
                    }
                ]
            }
            ```
        """.trimIndent()

        val result = linter.lintInvestigation(json)
        
        assertTrue("Expected Success, got ${result::class.simpleName}", result is ArchitectInvestigationLinterResult.Success)
        val success = result as ArchitectInvestigationLinterResult.Success
        
        assertEquals("# Analysis Report\n\nThis is a standard report.", success.result.analysisContent)
        assertEquals(2, success.result.suggestedWorkflows.size)
        assertEquals("EXPORT_PDF", success.result.suggestedWorkflows[0].workflowId)
        assertEquals("Export Report to PDF", success.result.suggestedWorkflows[0].label)
    }

    @Test
    fun `lintInvestigation handles missing suggestedWorkflows`() {
        val json = """
            {
                "analysisContent": "Test Content without workflows"
            }
        """.trimIndent()

        val result = linter.lintInvestigation(json)
        
        assertTrue(result is ArchitectInvestigationLinterResult.Success)
        val success = result as ArchitectInvestigationLinterResult.Success
        
        assertEquals("Test Content without workflows", success.result.analysisContent)
        assertTrue(success.result.suggestedWorkflows.isEmpty())
    }

    @Test
    fun `lintInvestigation returns Error on completely malformed JSON`() {
        val json = "This is just plain text, not JSON at all."

        val result = linter.lintInvestigation(json)
        
        assertTrue(result is ArchitectInvestigationLinterResult.Error)
    }
}
