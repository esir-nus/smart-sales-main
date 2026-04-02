package com.smartsales.prism.ui.sim

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimRuntimeIsolationTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `main activity mounts the unified runtime shell instead of split roots`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/MainActivity.kt")

        assertTrue(source.contains("class MainActivity"))
        assertTrue(source.contains("RuntimeShell("))
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
    fun `runtime shell owns the production runtime collaborators directly`() {
        val host = readSource("app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt")
        val content = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt")

        assertTrue(host.contains("val chatViewModel: SimAgentViewModel = hiltViewModel()"))
        assertTrue(host.contains("val audioViewModel: SimAudioDrawerViewModel = hiltViewModel()"))
        assertTrue(host.contains("RuntimeShellContent("))
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
