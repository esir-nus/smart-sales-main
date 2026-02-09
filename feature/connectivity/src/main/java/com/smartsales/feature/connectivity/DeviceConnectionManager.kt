package com.smartsales.feature.connectivity

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.badge.BadgeStateMonitor
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// 文件路径: feature/connectivity/src/main/java/com/smartsales/feature/connectivity/DeviceConnectionManager.kt
// 文件作用: 定义设备连接接口及默认实现
// 最近修改: 2025-11-14
interface DeviceConnectionManager {
    val state: StateFlow<ConnectionState>
    
    /**
     * Badge 录音就绪通知流 — Badge 发送 log#YYYYMMDD_HHMMSS 时触发
     * 
     * Emits filename WITHOUT .wav extension (e.g., "20260209_142605")
     * Downstream consumers append ".wav" when downloading via HTTP
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
    private val bleGateway: com.smartsales.feature.connectivity.gateway.GattSessionLifecycle,
    private val dispatchers: DispatcherProvider,
    private val httpChecker: HttpEndpointChecker,
    private val badgeStateMonitor: BadgeStateMonitor,
    private val sessionStore: SessionStore,
    @ConnectivityScope private val scope: CoroutineScope
) : DeviceConnectionManager, Closeable {
    private val lock = Mutex()
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    override val state: StateFlow<ConnectionState> = _state.asStateFlow()
    
    private val _recordingReadyEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 3
    )
    override val recordingReadyEvents: SharedFlow<String> = 
        _recordingReadyEvents.asSharedFlow()

    private var currentSession: BleSession? = null
    private var lastCredentials: WifiCredentials? = null
    private var heartbeatJob: Job? = null
    private var notificationListenerJob: Job? = null
    private var autoRetryJob: Job? = null
    private var autoRetryAttempts = 0
    private var reconnectMeta = AutoReconnectMeta()
    
    init {
        // Restore session from persistence
        sessionStore.load()?.let { (session, credentials) ->
            currentSession = session
            lastCredentials = credentials
            ConnectivityLogger.d("🔌 Restored session: ${session.peripheralId}")
        }
    }

    /** 取消所有后台协程：心跳、通知监听、自动重连 */
    private fun cancelAllJobs() {
        heartbeatJob?.cancel(); heartbeatJob = null
        notificationListenerJob?.cancel(); notificationListenerJob = null
        cancelAutoRetry()
    }

    override fun close() = cancelAllJobs()


    override fun selectPeripheral(peripheral: BlePeripheral) {
        val session = BleSession.fromPeripheral(peripheral)
        currentSession = session
        _state.value = ConnectionState.Connected(session)
        badgeStateMonitor.onBleConnected(session)
        
        // 建立持久 GATT 会话并启动通知监听
        scope.launch(dispatchers.io) {
            val result = bleGateway.connect(peripheral.id)
            if (result is com.smartsales.core.util.Result.Success) {
                startNotificationListener(session)
                ConnectivityLogger.i("📡 Persistent session + listener active for ${peripheral.name}")
            } else {
                ConnectivityLogger.w("⚠️ Failed to establish persistent GATT for ${peripheral.name}: $result")
            }
        }
        
        ConnectivityLogger.d(
            "🔌 Select peripheral id=${peripheral.id} name=${peripheral.name} profile=${peripheral.profileId ?: "dynamic"}"
        )
    }

    override suspend fun startPairing(
        peripheral: BlePeripheral,
        credentials: WifiCredentials
    ): Result<Unit> = lock.withLock {
        if (_state.value is ConnectionState.Pairing) {
            return@withLock Result.Error(
                IllegalStateException("Pairing already in progress for ${peripheral.name}")
            )
        }

        currentSession = BleSession.fromPeripheral(peripheral)
        lastCredentials = credentials
        autoRetryAttempts = 0
        cancelAutoRetry()
        _state.value = ConnectionState.Pairing(
            deviceName = peripheral.name,
            progressPercent = 10,
            signalStrengthDbm = peripheral.signalStrengthDbm
        )
        launchProvisioning()
        Result.Success(Unit)
    }

