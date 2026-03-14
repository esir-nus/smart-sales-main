package com.smartsales.prism.ui

import com.smartsales.prism.domain.model.UiState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Anti-Drift Mechanical Check: The Docs-First UI Protocol Guard.
 * This test enforces that the Cerb Specification (interface.md) and the Kotlin Code (UiState)
 * are in mathematically perfect 100% bijection.
 * 
 * Hallucination Prevention:
 * 1. An Agent reads interface.md and assumes a state exists.
 * 2. If the state was deleted in code but the spec was not updated, an Agent hallucinates.
 * 3. This test breaks the build if they drift, forcing developers to sync the Docs.
 */
class UiSpecAlignmentTest {

    @Test
    fun verifyUiStateMatchesCerbSpec() {
        // 1. Resolve relative path (from app-core/ to project root)
        val projectRoot = File("..")
        val specFile = File(projectRoot, "docs/cerb-ui/agent-intelligence/interface.md")
        
        // Failsafe: Ensure the file actually exists
        assert(specFile.exists()) { "Cerb Interface Spec missing at: ${specFile.absolutePath}" }

        val specContent = specFile.readText()

        // 2. Parse Markdown for UI States
        // We look for strict list items starting with "- `UiState.X"
        val stateRegex = Regex("""-\s*`UiState\.([A-Za-z0-9_]+)""")
        val documentedStates = stateRegex.findAll(specContent)
            .map { it.groupValues[1] }
            .toSet()

        // 3. Parse Kotlin via Reflection
        // Get all sealed subclasses of UiState
        val compiledClasses = UiState::class.sealedSubclasses
        val compiledStates = compiledClasses.mapNotNull { it.simpleName }.toSet()

        // 4. Mathematical Bijection Check
        val missingInCode = documentedStates - compiledStates
        val missingInDocs = compiledStates - documentedStates

        val errorMessage = buildString {
            if (missingInCode.isNotEmpty()) {
                append("❌ States documented in Markdown but missing in Kotlin code: $missingInCode\n")
            }
            if (missingInDocs.isNotEmpty()) {
                append("❌ States existing in Kotlin code but missing in Markdown: $missingInDocs\n")
            }
        }

        assertEquals(
            "UI SPEC DRIFT DETECTED!\n$errorMessage\nSolution: Update interface.md or UiState.kt to re-establish Bijection.",
            documentedStates,
            compiledStates
        )
    }

    @Test
    fun verifySchedulerUiStateMatchesCerbSpec() {
        val projectRoot = File("..")
        val specFile = File(projectRoot, "docs/cerb-ui/scheduler/contract.md")
        
        assert(specFile.exists()) { "Cerb Scheduler Contract missing at: ${specFile.absolutePath}" }

        val specContent = specFile.readText()

        val stateRegex = Regex("""(object|data class)\s+([A-Za-z0-9_]+)""")
        val documentedStates = stateRegex.findAll(specContent)
            .map { it.groupValues[2] }
            .filter { it != "SchedulerUiState" && it != "SchedulerIntent" && it != "OnConfirm" && it != "OnCancel" && it != "OnEditField" && it != "OnResolveConflict" }
            .toSet()

        val compiledClasses = com.smartsales.prism.ui.drawers.scheduler.SchedulerUiState::class.sealedSubclasses
        val compiledStates = compiledClasses.mapNotNull { it.simpleName }.toSet()

        val missingInCode = documentedStates.subtract(compiledStates)
        val missingInDocs = compiledStates.subtract(documentedStates)

        val errorMessage = buildString {
            if (missingInCode.isNotEmpty()) {
                append("❌ States documented in Markdown but missing in Kotlin code: $missingInCode\n")
            }
            if (missingInDocs.isNotEmpty()) {
                append("❌ States existing in Kotlin code but missing in Markdown: $missingInDocs\n")
            }
        }

        assertEquals(
            "SCHEDULER UI SPEC DRIFT DETECTED!\n$errorMessage\nSolution: Update contract.md or SchedulerUiState.kt to re-establish Bijection.",
            documentedStates,
            compiledStates
        )
    }
}
