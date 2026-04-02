package com.smartsales.prism.data.audio

import android.util.Log
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.core.util.Result
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal const val SIM_AUDIO_BADGE_SYNC_REQUESTED_SUMMARY = "SIM audio badge sync requested"
internal const val SIM_AUDIO_BADGE_SYNC_COMPLETED_SUMMARY = "SIM audio badge sync completed"
internal const val SIM_AUDIO_SYNC_FAILED_WHEN_CONNECTIVITY_UNAVAILABLE_SUMMARY =
    "SIM audio sync failed while connectivity unavailable"
internal const val SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE =
    "当前无法连接徽章，暂时不能同步录音。请检查设备连接后重试。"

private const val SIM_BADGE_SYNC_LIST_FAILURE_MESSAGE =
    "暂时无法获取徽章录音列表，请稍后重试。"
private const val SIM_BADGE_SYNC_DOWNLOAD_FAILURE_MESSAGE =
    "发现新的徽章录音，但暂时无法下载。请稍后重试。"
private const val SIM_AUDIO_OFFLINE_LOG_TAG = "AudioPipeline"
private const val SIM_AUDIO_SYNC_LOG_TAG = "AudioPipeline"

// WAV 头部为 44 字节；低于 1KB 的文件不可能包含有意义的音频内容
private const val MIN_BADGE_WAV_SIZE_BYTES = 1024L

internal enum class SimBadgeSyncTrigger {
    MANUAL,
    AUTO
}

internal enum class SimBadgeSyncSkippedReason {
    NOT_READY,
    ALREADY_RUNNING
}

internal enum class SimBadgeSyncResultBranch {
    DEVICE_EMPTY,
    ALREADY_PRESENT,
    IMPORTED
}

internal data class SimBadgeSyncOutcome(
    val trigger: SimBadgeSyncTrigger,
    val importedCount: Int = 0,
    val skippedEmptyCount: Int = 0,
    val resultBranch: SimBadgeSyncResultBranch? = null,
    val skippedReason: SimBadgeSyncSkippedReason? = null
)

private data class SimBadgeSyncExecutionResult(
    val importedCount: Int,
    val skippedEmptyCount: Int,
    val resultBranch: SimBadgeSyncResultBranch
)

internal fun existingSimBadgeFilenames(entries: List<AudioFile>): Set<String> {
    return entries
        .filter { it.source == AudioSource.SMARTBADGE }
        .map { normalizeSimBadgeFilename(it.filename) }
        .filter(String::isNotBlank)
        .toSet()
}

internal fun selectNewSimBadgeFilenames(
    badgeFilenames: List<String>,
    existingBadgeFilenames: Set<String>,
    pendingBadgeDeleteFilenames: Set<String> = emptySet()
): List<String> {
    val normalizedExisting = existingBadgeFilenames
        .map(::normalizeSimBadgeFilename)
        .filter(String::isNotBlank)
        .toSet()
    val normalizedPendingDeletes = pendingBadgeDeleteFilenames
        .map(::normalizeSimBadgeFilename)
        .filter(String::isNotBlank)
        .toSet()

    return badgeFilenames
        .map(::normalizeSimBadgeFilename)
        .filter(String::isNotBlank)
        .distinct()
        .filter { it !in normalizedExisting }
        .filter { it !in normalizedPendingDeletes }
}

internal fun normalizeSimBadgeFilename(filename: String): String = simPendingBadgeDeleteFilename(filename)

internal fun simBadgeSyncSuccessMessage(outcome: SimBadgeSyncOutcome): String {
    val skippedSuffix = if (outcome.skippedEmptyCount > 0) {
        "（跳过 ${outcome.skippedEmptyCount} 条空录音）"
    } else ""
    return when (outcome.resultBranch) {
        SimBadgeSyncResultBranch.DEVICE_EMPTY -> "徽章当前没有录音"
        SimBadgeSyncResultBranch.ALREADY_PRESENT -> "录音已在列表中，无需重复同步$skippedSuffix"
        SimBadgeSyncResultBranch.IMPORTED -> "已同步 ${outcome.importedCount} 条徽章录音$skippedSuffix"
        null -> error("sync outcome message requires a completed result branch")
    }
}

