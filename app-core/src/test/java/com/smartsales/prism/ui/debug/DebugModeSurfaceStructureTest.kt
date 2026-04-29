package com.smartsales.prism.ui.debug

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugModeSurfaceStructureTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `runtime debug mode gates debug surfaces and global scrim`() {
        val runtimeShell = readSource("app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt")
        val simShell = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt")
        val content = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt")
        val schedulerDrawer = readSource("app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt")
        val connectivityModal = readSource("app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt")

        assertTrue(runtimeShell.contains("debugModeEnabled by connectivityViewModel.debugModeEnabled.collectAsStateWithLifecycle()"))
        assertTrue(runtimeShell.contains("debugModeEnabled = debugModeEnabled"))
        assertTrue(simShell.contains("debugModeEnabled by connectivityViewModel.debugModeEnabled.collectAsStateWithLifecycle()"))
        assertTrue(simShell.contains("debugModeEnabled = debugModeEnabled"))

        assertTrue(content.contains("val showDebugSurfaces = BuildConfig.DEBUG && debugModeEnabled"))
        assertTrue(content.contains("showDebugButton = showDebugSurfaces"))
        assertTrue(content.contains("showDebugControls = showDebugSurfaces"))
        assertTrue(content.contains("showTestImportAction = showDebugSurfaces"))
        assertTrue(content.contains("DEBUG_MODE_GLOBAL_SCRIM_TEST_TAG"))
        assertTrue(content.contains("Color.Gray.copy(alpha = 0.14f)"))

        assertTrue(schedulerDrawer.contains("showDebugControls: Boolean = com.smartsales.prism.BuildConfig.DEBUG"))
        assertTrue(schedulerDrawer.contains("com.smartsales.prism.BuildConfig.DEBUG && showDebugControls"))
        assertTrue(connectivityModal.contains("BuildConfig.DEBUG && debugModeEnabled"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File(workingDir, relativePath),
            File(workingDir.parentFile ?: workingDir, relativePath)
        )

        return candidates.firstOrNull { it.exists() }?.readText()
            ?: error("Source file not found for $relativePath from ${workingDir.absolutePath}")
    }
}
