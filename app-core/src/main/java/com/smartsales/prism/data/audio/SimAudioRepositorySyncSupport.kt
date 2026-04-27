package com.smartsales.prism.data.audio

import android.util.Log
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.BadgeEndpointSnapshot
import com.smartsales.prism.data.connectivity.BadgeRuntimeKey
import com.smartsales.prism.data.connectivity.legacy.PhoneWifiSnapshot
import com.smartsales.prism.data.connectivity.legacy.currentNormalizedSsid
import com.smartsales.prism.domain.connectivity.IsolationTriggerContext
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioLocalAvailability
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import com.smartsales.prism.service.DownloadServiceOrchestrator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
    ALREADY_RUNNING,
    KNOWN_HTTP_UNREACHABLE
}

/**
 * 同步前安全检查门检测到疑似 AP 客户端隔离时抛出。
 * 由调用方（SimAudioDrawerViewModel）捕获，区别于一般连接不可用异常，
 * 避免展示错误 snackbar（隔离提示已通过 ConnectivityModal 单独处理）。
 */
internal class IsolationBlockedSyncException(val badgeIp: String) : Exception("sync-isolation-blocked")

internal enum class SimBadgeSyncResultBranch {
    DEVICE_EMPTY,
    ALREADY_PRESENT,
    QUEUED
}

internal data class SimBadgeSyncOutcome(
    val trigger: SimBadgeSyncTrigger,
    val queuedCount: Int = 0,
    val retryQueuedCount: Int = 0,
    val skippedEmptyCount: Int = 0,
    val resultBranch: SimBadgeSyncResultBranch? = null,
    val skippedReason: SimBadgeSyncSkippedReason? = null
)

private data class SimBadgeSyncExecutionResult(
    val queuedCount: Int,
    val retryQueuedCount: Int,
    val resultBranch: SimBadgeSyncResultBranch
)

private data class SimBadgeQueuePlan(
    val placeholderFilenames: List<String>,
    val queueFilenames: List<String>
)

