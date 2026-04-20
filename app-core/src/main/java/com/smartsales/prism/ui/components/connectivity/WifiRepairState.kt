// File: app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/WifiRepairState.kt
// Module: :app-core
// Summary: Wi-Fi 修复流程专用状态机 — 独立于 ConnectivityManagerState
// Author: created on 2026-04-17

package com.smartsales.prism.ui.components.connectivity

/**
 * Wi-Fi 修复流程状态机。
 * 独立于 ConnectivityManagerState，由 ConnectivityViewModel.repairState 暴露。
 * WifiMismatchView 绑定此状态，而非推断 managerState。
 */
sealed class WifiRepairState {

    /** 修复流程未激活，表单未展示 */
    data object Idle : WifiRepairState()

    /** 表单展示中，等待用户输入凭据 */
    data class EditCredentials(val suggestedSsid: String?) : WifiRepairState()

    /** 凭据已提交，正在写入 BLE 通道 */
    data class SendingCredentials(val ssid: String) : WifiRepairState()

    /** BLE 写入完成，等待设备切换网络 */
    data class WaitingForBadgeNetworkSwitch(val ssid: String) : WifiRepairState()

    /** 传输确认：IP 可用 + SSID 匹配 */
    data class TransportConfirmed(val ip: String, val badgeSsid: String?) : WifiRepairState()

    /** 传输已确认，开始独立 HTTP 就绪探测 */
    data class HttpCheckPending(val baseUrl: String) : WifiRepairState()

    /** HTTP 探测成功，修复完全完成 */
    data class HttpReady(val baseUrl: String) : WifiRepairState()

    /**
     * 传输成功，但 HTTP 服务仍在预热。
     * UI 展示"网络已切换成功，服务启动中"，不展示错误样式。
     */
    data class HttpDelayed(val badgeSsid: String?, val baseUrl: String) : WifiRepairState()

    /** 可重试失败：原因不确定，建议用户重新输入凭据 */
    data class RetryableFailure(val reason: String) : WifiRepairState()

    /**
     * 硬失败：原因明确，用户必须采取行动。
     * SSID_MISMATCH / BADGE_OFFLINE / CREDENTIAL_REPLAY_FAILED：重新输入凭据。
     * SUSPECTED_ISOLATION：网络隔离，引导用户切换网络或开启热点。
     */
    data class HardFailure(val reason: HardFailureReason) : WifiRepairState() {
        enum class HardFailureReason {
            SSID_MISMATCH,
            BADGE_OFFLINE,
            CREDENTIAL_REPLAY_FAILED,
            // 配对后 HTTP 探测失败且手机网络已验证 — 疑似客户端隔离
            SUSPECTED_ISOLATION,
        }
    }
}
