package com.smartsales.prism.data.connectivity

import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.connectivity.WifiProvisioner
import com.smartsales.prism.domain.connectivity.ConnectivityService
import com.smartsales.prism.domain.connectivity.ReconnectResult
import com.smartsales.prism.domain.connectivity.UpdateResult
import com.smartsales.prism.domain.connectivity.WifiConfigResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real ConnectivityService — 将 ConnectivityViewModel → ConnectivityBridge 桥接
 * 
 * Wave 2.5: 连接 UI 层到已实现的 RealConnectivityBridge
 * 
 * 数据流：
 * User Action → ConnectivityModal → ConnectivityViewModel 
 *     → ConnectivityService (THIS) → DeviceConnectionManager/WifiProvisioner (Legacy)
 * 
 * @see docs/cerb/connectivity-bridge/spec.md Wave 2.5
 */
@Singleton
class RealConnectivityService @Inject constructor(
    private val deviceManager: DeviceConnectionManager,
    private val wifiProvisioner: WifiProvisioner
) : ConnectivityService {
    
    override suspend fun checkForUpdate(): UpdateResult {
        // Wave 3 工作：固件更新检测
        // 目前返回 None，待后续实现 OTA 更新逻辑
        return UpdateResult.None
    }
    
    override suspend fun reconnect(): ReconnectResult {
        // 使用 DeviceConnectionManager 的强制重连（跳过退避）
        deviceManager.forceReconnectNow()
        
        // 给予时间让状态转换完成
        kotlinx.coroutines.delay(1500)
        
        return when (val state = deviceManager.state.value) {
            is com.smartsales.feature.connectivity.ConnectionState.Connected,
            is com.smartsales.feature.connectivity.ConnectionState.WifiProvisioned,
            is com.smartsales.feature.connectivity.ConnectionState.Syncing -> 
                ReconnectResult.Connected
            
            is com.smartsales.feature.connectivity.ConnectionState.Error -> {
                val error = state.error
                if (error is com.smartsales.feature.connectivity.ConnectivityError.DeviceNotFound) {
                    ReconnectResult.DeviceNotFound
                } else {
                    ReconnectResult.Error(error.toString())
                }
            }
            
            else -> ReconnectResult.DeviceNotFound
        }
    }
    
    override suspend fun disconnect() {
        deviceManager.forgetDevice()
    }
    
    override suspend fun updateWifiConfig(ssid: String, password: String): WifiConfigResult {
        val session = when (val connectState = deviceManager.state.value) {
            is com.smartsales.feature.connectivity.ConnectionState.Connected -> connectState.session
            is com.smartsales.feature.connectivity.ConnectionState.WifiProvisioned -> connectState.session
            is com.smartsales.feature.connectivity.ConnectionState.Syncing -> connectState.session
            else -> return WifiConfigResult.Error("设备未连接")
        }
        
        val credentials = WifiCredentials(ssid, password)
        
        return when (val result = wifiProvisioner.provision(session, credentials)) {
            is Result.Success -> WifiConfigResult.Success
            is Result.Error -> WifiConfigResult.Error(result.throwable.message ?: "WiFi 配置失败")
        }
    }
}
