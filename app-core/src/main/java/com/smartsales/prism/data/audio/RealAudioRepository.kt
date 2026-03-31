package com.smartsales.prism.data.audio

import android.net.Uri
import android.content.Context
import com.smartsales.core.util.Result
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioDeleteResult
import com.smartsales.prism.domain.audio.AudioRepository
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.data.oss.OssUploadResult
import com.smartsales.data.oss.OssUploader
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.domain.tingwu.TingwuJobState
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import com.smartsales.prism.domain.tingwu.TingwuRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

internal const val AUDIO_METADATA_FILENAME = "audio_metadata.json"
internal const val AUDIO_PENDING_BADGE_DELETE_FILENAME = "audio_pending_badge_deletes.json"

internal fun normalizeAudioPendingBadgeDeleteFilename(filename: String): String = filename.trim()

internal fun existingAudioBadgeFilenames(entries: List<AudioFile>): Set<String> {
    return entries
        .filter(::isBadgeOriginAudio)
        .map { it.filename }
        .toSet()
}

internal fun selectNewAudioBadgeFilenames(
    badgeFilenames: List<String>,
    existingBadgeFilenames: Set<String>,
    pendingBadgeDeleteFilenames: Set<String> = emptySet()
): List<String> {
    return badgeFilenames
        .distinct()
        .filter { it !in existingBadgeFilenames }
        .filter { it !in pendingBadgeDeleteFilenames }
}

/**
 * 真实音频仓库 — Wave 2 (JSON file backed)
 * 使用带有 Mutex 锁的本地 JSON 文件作为持久层，满足无 Room 依赖的要求。
 */
