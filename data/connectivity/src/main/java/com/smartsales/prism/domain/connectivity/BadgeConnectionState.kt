package com.smartsales.prism.domain.connectivity

/**
 * Badge 连接状态
 * 
 * 简化版 — 从 legacy ConnectionState 映射而来
 * 
 * @see connectivity-bridge/spec.md
 */
sealed class BadgeConnectionState {
    /** 未连接 */
    object Disconnected : BadgeConnectionState()
    
    /** 需要配网 — currentSession 为空，需完成初始化配对 */
    object NeedsSetup : BadgeConnectionState()
    
    /** 连接中（配对或自动重连） */
    object Connecting : BadgeConnectionState()
    
    /** 已连接，可通过 WiFi 访问 */
    data class Connected(
        val badgeIp: String,
        val ssid: String
    ) : BadgeConnectionState()
    
    /** 连接错误 */
    data class Error(val message: String) : BadgeConnectionState()
}
