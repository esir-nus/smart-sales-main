package com.smartsales.prism.data.connectivity.legacy.gateway

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex

internal class GattBleGatewayRuntime {
    val lock = Mutex()
    val sessionLock = Mutex()
    val configCache = mutableMapOf<String, BleGatewayConfig>()
    var persistentSession: GattContext? = null
    var persistentPeripheralId: String? = null
    val badgeNotifications = MutableSharedFlow<BadgeNotification>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val unexpectedDisconnects = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
}
