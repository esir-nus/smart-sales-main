package com.smartsales.feature.connectivity.gateway

import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import java.util.UUID

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/gateway/BleGateway.kt
// 模块：:feature:connectivity
// 说明：定义 BLE 网关接口，用于发送 Wi-Fi 凭据与读取热点信息
// 作者：创建于 2025-11-16
data class BleGatewayConfig(
    val serviceUuid: UUID,
    val credentialCharacteristicUuid: UUID,
    val provisioningStatusCharacteristicUuid: UUID,
    val hotspotCharacteristicUuid: UUID,
    val connectionTimeoutMillis: Long = 10_000L,
    val operationTimeoutMillis: Long = 5_000L
)

sealed interface BleGatewayResult {
    data class Success(val handshakeId: String, val credentialsHash: String) : BleGatewayResult
    data class PermissionDenied(val permissions: Set<String>) : BleGatewayResult
    data object Timeout : BleGatewayResult
    data class TransportError(val reason: String) : BleGatewayResult
    data class CredentialRejected(val reason: String) : BleGatewayResult
    data class DeviceMissing(val peripheralId: String) : BleGatewayResult
}

sealed interface HotspotResult {
    data class Success(val credentials: WifiCredentials) : HotspotResult
    data class PermissionDenied(val permissions: Set<String>) : HotspotResult
    data class Timeout(val timeoutMillis: Long) : HotspotResult
    data class TransportError(val reason: String) : HotspotResult
    data class DeviceMissing(val peripheralId: String) : HotspotResult
}

sealed interface NetworkQueryResult {
    data class Success(val status: DeviceNetworkStatus) : NetworkQueryResult
    data class PermissionDenied(val permissions: Set<String>) : NetworkQueryResult
    data class Timeout(val timeoutMillis: Long) : NetworkQueryResult
    data class TransportError(val reason: String) : NetworkQueryResult
    data class DeviceMissing(val peripheralId: String) : NetworkQueryResult
}

interface BleGateway {
    suspend fun provision(
        session: BleSession,
        credentials: WifiCredentials
    ): BleGatewayResult

    suspend fun requestHotspot(session: BleSession): HotspotResult

    suspend fun queryNetwork(session: BleSession): NetworkQueryResult
    fun forget(peripheral: BlePeripheral)
}
