package com.smartsales.aitest.audio

// 文件：app/src/main/java/com/smartsales/aitest/audio/LocalAudioStorageRepository.kt
// 模块：:app
// 说明：将设备/手机音频统一存入应用私有目录并提供列表、导入、删除能力
// 作者：创建于 2025-11-21

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.media.audiofiles.AudioOrigin
import com.smartsales.feature.media.audiofiles.AudioStorageRepository
import com.smartsales.feature.media.audiofiles.StoredAudio
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class LocalAudioStorageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
    private val mediaDownloader: DeviceMediaDownloader
) : AudioStorageRepository {

    private val audioDir: File = File(context.filesDir, "audio").apply { mkdirs() }
    private val internal = MutableStateFlow(readAll())
    override val audios = internal.asStateFlow()

    override suspend fun importFromDevice(baseUrl: String, file: DeviceMediaFile): StoredAudio =
        withContext(dispatchers.io) {
            val local = File(audioDir, file.name)
            when (val download = mediaDownloader.download(baseUrl, file)) {
                is Result.Error -> throw download.throwable
                is Result.Success -> {
                    download.data.copyTo(local, overwrite = true)
                }
            }
            local.setLastModified(file.modifiedAtMillis)
            val stored = local.toStoredAudio(AudioOrigin.DEVICE)
            upsert(stored)
            stored
        }

    override suspend fun importFromPhone(uri: Uri): StoredAudio =
        withContext(dispatchers.io) {
            val name = resolveFileName(uri) ?: "audio-${System.currentTimeMillis()}.mp3"
            val local = File(audioDir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                local.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("无法读取所选音频")
            val stored = local.toStoredAudio(AudioOrigin.PHONE)
            upsert(stored)
            stored
        }

    override suspend fun delete(audioId: String) = withContext(dispatchers.io) {
        val removed = audioDir.listFiles()?.firstOrNull { it.name == audioId }
        removed?.delete()
        internal.update { list -> list.filterNot { it.id == audioId } }
    }

    private fun upsert(audio: StoredAudio) {
        internal.update { current ->
            current.filterNot { it.id == audio.id } + audio
        }
    }

    private fun readAll(): List<StoredAudio> =
        audioDir.listFiles()?.map { it.toStoredAudio(AudioOrigin.PHONE) } ?: emptyList()

    private fun File.toStoredAudio(origin: AudioOrigin): StoredAudio {
        val duration = runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
            }
        }.getOrNull()
        return StoredAudio(
            id = name,
            displayName = name,
            sizeBytes = length(),
            durationMillis = duration,
            timestampMillis = lastModified(),
            origin = origin,
            localUri = toUri()
        )
    }

    private fun resolveFileName(uri: Uri): String? {
        val resolver: ContentResolver = context.contentResolver
        val cursor = resolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) {
                return it.getString(nameIndex)
            }
        }
        return null
    }
}
