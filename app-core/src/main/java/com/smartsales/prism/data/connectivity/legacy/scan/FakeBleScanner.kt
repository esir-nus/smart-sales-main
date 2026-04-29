package com.smartsales.prism.data.connectivity.legacy.scan

import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake implementation for testing. Supports stubbing device list.
 */
class FakeBleScanner : BleScanner {
    private val _devices = MutableStateFlow<List<BlePeripheral>>(emptyList())
    private val _isScanning = MutableStateFlow(false)

    override val devices: StateFlow<List<BlePeripheral>> = _devices
    override val isScanning: StateFlow<Boolean> = _isScanning
    override val isBluetoothEnabled: Boolean = true

    var startCalls = 0
    var stopCalls = 0
    var scanForFirstResult: BlePeripheral? = null
    var scanForMacResult: BlePeripheral? = null
    val scanForMacCalls = mutableListOf<Pair<String, Long>>()

    override fun start() {
        startCalls++
        _isScanning.value = true
    }

    override fun stop() {
        stopCalls++
        _isScanning.value = false
    }

    override suspend fun scanForFirst(timeoutMs: Long): BlePeripheral? = scanForFirstResult

    override suspend fun scanForMac(macAddress: String, timeoutMs: Long): BlePeripheral? {
        scanForMacCalls += macAddress to timeoutMs
        return scanForMacResult?.takeIf { it.id == macAddress }
    }

    fun setDevices(list: List<BlePeripheral>) {
        _devices.value = list
    }

    fun reset() {
        _devices.value = emptyList()
        _isScanning.value = false
        startCalls = 0
        stopCalls = 0
        scanForFirstResult = null
        scanForMacResult = null
        scanForMacCalls.clear()
    }
}
