package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.connectivity.ConnectivityService
import com.smartsales.prism.domain.connectivity.ReconnectResult
import com.smartsales.prism.domain.connectivity.UpdateResult
import com.smartsales.prism.domain.connectivity.WifiConfigResult
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Fake 连接服务 - 用于 UI 原型验证
 * Fake Connectivity Service for UI prototype validation.
 * Delays simulate real network/Bluetooth latency.
 */
@Singleton
class FakeConnectivityService @Inject constructor() : ConnectivityService {
    
    // 模拟状态: 控制 reconnect 的交替行为
    private var reconnectAttempts = 0
    
    override suspend fun checkForUpdate(): UpdateResult {
        delay(1500) // 模拟网络延迟
        // 50% 概率发现更新
        return if (Random.nextBoolean()) {
            UpdateResult.Found("v1.3.0", "包含重要安全修复与性能提升")
        } else {
            UpdateResult.None
        }
    }
    
    override suspend fun reconnect(): ReconnectResult {
        delay(2000) // 模拟 BLE 扫描延迟
        reconnectAttempts++
        // 交替返回: Connected -> WifiMismatch -> Connected -> ...
        return if (reconnectAttempts % 2 == 0) {
            ReconnectResult.Connected
        } else {
            ReconnectResult.WifiMismatch
        }
    }
    
    override suspend fun disconnect() {
        delay(500) // 模拟断开延迟
        // Fake: 什么都不做，只是延迟
    }
    
    override suspend fun unpair() {
        delay(500) // 模拟取消配对延迟
        // Fake: 什么都不做，只是延迟
    }
    
    override suspend fun updateWifiConfig(ssid: String, password: String): WifiConfigResult {
        delay(1500) // 模拟配置传输延迟
        // 总是成功 (Fake 不验证密码)
        return WifiConfigResult.Success
    }
}
