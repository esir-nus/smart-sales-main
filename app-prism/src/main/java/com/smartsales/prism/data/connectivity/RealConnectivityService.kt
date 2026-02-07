package com.smartsales.prism.data.connectivity

import android.util.Log
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

private const val TAG = "ConnectivityService"

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
        Log.d(TAG, "reconnect() called, initial state: ${deviceManager.state.value}")
        
        val outcome = deviceManager.reconnectAndWait()
        
        val result = when (outcome) {
            is com.smartsales.feature.connectivity.ConnectionState.Connected,
            is com.smartsales.feature.connectivity.ConnectionState.WifiProvisioned,
            is com.smartsales.feature.connectivity.ConnectionState.Syncing -> 
                ReconnectResult.Connected
            is com.smartsales.feature.connectivity.ConnectionState.NeedsSetup ->
                ReconnectResult.DeviceNotFound
            is com.smartsales.feature.connectivity.ConnectionState.Error -> {
                val error = outcome.error
                if (error is com.smartsales.feature.connectivity.ConnectivityError.DeviceNotFound) {
                    ReconnectResult.DeviceNotFound
                } else {
                    ReconnectResult.Error(error.toString())
                }
            }
            else -> ReconnectResult.DeviceNotFound
        }
        
        Log.d(TAG, "reconnect() result: $result, current state: ${deviceManager.state.value}")
        return result
    }
    
    override suspend fun disconnect() {
        Log.d(TAG, "disconnect() called (soft disconnect - session preserved)")
        deviceManager.disconnectBle()
        Log.d(TAG, "disconnect() completed")
    }
    
    override suspend fun unpair() {
        Log.d(TAG, "unpair() called (hard disconnect - session cleared)")
        deviceManager.forgetDevice()
        Log.d(TAG, "unpair() completed")
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