@Singleton
class RealAudioRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectivityBridge: ConnectivityBridge,
    private val ossUploader: OssUploader,
    private val tingwuPipeline: TingwuPipeline
) : AudioRepository {
    
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val audioFilesFile = File(context.filesDir, AUDIO_METADATA_FILENAME)
    private val pendingBadgeDeleteFile = File(context.filesDir, AUDIO_PENDING_BADGE_DELETE_FILENAME)
    private val fileMutex = Mutex()
    private val syncMutex = Mutex()
    private val _audioFiles = MutableStateFlow<List<AudioFile>>(emptyList())
    private val pendingBadgeDeletes = MutableStateFlow<Set<String>>(emptySet())
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    init {
        loadFromDisk()
        loadPendingBadgeDeletes()
    }

    override fun getAudioFiles(): Flow<List<AudioFile>> = _audioFiles.asStateFlow()

    override suspend fun syncFromDevice() = withContext(ioDispatcher) {
        syncMutex.withLock {
            android.util.Log.d("RealAudioRepository", "syncFromDevice: Starting badge sync")
            val listResult = connectivityBridge.listRecordings()
            val badgeFiles = when (listResult) {
                is Result.Success -> listResult.data
                is Result.Error -> throw Exception("无法获取徽章文件列表: ${listResult.throwable.message}")
            }

            val suppressedPendingDeletes = reconcilePendingBadgeDeletes(badgeFiles)

            val newFilesToDownload = selectNewAudioBadgeFilenames(
                badgeFilenames = badgeFiles,
                existingBadgeFilenames = existingAudioBadgeFilenames(_audioFiles.value),
                pendingBadgeDeleteFilenames = suppressedPendingDeletes + pendingBadgeDeletes.value
            )

            android.util.Log.d(
                "RealAudioRepository",
                "syncFromDevice: Found ${newFilesToDownload.size} new files to download"
            )

            var downloadedCount = 0
            for (filename in newFilesToDownload) {
                android.util.Log.d("RealAudioRepository", "syncFromDevice: Downloading $filename")
                when (val downloadResult = connectivityBridge.downloadRecording(filename)) {
                    is WavDownloadResult.Success -> {
                        val newId = UUID.randomUUID().toString()
                        val destFile = File(context.filesDir, "$newId.wav")
                        downloadResult.localFile.copyTo(destFile, overwrite = true)
                        downloadResult.localFile.delete()

                        val newFile = AudioFile(
                            id = newId,
                            filename = filename,
                            timeDisplay = "Just now",
                            source = AudioSource.SMARTBADGE,
                            status = TranscriptionStatus.PENDING,
                            isStarred = false
                        )
                        mutateAndSave { currentList ->
                            currentList + newFile
                        }
                        downloadedCount++
                    }

                    is WavDownloadResult.Error -> {
                        android.util.Log.e(
                            "RealAudioRepository",
                            "syncFromDevice: Failed to download $filename: ${downloadResult.message}"
                        )
                    }
                }
            }

            if (newFilesToDownload.isNotEmpty() && downloadedCount == 0) {
                throw Exception("发现新录音，但全部下载失败")
            }
        }
    }

    override suspend fun addLocalAudio(uriString: String) = withContext(ioDispatcher) {
        val uri = Uri.parse(uriString)
        val newId = UUID.randomUUID().toString()
        val destFile = File(context.filesDir, "$newId.wav")
        
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("无法读取本地音频文件")
        }
        
        val newFile = AudioFile(
            id = newId,
            filename = "Local_${System.currentTimeMillis()}.wav",
            timeDisplay = "Just now",
            source = AudioSource.PHONE,
            status = TranscriptionStatus.PENDING,
            isStarred = false
        )
        mutateAndSave { currentList ->
            currentList + newFile
        }
    }

    override suspend fun startTranscription(audioId: String) {
        // 1. Mark as transcribing initially
        mutateAndSave { currentList ->
            currentList.map { 
                if (it.id == audioId) it.copy(status = TranscriptionStatus.TRANSCRIBING, progress = 0.05f) else it 
            }
        }

        try {
            android.util.Log.d("RealAudioRepository", "startTranscription: audioId=$audioId -> starting OSS upload")
            val fileToTranscribe = File(context.filesDir, "$audioId.wav")
            if (!fileToTranscribe.exists()) {
                throw Exception("找不到本地音频文件实体")
            }

            // 2. Upload to OSS
            val objectKey = "smartsales/audio/${System.currentTimeMillis()}/$audioId.wav"
            val uploadResult = ossUploader.upload(fileToTranscribe, objectKey)
            
            val publicUrl = when (uploadResult) {
                is OssUploadResult.Success -> uploadResult.publicUrl
                is OssUploadResult.Error -> throw Exception("[OSS_${uploadResult.code}] ${uploadResult.message}")
            }
            
            android.util.Log.d("RealAudioRepository", "startTranscription: audioId=$audioId -> OSS upload success, submitting to Tingwu")
            
            // 3. Submit to Tingwu
            val tingwuInput = TingwuRequest(
                ossObjectKey = objectKey,
                fileUrl = publicUrl,
                audioAssetName = fileToTranscribe.name,
                language = "zh-CN",
                audioFilePath = fileToTranscribe
            )
            val submitResult = tingwuPipeline.submit(tingwuInput)
            val taskId = when (submitResult) {
                is Result.Success -> submitResult.data
                is Result.Error -> throw Exception("Tingwu submission failed", submitResult.throwable)
            }

            android.util.Log.d("RealAudioRepository", "startTranscription: audioId=$audioId -> Tingwu Task ID=$taskId, observing progress...")

            // 4. Observe Job State
            tingwuPipeline.observeJob(taskId).collect { state ->
                when (state) {
                    TingwuJobState.Idle -> { /* no-op */ }
                    is TingwuJobState.InProgress -> {
                        mutateAndSave { currentList ->
                            currentList.map { 
                                if (it.id == audioId) it.copy(progress = state.progressPercent / 100f) else it 
                            }
                        }
                    }
                    is TingwuJobState.Completed -> {
                        android.util.Log.d("RealAudioRepository", "startTranscription: audioId=$audioId -> Completed. Writing artifacts to SSD.")
                        
                        // Save the rich JSON artifact to SSD
                        val artifactFile = File(context.filesDir, "${audioId}_artifacts.json")
                        withContext(ioDispatcher) {
                            artifactFile.writeText(json.encodeToString(state.artifacts))
                        }
                        
                        // Map the smart summary to the AudioFile.summary for the UI Drawer
                        val textSummary = state.artifacts?.smartSummary?.summary ?: "(智能摘要缺失)"
                        
                        mutateAndSave { currentList ->
                            currentList.map { 
                                if (it.id == audioId) it.copy(
                                    status = TranscriptionStatus.TRANSCRIBED, 
                                    summary = textSummary,
                                    progress = 1.0f
                                ) else it 
                            }
                        }
                    }
                    is TingwuJobState.Failed -> {
                        android.util.Log.e("RealAudioRepository", "startTranscription: Tingwu job failed state: ${state.reason}")
                        // Rollback state cleanly without blowing up the Flow
                        _audioFiles.update { currentList ->
                            currentList.map { 
                                if (it.id == audioId) it.copy(status = TranscriptionStatus.PENDING, progress = 0f) else it 
                            }
                        }
                        saveToDiskAsync()
                        return@collect 
                    }
                    else -> { /* Safe fallback for exhaustive when */ }
                }
            }
        } catch (e: Exception) {
            val unwrappedData = generateSequence<Throwable>(e) { it.cause }
                .firstOrNull { it.javaClass.name == "retrofit2.HttpException" }
            
            var detailedMsg: String? = null
            if (unwrappedData != null) {
                try {
                    val responseMethod = unwrappedData.javaClass.getMethod("response")
                    val responseObj = responseMethod.invoke(unwrappedData)
                    if (responseObj != null) {
                        val errorBodyMethod = responseObj.javaClass.getMethod("errorBody")
                        val errorBodyObj = errorBodyMethod.invoke(responseObj)
                        if (errorBodyObj != null) {
                            val stringMethod = errorBodyObj.javaClass.getMethod("string")
                            detailedMsg = stringMethod.invoke(errorBodyObj) as? String
                        }
                    }
                } catch (ignored: Exception) { }
            }
            
            val errorDetails = detailedMsg ?: unwrappedData?.message ?: e.message ?: "Unknown error"
            
            android.util.Log.e("RealAudioRepository", "startTranscription: FAILED audioId=$audioId. Details: $errorDetails", e)
            
            // Rollback memory immediately, save disk async
            _audioFiles.update { currentList ->
                currentList.map { 
                    if (it.id == audioId) it.copy(
                        status = TranscriptionStatus.PENDING, 
                        progress = 0f
                    ) else it 
                }
            }
            saveToDiskAsync()
            
            throw Exception("转写失败: $errorDetails", e) // Pass refined message to ViewModel
        }
    }

    override suspend fun deleteAudio(audioId: String): AudioDeleteResult = withContext(ioDispatcher) {
        val fileToDelete = _audioFiles.value.find { it.id == audioId }
            ?: return@withContext AudioDeleteResult.NotFound
        val pendingDeleteFilename = fileToDelete.filename
            .takeIf { isBadgeOriginAudio(fileToDelete) }
            ?.let(::normalizeAudioPendingBadgeDeleteFilename)

        fileMutex.withLock {
            _audioFiles.value = _audioFiles.value.filterNot { it.id == audioId }
            if (pendingDeleteFilename != null) {
                pendingBadgeDeletes.value = pendingBadgeDeletes.value + pendingDeleteFilename
            }
            writeMetadataLocked(_audioFiles.value)
            writePendingBadgeDeletesLocked(pendingBadgeDeletes.value)
        }

        val localFile = File(context.filesDir, "$audioId.wav")
        if (localFile.exists()) {
            localFile.delete()
        }
        val artifactFile = File(context.filesDir, "${audioId}_artifacts.json")
        if (artifactFile.exists()) {
            artifactFile.delete()
        }

        if (pendingDeleteFilename != null) {
            val success = runCatching {
                connectivityBridge.deleteRecording(fileToDelete.filename)
            }.getOrDefault(false)
            if (success) {
                clearPendingBadgeDeletes(setOf(pendingDeleteFilename))
            }
            return@withContext AudioDeleteResult.Badge(
                filename = fileToDelete.filename,
                remoteDeleteSucceeded = success
            )
        }

        AudioDeleteResult.LocalOnly(filename = fileToDelete.filename)
    }

    override fun toggleStar(audioId: String) {
        _audioFiles.update { files ->
            files.map { if (it.id == audioId) it.copy(isStarred = !it.isStarred) else it }
        }
        saveToDiskAsync()
    }

    override fun getAudio(audioId: String): AudioFile? {
        return _audioFiles.value.find { it.id == audioId }
    }

    override suspend fun getArtifacts(audioId: String): TingwuJobArtifacts? = withContext(ioDispatcher) {
        val artifactFile = File(context.filesDir, "${audioId}_artifacts.json")
        if (!artifactFile.exists()) return@withContext null
        try {
            json.decodeFromString<TingwuJobArtifacts>(artifactFile.readText())
        } catch (e: Exception) {
            android.util.Log.e("RealAudioRepository", "Failed to load artifacts for $audioId", e)
            null
        }
    }

    override fun bindSession(audioId: String, sessionId: String) {
        _audioFiles.update { files ->
            files.map { if (it.id == audioId) it.copy(boundSessionId = sessionId) else it }
        }
        saveToDiskAsync()
    }

    override fun getBoundSessionId(audioId: String): String? {
        return _audioFiles.value.find { it.id == audioId }?.boundSessionId
    }

    // --- Private Persistence Logic ---

    private suspend fun mutateAndSave(mutateBlock: (List<AudioFile>) -> List<AudioFile>) {
        fileMutex.withLock {
            val newList = mutateBlock(_audioFiles.value)
            _audioFiles.value = newList
            writeMetadataLocked(newList)
        }
    }

    // Used for non-suspend operations (Optimistic update in memory, background save)
    private fun saveToDiskAsync() {
        kotlinx.coroutines.GlobalScope.launch(ioDispatcher) {
            fileMutex.withLock {
                writeMetadataLocked(_audioFiles.value)
            }
        }
    }

    private fun loadFromDisk() {
        if (!audioFilesFile.exists()) return
        try {
            val contents = audioFilesFile.readText()
            if (contents.isNotBlank()) {
                val parsed = json.decodeFromString<List<AudioFile>>(contents)
                val normalization = normalizeBadgeOriginEntries(parsed)
                _audioFiles.value = normalization.entries
                if (normalization.normalizedCount > 0) {
                    android.util.Log.w(
                        "RealAudioRepository",
                        "loadFromDisk: normalized ${normalization.normalizedCount} legacy badge entries by filename"
                    )
                    audioFilesFile.writeText(json.encodeToString(normalization.entries))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // On parse error, just start fresh. File might be corrupted.
        }
    }

    private fun loadPendingBadgeDeletes() {
        if (!pendingBadgeDeleteFile.exists()) return
        try {
            val contents = pendingBadgeDeleteFile.readText()
            if (contents.isNotBlank()) {
                pendingBadgeDeletes.value = json.decodeFromString<List<String>>(contents)
                    .map(::normalizeAudioPendingBadgeDeleteFilename)
                    .filter(String::isNotBlank)
                    .toSet()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun clearPendingBadgeDeletes(filenames: Set<String>) {
        if (filenames.isEmpty()) return
        fileMutex.withLock {
            val updated = pendingBadgeDeletes.value - filenames
            if (updated == pendingBadgeDeletes.value) return@withLock
            pendingBadgeDeletes.value = updated
            writePendingBadgeDeletesLocked(updated)
        }
    }

    private suspend fun reconcilePendingBadgeDeletes(badgeFiles: List<String>): Set<String> {
        val currentPendingDeletes = pendingBadgeDeletes.value
        if (currentPendingDeletes.isEmpty()) return emptySet()

        val badgeFilenameSet = badgeFiles.map(::normalizeAudioPendingBadgeDeleteFilename).toSet()
        val missingOnBadge = currentPendingDeletes - badgeFilenameSet
        if (missingOnBadge.isNotEmpty()) {
            clearPendingBadgeDeletes(missingOnBadge)
        }

        val stillPresentOnBadge = pendingBadgeDeletes.value.intersect(badgeFilenameSet)
        stillPresentOnBadge.forEach { filename ->
            val deleted = runCatching {
                connectivityBridge.deleteRecording(filename)
            }.getOrDefault(false)
            if (deleted) {
                clearPendingBadgeDeletes(setOf(filename))
            } else {
                android.util.Log.w(
                    "RealAudioRepository",
                    "pending badge delete still blocked filename=$filename"
                )
            }
        }

        return currentPendingDeletes.intersect(badgeFilenameSet)
    }

    private suspend fun writeMetadataLocked(entries: List<AudioFile>) {
        withContext(ioDispatcher) {
            audioFilesFile.parentFile?.mkdirs()
            audioFilesFile.writeText(json.encodeToString(entries))
        }
    }

    private suspend fun writePendingBadgeDeletesLocked(filenames: Set<String>) {
        withContext(ioDispatcher) {
            pendingBadgeDeleteFile.parentFile?.mkdirs()
            pendingBadgeDeleteFile.writeText(json.encodeToString(filenames.toList().sorted()))
        }
    }
}
