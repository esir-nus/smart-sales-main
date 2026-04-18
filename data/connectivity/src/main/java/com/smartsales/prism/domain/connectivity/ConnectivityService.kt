package com.smartsales.prism.domain.connectivity

/**
 * 连接服务接口 - 管理设备连接、固件检查、WiFi配置
 * Connectivity Service for managing device connection, firmware updates, and WiFi configuration.
 */
interface ConnectivityService {
    
    /**
     * 检查固件更新
     */
    suspend fun checkForUpdate(): UpdateResult
    
    /**
     * 重新连接设备
     */
    suspend fun reconnect(): ReconnectResult
    
    /**
     * 断开设备连接（软断开 - 保留 session，可重连）
     */
    suspend fun disconnect()
    
    /**
     * 取消配对（硬断开 - 清除 session，需要重新配网）
     */
    suspend fun unpair()
    
    /**
     * 更新 WiFi 配置
     */
    suspend fun updateWifiConfig(ssid: String, password: String): WifiConfigResult

    /**
     * 调度自动重连 — 非阻塞，尊重退避策略
     */
    fun scheduleAutoReconnect()
}

/**
 * 固件更新检查结果
 */
sealed class UpdateResult {
    data class Found(val version: String, val changelog: String = "") : UpdateResult()
    object None : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

/**
 * 重新连接结果
 */
sealed class ReconnectResult {
    object Connected : ReconnectResult()
    data class WifiMismatch(
        val currentPhoneSsid: String? = null,
        val errorMessage: String? = null,
    ) : ReconnectResult()
    object DeviceNotFound : ReconnectResult()
    data class Error(val message: String) : ReconnectResult()
}

/**
 * WiFi 配置更新结果
 */
sealed class WifiConfigResult {
    object Success : WifiConfigResult()
    /**
     * 传输已确认（IP + SSID 切换），但 HTTP :8088 服务尚未响应。
     * 这不是失败 — 网络切换成功，HTTP 服务仍在预热。
     */
    data class TransportConfirmedHttpDelayed(
        val badgeSsid: String?,
        val baseUrl: String,
    ) : WifiConfigResult()
    data class Error(val message: String) : WifiConfigResult()
}
