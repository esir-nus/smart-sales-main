package com.smartsales.feature.connectivity.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.connectivity.scan.BleScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/setup/DeviceSetupViewModel.kt
// 模块：:feature:connectivity
// 说明：管理 BLE 扫描 → Wi-Fi 配网 → 验证网络的状态机
// 作者：创建于 2025-11-20
@HiltViewModel
class DeviceSetupViewModel @Inject constructor(
    private val connectionManager: DeviceConnectionManager,
    private val bleScanner: BleScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceSetupUiState())
    val uiState: StateFlow<DeviceSetupUiState> = _uiState.asStateFlow()

    private var latestPeripherals: List<BlePeripheral> = emptyList()

    init {
        observeScanner()
        observeConnection()
    }

    override fun onCleared() {
        super.onCleared()
        bleScanner.stop()
    }

    fun onStartScan() {
        bleScanner.start()
        _uiState.update {
            it.copy(
                step = DeviceSetupStep.SCAN,
                isScanning = true,
                devices = emptyList(),
                selectedDeviceId = null,
                errorMessage = null
            )
        }
    }

    fun onSelectDevice(deviceId: String) {
        val peripheral = latestPeripherals.firstOrNull { it.id == deviceId } ?: run {
            _uiState.update { it.copy(errorMessage = "找不到该设备，请重新扫描") }
            return
        }
        connectionManager.selectPeripheral(peripheral)
        _uiState.update {
            it.copy(
                selectedDeviceId = deviceId,
                devices = latestPeripherals.toUiModels(deviceId),
                step = DeviceSetupStep.ENTER_WIFI,
                errorMessage = null
            )
        }
    }

    fun onWifiSsidChanged(value: String) {
        _uiState.update { it.copy(wifiSsid = value, errorMessage = null) }
    }

    fun onWifiPasswordChanged(value: String) {
        _uiState.update { it.copy(wifiPassword = value, errorMessage = null) }
    }

    fun onSubmitWifiCredentials() {
        val state = _uiState.value
        val device = latestPeripherals.firstOrNull { it.id == state.selectedDeviceId }
        if (device == null) {
            _uiState.update { it.copy(errorMessage = "请先选择需要配网的设备") }
            return
        }
        val ssid = state.wifiSsid.trim()
        val password = state.wifiPassword.trim()
        if (ssid.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "请输入 Wi-Fi 名称") }
            return
        }
        if (password.length < 8) {
            _uiState.update { it.copy(errorMessage = "Wi-Fi 密码至少 8 位") }
            return
        }
        val credentials = WifiCredentials(ssid = ssid, password = password)
        viewModelScope.launch {
            _uiState.update { it.copy(step = DeviceSetupStep.PROVISIONING, isSubmitting = true, errorMessage = null) }
            when (val result = connectionManager.startPairing(device, credentials)) {
                is Result.Success -> Unit
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            step = DeviceSetupStep.FAILED,
                            isSubmitting = false,
                            errorMessage = result.throwable.message ?: "配网失败"
                        )
                    }
                }
            }
        }
    }

    fun onRetry() {
        bleScanner.start()
        _uiState.update {
            it.copy(
                step = if (it.selectedDeviceId == null) DeviceSetupStep.SCAN else DeviceSetupStep.ENTER_WIFI,
                isSubmitting = false,
                errorMessage = null
            )
        }
    }

    private fun observeScanner() {
        viewModelScope.launch {
            bleScanner.devices.collectLatest { devices ->
                latestPeripherals = devices
                _uiState.update { state ->
                    val nextStep = when {
                        devices.isNotEmpty() && state.step == DeviceSetupStep.SCAN -> DeviceSetupStep.SELECT_DEVICE
                        else -> state.step
                    }
                    state.copy(
                        devices = devices.toUiModels(state.selectedDeviceId),
                        step = nextStep
                    )
                }
            }
        }
        viewModelScope.launch {
            bleScanner.isScanning.collectLatest { scanning ->
                _uiState.update { it.copy(isScanning = scanning) }
            }
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            connectionManager.state.collectLatest { state ->
                when (state) {
                    is ConnectionState.Pairing ->
                        _uiState.update { it.copy(step = DeviceSetupStep.PROVISIONING, isSubmitting = true) }

                    is ConnectionState.WifiProvisioned -> {
                        _uiState.update { it.copy(step = DeviceSetupStep.VERIFYING) }
                        verifyNetwork()
                    }

                    is ConnectionState.Syncing ->
                        _uiState.update { it.copy(step = DeviceSetupStep.COMPLETED, isSubmitting = false, errorMessage = null) }

                    is ConnectionState.Error ->
                        _uiState.update {
                            it.copy(
                                step = DeviceSetupStep.FAILED,
                                isSubmitting = false,
                                errorMessage = state.error.describe()
                            )
                        }

                    ConnectionState.Disconnected -> Unit
                }
            }
        }
    }

    private fun verifyNetwork() {
        viewModelScope.launch {
            when (val result = connectionManager.queryNetworkStatus()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            step = DeviceSetupStep.COMPLETED,
                            isSubmitting = false,
                            errorMessage = null,
                            networkStatus = result.data
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            step = DeviceSetupStep.FAILED,
                            isSubmitting = false,
                            errorMessage = result.throwable.message ?: "网络验证失败"
                        )
                    }
                }
            }
        }
    }

    private fun List<BlePeripheral>.toUiModels(selectedId: String?): List<DeviceSetupDeviceUi> =
        map {
            DeviceSetupDeviceUi(
                id = it.id,
                name = it.name.ifBlank { "未知设备" },
                signalStrengthDbm = it.signalStrengthDbm,
                isSelected = it.id == selectedId
            )
        }

    private fun ConnectivityError.describe(): String = when (this) {
        is ConnectivityError.PairingInProgress -> "设备正在配网，请稍候"
        is ConnectivityError.ProvisioningFailed -> reason
        is ConnectivityError.PermissionDenied -> "缺少权限：${permissions.joinToString()}"
        is ConnectivityError.Timeout -> "操作超时(${timeoutMillis}ms)"
        is ConnectivityError.Transport -> reason
        ConnectivityError.MissingSession -> "当前没有活跃的设备会话"
    }

    private fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
        value = transform(value)
    }
}

data class DeviceSetupUiState(
    val step: DeviceSetupStep = DeviceSetupStep.SCAN,
    val devices: List<DeviceSetupDeviceUi> = emptyList(),
    val selectedDeviceId: String? = null,
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val isScanning: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val networkStatus: DeviceNetworkStatus? = null
)

data class DeviceSetupDeviceUi(
    val id: String,
    val name: String,
    val signalStrengthDbm: Int,
    val isSelected: Boolean
)

enum class DeviceSetupStep {
    SCAN,
    SELECT_DEVICE,
    ENTER_WIFI,
    PROVISIONING,
    VERIFYING,
    COMPLETED,
    FAILED
}
