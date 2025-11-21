package com.smartsales.aitest

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleProfileConfig
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.connectivity.scan.BleScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 文件：aiFeatureTestApp/src/main/java/com/smartsales/aitest/ConnectivityControlViewModel.kt
// 模块：:aiFeatureTestApp
// 说明：测试 App 使用的 BLE 扫描与 Wi-Fi 凭据发送面板状态
// 作者：创建于 2025-11-18
private const val DEFAULT_MEDIA_SERVER_BASE_URL = "http://10.0.2.2:8000"
private const val DEFAULT_SERVICE_PORT = "8000"

@Immutable
data class PeripheralUiModel(
    val id: String,
    val name: String,
    val signalStrengthDbm: Int,
    val isSelected: Boolean
)

@Immutable
data class ConnectivityControlState(
    val isScanning: Boolean = false,
    val peripherals: List<PeripheralUiModel> = emptyList(),
    val selectedPeripheralId: String? = null,
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: WifiCredentials.Security = WifiCredentials.Security.WPA2,
    val isTransferring: Boolean = false,
    val transferStatus: String? = null,
    val errorMessage: String? = null,
    val hotspotSsid: String? = null,
    val hotspotPassword: String? = null,
    val connectionSummary: String? = null,
    val httpIp: String? = null,
    val deviceWifiName: String? = null,
    val phoneWifiName: String? = null,
    val matchingStatus: String? = null,
    val networkMatchMessage: String? = null,
    val networkMatched: Boolean? = null,
    val consoleStatusMessage: String? = null,
    val mediaServerBaseUrl: String = DEFAULT_MEDIA_SERVER_BASE_URL,
    val bleLogs: List<String> = emptyList()
) {
    val canSendCredentials: Boolean
        get() = !isTransferring &&
            !wifiSsid.isBlank() &&
            wifiPassword.length >= 8 &&
            selectedPeripheralId != null
}