private data class SimKnownHttpUnreachableLatch(
    val runtimeKey: BadgeRuntimeKey,
    val badgeIp: String,
    val baseUrl: String
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
        SimBadgeSyncResultBranch.QUEUED -> {
            if (outcome.queuedCount > 0) {
                "已发现 ${outcome.queuedCount} 条徽章录音，正在后台同步"
            } else {
                "录音已在列表中，后台同步继续进行$skippedSuffix"
            }
        }
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
    queuedCount: Int,
    retryQueuedCount: Int,
    resultBranch: SimBadgeSyncResultBranch,
    log: (String) -> Unit = { message -> android.util.Log.d(SIM_AUDIO_SYNC_LOG_TAG, message) }
) {
    val detail =
        "trigger=${trigger.name.lowercase()} queuedCount=$queuedCount retryQueuedCount=$retryQueuedCount branch=${resultBranch.name.lowercase()}"
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

internal class SimAudioRepositorySyncSupport(
    private val runtime: SimAudioRepositoryRuntime,
    private val storeSupport: SimAudioRepositoryStoreSupport,
    private val orchestrator: DownloadServiceOrchestrator
) {
    private var knownHttpUnreachableLatch: SimKnownHttpUnreachableLatch? = null

    suspend fun canSyncFromBadge(): Boolean = withContext(runtime.ioDispatcher) {
        reconcileKnownHttpUnreachableLatch()
        val ready = runtime.connectivityBridge.isReady()
        if (ready) {
            clearKnownHttpUnreachableLatch("ready_probe_success")
        }
        ready
    }

    suspend fun shouldSuppressAutoSync(): Boolean = withContext(runtime.ioDispatcher) {
        shouldSuppressAutoSyncInternal(logSuppression = true)
    }

    suspend fun syncFromBadge(trigger: SimBadgeSyncTrigger): SimBadgeSyncOutcome = withContext(runtime.ioDispatcher) {
        syncFromBadgeInternal(trigger = trigger, requireStrictPreflight = true)
    }

    suspend fun syncFromBadgeAfterVerifiedReadiness(
        trigger: SimBadgeSyncTrigger
    ): SimBadgeSyncOutcome = withContext(runtime.ioDispatcher) {
        syncFromBadgeInternal(trigger = trigger, requireStrictPreflight = false)
    }

    private suspend fun syncFromBadgeInternal(
        trigger: SimBadgeSyncTrigger,
        requireStrictPreflight: Boolean
    ): SimBadgeSyncOutcome {
        if (trigger == SimBadgeSyncTrigger.AUTO && shouldSuppressAutoSyncInternal(logSuppression = true)) {
            return SimBadgeSyncOutcome(
                trigger = trigger,
                skippedReason = SimBadgeSyncSkippedReason.KNOWN_HTTP_UNREACHABLE
            )
        }

        if (requireStrictPreflight) {
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
                // 连接后 preflight 失败：手机 WiFi 已验证但 HTTP 不可达 → 疑似 AP 客户端隔离
                val latestIp = runtime.endpointRecoveryCoordinator.latestResolvedEndpoint()?.badgeIp
                val wifiSnapshot = runtime.phoneWifiProvider.currentWifiSnapshot()
                val phoneWifiValidated =
                    wifiSnapshot is PhoneWifiSnapshot.Connected && wifiSnapshot.isValidated
                if (phoneWifiValidated && latestIp != null) {
                    Log.w(
                        SIM_AUDIO_SYNC_LOG_TAG,
                        "SIM badge sync isolation suspected ip=$latestIp context=on_connect — triggering isolation prompt"
                    )
                    runtime.connectivityPrompt.promptSuspectedIsolation(
                        badgeIp = latestIp,
                        triggerContext = IsolationTriggerContext.ON_CONNECT,
                        suggestedSsid = (wifiSnapshot as? PhoneWifiSnapshot.Connected)?.normalizedSsid
                    )
                    maybeArmKnownHttpUnreachableLatch("on_connect_isolation", shouldArm = true)
                }
                return SimBadgeSyncOutcome(
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

                // 同步前安全检查门：preflight 失败时区分隔离与普通不可用
                // 手机 WiFi 已验证（互联网可达）但 HTTP 不可达 → 疑似 AP 客户端隔离
                val latestIp = runtime.endpointRecoveryCoordinator.latestResolvedEndpoint()?.badgeIp
                val wifiSnapshot = runtime.phoneWifiProvider.currentWifiSnapshot()
                val phoneWifiValidated =
                    wifiSnapshot is PhoneWifiSnapshot.Connected && wifiSnapshot.isValidated
                if (phoneWifiValidated && latestIp != null) {
                    Log.w(
                        SIM_AUDIO_SYNC_LOG_TAG,
                        "SIM badge sync isolation suspected ip=$latestIp context=pre_sync — blocking sync, triggering isolation prompt"
                    )
                    runtime.connectivityPrompt.promptSuspectedIsolation(
                        badgeIp = latestIp,
                        triggerContext = IsolationTriggerContext.PRE_SYNC,
                        suggestedSsid = (wifiSnapshot as? PhoneWifiSnapshot.Connected)?.normalizedSsid
                    )
                    throw IsolationBlockedSyncException(latestIp)
                }

                // 非隔离情形：手机 WiFi 未验证或无已知徽章 IP → 普通连接不可用
                promptWifiMismatchIfManual(trigger)
                throw Exception(SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE)
            }
        } else {
            Log.d(
                SIM_AUDIO_SYNC_LOG_TAG,
                "SIM badge sync preflight accepted upstream trigger=${trigger.name.lowercase()}"
            )
        }

        if (!runtime.syncMutex.tryLock()) {
            return SimBadgeSyncOutcome(
                trigger = trigger,
                skippedReason = SimBadgeSyncSkippedReason.ALREADY_RUNNING
            )
        }

        return try {
            emitSimAudioBadgeSyncRequestedTelemetry(trigger)
            val executionResult = performBadgeSyncLocked(trigger)
            if (trigger == SimBadgeSyncTrigger.MANUAL) {
                clearKnownHttpUnreachableLatch("manual_sync_success")
            }
            emitSimAudioBadgeSyncCompletedTelemetry(
                trigger = trigger,
                queuedCount = executionResult.queuedCount,
                retryQueuedCount = executionResult.retryQueuedCount,
                resultBranch = executionResult.resultBranch
            )
            SimBadgeSyncOutcome(
                trigger = trigger,
                queuedCount = executionResult.queuedCount,
                retryQueuedCount = executionResult.retryQueuedCount,
                resultBranch = executionResult.resultBranch
            )
        } finally {
            runtime.syncMutex.unlock()
        }
    }

    suspend fun syncFromDevice(): Unit = withContext(runtime.ioDispatcher) {
        runtime.syncMutex.withLock {
            performBadgeSyncLocked(SimBadgeSyncTrigger.MANUAL)
        }
    }

    private suspend fun performBadgeSyncLocked(
        trigger: SimBadgeSyncTrigger
    ): SimBadgeSyncExecutionResult {
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
                maybeArmKnownHttpUnreachableLatch(
                    reason = "list_failed:${reason}",
                    shouldArm = isLikelySimBadgeConnectivityUnavailable(reason)
                )
                emitSimAudioSyncFailureWhileConnectivityUnavailableTelemetry(
                    detail = "listRecordings failed: $reason"
                )
                promptWifiMismatchIfManual(trigger)
                throw Exception(buildSimBadgeSyncListFailureMessage(reason))
            }
        }

        val suppressedPendingDeletes = reconcilePendingBadgeDeletes(badgeFiles)
        val pendingDeleteFilenames =
            suppressedPendingDeletes + storeSupport.getPendingBadgeDeletesSnapshot()

        val queuePlan = planBadgeDownloads(
            badgeFiles = badgeFiles,
            pendingDeleteFilenames = pendingDeleteFilenames
        )
        val createdCount = storeSupport.createQueuedBadgePlaceholders(queuePlan.placeholderFilenames)
        enqueueBadgeDownloads(queuePlan.queueFilenames)

        val resultBranch = when {
            badgeFiles.isEmpty() -> SimBadgeSyncResultBranch.DEVICE_EMPTY
            createdCount > 0 || queuePlan.queueFilenames.isNotEmpty() -> SimBadgeSyncResultBranch.QUEUED
            else -> SimBadgeSyncResultBranch.ALREADY_PRESENT
        }

        Log.d(
            SIM_AUDIO_SYNC_LOG_TAG,
            "SIM badge sync outcome badgeListCount=${badgeFiles.size} pendingDeleteCount=${pendingDeleteFilenames.size} placeholderCount=$createdCount queueCount=${queuePlan.queueFilenames.size} retryQueueCount=${queuePlan.queueFilenames.size - createdCount} branch=${resultBranch.name.lowercase()}"
        )

        return SimBadgeSyncExecutionResult(
            queuedCount = createdCount,
            retryQueuedCount = (queuePlan.queueFilenames.size - createdCount).coerceAtLeast(0),
            resultBranch = resultBranch
        )
    }

    suspend fun cancelBadgeDownload(filename: String) = withContext(runtime.ioDispatcher) {
        val normalizedFilename = normalizeSimBadgeFilename(filename)
        runtime.badgeDownloadQueueMutex.withLock {
            runtime.queuedBadgeDownloads.remove(normalizedFilename)
            if (runtime.activeBadgeDownloadFilename == normalizedFilename) {
                runtime.activeBadgeDownloadJob?.cancel(
                    CancellationException("badge audio deleted")
                )
            }
        }
        Log.d(
            SIM_AUDIO_SYNC_LOG_TAG,
            "SIM badge sync download canceled filename=$normalizedFilename"
        )
    }

    suspend fun cancelAllBadgeDownloads() = withContext(runtime.ioDispatcher) {
        runtime.badgeDownloadQueueMutex.withLock {
            runtime.queuedBadgeDownloads.clear()
            runtime.activeBadgeDownloadJob?.cancel(
                CancellationException("device switched")
            )
            runtime.badgeDownloadWorkerJob?.cancel(
                CancellationException("device switched")
            )
            runtime.activeBadgeDownloadFilename = null
            runtime.activeBadgeDownloadJob = null
            runtime.badgeDownloadWorkerJob = null
        }
        Log.d(SIM_AUDIO_SYNC_LOG_TAG, "SIM badge sync: all downloads cancelled (device switch)")
    }

    private fun planBadgeDownloads(
        badgeFiles: List<String>,
        pendingDeleteFilenames: Set<String>
    ): SimBadgeQueuePlan {
        val normalizedPendingDeletes = pendingDeleteFilenames
            .map(::normalizeSimBadgeFilename)
            .toSet()
        val currentEntries = runtime.audioFiles.value
            .filter { it.source == AudioSource.SMARTBADGE }
            .associateBy { normalizeSimBadgeFilename(it.filename) }

        val placeholderFilenames = mutableListOf<String>()
        val queueFilenames = mutableListOf<String>()

        badgeFiles.forEach { filename ->
            val normalizedFilename = normalizeSimBadgeFilename(filename)
            if (normalizedFilename in normalizedPendingDeletes) return@forEach

            val existing = currentEntries[normalizedFilename]
            when {
                existing == null -> {
                    placeholderFilenames += normalizedFilename
                    queueFilenames += normalizedFilename
                }
                existing.localAvailability == AudioLocalAvailability.QUEUED ||
                    existing.localAvailability == AudioLocalAvailability.FAILED -> {
                    queueFilenames += normalizedFilename
                }
                else -> Unit
            }
        }

        return SimBadgeQueuePlan(
            placeholderFilenames = placeholderFilenames.distinct(),
            queueFilenames = queueFilenames.distinct()
        )
    }

    private fun enqueueBadgeDownloads(filenames: List<String>) {
        if (filenames.isEmpty()) return
        orchestrator.notifyDownloadStarting()
        runtime.repositoryScope.launch {
            runtime.badgeDownloadQueueMutex.withLock {
                filenames.forEach { runtime.queuedBadgeDownloads.add(normalizeSimBadgeFilename(it)) }
                if (runtime.badgeDownloadWorkerJob?.isActive != true) {
                    runtime.badgeDownloadWorkerJob = runtime.repositoryScope.launch {
                        processBadgeDownloadQueue()
                    }
                }
            }
        }
    }

    private suspend fun processBadgeDownloadQueue() {
        Log.d(SIM_AUDIO_SYNC_LOG_TAG, "SIM badge sync background queue started")
        while (true) {
            val nextFilename = runtime.badgeDownloadQueueMutex.withLock {
                runtime.queuedBadgeDownloads.firstOrNull()?.also { runtime.queuedBadgeDownloads.remove(it) }
            } ?: break

            val placeholder = storeSupport.getAudioByNormalizedBadgeFilename(nextFilename)
            if (placeholder == null) {
                Log.d(
                    SIM_AUDIO_SYNC_LOG_TAG,
                    "SIM badge sync skipped missing placeholder filename=$nextFilename"
                )
                continue
            }
            if (normalizeSimBadgeFilename(placeholder.filename) in storeSupport.getPendingBadgeDeletesSnapshot()) {
                Log.d(
                    SIM_AUDIO_SYNC_LOG_TAG,
                    "SIM badge sync skipped tombstoned placeholder filename=$nextFilename"
                )
                continue
            }

            storeSupport.markBadgeDownloadAvailability(
                filename = nextFilename,
                availability = AudioLocalAvailability.DOWNLOADING,
                errorMessage = null
            )
            Log.d(
                SIM_AUDIO_SYNC_LOG_TAG,
                "SIM badge sync background download start filename=$nextFilename"
            )

            try {
                var lastProgressUpdateMs = 0L
                val onProgress: (Long, Long) -> Unit = { bytesRead, totalBytes ->
                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdateMs >= 200L || (totalBytes > 0 && bytesRead == totalBytes)) {
                        lastProgressUpdateMs = now
                        storeSupport.updateBadgeDownloadProgress(
                            filename = nextFilename,
                            downloadProgress = if (totalBytes > 0) {
                                (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f)
                            } else {
                                -1f
                            },
                            downloadedBytes = bytesRead,
                            downloadTotalBytes = totalBytes
                        )
                    }
                }
                val downloadResult = coroutineScope {
                    val activeDownload = async {
                        runtime.connectivityBridge.downloadRecording(nextFilename, onProgress)
                    }
                    runtime.activeBadgeDownloadFilename = nextFilename
                    runtime.activeBadgeDownloadJob = activeDownload
                    activeDownload.await()
                }

                if (nextFilename in storeSupport.getPendingBadgeDeletesSnapshot()) {
                    if (downloadResult is WavDownloadResult.Success) {
                        downloadResult.localFile.delete()
                    }
                    Log.d(
                        SIM_AUDIO_SYNC_LOG_TAG,
                        "SIM badge sync discarded deleted download filename=$nextFilename"
                    )
                    continue
                }

                when (downloadResult) {
                    is WavDownloadResult.Success -> {
                        if (downloadResult.sizeBytes < MIN_BADGE_WAV_SIZE_BYTES) {
                            downloadResult.localFile.delete()
                            storeSupport.removeBadgeAudioByFilename(nextFilename)
                            Log.d(
                                SIM_AUDIO_SYNC_LOG_TAG,
                                "SIM badge sync removed empty placeholder filename=$nextFilename sizeBytes=${downloadResult.sizeBytes}"
                            )
                            continue
                        }
                        storeSupport.importDownloadedBadgeAudio(nextFilename, downloadResult.localFile)
                        Log.d(
                            SIM_AUDIO_SYNC_LOG_TAG,
                            "SIM badge sync background download success filename=$nextFilename sizeBytes=${downloadResult.sizeBytes}"
                        )
                    }

                    is WavDownloadResult.Error -> {
                        maybeArmKnownHttpUnreachableLatch(
                            reason = "download_failed:${downloadResult.message}",
                            shouldArm = downloadResult.code == WavDownloadResult.ErrorCode.DOWNLOAD_FAILED &&
                                isLikelySimBadgeConnectivityUnavailable(downloadResult.message)
                        )
                        storeSupport.markBadgeDownloadAvailability(
                            filename = nextFilename,
                            availability = AudioLocalAvailability.FAILED,
                            errorMessage = downloadResult.message
                        )
                        emitSimAudioSyncFailureWhileConnectivityUnavailableTelemetry(
                            detail = "downloadRecording failed filename=$nextFilename reason=${downloadResult.message}"
                        )
                        Log.e(
                            SIM_AUDIO_SYNC_LOG_TAG,
                            "SIM badge sync background download failed filename=$nextFilename reason=${downloadResult.message}"
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                storeSupport.markBadgeDownloadAvailability(
                    filename = nextFilename,
                    availability = AudioLocalAvailability.FAILED,
                    errorMessage = "下载被中断，请重试同步"
                )
                Log.d(
                    SIM_AUDIO_SYNC_LOG_TAG,
                    "SIM badge sync background download canceled filename=$nextFilename"
                )
            } finally {
                runtime.activeBadgeDownloadFilename = null
                runtime.activeBadgeDownloadJob = null
            }
        }
        Log.d(SIM_AUDIO_SYNC_LOG_TAG, "SIM badge sync background queue drained")
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

    private suspend fun promptWifiMismatchIfManual(trigger: SimBadgeSyncTrigger) {
        if (trigger != SimBadgeSyncTrigger.MANUAL) return
        Log.d(
            SIM_AUDIO_SYNC_LOG_TAG,
            "SIM badge sync prompt wifi_mismatch requested source=repository_sync trigger=${trigger.name.lowercase()}"
        )
        runtime.connectivityPrompt.promptWifiMismatch(
            runtime.phoneWifiProvider.currentNormalizedSsid()
        )
    }

    private suspend fun shouldSuppressAutoSyncInternal(logSuppression: Boolean): Boolean {
        reconcileKnownHttpUnreachableLatch()
        val latch = knownHttpUnreachableLatch ?: return false
        if (logSuppression) {
            Log.i(
                SIM_AUDIO_SYNC_LOG_TAG,
                "SIM badge auto sync suppressed runtime=${latch.runtimeKey.toLogString()} baseUrl=${latch.baseUrl} reason=known_http_unreachable"
            )
        }
        return true
    }

    private suspend fun reconcileKnownHttpUnreachableLatch() {
        val latch = knownHttpUnreachableLatch ?: return
        val currentRuntimeKey = runtime.endpointRecoveryCoordinator.currentRuntimeKey()
        if (currentRuntimeKey != latch.runtimeKey) {
            clearKnownHttpUnreachableLatch("runtime_changed")
            return
        }

        val latestEndpoint = runtime.endpointRecoveryCoordinator.latestResolvedEndpoint()
        if (latestEndpoint != null && endpointChanged(latch, latestEndpoint)) {
            clearKnownHttpUnreachableLatch("endpoint_changed")
        }
    }

    private suspend fun maybeArmKnownHttpUnreachableLatch(
        reason: String,
        shouldArm: Boolean
    ) {
        if (!shouldArm) return
        val latestEndpoint = runtime.endpointRecoveryCoordinator.latestResolvedEndpoint() ?: return
        val nextLatch = SimKnownHttpUnreachableLatch(
            runtimeKey = latestEndpoint.runtimeKey,
            badgeIp = latestEndpoint.badgeIp,
            baseUrl = latestEndpoint.baseUrl
        )
        if (knownHttpUnreachableLatch == nextLatch) return
        knownHttpUnreachableLatch = nextLatch
        Log.i(
            SIM_AUDIO_SYNC_LOG_TAG,
            "SIM badge auto sync latch armed runtime=${nextLatch.runtimeKey.toLogString()} baseUrl=${nextLatch.baseUrl} reason=$reason"
        )
    }

    private fun clearKnownHttpUnreachableLatch(reason: String) {
        val latch = knownHttpUnreachableLatch ?: return
        knownHttpUnreachableLatch = null
        Log.i(
            SIM_AUDIO_SYNC_LOG_TAG,
            "SIM badge auto sync latch cleared runtime=${latch.runtimeKey.toLogString()} baseUrl=${latch.baseUrl} reason=$reason"
        )
    }

    private fun endpointChanged(
        latch: SimKnownHttpUnreachableLatch,
        latestEndpoint: BadgeEndpointSnapshot
    ): Boolean {
        return latestEndpoint.badgeIp != latch.badgeIp ||
            latestEndpoint.baseUrl != latch.baseUrl
    }
}