    override suspend fun retry(): Result<Unit> = lock.withLock {
        val session = currentSession
        val credentials = lastCredentials
        if (session == null || credentials == null) {
            return@withLock Result.Error(IllegalStateException("No previous pairing attempt to retry"))
        }
        heartbeatJob?.cancel()
        cancelAutoRetry()
        autoRetryAttempts = 0
        _state.value = ConnectionState.Pairing(
            deviceName = session.peripheralName,
            progressPercent = 5,
            signalStrengthDbm = session.signalStrengthDbm
        )
        launchProvisioning()
        Result.Success(Unit)
    }
    
    override fun disconnectBle() {
        cancelAllJobs()
        badgeStateMonitor.onBleDisconnected()
        scope.launch(dispatchers.io) { bleGateway.disconnect() }
        // currentSession + lastCredentials KEPT — reconnection possible
        _state.value = ConnectionState.Disconnected
        ConnectivityLogger.d("🔌 Soft disconnect (session preserved)")
    }

    override fun forgetDevice() {
        cancelAllJobs()
        badgeStateMonitor.onBleDisconnected()
        scope.launch(dispatchers.io) { bleGateway.disconnect() }
        sessionStore.clear()
        currentSession = null
        lastCredentials = null
        reconnectMeta = AutoReconnectMeta()
        _state.value = ConnectionState.Disconnected
        ConnectivityLogger.d("🔌 Hard disconnect (session cleared)")
    }

    override fun scheduleAutoReconnectIfNeeded() {
        val credsReady = hasStoredSession()
        if (!credsReady) {
            _state.value = ConnectionState.NeedsSetup
            return
        }
        when (_state.value) {
            is ConnectionState.AutoReconnecting,
            is ConnectionState.Pairing -> return
            else -> Unit
        }
        val now = System.currentTimeMillis()
        val requiredInterval = requiredIntervalFor(reconnectMeta.failureCount)
        val elapsed = now - reconnectMeta.lastAttemptMillis
        if (elapsed < requiredInterval) {
            ConnectivityLogger.d("🔄 Backoff: ${elapsed}ms < ${requiredInterval}ms")
            return
        }
        launchReconnect()
    }

    override fun forceReconnectNow() {
        if (!hasStoredSession()) {
            _state.value = ConnectionState.NeedsSetup
            return
        }
        launchReconnect(ignoreBackoff = true)
    }

    override suspend fun reconnectAndWait(): ConnectionState = withContext(dispatchers.io) {
        val sessionSnapshot = currentSession
        if (sessionSnapshot == null) {
            _state.value = ConnectionState.NeedsSetup
            return@withContext ConnectionState.NeedsSetup
        }
        _state.value = ConnectionState.AutoReconnecting(1)
        val outcome = connectUsingSession(sessionSnapshot)
        when (outcome) {
            is ConnectionState.Connected -> {
                _state.value = outcome
                reconnectMeta = AutoReconnectMeta()
            }
            else -> {
                _state.value = ConnectionState.Disconnected
            }
        }
        outcome
    }

    private fun launchReconnect(ignoreBackoff: Boolean = false) {
        if (ignoreBackoff) {
            reconnectMeta = reconnectMeta.copy(lastAttemptMillis = 0L)
        }
        val attempt = reconnectMeta.failureCount + 1
        _state.value = ConnectionState.AutoReconnecting(attempt)
        val sessionSnapshot = currentSession
        scope.launch(dispatchers.io) {
            reconnectMeta = reconnectMeta.copy(lastAttemptMillis = System.currentTimeMillis())
            if (sessionSnapshot == null) {
                _state.value = ConnectionState.NeedsSetup
                return@launch
            }
            when (val outcome = connectUsingSession(sessionSnapshot)) {
                is ConnectionState.Connected -> {
                    _state.value = outcome
                    reconnectMeta = AutoReconnectMeta()
                }
                is ConnectionState.Error -> {
                    val nextFailure = reconnectMeta.failureCount + 1
                    reconnectMeta = reconnectMeta.copy(failureCount = nextFailure)
                    _state.value = outcome
                    _state.value = ConnectionState.Disconnected
                }
                else -> {
                    val nextFailure = reconnectMeta.failureCount + 1
                    reconnectMeta = reconnectMeta.copy(failureCount = nextFailure)
                    _state.value = ConnectionState.Disconnected
                }
            }
        }
    }