@HiltViewModel
class ConnectivityControlViewModel @Inject constructor(
    private val connectionManager: DeviceConnectionManager,
    private val phoneWifiMonitor: PhoneWifiMonitor,
    private val bleScanner: BleScanner,
    private val bleProfiles: List<BleProfileConfig>
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectivityControlState())
    val uiState: StateFlow<ConnectivityControlState> = _uiState.asStateFlow()

    private var discoveredPeripherals: List<BlePeripheral> = emptyList()
    private var scanTimeoutJob: Job? = null
    private val profileSummary: String = bleProfiles.joinToString { it.displayName }
        .ifBlank { "目标设备" }

    init {
        viewModelScope.launch {
            connectionManager.state.collect { state ->
                _uiState.update { it.copy(connectionSummary = state.describe()) }
            }
        }
        viewModelScope.launch {
            phoneWifiMonitor.currentSsid.collect { ssid ->
                _uiState.update { current ->
                    val sanitized = sanitizeSsid(ssid)
                    val candidate = sanitized ?: current.phoneWifiName ?: current.wifiSsid.takeIf { it.isNotBlank() }
                    val match = evaluateNetworkMatch(current.deviceWifiName, candidate)
                    current.copy(
                        phoneWifiName = sanitized ?: current.phoneWifiName,
                        networkMatchMessage = match?.first ?: current.networkMatchMessage,
                        networkMatched = match?.second ?: current.networkMatched
                    )
                }
            }
        }
        viewModelScope.launch {
            bleScanner.devices.collect { devices ->
                discoveredPeripherals = devices
                _uiState.update { state ->
                    state.copy(
                        peripherals = devices.toUiModels(state.selectedPeripheralId),
                        matchingStatus = when {
                            devices.isEmpty() && state.isScanning ->
                                "正在寻找 $profileSummary"
                            devices.isEmpty() -> "未发现 $profileSummary"
                            state.selectedPeripheralId == devices.first().id ->
                                "已连接 ${devices.first().name}"
                            else -> "发现 ${devices.first().name}，点击连接"
                        }
                    )
                }
                if (devices.isNotEmpty()) {
                    stopScanTimeout()
                }
            }
        }
        viewModelScope.launch {
            bleScanner.isScanning.collect { scanning ->
                _uiState.update { it.copy(isScanning = scanning) }
            }
        }
        startScan()
    }

    fun startScan() {
        appendLog("开始扫描 $profileSummary")
        bleScanner.start()
        scanTimeoutJob?.cancel()
        scanTimeoutJob = viewModelScope.launch {
            delay(SCAN_TIMEOUT_MS)
            if (_uiState.value.isScanning) {
                bleScanner.stop()
                _uiState.update { it.copy(matchingStatus = "扫描超时，可重试") }
                appendLog("扫描在 $SCAN_TIMEOUT_MS ms 内未找到设备")
            }
        }
    }

    fun stopScan() {
        bleScanner.stop()
        stopScanTimeout()
        _uiState.update { it.copy(matchingStatus = "扫描已停止") }
        appendLog("停止扫描")
    }

    fun selectPeripheral(peripheralId: String) {
        val target = discoveredPeripherals.firstOrNull { it.id == peripheralId } ?: return
        connectionManager.selectPeripheral(target)
        _uiState.update {
            it.copy(
                selectedPeripheralId = peripheralId,
                peripherals = discoveredPeripherals.toUiModels(peripheralId),
                transferStatus = null,
                errorMessage = null,
                matchingStatus = "已连接 ${target.name}"
            )
        }
        appendLog("已连接 ${target.name}")
    }

    fun updateWifiSsid(value: String) {
        _uiState.update { it.copy(wifiSsid = value, transferStatus = null) }
    }

    fun updateWifiPassword(value: String) {
        _uiState.update { it.copy(wifiPassword = value, transferStatus = null) }
    }

    fun setSecurity(security: WifiCredentials.Security) {
        _uiState.update { it.copy(wifiSecurity = security) }
    }

    fun stopConnection() {
        connectionManager.forgetDevice()
        _uiState.update {
            it.copy(
                selectedPeripheralId = null,
                matchingStatus = "连接已中断",
                peripherals = discoveredPeripherals.toUiModels(null),
                networkMatchMessage = null,
                networkMatched = null
            )
        }
        appendLog("已停止连接")
    }

    fun sendCredentials() {
        val state = _uiState.value
        val peripheral = discoveredPeripherals.firstOrNull { it.id == state.selectedPeripheralId }
        if (peripheral == null) {
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
        viewModelScope.launch {
            _uiState.update { it.copy(isTransferring = true, errorMessage = null) }
            val credentials = WifiCredentials(
                ssid = ssid,
                password = password,
                security = state.wifiSecurity
            )
            when (val result = connectionManager.startPairing(peripheral, credentials)) {
                is Result.Success -> {
                    appendLog("凭据已发送到 ${peripheral.name}：wifi#connect#${credentials.ssid}#${credentials.password}")
                    _uiState.update {
                        it.copy(
                            isTransferring = false,
                            transferStatus = "凭据已发送，等待设备确认",
                            errorMessage = null
                        )
                    }
                }

                is Result.Error -> {
                    appendLog("发送失败：${result.throwable.message}")
                    _uiState.update {
                        it.copy(
                            isTransferring = false,
                            errorMessage = result.throwable.message ?: "发送失败",
                            transferStatus = null
                        )
                    }
                }
            }
        }
    }

    fun requestHotspot() {
        viewModelScope.launch {
            when (val result = connectionManager.requestHotspotCredentials()) {
                is Result.Success -> {
                    appendLog("读取热点：${result.data.ssid}")
                    _uiState.update {
                        it.copy(
                            hotspotSsid = result.data.ssid,
                            hotspotPassword = result.data.password,
                            transferStatus = "已读取热点信息",
                            errorMessage = null
                        )
                    }
                }

                is Result.Error -> {
                    appendLog("读取热点失败：${result.throwable.message}")
                    _uiState.update {
                        it.copy(
                            errorMessage = result.throwable.message ?: "无法读取热点信息",
                            transferStatus = null
                        )
                    }
                }
            }
        }
    }

    fun queryNetworkStatus() {
        viewModelScope.launch {
            appendLog("查询设备网络状态：wifi#address#ip#name")
            _uiState.update {
                it.copy(
                    transferStatus = "正在查询网络...",
                    errorMessage = null
                )
            }
            when (val result = connectionManager.queryNetworkStatus()) {
                is Result.Success -> {
                    val status = result.data
                    val sanitizedDevice = sanitizeSsid(status.deviceWifiName)
                    val fallbackPhone = _uiState.value.phoneWifiName
                        ?: sanitizeSsid(status.phoneWifiName)
                        ?: _uiState.value.wifiSsid.takeIf { it.isNotBlank() }
                    val match = evaluateNetworkMatch(sanitizedDevice ?: status.deviceWifiName, fallbackPhone)
                    val newBase = buildBaseUrl(status.ipAddress, _uiState.value.mediaServerBaseUrl)
                    _uiState.update {
                        it.copy(
                            httpIp = status.ipAddress,
                            deviceWifiName = sanitizedDevice ?: status.deviceWifiName,
                            phoneWifiName = fallbackPhone ?: it.phoneWifiName,
                            matchingStatus = "已同步网络：${sanitizedDevice ?: status.deviceWifiName}",
                            transferStatus = null,
                            mediaServerBaseUrl = newBase,
                            networkMatchMessage = match?.first,
                            networkMatched = match?.second
                        )
                    }
                    appendLog("网络信息：${status.ipAddress} / ${sanitizedDevice ?: status.deviceWifiName}")
                }

                is Result.Error -> {
                    val message = result.throwable.message ?: "查询网络失败"
                    _uiState.update {
                        it.copy(
                            errorMessage = message,
                            transferStatus = null,
                            networkMatchMessage = null,
                            networkMatched = null
                        )
                    }
                    appendLog("查询网络失败：$message")
                }
            }
        }
    }

    fun updateMediaServerBaseUrl(raw: String) {
        val currentBase = _uiState.value.mediaServerBaseUrl
        val normalized = normalizeBaseUrl(raw, extractPort(currentBase))
        _uiState.update {
            it.copy(
                mediaServerBaseUrl = normalized,
                consoleStatusMessage = "服务地址已更新：$normalized"
            )
        }
    }

    fun openWebConsole() {
        val base = _uiState.value.mediaServerBaseUrl
        appendLog("尝试打开 Web 控制台 $base")
        _uiState.update { it.copy(consoleStatusMessage = "已尝试打开 Web 控制台：$base") }
    }

    fun openMediaManager() {
        val base = _uiState.value.mediaServerBaseUrl
        appendLog("打开媒体管理器 $base")
        _uiState.update { it.copy(consoleStatusMessage = "媒体管理入口已触发：$base") }
    }

    override fun onCleared() {
        super.onCleared()
        bleScanner.stop()
        stopScanTimeout()
    }

    private fun stopScanTimeout() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
    }

    private fun Collection<BlePeripheral>.toUiModels(selectedId: String?): List<PeripheralUiModel> =
        map {
            PeripheralUiModel(
                id = it.id,
                name = it.name,
                signalStrengthDbm = it.signalStrengthDbm,
                isSelected = it.id == selectedId
            )
        }

    private fun ConnectionState.describe(): String = when (this) {
        ConnectionState.Disconnected -> "未连接"
        is ConnectionState.Connected -> "已连接 ${session.peripheralName}"
        is ConnectionState.Pairing -> "配对 ${deviceName} · 进度 ${progressPercent}%"
        is ConnectionState.WifiProvisioned -> "已同步 Wi-Fi：${status.wifiSsid}"
        is ConnectionState.Syncing -> "同步中 · 心跳 ${lastHeartbeatAtMillis}"
        is ConnectionState.Error -> "错误：${error.javaClass.simpleName}"
    }

    private fun sanitizeSsid(input: String?): String? =
        input?.trim()?.trim('"')?.ifBlank { null }

    private fun evaluateNetworkMatch(
        deviceWifi: String?,
        phoneWifi: String?
    ): Pair<String, Boolean>? {
        val device = sanitizeSsid(deviceWifi)
        val phone = sanitizeSsid(phoneWifi)
        if (device.isNullOrBlank() || phone.isNullOrBlank()) return null
        val matched = device.equals(phone, ignoreCase = true)
        val message = if (matched) "网络匹配" else "网络不匹配，请确认在同一网络"
        return message to matched
    }

    private fun extractPort(baseUrl: String): String =
        runCatching {
            val uri = Uri.parse(baseUrl)
            if (uri.port == -1) DEFAULT_SERVICE_PORT else uri.port.toString()
        }.getOrDefault(DEFAULT_SERVICE_PORT)

    private fun normalizeBaseUrl(
        raw: String,
        fallbackPort: String = DEFAULT_SERVICE_PORT
    ): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return DEFAULT_MEDIA_SERVER_BASE_URL
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return runCatching {
            val uri = Uri.parse(withScheme)
            val scheme = uri.scheme ?: "http"
            val host = uri.host ?: uri.toString().removePrefix("$scheme://").substringBefore("/")
            val port = if (uri.port == -1) fallbackPort else uri.port.toString()
            "$scheme://$host:$port"
        }.getOrDefault(DEFAULT_MEDIA_SERVER_BASE_URL)
    }

    private fun buildBaseUrl(host: String?, currentBase: String): String {
        val sanitizedHost = host
            ?.removePrefix("http://")
            ?.removePrefix("https://")
            ?.substringBefore("/")
            ?.ifBlank { null } ?: return currentBase
        val port = extractPort(currentBase)
        return "http://$sanitizedHost:$port"
    }

    private fun appendLog(line: String) {
        _uiState.update {
            it.copy(
                bleLogs = (listOf("[${System.currentTimeMillis() % 100000}] $line") + it.bleLogs)
                    .take(20)
            )
        }
    }

    private companion object {
        private const val SCAN_TIMEOUT_MS = 12_000L
    }
}
