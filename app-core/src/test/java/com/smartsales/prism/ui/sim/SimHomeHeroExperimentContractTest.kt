package com.smartsales.prism.ui.sim

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimHomeHeroExperimentContractTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `sim hero shell frame stays shared across idle and active sim chat`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentIntelligenceContent.kt")

        assertTrue(source.contains("SIM_ENABLE_SHARED_HOME_HERO_SHELL"))
        assertTrue(source.contains("showSimSharedHomeHeroShell = SIM_ENABLE_SHARED_HOME_HERO_SHELL"))
        assertTrue(source.contains("SimHomeHeroShellFrame("))
        assertTrue(source.contains("if (history.isEmpty()) {"))
        assertTrue(source.contains("enableSimSchedulerPullGesture"))
        assertTrue(source.contains("enableSimAudioPullGesture"))
    }

    @Test
    fun `sim home hero shell keeps prototype subtitle monolith composer and shared center stage`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimHomeHeroShell.kt")
        val tokenSource = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimHomeHeroTokens.kt")
        val gestureSource = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimDrawerGestures.kt")

        assertTrue(source.contains("SimHomeHeroTokens"))
        assertTrue(source.contains("Canvas(modifier = modifier.fillMaxSize())"))
        assertTrue(source.contains("SIM_INPUT_BAR_TEST_TAG"))
        assertTrue(source.contains("BoxWithConstraints("))
        assertTrue(source.contains("statusBarsPadding()"))
        assertTrue(source.contains("SimHomeHeroTopCap("))
        assertTrue(source.contains("SimVerticalDragTrigger("))
        assertTrue(tokenSource.contains("val HeaderHeight = 64.dp"))
        assertTrue(tokenSource.contains("val BottomMonolithHeight = 56.dp"))
        assertTrue(tokenSource.contains("val IslandMaxWidth = 240.dp"))
        assertTrue(tokenSource.contains("val CenterCanvasHorizontalPadding = 16.dp"))
        assertTrue(source.contains("internal fun SimHomeHeroCenterStage("))
        assertTrue(source.contains("heightIn(min = SimHomeHeroTokens.BottomMonolithHeight)"))
        assertTrue(!gestureSource.contains(".zIndex(PrismElevation.Handles)"))
    }

    @Test
    fun `sim empty home greeting stays prototype aligned inside the shared shell`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimHomeHeroShell.kt")
        val contentSource = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentIntelligenceContent.kt")

        assertTrue(source.contains("SIM_EMPTY_HOME_GREETING = \"你好, SmartSales 用户\""))
        assertTrue(contentSource.contains("greeting = SIM_EMPTY_HOME_GREETING"))
        assertFalse(contentSource.contains("greeting = heroGreeting"))
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
