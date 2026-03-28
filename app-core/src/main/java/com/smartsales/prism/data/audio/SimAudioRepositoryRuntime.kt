package com.smartsales.prism.data.audio

import android.content.Context
import com.smartsales.data.oss.OssUploader
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json

internal class SimAudioRepositoryRuntime(
    val context: Context,
    val connectivityBridge: ConnectivityBridge,
    val ossUploader: OssUploader,
    val tingwuPipeline: TingwuPipeline
) {
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    val metadataFile = File(context.filesDir, SIM_AUDIO_METADATA_FILENAME)
    val fileMutex = Mutex()
    val syncMutex = Mutex()
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val audioFiles = MutableStateFlow<List<AudioFile>>(emptyList())
    val observationJobs = mutableMapOf<String, Job>()
    val seedDefinitions = listOf(
        SimSeedDefinition(
            id = "sim_wave2_seed",
            assetName = "sim_wave2_seed.mp3",
            filename = "SIM_Wave2_Seed.mp3",
            isStarred = true
        ),
        SimSeedDefinition(
            id = "sim_wave2_seed_pending_a",
            assetName = "sim_wave2_seed.mp3",
            filename = "SIM_Wave2_Pending_A.mp3"
        ),
        SimSeedDefinition(
            id = "sim_wave2_seed_pending_b",
            assetName = "sim_wave2_seed.mp3",
            filename = "SIM_Wave2_Pending_B.mp3"
        ),
        SimSeedDefinition(
            id = "sim_wave2_seed_pending_c",
            assetName = "sim_wave2_seed.mp3",
            filename = "SIM_Wave2_Pending_C.mp3"
        ),
        SimSeedDefinition(
            id = "sim_wave2_seed_pending_d",
            assetName = "sim_wave2_seed.mp3",
            filename = "SIM_Wave2_Pending_D.mp3"
        ),
        SimSeedDefinition(
            id = "sim_wave2_seed_pending_e",
            assetName = "sim_wave2_seed.mp3",
            filename = "SIM_Wave2_Pending_E.mp3"
        )
    )
}

internal data class SimSeedDefinition(
    val id: String,
    val assetName: String,
    val filename: String,
    val isStarred: Boolean = false
)
