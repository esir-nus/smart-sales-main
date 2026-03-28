package com.smartsales.prism.ui.sim

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimShellStructureTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `sim shell host file is reduced to host responsibilities`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt")

        assertTrue(source.contains("fun SimShell("))
        assertTrue(source.contains("val chatViewModel: SimAgentViewModel = hiltViewModel()"))
        assertTrue(source.contains("val audioViewModel: SimAudioDrawerViewModel = hiltViewModel()"))
        assertTrue(source.contains("SimShellContent("))
        assertFalse(source.contains("fun emitSimConnectivityRouteTelemetry("))
        assertFalse(source.contains("fun openSimConnectivityModal("))
        assertFalse(source.contains("fun buildSimDynamicIslandItems("))
        assertFalse(source.contains("fun SimSchedulerFollowUpPrompt("))
        assertFalse(source.contains("AgentIntelligenceScreen("))
    }

    @Test
    fun `wave1c extracted files own the moved shell structure`() {
        val content = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellContent.kt")
        val reducer = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellReducer.kt")
        val actions = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellActions.kt")
        val telemetry = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellTelemetry.kt")
        val projection = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellProjection.kt")
        val sections = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellSections.kt")

        assertTrue(content.contains("fun SimShellContent("))
        assertTrue(content.contains("AgentIntelligenceScreen("))
        assertTrue(content.contains("SimHistoryDrawer("))
        assertTrue(content.contains("SimUserCenterDrawer("))
        assertTrue(content.contains("mutateShellState(::openSimSettings)"))
        assertTrue(content.contains("ConnectivityManagerScreen("))
        assertFalse(content.contains("import com.smartsales.prism.ui.drawers.HistoryDrawer"))
        assertFalse(content.contains("import com.smartsales.prism.ui.settings.UserCenterScreen"))

        assertTrue(reducer.contains("fun openSimConnectivityModal("))
        assertTrue(reducer.contains("fun handleSimConnectivityEntryRequest("))
        assertTrue(reducer.contains("fun openSimHistory("))
        assertTrue(reducer.contains("fun openSimSettings("))
        assertTrue(reducer.contains("fun handleSimHistoryEntryRequest("))
        assertTrue(reducer.contains("fun shouldShowSimShellScrim("))
        assertTrue(reducer.contains("fun resolveSimShellScrimAlpha("))

        assertTrue(actions.contains("fun handleSchedulerShelfAskAiHandoff("))
        assertTrue(actions.contains("fun handleBadgeSchedulerContinuityIngress("))
        assertTrue(actions.contains("fun handleSimSessionDeleteAction("))

        assertTrue(telemetry.contains("fun emitSimConnectivityRouteTelemetry("))
        assertTrue(telemetry.contains("fun emitSimHistoryRouteTelemetry("))
        assertTrue(telemetry.contains("fun emitSimAudioGroundedChatOpenedFromArtifactTelemetry("))

        assertTrue(projection.contains("fun buildSimDynamicIslandItems("))
        assertTrue(sections.contains("fun SimSchedulerFollowUpPrompt("))
        assertTrue(sections.contains("fun SimSchedulerFollowUpActionStrip("))

        val historyDrawer = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimHistoryDrawer.kt")
        val settingsDrawer = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimUserCenterDrawer.kt")
        assertTrue(historyDrawer.contains("combinedClickable("))
        assertFalse(historyDrawer.contains("MoreVert"))
        assertFalse(historyDrawer.contains("ChatBubbleOutline"))
        assertFalse(historyDrawer.contains("formatSimHistoryRecency("))
        assertFalse(historyDrawer.contains("label = { Text(\"摘要\") }"))
        assertFalse(settingsDrawer.contains("Icons.Default.Close"))
        assertFalse(settingsDrawer.contains("IconButton("))
        assertFalse(settingsDrawer.contains("label = \"面容 ID\""))
    }

    @Test
    fun `scheduler page mode hides the sim bottom composer`() {
        val content = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellContent.kt")
        val heroShell = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimHomeHeroShell.kt")

        assertTrue(content.contains("val showSimBottomComposer = shellState.activeDrawer != SimDrawerType.SCHEDULER"))
        assertTrue(content.contains("showSimBottomComposer = showSimBottomComposer"))
        assertTrue(heroShell.contains("if (showBottomComposer) {"))
        assertTrue(heroShell.contains("SimHomeHeroBottomMonolith("))
        assertTrue(heroShell.contains("Spacer("))
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
