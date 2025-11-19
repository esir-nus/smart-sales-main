package com.smartsales.feature.connectivity

import com.smartsales.core.util.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import kotlin.random.Random
import com.smartsales.core.util.DispatcherProvider

// 文件路径: feature/connectivity/src/main/java/com/smartsales/feature/connectivity/SimulatedWifiProvisioner.kt
// 文件作用: 提供一个用于离线环境的Wi-Fi配网模拟器
// 最近修改: 2025-11-14
@Singleton
class SimulatedWifiProvisioner @Inject constructor(
    private val dispatchers: DispatcherProvider
) : WifiProvisioner {

    private val sessionCredentials = mutableMapOf<String, WifiCredentials>()

    override suspend fun provision(
        session: BleSession,
        credentials: WifiCredentials
    ): Result<ProvisioningStatus> = withContext(dispatchers.io) {
        delay(PROVISIONING_DELAY_MS)
        if (credentials.password.length < MIN_PASSWORD_LENGTH) {
            return@withContext Result.Error(
                ProvisioningException.CredentialRejected("Wi-Fi 密码至少需要 $MIN_PASSWORD_LENGTH 位")
            )
        }

        val hashSource = "${credentials.ssid}${credentials.password}${session.secureToken}"
        Result.Success(
            ProvisioningStatus(
                wifiSsid = credentials.ssid,
                handshakeId = hashSource.hashCode().toString(),
                credentialsHash = hashSource.hashCode().absoluteValue.toString()
            )
        ).also {
            sessionCredentials[session.peripheralId] = credentials
        }
    }

    override suspend fun requestHotspotCredentials(session: BleSession): Result<WifiCredentials> =
        withContext(dispatchers.io) {
            delay(HOTSPOT_DELAY_MS)
            Result.Success(
                WifiCredentials(
                    ssid = "SmartSales-${session.peripheralName.take(6)}",
                    password = Random(session.peripheralName.hashCode()).nextInt(10000000, 99999999).toString()
                )
            )
        }

    override suspend fun queryNetworkStatus(session: BleSession): Result<DeviceNetworkStatus> =
        withContext(dispatchers.io) {
            delay(NETWORK_QUERY_DELAY_MS)
            val random = Random(session.peripheralName.hashCode())
            val ip = "192.168.50.${random.nextInt(10, 200)}"
            val credentials = sessionCredentials[session.peripheralId]
            val deviceWifi = credentials?.ssid?.ifBlank { null }
                ?: "BT311-${session.peripheralName.takeLast(2)}"
            val phoneWifi = "" // 真实手机 Wi-Fi 由 PhoneWifiMonitor 负责
            Result.Success(
                DeviceNetworkStatus(
                    ipAddress = ip,
                    deviceWifiName = deviceWifi,
                    phoneWifiName = phoneWifi,
                    rawResponse = "wifi#address#$ip#$deviceWifi#$phoneWifi"
                )
            )
        }

    private companion object {
        const val PROVISIONING_DELAY_MS = 500L
        const val HOTSPOT_DELAY_MS = 250L
        const val MIN_PASSWORD_LENGTH = 8
        const val NETWORK_QUERY_DELAY_MS = 400L
    }
}
