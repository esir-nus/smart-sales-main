package com.smartsales.prism.domain.connectivity

/**
 * Connectivity manager专用状态。
 *
 * 保留 shared `BadgeConnectionState` 的严格“可用传输”语义，
 * 但允许连接管理界面展示更细的 BLE / Wi‑Fi 观察结果。
 */
sealed class BadgeManagerStatus {
    /** 未知或尚未拿到可用于管理界面的细分信息 */
    object Unknown : BadgeManagerStatus()

    /** 无已保存 session，需要走初始化配网 */
    object NeedsSetup : BadgeManagerStatus()

    /** 当前无可用 BLE/session 迹象 */
    object Disconnected : BadgeManagerStatus()

    /** 正在建立或恢复连接 */
    object Connecting : BadgeManagerStatus()

    /** BLE 仍被 app 持有，但网络状态尚未确认 */
    object BlePairedNetworkUnknown : BadgeManagerStatus()

    /** BLE 已连接，但 Badge 当前未接入可用网络 */
    object BlePairedNetworkOffline : BadgeManagerStatus()

    /** 管理意义上的完整可用态 */
    data class Ready(
        val badgeIp: String? = null,
        val ssid: String? = null
    ) : BadgeManagerStatus()

    /** 显式连接错误；供 manager 端保留优先级，不被 paired/offline 覆盖 */
    data class Error(val message: String) : BadgeManagerStatus()
}
