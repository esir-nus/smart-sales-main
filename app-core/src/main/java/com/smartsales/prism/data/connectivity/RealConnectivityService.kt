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
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.WifiProvisioned,
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Syncing ->
                ReconnectResult.Connected
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Connected ->
                ReconnectResult.Error("设备连接未完成")
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.NeedsSetup ->
                ReconnectResult.DeviceNotFound
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Error -> {
                val error = outcome.error
                when (error) {
                    is com.smartsales.prism.data.connectivity.legacy.ConnectivityError.DeviceNotFound ->
                        ReconnectResult.DeviceNotFound
                    is com.smartsales.prism.data.connectivity.legacy.ConnectivityError.WifiDisconnected ->
                        mapReconnectWifiError(error)
                    else -> ReconnectResult.Error(error.toString())
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
            else -> sessionStore.loadSession()
        } ?: return WifiConfigResult.Error("设备未连接")
        
        val credentials = WifiCredentials(ssid, password)
        
        return when (val result = wifiProvisioner.provision(session, credentials)) {
            is Result.Success -> {
                sessionStore.saveSession(session)
                sessionStore.upsertKnownNetwork(credentials)
                when (val confirmation = deviceManager.confirmManualWifiProvision(credentials)) {
                    is com.smartsales.prism.data.connectivity.legacy.ConnectionState.WifiProvisioned,
                    is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Syncing ->
                        WifiConfigResult.Success

                    is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Error -> {
                        val error = confirmation.error
                        if (
                            error is com.smartsales.prism.data.connectivity.legacy.ConnectivityError.WifiDisconnected &&
                            error.reason == com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.BADGE_PHONE_NETWORK_MISMATCH
                        ) {
                            WifiConfigResult.Error("设备与输入的 Wi‑Fi 不匹配，请重新检查配置")
                        } else {
                            Log.w(TAG, "manual wifi repair did not confirm online: $error")
                            WifiConfigResult.Success
                        }
                    }

                    else -> {
                        Log.w(TAG, "manual wifi repair fell back to live manager state: $confirmation")
                        WifiConfigResult.Success
                    }
                }
            }
            is Result.Error -> WifiConfigResult.Error(result.throwable.message ?: "WiFi 配置失败")
        }
    }

    private fun mapReconnectWifiError(
        error: com.smartsales.prism.data.connectivity.legacy.ConnectivityError.WifiDisconnected
    ): ReconnectResult {
        return when (error.reason) {
            com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.NO_KNOWN_CREDENTIAL_FOR_PHONE_WIFI,
            com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.PHONE_WIFI_SSID_UNREADABLE,
            com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.BADGE_PHONE_NETWORK_MISMATCH ->
                ReconnectResult.WifiMismatch
            com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.PHONE_WIFI_UNAVAILABLE ->
                ReconnectResult.Error("手机当前未连接可用 Wi‑Fi")
            com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.BADGE_WIFI_OFFLINE ->
                ReconnectResult.Error("设备当前未接入可用 Wi‑Fi")
            com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.CREDENTIAL_REPLAY_FAILED ->
                ReconnectResult.Error("已尝试恢复已保存 Wi‑Fi，但设备仍未接入网络")
        }
    }
}
