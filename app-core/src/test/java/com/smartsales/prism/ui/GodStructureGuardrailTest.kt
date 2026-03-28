package com.smartsales.prism.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GodStructureGuardrailTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    private data class GuardrailExpectation(
        val budget: Int,
        val status: String
    )

    private val trackedExpectations = linkedMapOf(
        "app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt" to
            GuardrailExpectation(budget = 550, status = "Accepted"),
        "app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt" to
            GuardrailExpectation(budget = 550, status = "Accepted"),
        "app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt" to
            GuardrailExpectation(budget = 650, status = "Accepted"),
        "app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt" to
            GuardrailExpectation(budget = 650, status = "Accepted"),
        "app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawer.kt" to
            GuardrailExpectation(budget = 550, status = "Accepted"),
        "app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGateway.kt" to
            GuardrailExpectation(budget = 650, status = "Accepted"),
        "app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt" to
            GuardrailExpectation(budget = 650, status = "Accepted"),
        "app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt" to
            GuardrailExpectation(budget = 650, status = "Accepted"),
        "domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinter.kt" to
            GuardrailExpectation(budget = 650, status = "Accepted")
    )

    @Test
    fun `tracked guardrail rows stay valid against structure contract`() {
        val trackerFile = resolvePath("docs/plans/god-tracker.md")
        val contractFile = resolvePath("docs/specs/code-structure-contract.md")

        assertTrue("Missing god tracker: ${trackerFile.absolutePath}", trackerFile.exists())
        assertTrue("Missing structure contract: ${contractFile.absolutePath}", contractFile.exists())

        val rowsByFile = parseTrackedFileRows(trackerFile)

        trackedExpectations.forEach { (filePath, expectation) ->
            val row = rowsByFile[filePath]
                ?: error("Missing guardrail row for $filePath in ${trackerFile.path}")

            requireValidField(filePath, row, "Target Decomposition")
            requireValidField(filePath, row, "Owner")
            requireValidField(filePath, row, "Required Tests")
            requireValidField(filePath, row, "Status")
            if (expectation.status == "Exception") {
                requireValidField(filePath, row, "Sunset")
            }

            assertEquals(
                "Tracked file has unexpected structural status in docs/plans/god-tracker.md: $filePath",
                expectation.status,
                row["Status"]
            )
        }
    }

    @Test
    fun `tracked files above budget must keep valid tracker exceptions`() {
        val rowsByFile = parseTrackedFileRows(resolvePath("docs/plans/god-tracker.md"))

        trackedExpectations.forEach { (filePath, expectation) ->
            val sourceFile = resolvePath(filePath)
            assertTrue("Missing tracked source file: ${sourceFile.absolutePath}", sourceFile.exists())

            val loc = sourceFile.readLines().size
            val row = rowsByFile[filePath]
            val validException = row != null && isValidExceptionRow(row)
            val status = row?.get("Status").orEmpty()

            assertTrue(
                buildString {
                    append("Tracked structural budget violation for ")
                    append(filePath)
                    append(": measured LOC=")
                    append(loc)
                    append(", budget=")
                    append(expectation.budget)
                    append(", exception=")
                    append(
                        when {
                            row == null -> "missing"
                            validException -> "valid"
                            else -> "invalid"
                        }
                    )
                },
                when (status) {
                    "Accepted" -> loc <= expectation.budget
                    "Exception" -> loc <= expectation.budget || validException
                    else -> loc <= expectation.budget
                }
            )
        }
    }

    private fun requireValidField(
        filePath: String,
        row: Map<String, String>,
        column: String
    ) {
        val value = row[column].orEmpty()
        assertTrue(
            "Invalid tracked exception field for $filePath: column=$column, actual='${value.ifBlank { "<blank>" }}'",
            isMeaningfulCell(value)
        )
    }

    private fun isValidExceptionRow(row: Map<String, String>): Boolean {
        return listOf(
            "Target Decomposition",
            "Owner",
            "Sunset",
            "Required Tests",
            "Status"
        ).all { key -> isMeaningfulCell(row[key].orEmpty()) } &&
            row["Status"] == "Exception"
    }

    private fun isMeaningfulCell(value: String): Boolean {
        return value.isNotBlank() && value != "—"
    }

    private fun parseTrackedFileRows(trackerFile: File): Map<String, Map<String, String>> {
        val lines = trackerFile.readLines()
        val trackedFilesIndex = lines.indexOfFirst { it.trim() == "## Tracked Files" }
        check(trackedFilesIndex >= 0) {
            "Tracked Files section missing in ${trackerFile.path}"
        }

        val sectionLines = lines.drop(trackedFilesIndex + 1)
        val firstTableLineIndex = sectionLines.indexOfFirst { it.trimStart().startsWith("|") }
        check(firstTableLineIndex >= 0) {
            "Tracked Files table missing in ${trackerFile.path}"
        }

        val tableLines = sectionLines.drop(firstTableLineIndex)
            .takeWhile { it.trimStart().startsWith("|") }

        check(tableLines.size >= 3) {
            "Tracked Files table missing or incomplete in ${trackerFile.path}"
        }

        val headers = parseMarkdownCells(tableLines.first())
        val rows = tableLines.drop(2)
            .filter { it.contains("|") }
            .map { rowLine ->
                val cells = parseMarkdownCells(rowLine)
                headers.mapIndexed { index, header ->
                    header to cells.getOrElse(index) { "" }
                }.toMap()
            }

        return rows.associateBy { row ->
            row["File"].orEmpty()
        }
    }

    private fun parseMarkdownCells(line: String): List<String> {
        return line.trim()
            .removePrefix("|")
            .removeSuffix("|")
            .split("|")
            .map { cell ->
                cell.trim().removePrefix("`").removeSuffix("`")
            }
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
