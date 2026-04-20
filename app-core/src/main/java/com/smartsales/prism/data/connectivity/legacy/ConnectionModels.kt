package com.smartsales.prism.data.connectivity.legacy

import java.util.UUID

// 文件路径: feature/connectivity/src/main/java/com/smartsales/feature/connectivity/ConnectionModels.kt
// 文件作用: 定义连接模块的公共数据结构
// 最近修改: 2025-11-14
data class BlePeripheral(
    val id: String,
    val name: String,
    val signalStrengthDbm: Int,
    val profileId: String? = null
)

data class BleSession(
    val peripheralId: String,
    val peripheralName: String,
    val signalStrengthDbm: Int,
    val profileId: String?,
    val secureToken: String,
    val establishedAtMillis: Long
) {
    companion object {
        fun fromPeripheral(peripheral: BlePeripheral, timestamp: Long = System.currentTimeMillis()): BleSession =
            BleSession(
                peripheralId = peripheral.id,
                peripheralName = peripheral.name,
                signalStrengthDbm = peripheral.signalStrengthDbm,
                profileId = peripheral.profileId,
                secureToken = UUID.randomUUID().toString(),
                establishedAtMillis = timestamp
            )
    }
}

data class WifiCredentials(
    val ssid: String,
    val password: String,
    val security: Security = Security.WPA2
) {
    enum class Security { WPA2, WPA3 }
}

data class KnownWifiCredential(
    val credentials: WifiCredentials,
    val normalizedSsid: String,
    val lastUsedAtMillis: Long
)

data class ProvisioningStatus(
    val wifiSsid: String,
    val handshakeId: String,
    val credentialsHash: String
)

data class DeviceNetworkStatus(
    val ipAddress: String,
    val deviceWifiName: String,
    val phoneWifiName: String,
    val rawResponse: String
)

enum class WifiDisconnectedReason {
    BADGE_WIFI_OFFLINE,
    HTTP_UNREACHABLE,
    PHONE_WIFI_UNAVAILABLE,
    PHONE_WIFI_SSID_UNREADABLE,
    NO_KNOWN_CREDENTIAL_FOR_PHONE_WIFI,
    BADGE_PHONE_NETWORK_MISMATCH,
    CREDENTIAL_REPLAY_FAILED
}

sealed interface ConnectivityError {
    data class PairingInProgress(val deviceName: String) : ConnectivityError
    data class ProvisioningFailed(val reason: String) : ConnectivityError
    data class PermissionDenied(val permissions: Set<String>) : ConnectivityError
    data class Timeout(val timeoutMillis: Long) : ConnectivityError
    data class Transport(val reason: String) : ConnectivityError
    data class DeviceNotFound(val deviceId: String) : ConnectivityError
    data class EndpointUnreachable(val reason: String) : ConnectivityError
    data class WifiDisconnected(
        val reason: WifiDisconnectedReason,
        val phoneSsid: String? = null,
        val badgeSsid: String? = null
    ) : ConnectivityError
    data object MissingSession : ConnectivityError
}

sealed interface ConnectionState {
    /** 尚未保存任何设备信息，需要完整配网。 */
    data object NeedsSetup : ConnectionState

    data object Disconnected : ConnectionState
    data class Connected(val session: BleSession) : ConnectionState
    data class AutoReconnecting(val attempt: Int) : ConnectionState
    data class Pairing(
        val deviceName: String,
        val progressPercent: Int,
        val signalStrengthDbm: Int
    ) : ConnectionState

    data class WifiProvisioned(
        val session: BleSession,
        val status: ProvisioningStatus
    ) : ConnectionState

    /**
     * 传输已确认（IP 可用 + SSID 匹配），但 HTTP :8088 服务尚未响应。
     * 区别于 BADGE_WIFI_OFFLINE — 网络切换本身成功，HTTP 仍在预热。
     */
    data class WifiProvisionedHttpDelayed(
        val session: BleSession,
        val status: ProvisioningStatus,
        val baseUrl: String,
    ) : ConnectionState

    data class Syncing(
        val session: BleSession,
        val status: ProvisioningStatus,
        val lastHeartbeatAtMillis: Long
    ) : ConnectionState
    data class Error(val error: ConnectivityError) : ConnectionState
}

sealed interface ReconnectErrorReason {
    data object DeviceNotFound : ReconnectErrorReason
    data object Network : ReconnectErrorReason
    data object Backoff : ReconnectErrorReason
}

internal fun normalizeWifiSsid(rawSsid: String?): String? {
    val normalized = rawSsid
        ?.trim()
        ?.removePrefix("\"")
        ?.removeSuffix("\"")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    return when (normalized.lowercase()) {
        "<unknown ssid>", "unknown ssid", "n/a" -> null
        else -> normalized
    }
}

internal fun hasUsableBadgeIp(rawIp: String?): Boolean {
    val normalized = rawIp?.trim().orEmpty()
    return normalized.isNotBlank() && normalized != "0.0.0.0" && !normalized.startsWith("0.")
}
