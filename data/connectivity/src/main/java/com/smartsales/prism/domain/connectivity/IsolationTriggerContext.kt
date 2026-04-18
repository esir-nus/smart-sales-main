// File: data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/IsolationTriggerContext.kt
// Module: :data:connectivity
// Summary: 隔离探测触发场景枚举，用于区分配对后、同步前、断开时、连接时四种探测上下文
// Author: created on 2026-04-18

package com.smartsales.prism.domain.connectivity

/**
 * 客户端隔离探测的触发来源。
 * 决定 ConnectivityModal 中展示的标题与说明文案。
 */
enum class IsolationTriggerContext {
    /** 配对完成后立即探测（原有行为） */
    POST_PAIRING,

    /** 同步前安全检查门：preflight 可达性失败且手机 WiFi 已验证 */
    PRE_SYNC,

    /** 设备意外断开时延迟探测：BLE 掉线后 HTTP 不可达且手机 WiFi 仍已验证 */
    ON_DISCONNECT,

    /** 徽章刚变为 Ready，自动同步 preflight HTTP 探测失败且手机 WiFi 已验证 */
    ON_CONNECT
}
