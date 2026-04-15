package com.smartsales.prism.data.connectivity.registry

import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import kotlinx.coroutines.flow.StateFlow

/**
 * 设备注册表管理器 — 多设备编排层
 *
 * 位于 DeviceConnectionManager（单设备）之上，管理"哪个设备"这个问题。
 * DeviceConnectionManager 继续管理"怎么连接"。
 */
interface DeviceRegistryManager {
    /** 所有已注册设备，响应式。注册/移除/切换后更新。 */
    val registeredDevices: StateFlow<List<RegisteredDevice>>

    /** 当前活跃设备（DeviceConnectionManager 正在操作的设备）。无设备时为 null。 */
    val activeDevice: StateFlow<RegisteredDevice?>

    /** 注册新配对的设备。第一个设备自动成为默认设备。 */
    fun registerDevice(peripheral: BlePeripheral, session: BleSession)

    /** 重命名设备。 */
    fun renameDevice(macAddress: String, newName: String)

    /** 设为默认设备。不会立即切换连接。 */
    fun setDefault(macAddress: String)

    /** 切换活跃设备。断开当前设备，启动新设备的重连。 */
    suspend fun switchToDevice(macAddress: String)

    /** 移除已注册设备。若为活跃设备，先断开。若为默认设备，自动提升其他设备。 */
    fun removeDevice(macAddress: String)

    /** 应用启动时调用。加载默认设备，执行迁移（如需），触发自动重连。 */
    fun initializeOnLaunch()
}
