package com.smartsales.prism.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseRuntimeShellCoreStructureTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `shared shell core owns common base runtime routing rules`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/shell/BaseRuntimeShellCore.kt")

        assertTrue(source.contains("data class BaseRuntimeShellCoreState("))
        assertTrue(source.contains("enum class BaseRuntimeDrawerType"))
        assertTrue(source.contains("fun openBaseRuntimeScheduler("))
        assertTrue(source.contains("fun openBaseRuntimeHistory("))
        assertTrue(source.contains("fun openBaseRuntimeAudioDrawer("))
        assertTrue(source.contains("fun openBaseRuntimeConnectivityModal("))
        assertTrue(source.contains("fun openBaseRuntimeConnectivitySetup("))
        assertTrue(source.contains("fun openBaseRuntimeConnectivityManager("))
        assertTrue(source.contains("fun openBaseRuntimeSettings("))
        assertTrue(source.contains("fun closeBaseRuntimeOverlays("))
        assertTrue(source.contains("fun shouldShowBaseRuntimeScrim("))
    }

    @Test
    fun `unified runtime shell adapts through the shared shell core`() {
        val reducer = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt")

        assertTrue(reducer.contains("toBaseRuntimeShellCoreState()"))
        assertTrue(reducer.contains("openBaseRuntimeScheduler("))
        assertTrue(reducer.contains("openBaseRuntimeHistory("))
        assertTrue(reducer.contains("openBaseRuntimeAudioDrawer("))
        assertTrue(reducer.contains("openBaseRuntimeConnectivityModal("))
        assertTrue(reducer.contains("openBaseRuntimeConnectivitySetup("))
        assertTrue(reducer.contains("openBaseRuntimeConnectivityManager("))
        assertTrue(reducer.contains("openBaseRuntimeSettings("))
        assertTrue(reducer.contains("resolveBaseRuntimeScrimAlpha("))
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
