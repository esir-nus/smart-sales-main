package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.prism.data.connectivity.legacy.gateway.BadgeNotification
import com.smartsales.prism.data.connectivity.legacy.gateway.GattSessionLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class DeviceConnectionManagerIngressSupport(
    private val bleGateway: GattSessionLifecycle,
    private val dispatchers: DispatcherProvider,
    private val scope: CoroutineScope,
    private val runtime: DeviceConnectionManagerRuntime
) {

    fun startNotificationListener(session: BleSession) {
        runtime.notificationListenerJob?.cancel()
        val generation = ++runtime.notificationListenerGeneration
        runtime.notificationListenerActive = true
        runtime.notificationListenerJob = scope.launch(dispatchers.io) {
            try {
                ConnectivityLogger.d("📡 Starting persistent notification listener for ${session.peripheralId}")
                bleGateway.listenForBadgeNotifications().collect { event ->
                    when (event) {
                        is BadgeNotification.RecordingReady -> {
                            val filename = event.filename.toBadgeDownloadFilename()
                            ConnectivityLogger.i("📥 Badge recording ready: $filename")
                            runtime.recordingReadyEvents.tryEmit(filename)
                        }
                        is BadgeNotification.AudioRecordingReady -> {
                            val filename = event.token.toBadgeAudioFilename()
                            ConnectivityLogger.i("📥 Badge audio recording ready: $filename")
                            runtime.audioRecordingReadyEvents.tryEmit(filename)
                        }
                        is BadgeNotification.BatteryLevel -> {
                            ConnectivityLogger.d("🔋 Badge battery update: ${event.percent}")
                            runtime.batteryEvents.tryEmit(event.percent)
                        }
                        is BadgeNotification.TimeSyncRequested -> {
                            ConnectivityLogger.d("⏰ Badge time sync requested")
                        }
                        is BadgeNotification.Unknown -> {
                            ConnectivityLogger.d("❓ Badge unknown notification: ${event.raw}")
                        }
                    }
                }
            } finally {
                // 仅当本代的监听器仍为当前代时才清除标记
                if (runtime.notificationListenerGeneration == generation) {
                    runtime.notificationListenerActive = false
                }
            }
        }
    }

    fun syntheticProvisioningStatus(status: DeviceNetworkStatus): ProvisioningStatus {
        val wifiName = status.deviceWifiName.ifBlank { "BT311" }
        return ProvisioningStatus(
            wifiSsid = wifiName,
            handshakeId = "network-${status.rawResponse.hashCode()}",
            credentialsHash = "${wifiName}-${status.ipAddress}".hashCode().toString()
        )
    }
}

internal fun String.toBadgeDownloadFilename(): String {
    val trimmed = trim()
    val normalized = trimmed.removeSuffix(".wav").removeSuffix(".WAV").trim()
    return when {
        trimmed.isBlank() -> ""
        trimmed.startsWith("log_", ignoreCase = true) -> {
            val token = normalized.removePrefix("log_").trim()
            "log_$token.wav"
        }
        trimmed.startsWith("log#", ignoreCase = true) -> {
            val token = normalized.removePrefix("log#").trim()
            "log_$token.wav"
        }
        else -> "log_$normalized.wav"
    }
}

/** rec# / rec_ 令牌统一归一化为 rec_YYYYMMDD_HHMMSS.wav */
internal fun String.toBadgeAudioFilename(): String {
    val trimmed = trim()
    val normalized = trimmed.removeSuffix(".wav").removeSuffix(".WAV").trim()
    return when {
        trimmed.isBlank() -> ""
        trimmed.startsWith("rec_", ignoreCase = true) -> {
            val token = normalized.removePrefix("rec_").trim()
            "rec_$token.wav"
        }
        else -> "rec_$normalized.wav"
    }
}
