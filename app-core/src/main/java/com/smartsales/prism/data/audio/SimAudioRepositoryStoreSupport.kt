package com.smartsales.prism.data.audio

import android.content.Context
import android.net.Uri
import com.smartsales.prism.data.connectivity.legacy.toBadgeDownloadFilename
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioLocalAvailability
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal const val ORPHANED_SIM_TRANSCRIPTION_MESSAGE =
    "检测到未完成的旧转写状态，请重新开始转写。"
internal const val SIM_AUDIO_METADATA_FILENAME = "sim_audio_metadata.json"
internal const val SIM_AUDIO_PENDING_BADGE_DELETE_FILENAME =
    "sim_audio_pending_badge_deletes.json"
internal const val BADGE_SWITCH_INTERRUPTED_MESSAGE = "设备已切换，请重新同步"

internal sealed interface SimAudioDeleteResult {
    data object NotFound : SimAudioDeleteResult
    data class LocalOnly(val filename: String) : SimAudioDeleteResult
    data class Badge(
        val filename: String,
        val remoteDeleteSucceeded: Boolean
    ) : SimAudioDeleteResult
}

internal fun recoverOrphanedSimTranscriptions(entries: List<AudioFile>): List<AudioFile> {
    return entries.map { audio ->
        if (audio.status == TranscriptionStatus.TRANSCRIBING && audio.activeJobId.isNullOrBlank()) {
            audio.copy(
                status = TranscriptionStatus.PENDING,
                progress = 0f,
                lastErrorMessage = audio.lastErrorMessage ?: ORPHANED_SIM_TRANSCRIPTION_MESSAGE
            )
        } else {
            audio
        }
    }
}

internal fun recoverInterruptedSimDownloads(entries: List<AudioFile>): List<AudioFile> {
    return entries.map { audio ->
        if (audio.localAvailability == AudioLocalAvailability.DOWNLOADING) {
            audio.copy(localAvailability = AudioLocalAvailability.QUEUED)
        } else {
            audio
        }
    }
}

internal fun simStoredAudioFilename(audioId: String, extension: String): String {
    return "sim_${audioId}.$extension"
}

internal fun simStoredAudioFile(context: Context, audioId: String, extension: String): File {
    return File(context.filesDir, simStoredAudioFilename(audioId, extension))
}

internal fun simPendingBadgeDeleteFilename(filename: String): String {
    val trimmed = filename.trim()
    if (trimmed.isBlank()) return ""

    val token = trimmed.removeSuffix(".wav").removeSuffix(".WAV").trim()
    // 仅固件时间戳/日志载荷补全 log_ 前缀；普通已命名文件保持原名。
    val normalized = when {
        token.matches(Regex("^\\d{8}_\\d{6}$")) ||
            trimmed.startsWith("log_", ignoreCase = true) ||
            trimmed.startsWith("log#", ignoreCase = true) -> trimmed.toBadgeDownloadFilename()
        else -> trimmed
    }
    return normalized.ifBlank { trimmed }
}

internal fun resolveSimStoredAudioFile(context: Context, audioId: String): File? {
    val candidates = listOf("wav", "mp3", "m4a", "aac", "ogg")
        .map { extension -> simStoredAudioFile(context, audioId, extension) }
    return candidates.firstOrNull { it.exists() }
}

internal fun guessSimExtensionFromUri(uriString: String): String {
    val lower = uriString.lowercase()
    return when {
        lower.endsWith(".mp3") -> "mp3"
        lower.endsWith(".m4a") -> "m4a"
        lower.endsWith(".aac") -> "aac"
        lower.endsWith(".ogg") -> "ogg"
        else -> "wav"
    }
}

