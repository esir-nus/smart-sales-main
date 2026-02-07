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
    object WifiMismatch : ReconnectResult()
    object DeviceNotFound : ReconnectResult()
    data class Error(val message: String) : ReconnectResult()
}

/**
 * WiFi 配置更新结果
 */
sealed class WifiConfigResult {
    object Success : WifiConfigResult()
    data class Error(val message: String) : WifiConfigResult()
}
