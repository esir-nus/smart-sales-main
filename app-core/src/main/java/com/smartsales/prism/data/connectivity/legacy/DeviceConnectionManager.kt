package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeStateMonitor
import com.smartsales.prism.data.connectivity.legacy.gateway.GattSessionLifecycle
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

// 文件路径: feature/connectivity/src/main/java/com/smartsales/feature/connectivity/DeviceConnectionManager.kt
// 文件作用: 定义设备连接接口及默认实现
// 最近修改: 2025-11-14
interface DeviceConnectionManager {
    val state: StateFlow<ConnectionState>

    /**
     * Badge 录音就绪通知流 — Badge 发送 log#YYYYMMDD_HHMMSS 时触发
     *
     * Emits full downloadable badge filename (e.g., "log_20260209_142605.wav")
     */
    val recordingReadyEvents: SharedFlow<String>

    fun selectPeripheral(peripheral: BlePeripheral)
    suspend fun startPairing(peripheral: BlePeripheral, credentials: WifiCredentials): Result<Unit>
    suspend fun retry(): Result<Unit>

    /** 软断开：停止心跳，保留 session — 可重连 */
    fun disconnectBle()

    /** 硬断开：清空 session + 凭据 — 需要重新配网 */
    fun forgetDevice()

    suspend fun requestHotspotCredentials(): Result<WifiCredentials>
    suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus>

    /** 若已有凭据且未连接，则根据退避策略尝试自动重连。 */
    fun scheduleAutoReconnectIfNeeded()

    /** 立即重连，跳过退避（需已有凭据）。 */
    fun forceReconnectNow()

    /** 挂起式重连：等待 BLE GATT + 网络查询完成后返回实际结果。 */
    suspend fun reconnectAndWait(): ConnectionState
}

@Singleton
class DefaultDeviceConnectionManager @Inject constructor(
    private val provisioner: WifiProvisioner,
    private val bleGateway: GattSessionLifecycle,
    private val dispatchers: DispatcherProvider,
    private val badgeStateMonitor: BadgeStateMonitor,
    private val sessionStore: SessionStore,
    private val phoneWifiProvider: PhoneWifiProvider,
    @ConnectivityScope private val scope: CoroutineScope
) : DeviceConnectionManager, Closeable {

    private val runtime = DeviceConnectionManagerRuntime()
    private val ingressSupport = DeviceConnectionManagerIngressSupport(
        bleGateway = bleGateway,
        dispatchers = dispatchers,
        scope = scope,
        runtime = runtime
    )
    private val connectionSupport = DeviceConnectionManagerConnectionSupport(
        provisioner = provisioner,
        bleGateway = bleGateway,
        dispatchers = dispatchers,
        badgeStateMonitor = badgeStateMonitor,
        sessionStore = sessionStore,
        phoneWifiProvider = phoneWifiProvider,
        scope = scope,
        runtime = runtime,
        ingressSupport = ingressSupport
    )
    private val reconnectSupport = DeviceConnectionManagerReconnectSupport(
        dispatchers = dispatchers,
        scope = scope,
        runtime = runtime,
        connectionSupport = connectionSupport
    )

    override val state: StateFlow<ConnectionState> = runtime.state.asStateFlow()
    override val recordingReadyEvents: SharedFlow<String> = runtime.recordingReadyEvents.asSharedFlow()

    init {
        connectionSupport.restoreSession()
    }

    override fun close() = connectionSupport.cancelAllJobs()

    override fun selectPeripheral(peripheral: BlePeripheral) {
        connectionSupport.selectPeripheral(peripheral)
    }

    override suspend fun startPairing(
        peripheral: BlePeripheral,
        credentials: WifiCredentials
    ): Result<Unit> {
        return connectionSupport.startPairing(peripheral, credentials)
    }

    override suspend fun retry(): Result<Unit> {
        return connectionSupport.retry()
    }

    override fun disconnectBle() {
        connectionSupport.disconnectBle()
    }

    override fun forgetDevice() {
        connectionSupport.forgetDevice()
    }

    override suspend fun requestHotspotCredentials(): Result<WifiCredentials> {
        return connectionSupport.requestHotspotCredentials()
    }

    override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> {
        return connectionSupport.queryNetworkStatus()
    }

    override fun scheduleAutoReconnectIfNeeded() {
        reconnectSupport.scheduleAutoReconnectIfNeeded()
    }

    override fun forceReconnectNow() {
        reconnectSupport.forceReconnectNow()
    }

    override suspend fun reconnectAndWait(): ConnectionState {
        return reconnectSupport.reconnectAndWait()
    }
}
