package com.smartsales.prism.data.audio

import android.content.Context
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import java.io.File
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal fun simArtifactFilename(audioId: String): String {
    return "sim_${audioId}_artifacts.json"
}

internal fun simArtifactFile(context: Context, audioId: String): File {
    return File(context.filesDir, simArtifactFilename(audioId))
}

internal fun summarizeSimArtifacts(artifacts: TingwuJobArtifacts): String? {
    return artifacts.smartSummary?.summary ?: artifacts.transcriptMarkdown?.take(120)
}

internal class SimAudioRepositoryArtifactSupport(
    private val runtime: SimAudioRepositoryRuntime,
    private val storeSupport: SimAudioRepositoryStoreSupport
) {

    suspend fun getArtifacts(audioId: String): TingwuJobArtifacts? = withContext(runtime.ioDispatcher) {
        val file = simArtifactFile(runtime.context, audioId)
        if (!file.exists()) return@withContext null
        runCatching { runtime.json.decodeFromString<TingwuJobArtifacts>(file.readText()) }
            .getOrElse {
                android.util.Log.e("SimAudioRepository", "load artifacts failed for $audioId", it)
                null
            }
    }

    suspend fun writeArtifacts(audioId: String, artifacts: TingwuJobArtifacts) {
        withContext(runtime.ioDispatcher) {
            simArtifactFile(runtime.context, audioId)
                .writeText(runtime.json.encodeToString(artifacts))
        }
    }
}