internal fun emitSimAudioBadgeSyncRequestedTelemetry(
    trigger: SimBadgeSyncTrigger,
    log: (String) -> Unit = { message -> android.util.Log.d(SIM_AUDIO_SYNC_LOG_TAG, message) }
) {
    val detail = "trigger=${trigger.name.lowercase()}"
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = detail.length,
        summary = SIM_AUDIO_BADGE_SYNC_REQUESTED_SUMMARY,
        rawDataDump = detail
    )
    log("$SIM_AUDIO_BADGE_SYNC_REQUESTED_SUMMARY: $detail")
}

internal fun emitSimAudioBadgeSyncCompletedTelemetry(
    trigger: SimBadgeSyncTrigger,
    importedCount: Int,
    resultBranch: SimBadgeSyncResultBranch,
    log: (String) -> Unit = { message -> android.util.Log.d(SIM_AUDIO_SYNC_LOG_TAG, message) }
) {
    val detail =
        "trigger=${trigger.name.lowercase()} importedCount=$importedCount branch=${resultBranch.name.lowercase()}"
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = detail.length,
        summary = SIM_AUDIO_BADGE_SYNC_COMPLETED_SUMMARY,
        rawDataDump = detail
    )
    log("$SIM_AUDIO_BADGE_SYNC_COMPLETED_SUMMARY: $detail")
}

internal fun emitSimAudioSyncFailureWhileConnectivityUnavailableTelemetry(
    detail: String,
    log: (String) -> Unit = { message -> android.util.Log.d(SIM_AUDIO_OFFLINE_LOG_TAG, message) }
) {
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = detail.length,
        summary = SIM_AUDIO_SYNC_FAILED_WHEN_CONNECTIVITY_UNAVAILABLE_SUMMARY,
        rawDataDump = detail
    )
    log("$SIM_AUDIO_SYNC_FAILED_WHEN_CONNECTIVITY_UNAVAILABLE_SUMMARY: $detail")
}

internal fun isLikelySimBadgeConnectivityUnavailable(rawReason: String?): Boolean {
    val normalized = rawReason
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }
        ?: return true

    return listOf(
        "oss_unknown",
        "timeout",
        "timed out",
        "offline",
        "network",
        "socket",
        "connect",
        "connection",
        "unreachable",
        "refused",
        "no route",
        "unable to resolve",
        "unknown host",
        "dns",
        "null"
    ).any { normalized.contains(it) }
}

internal fun buildSimBadgeSyncListFailureMessage(rawReason: String?): String {
    return if (isLikelySimBadgeConnectivityUnavailable(rawReason)) {
        SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE
    } else {
        SIM_BADGE_SYNC_LIST_FAILURE_MESSAGE
    }
}

internal fun buildSimBadgeSyncDownloadFailureMessage(rawReason: String?): String {
    return if (isLikelySimBadgeConnectivityUnavailable(rawReason)) {
        SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE
    } else {
        SIM_BADGE_SYNC_DOWNLOAD_FAILURE_MESSAGE
    }
}

