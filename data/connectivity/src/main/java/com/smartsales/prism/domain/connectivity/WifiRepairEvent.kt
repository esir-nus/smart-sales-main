// File: data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/WifiRepairEvent.kt
// Module: :data:connectivity
// Summary: Wi-Fi 修复流程的细粒度事件流 — 传输优先成功语义
// Author: created on 2026-04-17

package com.smartsales.prism.domain.connectivity

/**
 * Wi-Fi 修复流程事件。
 * 由 legacy 层发射，经 ConnectivityBridge 暴露给 ViewModel。
 * 传输确认（TransportConfirmed）是修复成功的权威信号；HTTP 就绪是后续状态。
 */
sealed class WifiRepairEvent {

    /** 凭据已写入 BLE 通道，设备正在处理 */
    data class CredentialsDispatched(val ssid: String) : WifiRepairEvent()

    /** 从设备读到的 SSID 与目标匹配 */
    data class TargetSsidObserved(val badgeSsid: String) : WifiRepairEvent()

    /** 设备已获得可用 IP（非 0.0.0.0） */
    data class UsableIpObserved(val ip: String) : WifiRepairEvent()

    /**
     * 传输层确认：IP 可用 + SSID 匹配（若可读）+ BLE 存活。
     * 这是"网络切换成功"的权威信号，HTTP 就绪是独立的后续状态。
     */
    data class TransportConfirmed(
        val ip: String,
        val badgeSsid: String?,
        val runtimeKeyLog: String,
    ) : WifiRepairEvent()

    /** HTTP :8088 在传输确认后探测成功 */
    data class HttpReady(val baseUrl: String) : WifiRepairEvent()

    /**
     * HTTP 在传输确认后的 grace 探测预算内未就绪。
     * 这不是失败 — 服务仍在预热，UI 应展示非错误状态。
     */
    data class HttpDelayed(val baseUrl: String) : WifiRepairEvent()

    /** 设备 SSID 与目标明确不匹配 — 凭据有误 */
    data class DefinitiveMismatch(
        val expectedSsid: String,
        val badgeSsid: String,
    ) : WifiRepairEvent()

    /** grace 预算耗尽，设备始终未接入网络 */
    data object BadgeOffline : WifiRepairEvent()

    /** 凭据重播失败（对称性保留，供 reconnect 路径使用） */
    data object CredentialReplayFailed : WifiRepairEvent()
}
