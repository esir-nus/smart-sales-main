package com.smartsales.prism.ui.components.connectivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.connectivity.ConnectivityService
import com.smartsales.prism.domain.connectivity.ReconnectResult
import com.smartsales.prism.domain.connectivity.UpdateResult
import com.smartsales.prism.domain.connectivity.WifiConfigResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val connectivityService: ConnectivityService
) : ViewModel() {

    // 连接状态
    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // 电池电量 (Mock)
    private val _batteryLevel = MutableStateFlow(85)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    // 待更新版本
    private val _pendingVersion = MutableStateFlow<String?>(null)
    val pendingVersion: StateFlow<String?> = _pendingVersion.asStateFlow()

    /**
     * 检查固件更新
     */
    fun checkForUpdate() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.CHECKING_UPDATE
            val result = connectivityService.checkForUpdate()
            _connectionState.value = when (result) {
                is UpdateResult.Found -> {
                    _pendingVersion.value = result.version
                    ConnectionState.UPDATE_FOUND
                }
                else -> ConnectionState.CONNECTED
            }
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        viewModelScope.launch {
            connectivityService.disconnect()
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * 重新连接
     */
    fun reconnect() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.RECONNECTING
            val result = connectivityService.reconnect()
            _connectionState.value = when (result) {
                ReconnectResult.Connected -> ConnectionState.CONNECTED
                ReconnectResult.WifiMismatch -> ConnectionState.WIFI_MISMATCH
                else -> ConnectionState.DISCONNECTED
            }
        }
    }

    /**
     * 开始固件更新
     */
    fun startUpdate() {
        _connectionState.value = ConnectionState.UPDATING
    }

    /**
     * 更新完成 (由 UI 动画完成时调用)
     */
    fun completeUpdate() {
        _connectionState.value = ConnectionState.CONNECTED
        _pendingVersion.value = null
    }

    /**
     * 取消/忽略操作，返回默认状态
     */
    fun cancel() {
        _connectionState.value = ConnectionState.CONNECTED
    }

    /**
     * 更新 WiFi 配置
     */
    fun updateWifiConfig(ssid: String, password: String) {
        viewModelScope.launch {
            val result = connectivityService.updateWifiConfig(ssid, password)
            _connectionState.value = when (result) {
                is WifiConfigResult.Success -> ConnectionState.CONNECTED
                is WifiConfigResult.Error -> ConnectionState.WIFI_MISMATCH // 保持在配置界面
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
