package com.smartsales.prism.data.pairing

import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.ConnectionState
import com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.WifiCredentials as LegacyWifiCredentials
import com.smartsales.prism.data.connectivity.legacy.scan.BleScanner
import com.smartsales.prism.domain.pairing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实配对服务实现
 * 
 * 封装 legacy BleScanner + DeviceConnectionManager 逻辑
 * 简化 9-state 状态机为 6-state 消费者模型
 */
@Singleton
class RealPairingService @Inject constructor(
    private val bleScanner: BleScanner,
    private val connectionManager: DeviceConnectionManager
) : PairingService {
    
    private val _state = MutableStateFlow<PairingState>(PairingState.Idle)
    override val state: StateFlow<PairingState> = _state.asStateFlow()
    
    private var scanJob: Job? = null
    private var scanTimeoutJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val discoveredPeripherals = mutableMapOf<String, BlePeripheral>()
    
    companion object {
        private const val TAG = "PairingService"
        private const val SCAN_TIMEOUT_MS = 12_000L
        private const val NETWORK_CHECK_RETRY_COUNT = 3
        private const val NETWORK_CHECK_RETRY_DELAY_MS = 1_500L
    }
    
    override suspend fun startScan() {
        Log.d(TAG, "startScan() called")
        
        // 重置状态
        cancelPairing()
        discoveredPeripherals.clear()
        _state.value = PairingState.Scanning
        Log.d(TAG, "State → Scanning")
        
        // 检查蓝牙是否启用
        Log.d(TAG, "isBluetoothEnabled = ${bleScanner.isBluetoothEnabled}")
        if (!bleScanner.isBluetoothEnabled) {
            Log.w(TAG, "Bluetooth disabled, aborting scan")
            _state.value = PairingState.Error(
                message = "请先开启蓝牙",
                reason = ErrorReason.PERMISSION_DENIED,
                canRetry = true
            )
            return
        }
        
        // 启动扫描
        Log.d(TAG, "Calling bleScanner.start()")
        bleScanner.start()
        Log.d(TAG, "bleScanner.start() completed, isScanning = ${bleScanner.isScanning.value}")
        
        // 启动超时计时器
        scanTimeoutJob = scope.launch {
            Log.d(TAG, "Timeout timer started (${SCAN_TIMEOUT_MS}ms)")
            delay(SCAN_TIMEOUT_MS)
            if (_state.value is PairingState.Scanning) {
                Log.w(TAG, "Scan timeout! No device found in ${SCAN_TIMEOUT_MS}ms")
                bleScanner.stop()
                _state.value = PairingState.Error(
                    message = "未发现设备，请检查设备电源与距离",
                    reason = ErrorReason.SCAN_TIMEOUT,
                    canRetry = true
                )
            }
        }
        
        // 监听扫描结果
        scanJob = scope.launch {
            Log.d(TAG, "Starting to collect bleScanner.devices flow")
            bleScanner.devices.collectLatest { devices ->
                replaceDiscoveredPeripherals(devices)
                Log.d(TAG, "devices flow emitted: ${devices.size} devices")
                if (devices.isNotEmpty() && _state.value is PairingState.Scanning) {
                    val first = devices.first()
                    Log.i(TAG, "Device found! id=${first.id}, name=${first.name}, rssi=${first.signalStrengthDbm}")
                    scanTimeoutJob?.cancel()
                    bleScanner.stop()
                    _state.value = PairingState.DeviceFound(
                        badge = DiscoveredBadge(
                            id = first.id,
                            name = first.name,
                            signalStrengthDbm = first.signalStrengthDbm
                        )
                    )
                    Log.d(TAG, "State → DeviceFound")
                }
            }
        }
    }
    
    override suspend fun pairBadge(
        badge: DiscoveredBadge,
        wifiCreds: WifiCredentials
    ): PairingResult {
        Log.d(TAG, "pairBadge() called for ${badge.name} with SSID=${wifiCreds.ssid}")
        val peripheral = discoveredPeripherals[badge.id]
        if (peripheral == null) {
            Log.w(TAG, "pairBadge() missing peripheral for badgeId=${badge.id}")
            val error = PairingState.Error(
                message = "设备已不可用，请重新扫描",
                reason = ErrorReason.DEVICE_NOT_FOUND,
                canRetry = true
            )
            _state.value = error
            return PairingResult.Error(error.message, error.reason)
        }

        // 阶段 1：选择设备 (10%)
        Log.d(TAG, "State → Pairing(10%) - Selecting peripheral")
        _state.value = PairingState.Pairing(progress = 10)
        connectionManager.selectPeripheral(peripheral)

        // 阶段 2：发送 WiFi 配网 (40%)
        Log.d(TAG, "State → Pairing(40%) - Starting WiFi provisioning")
        _state.value = PairingState.Pairing(progress = 40)
        val pairingResult = connectionManager.startPairing(
            peripheral = peripheral,
            credentials = LegacyWifiCredentials(
                ssid = wifiCreds.ssid,
                password = wifiCreds.password
            )
        )
        
        if (pairingResult is Result.Error) {
            Log.e(TAG, "startPairing returned error: ${pairingResult.throwable}")
            val reason = pairingResult.throwable.message?.takeIf { it.isNotBlank() } ?: "请检查网络和密码"
            val error = PairingState.Error(
                message = "WiFi 配网失败: $reason",
                reason = ErrorReason.WIFI_PROVISIONING_FAILED,
                canRetry = true
            )
            _state.value = error
            return PairingResult.Error(error.message, error.reason)
        }
        
        Log.d(TAG, "startPairing returned success, now waiting for WifiProvisioned state...")
        
        // 阶段 3：等待 legacy 状态机完成 provisioning (70%)
        _state.value = PairingState.Pairing(progress = 70)
        
        // 观察 legacy DeviceConnectionManager.state 等待完成
        val provisionResult = withTimeoutOrNull(30_000L) {
            connectionManager.state.first { state ->
                Log.d(TAG, "Observing legacy state: $state")
                when (state) {
                    is ConnectionState.WifiProvisioned,
                    is ConnectionState.Connected,
                    is ConnectionState.Syncing -> true
                    is ConnectionState.Error -> true
                    else -> false
                }
            }
        }
        
        Log.d(TAG, "Provision observation result: $provisionResult")
        
        when (provisionResult) {
            is ConnectionState.WifiProvisioned,
            is ConnectionState.Connected,
            is ConnectionState.Syncing -> {
                // 成功
                Log.d(TAG, "WiFi provisioning successful!")
            }
            is ConnectionState.Error -> {
                Log.e(TAG, "Provisioning failed with error: ${provisionResult.error}")
                val error = PairingState.Error(
                    message = "WiFi 配网失败: ${provisionResult.error}",
                    reason = ErrorReason.WIFI_PROVISIONING_FAILED,
                    canRetry = true
                )
                _state.value = error
                return PairingResult.Error(error.message, error.reason)
            }
            null -> {
                Log.e(TAG, "Provisioning timed out after 30s")
                val error = PairingState.Error(
                    message = "配网超时，请重试",
                    reason = ErrorReason.WIFI_PROVISIONING_FAILED,
                    canRetry = true
                )
                _state.value = error
                return PairingResult.Error(error.message, error.reason)
            }
            else -> {
                Log.e(TAG, "Unexpected state: $provisionResult")
                val error = PairingState.Error(
                    message = "配网失败，请重试",
                    reason = ErrorReason.WIFI_PROVISIONING_FAILED,
                    canRetry = true
                )
                _state.value = error
                return PairingResult.Error(error.message, error.reason)
            }
        }
        
        // 阶段 4：验证网络 (85%)
        _state.value = PairingState.Pairing(progress = 85)
        
        // 轮询网络状态
        var networkOk = false
        repeat(NETWORK_CHECK_RETRY_COUNT) { attempt ->
            Log.d(TAG, "Network check attempt ${attempt + 1}/${NETWORK_CHECK_RETRY_COUNT}")
            val result = runCatching { connectionManager.queryNetworkStatus() }
                .getOrElse { Result.Error(Exception("查询失败: ${it.message}")) }
            
            if (result is Result.Success && !result.data.ipAddress.isNullOrBlank()) {
                Log.d(TAG, "Network check success: IP=${result.data.ipAddress}")
                networkOk = true
                return@repeat
            }
            
            if (attempt < NETWORK_CHECK_RETRY_COUNT - 1) {
                delay(NETWORK_CHECK_RETRY_DELAY_MS)
            }
        }
        
        if (!networkOk) {
            Log.w(TAG, "Network check failed after $NETWORK_CHECK_RETRY_COUNT attempts")
            val error = PairingState.Error(
                message = "设备尚未上线，请检查 WiFi",
                reason = ErrorReason.NETWORK_CHECK_FAILED,
                canRetry = true
            )
            _state.value = error
            return PairingResult.Error(error.message, error.reason)
        }
        
        // 阶段 4：成功 (100%)
        _state.value = PairingState.Success(
            badgeId = badge.id,
            badgeName = badge.name
        )
        
        return PairingResult.Success(badge.id)
    }
    
    override fun cancelPairing() {
        scanJob?.cancel()
        scanTimeoutJob?.cancel()
        bleScanner.stop()
        discoveredPeripherals.clear()
        _state.value = PairingState.Idle
    }

    private fun replaceDiscoveredPeripherals(devices: List<BlePeripheral>) {
        discoveredPeripherals.clear()
        devices.forEach { peripheral ->
            discoveredPeripherals[peripheral.id] = peripheral
        }
    }
    
    /**
     * 映射 legacy ConnectionState 到 PairingState
     * 用于监听来自 DeviceConnectionManager 的状态变化
     */
    private fun mapLegacyState(legacy: ConnectionState): PairingState {
        return when (legacy) {
            is ConnectionState.NeedsSetup -> PairingState.Error(
                message = "请先完成初次配对",
                reason = ErrorReason.NEED_INITIAL_PAIRING,
                canRetry = true
            )
            is ConnectionState.Disconnected -> PairingState.Idle
            is ConnectionState.Pairing -> PairingState.Pairing(progress = 30)
            is ConnectionState.AutoReconnecting -> PairingState.Pairing(progress = 50)
            is ConnectionState.Connected -> PairingState.Pairing(progress = 60)
            is ConnectionState.WifiProvisioned -> PairingState.Pairing(progress = 80)
            is ConnectionState.Syncing -> PairingState.Pairing(progress = 90)
            is ConnectionState.Error -> PairingState.Error(
                message = legacy.error.toString(),
                reason = ErrorReason.UNKNOWN,
                canRetry = true
            )
        }
    }
}
