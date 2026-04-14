package com.smartsales.prism.data.connectivity.legacy.gateway

import com.smartsales.core.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * 持久 GATT 会话的生命周期接口。
 * GattBleGateway 实现此接口；测试使用 Fake。
 */
interface GattSessionLifecycle {
    suspend fun connect(peripheralId: String): Result<Unit>
    suspend fun disconnect()
    fun listenForBadgeNotifications(): Flow<BadgeNotification>
    suspend fun isReachable(): Boolean
}
