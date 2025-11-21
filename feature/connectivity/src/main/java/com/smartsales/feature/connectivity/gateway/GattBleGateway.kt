package com.smartsales.feature.connectivity.gateway

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.ConnectivityLogger
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import java.io.Closeable
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import dagger.hilt.android.qualifiers.ApplicationContext

private const val NETWORK_QUERY_COMMAND = "wifi#address#ip#name"
private const val DEFAULT_CONNECTION_TIMEOUT_MS = 10_000L
private const val DEFAULT_OPERATION_TIMEOUT_MS = 5_000L

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/gateway/GattBleGateway.kt
// 模块：:feature:connectivity
// 说明：通过 Android BLE GATT 写入 Wi-Fi 凭据并读取热点信息
// 作者：创建于 2025-11-16
@Singleton
class GattBleGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothManager: BluetoothManager,
    private val dispatchers: DispatcherProvider
) : BleGateway {

    private val lock = Mutex()
    private val configCache = mutableMapOf<String, BleGatewayConfig>()

    override suspend fun provision(
        session: BleSession,
        credentials: WifiCredentials
    ): BleGatewayResult =
        when (val outcome = execute(session.peripheralId) { gattContext, config ->
            val payload = buildCredentialsPayload(credentials)
            gattContext.writeCharacteristic(config.credentialCharacteristicUuid, payload)
            val ackBytes = gattContext.awaitNotificationOrRead(config.provisioningStatusCharacteristicUuid)
            parseProvisioningAck(credentials, ackBytes)
        }) {
            is GatewayOutcome.Success -> outcome.value
            is GatewayOutcome.Failure -> outcome.error
        }

    override suspend fun requestHotspot(
        session: BleSession
    ): HotspotResult =
        when (val outcome = execute(session.peripheralId) { gattContext, config ->
            val response = gattContext.readCharacteristic(config.hotspotCharacteristicUuid)
            parseHotspot(response)
        }) {
            is GatewayOutcome.Success -> HotspotResult.Success(outcome.value)
            is GatewayOutcome.Failure -> when (val error = outcome.error) {
                is BleGatewayResult.PermissionDenied -> HotspotResult.PermissionDenied(error.permissions)
                BleGatewayResult.Timeout -> HotspotResult.Timeout(DEFAULT_OPERATION_TIMEOUT_MS)
                is BleGatewayResult.TransportError -> HotspotResult.TransportError(error.reason)
                is BleGatewayResult.CredentialRejected -> HotspotResult.TransportError(error.reason)
                is BleGatewayResult.DeviceMissing -> HotspotResult.DeviceMissing(error.peripheralId)
                is BleGatewayResult.Success -> HotspotResult.TransportError("无效错误类型")
            }
        }

    override suspend fun queryNetwork(
        session: BleSession
    ): NetworkQueryResult =
        when (val outcome = execute(session.peripheralId) { gattContext, config ->
            val command = buildNetworkQueryPayload()
            gattContext.writeCharacteristic(config.credentialCharacteristicUuid, command)
            val response = gattContext.awaitNotificationOrRead(config.provisioningStatusCharacteristicUuid)
            parseNetworkResponse(response)
        }) {
            is GatewayOutcome.Success -> NetworkQueryResult.Success(outcome.value)
            is GatewayOutcome.Failure -> when (val error = outcome.error) {
                is BleGatewayResult.PermissionDenied -> NetworkQueryResult.PermissionDenied(error.permissions)
                BleGatewayResult.Timeout -> NetworkQueryResult.Timeout(DEFAULT_OPERATION_TIMEOUT_MS)
                is BleGatewayResult.TransportError -> NetworkQueryResult.TransportError(error.reason)
                is BleGatewayResult.CredentialRejected -> NetworkQueryResult.TransportError(error.reason)
                is BleGatewayResult.DeviceMissing -> NetworkQueryResult.DeviceMissing(error.peripheralId)
                is BleGatewayResult.Success -> NetworkQueryResult.TransportError("无效错误类型")
            }
        }

    override fun forget(peripheral: BlePeripheral) {
        configCache.remove(peripheral.id)
    }

    private suspend fun <T> execute(
        peripheralId: String,
        block: suspend (GattContext, BleGatewayConfig) -> T
    ): GatewayOutcome<T> = withContext(dispatchers.io) {
        lock.withLock {
            val adapter = bluetoothManager.adapter ?: return@withContext GatewayOutcome.Failure(
                BleGatewayResult.TransportError("蓝牙不可用")
            )
            val device = try {
                adapter.getRemoteDevice(peripheralId)
            } catch (ex: IllegalArgumentException) {
                return@withContext GatewayOutcome.Failure(BleGatewayResult.DeviceMissing(peripheralId))
            } catch (ex: SecurityException) {
                return@withContext GatewayOutcome.Failure(
                    BleGatewayResult.PermissionDenied(
                        setOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        )
                    )
                )
            }

            val callback = GatewayGattCallback()
            val gatt = connect(device, callback)
                ?: return@withContext GatewayOutcome.Failure(BleGatewayResult.TransportError("连接失败"))
            val context = GattContext(gatt, callback)
            try {
                context.awaitServices()
                val config = configCache[peripheralId] ?: discoverConfig(gatt).also {
                    configCache[peripheralId] = it
                    ConnectivityLogger.i(
                        "Discovered BLE service=${it.serviceUuid} write=${it.credentialCharacteristicUuid} status=${it.provisioningStatusCharacteristicUuid} hotspot=${it.hotspotCharacteristicUuid}"
                    )
                }
                context.attachConfig(config)
                context.ensureNotifications(config.provisioningStatusCharacteristicUuid)
                if (config.hotspotCharacteristicUuid != config.provisioningStatusCharacteristicUuid) {
                    context.ensureNotifications(config.hotspotCharacteristicUuid)
                }
                GatewayOutcome.Success(block(context, config))
            } catch (ex: TimeoutCancellationException) {
                GatewayOutcome.Failure(BleGatewayResult.Timeout)
            } catch (ex: SecurityException) {
                GatewayOutcome.Failure(
                    BleGatewayResult.PermissionDenied(
                        setOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        )
                    )
                )
            } catch (ex: CredentialRejectedException) {
                GatewayOutcome.Failure(
                    BleGatewayResult.CredentialRejected(ex.message ?: "凭据被设备拒绝")
                )
            } catch (ex: Exception) {
                GatewayOutcome.Failure(
                    BleGatewayResult.TransportError(ex.message ?: "BLE 传输失败")
                )
            } finally {
                context.close()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connect(
        device: BluetoothDevice,
        callback: GatewayGattCallback
    ): BluetoothGatt? {
        val adapter: BluetoothAdapter = bluetoothManager.adapter ?: return null
        if (!adapter.isEnabled) return null
        val gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        return try {
            withTimeout(DEFAULT_CONNECTION_TIMEOUT_MS) {
                callback.awaitConnection()
            }
            gatt
        } catch (ex: IllegalStateException) {
            ConnectivityLogger.w("BLE 连接失败：${ex.message}")
            gatt.disconnect()
            gatt.close()
            null
        } catch (ex: TimeoutCancellationException) {
            gatt.disconnect()
            gatt.close()
            null
        }
    }

    private fun discoverConfig(gatt: BluetoothGatt): BleGatewayConfig {
        val services = gatt.services
        if (services.isNullOrEmpty()) {
            throw IllegalStateException("设备未暴露任何 BLE 服务")
        }
        val explicit = services.firstNotNullOfOrNull { service ->
            val write = service.characteristics.firstOrNull { it.supportsWrite() } ?: return@firstNotNullOfOrNull null
            val notify = service.characteristics.firstOrNull { it.supportsNotify() }
                ?: service.characteristics.firstOrNull { it.supportsRead() }
                ?: return@firstNotNullOfOrNull null
            val hotspot = service.characteristics
                .firstOrNull { it.supportsRead() && it.uuid != notify.uuid }
                ?: notify
            BleGatewayConfig(
                serviceUuid = service.uuid,
                credentialCharacteristicUuid = write.uuid,
                provisioningStatusCharacteristicUuid = notify.uuid,
                hotspotCharacteristicUuid = hotspot.uuid,
                connectionTimeoutMillis = DEFAULT_CONNECTION_TIMEOUT_MS,
                operationTimeoutMillis = DEFAULT_OPERATION_TIMEOUT_MS
            )
        }
        if (explicit != null) return explicit

        val fallbackService = services.firstOrNull { service ->
            service.characteristics.any { it.supportsWrite() }
        } ?: services.first()
        val writeCharacteristic = fallbackService.characteristics.firstOrNull { it.supportsWrite() }
            ?: throw IllegalStateException("找不到可写入的特征")
        val notifyCharacteristic = fallbackService.characteristics.firstOrNull { it.supportsNotify() }
            ?: fallbackService.characteristics.firstOrNull { it.supportsRead() }
            ?: writeCharacteristic
        val hotspotCharacteristic = fallbackService.characteristics
            .firstOrNull { it.supportsRead() && it.uuid != notifyCharacteristic.uuid }
            ?: notifyCharacteristic
        return BleGatewayConfig(
            serviceUuid = fallbackService.uuid,
            credentialCharacteristicUuid = writeCharacteristic.uuid,
            provisioningStatusCharacteristicUuid = notifyCharacteristic.uuid,
            hotspotCharacteristicUuid = hotspotCharacteristic.uuid,
            connectionTimeoutMillis = DEFAULT_CONNECTION_TIMEOUT_MS,
            operationTimeoutMillis = DEFAULT_OPERATION_TIMEOUT_MS
        )
    }

    private fun buildCredentialsPayload(credentials: WifiCredentials): ByteArray {
        val sanitizedSsid = sanitizeSegment(credentials.ssid)
        val sanitizedPassword = sanitizeSegment(credentials.password)
        val payload = listOf("wifi", "connect", sanitizedSsid, sanitizedPassword).joinToString("#")
        return payload.toByteArray(Charsets.UTF_8)
    }

    private fun buildNetworkQueryPayload(): ByteArray =
        NETWORK_QUERY_COMMAND.toByteArray(Charsets.UTF_8)

    private fun sanitizeSegment(input: String): String = input.replace("#", "-")

    private fun parseProvisioningAck(
        credentials: WifiCredentials,
        payload: ByteArray
    ): BleGatewayResult {
        val raw = payload.decodeToString().trim()
        if (raw.isBlank()) {
            return BleGatewayResult.TransportError("设备未返回确认信息")
        }
        return if (raw.startsWith("{")) {
            parseProvisioningAckJson(credentials, raw)
        } else {
            parseProvisioningAckDelimited(credentials, raw)
        }
    }

    private fun parseProvisioningAckJson(
        credentials: WifiCredentials,
        raw: String
    ): BleGatewayResult = try {
        val json = JSONObject(raw)
        val handshakeId = json.optString("handshake_id", UUID.randomUUID().toString())
        val credentialsHash = json.optString(
            "credentials_hash",
            sha256("${credentials.ssid}${credentials.password}")
        )
        if (json.optBoolean("rejected", false)) {
            BleGatewayResult.CredentialRejected(json.optString("reason", "凭据无效"))
        } else {
            BleGatewayResult.Success(
                handshakeId = handshakeId,
                credentialsHash = credentialsHash
            )
        }
    } catch (ex: Exception) {
        BleGatewayResult.TransportError("解析设备响应失败：${ex.message}")
    }

    private fun parseProvisioningAckDelimited(
        credentials: WifiCredentials,
        raw: String
    ): BleGatewayResult {
        val parts = raw.split("#")
        if (parts.size < 3) {
            return BleGatewayResult.TransportError("BLE 响应格式不正确：$raw")
        }
        val command = parts[0].lowercase()
        val action = parts[1].lowercase()
        if (command != "wifi" || action != "connect") {
            return BleGatewayResult.TransportError("收到未知确认指令：$raw")
        }
        val status = parts[2].lowercase()
        return if (status == "ok" || status == "success" || status == "connected") {
            BleGatewayResult.Success(
                handshakeId = parts.getOrNull(3) ?: UUID.randomUUID().toString(),
                credentialsHash = sha256("${credentials.ssid}${credentials.password}")
            )
        } else {
            BleGatewayResult.CredentialRejected(parts.getOrNull(3) ?: "凭据被设备拒绝：$raw")
        }
    }

    private fun parseHotspot(bytes: ByteArray): WifiCredentials {
        try {
            val json = JSONObject(bytes.decodeToString())
            val ssid = json.optString("ssid")
            val password = json.optString("password")
            if (ssid.isBlank() || password.isBlank()) {
                throw IllegalStateException("热点返回值缺失")
            } else {
                return WifiCredentials(ssid = ssid, password = password)
            }
        } catch (ex: Exception) {
            throw IllegalStateException("热点响应解析失败：${ex.message}")
        }
    }

    private fun parseNetworkResponse(payload: ByteArray): DeviceNetworkStatus {
        val raw = payload.decodeToString().trim()
        if (raw.isBlank()) throw IllegalStateException("网络响应为空")
        return if (raw.startsWith("{")) {
            parseNetworkJson(raw)
        } else {
            parseDelimitedNetworkResponse(raw)
        }
    }

    private fun parseNetworkJson(raw: String): DeviceNetworkStatus {
        return try {
            val json = JSONObject(raw)
            val ip = json.optString("ip", json.optString("ipAddress"))
            val deviceWifi = json.optString("device_wifi", json.optString("deviceName"))
            val phoneWifi = json.optString("phone_wifi", json.optString("phoneName", deviceWifi))
            if (ip.isBlank()) throw IllegalStateException("网络响应缺少 IP")
            val deviceName = deviceWifi.ifBlank { "未知Wi-Fi" }
            val phoneName = phoneWifi.ifBlank { deviceName }
            DeviceNetworkStatus(
                ipAddress = ip,
                deviceWifiName = deviceName,
                phoneWifiName = phoneName,
                rawResponse = raw
            )
        } catch (ex: Exception) {
            throw IllegalStateException("网络响应解析失败：${ex.message}")
        }
    }

    private fun parseDelimitedNetworkResponse(raw: String): DeviceNetworkStatus {
        val parts = raw.split("#")
        if (parts.size < 4) throw IllegalStateException("网络响应格式错误：$raw")
        if (!parts[0].equals("wifi", true) || !parts[1].equals("address", true)) {
            throw IllegalStateException("网络响应类型不符：$raw")
        }
        val ip = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("网络响应缺少 IP：$raw")
        val deviceWifi = parts.getOrNull(3)?.ifBlank { "BT311" } ?: "BT311"
        val phoneWifi = parts.getOrNull(4)?.ifBlank { deviceWifi } ?: deviceWifi
        return DeviceNetworkStatus(
            ipAddress = ip,
            deviceWifiName = deviceWifi,
            phoneWifiName = phoneWifi,
            rawResponse = raw
        )
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

private fun BluetoothGattCharacteristic.supportsWrite(): Boolean =
    (properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0

private fun BluetoothGattCharacteristic.supportsNotify(): Boolean =
    (properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0

private fun BluetoothGattCharacteristic.supportsRead(): Boolean =
    (properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0

@SuppressLint("MissingPermission")
private class GattContext(
    private val gatt: BluetoothGatt,
    private val callback: GatewayGattCallback,
    private var operationTimeoutMillis: Long = DEFAULT_OPERATION_TIMEOUT_MS
) : Closeable {

    private val closed = Mutex(locked = false)
    private val notificationEnabled = mutableSetOf<UUID>()
    private var config: BleGatewayConfig? = null

    fun attachConfig(value: BleGatewayConfig) {
        config = value
        operationTimeoutMillis = value.operationTimeoutMillis
    }

    private fun requireConfig(): BleGatewayConfig =
        config ?: throw IllegalStateException("尚未加载 BLE 配置")

    suspend fun awaitServices() {
        if (!gatt.discoverServices()) throw IllegalStateException("无法启动服务发现")
        withTimeout(operationTimeoutMillis) {
            callback.awaitServiceDiscovery()
        }
    }

    suspend fun ensureNotifications(uuid: UUID) {
        if (notificationEnabled.contains(uuid)) return
        val characteristic = findCharacteristic(uuid)
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            throw IllegalStateException("无法打开通知：$uuid")
        }
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            ?: throw IllegalStateException("找不到通知描述符 $uuid")
        callback.prepareDescriptor(uuid)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        if (!gatt.writeDescriptor(descriptor)) {
            throw IllegalStateException("写入通知描述符失败：$uuid")
        }
        withTimeout(operationTimeoutMillis) {
            callback.awaitDescriptorResult(uuid)
        }
        notificationEnabled.add(uuid)
    }

    suspend fun writeCharacteristic(uuid: UUID, payload: ByteArray) {
        val characteristic = findCharacteristic(uuid)
        callback.prepareWrite(uuid)
        characteristic.value = payload
        if (!gatt.writeCharacteristic(characteristic)) {
            throw IllegalStateException("写入特征失败：$uuid")
        }
        withTimeout(operationTimeoutMillis) {
            callback.awaitWriteResult(uuid)
        }
    }

    suspend fun readCharacteristic(uuid: UUID): ByteArray {
        val characteristic = findCharacteristic(uuid)
        callback.prepareRead(uuid)
        if (!gatt.readCharacteristic(characteristic)) {
            throw IllegalStateException("读取特征失败：$uuid")
        }
        return withTimeout(operationTimeoutMillis) {
            callback.awaitReadResult(uuid)
        }
    }

    suspend fun awaitNotification(uuid: UUID): ByteArray =
        withTimeout(operationTimeoutMillis) {
            callback.awaitNotification(uuid)
        }

    private fun findCharacteristic(uuid: UUID): BluetoothGattCharacteristic {
        val cfg = requireConfig()
        val service = gatt.getService(cfg.serviceUuid)
            ?: throw IllegalStateException("找不到服务 ${cfg.serviceUuid}")
        return service.getCharacteristic(uuid)
            ?: throw IllegalStateException("找不到特征 $uuid")
    }

    override fun close() {
        if (closed.tryLock()) {
            try {
                gatt.disconnect()
                gatt.close()
            } finally {
                closed.unlock()
            }
        }
    }
}

private class GatewayGattCallback : BluetoothGattCallback() {

    private val connectionReady = CompletableDeferred<Boolean>()
    private val servicesReady = CompletableDeferred<Boolean>()
    private val writeResultChannel = Channel<UUID>(Channel.BUFFERED)
    private val readResultChannel = Channel<Pair<UUID, ByteArray>>(Channel.BUFFERED)
    private val descriptorResultChannel = Channel<UUID>(Channel.BUFFERED)
    private val notificationChannel = Channel<Pair<UUID, ByteArray>>(Channel.BUFFERED)
    private var pendingWriteUuid: UUID? = null
    private var pendingReadUuid: UUID? = null
    private var pendingDescriptorUuid: UUID? = null

    suspend fun awaitConnection() {
        val success = connectionReady.await()
        if (!success) throw IllegalStateException("BLE 连接失败")
    }

    suspend fun awaitServiceDiscovery() {
        val success = servicesReady.await()
        if (!success) throw IllegalStateException("服务发现失败")
    }

    fun prepareWrite(uuid: UUID) {
        pendingWriteUuid = uuid
    }

    fun prepareRead(uuid: UUID) {
        pendingReadUuid = uuid
    }

    fun prepareDescriptor(uuid: UUID) {
        pendingDescriptorUuid = uuid
    }

    suspend fun awaitWriteResult(uuid: UUID) {
        val result = writeResultChannel.receive()
        if (result != uuid) throw IllegalStateException("写入确认不匹配 $uuid")
    }

    suspend fun awaitReadResult(uuid: UUID): ByteArray {
        val (characteristicUuid, payload) = readResultChannel.receive()
        if (characteristicUuid != uuid) throw IllegalStateException("读取确认不匹配 $uuid")
        return payload
    }

    suspend fun awaitDescriptorResult(uuid: UUID) {
        val descriptorUuid = descriptorResultChannel.receive()
        if (descriptorUuid != uuid) throw IllegalStateException("通知配置不匹配 $uuid")
    }

    suspend fun awaitNotification(uuid: UUID): ByteArray {
        val (characteristicUuid, payload) = notificationChannel.receive()
        if (characteristicUuid != uuid) throw IllegalStateException("通知来源不匹配 $uuid")
        return payload
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            connectionReady.complete(true)
        } else if (status != BluetoothGatt.GATT_SUCCESS) {
            connectionReady.complete(false)
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        servicesReady.complete(status == BluetoothGatt.GATT_SUCCESS)
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        val expected = pendingWriteUuid
        if (status == BluetoothGatt.GATT_SUCCESS && expected != null) {
            writeResultChannel.trySend(expected)
        } else if (status != BluetoothGatt.GATT_SUCCESS) {
            ConnectivityLogger.w("Characteristic write failed: $status")
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        val expected = pendingReadUuid
        if (status == BluetoothGatt.GATT_SUCCESS && expected != null) {
            readResultChannel.trySend(characteristic.uuid to (characteristic.value ?: byteArrayOf()))
        } else if (status != BluetoothGatt.GATT_SUCCESS) {
            ConnectivityLogger.w("Characteristic read failed: $status")
        }
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        val expected = pendingDescriptorUuid
        if (status == BluetoothGatt.GATT_SUCCESS && expected != null) {
            descriptorResultChannel.trySend(expected)
        } else if (status != BluetoothGatt.GATT_SUCCESS) {
            ConnectivityLogger.w("Descriptor write failed: $status")
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        val value = characteristic.value ?: byteArrayOf()
        notificationChannel.trySend(characteristic.uuid to value)
    }

    private companion object {
        const val TAG = "GattBleGateway"
    }
}

private class CredentialRejectedException(message: String) : RuntimeException(message)

private sealed interface GatewayOutcome<out T> {
    data class Success<T>(val value: T) : GatewayOutcome<T>
    data class Failure(val error: BleGatewayResult) : GatewayOutcome<Nothing>
}

private val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

private suspend fun GattContext.awaitNotificationOrRead(uuid: UUID): ByteArray =
    try {
        awaitNotification(uuid)
    } catch (ex: TimeoutCancellationException) {
        readCharacteristic(uuid)
    }
