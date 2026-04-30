package com.smartsales.prism.data.connectivity.registry

/**
 * 设备注册表 — 持久化已配对设备列表
 *
 * 管理多设备注册、默认设备标记、设备重命名。
 * 实现类负责线程安全的持久化。
 */
interface DeviceRegistry {
    fun loadAll(): List<RegisteredDevice>
    fun findByMac(macAddress: String): RegisteredDevice?
    fun getDefault(): RegisteredDevice?
    fun register(device: RegisteredDevice)
    fun rename(macAddress: String, newDisplayName: String)
    fun updateLastConnected(macAddress: String, timestampMillis: Long = System.currentTimeMillis())
    fun updateLastUserIntent(macAddress: String, timestampMillis: Long = System.currentTimeMillis())
    fun updateMacAddress(oldMac: String, newMac: String)
    fun setDefault(macAddress: String)
    fun remove(macAddress: String)
    fun isEmpty(): Boolean
    fun updateManuallyDisconnected(macAddress: String, value: Boolean)
    fun updateBleDetected(macAddress: String, value: Boolean)
}
