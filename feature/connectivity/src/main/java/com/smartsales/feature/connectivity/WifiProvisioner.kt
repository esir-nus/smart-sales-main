package com.smartsales.feature.connectivity

import com.smartsales.core.util.Result

// 文件路径: feature/connectivity/src/main/java/com/smartsales/feature/connectivity/WifiProvisioner.kt
// 文件作用: 描述Wi-Fi配网流程接口及默认模拟实现
// 最近修改: 2025-11-14
interface WifiProvisioner {
    suspend fun provision(session: BleSession, credentials: WifiCredentials): Result<ProvisioningStatus>
    suspend fun requestHotspotCredentials(session: BleSession): Result<WifiCredentials>
    suspend fun queryNetworkStatus(session: BleSession): Result<DeviceNetworkStatus>
}
