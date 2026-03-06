package com.smartsales.prism.data.connectivity

import android.util.Log
import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.SessionStore
import com.smartsales.prism.data.connectivity.legacy.WifiCredentials
import com.smartsales.prism.data.connectivity.legacy.WifiProvisioner
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
    private val wifiProvisioner: WifiProvisioner,
    private val sessionStore: SessionStore
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
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Connected,
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.WifiProvisioned,
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Syncing -> {
                // BLE 已连接 — 检查 WiFi 是否与配网时一致
                if (isWifiMismatch()) ReconnectResult.WifiMismatch
                else ReconnectResult.Connected
            }
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.NeedsSetup ->
                ReconnectResult.DeviceNotFound
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Error -> {
                val error = outcome.error
                if (error is com.smartsales.prism.data.connectivity.legacy.ConnectivityError.DeviceNotFound) {
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
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Connected -> connectState.session
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.WifiProvisioned -> connectState.session
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Syncing -> connectState.session
            else -> return WifiConfigResult.Error("设备未连接")
        }
        
        val credentials = WifiCredentials(ssid, password)
        
        return when (val result = wifiProvisioner.provision(session, credentials)) {
            is Result.Success -> WifiConfigResult.Success
            is Result.Error -> WifiConfigResult.Error(result.throwable.message ?: "WiFi 配置失败")
        }
    }
    
    /**
     * 比较 Badge 当前 WiFi SSID 与配网时存储的 SSID。
     * 不同则说明 Badge 连了别的网络，需要重新配网。
     */
    private suspend fun isWifiMismatch(): Boolean {
        val storedSsid = sessionStore.load()?.second?.ssid ?: return false
        val networkResult = deviceManager.queryNetworkStatus()
        val badgeSsid = (networkResult as? Result.Success)?.data?.deviceWifiName ?: return false
        val mismatch = badgeSsid.isNotBlank() && !badgeSsid.equals(storedSsid, ignoreCase = true)
        if (mismatch) {
            Log.d(TAG, "⚠️ WiFi mismatch: badge=$badgeSsid, stored=$storedSsid")
        }
        return mismatch
    }
}
