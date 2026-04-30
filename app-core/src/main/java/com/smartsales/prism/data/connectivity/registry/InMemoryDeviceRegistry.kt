package com.smartsales.prism.data.connectivity.registry

/**
 * In-memory fake device registry for testing (no Robolectric needed).
 */
class InMemoryDeviceRegistry : DeviceRegistry {

    private val devices = mutableListOf<RegisteredDevice>()
    private var defaultMac: String? = null

    override fun loadAll(): List<RegisteredDevice> =
        devices.map { it.copy(isDefault = it.macAddress == defaultMac) }

    override fun findByMac(macAddress: String): RegisteredDevice? =
        loadAll().find { it.macAddress == macAddress }

    override fun getDefault(): RegisteredDevice? =
        loadAll().find { it.isDefault }

    override fun register(device: RegisteredDevice) {
        devices.removeAll { it.macAddress == device.macAddress }
        devices += device.copy(isDefault = false)
        if (device.isDefault || devices.size == 1) {
            defaultMac = device.macAddress
        }
    }

    override fun rename(macAddress: String, newDisplayName: String) {
        val index = devices.indexOfFirst { it.macAddress == macAddress }
        if (index < 0) return
        devices[index] = devices[index].copy(displayName = newDisplayName)
    }

    override fun updateLastConnected(macAddress: String, timestampMillis: Long) {
        val index = devices.indexOfFirst { it.macAddress == macAddress }
        if (index < 0) return
        devices[index] = devices[index].copy(lastConnectedAtMillis = timestampMillis)
    }

    override fun updateLastUserIntent(macAddress: String, timestampMillis: Long) {
        val index = devices.indexOfFirst { it.macAddress == macAddress }
        if (index < 0) return
        devices[index] = devices[index].copy(lastUserIntentAtMillis = timestampMillis)
    }

    override fun updateMacAddress(oldMac: String, newMac: String) {
        val index = devices.indexOfFirst { it.macAddress == oldMac }
        if (index < 0) return
        devices[index] = devices[index].copy(macAddress = newMac)
        if (defaultMac == oldMac) defaultMac = newMac
    }

    override fun setDefault(macAddress: String) {
        if (devices.any { it.macAddress == macAddress }) {
            defaultMac = macAddress
        }
    }

    override fun remove(macAddress: String) {
        devices.removeAll { it.macAddress == macAddress }
        if (defaultMac == macAddress) {
            defaultMac = devices.maxByOrNull { it.lastConnectedAtMillis }?.macAddress
        }
    }

    override fun isEmpty(): Boolean = devices.isEmpty()

    override fun updateManuallyDisconnected(macAddress: String, value: Boolean) {
        val index = devices.indexOfFirst { it.macAddress == macAddress }
        if (index < 0) return
        devices[index] = devices[index].copy(manuallyDisconnected = value)
    }

    override fun updateBleDetected(macAddress: String, value: Boolean) {
        val index = devices.indexOfFirst { it.macAddress == macAddress }
        if (index < 0) return
        devices[index] = devices[index].copy(bleDetected = value)
    }
}
