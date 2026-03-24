package com.smartsales.prism.domain.scheduler

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerLinterStructureTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `scheduler linter host file is reduced to seam and delegation`() {
        val source = readSource(
            "domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinter.kt"
        )

        assertTrue(source.contains("class SchedulerLinter @Inject constructor()"))
        assertTrue(source.contains("private val parsingSupport = SchedulerLinterParsingSupport("))
        assertTrue(source.contains("private val legacySupport = SchedulerLinterLegacySupport("))

        assertFalse(source.contains("private fun rejectFabricatedExactTime("))
        assertFalse(source.contains("private fun parseUniMFragment("))
        assertFalse(source.contains("private fun parseSingleTaskLegacy("))
        assertFalse(source.contains("private fun parseDateTimeLegacy("))
        assertFalse(source.contains("sealed class LintResult"))
    }

    @Test
    fun `wave2a extracted files own parsing support utilities and legacy compatibility`() {
        val parsing = readSource(
            "domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterParsingSupport.kt"
        )
        val support = readSource(
            "domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterSupport.kt"
        )
        val legacySupport = readSource(
            "domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterLegacySupport.kt"
        )
        val legacyContracts = readSource(
            "domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterLegacyContracts.kt"
        )

        assertTrue(parsing.contains("internal class SchedulerLinterParsingSupport("))
        assertTrue(parsing.contains("fun parseFastTrackIntent("))
        assertTrue(parsing.contains("fun parseUniAExtraction("))
        assertTrue(parsing.contains("fun parseUniMExtraction("))
        assertTrue(parsing.contains("private fun parseUniMFragment("))
        assertTrue(parsing.contains("fun parseFollowUpRescheduleExtraction("))

        assertTrue(support.contains("internal fun schedulerLinterNormalizeUrgency("))
        assertTrue(support.contains("internal fun schedulerLinterParseTaskDefinition("))
        assertTrue(support.contains("internal fun schedulerLinterParseDuration("))
        assertTrue(support.contains("internal fun schedulerLinterNormalizeTime("))

        assertTrue(legacySupport.contains("internal class SchedulerLinterLegacySupport("))
        assertTrue(legacySupport.contains("fun lint("))
        assertTrue(legacySupport.contains("private fun parseSingleTaskLegacy("))
        assertTrue(legacySupport.contains("private fun parseDateTimeLegacy("))

        assertTrue(legacyContracts.contains("data class ParsedClues("))
        assertTrue(legacyContracts.contains("data class ParsedProfileMutation("))
        assertTrue(legacyContracts.contains("sealed class LintResult"))
    }

    private fun readSource(relativePath: String): String {
        val parent = workingDir.parentFile
        val grandParent = parent?.parentFile
        val candidates = listOf(
            File(workingDir, relativePath),
            File(parent ?: workingDir, relativePath),
            File(grandParent ?: workingDir, relativePath)
        )

        return candidates.firstOrNull { it.exists() }?.readText()
            ?: error("Source file not found for $relativePath from ${workingDir.absolutePath}")
    }
}
