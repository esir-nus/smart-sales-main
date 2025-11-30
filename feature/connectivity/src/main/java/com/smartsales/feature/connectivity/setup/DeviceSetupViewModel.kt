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

    init {
        observeConnection()
        observeScanner()
        updateUi { it }
    }

    fun onWifiSsidChanged(value: String) {
        updateUi { it.copy(wifiSsid = value) }
    }

    fun onWifiPasswordChanged(value: String) {
        updateUi { it.copy(wifiPassword = value) }
    }

    fun onStartScan() {
        val current = _uiState.value
        if (current.isScanning || current.step == DeviceSetupStep.WaitingForDeviceOnline) return
        bleScanner.start()
        updateUi {
            it.copy(
                step = DeviceSetupStep.Scanning,
                errorMessage = null,
                errorReason = null,
                isActionInProgress = true,
                isScanning = true,
                showWifiForm = false,
                wifiPassword = "",
                wifiSsid = ""
            )
        }
        scanTimeoutJob?.cancel()
        scanTimeoutJob = viewModelScope.launch {
            delay(SCAN_TIMEOUT_MS)
            if (_uiState.value.isScanning && _uiState.value.step == DeviceSetupStep.Scanning) {
                bleScanner.stop()
                setError(
                    message = "扫描超时，请重试配网",
                    reason = DeviceSetupErrorReason.ScanTimeout
                )
            }
        }
    }

    fun onProvisionWifi(ssid: String, password: String) {
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

    fun onRetry() {
        bleScanner.stop()
        updateUi {
            it.copy(
                step = DeviceSetupStep.Scanning,
                errorMessage = null,
                errorReason = null,
                isActionInProgress = true,
                isScanning = false,
                isSubmittingWifi = false,
                wifiPassword = ""
            )
        }
        viewModelScope.launch { connectionManager.retry() }
        onStartScan()
    }

    fun onOpenDeviceManager() {
        viewModelScope.launch { _events.emit(DeviceSetupEvent.OpenDeviceManager) }
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
                    ConnectionState.Disconnected -> updateUi {
                        it.copy(
                            step = DeviceSetupStep.Idle,
                            isActionInProgress = false,
                            isScanning = false,
                            isSubmittingWifi = false,
                            isDeviceOnline = false,
                            deviceIp = null,
                            errorMessage = null,
                            errorReason = null
                        )
                    }

                    is ConnectionState.Pairing -> updateUi {
                        it.copy(
                            step = DeviceSetupStep.Pairing,
                            deviceName = state.deviceName,
                            isActionInProgress = true,
                            isScanning = false
                        )
                    }

                    is ConnectionState.Connected -> updateUi {
                        it.copy(
                            step = DeviceSetupStep.Pairing,
                            deviceName = state.session.peripheralName,
                            isActionInProgress = false,
                            isScanning = false
                        )
                    }

                    is ConnectionState.WifiProvisioned,
                    is ConnectionState.Syncing -> {
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
                scanTimeoutJob?.cancel()
                updateUi { it.copy(isScanning = false, isActionInProgress = false) }
                val first = devices.first()
                connectionManager.selectPeripheral(first)
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
    }

    private fun setError(message: String, reason: DeviceSetupErrorReason = DeviceSetupErrorReason.Unknown) {
        bleScanner.stop()
        scanTimeoutJob?.cancel()
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
        scanTimeoutJob?.cancel()
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

    private fun DeviceSetupUiState.enrich(): DeviceSetupUiState {
        val (desc, primary, secondary, showWifi, enabled) = when (step) {
            DeviceSetupStep.Idle -> Tuple5(
                "未连接设备，点击开始配网。",
                "开始配网",
                "跳过，稍后再说",
                false,
                true
            )

            DeviceSetupStep.Scanning -> Tuple5(
                "正在扫描附近设备，请保持设备靠近。",
                "扫描中",
                "返回首页",
                false,
                false
            )

            DeviceSetupStep.Pairing -> {
                val name = deviceName?.let { " $it" }.orEmpty()
                Tuple5(
                    "已发现设备$name，填写 Wi-Fi 完成配网。",
                    "下发 Wi-Fi 并继续",
                    "返回首页",
                    true,
                    wifiSsid.isNotBlank() && wifiPassword.isNotBlank() && !isSubmittingWifi && !isActionInProgress
                )
            }

            DeviceSetupStep.WifiProvisioning -> Tuple5(
                "正在为设备配置网络，请稍候…",
                "正在配网",
                "返回首页",
                true,
                false
            )

            DeviceSetupStep.WaitingForDeviceOnline -> Tuple5(
                "等待设备上线，请保持设备通电并靠近路由器。",
                "等待设备上线",
                "返回首页",
                true,
                false
            )

            DeviceSetupStep.Ready -> {
                val ipPart = deviceIp?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                Tuple5(
                    "设备已连接$ipPart，可前往设备管理。",
                    "进入设备管理",
                    "返回首页",
                    false,
                    true
                )
            }

            DeviceSetupStep.Error -> Tuple5(
                errorMessage ?: "配网失败，请重试",
                "重试配网",
                "返回首页",
                false,
                !isActionInProgress
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
}
