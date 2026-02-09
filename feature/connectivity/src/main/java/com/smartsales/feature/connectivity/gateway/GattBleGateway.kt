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
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.ConnectivityLogger
import com.smartsales.feature.connectivity.ConnectivityScope
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import java.io.Closeable
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.annotation.RequiresApi
import android.bluetooth.BluetoothStatusCodes

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
    private val dispatchers: DispatcherProvider,
    @ConnectivityScope private val scope: CoroutineScope
) : BleGateway, GattSessionLifecycle {
    
    // 原有的事务锁（用于短期操作）
    private val lock = Mutex()
    private val configCache = mutableMapOf<String, BleGatewayConfig>()
    
    // 持久化会话状态
    private var persistentSession: GattContext? = null
    private val sessionLock = Mutex()

    private val _badgeNotifications = MutableSharedFlow<BadgeNotification>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    
   /**
     * 建立持久 GATT 连接。
     * 如果已连接，则返回成功。
     */
    override suspend fun connect(peripheralId: String): com.smartsales.core.util.Result<Unit> = withContext(dispatchers.io) {
        sessionLock.withLock {
            if (persistentSession != null) {
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
            
            val callback = GatewayGattCallback(_badgeNotifications, onDisconnect = { cb ->
                scope.launch(dispatchers.io) {
                    sessionLock.withLock {
                        if (persistentSession?.callback === cb) {
                            persistentSession = null
                            ConnectivityLogger.w("⚠️ Unexpected GATT disconnect -> Zombie session cleared")
                        }
                    }
                }
            }, onTimeSync = {
                scope.launch(dispatchers.io) {
                    respondToTimeSync()
                }
            })
            val gatt = connect(device, callback)
                ?: return@withContext com.smartsales.core.util.Result.Error(
                    IllegalStateException("BLE 连接失败")
                )
            
            val gattContext = GattContext(context, gatt, callback)
            try {
                gattContext.awaitServices()
                
                // 诊断：转储所有 BLE 服务/特征/描述符，验证 CCCD 支持
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
                                ConnectivityLogger.d("    📋 Descriptor: ${desc.uuid}${if (isCccd) " ← CCCD ✅" else ""}")
                            }
                        }
                    }
                }
                
                val config = configCache[peripheralId] ?: discoverConfig(gatt).also {
                    configCache[peripheralId] = it
                }
                gattContext.attachConfig(config)
                gattContext.ensureNotifications(config.provisioningStatusCharacteristicUuid)
                if (config.hotspotCharacteristicUuid != config.provisioningStatusCharacteristicUuid) {
                    gattContext.ensureNotifications(config.hotspotCharacteristicUuid)
                }
                
                persistentSession = gattContext
                ConnectivityLogger.i("🔌 Persistent GATT session established: $peripheralId")
                com.smartsales.core.util.Result.Success(Unit)
            } catch (ex: Exception) {
                gattContext.close()
                com.smartsales.core.util.Result.Error(ex)
            }
        }
    }
    
    /**
     * 断开持久 GATT 连接。
     */
    override suspend fun disconnect() = sessionLock.withLock {
        persistentSession?.close()
        persistentSession = null
        ConnectivityLogger.d("🔌 Persistent GATT session closed")
    }
    
    /**
     * 监听来自徽章的所有通知（tim#, log# 等）。
     * 此流在整个连接生命周期内持续发送事件。
     */
    override fun listenForBadgeNotifications(): Flow<BadgeNotification> = _badgeNotifications.asSharedFlow()

    override suspend fun provision(
        session: BleSession,
        credentials: WifiCredentials
    ): BleGatewayResult =
        when (val outcome = execute(session.peripheralId) { gattContext, config ->
            // ESP32 two-step WiFi provisioning protocol:
            // Step 1: SD#<ssid>  (Send SSID)
            // Step 2: PD#<password>  (Send Password)
            val ssidPayload = buildSsidPayload(credentials.ssid)
            val passwordPayload = buildPasswordPayload(credentials.password)
            
            ConnectivityLogger.i("BLE Provision: Step 1 - sending SSID: ${ssidPayload.decodeToString()}")
            ConnectivityLogger.tx("SSID", ssidPayload)
            gattContext.writeCharacteristic(config.credentialCharacteristicUuid, ssidPayload)
            
            // Delay between commands for ESP32 to process SSID before password
            // 300ms is more reliable than 100ms based on field testing
            kotlinx.coroutines.delay(300)
            
            ConnectivityLogger.i("BLE Provision: Step 2 - sending password: PD#****")
            ConnectivityLogger.tx("Password", passwordPayload)
            gattContext.writeCharacteristic(config.credentialCharacteristicUuid, passwordPayload)
            
            // Wait for badge to connect and respond
            val ackBytes = gattContext.awaitNotificationOrRead(config.provisioningStatusCharacteristicUuid)
            ConnectivityLogger.rx("ProvisionAck", ackBytes)
            
            // tim#get 作为 ProvisionAck 时由 parseProvisioningAckDelimited 处理（L615）
            // 时间同步写回由 onCharacteristicChanged → respondToTimeSync 自动处理
            
            parseProvisioningAck(credentials, ackBytes)
            // NOTE: pollForConnection workaround removed — it bypassed rate limiter and caused ESP32 freezes.
            // If firmware returns "failed", the user can retry provisioning.
        }) {
            is GatewayOutcome.Success -> outcome.value
            is GatewayOutcome.Failure -> outcome.error
        }

    override suspend fun requestHotspot(
        session: BleSession
    ): HotspotResult =
        when (val outcome = execute(session.peripheralId) { gattContext, config ->
            val response = gattContext.readCharacteristic(config.hotspotCharacteristicUuid)
            ConnectivityLogger.rx("Hotspot", response)
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
            ConnectivityLogger.tx("NetworkQuery", command)
            gattContext.writeCharacteristic(config.credentialCharacteristicUuid, command)
            
            // Badge 回复 wifi 查询时分多个 BLE 通知发送（IP#, SD# 等）
            // 需要收集所有片段后合并解析
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
                    // 非目标特征的通知或 CCCD 不支持等异常
                    ConnectivityLogger.d("⏭️ Fragment skipped: ${ex.message}")
                    null
                }
                if (response == null) return@repeat
                
                val raw = response.decodeToString().trim()
                ConnectivityLogger.rx("NetworkResponse", response)
                rawResponses.add(raw)
                // 拿到 IP 就可以停了（SD 等为补充信息）
                if (raw.startsWith("IP#", ignoreCase = true)) return@repeat
            }
            
            mergeNetworkFragments(rawResponses)
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


    override suspend fun sendWavCommand(
        session: BleSession,
        command: WavCommand
    ): WavCommandResult =
        when (val outcome = execute(session.peripheralId) { gattContext, config ->
            val payload = command.blePayload.toByteArray(Charsets.UTF_8)
            gattContext.writeCharacteristic(config.credentialCharacteristicUuid, payload)
            val response = gattContext.awaitNotificationOrRead(config.provisioningStatusCharacteristicUuid)
            parseWavResponse(command, response)
        }) {
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



    private fun formatCurrentTime(): String {
        val sdf = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }

    // 持久会话空闲时响应 Badge 的时间同步请求（tim#get → tim#YYYYMMDDHHMMSS）
    // tim#get 响应：独立处理，不与配网耦合。BLE 写串行化由 GattContext.writeLock 保证
    private suspend fun respondToTimeSync() {
        try {
            val session = persistentSession ?: return
            val config = configCache.values.firstOrNull() ?: return
            val response = "tim#${formatCurrentTime()}"
            session.writeCharacteristic(config.credentialCharacteristicUuid, response.toByteArray(Charsets.UTF_8))
            ConnectivityLogger.i("⏰ Time sync responded: $response")
        } catch (e: Exception) {
            ConnectivityLogger.w("⏰ Time sync response failed (non-fatal): ${e.message}")
        }
    }

    override fun forget(peripheral: BlePeripheral) {
        configCache.remove(peripheral.id)
    }

    private suspend fun <T> execute(
        peripheralId: String,
        block: suspend (GattContext, BleGatewayConfig) -> T
    ): GatewayOutcome<T> = withContext(dispatchers.io) {
        // 优先使用持久化会话
        sessionLock.withLock {
            val session = persistentSession
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
        
        // 降级模式：一次性连接（用于首次配网或无持久会话时）
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

            val callback = GatewayGattCallback()  // 临时连接不广播通知
            val gatt = connect(device, callback)
                ?: return@withContext GatewayOutcome.Failure(BleGatewayResult.TransportError("连接失败"))
            val gattContext = GattContext(context, gatt, callback)
            try {
                gattContext.awaitServices()
                val config = configCache[peripheralId] ?: discoverConfig(gatt).also {
                    configCache[peripheralId] = it
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

    /**
     * Build SSID payload for ESP32 two-step WiFi provisioning.
     * Format: SD#<ssid>
     */
    private fun buildSsidPayload(ssid: String): ByteArray {
        val sanitized = sanitizeSegment(ssid)
        return "SD#$sanitized".toByteArray(Charsets.UTF_8)
    }

    /**
     * Build password payload for ESP32 two-step WiFi provisioning.
     * Format: PD#<password>
     */
    private fun buildPasswordPayload(password: String): ByteArray {
        val sanitized = sanitizeSegment(password)
        return "PD#$sanitized".toByteArray(Charsets.UTF_8)
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
        
        // Badge sends "tim#get" to request time sync
        // During provisioning, this indicates badge is alive and proceeding
        val command = parts.getOrNull(0)?.lowercase() ?: ""
        if (command == "tim") {
            ConnectivityLogger.i("Badge requested time sync during provisioning ($raw), treating as success")
            return BleGatewayResult.Success(
                handshakeId = UUID.randomUUID().toString(),
                credentialsHash = sha256("${credentials.ssid}${credentials.password}")
            )
        }
        
        if (parts.size < 3) {
            return BleGatewayResult.TransportError("BLE 响应格式不正确：$raw")
        }
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


    private fun parseWavResponse(command: WavCommand, payload: ByteArray): WavCommandResult {
        val raw = payload.decodeToString().trim()
        if (raw.isBlank()) throw IllegalStateException("WAV响应为空")
        
        val parts = raw.split("#")
        if (parts.size < 2) throw IllegalStateException("WAV响应格式错误：$raw")
        
        // Expected responses: "wav#send" or "wav#ok"
        if (!parts[0].equals("wav", true)) {
            throw IllegalStateException("WAV响应类型不符：$raw")
        }
        
        return when (parts[1].lowercase()) {
            "send" -> WavCommandResult.Ready
            "ok" -> WavCommandResult.Done
            else -> throw IllegalStateException("未知WAV响应：$raw")
        }
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
    private val appContext: Context,
    internal val gatt: BluetoothGatt,  // internal for persistent session checks
    internal val callback: GatewayGattCallback, // internal for zombie session check
    private var operationTimeoutMillis: Long = DEFAULT_OPERATION_TIMEOUT_MS
) : Closeable {

    private val closed = java.util.concurrent.atomic.AtomicBoolean(false)
    private val notificationEnabled = mutableSetOf<UUID>()
    internal var config: BleGatewayConfig? = null  // internal for persistent session access

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
            return // Gracefully skip - will use polling instead
        }
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (descriptor == null) {
            // Badge doesn't have CCCD descriptor - notifications not supported
            // This is OK, we'll fall back to polling/read in awaitNotificationOrRead
            ConnectivityLogger.d("No CCCD descriptor for $uuid, notifications not available (will poll)")
            return
        }
        callback.prepareDescriptor(uuid)
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        if (!gatt.writeDescriptorCompat(appContext, descriptor)) {
            ConnectivityLogger.w("Failed to write CCCD for $uuid, will use polling")
            return // Gracefully skip
        }
        withTimeout(operationTimeoutMillis) {
            callback.awaitDescriptorResult(uuid)
        }
        notificationEnabled.add(uuid)
    }

    // 所有 BLE 写操作通过 writeLock 串行化，防止并发写入导致 GATT 操作失败
    private val writeLock = kotlinx.coroutines.sync.Mutex()

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

private class GatewayGattCallback(
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
        // 跳过非目标 UUID 的通知（它们已通过 SharedFlow 广播，不会丢失）
        // 外层总有 withTimeout 保证不会无限循环
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

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        val value = characteristic.legacyValue() ?: byteArrayOf()
        ConnectivityLogger.rx("Notification", value)
        
        // 保留原有的 Channel 发送（向后兼容）
        notificationChannel.trySend(characteristic.uuid to value)
        
        // 解析并广播到 SharedFlow（如果已注入）
        badgeNotifications?.let { flow ->
            val raw = value.decodeToString().trim()
            when {
                raw.startsWith("tim#get", ignoreCase = true) -> {
                    flow.tryEmit(BadgeNotification.TimeSyncRequested)
                    onTimeSync()
                }
                raw.startsWith("log#", ignoreCase = true) -> {
                    val filename = raw.removePrefix("log#").trim()
                    flow.tryEmit(BadgeNotification.RecordingReady(filename))
                }
                raw.isNotBlank() -> {
                    flow.tryEmit(BadgeNotification.Unknown(raw))
                }
                else -> { /* ignore empty notifications */ }
            }
        }
    }

    private companion object {
        const val TAG = "GattBleGateway"
    }
}

/**
 * 将多个 BLE 通知片段（如 "IP#192.168.0.1", "SD#MstRobot"）合并为 DeviceNetworkStatus。
 * 
 * 纯函数，方便单元测试。Badge 回复 WiFi 查询时分多条 BLE 通知发送。
 */
internal fun mergeNetworkFragments(rawResponses: List<String>): DeviceNetworkStatus {
    val fragments = mutableMapOf<String, String>()
    for (raw in rawResponses) {
        val parts = raw.split("#", limit = 2)
        if (parts.size == 2) {
            fragments[parts[0].uppercase()] = parts[1]
        }
    }
    
    val ip = fragments["IP"]
        ?: throw IllegalStateException("网络响应缺少 IP（收到: ${fragments.keys}）")
    val ssid = fragments["SD"] ?: "未知Wi-Fi"
    
    if (ip == "0.0.0.0" || ip.isBlank()) {
        throw IllegalStateException("设备未连接WiFi：IP=$ip")
    }
    
    return DeviceNetworkStatus(
        ipAddress = ip,
        deviceWifiName = ssid,
        phoneWifiName = ssid,
        rawResponse = fragments.entries.joinToString(", ") { "${it.key}#${it.value}" }
    )
}

private class CredentialRejectedException(message: String) : RuntimeException(message)

private sealed interface GatewayOutcome<out T> {
    data class Success<T>(val value: T) : GatewayOutcome<T>
    data class Failure(val error: BleGatewayResult) : GatewayOutcome<Nothing>
}

private val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

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
        // Badge doesn't support notifications (no CCCD descriptor), fall back to read
        ConnectivityLogger.d("Notification not supported for $uuid (${ex.message}), falling back to read")
        readCharacteristic(uuid)
    }

private fun Context.hasBleConnectPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED
}