internal class SimAudioRepositorySyncSupport(
    private val runtime: SimAudioRepositoryRuntime,
    private val storeSupport: SimAudioRepositoryStoreSupport
) {

    suspend fun canSyncFromBadge(): Boolean = withContext(runtime.ioDispatcher) {
        runtime.connectivityBridge.isReady()
    }

    suspend fun syncFromBadge(trigger: SimBadgeSyncTrigger): SimBadgeSyncOutcome = withContext(runtime.ioDispatcher) {
        Log.d(
            SIM_AUDIO_SYNC_LOG_TAG,
            "SIM badge sync preflight start trigger=${trigger.name.lowercase()}"
        )
        val isReady = canSyncFromBadge()
        Log.d(
            SIM_AUDIO_SYNC_LOG_TAG,
            "SIM badge sync preflight result trigger=${trigger.name.lowercase()} isReady=$isReady"
        )
        if (trigger == SimBadgeSyncTrigger.AUTO && !isReady) {
            Log.d(
                SIM_AUDIO_SYNC_LOG_TAG,
                "SIM badge sync skipped trigger=auto stage=strict-preflight reason=not-ready"
            )
            return@withContext SimBadgeSyncOutcome(
                trigger = trigger,
                skippedReason = SimBadgeSyncSkippedReason.NOT_READY
            )
        }

        if (trigger == SimBadgeSyncTrigger.MANUAL && !isReady) {
            Log.d(
                SIM_AUDIO_SYNC_LOG_TAG,
                "SIM badge sync blocked trigger=manual stage=strict-preflight reason=not-ready"
            )
            emitSimAudioSyncFailureWhileConnectivityUnavailableTelemetry(
                detail = "manual preflight failed: connectivity not ready"
            )
            throw Exception(SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE)
        }

        if (!runtime.syncMutex.tryLock()) {
            return@withContext SimBadgeSyncOutcome(
                trigger = trigger,
                skippedReason = SimBadgeSyncSkippedReason.ALREADY_RUNNING
            )
        }

        try {
            emitSimAudioBadgeSyncRequestedTelemetry(trigger)
            val executionResult = performBadgeSyncLocked()
            emitSimAudioBadgeSyncCompletedTelemetry(
                trigger = trigger,
                importedCount = executionResult.importedCount,
                resultBranch = executionResult.resultBranch
            )
            SimBadgeSyncOutcome(
                trigger = trigger,
                importedCount = executionResult.importedCount,
                skippedEmptyCount = executionResult.skippedEmptyCount,
                resultBranch = executionResult.resultBranch
            )
        } finally {
            runtime.syncMutex.unlock()
        }
    }

    suspend fun syncFromDevice(): Unit = withContext(runtime.ioDispatcher) {
        runtime.syncMutex.withLock {
            performBadgeSyncLocked()
        }
    }

    private suspend fun performBadgeSyncLocked(): SimBadgeSyncExecutionResult {
        val listResult = runtime.connectivityBridge.listRecordings()
        val badgeFiles = when (listResult) {
            is Result.Success -> {
                val normalized = listResult.data
                    .map(::normalizeSimBadgeFilename)
                    .filter(String::isNotBlank)
                    .distinct()
                Log.d(
                    SIM_AUDIO_SYNC_LOG_TAG,
                    "SIM badge sync listRecordings success count=${normalized.size}"
                )
                normalized
            }
            is Result.Error -> {
                val reason = listResult.throwable.message ?: "unknown"
                emitSimAudioSyncFailureWhileConnectivityUnavailableTelemetry(
                    detail = "listRecordings failed: $reason"
                )
                throw Exception(buildSimBadgeSyncListFailureMessage(reason))
            }
        }

        val existingBadgeCount = existingSimBadgeFilenames(runtime.audioFiles.value).size
        val suppressedPendingDeletes = reconcilePendingBadgeDeletes(badgeFiles)
        val pendingDeleteFilenames =
            suppressedPendingDeletes + storeSupport.getPendingBadgeDeletesSnapshot()

        val newFilesToDownload = selectNewSimBadgeFilenames(
            badgeFilenames = badgeFiles,
            existingBadgeFilenames = existingSimBadgeFilenames(runtime.audioFiles.value),
            pendingBadgeDeleteFilenames = pendingDeleteFilenames
        )

        var importedCount = 0
        var skippedEmptyCount = 0
        var failedDownloadCount = 0
        var firstDownloadFailureReason: String? = null
        for (filename in newFilesToDownload) {
            when (val downloadResult = runtime.connectivityBridge.downloadRecording(filename)) {
                is WavDownloadResult.Success -> {
                    if (downloadResult.sizeBytes < MIN_BADGE_WAV_SIZE_BYTES) {
                        // 空录音（误触/假启动）——静默跳过，清理临时文件
                        Log.d(
                            SIM_AUDIO_SYNC_LOG_TAG,
                            "SIM badge sync skipped empty recording filename=$filename sizeBytes=${downloadResult.sizeBytes}"
                        )
                        downloadResult.localFile.delete()
                        skippedEmptyCount += 1
                        continue
                    }
                    storeSupport.importDownloadedBadgeAudio(filename, downloadResult.localFile)
                    importedCount += 1
                    Log.d(
                        SIM_AUDIO_SYNC_LOG_TAG,
                        "SIM badge sync downloadRecording success filename=$filename sizeBytes=${downloadResult.sizeBytes}"
                    )
                }

                is WavDownloadResult.Error -> {
                    failedDownloadCount += 1
                    if (firstDownloadFailureReason == null) {
                        firstDownloadFailureReason = downloadResult.message
                    }
                    emitSimAudioSyncFailureWhileConnectivityUnavailableTelemetry(
                        detail = "downloadRecording failed filename=$filename reason=${downloadResult.message}"
                    )
                    android.util.Log.e(
                        "SimAudioRepository",
                        "syncFromDevice failed: ${downloadResult.message}"
                    )
                }
            }
        }

        if (newFilesToDownload.isNotEmpty() && importedCount == 0 && failedDownloadCount > 0) {
            throw Exception(buildSimBadgeSyncDownloadFailureMessage(firstDownloadFailureReason))
        }

        val resultBranch = when {
            badgeFiles.isEmpty() -> SimBadgeSyncResultBranch.DEVICE_EMPTY
            importedCount > 0 -> SimBadgeSyncResultBranch.IMPORTED
            else -> SimBadgeSyncResultBranch.ALREADY_PRESENT
        }

        Log.d(
            SIM_AUDIO_SYNC_LOG_TAG,
            "SIM badge sync outcome badgeListCount=${badgeFiles.size} existingBadgeCount=$existingBadgeCount pendingDeleteCount=${pendingDeleteFilenames.size} newFilesToDownloadCount=${newFilesToDownload.size} importedCount=$importedCount skippedEmptyCount=$skippedEmptyCount branch=${resultBranch.name.lowercase()}"
        )

        return SimBadgeSyncExecutionResult(
            importedCount = importedCount,
            skippedEmptyCount = skippedEmptyCount,
            resultBranch = resultBranch
        )
    }

    private suspend fun reconcilePendingBadgeDeletes(badgeFiles: List<String>): Set<String> {
        val currentPendingDeletes = storeSupport.getPendingBadgeDeletesSnapshot()
        if (currentPendingDeletes.isEmpty()) return emptySet()

        val badgeFilenameSet = badgeFiles.map(::simPendingBadgeDeleteFilename).toSet()
        val missingOnBadge = currentPendingDeletes - badgeFilenameSet
        if (missingOnBadge.isNotEmpty()) {
            storeSupport.clearPendingBadgeDeletes(missingOnBadge)
        }

        val stillPresentOnBadge = storeSupport.getPendingBadgeDeletesSnapshot().intersect(badgeFilenameSet)
        stillPresentOnBadge.forEach { filename ->
            val deleted = runCatching {
                runtime.connectivityBridge.deleteRecording(filename)
            }.getOrDefault(false)
            if (deleted) {
                storeSupport.clearPendingBadgeDeletes(setOf(filename))
            } else {
                Log.w(
                    SIM_AUDIO_SYNC_LOG_TAG,
                    "SIM badge pending delete still blocked filename=$filename"
                )
            }
        }
        return currentPendingDeletes.intersect(badgeFilenameSet)
    }
}
