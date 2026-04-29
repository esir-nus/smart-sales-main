package com.smartsales.prism.ui.components.connectivity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.data.connectivity.registry.DeviceRegistryManager
import com.smartsales.prism.data.connectivity.registry.RegisteredDevice
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.ConnectivityService
import com.smartsales.prism.domain.connectivity.IsolationTriggerContext
import com.smartsales.prism.domain.connectivity.ReconnectResult
import com.smartsales.prism.domain.connectivity.UpdateResult
import com.smartsales.prism.domain.connectivity.WifiConfigResult
import com.smartsales.prism.domain.connectivity.WifiRepairEvent
import com.smartsales.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    private val promptCoordinator: ConnectivityPromptCoordinator
) : ViewModel() {

    // 设备注册表 — 多设备管理
    val registeredDevices: StateFlow<List<RegisteredDevice>> = registryManager.registeredDevices
    val activeDevice: StateFlow<RegisteredDevice?> = registryManager.activeDevice

    // 排序后的设备列表：活跃设备置顶，其余按注册时间排序
    val sortedDevices: StateFlow<List<RegisteredDevice>> = combine(
        registryManager.registeredDevices,
        registryManager.activeDevice
    ) { devices, active ->
        devices.sortedWith(compareBy(
            { it.macAddress != active?.macAddress },
            { -it.lastConnectedAtMillis }
        ))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // 连接状态 — 融合 flat badge state、manager 诊断和 BLE 检测标记
    val connectionState: StateFlow<ConnectionState> = combine(
        connectivityBridge.connectionState,
        connectivityBridge.managerStatus,
        registryManager.activeDevice
    ) { badgeState, managerStatus, activeDevice -> fuseUiState(badgeState, managerStatus, activeDevice) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = fuseUiState(
                connectivityBridge.connectionState.value,
                connectivityBridge.managerStatus.value,
                registryManager.activeDevice.value
            )
        )

    // 连接管理界面专用状态 — richer BLE / Wi‑Fi 诊断，但不影响 shell 路由
    private val managerBaseState: StateFlow<ConnectivityManagerState> = connectivityBridge.managerStatus
        .map { managerStatus ->
            val uiState = mapToManagerUiState(managerStatus)
            Log.d(
                "ConnectivityVM",
                "managerStatus=${managerStatus.toLogLabel()} managerState=$uiState"
            )
            uiState
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = mapToManagerUiState(connectivityBridge.managerStatus.value)
        )

    // 电池电量 — 在首个 Bat# 推送到达前保持 null，以区分“暂无读数”和真实电量
    val batteryLevel: StateFlow<Int?> = connectivityBridge.batteryNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    // 固件版本 — 在首个 Ver# 响应前保持 null，以区分“尚未查询”和真实版本号
    private val _firmwareVersion = MutableStateFlow<String?>(null)
    val firmwareVersion: StateFlow<String?> = _firmwareVersion.asStateFlow()

    // SD 卡剩余空间 — 用户主动查询前保持 null，断开后清空
    private val _sdCardSpace = MutableStateFlow<String?>(null)
    val sdCardSpace: StateFlow<String?> = _sdCardSpace.asStateFlow()

    private val _wifiMismatchSuggestedSsid = MutableStateFlow<String?>(null)
    val wifiMismatchSuggestedSsid: StateFlow<String?> = _wifiMismatchSuggestedSsid.asStateFlow()
    private val _wifiMismatchErrorMessage = MutableStateFlow<String?>(null)
    val wifiMismatchErrorMessage: StateFlow<String?> = _wifiMismatchErrorMessage.asStateFlow()
    private val _promptRequests = MutableSharedFlow<WifiMismatchPromptRequest>(extraBufferCapacity = 1)
    val promptRequests: SharedFlow<WifiMismatchPromptRequest> = _promptRequests.asSharedFlow()

    // 疑似隔离时记录徽章 IP，用于界面诊断展示
    private val _isolationBadgeIp = MutableStateFlow<String?>(null)
    val isolationBadgeIp: StateFlow<String?> = _isolationBadgeIp.asStateFlow()

    // 隔离触发场景 — 决定 ConnectivityModal 展示的标题与说明文案
    private val _isolationTriggerContext = MutableStateFlow<IsolationTriggerContext?>(null)
    val isolationTriggerContext: StateFlow<IsolationTriggerContext?> = _isolationTriggerContext.asStateFlow()

    // 待更新版本
    private val _pendingVersion = MutableStateFlow<String?>(null)
    val pendingVersion: StateFlow<String?> = _pendingVersion.asStateFlow()

    // Wi-Fi 修复流程专用状态机 — 独立于 ConnectivityManagerState
    private val _repairState = MutableStateFlow<WifiRepairState>(WifiRepairState.Idle)
    val repairState: StateFlow<WifiRepairState> = _repairState.asStateFlow()

    private val _debugProbeText = MutableStateFlow<String?>(null)
    val debugProbeText: StateFlow<String?> = _debugProbeText.asStateFlow()

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
        fuseUiState(
            connectivityBridge.connectionState.value,
            connectivityBridge.managerStatus.value,
            registryManager.activeDevice.value
        )
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

    init {
        viewModelScope.launch {
            promptCoordinator.wifiMismatchRequests.collect { request ->
                _wifiMismatchSuggestedSsid.value = request.suggestedSsid
                _wifiMismatchErrorMessage.value = null
                _uiOverride.value = ConnectionState.WIFI_MISMATCH
                _promptRequests.emit(request)
            }
        }
        viewModelScope.launch {
            // 疑似客户端隔离：手机网络已验证但 HTTP 探测超时 → 展示隔离提示模态框
            promptCoordinator.suspectedIsolationRequests.collect { request ->
                _isolationBadgeIp.value = request.badgeIp
                _isolationTriggerContext.value = request.triggerContext
                _wifiMismatchSuggestedSsid.value = request.suggestedSsid
                _repairState.value = WifiRepairState.HardFailure(
                    WifiRepairState.HardFailure.HardFailureReason.SUSPECTED_ISOLATION
                )
                _uiOverride.value = ConnectionState.WIFI_MISMATCH
            }
        }
        viewModelScope.launch {
            connectivityBridge.wifiRepairEvents().collect { event ->
                _repairState.value = when (event) {
                    is WifiRepairEvent.CredentialsDispatched ->
                        WifiRepairState.SendingCredentials(event.ssid)
                    is WifiRepairEvent.UsableIpObserved ->
                        WifiRepairState.WaitingForBadgeNetworkSwitch(
                            (repairState.value as? WifiRepairState.SendingCredentials)?.ssid
                                ?: event.ip
                        )
                    is WifiRepairEvent.TransportConfirmed ->
                        WifiRepairState.TransportConfirmed(event.ip, event.badgeSsid)
                    is WifiRepairEvent.HttpReady ->
                        WifiRepairState.HttpReady(event.baseUrl)
                    is WifiRepairEvent.HttpDelayed ->
                        WifiRepairState.HttpDelayed(
                            badgeSsid = (repairState.value as? WifiRepairState.TransportConfirmed)?.badgeSsid,
                            baseUrl = event.baseUrl
                        )
                    is WifiRepairEvent.DefinitiveMismatch ->
                        WifiRepairState.HardFailure(WifiRepairState.HardFailure.HardFailureReason.SSID_MISMATCH)
                    is WifiRepairEvent.BadgeOffline ->
                        // 仅在传输从未确认时升级为 HardFailure；UsableIpObserved 后的 BadgeOffline 忽略
                        if (_repairState.value is WifiRepairState.TransportConfirmed ||
                            _repairState.value is WifiRepairState.HttpCheckPending
                        ) _repairState.value
                        else WifiRepairState.HardFailure(WifiRepairState.HardFailure.HardFailureReason.BADGE_OFFLINE)
                    is WifiRepairEvent.CredentialReplayFailed ->
                        WifiRepairState.HardFailure(WifiRepairState.HardFailure.HardFailureReason.CREDENTIAL_REPLAY_FAILED)
                    // TargetSsidObserved — 无独立 UI 状态变化，保持当前
                    else -> _repairState.value
                }
            }
        }
        viewModelScope.launch {
            connectivityBridge.firmwareVersionNotifications().collect { version ->
                _firmwareVersion.value = version
            }
        }
        viewModelScope.launch {
            connectivityBridge.sdCardSpaceNotifications().collect { formattedSize ->
                _sdCardSpace.value = formattedSize
            }
        }
        viewModelScope.launch {
            var wasConnected = false
            connectivityBridge.connectionState.collect { badgeState ->
                val isConnected = badgeState is BadgeConnectionState.Connected
                if (isConnected && !wasConnected) {
                    requestFirmwareVersion()
                } else if (!isConnected) {
                    _firmwareVersion.value = null
                    _sdCardSpace.value = null
                }
                wasConnected = isConnected
            }
        }
    }
    
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

    /**
     * 融合 flat badge state、manager 诊断和 BLE 检测标记。
     * 优先级：CONNECTED > BLE_DETECTED > PARTIAL_WIFI_DOWN > 其余状态
     */
    private fun fuseUiState(
        badgeState: BadgeConnectionState,
        managerStatus: BadgeManagerStatus,
        activeDevice: RegisteredDevice? = null
    ): ConnectionState {
        if (badgeState is BadgeConnectionState.Connected) {
            return ConnectionState.CONNECTED
        }
        if (activeDevice?.bleDetected == true) {
            return ConnectionState.BLE_DETECTED
        }
        return when (managerStatus) {
            is BadgeManagerStatus.BlePairedNetworkOffline,
            is BadgeManagerStatus.BlePairedNetworkUnknown -> ConnectionState.PARTIAL_WIFI_DOWN
            is BadgeManagerStatus.BleDetected -> ConnectionState.BLE_DETECTED
            else -> mapToUiState(badgeState)
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
            is BadgeManagerStatus.BleDetected -> ConnectivityManagerState.BLE_DETECTED
            is BadgeManagerStatus.HttpDelayed -> ConnectivityManagerState.HTTP_DELAYED
            is BadgeManagerStatus.Ready -> ConnectivityManagerState.CONNECTED
            is BadgeManagerStatus.Error -> ConnectivityManagerState.DISCONNECTED
        }
    }

    private fun BadgeManagerStatus.toLogLabel(): String {
        return when (this) {
            is BadgeManagerStatus.HttpDelayed ->
                "HttpDelayed(badgeIp=$badgeIp, ssid=$ssid, baseUrl=$baseUrl)"
            else -> this::class.simpleName ?: this.toString()
        }
    }

    private fun mapOverrideToManagerUiState(state: ConnectionState): ConnectivityManagerState {
        return when (state) {
            ConnectionState.CONNECTED -> ConnectivityManagerState.CONNECTED
            ConnectionState.PARTIAL_WIFI_DOWN -> ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE
            ConnectionState.BLE_DETECTED -> ConnectivityManagerState.BLE_DETECTED
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

    fun connectDevice(macAddress: String) {
        // 用户点选其他卡片时，新的连接请求必须抢占旧的重连显示。
        activeOperationJob?.cancel()
        val job = viewModelScope.launch {
            try {
                connectivityService.connect(macAddress)
                _uiOverride.value = ConnectionState.RECONNECTING
                withTimeoutOrNull(30_000L) {
                    managerBaseState.filter { it != ConnectivityManagerState.DISCONNECTED }.first()
                }
                clearTransientConnectivityUi()
            } finally {
                if (activeOperationJob === coroutineContext[Job]) {
                    activeOperationJob = null
                }
            }
        }
        activeOperationJob = job
    }

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

    fun startIsolationWifiRepair(): Boolean {
        val activeMac = activeDevice.value?.macAddress ?: run {
            Log.w("ConnectivityVM", "startIsolationWifiRepair skipped - no active device")
            return false
        }
        val suggestedSsid = _wifiMismatchSuggestedSsid.value
        Log.i(
            "ConnectivityVM",
            "startIsolationWifiRepair entering credential repair for active mac=$activeMac suggestedSsid=${suggestedSsid ?: "unknown"}"
        )
        activeOperationJob?.cancel()
        activeOperationJob = null
        _wifiMismatchErrorMessage.value = null
        _repairState.value = WifiRepairState.EditCredentials(suggestedSsid)
        _uiOverride.value = ConnectionState.WIFI_MISMATCH
        return true
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

    fun requestFirmwareVersion() {
        viewModelScope.launch {
            connectivityBridge.requestFirmwareVersion()
        }
    }

    fun requestSdCardSpace() {
        viewModelScope.launch {
            connectivityBridge.requestSdCardSpace()
        }
    }

    fun debugProbeMediaReadiness() {
        viewModelScope.launch {
            _debugProbeText.value = "media readiness: running"
            val ready = connectivityBridge.isReady()
            _debugProbeText.value = "media readiness: $ready"
            Log.i("ConnectivityVM", "debug media readiness result=$ready")
        }
    }

    fun debugListRecordings() {
        viewModelScope.launch {
            _debugProbeText.value = "/list: running"
            val result = connectivityBridge.listRecordings()
            _debugProbeText.value = when (result) {
                is Result.Success -> "/list: success count=${result.data.size}"
                is Result.Error -> "/list: error ${result.throwable.message ?: "unknown"}"
            }
            Log.i("ConnectivityVM", "debug list recordings result=${_debugProbeText.value}")
        }
    }

    fun debugEmitRecNotification() {
        viewModelScope.launch {
            _debugProbeText.value = "debug rec: listing"
            val result = connectivityBridge.listRecordings()
            val filename = when (result) {
                is Result.Success -> result.data
                    .map { it.trim() }
                    .firstOrNull { it.startsWith("rec_", ignoreCase = true) && it.endsWith(".wav", ignoreCase = true) }
                    ?: result.data.map { it.trim() }.firstOrNull { it.isNotBlank() }
                is Result.Error -> {
                    _debugProbeText.value = "debug rec: list error ${result.throwable.message ?: "unknown"}"
                    Log.w("ConnectivityVM", "debug rec source=debug_rec_button list failed", result.throwable)
                    null
                }
            }
            if (filename == null) {
                if (result is Result.Success) {
                    _debugProbeText.value = "debug rec: no files"
                    Log.i("ConnectivityVM", "debug rec source=debug_rec_button no files")
                }
                return@launch
            }

            _debugProbeText.value = "debug rec: emitting $filename"
            val emitted = connectivityBridge.debugEmitAudioRecordingReady(filename)
            _debugProbeText.value = if (emitted) {
                "debug rec emitted: $filename"
            } else {
                "debug rec blocked: $filename"
            }
            Log.i(
                "ConnectivityVM",
                "debug rec source=debug_rec_button filename=$filename emitted=$emitted"
            )
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
                    _wifiMismatchErrorMessage.value = result.errorMessage
                    _repairState.value = WifiRepairState.EditCredentials(result.currentPhoneSsid)
                    _uiOverride.value = ConnectionState.WIFI_MISMATCH
                }
                is ReconnectResult.Error -> clearTransientConnectivityUi()
            }
            Log.d("ConnectivityVM", "After reconnect: override=${_uiOverride.value}, effective=${effectiveState.value}")
        }
    }

    fun scheduleAutoReconnect() {
        if (activeOperationJob?.isActive == true) {
            Log.d("ConnectivityVM", "scheduleAutoReconnect skipped - another operation already in progress")
            return
        }
        // 允许在 DISCONNECTED 或 PARTIAL_WIFI_DOWN（BLE 已配对但 Wi-Fi 未就绪）场景下重连，
        // 保持 PARTIAL_WIFI_DOWN 引入前的行为不被削弱。
        val state = effectiveState.value
        if (state != ConnectionState.DISCONNECTED && state != ConnectionState.PARTIAL_WIFI_DOWN) {
            Log.d("ConnectivityVM", "scheduleAutoReconnect skipped - effectiveState=$state")
            return
        }
        Log.d("ConnectivityVM", "scheduleAutoReconnect() delegating to service")
        connectivityService.scheduleAutoReconnect()
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
            _repairState.value = WifiRepairState.SendingCredentials(normalizedSsid)
            _uiOverride.value = ConnectionState.RECONNECTING
            val result = connectivityService.updateWifiConfig(normalizedSsid, normalizedPassword)
            when (result) {
                is WifiConfigResult.Success -> clearTransientConnectivityUi()
                // 传输已确认，HTTP 仍在预热 — 保持表单区域但展示"切换成功"状态
                is WifiConfigResult.TransportConfirmedHttpDelayed -> {
                    _repairState.value = WifiRepairState.HttpDelayed(
                        badgeSsid = result.badgeSsid,
                        baseUrl = result.baseUrl
                    )
                    _uiOverride.value = ConnectionState.WIFI_MISMATCH
                }
                is WifiConfigResult.Error -> {
                    _repairState.value = WifiRepairState.RetryableFailure(result.message)
                    _wifiMismatchErrorMessage.value = result.message
                    _uiOverride.value = ConnectionState.WIFI_MISMATCH
                }
            }
        }
    }

    private fun clearTransientConnectivityUi() {
        _wifiMismatchSuggestedSsid.value = null
        _wifiMismatchErrorMessage.value = null
        _isolationBadgeIp.value = null
        _isolationTriggerContext.value = null
        _repairState.value = WifiRepairState.Idle
        _uiOverride.value = null
    }
}

internal const val WIFI_MISMATCH_EMPTY_CREDENTIALS_ERROR = "Wi-Fi 名称和密码不能为空"

/**
 * 连接模态框状态枚举
 */
enum class ConnectionState {
    CONNECTED,
    PARTIAL_WIFI_DOWN,   // BLE 已配对但设备 Wi-Fi 未就绪
    BLE_DETECTED,        // BLE 扫描检测到设备，在范围内可连接
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
    HTTP_DELAYED,
    DISCONNECTED,
    BLE_PAIRED_NETWORK_UNKNOWN,
    BLE_PAIRED_NETWORK_OFFLINE,
    BLE_DETECTED,        // BLE 扫描检测到设备，在范围内可连接
    NEEDS_SETUP,
    CHECKING_UPDATE,
    UPDATE_FOUND,
    UPDATING,
    RECONNECTING,
    WIFI_MISMATCH
}
