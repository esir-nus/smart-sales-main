package com.smartsales.prism.data.connectivity.legacy.gateway

import com.smartsales.core.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * 持久 GATT 会话的生命周期接口。
 * GattBleGateway 实现此接口；测试使用 Fake。
 */
interface GattSessionLifecycle {
    suspend fun connect(peripheralId: String, isReconnect: Boolean = false): Result<Unit>
    suspend fun disconnect()
    fun listenForBadgeNotifications(): Flow<BadgeNotification>

    /** 当持久 GATT 会话意外断开时触发（僵尸会话清理后）。 */
    fun unexpectedDisconnects(): Flow<Unit>
}
