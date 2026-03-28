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
        assertTrue(source.contains("SimShell("))
        assertTrue(source.contains("badgeAudioPipeline = badgeAudioPipeline"))
        assertTrue(source.contains("ThemePreferenceStore"))
        assertTrue(source.contains("themePreferenceStore.themeMode.collectAsStateWithLifecycle()"))
        assertTrue(source.contains("resolvePrismDarkTheme("))
        assertTrue(source.contains("PrismSystemBarsEffect("))
        assertFalse(source.contains("PrismTheme(darkTheme = true)"))
        assertFalse(source.contains("AgentShell("))
        assertFalse(source.contains("AgentMainActivity"))
    }

    @Test
    fun `sim shell owns sim runtime collaborators instead of smart chat root`() {
        val host = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt")
        val content = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellContent.kt")

        assertTrue(host.contains("val chatViewModel: SimAgentViewModel = hiltViewModel()"))
        assertTrue(host.contains("val audioViewModel: SimAudioDrawerViewModel = hiltViewModel()"))
        assertTrue(host.contains("SimShellContent("))
        assertTrue(content.contains("AgentIntelligenceScreen("))
        assertFalse(host.contains("AgentShell("))
        assertFalse(content.contains("AgentShell("))
        assertFalse(host.contains("val chatViewModel: AgentViewModel"))
        assertFalse(host.contains("hiltViewModel<AgentViewModel>"))
        assertFalse(host.contains("PrismModule"))
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
