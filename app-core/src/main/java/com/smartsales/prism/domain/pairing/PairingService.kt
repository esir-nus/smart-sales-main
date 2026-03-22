package com.smartsales.prism.domain.pairing

import kotlinx.coroutines.flow.StateFlow

/**
 * 配对服务接口
 * 
 * 封装 BLE 扫描 + WiFi 配网 + 网络检查的完整配对流程
 * 消费者：PairingFlowViewModel
 */
interface PairingService {
    /**
     * 配对状态流
     */
    val state: StateFlow<PairingState>
    
    /**
     * 开始扫描 BLE 设备
     * 自动超时：12秒
     */
    suspend fun startScan()
    
    /**
     * 配对并配网
     * 
     * @param badge 扫描发现的设备
     * @param wifiCreds WiFi 凭证
     * @return 成功返回 Badge ID，失败返回 Error
     */
    suspend fun pairBadge(
        badge: DiscoveredBadge,
        wifiCreds: WifiCredentials
    ): PairingResult
    
    /**
     * 取消配对流程
     */
    fun cancelPairing()
}

/**
 * 配对状态（简化模型，对比 legacy 9-state）
 */
sealed class PairingState {
    /** 空闲 */
    data object Idle : PairingState()
    
    /** 正在扫描 */
    data object Scanning : PairingState()
    
    /** 发现设备 */
    data class DeviceFound(val badge: DiscoveredBadge) : PairingState()
    
    /** 正在配对（包含 WiFi 配网 + 网络检查）*/
    data class Pairing(val progress: Int) : PairingState()
    
    /** 配对成功 */
    data class Success(val badgeId: String, val badgeName: String) : PairingState()
    
    /** 配对失败 */
    data class Error(
        val message: String, 
        val reason: ErrorReason, 
        val canRetry: Boolean
    ) : PairingState()
}

/**
 * 发现的设备
 */
data class DiscoveredBadge(
    val id: String,              // BLE MAC 地址
    val name: String,            // 设备名称
    val signalStrengthDbm: Int   // 信号强度
)

/**
 * WiFi 凭证
 */
data class WifiCredentials(
    val ssid: String,
    val password: String
)

/**
 * 配对结果
 */
sealed class PairingResult {
    data class Success(val badgeId: String) : PairingResult()
    data class Error(val message: String, val reason: ErrorReason) : PairingResult()
}

/**
 * 错误原因
 */
enum class ErrorReason {
    SCAN_TIMEOUT,              // 扫描超时
    DEVICE_NOT_FOUND,          // 设备丢失
    WIFI_PROVISIONING_FAILED,  // WiFi 配网失败
    NETWORK_CHECK_FAILED,      // 网络检查失败
    PERMISSION_DENIED,         // BLE 权限被拒
    NEED_INITIAL_PAIRING,      // 需要初次配对（无存储会话）
    UNKNOWN                    // 未知错误
}
