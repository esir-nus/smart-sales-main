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
                        is BadgeNotification.TimeSyncRequested -> {
                            ConnectivityLogger.d("⏰ Badge time sync requested")
                        }
                        is BadgeNotification.Unknown -> {
                            ConnectivityLogger.d("❓ Badge unknown notification: ${event.raw}")
                        }
                    }
                }
            } finally {
                runtime.notificationListenerActive = false
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
    return when {
        trimmed.isBlank() -> ""
        trimmed.endsWith(".wav", ignoreCase = true) -> trimmed
        trimmed.startsWith("log_", ignoreCase = true) -> "$trimmed.wav"
        trimmed.startsWith("log#", ignoreCase = true) -> {
            val token = trimmed.removePrefix("log#").trim()
            "log_$token.wav"
        }
        else -> "log_$trimmed.wav"
    }
}
