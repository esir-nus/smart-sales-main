package com.smartsales.prism.ui.sim

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimSchedulerViewModelStructureTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `sim scheduler view model host file is reduced to seam and delegation`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt")

        assertTrue(source.contains("class SimSchedulerViewModel @Inject constructor("))
        assertTrue(source.contains("private val projectionSupport = SimSchedulerProjectionSupport("))
        assertTrue(source.contains("private val reminderSupport = SimSchedulerReminderSupport("))
        assertTrue(source.contains("private val mutationCoordinator = SimSchedulerMutationCoordinator("))
        assertTrue(source.contains("private val ingressCoordinator = SimSchedulerIngressCoordinator("))

        assertFalse(source.contains("private suspend fun processTranscript("))
        assertFalse(source.contains("private suspend fun handleMultiTaskCreate("))
        assertFalse(source.contains("private suspend fun handleMutation("))
        assertFalse(source.contains("private suspend fun executeResolvedReschedule("))
        assertFalse(source.contains("private suspend fun scheduleReminderIfExact("))
        assertFalse(source.contains("private fun buildRescheduleExitMotion("))
        assertFalse(source.contains("private fun resolveMultiTaskFragment("))
    }

    @Test
    fun `wave1e extracted files own scheduler ingress mutation reminder and projection responsibilities`() {
        val ingress = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerIngressCoordinator.kt")
        val mutation = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerMutationCoordinator.kt")
        val reminder = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerReminderSupport.kt")
        val projection = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerProjectionSupport.kt")

        assertTrue(ingress.contains("internal class SimSchedulerIngressCoordinator("))
        assertTrue(ingress.contains("suspend fun processTranscript("))
        assertTrue(ingress.contains("private suspend fun handleMultiTaskCreate("))
        assertTrue(ingress.contains("private suspend fun handleVoiceRescheduleTranscript("))
        assertTrue(ingress.contains("private fun resolveMultiTaskFragment("))

        assertTrue(mutation.contains("internal class SimSchedulerMutationCoordinator("))
        assertTrue(mutation.contains("suspend fun handleMutation("))
        assertTrue(mutation.contains("suspend fun executeCreateIntent("))
        assertTrue(mutation.contains("suspend fun executeResolvedReschedule("))

        assertTrue(reminder.contains("internal class SimSchedulerReminderSupport("))
        assertTrue(reminder.contains("suspend fun scheduleReminderIfExact("))
        assertTrue(reminder.contains("suspend fun cancelReminderSafely("))

        assertTrue(projection.contains("internal class SimSchedulerProjectionSupport("))
        assertTrue(projection.contains("fun buildTopUrgentTasks("))
        assertTrue(projection.contains("fun buildTimelineItems("))
        assertTrue(projection.contains("fun emitStatus("))
        assertTrue(projection.contains("fun buildRescheduleExitMotion("))
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
