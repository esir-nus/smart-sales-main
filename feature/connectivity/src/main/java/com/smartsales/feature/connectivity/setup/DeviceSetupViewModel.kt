package com.smartsales.feature.connectivity.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleProfileConfig
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.connectivity.scan.BleScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/setup/DeviceSetupViewModel.kt
// 模块：:feature:connectivity
// 说明：封装扫描、配对、配网、等待上线到就绪的 UI 状态机
// 作者：创建于 2025-11-30
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DeviceSetupViewModel @Inject constructor(
    private val connectionManager: DeviceConnectionManager,
    private val bleScanner: BleScanner,
    private val bleProfiles: List<BleProfileConfig>
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceSetupUiState())
    val uiState: MutableStateFlow<DeviceSetupUiState> = _uiState

    private val _events = MutableSharedFlow<DeviceSetupEvent>()
    val events: MutableSharedFlow<DeviceSetupEvent> = _events

    private var networkCheckJob: Job? = null
    private var scanTimeoutJob: Job? = null
    private var pendingPeripheral: BlePeripheral? = null
    private var hasEmittedReadyEvent = false
    private var hasStarted = false

    init {
        observeConnection()
        observeScanner()
        startAutoScan()
    }

    private fun startAutoScan() {
        if (hasStarted) return
        hasStarted = true
        bleScanner.start()
        updateUi {
            it.copy(
                step = DeviceSetupStep.Scanning,
                isScanning = true,
                isActionInProgress = true,
                errorMessage = null,
                errorReason = null
            )
        }
        startScanTimeout()
    }

    fun onWifiSsidChanged(value: String) {
        updateUi { it.copy(wifiSsid = value) }
    }

    fun onWifiPasswordChanged(value: String) {
        updateUi { it.copy(wifiPassword = value) }
    }

    fun onPrimaryClick() {
        when (_uiState.value.step) {
            DeviceSetupStep.Idle,
            DeviceSetupStep.Scanning -> {
                if (!hasStarted) startAutoScan()
            }
            DeviceSetupStep.Found -> {
                selectPendingPeripheral()
                updateUi { it.copy(step = DeviceSetupStep.WifiInput, showWifiForm = true) }
            }
            DeviceSetupStep.WifiInput -> {
                val ssid = _uiState.value.wifiSsid.trim()
                val password = _uiState.value.wifiPassword.trim()
                if (ssid.isBlank() || password.isBlank()) return
                onProvisionWifi(ssid, password)
            }
            DeviceSetupStep.Pairing,
            DeviceSetupStep.WifiProvisioning,
            DeviceSetupStep.WaitingForDeviceOnline -> Unit
            DeviceSetupStep.Error -> onRetry()
            DeviceSetupStep.Ready -> viewModelScope.launch { _events.emit(DeviceSetupEvent.OpenDeviceManager) }
        }
    }

    fun onSecondaryClick() {
        viewModelScope.launch { _events.emit(DeviceSetupEvent.BackToHome) }
    }

    fun onRetry() {
        pendingPeripheral = null
        hasEmittedReadyEvent = false
        hasStarted = false
        bleScanner.stop()
        updateUi {
            it.copy(
                step = DeviceSetupStep.Scanning,
                errorMessage = null,
                errorReason = null,
                isActionInProgress = true,
                isScanning = false,
                isSubmittingWifi = false,
                wifiPassword = "",
                wifiSsid = ""
            )
        }
        connectionManager.forgetDevice()
        startAutoScan()
    }

    private fun selectPendingPeripheral() {
        val target = pendingPeripheral ?: return
        connectionManager.selectPeripheral(target)
    }

    private fun onProvisionWifi(ssid: String, password: String) {
        updateUi {
            it.copy(
                step = DeviceSetupStep.WifiProvisioning,
                wifiSsid = ssid,
                wifiPassword = password,
                errorMessage = null,
                isActionInProgress = true,
                isSubmittingWifi = true
            )
        }
        val current = connectionManager.state.value
        val session = when (current) {
            is ConnectionState.Connected -> current.session
            is ConnectionState.Pairing -> null
            is ConnectionState.WifiProvisioned -> current.session
            is ConnectionState.Syncing -> current.session
            else -> null
        }
        if (session == null) {
            setError(
                "尚未连接到设备，请先扫描并配对",
                DeviceSetupErrorReason.ProvisioningFailed
            )
            return
        }
        viewModelScope.launch {
            val result = connectionManager.startPairing(
                peripheral = BlePeripheral(
                    id = session.peripheralId,
                    name = session.peripheralName,
                    signalStrengthDbm = session.signalStrengthDbm,
                    profileId = session.profileId
                ),
                credentials = WifiCredentials(ssid = ssid, password = password)
            )
            if (result is Result.Error) {
                setError(
                    "Wi-Fi 配置失败，请检查网络和密码后重试",
                    DeviceSetupErrorReason.ProvisioningFailed
                )
            } else {
                updateUi { it.copy(isSubmittingWifi = true, step = DeviceSetupStep.WifiProvisioning) }
                triggerNetworkCheck(force = true)
            }
        }
    }

    fun onDismissError() {
        updateUi { it.copy(errorMessage = null) }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            connectionManager.state.collectLatest { state ->
                if (_uiState.value.step == DeviceSetupStep.Error || _uiState.value.step == DeviceSetupStep.Ready) {
                    if (state is ConnectionState.Error) {
                        setError(
                            message = toReadableError(state.error),
                            reason = DeviceSetupErrorReason.Unknown
                        )
                    }
                    return@collectLatest
                }
                when (state) {
                    ConnectionState.Disconnected -> {
                        if (_uiState.value.step == DeviceSetupStep.Found || _uiState.value.step == DeviceSetupStep.WifiInput) {
                            // 保留“发现设备/填写 Wi-Fi”界面，不回落到 Idle
                            return@collectLatest
                        }
                        updateUi {
                            it.copy(
                                step = if (hasStarted) DeviceSetupStep.Scanning else DeviceSetupStep.Idle,
                                isActionInProgress = hasStarted,
                                isScanning = hasStarted,
                                isSubmittingWifi = false,
                                isDeviceOnline = false,
                                deviceIp = null,
                                errorMessage = null,
                                errorReason = null
                            )
                        }
                    }

                    is ConnectionState.Pairing -> {
                        cancelScanTimeout()
                        updateUi {
                            it.copy(
                                step = DeviceSetupStep.Pairing,
                                deviceName = state.deviceName,
                                isActionInProgress = true,
                                isScanning = false
                            )
                        }
                    }

                    is ConnectionState.Connected -> {
                        cancelScanTimeout()
                        updateUi {
                            it.copy(
                                step = DeviceSetupStep.WifiInput,
                                deviceName = state.session.peripheralName,
                                isActionInProgress = false,
                                isScanning = false,
                                showWifiForm = true
                            )
                        }
                    }

                    is ConnectionState.WifiProvisioned,
                    is ConnectionState.Syncing -> {
                        cancelScanTimeout()
                        updateUi { current ->
                            if (current.step == DeviceSetupStep.Ready) current else current.copy(
                                step = DeviceSetupStep.WaitingForDeviceOnline,
                                isActionInProgress = true,
                                isSubmittingWifi = true
                            )
                        }
                        triggerNetworkCheck()
                    }

                    is ConnectionState.Error -> {
                        val message = toReadableError(state.error)
                        val reason = when (state.error) {
                            is ConnectivityError.Timeout -> DeviceSetupErrorReason.ScanTimeout
                            is ConnectivityError.ProvisioningFailed -> DeviceSetupErrorReason.ProvisioningFailed
                            else -> DeviceSetupErrorReason.Unknown
                        }
                        setError(message, reason)
                    }
                }
            }
        }
    }

    private fun observeScanner() {
        viewModelScope.launch {
            bleScanner.devices.collectLatest { devices ->
                if (devices.isEmpty()) return@collectLatest
                if (_uiState.value.step == DeviceSetupStep.Error || _uiState.value.step == DeviceSetupStep.Ready) {
                    return@collectLatest
                }
                val currentConnection = connectionManager.state.value
                if (currentConnection !is ConnectionState.Disconnected) {
                    updateUi { it.copy(isScanning = false, isActionInProgress = false) }
                    return@collectLatest
                }
                cancelScanTimeout()
                updateUi { it.copy(isScanning = false, isActionInProgress = false) }
                pendingPeripheral = devices.first()
                updateUi {
                    it.copy(
                        step = DeviceSetupStep.Found,
                        deviceName = pendingPeripheral?.name,
                        isActionInProgress = false,
                        isScanning = false,
                        showWifiForm = false
                    )
                }
            }
        }
        viewModelScope.launch {
            bleScanner.isScanning.collectLatest { scanning ->
                updateUi { it.copy(isScanning = scanning) }
            }
        }
    }

    private fun triggerNetworkCheck(force: Boolean = false) {
        networkCheckJob?.cancel()
        // 若已在线且有 IP，保持 Ready 不再重复轮询，除非显式 force
        if (!force && _uiState.value.isDeviceOnline && _uiState.value.deviceIp != null) return
        networkCheckJob = viewModelScope.launch {
            repeat(NETWORK_CHECK_ATTEMPTS) {
                when (val result = connectionManager.queryNetworkStatus()) {
                    is Result.Success -> {
                        setReady(result.data.ipAddress)
                        return@launch
                    }

                    is Result.Error -> {
                        // 等待下一次重试
                    }
                }
                delay(NETWORK_CHECK_INTERVAL_MS)
            }
            setError(
                message = "设备尚未上线，请检查 Wi-Fi 并重试",
                reason = DeviceSetupErrorReason.DeviceNotOnline
            )
        }
    }

    private fun setReady(ip: String?) {
        val sanitizedIp = ip?.takeIf { it.isNotBlank() }
        updateUi { current ->
            if (current.step == DeviceSetupStep.Ready && current.deviceIp != null) {
                current
            } else {
                current.copy(
                    step = DeviceSetupStep.Ready,
                    isActionInProgress = false,
                    isSubmittingWifi = false,
                    isScanning = false,
                    isDeviceOnline = true,
                    deviceIp = sanitizedIp ?: current.deviceIp
                )
            }
        }
        networkCheckJob?.cancel()
        networkCheckJob = null
        cancelScanTimeout()
        if (!hasEmittedReadyEvent) {
            hasEmittedReadyEvent = true
            viewModelScope.launch { _events.emit(DeviceSetupEvent.OpenDeviceManager) }
        }
    }

    private fun setError(message: String, reason: DeviceSetupErrorReason = DeviceSetupErrorReason.Unknown) {
        bleScanner.stop()
        cancelScanTimeout()
        updateUi {
            it.copy(
                step = DeviceSetupStep.Error,
                errorMessage = message,
                errorReason = reason,
                isActionInProgress = false,
                isScanning = false,
                isSubmittingWifi = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkCheckJob?.cancel()
        _events.resetReplayCache()
        bleScanner.stop()
        cancelScanTimeout()
    }

    companion object {
        private const val NETWORK_CHECK_INTERVAL_MS = 1_500L
        private const val NETWORK_CHECK_ATTEMPTS = 3
        private const val SCAN_TIMEOUT_MS = 12_000L
    }

    private fun toReadableError(error: ConnectivityError): String = when (error) {
        is ConnectivityError.Timeout -> "未找到设备，请确认设备已开启并靠近手机"
        is ConnectivityError.ProvisioningFailed -> error.reason
        is ConnectivityError.PermissionDenied -> "缺少权限：${error.permissions.joinToString()}"
        is ConnectivityError.Transport -> error.reason
        ConnectivityError.MissingSession -> "尚未建立会话，请重新扫描设备"
        is ConnectivityError.PairingInProgress -> "配对冲突：${error.deviceName} 已在使用"
    }

    private fun updateUi(transform: (DeviceSetupUiState) -> DeviceSetupUiState) {
        _uiState.update { transform(it).enrich() }
    }

    private fun startScanTimeout() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = viewModelScope.launch {
            delay(SCAN_TIMEOUT_MS)
            if (_uiState.value.step == DeviceSetupStep.Scanning && _uiState.value.isScanning) {
                bleScanner.stop()
                setError(
                    message = "扫描超时，请重试配网",
                    reason = DeviceSetupErrorReason.ScanTimeout
                )
            }
        }
    }

    private fun cancelScanTimeout() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
    }

    private fun DeviceSetupUiState.enrich(): DeviceSetupUiState {
        val (desc, primary, secondary, showWifi, enabled) = when (step) {
            DeviceSetupStep.Idle,
            DeviceSetupStep.Scanning -> Tuple5(
                "正在搜索设备…",
                "正在搜索…",
                "返回首页",
                false,
                false
            )

            DeviceSetupStep.Found -> Tuple5(
                "发现新设备，继续配置网络。",
                "配置网络",
                "返回首页",
                false,
                true
            )

            DeviceSetupStep.WifiInput -> Tuple5(
                "连接 Wi-Fi，确保与手机在同一网络。",
                "连接设备",
                "返回首页",
                true,
                wifiSsid.isNotBlank() && wifiPassword.isNotBlank()
            )

            DeviceSetupStep.Pairing,
            DeviceSetupStep.WifiProvisioning,
            DeviceSetupStep.WaitingForDeviceOnline -> Tuple5(
                "正在配对并同步配置，请稍候…",
                "正在连接…",
                "返回首页",
                false,
                false
            )

            DeviceSetupStep.Ready -> Tuple5(
                "连接成功，正在跳转到设备管理。",
                "进入设备管理",
                "返回首页",
                false,
                true
            )

            DeviceSetupStep.Error -> Tuple5(
                errorMessage ?: "配网失败，请重试",
                "重试配网",
                "返回首页",
                false,
                true
            )
        }
        return copy(
            title = "设备配网",
            description = desc,
            primaryLabel = primary,
            secondaryLabel = secondary,
            showWifiForm = showWifi,
            isPrimaryEnabled = enabled
        )
    }

    private data class Tuple5<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )
}

sealed class DeviceSetupEvent {
    data object OpenDeviceManager : DeviceSetupEvent()
    data object BackToHome : DeviceSetupEvent()
}
