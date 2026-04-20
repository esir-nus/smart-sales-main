package com.smartsales.prism.ui.components.connectivity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManager
import com.smartsales.prism.data.connectivity.registry.DeviceRegistryManager
import com.smartsales.prism.data.connectivity.registry.RegisteredDevice
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.ConnectivityService
import com.smartsales.prism.domain.connectivity.ReconnectResult
import com.smartsales.prism.domain.connectivity.UpdateResult
import com.smartsales.prism.domain.connectivity.WifiConfigResult
import com.smartsales.prism.ui.settings.VoiceVolumePreferenceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Connectivity ViewModel — 管理连接模态框状态
 * 将 ConnectivityService 的异步调用封装为可观察状态流。
 *
 * @see prism-ui-ux-contract.md §1.6.1
 */
@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    private val connectivityService: ConnectivityService,
    private val connectivityBridge: ConnectivityBridge,
    private val registryManager: DeviceRegistryManager,
    private val voiceVolumeStore: VoiceVolumePreferenceStore,
    private val connectionManager: DeviceConnectionManager
) : ViewModel() {

    // 设备注册表 — 多设备管理
    val registeredDevices: StateFlow<List<RegisteredDevice>> = registryManager.registeredDevices
    val activeDevice: StateFlow<RegisteredDevice?> = registryManager.activeDevice

    // 连接状态 — 从真实 ConnectivityBridge 订阅
    val connectionState: StateFlow<ConnectionState> = connectivityBridge.connectionState
        .map { badgeState -> mapToUiState(badgeState) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = mapToUiState(connectivityBridge.connectionState.value)
        )

    // 连接管理界面专用状态 — richer BLE / Wi‑Fi 诊断，但不影响 shell 路由
    private val managerBaseState: StateFlow<ConnectivityManagerState> = connectivityBridge.managerStatus
        .map { managerStatus -> mapToManagerUiState(managerStatus) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = mapToManagerUiState(connectivityBridge.managerStatus.value)
        )

    // 电池电量 (Mock) — Wave 3: 从 Badge 查询真实电量
    private val _batteryLevel = MutableStateFlow(85)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _wifiMismatchSuggestedSsid = MutableStateFlow<String?>(null)
    val wifiMismatchSuggestedSsid: StateFlow<String?> = _wifiMismatchSuggestedSsid.asStateFlow()
    private val _wifiMismatchErrorMessage = MutableStateFlow<String?>(null)
    val wifiMismatchErrorMessage: StateFlow<String?> = _wifiMismatchErrorMessage.asStateFlow()

    // 待更新版本
    private val _pendingVersion = MutableStateFlow<String?>(null)
    val pendingVersion: StateFlow<String?> = _pendingVersion.asStateFlow()

    // 临时 UI 状态覆盖（用于非 Badge 状态的 UI 流程）
    private val _uiOverride = MutableStateFlow<ConnectionState?>(null)
    
    // 最终 UI 状态：优先使用覆盖状态，否则使用真实 Badge 状态
    val effectiveState: StateFlow<ConnectionState> = kotlinx.coroutines.flow.combine(
        connectionState,
        _uiOverride
    ) { realState, override ->
        override ?: realState
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        mapToUiState(connectivityBridge.connectionState.value)
    )

    val managerState: StateFlow<ConnectivityManagerState> = combine(
        managerBaseState,
        _uiOverride
    ) { realState, override ->
        override?.let(::mapOverrideToManagerUiState) ?: realState
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        mapToManagerUiState(connectivityBridge.managerStatus.value)
    )
    
    /**
     * 映射 Badge 连接状态 → UI 状态
     */
    private fun mapToUiState(badgeState: BadgeConnectionState): ConnectionState {
        return when (badgeState) {
            is BadgeConnectionState.Disconnected -> ConnectionState.DISCONNECTED
            is BadgeConnectionState.NeedsSetup -> ConnectionState.NEEDS_SETUP
            is BadgeConnectionState.Connecting -> ConnectionState.RECONNECTING
            is BadgeConnectionState.Connected -> ConnectionState.CONNECTED
            is BadgeConnectionState.Error -> ConnectionState.DISCONNECTED
        }
    }

    private fun mapToManagerUiState(managerStatus: BadgeManagerStatus): ConnectivityManagerState {
        return when (managerStatus) {
            is BadgeManagerStatus.Unknown -> ConnectivityManagerState.DISCONNECTED
            is BadgeManagerStatus.NeedsSetup -> ConnectivityManagerState.NEEDS_SETUP
            is BadgeManagerStatus.Disconnected -> ConnectivityManagerState.DISCONNECTED
            is BadgeManagerStatus.Connecting -> ConnectivityManagerState.RECONNECTING
            is BadgeManagerStatus.BlePairedNetworkUnknown ->
                ConnectivityManagerState.BLE_PAIRED_NETWORK_UNKNOWN
            is BadgeManagerStatus.BlePairedNetworkOffline ->
                ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE
            is BadgeManagerStatus.Ready -> ConnectivityManagerState.CONNECTED
            is BadgeManagerStatus.Error -> ConnectivityManagerState.DISCONNECTED
        }
    }

    private fun mapOverrideToManagerUiState(state: ConnectionState): ConnectivityManagerState {
        return when (state) {
            ConnectionState.CONNECTED -> ConnectivityManagerState.CONNECTED
            ConnectionState.DISCONNECTED -> ConnectivityManagerState.DISCONNECTED
            ConnectionState.NEEDS_SETUP -> ConnectivityManagerState.NEEDS_SETUP
            ConnectionState.CHECKING_UPDATE -> ConnectivityManagerState.CHECKING_UPDATE
            ConnectionState.UPDATE_FOUND -> ConnectivityManagerState.UPDATE_FOUND
            ConnectionState.UPDATING -> ConnectivityManagerState.UPDATING
            ConnectionState.RECONNECTING -> ConnectivityManagerState.RECONNECTING
            ConnectionState.WIFI_MISMATCH -> ConnectivityManagerState.WIFI_MISMATCH
        }
    }


    // --- 设备注册表操作 ---

    fun switchToDevice(macAddress: String) {
        launchExclusiveOperation("switchToDevice") {
            _uiOverride.value = ConnectionState.RECONNECTING
            registryManager.switchToDevice(macAddress)
            clearTransientConnectivityUi()
        }
    }

    fun setDefault(macAddress: String) {
        registryManager.setDefault(macAddress)
    }

    fun removeDevice(macAddress: String) {
        registryManager.removeDevice(macAddress)
    }

    fun renameDevice(macAddress: String, newName: String) {
        registryManager.renameDevice(macAddress, newName)
    }

    /**
     * 检查固件更新
     */
    fun checkForUpdate() {
        viewModelScope.launch {
            _uiOverride.value = ConnectionState.CHECKING_UPDATE
            val result = connectivityService.checkForUpdate()
            when (result) {
                is UpdateResult.Found -> {
                    _pendingVersion.value = result.version
                    _uiOverride.value = ConnectionState.UPDATE_FOUND
                }
                else -> _uiOverride.value = null  // 恢复真实状态
            }
        }
    }

    /**
     * 断开连接 — 调用软断开（保留 session，可重连）
     * 
     * 调用真实服务，ConnectivityBridge 状态会自动更新为 Disconnected
     */
    fun disconnect() {
        viewModelScope.launch {
            connectivityService.disconnect()
            // disconnectBle() keeps session → state naturally becomes Disconnected
        }
    }

    /**
     * 重新连接 — 显示 RECONNECTING 状态并处理结果
     */
    // 独占任务引用 — 防止重连/修复并发叠加
    private var activeOperationJob: Job? = null

    fun reconnect() {
        Log.d("ConnectivityVM", "reconnect() called, current effectiveState=${effectiveState.value}")
        launchExclusiveOperation("reconnect") {
            _wifiMismatchErrorMessage.value = null
            _uiOverride.value = ConnectionState.RECONNECTING
            Log.d("ConnectivityVM", "Set override=RECONNECTING, calling service.reconnect()")
            val result = connectivityService.reconnect()
            Log.d("ConnectivityVM", "service.reconnect() returned: $result")
            when (result) {
                ReconnectResult.Connected -> clearTransientConnectivityUi()
                ReconnectResult.DeviceNotFound -> clearTransientConnectivityUi()
                is ReconnectResult.WifiMismatch -> {
                    _wifiMismatchSuggestedSsid.value = result.currentPhoneSsid
                    _uiOverride.value = ConnectionState.WIFI_MISMATCH
                }
                is ReconnectResult.Error -> clearTransientConnectivityUi()
            }
            Log.d("ConnectivityVM", "After reconnect: override=${_uiOverride.value}, effective=${effectiveState.value}")
        }
    }

    private fun launchExclusiveOperation(
        operationName: String,
        block: suspend () -> Unit
    ) {
        if (activeOperationJob?.isActive == true) {
            Log.d("ConnectivityVM", "$operationName skipped — another operation already in progress")
            return
        }
        val job = viewModelScope.launch {
            try {
                block()
            } finally {
                if (activeOperationJob === coroutineContext[Job]) {
                    activeOperationJob = null
                }
            }
        }
        activeOperationJob = job
    }

    /**
     * 开始固件更新
     */
    fun startUpdate() {
        _uiOverride.value = ConnectionState.UPDATING
    }

    /**
     * 更新完成 (由 UI 动画完成时调用)
     */
    fun completeUpdate() {
        _uiOverride.value = null  // 恢复真实状态
        _pendingVersion.value = null
    }

    /**
     * 取消/忽略操作，返回默认状态
     */
    fun cancel() {
        clearTransientConnectivityUi()
    }

    fun clearWifiMismatchError() {
        _wifiMismatchErrorMessage.value = null
    }

    /**
     * 重置瞬时连接 UI 状态
     */
    fun resetTransientState() {
        activeOperationJob?.cancel()
        activeOperationJob = null
        clearTransientConnectivityUi()
    }

    /**
     * 更新 WiFi 配置
     */
    fun updateWifiConfig(ssid: String, password: String) {
        val normalizedSsid = ssid.trim()
        val normalizedPassword = password.trim()
        if (normalizedSsid.isEmpty() || normalizedPassword.isEmpty()) {
            _wifiMismatchErrorMessage.value = WIFI_MISMATCH_EMPTY_CREDENTIALS_ERROR
            return
        }
        launchExclusiveOperation("updateWifiConfig") {
            _wifiMismatchErrorMessage.value = null
            _wifiMismatchSuggestedSsid.value = normalizedSsid
            _uiOverride.value = ConnectionState.RECONNECTING
            val result = connectivityService.updateWifiConfig(normalizedSsid, normalizedPassword)
            when (result) {
                is WifiConfigResult.Success -> clearTransientConnectivityUi()
                is WifiConfigResult.Error -> {
                    _wifiMismatchErrorMessage.value = result.message
                    _uiOverride.value = ConnectionState.WIFI_MISMATCH
                }
            }
        }
    }

    private fun clearTransientConnectivityUi() {
        _wifiMismatchSuggestedSsid.value = null
        _wifiMismatchErrorMessage.value = null
        _uiOverride.value = null
    }

    // --- 徽章语音音量（快速入口） ---
    // 拖动期间仅更新 UI 显示状态；松手时通过 onVoiceVolumeCommitted 下发 BLE，
    // 避免 ESP32 在高频写入下崩溃。
    private val _voiceVolume = MutableStateFlow(voiceVolumeStore.desiredVolume.value)
    val voiceVolume: StateFlow<Int> = _voiceVolume.asStateFlow()

    private var voiceVolumeJob: Job? = null

    fun onVoiceVolumeDrag(level: Int) {
        _voiceVolume.value = level.coerceIn(0, 100)
    }

    fun onVoiceVolumeCommitted() {
        val level = _voiceVolume.value
        voiceVolumeStore.setDesiredVolume(level)
        if (level == voiceVolumeStore.lastAppliedVolume.value) return
        voiceVolumeJob?.cancel()
        voiceVolumeJob = viewModelScope.launch {
            if (connectionManager.setVoiceVolume(level)) {
                voiceVolumeStore.markAppliedVolume(level)
            }
        }
    }
}

internal const val WIFI_MISMATCH_EMPTY_CREDENTIALS_ERROR = "Wi-Fi 名称和密码不能为空"

/**
 * 连接模态框状态枚举
 */
enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    NEEDS_SETUP,     // 需要初始化配网
    CHECKING_UPDATE,
    UPDATE_FOUND,
    UPDATING,
    RECONNECTING,
    WIFI_MISMATCH
}

enum class ConnectivityManagerState {
    CONNECTED,
    DISCONNECTED,
    BLE_PAIRED_NETWORK_UNKNOWN,
    BLE_PAIRED_NETWORK_OFFLINE,
    NEEDS_SETUP,
    CHECKING_UPDATE,
    UPDATE_FOUND,
    UPDATING,
    RECONNECTING,
    WIFI_MISMATCH
}
