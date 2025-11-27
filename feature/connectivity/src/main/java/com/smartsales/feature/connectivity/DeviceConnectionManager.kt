package com.smartsales.feature.connectivity

import androidx.annotation.VisibleForTesting
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    fun selectPeripheral(peripheral: BlePeripheral)
    suspend fun startPairing(peripheral: BlePeripheral, credentials: WifiCredentials): Result<Unit>
    suspend fun retry(): Result<Unit>
    fun forgetDevice()
    suspend fun requestHotspotCredentials(): Result<WifiCredentials>
    suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus>
}

@Singleton
class DefaultDeviceConnectionManager @Inject constructor(
    private val provisioner: WifiProvisioner,
    private val dispatchers: DispatcherProvider,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.default)
) : DeviceConnectionManager {

    private val scope = externalScope
    private val lock = Mutex()
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var currentSession: BleSession? = null
    private var lastCredentials: WifiCredentials? = null
    private var heartbeatJob: Job? = null
    private var autoRetryJob: Job? = null
    private var autoRetryAttempts = 0

    override fun selectPeripheral(peripheral: BlePeripheral) {
        val session = BleSession.fromPeripheral(peripheral)
        currentSession = session
        _state.value = ConnectionState.Connected(session)
        ConnectivityLogger.d(
            "Select peripheral id=${peripheral.id} name=${peripheral.name} profile=${peripheral.profileId ?: "dynamic"}"
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

    override fun forgetDevice() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        cancelAutoRetry()
        currentSession = null
        lastCredentials = null
        _state.value = ConnectionState.Disconnected
    }

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
            "Provision success device=${session.peripheralName} profile=${session.profileId ?: "dynamic"}"
        )
        _state.value = ConnectionState.WifiProvisioned(session, status)
        startHeartbeat(session, status)
    }

    private fun handleProvisioningFailure(session: BleSession, throwable: Throwable) {
        val error = throwable.toConnectivityError()
        ConnectivityLogger.w(
            "Provision failure device=${session.peripheralName} profile=${session.profileId ?: "dynamic"} error=$error",
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
                    "Auto retry #$autoRetryAttempts for ${session.peripheralName}"
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
            "Network status ok device=${session.peripheralName} ip=${status.ipAddress}"
        )
        _state.value = ConnectionState.WifiProvisioned(session, syntheticStatus)
        startHeartbeat(session, syntheticStatus)
    }

    @VisibleForTesting
    fun overrideStateForTest(state: ConnectionState) {
        _state.value = state
    }

    private companion object {
        const val HEARTBEAT_INTERVAL_MS = 1_500L
        const val AUTO_RETRY_DELAY_MS = 2_000L
        const val AUTO_RETRY_MAX_ATTEMPTS = 2
    }
}
