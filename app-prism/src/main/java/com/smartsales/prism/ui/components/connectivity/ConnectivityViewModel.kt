package com.smartsales.prism.ui.components.connectivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.ConnectivityService
import com.smartsales.prism.domain.connectivity.ReconnectResult
import com.smartsales.prism.domain.connectivity.UpdateResult
import com.smartsales.prism.domain.connectivity.WifiConfigResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val connectivityBridge: ConnectivityBridge
) : ViewModel() {

    // 连接状态 — 从真实 ConnectivityBridge 订阅
    val connectionState: StateFlow<ConnectionState> = connectivityBridge.connectionState
        .map { badgeState -> mapToUiState(badgeState) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ConnectionState.DISCONNECTED
        )

    // 电池电量 (Mock) — Wave 3: 从 Badge 查询真实电量
    private val _batteryLevel = MutableStateFlow(85)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.DISCONNECTED)
    
    /**
     * 映射 Badge 连接状态 → UI 状态
     */
    private fun mapToUiState(badgeState: BadgeConnectionState): ConnectionState {
        return when (badgeState) {
            is BadgeConnectionState.Disconnected -> ConnectionState.DISCONNECTED
            is BadgeConnectionState.Connecting -> ConnectionState.RECONNECTING
            is BadgeConnectionState.Connected -> ConnectionState.CONNECTED
            is BadgeConnectionState.Error -> ConnectionState.DISCONNECTED
        }
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
     * 断开连接
     * 
     * 调用真实服务，ConnectivityBridge 状态会自动更新为 Disconnected
     */
    fun disconnect() {
        viewModelScope.launch {
            connectivityService.disconnect()
            _uiOverride.value = null  // 清除覆盖，显示真实状态
        }
    }

    /**
     * 重新连接
     * 
     * ConnectivityService 内部已处理 RECONNECTING 状态，
     * 这里不需要手动设置 UI 状态
     */
    fun reconnect() {
        viewModelScope.launch {
            _uiOverride.value = null  // 清除覆盖
            connectivityService.reconnect()
            // 真实状态会通过 ConnectivityBridge 自动更新
        }
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
        _uiOverride.value = null  // 恢复真实状态
    }

    /**
     * 更新 WiFi 配置
     */
    fun updateWifiConfig(ssid: String, password: String) {
        viewModelScope.launch {
            val result = connectivityService.updateWifiConfig(ssid, password)
            when (result) {
                is WifiConfigResult.Success -> _uiOverride.value = null  // 恢复真实状态
                is WifiConfigResult.Error -> _uiOverride.value = ConnectionState.WIFI_MISMATCH
            }
        }
    }
}

/**
 * 连接模态框状态枚举
 */
enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    CHECKING_UPDATE,
    UPDATE_FOUND,
    UPDATING,
    RECONNECTING,
    WIFI_MISMATCH
}
