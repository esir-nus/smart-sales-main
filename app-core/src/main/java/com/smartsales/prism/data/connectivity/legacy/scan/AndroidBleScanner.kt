package com.smartsales.prism.data.connectivity.legacy.scan

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleProfileConfig
import com.smartsales.prism.data.connectivity.legacy.ConnectivityLogger

import androidx.core.content.ContextCompat
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

    private val appContext: Context = context
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
    override val isBluetoothEnabled: Boolean
        get() = adapter?.isEnabled == true

    private var debugLoggedCount = 0

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
        Log.d("BT311Scan", "start() called, isScanning=${_isScanning.value}")
        if (_isScanning.value) {
            Log.d("BT311Scan", "Already scanning, returning early")
            return
        }
        if (adapter == null || !adapter.isEnabled) {
            Log.w("BT311Scan", "Bluetooth adapter null or disabled, adapter=$adapter, isEnabled=${adapter?.isEnabled}")
            _devices.value = emptyList()
            _isScanning.value = false
            return
        }
        if (!hasScanPermissions()) {
            Log.w("BT311Scan", "Missing scan permissions (BLUETOOTH_SCAN or ACCESS_FINE_LOCATION)")
            _isScanning.value = false
            return
        }
        _devices.value = emptyList()
        _isScanning.value = true
        debugLoggedCount = 0
        Log.d("BT311Scan", "Calling startScan on scanner=$scanner")
        runCatching { scanner?.startScan(null, scanSettings, callback) }
            .onSuccess { Log.d("BT311Scan", "startScan() success") }
            .onFailure { e ->
                Log.e("BT311Scan", "startScan() FAILED: ${e.message}", e)
                _isScanning.value = false
            }
        Log.d("BT311Scan", "start() completed, isScanning=${_isScanning.value}")
    }

    @SuppressLint("MissingPermission")
    override fun stop() {
        if (!_isScanning.value) return
        runCatching { scanner?.stopScan(callback) }
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission") // 仅在已授权时读取 device.name
    private fun handleResult(result: ScanResult) {
        val device = result.device ?: return
        val displayName = (if (hasConnectPermission()) device.name else null)
            ?: result.scanRecord?.deviceName
            ?: ""
        val advertise = result.scanRecord
        val advertisedUuids = buildList {
            advertise?.serviceUuids?.map { it.uuid }?.let { addAll(it) }
            advertise?.bytes?.let { addAll(parseAdvertisedUuids(it)) }
        }
        logDebug(device.address, displayName, advertisedUuids)
        ConnectivityLogger.d(
            "扫描到设备 ${device.address} (${displayName.ifBlank { "Unknown" }}) rssi=${result.rssi} uuids=${advertisedUuids.joinToString()}"
        )
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
        
        // CRITICAL: Stop scan SYNCHRONOUSLY to prevent more callbacks
        // The scope.launch was async, allowing 8+ callbacks before stop took effect
        stop()
        
        scope.launch {
            _devices.value = listOf(peripheral)
        }
    }

    private fun hasConnectPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 31) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasScanPermissions(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) return false
        }
        return ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
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

    private fun logDebug(address: String, name: String, uuids: List<java.util.UUID>) {
        // if (!BuildConfig.DEBUG) return
        if (debugLoggedCount >= 30) return
        val defaultProfile = profiles.firstOrNull()
        val matches = defaultProfile?.matches(name, uuids) ?: false
        Log.d(
            "BT311Scan",
            "addr=$address, name=${name.ifBlank { "Unknown" }}, uuids=${uuids.joinToString()}, matches=$matches"
        )
        debugLoggedCount += 1
    }
}
