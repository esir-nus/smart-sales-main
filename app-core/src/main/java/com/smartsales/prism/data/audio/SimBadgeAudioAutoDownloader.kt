// File: app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt
// Module: :app-core
// Summary: rec# 音频自动下载器 — 收到 BLE rec# 通知后立即创建占位符并后台下载，不转写
// Author: created on 2026-04-09
package com.smartsales.prism.data.audio

import android.util.Log
import com.smartsales.prism.domain.audio.AudioLocalAvailability
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import com.smartsales.prism.service.DownloadServiceOrchestrator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// WAV 头部为 44 字节；低于 1KB 的文件不可能包含有意义的音频内容
private const val MIN_REC_WAV_SIZE_BYTES = 1024L
private const val TAG = "AudioPipeline"

@Singleton
class SimBadgeAudioAutoDownloader @Inject constructor(
    private val connectivityBridge: ConnectivityBridge,
    private val runtime: SimAudioRepositoryRuntime,
    private val orchestrator: DownloadServiceOrchestrator
) {

    private val storeSupport = SimAudioRepositoryStoreSupport(runtime)

    private val _syncIslandEvents = MutableSharedFlow<SimBadgeSyncIslandEvent>(
        replay = 0,
        extraBufferCapacity = 4
    )
    internal val syncIslandEvents: SharedFlow<SimBadgeSyncIslandEvent> = _syncIslandEvents.asSharedFlow()

    init {
        runtime.repositoryScope.launch {
            connectivityBridge.audioRecordingNotifications().collect { notification ->
                handleAudioRecordingReady(notification.filename)
            }
        }
    }

    private suspend fun handleAudioRecordingReady(filename: String) {
        if (filename.isBlank()) {
            Log.w(TAG, "rec# auto-download: blank filename, skipping")
            return
        }

        val ownerBadgeMac = currentRuntimeBadgeMac()
        Log.d(TAG, "rec# auto-download: notification received filename=$filename badgeMac=$ownerBadgeMac")

        // 立即在抽屉创建 QUEUED 占位符
        storeSupport.createQueuedBadgePlaceholders(listOf(filename), ownerBadgeMac)
        Log.d(TAG, "rec# auto-download: placeholder created filename=$filename")
        orchestrator.notifyDownloadStarting()

        // 后台下载
        runtime.repositoryScope.launch {
            downloadAndUpgrade(filename, ownerBadgeMac)
        }
    }

    private suspend fun downloadAndUpgrade(filename: String, ownerBadgeMac: String?) {
        storeSupport.markBadgeDownloadAvailability(
            filename = filename,
            availability = AudioLocalAvailability.DOWNLOADING,
            errorMessage = null,
            badgeMac = ownerBadgeMac
        )
        Log.d(TAG, "rec# auto-download: downloading filename=$filename badgeMac=$ownerBadgeMac")

        when (val result = connectivityBridge.downloadRecording(filename)) {
            is WavDownloadResult.Success -> {
                if (result.sizeBytes < MIN_REC_WAV_SIZE_BYTES) {
                    // 空文件，移除占位符
                    result.localFile.delete()
                    storeSupport.removeBadgeAudioByFilename(filename, ownerBadgeMac)
                    notifyCommandEndAsync()
                    Log.d(
                        TAG,
                        "rec# auto-download: removed empty placeholder filename=$filename sizeBytes=${result.sizeBytes}"
                    )
                    return
                }
                if (currentRuntimeBadgeMac() != ownerBadgeMac) {
                    result.localFile.delete()
                    storeSupport.markBadgeDownloadAvailability(
                        filename = filename,
                        availability = AudioLocalAvailability.FAILED,
                        errorMessage = "设备已切换，请重新同步",
                        badgeMac = ownerBadgeMac
                    )
                    notifyCommandEndAsync()
                    Log.d(
                        TAG,
                        "rec# auto-download: discarded cross-device download filename=$filename badgeMac=$ownerBadgeMac"
                    )
                    return
                }
                storeSupport.importDownloadedBadgeAudio(filename, result.localFile, ownerBadgeMac)
                notifyCommandEndAsync()
                Log.d(
                    TAG,
                    "rec# auto-download: success filename=$filename badgeMac=$ownerBadgeMac sizeBytes=${result.sizeBytes}"
                )
                _syncIslandEvents.tryEmit(SimBadgeSyncIslandEvent.RecFileDownloaded(filename))
            }

            is WavDownloadResult.Error -> {
                storeSupport.markBadgeDownloadAvailability(
                    filename = filename,
                    availability = AudioLocalAvailability.FAILED,
                    errorMessage = result.message,
                    badgeMac = ownerBadgeMac
                )
                notifyCommandEndAsync()
                Log.e(
                    TAG,
                    "rec# auto-download: failed filename=$filename reason=${result.message}"
                )
            }
        }
    }

    private fun notifyCommandEndAsync() {
        runtime.repositoryScope.launch {
            connectivityBridge.notifyCommandEnd()
        }
    }

    private suspend fun currentRuntimeBadgeMac(): String? {
        return runtime.endpointRecoveryCoordinator.currentRuntimeKey()?.peripheralId
            ?: runtime.deviceRegistryManager.activeDevice.value?.macAddress
    }
}
