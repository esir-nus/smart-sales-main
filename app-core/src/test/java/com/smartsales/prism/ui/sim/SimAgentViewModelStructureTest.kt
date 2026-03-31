package com.smartsales.prism.ui.sim

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimAgentViewModelStructureTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `sim agent view model host file is reduced to seam and delegation`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt")

        assertTrue(source.contains("class SimAgentViewModel @Inject constructor("))
        assertTrue(source.contains("private val sessionCoordinator = SimAgentSessionCoordinator("))
        assertTrue(source.contains("private val chatCoordinator = SimAgentChatCoordinator("))
        assertTrue(source.contains("private val followUpCoordinator = SimAgentFollowUpCoordinator("))
        assertTrue(source.contains("private val voiceDraftCoordinator = SimAgentVoiceDraftCoordinator("))

        assertFalse(source.contains("private suspend fun handleGeneralSend("))
        assertFalse(source.contains("private suspend fun handleAudioGroundedSend("))
        assertFalse(source.contains("private suspend fun handleSchedulerFollowUpReschedule("))
        assertFalse(source.contains("private fun buildGeneralChatPrompt("))
        assertFalse(source.contains("private fun buildAudioGroundedPrompt("))
        assertFalse(source.contains("private fun emitSchedulerFollowUpTelemetry("))
        assertFalse(source.contains("private fun normalizeDuplicateAudioLinks("))
        assertFalse(source.contains("private fun startVoiceDraft("))
        assertFalse(source.contains("private fun beginVoiceDraftProcessing("))
        assertFalse(source.contains("private suspend fun resolveVoiceDraftResult("))
        assertFalse(source.contains("private fun handleVoiceDraftEvent("))
    }

    @Test
    fun `wave1d and follow-up files own moved session chat follow-up and voice draft responsibilities`() {
        val session = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentSessionCoordinator.kt")
        val chat = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt")
        val followUp = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentFollowUpCoordinator.kt")
        val voiceDraft = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentVoiceDraftCoordinator.kt")

        assertTrue(session.contains("internal class SimAgentSessionCoordinator("))
        assertTrue(session.contains("fun loadPersistedSessions()"))
        assertTrue(session.contains("fun reconcileAudioBindings()"))
        assertTrue(session.contains("fun appendAiMessage("))

        assertTrue(chat.contains("internal class SimAgentChatCoordinator("))
        assertTrue(chat.contains("fun selectAudioForChat("))
        assertTrue(chat.contains("suspend fun handleGeneralSend("))
        assertTrue(chat.contains("suspend fun handleAudioGroundedSend("))
        assertTrue(chat.contains("private fun buildGeneralChatPrompt("))

        assertTrue(followUp.contains("internal class SimAgentFollowUpCoordinator("))
        assertTrue(followUp.contains("fun createBadgeSchedulerFollowUpSession("))
        assertTrue(followUp.contains("suspend fun handleSchedulerFollowUpInput("))
        assertTrue(followUp.contains("private suspend fun handleSchedulerFollowUpReschedule("))
        assertTrue(followUp.contains("private fun emitSchedulerFollowUpTelemetry("))

        assertTrue(voiceDraft.contains("internal class SimAgentVoiceDraftCoordinator("))
        assertTrue(voiceDraft.contains("fun startVoiceDraft()"))
        assertTrue(voiceDraft.contains("fun finishVoiceDraft()"))
        assertTrue(voiceDraft.contains("fun cancelVoiceDraft()"))
        assertTrue(voiceDraft.contains("fun handleVoiceDraftEvent("))
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
