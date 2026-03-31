package com.smartsales.prism.data.audio

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimAudioRepositoryStructureTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `sim audio repository host file is reduced to seam and delegation`() {
        val host = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt"
        )

        assertTrue(host.contains("class SimAudioRepository @Inject constructor("))
        assertTrue(host.contains("private val runtime: SimAudioRepositoryRuntime"))
        assertTrue(host.contains("private val storeSupport = SimAudioRepositoryStoreSupport(runtime)"))
        assertTrue(host.contains("private val artifactSupport = SimAudioRepositoryArtifactSupport(runtime, storeSupport)"))
        assertTrue(host.contains("private val syncSupport = SimAudioRepositorySyncSupport(runtime, storeSupport)"))
        assertTrue(host.contains("private val transcriptionSupport = SimAudioRepositoryTranscriptionSupport("))

        assertFalse(host.contains("private fun backfillSeedInventory("))
        assertFalse(host.contains("private suspend fun performBadgeSyncLocked("))
        assertFalse(host.contains("private fun observeTranscription("))
        assertFalse(host.contains("internal fun recoverOrphanedSimTranscriptions("))
        assertFalse(host.contains("internal fun simBadgeSyncSuccessMessage("))
    }

    @Test
    fun `wave2c extracted files own runtime store artifact sync and transcription roles`() {
        val runtime = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryRuntime.kt"
        )
        val store = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryStoreSupport.kt"
        )
        val artifact = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryArtifactSupport.kt"
        )
        val sync = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt"
        )
        val transcription = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryTranscriptionSupport.kt"
        )

        assertTrue(runtime.contains("@Singleton"))
        assertTrue(runtime.contains("class SimAudioRepositoryRuntime @Inject constructor("))
        assertTrue(runtime.contains("val metadataFile = File(context.filesDir, SIM_AUDIO_METADATA_FILENAME)"))
        assertTrue(runtime.contains("val pendingBadgeDeleteFile = File(context.filesDir, SIM_AUDIO_PENDING_BADGE_DELETE_FILENAME)"))
        assertTrue(runtime.contains("val pendingBadgeDeletes = MutableStateFlow<Set<String>>(emptySet())"))
        assertTrue(runtime.contains("val observationJobs = mutableMapOf<String, Job>()"))
        assertTrue(runtime.contains("val seedDefinitions = listOf("))

        assertTrue(store.contains("internal class SimAudioRepositoryStoreSupport("))
        assertTrue(store.contains("suspend fun addLocalAudio("))
        assertTrue(store.contains("fun bindSession(audioId: String, sessionId: String)"))
        assertTrue(store.contains("fun clearBoundSession(audioId: String)"))
        assertTrue(store.contains("fun loadFromDisk()"))
        assertTrue(store.contains("fun loadPendingBadgeDeletes()"))
        assertTrue(store.contains("suspend fun deleteAudio(audioId: String): SimAudioDeleteResult"))
        assertTrue(store.contains("fun getPendingBadgeDeletesSnapshot(): Set<String>"))
        assertTrue(store.contains("fun backfillSeedInventory()"))
        assertTrue(store.contains("internal fun recoverOrphanedSimTranscriptions("))
        assertTrue(store.contains("internal fun simStoredAudioFilename("))
        assertTrue(store.contains("internal const val SIM_AUDIO_PENDING_BADGE_DELETE_FILENAME"))

        assertTrue(artifact.contains("internal class SimAudioRepositoryArtifactSupport("))
        assertTrue(artifact.contains("suspend fun getArtifacts(audioId: String)"))
        assertTrue(artifact.contains("suspend fun seedDebugFailureScenario()"))
        assertTrue(artifact.contains("internal fun buildSimDebugMissingSectionsArtifacts()"))
        assertTrue(artifact.contains("internal fun buildSimDebugFallbackArtifacts()"))
        assertTrue(artifact.contains("internal fun simArtifactFilename(audioId: String)"))

        assertTrue(sync.contains("internal class SimAudioRepositorySyncSupport("))
        assertTrue(sync.contains("suspend fun syncFromBadge(trigger: SimBadgeSyncTrigger)"))
        assertTrue(sync.contains("suspend fun syncFromDevice()"))
        assertTrue(sync.contains("internal fun simBadgeSyncSuccessMessage(importedCount: Int)"))
        assertTrue(sync.contains("internal fun emitSimAudioBadgeSyncRequestedTelemetry("))
        assertTrue(sync.contains("internal fun emitSimAudioSyncFailureWhileConnectivityUnavailableTelemetry("))
        assertTrue(sync.contains("private suspend fun reconcilePendingBadgeDeletes("))

        assertTrue(transcription.contains("internal class SimAudioRepositoryTranscriptionSupport("))
        assertTrue(transcription.contains("fun resumeTrackedJobs()"))
        assertTrue(transcription.contains("suspend fun startTranscription(audioId: String)"))
        assertTrue(transcription.contains("private fun observeTranscription(audioId: String, jobId: String)"))
        assertTrue(transcription.contains("is TingwuJobState.Completed ->"))
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
