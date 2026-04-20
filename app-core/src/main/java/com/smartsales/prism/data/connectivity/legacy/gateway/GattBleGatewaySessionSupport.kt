package com.smartsales.prism.data.connectivity.legacy.gateway

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
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.ConnectivityLogger
import com.smartsales.prism.data.connectivity.legacy.WifiCredentials
import java.io.Closeable
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val DEFAULT_CONNECTION_TIMEOUT_MS = 10_000L
private const val DEFAULT_OPERATION_TIMEOUT_MS = 5_000L
private const val RSSI_TIMEOUT_MS = 3_000L

internal class GattBleGatewaySessionSupport(
    private val context: Context,
    private val bluetoothManager: BluetoothManager,
    private val dispatchers: DispatcherProvider,
    private val scope: CoroutineScope,
    private val runtime: GattBleGatewayRuntime,
    private val protocolSupport: GattBleGatewayProtocolSupport
) {

    suspend fun connect(peripheralId: String): com.smartsales.core.util.Result<Unit> = withContext(dispatchers.io) {
        runtime.sessionLock.withLock {
            if (runtime.persistentSession != null) {
                return@withContext com.smartsales.core.util.Result.Success(Unit)
            }

            val adapter = bluetoothManager.adapter
                ?: return@withContext com.smartsales.core.util.Result.Error(
                    IllegalStateException("蓝牙不可用")
                )

            val device = try {
                adapter.getRemoteDevice(peripheralId)
            } catch (ex: IllegalArgumentException) {
                return@withContext com.smartsales.core.util.Result.Error(
                    IllegalStateException("无效的设备ID: $peripheralId")
                )
            }

            val callback = GatewayGattCallback(
                badgeNotifications = runtime.badgeNotifications,
                onDisconnect = { disconnectedCallback ->
                    scope.launch(dispatchers.io) {
                        runtime.sessionLock.withLock {
                            if (runtime.persistentSession?.callback === disconnectedCallback) {
                                runtime.persistentSession = null
                                ConnectivityLogger.w("⚠️ Unexpected GATT disconnect -> Zombie session cleared")
                            }
                        }
                    }
                },
                onTimeSync = {
                    scope.launch(dispatchers.io) {
                        respondToTimeSync()
                    }
                }
            )
            val gatt = connectGattDevice(device, callback)
                ?: return@withContext com.smartsales.core.util.Result.Error(
                    IllegalStateException("BLE 连接失败")
                )

            val gattContext = GattContext(context, gatt, callback)
            try {
                gattContext.awaitServices()
                gatt.services.forEach { service ->
                    ConnectivityLogger.d("📋 Service: ${service.uuid}")
                    service.characteristics.forEach { char ->
                        val props = char.properties
                        val propStr = buildString {
                            if (props and 0x02 != 0) append("READ ")
                            if (props and 0x08 != 0) append("WRITE ")
                            if (props and 0x04 != 0) append("WRITE_NR ")
                            if (props and 0x10 != 0) append("NOTIFY ")
                            if (props and 0x20 != 0) append("INDICATE ")
                        }
                        ConnectivityLogger.d("  📋 Char: ${char.uuid} [$propStr]")
                        if (char.descriptors.isEmpty()) {
                            ConnectivityLogger.d("    📋 (no descriptors)")
                        } else {
                            char.descriptors.forEach { desc ->
                                val isCccd = desc.uuid.toString().startsWith("00002902")
                                ConnectivityLogger.d(
                                    "    📋 Descriptor: ${desc.uuid}${if (isCccd) " ← CCCD ✅" else ""}"
                                )
                            }
                        }
                    }
                }

                val config = runtime.configCache[peripheralId] ?: discoverConfig(gatt).also {
                    runtime.configCache[peripheralId] = it
                }
                gattContext.attachConfig(config)
                gattContext.ensureNotifications(config.provisioningStatusCharacteristicUuid)
                if (config.hotspotCharacteristicUuid != config.provisioningStatusCharacteristicUuid) {
                    gattContext.ensureNotifications(config.hotspotCharacteristicUuid)
                }

                runtime.persistentSession = gattContext
                ConnectivityLogger.i("🔌 Persistent GATT session established: $peripheralId")
                com.smartsales.core.util.Result.Success(Unit)
            } catch (ex: Exception) {
                gattContext.close()
                com.smartsales.core.util.Result.Error(ex)
            }
        }
    }

    suspend fun disconnect() {
        runtime.sessionLock.withLock {
            runtime.persistentSession?.close()
            runtime.persistentSession = null
            ConnectivityLogger.d("🔌 Persistent GATT session closed")
        }
    }

    fun listenForBadgeNotifications(): Flow<BadgeNotification> = runtime.badgeNotifications.asSharedFlow()

    @SuppressLint("MissingPermission")
    suspend fun isReachable(): Boolean = withContext(dispatchers.io) {
        runtime.sessionLock.withLock {
            val session = runtime.persistentSession ?: return@withContext false
            try {
                session.callback.prepareRssi()
                if (!session.gatt.readRemoteRssi()) return@withContext false
                withTimeout(RSSI_TIMEOUT_MS) {
                    session.callback.awaitRssiResult()
                }
            } catch (_: TimeoutCancellationException) {
                false
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun provision(
        session: BleSession,
        credentials: WifiCredentials
    ): BleGatewayResult = when (
        val outcome = execute(session.peripheralId) { gattContext, config ->
            protocolSupport.dispatchProvisioningCredentials(
                credentials = credentials,
                sendSsid = { ssidPayload ->
                    ConnectivityLogger.i("BLE Provision: Step 1 - sending SSID: ${ssidPayload.decodeToString()}")
                    ConnectivityLogger.tx("SSID", ssidPayload)
                    gattContext.writeCharacteristic(config.credentialCharacteristicUuid, ssidPayload)
                },
                sendPassword = { passwordPayload ->
                    ConnectivityLogger.i("BLE Provision: Step 2 - sending password: PD#****")
                    ConnectivityLogger.tx("Password", passwordPayload)
                    gattContext.writeCharacteristic(config.credentialCharacteristicUuid, passwordPayload)
                }
            )
        }
    ) {
        is GatewayOutcome.Success -> outcome.value
        is GatewayOutcome.Failure -> outcome.error
    }

    suspend fun requestHotspot(session: BleSession): HotspotResult = when (
        val outcome = execute(session.peripheralId) { gattContext, config ->
            val response = gattContext.readCharacteristic(config.hotspotCharacteristicUuid)
            ConnectivityLogger.rx("Hotspot", response)
            protocolSupport.parseHotspot(response)
        }
    ) {
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

    suspend fun queryNetwork(session: BleSession): NetworkQueryResult = when (
        val outcome = execute(session.peripheralId) { gattContext, config ->
            val command = protocolSupport.buildNetworkQueryPayload()
            ConnectivityLogger.tx("NetworkQuery", command)
            gattContext.writeCharacteristic(config.credentialCharacteristicUuid, command)

            val rawResponses = mutableListOf<String>()
            val maxFragments = 3
            val fragmentTimeout = 2000L

            repeat(maxFragments) {
                val response = try {
                    kotlinx.coroutines.withTimeout(fragmentTimeout) {
                        gattContext.awaitNotification(config.provisioningStatusCharacteristicUuid)
                    }
                } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                    null
                } catch (ex: IllegalStateException) {
                    ConnectivityLogger.d("⏭️ Fragment skipped: ${ex.message}")
                    null
                }
                if (response == null) return@repeat

                val raw = response.decodeToString().trim()
                ConnectivityLogger.rx("NetworkResponse", response)
                rawResponses.add(raw)
                if (raw.startsWith("IP#", ignoreCase = true)) return@repeat
            }

            mergeNetworkFragments(rawResponses)
        }
    ) {
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

    suspend fun sendWavCommand(
        session: BleSession,
        command: WavCommand
    ): WavCommandResult = when (
        val outcome = execute(session.peripheralId) { gattContext, config ->
            val payload = command.blePayload.toByteArray(Charsets.UTF_8)
            gattContext.writeCharacteristic(config.credentialCharacteristicUuid, payload)
            val response = gattContext.awaitNotificationOrRead(config.provisioningStatusCharacteristicUuid)
            protocolSupport.parseWavResponse(response)
        }
    ) {
        is GatewayOutcome.Success -> outcome.value
        is GatewayOutcome.Failure -> when (val error = outcome.error) {
            is BleGatewayResult.PermissionDenied -> WavCommandResult.PermissionDenied(error.permissions)
            BleGatewayResult.Timeout -> WavCommandResult.Timeout(DEFAULT_OPERATION_TIMEOUT_MS)
            is BleGatewayResult.TransportError -> WavCommandResult.TransportError(error.reason)
            is BleGatewayResult.CredentialRejected -> WavCommandResult.TransportError(error.reason)
            is BleGatewayResult.DeviceMissing -> WavCommandResult.DeviceMissing(error.peripheralId)
            is BleGatewayResult.Success -> WavCommandResult.TransportError("无效错误类型")
        }
    }

    fun forget(peripheral: BlePeripheral) {
        runtime.configCache.remove(peripheral.id)
    }

    /**
     * 直接向徽章写入一段 ASCII 命令（如 "commandend#1"）。
     * 不等待通知响应，失败时抛出异常由调用方处理。
     */
    suspend fun sendBadgeSignal(session: BleSession, payload: String) {
        val outcome = execute(session.peripheralId) { gattContext, config ->
            val bytes = payload.toByteArray(Charsets.US_ASCII)
            ConnectivityLogger.tx("BadgeSignal", bytes)
            gattContext.writeCharacteristic(config.credentialCharacteristicUuid, bytes)
        }
        when (outcome) {
            is GatewayOutcome.Success -> Unit
            is GatewayOutcome.Failure -> throw IllegalStateException(
                "徽章信号写入失败: ${outcome.error}"
            )
        }
    }

    private suspend fun respondToTimeSync() {
        try {
            val session = runtime.persistentSession ?: return
            val config = runtime.configCache.values.firstOrNull() ?: return
            val response = "tim#${protocolSupport.formatCurrentTime()}"
            session.writeCharacteristic(
                config.credentialCharacteristicUuid,
                response.toByteArray(Charsets.UTF_8)
            )
            ConnectivityLogger.i("⏰ Time sync responded: $response")
        } catch (e: Exception) {
            ConnectivityLogger.w("⏰ Time sync response failed (non-fatal): ${e.message}")
        }
    }

    private suspend fun <T> execute(
        peripheralId: String,
        block: suspend (GattContext, BleGatewayConfig) -> T
    ): GatewayOutcome<T> = withContext(dispatchers.io) {
        runtime.sessionLock.withLock {
            val session = runtime.persistentSession
            if (session != null && session.gatt.device.address == peripheralId) {
                return@withContext try {
                    val config = session.config
                        ?: return@withContext GatewayOutcome.Failure(
                            BleGatewayResult.TransportError("会话配置未加载")
                        )
                    GatewayOutcome.Success(block(session, config))
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
                }
            }
        }

        runtime.lock.withLock {
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
            val gatt = connectGattDevice(device, callback)
                ?: return@withContext GatewayOutcome.Failure(BleGatewayResult.TransportError("连接失败"))
            val gattContext = GattContext(context, gatt, callback)
            try {
                gattContext.awaitServices()
                val config = runtime.configCache[peripheralId] ?: discoverConfig(gatt).also {
                    runtime.configCache[peripheralId] = it
                    ConnectivityLogger.i(
                        "Discovered BLE service=${it.serviceUuid} write=${it.credentialCharacteristicUuid} status=${it.provisioningStatusCharacteristicUuid} hotspot=${it.hotspotCharacteristicUuid}"
                    )
                }
                gattContext.attachConfig(config)
                gattContext.ensureNotifications(config.provisioningStatusCharacteristicUuid)
                if (config.hotspotCharacteristicUuid != config.provisioningStatusCharacteristicUuid) {
                    gattContext.ensureNotifications(config.hotspotCharacteristicUuid)
                }
                GatewayOutcome.Success(block(gattContext, config))
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
                gattContext.close()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectGattDevice(
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
            val write = service.characteristics.firstOrNull { it.supportsWrite() }
                ?: return@firstNotNullOfOrNull null
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
}

private sealed interface GatewayOutcome<out T> {
    data class Success<T>(val value: T) : GatewayOutcome<T>
    data class Failure(val error: BleGatewayResult) : GatewayOutcome<Nothing>
}

private val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

@SuppressLint("MissingPermission")
internal class GattContext(
    private val appContext: Context,
    internal val gatt: BluetoothGatt,
    internal val callback: GatewayGattCallback,
    private var operationTimeoutMillis: Long = DEFAULT_OPERATION_TIMEOUT_MS
) : Closeable {

    private val closed = java.util.concurrent.atomic.AtomicBoolean(false)
    private val notificationEnabled = mutableSetOf<UUID>()
    internal var config: BleGatewayConfig? = null
    private val writeLock = kotlinx.coroutines.sync.Mutex()

    fun attachConfig(value: BleGatewayConfig) {
        config = value
        operationTimeoutMillis = value.operationTimeoutMillis
    }

    private fun ensureConnectPermission() {
        if (!appContext.hasBleConnectPermission()) {
            throw SecurityException("缺少 BLUETOOTH_CONNECT 权限")
        }
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
        ensureConnectPermission()
        val characteristic = findCharacteristic(uuid)
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            ConnectivityLogger.w("Cannot enable notifications for $uuid, will use polling")
            return
        }
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (descriptor == null) {
            ConnectivityLogger.d("No CCCD descriptor for $uuid, notifications not available (will poll)")
            return
        }
        callback.prepareDescriptor(uuid)
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        if (!gatt.writeDescriptorCompat(appContext, descriptor)) {
            ConnectivityLogger.w("Failed to write CCCD for $uuid, will use polling")
            return
        }
        withTimeout(operationTimeoutMillis) {
            callback.awaitDescriptorResult(uuid)
        }
        notificationEnabled.add(uuid)
    }

    suspend fun writeCharacteristic(uuid: UUID, payload: ByteArray) {
        writeLock.withLock {
            ensureConnectPermission()
            val characteristic = findCharacteristic(uuid)
            callback.prepareWrite(uuid)
            if (!gatt.writeCharacteristicCompat(appContext, characteristic, payload)) {
                throw IllegalStateException("写入特征失败：$uuid")
            }
            withTimeout(operationTimeoutMillis) {
                callback.awaitWriteResult(uuid)
            }
        }
    }

    suspend fun readCharacteristic(uuid: UUID): ByteArray {
        ensureConnectPermission()
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
        if (closed.compareAndSet(false, true)) {
            gatt.disconnect()
            gatt.close()
        }
    }
}

internal class GatewayGattCallback(
    private val badgeNotifications: MutableSharedFlow<BadgeNotification>? = null,
    private val onDisconnect: (GatewayGattCallback) -> Unit = {},
    private val onTimeSync: () -> Unit = {}
) : BluetoothGattCallback() {

    private val connectionReady = CompletableDeferred<Boolean>()
    private val servicesReady = CompletableDeferred<Boolean>()
    private val writeResultChannel = Channel<UUID>(Channel.BUFFERED)
    private val readResultChannel = Channel<Pair<UUID, ByteArray>>(Channel.BUFFERED)
    private val descriptorResultChannel = Channel<UUID>(Channel.BUFFERED)
    private val notificationChannel = Channel<Pair<UUID, ByteArray>>(Channel.BUFFERED)
    private var pendingWriteUuid: UUID? = null
    private var pendingReadUuid: UUID? = null
    private var pendingDescriptorUuid: UUID? = null
    private var rssiDeferred: CompletableDeferred<Boolean>? = null

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

    fun prepareRssi() {
        rssiDeferred = CompletableDeferred()
    }

    suspend fun awaitRssiResult(): Boolean {
        return rssiDeferred?.await() ?: false
    }

    suspend fun awaitNotification(uuid: UUID): ByteArray {
        while (true) {
            val (characteristicUuid, payload) = notificationChannel.receive()
            if (characteristicUuid == uuid) return payload
            ConnectivityLogger.d("⏭️ Skipped notification from $characteristicUuid (waiting for $uuid)")
        }
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            connectionReady.complete(true)
        } else if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
            connectionReady.complete(false)
            onDisconnect(this)
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
            readResultChannel.trySend(characteristic.uuid to (characteristic.legacyValue() ?: byteArrayOf()))
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

    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        rssiDeferred?.complete(status == BluetoothGatt.GATT_SUCCESS)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        val value = characteristic.legacyValue() ?: byteArrayOf()
        ConnectivityLogger.rx("Notification", value)
        notificationChannel.trySend(characteristic.uuid to value)
        badgeNotifications?.let { flow ->
            when (val notification = parseBadgeNotificationPayload(value.decodeToString())) {
                is BadgeNotification.TimeSyncRequested -> {
                    flow.tryEmit(BadgeNotification.TimeSyncRequested)
                    onTimeSync()
                }
                is BadgeNotification.RecordingReady -> flow.tryEmit(notification)
                is BadgeNotification.AudioRecordingReady -> flow.tryEmit(notification)
                is BadgeNotification.Unknown -> flow.tryEmit(notification)
                null -> Unit
            }
        }
    }
}

private fun BluetoothGattCharacteristic.supportsWrite(): Boolean =
    (properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0

private fun BluetoothGattCharacteristic.supportsNotify(): Boolean =
    (properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0

private fun BluetoothGattCharacteristic.supportsRead(): Boolean =
    (properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0

@SuppressLint("MissingPermission")
private fun BluetoothGatt.writeDescriptorCompat(
    context: Context,
    descriptor: BluetoothGattDescriptor
): Boolean {
    if (!context.hasBleConnectPermission()) {
        throw SecurityException("缺少 BLUETOOTH_CONNECT 权限")
    }
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            writeDescriptor(descriptor)
        } else {
            @Suppress("DEPRECATION")
            writeDescriptor(descriptor)
        }
    }.getOrElse { throwable ->
        ConnectivityLogger.w("Descriptor write aborted：${throwable.message}")
        false
    }
}

@SuppressLint("MissingPermission")
private fun BluetoothGatt.writeCharacteristicCompat(
    context: Context,
    characteristic: BluetoothGattCharacteristic,
    payload: ByteArray
): Boolean {
    if (!context.hasBleConnectPermission()) {
        throw SecurityException("缺少 BLUETOOTH_CONNECT 权限")
    }
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            writeCharacteristic(
                characteristic,
                payload,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.legacySetValue(payload)
            @Suppress("DEPRECATION")
            writeCharacteristic(characteristic)
        }
    }.getOrElse { throwable ->
        ConnectivityLogger.w("Characteristic write aborted：${throwable.message}")
        false
    }
}

private fun BluetoothGattCharacteristic.legacyValue(): ByteArray? {
    @Suppress("DEPRECATION")
    return value
}

private fun BluetoothGattCharacteristic.legacySetValue(payload: ByteArray): Boolean {
    @Suppress("DEPRECATION")
    return setValue(payload)
}

private suspend fun GattContext.awaitNotificationOrRead(uuid: UUID): ByteArray =
    try {
        awaitNotification(uuid)
    } catch (ex: TimeoutCancellationException) {
        ConnectivityLogger.d("Notification timeout for $uuid, falling back to read")
        readCharacteristic(uuid)
    } catch (ex: IllegalStateException) {
        ConnectivityLogger.d("Notification not supported for $uuid (${ex.message}), falling back to read")
        readCharacteristic(uuid)
    }

private fun Context.hasBleConnectPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED
}
