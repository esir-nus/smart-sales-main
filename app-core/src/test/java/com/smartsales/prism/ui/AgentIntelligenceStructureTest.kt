package com.smartsales.prism.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentIntelligenceStructureTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `host file is reduced to host responsibilities`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt")

        assertTrue(source.contains("fun AgentIntelligenceScreen("))
        assertFalse(source.contains("fun AgentIntelligenceContent("))
        assertFalse(source.contains("fun ChatTimeline("))
        assertFalse(source.contains("fun SimConversationTimeline("))
        assertFalse(source.contains("fun HomeHeroDashboard("))
        assertFalse(source.contains("@Preview("))
        assertFalse(source.contains("hiltViewModel<AgentViewModel>()"))
        assertFalse(source.contains("viewModel as? SimAgentViewModel"))
    }

    @Test
    fun `shared surfaces no longer default to legacy runtime owners`() {
        val screen = readSource("app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt")
        val schedulerDrawer = readSource("app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt")

        assertTrue(screen.contains("viewModel: IAgentViewModel"))
        assertTrue(schedulerDrawer.contains("viewModel: ISchedulerViewModel"))
        assertFalse(screen.contains("viewModel: IAgentViewModel ="))
        assertFalse(schedulerDrawer.contains("viewModel: ISchedulerViewModel ="))
        assertFalse(schedulerDrawer.contains("hiltViewModel<SchedulerViewModel>()"))
    }

    @Test
    fun `wave1b extracted files own the moved structure`() {
        val content = readSource("app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceContent.kt")
        val timeline = readSource("app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceChatTimeline.kt")
        val sections = readSource("app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceHomeSections.kt")
        val preview = readSource("app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligencePreview.kt")
        val sim = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentIntelligenceContent.kt")

        assertTrue(content.contains("fun AgentIntelligenceContent("))
        assertTrue(content.contains("SimAgentIntelligenceContent("))
        assertTrue(timeline.contains("fun ChatTimeline("))
        assertTrue(sections.contains("fun ProMaxHeader("))
        assertTrue(sections.contains("fun HomeHeroDashboard("))
        assertTrue(preview.contains("fun PreviewAgentIntelligence_Idle()"))
        assertTrue(sim.contains("fun SimAgentIntelligenceContent("))
        assertTrue(sim.contains("fun resolveSimDynamicIslandIndex("))
        assertTrue(sim.contains("fun SimConversationTimeline("))
        assertTrue(sim.contains("SIM_INPUT_BAR_TEST_TAG"))
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
