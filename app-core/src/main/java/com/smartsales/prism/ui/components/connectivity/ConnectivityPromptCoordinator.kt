package com.smartsales.prism.ui.components.connectivity

import com.smartsales.prism.domain.connectivity.ConnectivityPrompt
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class WifiMismatchPromptRequest(
    val suggestedSsid: String?
)

@Singleton
class ConnectivityPromptCoordinator @Inject constructor() : ConnectivityPrompt {

    private val _wifiMismatchRequests = MutableSharedFlow<WifiMismatchPromptRequest>(
        extraBufferCapacity = 8
    )
    val wifiMismatchRequests: SharedFlow<WifiMismatchPromptRequest> =
        _wifiMismatchRequests.asSharedFlow()

    override suspend fun promptWifiMismatch(suggestedSsid: String?) {
        _wifiMismatchRequests.emit(
            WifiMismatchPromptRequest(
                suggestedSsid = suggestedSsid
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            )
        )
    }
}
