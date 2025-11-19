package com.smartsales.feature.connectivity

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

sealed interface ConnectivityError {
    data class PairingInProgress(val deviceName: String) : ConnectivityError
    data class ProvisioningFailed(val reason: String) : ConnectivityError
    data class PermissionDenied(val permissions: Set<String>) : ConnectivityError
    data class Timeout(val timeoutMillis: Long) : ConnectivityError
    data class Transport(val reason: String) : ConnectivityError
    data object MissingSession : ConnectivityError
}

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Pairing(
        val deviceName: String,
        val progressPercent: Int,
        val signalStrengthDbm: Int
    ) : ConnectionState

    data class WifiProvisioned(
        val session: BleSession,
        val status: ProvisioningStatus
    ) : ConnectionState

    data class Syncing(
        val session: BleSession,
        val status: ProvisioningStatus,
        val lastHeartbeatAtMillis: Long
    ) : ConnectionState

    data class Error(val error: ConnectivityError) : ConnectionState
}
