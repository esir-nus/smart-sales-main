package com.smartsales.prism.data.audio

import android.content.Context
import com.smartsales.data.oss.OssUploader
import com.smartsales.prism.data.connectivity.BadgeEndpointRecoveryCoordinator
import com.smartsales.prism.data.connectivity.legacy.PhoneWifiProvider
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.ConnectivityPrompt
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json

@Singleton
class SimAudioRepositoryRuntime @Inject constructor(
    @ApplicationContext val context: Context,
    val connectivityBridge: ConnectivityBridge,
    val endpointRecoveryCoordinator: BadgeEndpointRecoveryCoordinator,
    val ossUploader: OssUploader,
    val tingwuPipeline: TingwuPipeline,
    val connectivityPrompt: ConnectivityPrompt,
    val phoneWifiProvider: PhoneWifiProvider
) {
    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    var repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    val metadataFile = File(context.filesDir, SIM_AUDIO_METADATA_FILENAME)
    val pendingBadgeDeleteFile = File(context.filesDir, SIM_AUDIO_PENDING_BADGE_DELETE_FILENAME)
    val fileMutex = Mutex()
    val syncMutex = Mutex()
    val badgeDownloadQueueMutex = Mutex()
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val audioFiles = MutableStateFlow<List<AudioFile>>(emptyList())
    val pendingBadgeDeletes = MutableStateFlow<Set<String>>(emptySet())
    val observationJobs = mutableMapOf<String, Job>()
    val queuedBadgeDownloads = LinkedHashSet<String>()
    var badgeDownloadWorkerJob: Job? = null
    var activeBadgeDownloadFilename: String? = null
    var activeBadgeDownloadJob: Job? = null
    internal val seedDefinitions = listOf(
        SimSeedDefinition(
            id = "sim_wave2_seed",
            assetName = "sim_wave2_seed.mp3",
            filename = "SIM_Wave2_Seed.mp3",
            isStarred = true
        )
    )
    internal val retiredSeedIds = setOf(
        "sim_wave2_seed_pending_a",
        "sim_wave2_seed_pending_b",
        "sim_wave2_seed_pending_c",
        "sim_wave2_seed_pending_d",
        "sim_wave2_seed_pending_e"
    )

    internal fun overrideConcurrencyForTests(dispatcher: CoroutineDispatcher, scope: CoroutineScope) {
        ioDispatcher = dispatcher
        repositoryScope = scope
    }
}

internal data class SimSeedDefinition(
    val id: String,
    val assetName: String,
    val filename: String,
    val isStarred: Boolean = false
)
