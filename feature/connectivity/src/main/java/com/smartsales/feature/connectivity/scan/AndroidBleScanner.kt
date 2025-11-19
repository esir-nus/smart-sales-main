package com.smartsales.feature.connectivity.scan

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleProfileConfig
import com.smartsales.feature.connectivity.ConnectivityLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/scan/AndroidBleScanner.kt
// 模块：:feature:connectivity
// 说明：实际的 BLE 扫描实现，复用供应商 UART 服务 UUID 过滤
// 作者：创建于 2025-11-18
@Singleton
class AndroidBleScanner @Inject constructor(
    @ApplicationContext context: Context,
    bluetoothManager: BluetoothManager,
    private val profiles: List<BleProfileConfig>
) : BleScanner {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner get() = adapter?.bluetoothLeScanner
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val _devices = MutableStateFlow<List<BlePeripheral>>(emptyList())
    override val devices: StateFlow<List<BlePeripheral>> = _devices
    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { handleResult(it) }
        }
    }

    @SuppressLint("MissingPermission")
    override fun start() {
        if (_isScanning.value) return
        if (adapter == null || !adapter.isEnabled) {
            _devices.value = emptyList()
            _isScanning.value = false
            return
        }
        _devices.value = emptyList()
        _isScanning.value = true
        runCatching { scanner?.startScan(null, scanSettings, callback) }
            .onFailure {
                _isScanning.value = false
            }
    }

    @SuppressLint("MissingPermission")
    override fun stop() {
        if (!_isScanning.value) return
        runCatching { scanner?.stopScan(callback) }
        _isScanning.value = false
    }

    private fun handleResult(result: ScanResult) {
        val device = result.device ?: return
        val displayName = device.name ?: result.scanRecord?.deviceName ?: ""
        val advertise = result.scanRecord
        val advertisedUuids = buildList {
            advertise?.serviceUuids?.map { it.uuid }?.let { addAll(it) }
            advertise?.bytes?.let { addAll(parseAdvertisedUuids(it)) }
        }
        val profile = profiles.firstOrNull { cfg ->
            cfg.matches(displayName, advertisedUuids)
        }
        if (profile == null) {
            ConnectivityLogger.d(
                "忽略设备 ${device.address} (${displayName.ifBlank { "Unknown" }})：未匹配任何 profile"
            )
            return
        }
        val peripheral = BlePeripheral(
            id = device.address,
            name = if (displayName.isNotBlank()) displayName else profile.displayName,
            signalStrengthDbm = result.rssi,
            profileId = profile.id
        )
        ConnectivityLogger.i(
            "发现设备 ${peripheral.name} (${peripheral.id}) 匹配 profile=${profile.id}"
        )
        scope.launch {
            _devices.value = listOf(peripheral)
            _isScanning.value = false
            stop()
        }
    }

    private fun parseAdvertisedUuids(payload: ByteArray): List<java.util.UUID> {
        val uuids = mutableListOf<java.util.UUID>()
        var offset = 0
        while (offset < payload.size - 2) {
            var len = payload[offset++].toInt() and 0xFF
            if (len == 0) break
            val type = payload[offset++].toInt() and 0xFF
            when (type) {
                0x02, 0x03 -> {
                    var remaining = len - 1
                    while (remaining >= 2 && offset + 1 < payload.size) {
                        val uuid16 =
                            (payload[offset++].toInt() and 0xFF) or ((payload[offset++].toInt() and 0xFF) shl 8)
                        remaining -= 2
                        uuids.add(
                            java.util.UUID.fromString(
                                String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)
                            )
                        )
                    }
                }

                0x06, 0x07 -> {
                    var remaining = len - 1
                    while (remaining >= 16 && offset + 15 < payload.size) {
                        val buffer = ByteBuffer.wrap(payload, offset, 16).order(ByteOrder.LITTLE_ENDIAN)
                        val msb = buffer.long
                        val lsb = buffer.long
                        uuids.add(java.util.UUID(lsb, msb))
                        offset += 16
                        remaining -= 16
                    }
                }

                else -> offset += (len - 1)
            }
        }
        return uuids
    }
}
