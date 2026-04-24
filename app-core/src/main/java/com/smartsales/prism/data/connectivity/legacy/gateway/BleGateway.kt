package com.smartsales.prism.data.connectivity.legacy.gateway

import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.DeviceNetworkStatus
import com.smartsales.prism.data.connectivity.legacy.WifiCredentials
import kotlinx.coroutines.flow.Flow
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
    
    suspend fun sendWavCommand(session: BleSession, command: WavCommand): WavCommandResult

    /**
     * Fire-and-forget 信号写入，用于向徽章发送不期待响应的短命令
     * （例如任务闹钟触发时的提示音信号 "commandend#1"）。
     * 遇到任何错误都会抛出给调用方，由调用方决定是否吞掉。
     */
    suspend fun sendBadgeSignal(session: BleSession, payload: String)

    fun forget(peripheral: BlePeripheral)
}



enum class WavCommand(val blePayload: String) {
    GET("wav#get"),
    END("wav#end")
}

sealed interface WavCommandResult {
    data object Ready : WavCommandResult           // wav#send
    data object Done : WavCommandResult            // wav#ok
    data class PermissionDenied(val permissions: Set<String>) : WavCommandResult
    data class Timeout(val timeoutMillis: Long) : WavCommandResult
    data class TransportError(val reason: String) : WavCommandResult
    data class DeviceMissing(val peripheralId: String) : WavCommandResult
}

/** 徽章通过 BLE 发送的通知事件 */
sealed class BadgeNotification {
    /** 徽章请求时间同步 (tim#get) */
    data object TimeSyncRequested : BadgeNotification()

    /** 徽章报告录音就绪 (log#YYYYMMDD_HHMMSS) */
    data class RecordingReady(val filename: String) : BadgeNotification()

    /** 徽章报告音频文件就绪，仅下载不转写 (rec#YYYYMMDD_HHMMSS) */
    data class AudioRecordingReady(val token: String) : BadgeNotification()

    /** 徽章主动推送电量 (Bat#0..100) */
    data class BatteryLevel(val percent: Int) : BadgeNotification()

    /** 徽章返回固件版本 (Ver#<project>.<major>.<minor>.<feature>) */
    data class FirmwareVersion(val version: String) : BadgeNotification()

    /** 未识别的命令 */
    data class Unknown(val raw: String) : BadgeNotification()
}

/** Provisioning 握手应答解析结果 */
sealed interface ProvisioningAckResult {
    data object Accepted : ProvisioningAckResult
    data class Rejected(val reason: String) : ProvisioningAckResult
}
