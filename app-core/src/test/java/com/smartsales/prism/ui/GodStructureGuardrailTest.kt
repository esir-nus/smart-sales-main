package com.smartsales.prism.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GodStructureGuardrailTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    private data class PilotExpectation(
        val budget: Int,
        val status: String
    )

    private val pilotExpectations = linkedMapOf(
        "app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt" to
            PilotExpectation(budget = 550, status = "Accepted"),
        "app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt" to
            PilotExpectation(budget = 550, status = "Accepted"),
        "app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt" to
            PilotExpectation(budget = 650, status = "Exception"),
        "app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt" to
            PilotExpectation(budget = 650, status = "Exception")
    )

    @Test
    fun `pilot tracker exceptions stay valid against structure contract`() {
        val trackerFile = resolvePath("docs/plans/god-tracker.md")
        val contractFile = resolvePath("docs/specs/code-structure-contract.md")

        assertTrue("Missing god tracker: ${trackerFile.absolutePath}", trackerFile.exists())
        assertTrue("Missing structure contract: ${contractFile.absolutePath}", contractFile.exists())

        val rowsByFile = parseTrackedFileRows(trackerFile)

        pilotExpectations.forEach { (filePath, expectation) ->
            val row = rowsByFile[filePath]
                ?: error("Missing pilot exception row for $filePath in ${trackerFile.path}")

            requireValidPilotField(filePath, row, "Target Decomposition")
            requireValidPilotField(filePath, row, "Owner")
            requireValidPilotField(filePath, row, "Required Tests")
            requireValidPilotField(filePath, row, "Status")
            if (expectation.status == "Exception") {
                requireValidPilotField(filePath, row, "Sunset")
            }

            assertEquals(
                "Pilot file has unexpected structural status in docs/plans/god-tracker.md: $filePath",
                expectation.status,
                row["Status"]
            )
        }
    }

    @Test
    fun `pilot files above budget must keep valid tracker exceptions`() {
        val rowsByFile = parseTrackedFileRows(resolvePath("docs/plans/god-tracker.md"))

        pilotExpectations.forEach { (filePath, expectation) ->
            val sourceFile = resolvePath(filePath)
            assertTrue("Missing pilot source file: ${sourceFile.absolutePath}", sourceFile.exists())

            val loc = sourceFile.readLines().size
            val row = rowsByFile[filePath]
            val validException = row != null && isValidPilotExceptionRow(row)
            val status = row?.get("Status").orEmpty()

            assertTrue(
                buildString {
                    append("Pilot structural budget violation for ")
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

    private fun requireValidPilotField(
        filePath: String,
        row: Map<String, String>,
        column: String
    ) {
        val value = row[column].orEmpty()
        assertTrue(
            "Invalid pilot exception field for $filePath: column=$column, actual='${value.ifBlank { "<blank>" }}'",
            isMeaningfulCell(value)
        )
    }

    private fun isValidPilotExceptionRow(row: Map<String, String>): Boolean {
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