    private fun hasStoredSession(): Boolean = currentSession != null

    override suspend fun requestHotspotCredentials(): Result<WifiCredentials> =
        withContext(dispatchers.io) {
            val session = currentSession
            if (session == null) {
                Result.Error(IllegalStateException("No active session"))
            } else {
                provisioner.requestHotspotCredentials(session)
            }
        }

    override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> =
        withContext(dispatchers.io) {
            val session = currentSession
            if (session == null) {
                Result.Error(IllegalStateException("No active session"))
            } else {
                when (val result = provisioner.queryNetworkStatus(session)) {
                    is Result.Success -> {
                        handleNetworkStatusSuccess(session, result.data)
                        result
                    }
                    is Result.Error -> result
                }
            }
        }

    private fun launchProvisioning() {
        val session = currentSession ?: return
        val credentials = lastCredentials ?: return
        heartbeatJob?.cancel()
        scope.launch(dispatchers.io) {
            when (val result = provisioner.provision(session, credentials)) {
                is Result.Success -> handleProvisioningSuccess(session, result.data)
                is Result.Error -> handleProvisioningFailure(session, result.throwable)
            }
        }
    }

    private fun handleProvisioningSuccess(session: BleSession, status: ProvisioningStatus) {
        autoRetryAttempts = 0
        cancelAutoRetry()
        ConnectivityLogger.i(
            "✅ Provision success device=${session.peripheralName} profile=${session.profileId ?: "dynamic"}"
        )
        
        // Persist session and credentials for reconnection
        lastCredentials?.let { credentials ->
            sessionStore.save(session, credentials)
            ConnectivityLogger.d("🔌 Session persisted: ${session.peripheralId}")
        }
        
        _state.value = ConnectionState.WifiProvisioned(session, status)
        startHeartbeat(session, status)
        startNotificationListener(session)
    }

    private fun handleProvisioningFailure(session: BleSession, throwable: Throwable) {
        val error = throwable.toConnectivityError()
        ConnectivityLogger.w(
            "❌ Provision failure device=${session.peripheralName} profile=${session.profileId ?: "dynamic"} error=$error",
            throwable
        )
        _state.value = ConnectionState.Error(error)
        if (shouldAutoRetry(error)) {
            scheduleAutoRetry()
        }
    }

    private fun shouldAutoRetry(error: ConnectivityError): Boolean =
        error is ConnectivityError.Timeout || error is ConnectivityError.Transport

    private fun scheduleAutoRetry() {
        if (autoRetryAttempts >= AUTO_RETRY_MAX_ATTEMPTS) return
        autoRetryJob?.cancel()
        autoRetryJob = scope.launch(dispatchers.default) {
            delay(AUTO_RETRY_DELAY_MS)
            lock.withLock {
                val session = currentSession
                val credentials = lastCredentials
                if (session == null || credentials == null) return@withLock
                if (_state.value !is ConnectionState.Error) return@withLock
                autoRetryAttempts += 1
                ConnectivityLogger.i(
                    "🔄 Auto retry #$autoRetryAttempts for ${session.peripheralName}"
                )
                _state.value = ConnectionState.Pairing(
                    deviceName = session.peripheralName,
                    progressPercent = 5,
                    signalStrengthDbm = session.signalStrengthDbm
                )
                launchProvisioning()
            }
        }
    }

    private fun cancelAutoRetry() {
        autoRetryJob?.cancel()
        autoRetryJob = null
    }

