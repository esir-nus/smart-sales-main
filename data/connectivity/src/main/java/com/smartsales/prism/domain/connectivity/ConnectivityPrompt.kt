package com.smartsales.prism.domain.connectivity

/**
 * 连接修复提示接口。
 *
 * 允许非 UI 层在不依赖具体 ViewModel / Compose 宿主的前提下，
 * 请求现有的 Wi-Fi mismatch 修复表单。
 */
interface ConnectivityPrompt {
    suspend fun promptWifiMismatch(suggestedSsid: String?)

    /**
     * HTTP 不可达且手机 WiFi 已验证 → 疑似 AP 客户端隔离。
     *
     * @param badgeIp 探测失败的徽章 IP，用于界面诊断展示
     * @param triggerContext 探测触发来源（配对后 / 同步前 / 断开时），决定界面文案
     */
    suspend fun promptSuspectedIsolation(
        badgeIp: String,
        triggerContext: IsolationTriggerContext = IsolationTriggerContext.POST_PAIRING
    )
}
