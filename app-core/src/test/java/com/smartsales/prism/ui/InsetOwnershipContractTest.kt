package com.smartsales.prism.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsetOwnershipContractTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `runtime shell no longer owns the global top status bar inset`() {
        val host = readSource("app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt")
        val content = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt")

        assertFalse(host.contains(".statusBarsPadding()"))
        assertFalse(content.contains(".statusBarsPadding()"))
        assertTrue(content.contains("AgentIntelligenceScreen("))
    }

    @Test
    fun `scheduler drawer keeps separate standard and sim top inset patterns`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt")

        assertTrue(source.contains(".prismStatusBarPadding()"))
        assertTrue(source.contains(".prismMonolithTopInsetPadding(SimHomeHeroTokens.HeaderHeight)"))
    }

    @Test
    fun `top safe band surfaces use surface owned inset helpers`() {
        val simHistory = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimHistoryDrawer.kt")
        val simSettings = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimUserCenterDrawer.kt")
        val userCenter = readSource("app-core/src/main/java/com/smartsales/prism/ui/settings/UserCenterScreen.kt")
        val topSafeArea = readSource("app-core/src/main/java/com/smartsales/prism/ui/components/PrismTopSafeArea.kt")

        assertTrue(topSafeArea.contains("fun PrismStatusBarTopSafeArea("))
        assertTrue(simHistory.contains("PrismStatusBarTopSafeArea()"))
        assertTrue(simSettings.contains("PrismStatusBarTopSafeArea()"))
        assertTrue(userCenter.contains("PrismStatusBarTopSafeArea()"))
    }

    @Test
    fun `bottom drawers own navigation bar clearance`() {
        val simAudio = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawer.kt")
        val audio = readSource("app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioDrawer.kt")

        assertTrue(simAudio.contains(".prismNavigationBarPadding()"))
        assertTrue(audio.contains(".prismNavigationBarPadding()"))
    }

    @Test
    fun `sim shell content uses shared inset helpers instead of raw inset modifiers`() {
        val simContent = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentIntelligenceContent.kt")
        val simHomeHero = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimHomeHeroShell.kt")

        assertFalse(simContent.contains(".statusBarsPadding()"))
        assertFalse(simContent.contains(".navigationBarsPadding()"))
        assertTrue(simContent.contains(".prismStatusBarPadding()"))
        assertTrue(simContent.contains(".prismNavigationBarPadding()"))
        assertFalse(simHomeHero.contains(".navigationBarsPadding()"))
        assertTrue(simHomeHero.contains(".prismNavigationBarPadding()"))
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
