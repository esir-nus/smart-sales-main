package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.BadgeEndpointRecoveryCoordinator
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeStateMonitor
import com.smartsales.prism.data.connectivity.legacy.gateway.BleGateway
import com.smartsales.prism.data.connectivity.legacy.gateway.GattSessionLifecycle
import com.smartsales.prism.data.connectivity.legacy.scan.BleScanner
import com.smartsales.prism.domain.connectivity.WifiRepairEvent
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

    /**
     * Badge 音频就绪通知流 — Badge 发送 rec#YYYYMMDD_HHMMSS 时触发
     *
     * Emits full downloadable badge filename (e.g., "rec_20260409_150000.wav")
     */
    val audioRecordingReadyEvents: SharedFlow<String>

    /**
     * Badge 电量通知流 — Badge 发送 Bat#<0..100> 时触发
     */
    val batteryEvents: SharedFlow<Int>

    /**
     * Badge 固件版本通知流 — Badge 返回 Ver#<project>.<major>.<minor>.<feature> 时触发
     */
    val firmwareVersionEvents: SharedFlow<String>

    /**
     * Wi-Fi 修复流程细粒度事件流。
     * Hot flow，replay=0，仅在 confirmManualWifiProvision() 窗口内发射。
     */
    val wifiRepairEvents: SharedFlow<WifiRepairEvent>

    fun selectPeripheral(peripheral: BlePeripheral)
    suspend fun startPairing(peripheral: BlePeripheral, credentials: WifiCredentials): Result<Unit>
    suspend fun retry(): Result<Unit>

    /** 软断开：停止心跳，保留 session — 可重连 */
    fun disconnectBle()

    /** 硬断开：清空 session + 凭据 — 需要重新配网 */
    fun forgetDevice()

    suspend fun requestHotspotCredentials(): Result<WifiCredentials>
    suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus>
    suspend fun confirmManualWifiProvision(credentials: WifiCredentials): ConnectionState

    /** 若已有凭据且未连接，则根据退避策略尝试自动重连。 */
    fun scheduleAutoReconnectIfNeeded()

    /** 立即重连，跳过退避（需已有凭据）。 */
    fun forceReconnectNow()

    /** 挂起式重连：等待 BLE GATT + 网络查询完成后返回实际结果。 */
    suspend fun reconnectAndWait(): ConnectionState

    /**
     * 向徽章发送一次"任务创建成功"信号（ASCII "commandend#1"）。
     * 若 BLE 未连接或写入失败，静默忽略；不影响调用方流程。
     */
    suspend fun notifyTaskCreated()

    /**
     * 向徽章发送一次"任务闹钟触发"信号（ASCII "commandend#1"）。
     * 若 BLE 未连接或写入失败，静默忽略；不影响调用方流程。
     */
    suspend fun notifyTaskFired()

    /**
     * 设置徽章语音播报音量，payload 为 "volume#<0..100>"。
     * UI 层应仅在滑块松手时调用，避免 ESP32 端高频写入。
     * 若 BLE 未连接或写入失败，静默忽略。
     */
    suspend fun setVoiceVolume(level: Int): Boolean

    /**
     * 请求徽章返回固件版本，payload 为 "Ver#get"。
     * 若 BLE 未连接或写入失败，静默忽略。
     */
    suspend fun requestFirmwareVersion(): Boolean
}

@Singleton
class DefaultDeviceConnectionManager @Inject constructor(
    private val provisioner: WifiProvisioner,
    private val bleGateway: GattSessionLifecycle,
    private val badgeGateway: BleGateway,
    private val badgeHttpClient: BadgeHttpClient,
    private val endpointRecoveryCoordinator: BadgeEndpointRecoveryCoordinator,
    private val dispatchers: DispatcherProvider,
    private val badgeStateMonitor: BadgeStateMonitor,
    private val sessionStore: SessionStore,
    private val bleScanner: BleScanner,
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
        httpReachabilityProbe = { baseUrl -> badgeHttpClient.isReachable(baseUrl) },
        endpointRecoveryCoordinator = endpointRecoveryCoordinator,
        dispatchers = dispatchers,
        badgeStateMonitor = badgeStateMonitor,
        sessionStore = sessionStore,
        scope = scope,
        runtime = runtime,
        ingressSupport = ingressSupport,
        bleScanner = bleScanner
    )
    private val reconnectSupport = DeviceConnectionManagerReconnectSupport(
        dispatchers = dispatchers,
        scope = scope,
        runtime = runtime,
        connectionSupport = connectionSupport
    )

    override val state: StateFlow<ConnectionState> = runtime.state.asStateFlow()
    override val recordingReadyEvents: SharedFlow<String> = runtime.recordingReadyEvents.asSharedFlow()
    override val audioRecordingReadyEvents: SharedFlow<String> = runtime.audioRecordingReadyEvents.asSharedFlow()
    override val batteryEvents: SharedFlow<Int> = runtime.batteryEvents.asSharedFlow()
    override val firmwareVersionEvents: SharedFlow<String> = runtime.firmwareVersionEvents.asSharedFlow()
    override val wifiRepairEvents: SharedFlow<WifiRepairEvent> = runtime.repairEvents.asSharedFlow()

    init {
        connectionSupport.reconnectSupport = reconnectSupport
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

    override suspend fun confirmManualWifiProvision(credentials: WifiCredentials): ConnectionState {
        return connectionSupport.confirmManualWifiProvision(credentials)
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

    override suspend fun notifyTaskCreated() {
        val session = connectionSupport.currentSessionOrNull() ?: return
        if (!badgeStateMonitor.status.value.bleConnected) return
        try {
            badgeGateway.sendBadgeSignal(session, "commandend#1")
            ConnectivityLogger.i("🔔 Badge chime signal sent for task create")
        } catch (ex: Exception) {
            ConnectivityLogger.w("🔔 Badge chime send failed on task create (non-fatal): ${ex.message}")
        }
    }

    override suspend fun notifyTaskFired() {
        val session = connectionSupport.currentSessionOrNull() ?: return
        if (!badgeStateMonitor.status.value.bleConnected) return
        try {
            badgeGateway.sendBadgeSignal(session, "commandend#1")
            ConnectivityLogger.i("🔔 Badge chime signal sent for task fire")
        } catch (ex: Exception) {
            ConnectivityLogger.w("🔔 Badge chime send failed (non-fatal): ${ex.message}")
        }
    }

    override suspend fun setVoiceVolume(level: Int): Boolean {
        val clamped = level.coerceIn(0, 100)
        val session = connectionSupport.currentSessionOrNull() ?: return false
        if (!badgeStateMonitor.status.value.bleConnected) return false
        try {
            badgeGateway.sendBadgeSignal(session, "volume#$clamped")
            ConnectivityLogger.i("🔊 Badge voice volume set to $clamped")
            return true
        } catch (ex: Exception) {
            ConnectivityLogger.w("🔊 Badge volume send failed (non-fatal): ${ex.message}")
            return false
        }
    }

    override suspend fun requestFirmwareVersion(): Boolean {
        val session = connectionSupport.currentSessionOrNull() ?: return false
        if (!badgeStateMonitor.status.value.bleConnected) return false
        try {
            badgeGateway.sendBadgeSignal(session, "Ver#get")
            ConnectivityLogger.i("🧾 Badge firmware version query sent")
            return true
        } catch (ex: Exception) {
            ConnectivityLogger.w("🧾 Badge firmware version query failed (non-fatal): ${ex.message}")
            return false
        }
    }
}
