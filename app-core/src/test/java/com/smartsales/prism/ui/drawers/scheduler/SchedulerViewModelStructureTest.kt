package com.smartsales.prism.ui.drawers.scheduler

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerViewModelStructureTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `scheduler host no longer contains manual refresh trigger plumbing`() {
        val host = readSource("app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt")
        val projection = readSource("app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModelProjectionSupport.kt")
        val legacyActions = readSource("app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModelLegacyActions.kt")

        assertFalse(host.contains("_refreshTrigger"))
        assertFalse(host.contains("refreshTrigger ="))
        assertFalse(projection.contains("refreshTrigger:"))
        assertFalse(legacyActions.contains("emitRefresh"))
        assertTrue(projection.contains("activeDayOffset.flatMapLatest"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File(workingDir, relativePath),
            File(workingDir, "app-core/$relativePath"),
            File(workingDir.parentFile ?: workingDir, relativePath),
            File(workingDir.parentFile ?: workingDir, "app-core/$relativePath")
        )

        return candidates.firstOrNull { it.exists() }?.readText()
            ?: error("Source file not found for $relativePath from ${workingDir.absolutePath}")
    }
}
