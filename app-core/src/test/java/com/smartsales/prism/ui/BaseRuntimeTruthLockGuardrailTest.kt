package com.smartsales.prism.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseRuntimeTruthLockGuardrailTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `authoritative unification docs keep one non mono product truth visible`() {
        assertFileContains(
            "docs/specs/base-runtime-unification.md",
            "There is no longer a valid non-Mono split"
        )
        assertFileContains(
            "docs/specs/base-runtime-unification.md",
            "SIM` naming may remain temporarily in code/docs, but it must not be used as permission to fork non-Mono behavior"
        )
        assertFileContains(
            "docs/cerb/interface-map.md",
            "best available **base-runtime baseline** for non-Mono work"
        )
        assertFileContains(
            "docs/cerb/interface-map.md",
            "do not create a second non-Mono product truth"
        )
        assertFileContains(
            "docs/plans/tracker.md",
            "Future non-Mono work must not reintroduce separate SIM-vs-full product truth."
        )
        assertFileContains(
            "docs/specs/prism-ui-ux-contract.md",
            "best available base-runtime baseline rather than as permission for a second product truth"
        )
    }

    @Test
    fun `sim baseline docs keep standalone boundaries without becoming second product truth`() {
        assertHeadContains(
            "docs/to-cerb/sim-standalone-prototype/concept.md",
            "best available **base-runtime baseline**",
            headLineCount = 48
        )
        assertHeadContains(
            "docs/to-cerb/sim-standalone-prototype/concept.md",
            "second non-Mono product line",
            headLineCount = 48
        )
        assertHeadContains(
            "docs/to-cerb/sim-standalone-prototype/mental-model.md",
            "best available **base-runtime baseline**",
            headLineCount = 40
        )
        assertHeadContains(
            "docs/plans/sim-tracker.md",
            "best available **base-runtime baseline**",
            headLineCount = 24
        )
        assertHeadContains(
            "docs/cerb/sim-shell/spec.md",
            "base-runtime shell baseline",
            headLineCount = 48
        )
        assertHeadContains(
            "docs/cerb/sim-shell/spec.md",
            "second non-Mono shell truth",
            headLineCount = 48
        )
        assertHeadContains(
            "docs/cerb/sim-scheduler/spec.md",
            "base-runtime scheduler baseline",
            headLineCount = 42
        )
        assertHeadContains(
            "docs/cerb/sim-audio-chat/spec.md",
            "base-runtime audio/chat baseline",
            headLineCount = 42
        )
    }

    private fun assertFileContains(relativePath: String, expected: String) {
        val file = resolvePath(relativePath)
        assertTrue("Missing file: ${file.absolutePath}", file.exists())
        assertTrue(
            "Expected '$expected' in $relativePath",
            file.readText().contains(expected)
        )
    }

    private fun assertHeadContains(
        relativePath: String,
        expected: String,
        headLineCount: Int
    ) {
        val file = resolvePath(relativePath)
        assertTrue("Missing file: ${file.absolutePath}", file.exists())
        val head = file.readLines().take(headLineCount).joinToString("\n")
        assertTrue(
            "Expected '$expected' in the first $headLineCount lines of $relativePath",
            head.contains(expected)
        )
    }

    private fun resolvePath(relativePath: String): File {
        val candidates = listOf(
            File(workingDir, relativePath),
            File(workingDir, "app-core/$relativePath"),
            File(workingDir.parentFile ?: workingDir, relativePath),
            File(workingDir.parentFile ?: workingDir, "app-core/$relativePath")
        )

        return candidates.firstOrNull { it.exists() } ?: candidates.first()
    }
}
