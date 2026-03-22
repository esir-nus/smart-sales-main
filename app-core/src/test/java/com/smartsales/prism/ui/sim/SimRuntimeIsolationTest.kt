package com.smartsales.prism.ui.sim

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimRuntimeIsolationTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `sim main activity mounts sim shell instead of smart shell root`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/SimMainActivity.kt")

        assertTrue(source.contains("class SimMainActivity"))
        assertTrue(source.contains("SimShell(badgeAudioPipeline = badgeAudioPipeline)"))
        assertFalse(source.contains("AgentShell("))
        assertFalse(source.contains("AgentMainActivity"))
    }

    @Test
    fun `sim shell owns sim runtime collaborators instead of smart chat root`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt")

        assertTrue(source.contains("val chatViewModel: SimAgentViewModel = hiltViewModel()"))
        assertTrue(source.contains("val audioViewModel: SimAudioDrawerViewModel = hiltViewModel()"))
        assertTrue(source.contains("AgentIntelligenceScreen("))
        assertFalse(source.contains("AgentShell("))
        assertFalse(source.contains("val chatViewModel: AgentViewModel"))
        assertFalse(source.contains("hiltViewModel<AgentViewModel>"))
        assertFalse(source.contains("PrismModule"))
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
