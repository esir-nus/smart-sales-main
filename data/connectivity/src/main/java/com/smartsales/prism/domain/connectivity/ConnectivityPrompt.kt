package com.smartsales.prism.domain.connectivity

/**
 * 连接修复提示接口。
 *
 * 允许非 UI 层在不依赖具体 ViewModel / Compose 宿主的前提下，
 * 请求现有的 Wi-Fi mismatch 修复表单。
 */
interface ConnectivityPrompt {
    suspend fun promptWifiMismatch(suggestedSsid: String?)
}
