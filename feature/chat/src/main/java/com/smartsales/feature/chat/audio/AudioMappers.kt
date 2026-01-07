// File: feature/chat/src/main/java/com/smartsales/feature/chat/audio/AudioMappers.kt
// Module: :feature:chat
// Summary: Extension functions to map device/audio states to UI models
// Author: created on 2026-01-07 (extracted from HomeViewModel)

package com.smartsales.feature.chat.audio

import com.smartsales.feature.chat.home.AudioSummaryUi
import com.smartsales.feature.chat.home.DeviceConnectionStateUi
import com.smartsales.feature.chat.home.DeviceSnapshotUi
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.feature.media.MediaSyncState
import com.smartsales.feature.media.MediaClipStatus

/**
 * Converts ConnectionState to DeviceSnapshotUi for display.
 */
fun ConnectionState.toDeviceSnapshot(): DeviceSnapshotUi = when (this) {
    ConnectionState.NeedsSetup -> DeviceSnapshotUi(
        statusText = "设备未配网，点击开始配置",
        connectionState = DeviceConnectionStateUi.DISCONNECTED
    )

    ConnectionState.Disconnected -> DeviceSnapshotUi(
        statusText = "设备未连接，点击开始配网",
        connectionState = DeviceConnectionStateUi.DISCONNECTED
    )

    is ConnectionState.Pairing -> DeviceSnapshotUi(
        deviceName = deviceName,
        statusText = "正在连接设备，请保持靠近",
        connectionState = DeviceConnectionStateUi.CONNECTING
    )

    is ConnectionState.AutoReconnecting -> DeviceSnapshotUi(
        statusText = "正在自动重连设备…",
        connectionState = DeviceConnectionStateUi.CONNECTING
    )

    is ConnectionState.Connected -> DeviceSnapshotUi(
        deviceName = session.peripheralName,
        statusText = "设备已连接，等待联网",
        connectionState = DeviceConnectionStateUi.WAITING_FOR_NETWORK
    )

    is ConnectionState.WifiProvisioned -> DeviceSnapshotUi(
        deviceName = session.peripheralName,
        statusText = "设备已上线，网络：${status.wifiSsid}",
        connectionState = DeviceConnectionStateUi.CONNECTED,
        wifiName = status.wifiSsid
    )

    is ConnectionState.Syncing -> DeviceSnapshotUi(
        deviceName = session.peripheralName,
        statusText = "设备在线，网络：${status.wifiSsid}",
        connectionState = DeviceConnectionStateUi.CONNECTED,
        wifiName = status.wifiSsid
    )

    is ConnectionState.Error -> DeviceSnapshotUi(
        statusText = "连接异常",
        connectionState = DeviceConnectionStateUi.ERROR,
        errorSummary = error.toReadableMessage()
    )
}

/**
 * Converts ConnectivityError to readable Chinese message.
 */
fun ConnectivityError.toReadableMessage(): String = when (this) {
    is ConnectivityError.PairingInProgress -> "配对冲突：${deviceName} 已在使用"
    is ConnectivityError.ProvisioningFailed -> reason
    is ConnectivityError.PermissionDenied -> "缺少权限：${permissions.joinToString()}"
    is ConnectivityError.Timeout -> "连接超时，请稍后重试"
    is ConnectivityError.Transport -> reason
    is ConnectivityError.EndpointUnreachable -> reason.ifBlank { "设备服务不可达" }
    is ConnectivityError.DeviceNotFound -> "未找到设备 ${deviceId}"
    ConnectivityError.MissingSession -> "当前没有有效的配对会话"
}

/**
 * Converts MediaSyncState to AudioSummaryUi for display.
 * Returns null if there's nothing worth showing.
 */
fun MediaSyncState.toAudioSummaryUi(): AudioSummaryUi? {
    val readyCount = items.count { it.status == MediaClipStatus.Ready }
    val uploadingCount = items.count { it.status == MediaClipStatus.Uploading }
    val failedCount = items.count { it.status == MediaClipStatus.Failed }
    val pendingTranscriptions = items.count {
        it.status == MediaClipStatus.Ready && it.transcriptSource == null
    }
    if (!syncing && readyCount == 0 && uploadingCount == 0 && failedCount == 0) {
        return null
    }
    val headline = when {
        uploadingCount > 0 -> "${uploadingCount} 条录音待上传"
        syncing -> "正在同步录音..."
        failedCount > 0 -> "${failedCount} 条录音同步失败"
        readyCount > 0 -> "${readyCount} 条录音已就绪"
        else -> "无录音文件"
    }
    val detail = when {
        pendingTranscriptions > 0 -> "${pendingTranscriptions} 条录音可转写"
        readyCount > 0 -> "点击查看详情"
        else -> null
    }
    return AudioSummaryUi(
        headline = headline,
        detail = detail,
        syncedCount = readyCount,
        pendingUploadCount = uploadingCount,
        pendingTranscriptionCount = pendingTranscriptions,
        lastSyncedAtMillis = lastSyncedAtMillis
    )
}
