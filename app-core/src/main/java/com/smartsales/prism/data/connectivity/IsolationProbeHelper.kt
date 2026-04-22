// File: app-core/src/main/java/com/smartsales/prism/data/connectivity/IsolationProbeHelper.kt
// Module: :app-core
// Summary: 共用的 AP 客户端隔离探测函数，消除配对后、同步前、断开时三处重复的
//          "!isReachable && isValidated" 判断逻辑
// Author: created on 2026-04-18

package com.smartsales.prism.data.connectivity

import android.util.Log
import com.smartsales.prism.data.connectivity.legacy.BadgeHttpClient
import com.smartsales.prism.data.connectivity.legacy.PhoneWifiProvider
import com.smartsales.prism.data.connectivity.legacy.PhoneWifiSnapshot
import com.smartsales.prism.domain.connectivity.ConnectivityPrompt
import com.smartsales.prism.domain.connectivity.IsolationTriggerContext

private const val ISOLATION_PROBE_TAG = "IsolationProbe"

/**
 * 对指定徽章 IP 执行 AP 客户端隔离探测。
 *
 * 探测条件（两者同时满足时疑似隔离）：
 *  1. 手机 WiFi 已通过 NET_CAPABILITY_VALIDATED（互联网可达）
 *  2. 徽章 HTTP :8088 不可达（TCP 连接被拒绝或超时）
 *
 * 探测结果不阻断调用方；若疑似隔离则异步触发 ConnectivityPrompt，
 * 由 UI 层（ConnectivityViewModel → ConnectivityModal）决策展示。
 *
 * ESP32 约束：此函数不发起任何 BLE 命令，仅执行单次 HTTP 探测，
 * 不会对 ESP32 产生额外的 BLE 查询压力。
 *
 * @param badgeIp 当前已知徽章 IP；为 null 时立即返回 false
 * @param httpClient 用于 HTTP 可达性探测的客户端
 * @param phoneWifiProvider 用于读取手机 WiFi 验证状态
 * @param connectivityPrompt 疑似隔离时触发提示的接口
 * @param triggerContext 本次探测的触发来源，影响界面展示文案
 * @return 是否疑似 AP 客户端隔离
 */
internal suspend fun runIsolationProbeIfSuspected(
    badgeIp: String?,
    httpClient: BadgeHttpClient,
    phoneWifiProvider: PhoneWifiProvider,
    connectivityPrompt: ConnectivityPrompt,
    triggerContext: IsolationTriggerContext
): Boolean {
    if (badgeIp == null) return false

    val snapshot = phoneWifiProvider.currentWifiSnapshot()
    val isValidated = snapshot is PhoneWifiSnapshot.Connected && snapshot.isValidated
    // HTTP 探测：仅对已有徽章 IP 的端点发起，不触发新的 BLE 查询
    val isReachable = httpClient.isReachable("http://$badgeIp:8088")

    Log.d(
        ISOLATION_PROBE_TAG,
        "probe ip=$badgeIp isValidated=$isValidated isReachable=$isReachable context=${triggerContext.name.lowercase()}"
    )

    return if (!isReachable && isValidated) {
        Log.w(
            ISOLATION_PROBE_TAG,
            "Suspected AP isolation. ip=$badgeIp context=${triggerContext.name.lowercase()}"
        )
        connectivityPrompt.promptSuspectedIsolation(badgeIp, triggerContext)
        true
    } else {
        false
    }
}
