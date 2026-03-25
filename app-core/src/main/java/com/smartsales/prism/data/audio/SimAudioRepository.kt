package com.smartsales.prism.data.audio

import android.content.Context
import com.smartsales.data.oss.OssUploader
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SIM 专属音频仓库。
 * 保持公开入口稳定，把存储、同步、工件、转写职责下沉到支持对象。
 */
@Singleton
class SimAudioRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectivityBridge: ConnectivityBridge,
    private val ossUploader: OssUploader,
    private val tingwuPipeline: TingwuPipeline
) {

    private val runtime = SimAudioRepositoryRuntime(
        context = context,
        connectivityBridge = connectivityBridge,
        ossUploader = ossUploader,
        tingwuPipeline = tingwuPipeline
    )
    private val storeSupport = SimAudioRepositoryStoreSupport(runtime)
    private val artifactSupport = SimAudioRepositoryArtifactSupport(runtime, storeSupport)
    private val syncSupport = SimAudioRepositorySyncSupport(runtime, storeSupport)
    private val transcriptionSupport = SimAudioRepositoryTranscriptionSupport(
        runtime = runtime,
        storeSupport = storeSupport,
        artifactSupport = artifactSupport
    )

    init {
        storeSupport.loadFromDisk()
        storeSupport.backfillSeedInventory()
        transcriptionSupport.resumeTrackedJobs()
    }

    fun getAudioFiles(): Flow<List<AudioFile>> = runtime.audioFiles.asStateFlow()

    internal suspend fun canSyncFromBadge(): Boolean = syncSupport.canSyncFromBadge()

    internal suspend fun syncFromBadge(trigger: SimBadgeSyncTrigger): SimBadgeSyncOutcome {
        return syncSupport.syncFromBadge(trigger)
    }

    suspend fun syncFromDevice() {
        syncSupport.syncFromDevice()
    }

    suspend fun addLocalAudio(uriString: String) {
        storeSupport.addLocalAudio(uriString)
    }

    suspend fun seedDebugFailureScenario() {
        artifactSupport.seedDebugFailureScenario()
    }

    suspend fun seedDebugMissingSectionsScenario() {
        artifactSupport.seedDebugMissingSectionsScenario()
    }

    suspend fun seedDebugFallbackScenario() {
        artifactSupport.seedDebugFallbackScenario()
    }

    suspend fun startTranscription(audioId: String) {
        transcriptionSupport.startTranscription(audioId)
    }

    fun deleteAudio(audioId: String) {
        storeSupport.deleteAudio(audioId)
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
