package com.smartsales.prism.data.audio

import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.service.DownloadServiceOrchestrator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SIM 专属音频仓库。
 * 保持公开入口稳定，把存储、同步、工件、转写职责下沉到支持对象。
 */
@Singleton
class SimAudioRepository @Inject constructor(
    private val runtime: SimAudioRepositoryRuntime,
    private val orchestrator: DownloadServiceOrchestrator
) {

    private val storeSupport = SimAudioRepositoryStoreSupport(runtime)
    private val artifactSupport = SimAudioRepositoryArtifactSupport(runtime, storeSupport)
    private val syncSupport = SimAudioRepositorySyncSupport(runtime, storeSupport, orchestrator)
    private val transcriptionSupport = SimAudioRepositoryTranscriptionSupport(
        runtime = runtime,
        storeSupport = storeSupport,
        artifactSupport = artifactSupport
    )

    init {
        storeSupport.loadFromDisk()
        storeSupport.loadPendingBadgeDeletes()
        storeSupport.backfillSeedInventory()
        transcriptionSupport.resumeTrackedJobs()
        observeActiveDeviceChanges()
    }

    private fun observeActiveDeviceChanges() {
        runtime.repositoryScope.launch {
            val activeDevice = runCatching { runtime.deviceRegistryManager.activeDevice }.getOrNull()
                ?: return@launch
            var previousMac = activeDevice.value?.macAddress
            activeDevice.collect { device ->
                val currentMac = device?.macAddress
                if (previousMac != null && currentMac != previousMac) {
                    syncSupport.cancelAllBadgeDownloads()
                }
                previousMac = currentMac
            }
        }
    }

    fun getAudioFiles(): Flow<List<AudioFile>> = runtime.audioFiles.asStateFlow()

    internal suspend fun canSyncFromBadge(): Boolean = syncSupport.canSyncFromBadge()

    internal suspend fun syncFromBadge(trigger: SimBadgeSyncTrigger): SimBadgeSyncOutcome {
        return syncSupport.syncFromBadge(trigger)
    }

    internal suspend fun shouldSuppressAutoSync(): Boolean {
        return syncSupport.shouldSuppressAutoSync()
    }

    internal suspend fun syncFromBadgeAfterVerifiedReadiness(
        trigger: SimBadgeSyncTrigger
    ): SimBadgeSyncOutcome {
        return syncSupport.syncFromBadgeAfterVerifiedReadiness(trigger)
    }

    suspend fun syncFromDevice() {
        syncSupport.syncFromDevice()
    }

    suspend fun addLocalAudio(uriString: String) {
        storeSupport.addLocalAudio(uriString)
    }

    suspend fun startTranscription(audioId: String) {
        transcriptionSupport.startTranscription(audioId)
    }

    internal suspend fun deleteAudio(audioId: String): SimAudioDeleteResult {
        val target = storeSupport.getAudio(audioId) ?: return SimAudioDeleteResult.NotFound
        syncSupport.cancelBadgeDownload(target.filename)
        return storeSupport.deleteAudio(audioId)
    }

    fun toggleStar(audioId: String) {
        storeSupport.toggleStar(audioId)
    }

    fun getAudio(audioId: String): AudioFile? = storeSupport.getAudio(audioId)

    suspend fun getArtifacts(audioId: String): TingwuJobArtifacts? {
        return artifactSupport.getArtifacts(audioId)
    }

    fun bindSession(audioId: String, sessionId: String) {
        storeSupport.bindSession(audioId, sessionId)
    }

    fun clearBoundSession(audioId: String) {
        storeSupport.clearBoundSession(audioId)
    }

    fun getBoundSessionId(audioId: String): String? = storeSupport.getBoundSessionId(audioId)

    fun getAudioFilesSnapshot(): List<AudioFile> = storeSupport.getAudioFilesSnapshot()
}
