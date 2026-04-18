package com.smartsales.prism.ui.components.connectivity

import com.smartsales.prism.domain.connectivity.ConnectivityPrompt
import com.smartsales.prism.domain.connectivity.IsolationTriggerContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class WifiMismatchPromptRequest(
    val suggestedSsid: String?
)

// 疑似客户端隔离提示请求 — 携带徽章 IP 和触发场景用于界面诊断展示及文案选择
data class SuspectedIsolationPromptRequest(
    val badgeIp: String,
    val triggerContext: IsolationTriggerContext = IsolationTriggerContext.POST_PAIRING
)

@Singleton
class ConnectivityPromptCoordinator @Inject constructor() : ConnectivityPrompt {

    private val _wifiMismatchRequests = MutableSharedFlow<WifiMismatchPromptRequest>(
        extraBufferCapacity = 8
    )
    val wifiMismatchRequests: SharedFlow<WifiMismatchPromptRequest> =
        _wifiMismatchRequests.asSharedFlow()

    private val _suspectedIsolationRequests = MutableSharedFlow<SuspectedIsolationPromptRequest>(
        extraBufferCapacity = 8
    )
    val suspectedIsolationRequests: SharedFlow<SuspectedIsolationPromptRequest> =
        _suspectedIsolationRequests.asSharedFlow()

    override suspend fun promptWifiMismatch(suggestedSsid: String?) {
        _wifiMismatchRequests.emit(
            WifiMismatchPromptRequest(
                suggestedSsid = suggestedSsid
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            )
        )
    }

    override suspend fun promptSuspectedIsolation(
        badgeIp: String,
        triggerContext: IsolationTriggerContext
    ) {
        _suspectedIsolationRequests.emit(
            SuspectedIsolationPromptRequest(
                badgeIp = badgeIp.trim(),
                triggerContext = triggerContext
            )
        )
    }
}
