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
     * Badge SD 卡剩余空间通知流 — Badge 返回 SD#space#<size> 时触发
     */
    val sdCardSpaceEvents: SharedFlow<String>

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

    /** 使用指定会话立即重连，用于注册表切换后避免读到旧运行时会话。 */
    fun forceReconnectToSession(session: BleSession)

    /** 挂起式重连：等待 BLE GATT + 网络查询完成后返回实际结果。 */
    suspend fun reconnectAndWait(): ConnectionState

    /**
     * HTTP 媒体链路失败后的受限凭据重播。
     * 仅在 BLE/GATT 与可用 IP 已成立，但 `/list` / `/download` / `/delete`
     * 或 HTTP readiness 失败时调用；最多 3 次，不能作为后台轮询入口。
     */
    suspend fun replayLatestSavedWifiCredentialForMediaFailure(): ConnectionState

    /**
     * 向徽章发送一次任务完成信号（ASCII "Command#end"）。
     * 若 BLE 未连接或写入失败，静默忽略；不影响调用方流程。
     */
    suspend fun notifyCommandEnd()

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

    /**
     * 请求徽章返回 SD 卡剩余空间，payload 为 "SD#space"。
     * 若 BLE 未连接或写入失败，静默忽略。
     */
    suspend fun requestSdCardSpace(): Boolean

    /** 由 DeviceRegistryManager 在手动断开/重连时调用，控制是否跳过自动重连。 */
    fun setManuallyDisconnected(value: Boolean)
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
    override val sdCardSpaceEvents: SharedFlow<String> = runtime.sdCardSpaceEvents.asSharedFlow()
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

    override fun setManuallyDisconnected(value: Boolean) {
        runtime.activeDeviceManuallyDisconnected = value
    }

    override fun forceReconnectNow() {
        reconnectSupport.forceReconnectNow()
    }

    override fun forceReconnectToSession(session: BleSession) {
        reconnectSupport.forceReconnectNow(session)
    }

    override suspend fun reconnectAndWait(): ConnectionState {
        return reconnectSupport.reconnectAndWait()
    }

    override suspend fun replayLatestSavedWifiCredentialForMediaFailure(): ConnectionState {
        return connectionSupport.replayLatestSavedWifiCredentialForMediaFailure()
    }

    override suspend fun notifyCommandEnd() {
        val session = connectionSupport.currentSessionOrNull() ?: return
        if (!badgeStateMonitor.status.value.bleConnected) return
        try {
            badgeGateway.sendBadgeSignal(session, "Command#end")
            ConnectivityLogger.i("✅ Badge command-end signal sent")
        } catch (ex: Exception) {
            ConnectivityLogger.w("✅ Badge command-end send failed (non-fatal): ${ex.message}")
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

    override suspend fun requestSdCardSpace(): Boolean {
        val session = connectionSupport.currentSessionOrNull() ?: return false
        if (!badgeStateMonitor.status.value.bleConnected) return false
        try {
            badgeGateway.sendBadgeSignal(session, "SD#space")
            ConnectivityLogger.i("💾 Badge SD card space query sent")
            return true
        } catch (ex: Exception) {
            ConnectivityLogger.w("💾 Badge SD card space query failed (non-fatal): ${ex.message}")
            return false
        }
    }
}