    private fun startHeartbeat(session: BleSession, status: ProvisioningStatus) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(dispatchers.default) {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                _state.value = ConnectionState.Syncing(
                    session = session,
                    status = status,
                    lastHeartbeatAtMillis = System.currentTimeMillis()
                )
            }
        }
    }

    private fun handleNetworkStatusSuccess(session: BleSession, status: DeviceNetworkStatus) {
        val wifiName = status.deviceWifiName.ifBlank { "BT311" }
        val syntheticStatus = ProvisioningStatus(
            wifiSsid = wifiName,
            handshakeId = "network-${status.rawResponse.hashCode()}",
            credentialsHash = "${wifiName}-${status.ipAddress}".hashCode().toString()
        )
        ConnectivityLogger.d(
            "🌐 Network status ok device=${session.peripheralName} ip=${status.ipAddress}"
        )
        _state.value = ConnectionState.WifiProvisioned(session, syntheticStatus)
        startHeartbeat(session, syntheticStatus)
    }

    private suspend fun connectUsingSession(session: BleSession): ConnectionState {
        // 建立持久 GATT 会话
        bleGateway.connect(session.peripheralId).let { result ->
            if (result is com.smartsales.core.util.Result.Error) {
                ConnectivityLogger.w("🔌 Persistent GATT connect failed: ${result.throwable.message}")
                // 不阻塞 — 仍尝试通过 provisioner 查询
            }
        }
        
        val networkStatus = provisioner.queryNetworkStatus(session)
        return when (networkStatus) {
            is Result.Success -> {
                val ip = networkStatus.data.ipAddress
                if (ip.isNullOrBlank() || ip == "0.0.0.0" || ip.startsWith("0.")) {
                    ConnectivityLogger.d("🔌 connectUsingSession: badge offline, ip=$ip")
                    ConnectionState.Error(ConnectivityError.EndpointUnreachable("设备未连接WiFi"))
                } else {
                    ConnectivityLogger.d("🔌 connectUsingSession: connected, ip=$ip")
                    startNotificationListener(session)
                    ConnectionState.Connected(session)
                }
            }

            is Result.Error -> {
                val error = mapProvisioningError(networkStatus.throwable)
                ConnectionState.Error(error)
            }
        }
    }
    
    /**
     * 启动持久通知监听器，接收 log# 和 tim# 等消息。
     */
    private fun startNotificationListener(session: BleSession) {
        notificationListenerJob?.cancel()
        notificationListenerJob = scope.launch(dispatchers.io) {
            ConnectivityLogger.d("📡 Starting persistent notification listener for ${session.peripheralId}")
            bleGateway.listenForBadgeNotifications().collect { event ->
                when (event) {
                    is com.smartsales.feature.connectivity.gateway.BadgeNotification.RecordingReady -> {
                        ConnectivityLogger.i("📥 Badge recording ready: ${event.filename}")
                        _recordingReadyEvents.tryEmit(event.filename)
                    }
                    is com.smartsales.feature.connectivity.gateway.BadgeNotification.TimeSyncRequested -> {
                        ConnectivityLogger.d("⏰ Badge time sync requested")
                    }
                    is com.smartsales.feature.connectivity.gateway.BadgeNotification.Unknown -> {
                        ConnectivityLogger.d("❓ Badge unknown notification: ${event.raw}")
                    }
                }
            }
        }
    }

    private fun mapProvisioningError(throwable: Throwable): ConnectivityError {
        return when (throwable) {
            is ProvisioningException.Transport -> {
                val msg = throwable.message ?: "传输失败"
                if (msg.contains("找不到设备")) {
                    ConnectivityError.DeviceNotFound(currentSession?.peripheralId ?: "unknown")
                } else {
                    ConnectivityError.Transport(msg)
                }
            }

            else -> throwable.toConnectivityError()
        }
    }

    private fun buildBaseUrl(host: String?): String? {
        val sanitizedHost = host?.trim().orEmpty().ifBlank { return null }
        val pureHost = sanitizedHost.removePrefix("http://").removePrefix("https://").substringBefore("/")
        if (pureHost.isBlank()) return null
        val port = DEFAULT_MEDIA_SERVER_PORT
        return "http://$pureHost:$port"
    }

    private companion object {
        const val HEARTBEAT_INTERVAL_MS = 1_500L
        const val AUTO_RETRY_DELAY_MS = 2_000L
        const val AUTO_RETRY_MAX_ATTEMPTS = 2
        const val DEFAULT_MEDIA_SERVER_PORT = 8088
    }
}

private data class AutoReconnectMeta(
    val lastAttemptMillis: Long = 0L,
    val failureCount: Int = 0
)

private fun requiredIntervalFor(failureCount: Int): Long = when (failureCount) {
    0 -> 0L
    1 -> 5_000L
    2 -> 10_000L
    3 -> 20_000L
    4 -> 40_000L
    else -> 60_000L
}
