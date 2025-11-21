package com.smartsales.feature.connectivity.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.LogTags
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
// 作者：创建于 2025-11-21
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
    }

    fun onStartScan() {
        if (_uiState.value.isScanning) return
        bleScanner.start()
        val target = bleProfiles.joinToString { it.displayName }.ifBlank { "目标设备" }
        _uiState.update {
            it.copy(
                step = DeviceSetupStep.Scanning,
                progressMessage = "正在扫描设备（$target）…",
                errorMessage = null,
                isActionInProgress = true,
                isScanning = true
            )
        }
        scanTimeoutJob?.cancel()
        scanTimeoutJob = viewModelScope.launch {
            delay(SCAN_TIMEOUT_MS)
            if (_uiState.value.isScanning && _uiState.value.step == DeviceSetupStep.Scanning) {
                bleScanner.stop()
                setError("未找到设备，请确认设备已开启并靠近手机")
            }
        }
    }

    fun onPair(peripheral: BlePeripheral) {
        _uiState.update {
            it.copy(
                step = DeviceSetupStep.Pairing,
                progressMessage = "正在配对 ${peripheral.name}…",
                deviceName = peripheral.name,
                errorMessage = null,
                isActionInProgress = true
            )
        }
        viewModelScope.launch {
            val result = connectionManager.startPairing(
                peripheral = peripheral,
                credentials = WifiCredentials(ssid = "", password = "")
            )
            if (result is Result.Error) {
                setError("配对失败，请重试")
            }
        }
    }

    fun onProvisionWifi(ssid: String, password: String) {
        val device = uiState.value.deviceName ?: "设备"
        _uiState.update {
            it.copy(
                step = DeviceSetupStep.WifiProvisioning,
                progressMessage = "正在为 $device 配置 Wi-Fi…",
                wifiSsid = ssid,
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
            setError("尚未连接到设备，请先扫描并配对")
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
                setError("Wi-Fi 配置失败，请检查网络和密码后重试")
            } else {
                _uiState.update { it.copy(isSubmittingWifi = true, step = DeviceSetupStep.WifiProvisioning) }
                triggerNetworkCheck(force = true)
            }
        }
    }

    fun onRetry() {
        _uiState.update {
            it.copy(
                step = DeviceSetupStep.Scanning,
                progressMessage = "正在重新扫描设备…",
                errorMessage = null,
                isActionInProgress = true,
                isScanning = false,
                isSubmittingWifi = false
            )
        }
        bleScanner.stop()
        viewModelScope.launch { connectionManager.retry() }
        onStartScan()
    }

    fun onOpenDeviceManager() {
        viewModelScope.launch { _events.emit(DeviceSetupEvent.OpenDeviceManager) }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            connectionManager.state.collectLatest { state ->
                when (state) {
                    ConnectionState.Disconnected -> _uiState.update {
                        it.copy(
                            step = DeviceSetupStep.Idle,
                            progressMessage = "未连接，点击开始扫描",
                            isActionInProgress = false,
                            isScanning = false,
                            isSubmittingWifi = false,
                            isDeviceOnline = false,
                            deviceIp = null
                        )
                    }

                    is ConnectionState.Pairing -> _uiState.update {
                        it.copy(
                            step = DeviceSetupStep.Pairing,
                            progressMessage = "正在配对 ${state.deviceName} (${state.progressPercent}%)",
                            deviceName = state.deviceName,
                            isActionInProgress = true,
                            isScanning = false
                        )
                    }

                    is ConnectionState.Connected -> _uiState.update {
                        it.copy(
                            step = DeviceSetupStep.Pairing,
                            progressMessage = "已连接 ${state.session.peripheralName}，请配置 Wi-Fi",
                            deviceName = state.session.peripheralName,
                            isActionInProgress = false,
                            isScanning = false
                        )
                    }

                    is ConnectionState.WifiProvisioned,
                    is ConnectionState.Syncing -> {
                        _uiState.update { current ->
                            if (current.step == DeviceSetupStep.Ready) current else current.copy(
                                step = DeviceSetupStep.WaitingForDeviceOnline,
                                progressMessage = "已发送 Wi-Fi，等待设备上线…",
                                isActionInProgress = true,
                                isSubmittingWifi = true
                            )
                        }
                        triggerNetworkCheck()
                    }

                    is ConnectionState.Error -> {
                        val message = when (state.error) {
                            is ConnectivityError.Timeout ->
                                "未找到设备，请确认设备已开启并靠近手机"

                            is ConnectivityError.ProvisioningFailed ->
                                "Wi-Fi 配置失败，请检查网络和密码后重试"

                            else -> "连接出错，请重试"
                        }
                        setError(message)
                    }
                }
            }
        }
    }

    private fun observeScanner() {
        viewModelScope.launch {
            bleScanner.devices.collectLatest { devices ->
                if (devices.isEmpty()) return@collectLatest
                val currentConnection = connectionManager.state.value
                if (currentConnection !is ConnectionState.Disconnected) {
                    _uiState.update { it.copy(isScanning = false, isActionInProgress = false) }
                    return@collectLatest
                }
                scanTimeoutJob?.cancel()
                _uiState.update { it.copy(isScanning = false, isActionInProgress = false) }
                val first = devices.first()
                connectionManager.selectPeripheral(first)
            }
        }
        viewModelScope.launch {
            bleScanner.isScanning.collectLatest { scanning ->
                _uiState.update { it.copy(isScanning = scanning) }
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
            setError("无法连接到设备，请检查设备 Wi-Fi 状态")
        }
    }

    private fun setReady(ip: String?) {
        val sanitizedIp = ip?.takeIf { it.isNotBlank() }
        _uiState.update { current ->
            if (current.step == DeviceSetupStep.Ready && current.deviceIp != null) {
                current
            } else {
                current.copy(
                    step = DeviceSetupStep.Ready,
                    progressMessage = if (sanitizedIp != null) "设备已上线，地址 $sanitizedIp" else "设备已上线",
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

    private fun setError(message: String) {
        bleScanner.stop()
        _uiState.update {
            it.copy(
                step = DeviceSetupStep.Error,
                errorMessage = message,
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
        private const val TAG = "${LogTags.CONNECTIVITY}/DeviceSetup"
    }
}

sealed class DeviceSetupEvent {
    data object OpenDeviceManager : DeviceSetupEvent()
}
