package com.smartsales.feature.connectivity.scan

import com.smartsales.feature.connectivity.BlePeripheral
import kotlinx.coroutines.flow.StateFlow

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/scan/BleScanner.kt
// 模块：:feature:connectivity
// 说明：抽象 BLE 扫描器，向上层暴露统一的设备列表与控制
// 作者：创建于 2025-11-18
interface BleScanner {
    val devices: StateFlow<List<BlePeripheral>>
    val isScanning: StateFlow<Boolean>

    fun start()
    fun stop()
}
