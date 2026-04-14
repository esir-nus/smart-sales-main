package com.smartsales.prism.ui.sim

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimAudioDrawerStructureTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `sim audio drawer host file is reduced to seam and delegation`() {
        val host = readSource(
            "app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawer.kt"
        )

        assertTrue(host.contains("fun SimAudioDrawer("))
        assertTrue(host.contains("SimAudioDrawerContent("))
        assertTrue(host.contains("connectionState = connectionState"))
        assertTrue(host.contains("syncFeedback = syncFeedback.value"))
        assertTrue(host.contains("viewModel.confirmBadgeDelete("))

        assertFalse(host.contains("private fun SimDrawerHeaderAction("))
        assertFalse(host.contains("private fun SimTestImportButton("))
        assertFalse(host.contains("internal fun SimAudioCard("))
        assertFalse(host.contains("internal fun buildSimAudioSelectBodyText("))
        assertFalse(host.contains("private fun SimArtifactSections("))
        assertFalse(host.contains("private fun buildProviderAdjacentSection("))
    }

    @Test
    fun `wave3a extracted files own drawer content card and support roles`() {
        val content = readSource(
            "app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerContent.kt"
        )
        val card = readSource(
            "app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerCard.kt"
        )
        val support = readSource(
            "app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerSupport.kt"
        )

        assertTrue(content.contains("internal fun SimAudioDrawerContent("))
        assertTrue(content.contains("private fun SimAudioBrowseHeader("))
        assertTrue(content.contains("internal fun SimAudioBrowseGrip("))
        assertTrue(content.contains("private fun SimAudioSmartCapsule("))
        assertTrue(content.contains("private fun SimTestImportButton("))
        assertFalse(content.contains("SettingsInputAntenna"))

        assertTrue(card.contains("internal fun SimAudioCard("))
        assertTrue(card.contains("SimArtifactContent(artifacts = artifacts!!)"))
        assertTrue(card.contains("internal fun canSwipeRightToTranscribe("))
        assertTrue(card.contains("internal fun canSwipeLeftToDelete("))
        assertTrue(card.contains("private fun SimAudioCompactPreviewRow("))
        assertTrue(card.contains("private fun SimBrowseModeSwipePrompt("))
        assertFalse(card.contains("private fun SimArtifactSections("))

        assertTrue(support.contains("data class SimChatAudioSelection("))
        assertTrue(support.contains("internal fun buildSimAudioSelectBodyText("))
        assertTrue(support.contains("internal fun buildSimAudioTranscriptPreview("))
        assertTrue(support.contains("internal fun buildTransparentStateLabel("))
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
