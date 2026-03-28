package com.smartsales.prism.data.connectivity.legacy.gateway

import android.bluetooth.BluetoothManager
import android.content.Context
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.ConnectivityScope
import com.smartsales.prism.data.connectivity.legacy.WifiCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/gateway/GattBleGateway.kt
// 模块：:feature:connectivity
// 说明：通过 Android BLE GATT 写入 Wi-Fi 凭据并读取热点信息
// 作者：创建于 2025-11-16
@Singleton
class GattBleGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothManager: BluetoothManager,
    private val dispatchers: DispatcherProvider,
    @ConnectivityScope private val scope: CoroutineScope
) : BleGateway, GattSessionLifecycle {

    private val runtime = GattBleGatewayRuntime()
    private val protocolSupport = GattBleGatewayProtocolSupport()
    private val sessionSupport = GattBleGatewaySessionSupport(
        context = context,
        bluetoothManager = bluetoothManager,
        dispatchers = dispatchers,
        scope = scope,
        runtime = runtime,
        protocolSupport = protocolSupport
    )

    override suspend fun connect(peripheralId: String): com.smartsales.core.util.Result<Unit> {
        return sessionSupport.connect(peripheralId)
    }

    override suspend fun disconnect() {
        sessionSupport.disconnect()
    }

    override fun listenForBadgeNotifications(): Flow<BadgeNotification> {
        return sessionSupport.listenForBadgeNotifications()
    }

    override suspend fun provision(
        session: BleSession,
        credentials: WifiCredentials
    ): BleGatewayResult {
        return sessionSupport.provision(session, credentials)
    }

    override suspend fun requestHotspot(session: BleSession): HotspotResult {
        return sessionSupport.requestHotspot(session)
    }

    override suspend fun queryNetwork(session: BleSession): NetworkQueryResult {
        return sessionSupport.queryNetwork(session)
    }

    override suspend fun sendWavCommand(
        session: BleSession,
        command: WavCommand
    ): WavCommandResult {
        return sessionSupport.sendWavCommand(session, command)
    }

    override fun forget(peripheral: BlePeripheral) {
        sessionSupport.forget(peripheral)
    }
}
