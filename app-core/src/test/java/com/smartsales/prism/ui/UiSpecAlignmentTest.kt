package com.smartsales.prism.ui

import com.smartsales.prism.domain.model.UiState
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * UiSpecAlignmentTest
 * 
 * Enforces the "Docs-First Protocol" for the Presentation Layer.
 * This test uses Kotlin Reflection to scan the UiState sealed class
 * and matches it against the markdown table in `docs/cerb-ui/agent-intelligence/interface.md`.
 * 
 * If a developer adds a new state to the code but forgets to document it, this test fails.
 * If a PM adds a state to the spec but it's not implemented, this test fails.
 */
class UiSpecAlignmentTest {

    @Test
    fun `UiState code exactly matches Cerb interface documentation`() {
        // 1. Get all UiState names from Kotlin Code
        val codeStates = UiState::class.sealedSubclasses.map { it.simpleName }.toSet()

        // 2. Read the Cerb Spec from the filesystem
        // We navigate up from the app-core/src/test/java directory to the project root
        val projectRoot = File(".").absoluteFile.parentFile.parentFile ?: File(".")
        val interfaceFile = File(projectRoot, "docs/cerb-ui/agent-intelligence/interface.md")
        
        if (!interfaceFile.exists()) {
            fail("CRITICAL: Cerb UI Interface Spec missing at ${interfaceFile.absolutePath}")
        }

        val specContent = interfaceFile.readText()

        // 3. Extract documented states using a stricter regex parsing strategy
        // We look for "- `UiState.ClassName" or "- `UiState.ClassName(" 
        val stateRegex = Regex("""-\s*`UiState\.([A-Za-z0-9_]+)""")
        val documentedStates = stateRegex.findAll(specContent)
            .map { it.groupValues[1] }
            .toSet()

        // 4. Calculate Drift (The Anti-Illusion Check)
        val undocumentedCode = codeStates - documentedStates
        val unimplementedDocs = documentedStates - codeStates

        // 5. Build Error Report
        val errorMessage = buildString {
            if (undocumentedCode.isNotEmpty()) {
                append("\n❌ CODE DRIFT: The following UiStates exist in code but are NOT in docs/cerb-ui/agent-intelligence/interface.md:\n")
                undocumentedCode.forEach { append("  - $it\n") }
                append("Fix: Add them to the 'Data Contract' section of the spec.\n")
            }
            if (unimplementedDocs.isNotEmpty()) {
                append("\n❌ SPEC DRIFT: The following UiStates are documented in Cerb but DO NOT exist as Kotlin classes in UiState.kt:\n")
                unimplementedDocs.forEach { append("  - $it\n") }
                append("Fix: Implement them in UiState.kt or remove them from the spec if deprecated.\n")
            }
        }

        assertTrue(errorMessage, undocumentedCode.isEmpty() && unimplementedDocs.isEmpty())
    }
}