internal class SimAudioRepositoryStoreSupport(
    private val runtime: SimAudioRepositoryRuntime
) {

    suspend fun addLocalAudio(uriString: String) = withContext(runtime.ioDispatcher) {
        val uri = Uri.parse(uriString)
        val newId = UUID.randomUUID().toString()
        val extension = guessSimExtensionFromUri(uriString)
        persistTestAudioEntry(
            audioId = newId,
            extension = extension,
            displayFilename = "SIM_Local_${System.currentTimeMillis()}.$extension",
            writeContent = { copiedTarget ->
                try {
                    runtime.context.contentResolver.openInputStream(uri)?.use { input ->
                        copiedTarget.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("无法打开本地音频流")
                } catch (e: Exception) {
                    throw Exception("无法读取本地音频文件", e)
                }
            }
        )
    }

    suspend fun deleteAudio(audioId: String): SimAudioDeleteResult = withContext(runtime.ioDispatcher) {
        val target = runtime.audioFiles.value.find { it.id == audioId }
            ?: return@withContext SimAudioDeleteResult.NotFound
        runtime.observationJobs.remove(audioId)?.cancel()
        val pendingDeleteFilename = target.filename.takeIf { isBadgeOriginAudio(target) }
            ?.let(::simPendingBadgeDeleteFilename)

        runtime.fileMutex.withLock {
            runtime.audioFiles.value = runtime.audioFiles.value.filterNot { it.id == audioId }
            if (pendingDeleteFilename != null) {
                runtime.pendingBadgeDeletes.value = runtime.pendingBadgeDeletes.value + pendingDeleteFilename
            }
            writeMetadataLocked(runtime.audioFiles.value)
            writePendingBadgeDeletesLocked(runtime.pendingBadgeDeletes.value)
        }

        resolveSimStoredAudioFile(runtime.context, audioId)?.delete()
        simArtifactFile(runtime.context, audioId).delete()

        if (pendingDeleteFilename != null) {
            val remoteDeleteSucceeded = runCatching {
                runtime.connectivityBridge.deleteRecording(target.filename)
            }.getOrDefault(false)
            if (remoteDeleteSucceeded) {
                clearPendingBadgeDeletes(setOf(pendingDeleteFilename))
            }
            return@withContext SimAudioDeleteResult.Badge(
                filename = target.filename,
                remoteDeleteSucceeded = remoteDeleteSucceeded
            )
        }
        SimAudioDeleteResult.LocalOnly(filename = target.filename)
    }

    fun toggleStar(audioId: String) {
        runtime.audioFiles.update { current ->
            current.map { if (it.id == audioId) it.copy(isStarred = !it.isStarred) else it }
        }
        saveToDiskAsync()
    }

    fun getAudio(audioId: String): AudioFile? = runtime.audioFiles.value.find { it.id == audioId }

    fun getAudioByNormalizedBadgeFilename(filename: String, badgeMac: String? = null): AudioFile? {
        val normalizedFilename = simPendingBadgeDeleteFilename(filename)
        return runtime.audioFiles.value.firstOrNull { matchesBadgeAudio(it, normalizedFilename, badgeMac) }
    }

    fun bindSession(audioId: String, sessionId: String) {
        runtime.audioFiles.update { current ->
            current.map { if (it.id == audioId) it.copy(boundSessionId = sessionId) else it }
        }
        saveToDiskAsync()
    }

    fun clearBoundSession(audioId: String) {
        runtime.audioFiles.update { current ->
            current.map { if (it.id == audioId) it.copy(boundSessionId = null) else it }
        }
        saveToDiskAsync()
    }

    fun getBoundSessionId(audioId: String): String? = getAudio(audioId)?.boundSessionId

    fun getAudioFilesSnapshot(): List<AudioFile> = runtime.audioFiles.value

    fun getPendingBadgeDeletesSnapshot(): Set<String> = runtime.pendingBadgeDeletes.value

    fun loadFromDisk() {
        if (!runtime.metadataFile.exists()) return
        runCatching {
            val contents = runtime.metadataFile.readText()
            if (contents.isNotBlank()) {
                val parsed = runtime.json.decodeFromString<List<AudioFile>>(contents)
                val normalization = normalizeBadgeOriginEntries(parsed)
                runtime.audioFiles.value = recoverInterruptedSimDownloads(
                    recoverOrphanedSimTranscriptions(normalization.entries)
                )
                if (normalization.normalizedCount > 0) {
                    android.util.Log.w(
                        "SimAudioRepository",
                        "loadFromDisk: normalized ${normalization.normalizedCount} legacy badge entries by filename"
                    )
                    runtime.metadataFile.writeText(runtime.json.encodeToString(runtime.audioFiles.value))
                }
            }
        }.onFailure {
            android.util.Log.e("SimAudioRepository", "load metadata failed", it)
        }
    }

    fun loadPendingBadgeDeletes() {
        if (!runtime.pendingBadgeDeleteFile.exists()) return
        runCatching {
            val contents = runtime.pendingBadgeDeleteFile.readText()
            if (contents.isNotBlank()) {
                runtime.pendingBadgeDeletes.value = runtime.json.decodeFromString<List<String>>(contents)
                    .map(::simPendingBadgeDeleteFilename)
                    .filter(String::isNotBlank)
                    .toSet()
            }
        }.onFailure {
            android.util.Log.e("SimAudioRepository", "load pending badge deletes failed", it)
        }
    }

    fun backfillSeedInventory() {
        runCatching {
            pruneRetiredSeedInventory()

            val existingIds = runtime.audioFiles.value.map { it.id }.toSet()
            val updatedFiles = runtime.audioFiles.value.toMutableList()

            runtime.seedDefinitions.forEach { seed ->
                val seedFile = simStoredAudioFile(runtime.context, seed.id, "mp3")
                if (!seedFile.exists()) {
                    runtime.context.assets.open(seed.assetName).use { input ->
                        seedFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }

                if (seed.id !in existingIds) {
                    updatedFiles += AudioFile(
                        id = seed.id,
                        filename = seed.filename,
                        timeDisplay = "内置样本",
                        source = AudioSource.PHONE,
                        status = TranscriptionStatus.PENDING,
                        localAvailability = AudioLocalAvailability.READY,
                        isStarred = seed.isStarred
                    )
                }
            }

            if (updatedFiles != runtime.audioFiles.value) {
                runtime.audioFiles.value = updatedFiles
                runtime.metadataFile.parentFile?.mkdirs()
                runtime.metadataFile.writeText(runtime.json.encodeToString(updatedFiles))
            } else if (!runtime.metadataFile.exists()) {
                runtime.metadataFile.parentFile?.mkdirs()
                runtime.metadataFile.writeText(runtime.json.encodeToString(runtime.audioFiles.value))
            }
        }.onFailure {
            android.util.Log.e("SimAudioRepository", "seed inventory failed", it)
        }
    }

    private fun pruneRetiredSeedInventory() {
        if (runtime.retiredSeedIds.isEmpty()) return

        runtime.retiredSeedIds.forEach { retiredSeedId ->
            runtime.observationJobs.remove(retiredSeedId)?.cancel()
            resolveSimStoredAudioFile(runtime.context, retiredSeedId)?.delete()
            simArtifactFile(runtime.context, retiredSeedId).delete()
        }

        val retainedFiles = runtime.audioFiles.value.filterNot { it.id in runtime.retiredSeedIds }
        if (retainedFiles == runtime.audioFiles.value) return

        runtime.audioFiles.value = retainedFiles
        runtime.metadataFile.parentFile?.mkdirs()
        runtime.metadataFile.writeText(runtime.json.encodeToString(retainedFiles))
    }

    suspend fun createQueuedBadgePlaceholders(filenames: List<String>, badgeMac: String? = currentBadgeMac()): Int {
        if (filenames.isEmpty()) return 0
        val normalizedFilenames = filenames.map(::simPendingBadgeDeleteFilename)
        var createdCount = 0
        mutateAndSave { current ->
            val existingByFilename = current
                .filter { badgeMac == null || it.badgeMac == badgeMac }
                .map { simPendingBadgeDeleteFilename(it.filename) }
                .toMutableSet()
            val updated = current.toMutableList()
            normalizedFilenames.forEach { normalizedFilename ->
                if (!existingByFilename.add(normalizedFilename)) return@forEach
                updated += AudioFile(
                    id = UUID.randomUUID().toString(),
                    filename = normalizedFilename,
                    timeDisplay = "Just now",
                    source = AudioSource.SMARTBADGE,
                    status = TranscriptionStatus.PENDING,
                    localAvailability = AudioLocalAvailability.QUEUED,
                    isStarred = false,
                    badgeMac = badgeMac
                )
                createdCount += 1
            }
            updated
        }
        return createdCount
    }

    suspend fun markBadgeDownloadAvailability(
        filename: String,
        availability: AudioLocalAvailability,
        errorMessage: String? = null,
        badgeMac: String? = null
    ) {
        val normalizedFilename = simPendingBadgeDeleteFilename(filename)
        mutateAndSave { current ->
            current.map { audio ->
                if (matchesBadgeAudio(audio, normalizedFilename, badgeMac)) {
                    audio.copy(
                        localAvailability = availability,
                        lastErrorMessage = when {
                            errorMessage != null -> errorMessage
                            availability == AudioLocalAvailability.READY ||
                                availability == AudioLocalAvailability.DOWNLOADING ||
                                availability == AudioLocalAvailability.QUEUED -> null
                            else -> audio.lastErrorMessage
                        },
                        downloadProgress = 0f,
                        downloadedBytes = 0L,
                        downloadTotalBytes = 0L
                    )
                } else {
                    audio
                }
            }
        }
    }

    fun updateBadgeDownloadProgress(
        filename: String,
        downloadProgress: Float,
        downloadedBytes: Long,
        downloadTotalBytes: Long,
        badgeMac: String? = null
    ) {
        val normalizedFilename = simPendingBadgeDeleteFilename(filename)
        runtime.audioFiles.update { current ->
            current.map { audio ->
                if (matchesBadgeAudio(audio, normalizedFilename, badgeMac)) {
                    audio.copy(
                        downloadProgress = downloadProgress,
                        downloadedBytes = downloadedBytes,
                        downloadTotalBytes = downloadTotalBytes
                    )
                } else {
                    audio
                }
            }
        }
    }

    suspend fun removeBadgeAudioByFilename(filename: String, badgeMac: String? = null): Boolean {
        val normalizedFilename = simPendingBadgeDeleteFilename(filename)
        var removed = false
        mutateAndSave { current ->
            current.filterNot { audio ->
                val matches = matchesBadgeAudio(audio, normalizedFilename, badgeMac)
                if (matches) {
                    removed = true
                    runtime.observationJobs.remove(audio.id)?.cancel()
                    resolveSimStoredAudioFile(runtime.context, audio.id)?.delete()
                    simArtifactFile(runtime.context, audio.id).delete()
                }
                matches
            }
        }
        return removed
    }

    suspend fun importDownloadedBadgeAudio(filename: String, downloadedFile: File, badgeMac: String? = currentBadgeMac()) {
        val existing = getAudioByNormalizedBadgeFilename(filename, badgeMac)
        val audioId = existing?.id ?: UUID.randomUUID().toString()
        val destFile = simStoredAudioFile(runtime.context, audioId, "wav")
        downloadedFile.copyTo(destFile, overwrite = true)
        downloadedFile.delete()

        val normalizedFilename = simPendingBadgeDeleteFilename(filename)
        val updatedFile = AudioFile(
            id = audioId,
            filename = normalizedFilename,
            timeDisplay = existing?.timeDisplay ?: "Just now",
            source = AudioSource.SMARTBADGE,
            status = existing?.status ?: TranscriptionStatus.PENDING,
            localAvailability = AudioLocalAvailability.READY,
            isStarred = existing?.isStarred ?: false,
            isTestImport = existing?.isTestImport ?: false,
            summary = existing?.summary,
            progress = if (existing?.status == TranscriptionStatus.TRANSCRIBING) {
                existing.progress
            } else {
                0f
            },
            boundSessionId = existing?.boundSessionId,
            activeJobId = existing?.activeJobId,
            lastErrorMessage = null,
            badgeMac = existing?.badgeMac ?: badgeMac
        )
        mutateAndSave { current ->
            if (existing == null) {
                current + updatedFile
            } else {
                current.map { audio -> if (audio.id == audioId) updatedFile else audio }
            }
        }
    }

    suspend fun clearPendingBadgeDeletes(filenames: Set<String>) {
        if (filenames.isEmpty()) return
        runtime.fileMutex.withLock {
            val updated = runtime.pendingBadgeDeletes.value - filenames
            if (updated == runtime.pendingBadgeDeletes.value) return@withLock
            runtime.pendingBadgeDeletes.value = updated
            writePendingBadgeDeletesLocked(updated)
        }
    }

    suspend fun markInterruptedBadgeDownloadsForDevice(badgeMac: String?) {
        if (badgeMac.isNullOrBlank()) return
        mutateAndSave { current ->
            current.map { audio ->
                if (
                    audio.source == AudioSource.SMARTBADGE &&
                    audio.badgeMac == badgeMac &&
                    (
                        audio.localAvailability == AudioLocalAvailability.DOWNLOADING ||
                            audio.localAvailability == AudioLocalAvailability.QUEUED
                        )
                ) {
                    audio.copy(
                        localAvailability = AudioLocalAvailability.FAILED,
                        lastErrorMessage = BADGE_SWITCH_INTERRUPTED_MESSAGE,
                        downloadProgress = 0f,
                        downloadedBytes = 0L,
                        downloadTotalBytes = 0L
                    )
                } else {
                    audio
                }
            }
        }
    }

    internal suspend fun mutateAndSave(
        mutate: (List<AudioFile>) -> List<AudioFile>
    ) {
        runtime.fileMutex.withLock {
            val newList = mutate(runtime.audioFiles.value)
            runtime.audioFiles.value = newList
            writeMetadataLocked(newList)
        }
    }

    private suspend fun persistTestAudioEntry(
        audioId: String,
        extension: String,
        displayFilename: String,
        writeContent: (File) -> Unit
    ) {
        val destFile = simStoredAudioFile(runtime.context, audioId, extension)
        runCatching {
            writeContent(destFile)
        }.getOrElse { error ->
            destFile.delete()
            throw Exception("无法写入测试音频文件", error)
        }

        val newFile = AudioFile(
            id = audioId,
            filename = displayFilename,
            timeDisplay = "Just now",
            source = AudioSource.PHONE,
            status = TranscriptionStatus.PENDING,
            localAvailability = AudioLocalAvailability.READY,
            isStarred = false,
            isTestImport = true
        )
        mutateAndSave { current -> current + newFile }
    }

    private fun saveToDiskAsync() {
        runtime.repositoryScope.launch {
            runCatching {
                runtime.fileMutex.withLock {
                    writeMetadataLocked(runtime.audioFiles.value)
                }
            }.onFailure {
                android.util.Log.e("SimAudioRepository", "save metadata failed", it)
            }
        }
    }

    private fun currentBadgeMac(): String? = runtime.deviceRegistryManager.activeDevice.value?.macAddress

    private fun matchesBadgeAudio(audio: AudioFile, normalizedFilename: String, badgeMac: String?): Boolean {
        return simPendingBadgeDeleteFilename(audio.filename) == normalizedFilename &&
            (badgeMac == null || audio.badgeMac == badgeMac)
    }

    private suspend fun writeMetadataLocked(entries: List<AudioFile>) {
        withContext(runtime.ioDispatcher) {
            runtime.metadataFile.parentFile?.mkdirs()
            runtime.metadataFile.writeText(runtime.json.encodeToString(entries))
        }
    }

    private suspend fun writePendingBadgeDeletesLocked(filenames: Set<String>) {
        withContext(runtime.ioDispatcher) {
            runtime.pendingBadgeDeleteFile.parentFile?.mkdirs()
            runtime.pendingBadgeDeleteFile.writeText(
                runtime.json.encodeToString(filenames.toList().sorted())
            )
        }
    }
}
