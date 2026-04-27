package com.smartsales.prism.data.connectivity

import android.util.Log
import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.SessionStore
import com.smartsales.prism.data.connectivity.legacy.ConnectivityLogger
import com.smartsales.prism.data.connectivity.legacy.WifiCredentials
import com.smartsales.prism.data.connectivity.legacy.WifiProvisioner
import com.smartsales.prism.data.connectivity.registry.DeviceRegistryManager
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
    private val sessionStore: SessionStore,
    private val registryManager: DeviceRegistryManager
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
        val activeMac = registryManager.activeDevice.value?.macAddress
        if (activeMac != null) {
            registryManager.markManuallyDisconnected(activeMac, true)
        }
        deviceManager.disconnectBle()
        Log.d(TAG, "disconnect() completed")
    }

    override suspend fun connect(macAddress: String) {
        Log.d(TAG, "connect() called for $macAddress")
        registryManager.markManuallyDisconnected(macAddress, false)
        registryManager.switchToDevice(macAddress)
        Log.d(TAG, "connect() completed for $macAddress")
    }
    
    override suspend fun unpair() {
        Log.d(TAG, "unpair() called (hard disconnect - session cleared)")
        deviceManager.forgetDevice()
        Log.d(TAG, "unpair() completed")
    }
    
    override suspend fun updateWifiConfig(ssid: String, password: String): WifiConfigResult {
        val normalizedSsid = ssid.trim()
        val normalizedPassword = password.trim()
        if (normalizedSsid.isEmpty() || normalizedPassword.isEmpty()) {
            return WifiConfigResult.Error("Wi-Fi 名称和密码不能为空")
        }
        val session = when (val connectState = deviceManager.state.value) {
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Connected -> connectState.session
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.WifiProvisioned -> connectState.session
            is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Syncing -> connectState.session
            else -> sessionStore.loadSession()
        } ?: return WifiConfigResult.Error("设备未连接")
        
        val credentials = WifiCredentials(normalizedSsid, normalizedPassword)
        ConnectivityLogger.i("🛜 repair submit ssid=$normalizedSsid")
        
        return when (val result = wifiProvisioner.provision(session, credentials)) {
            is Result.Success -> {
                ConnectivityLogger.i("🛜 repair write dispatched ssid=$normalizedSsid")
                sessionStore.saveSession(session)
                sessionStore.upsertKnownNetwork(credentials)
                when (val confirmation = deviceManager.confirmManualWifiProvision(credentials)) {
                    is com.smartsales.prism.data.connectivity.legacy.ConnectionState.WifiProvisioned,
                    is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Syncing ->
                        WifiConfigResult.Success

                    // 传输已确认，HTTP 服务仍在预热 — 非失败
                    is com.smartsales.prism.data.connectivity.legacy.ConnectionState.WifiProvisionedHttpDelayed -> {
                        ConnectivityLogger.i(
                            "🛜 repair transport confirmed, HTTP delayed baseUrl=${confirmation.baseUrl}"
                        )
                        WifiConfigResult.TransportConfirmedHttpDelayed(
                            badgeSsid = confirmation.status.wifiSsid,
                            baseUrl = confirmation.baseUrl
                        )
                    }

                    is com.smartsales.prism.data.connectivity.legacy.ConnectionState.Error -> {
                        val error = confirmation.error
                        if (error is com.smartsales.prism.data.connectivity.legacy.ConnectivityError.WifiDisconnected) {
                            Log.w(TAG, "manual wifi repair did not confirm online: $error")
                            WifiConfigResult.Error(mapWifiDisconnectedRepairMessage(error))
                        } else {
                            Log.w(TAG, "manual wifi repair did not confirm online: $error")
                            WifiConfigResult.Error("Wi‑Fi 修复未确认在线，请稍后重试")
                        }
                    }

                    else -> {
                        Log.w(TAG, "manual wifi repair fell back to live manager state: $confirmation")
                        WifiConfigResult.Error("Wi‑Fi 修复未达到预期状态: $confirmation")
                    }
                }
            }
            is Result.Error -> WifiConfigResult.Error(result.throwable.message ?: "WiFi 配置失败")
        }
    }

    override fun scheduleAutoReconnect() {
        Log.d(TAG, "scheduleAutoReconnect() delegating to deviceManager")
        deviceManager.scheduleAutoReconnectIfNeeded()
    }

    private fun mapReconnectWifiError(
        error: com.smartsales.prism.data.connectivity.legacy.ConnectivityError.WifiDisconnected
    ): ReconnectResult {
        // 蓝牙已配对但徽章网络链路失败时统一引导用户回到凭据表单重新输入
        return when (error.reason) {
            com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.BADGE_PHONE_NETWORK_MISMATCH ->
                ReconnectResult.WifiMismatch(currentPhoneSsid = error.phoneSsid)
            com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.BADGE_WIFI_OFFLINE ->
                ReconnectResult.WifiMismatch(
                    currentPhoneSsid = error.phoneSsid,
                    errorMessage = wifiDisconnectedChineseMessage(error.reason)
                )
            com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.CREDENTIAL_REPLAY_FAILED ->
                ReconnectResult.WifiMismatch(
                    currentPhoneSsid = error.phoneSsid,
                    errorMessage = wifiDisconnectedChineseMessage(error.reason)
                )
        }
    }

    private fun mapWifiDisconnectedRepairMessage(
        error: com.smartsales.prism.data.connectivity.legacy.ConnectivityError.WifiDisconnected
    ): String {
        return when (error.reason) {
            com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.BADGE_PHONE_NETWORK_MISMATCH ->
                "设备与输入的 Wi‑Fi 不匹配，请重新检查配置"
            else -> wifiDisconnectedChineseMessage(error.reason)
        }
    }

    private fun wifiDisconnectedChineseMessage(
        reason: com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason
    ): String {
        return when (reason) {
            com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.BADGE_WIFI_OFFLINE ->
                "设备当前未接入可用 Wi‑Fi，请重新输入凭据"
            com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.BADGE_PHONE_NETWORK_MISMATCH ->
                "设备与输入的 Wi‑Fi 不匹配，请重新检查配置"
            com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason.CREDENTIAL_REPLAY_FAILED ->
                "已尝试恢复已保存 Wi‑Fi，但设备仍未接入网络，请重新输入凭据"
        }
    }
}
