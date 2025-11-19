package com.smartsales.feature.connectivity

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.gateway.BleGateway
import com.smartsales.feature.connectivity.gateway.BleGatewayResult
import com.smartsales.feature.connectivity.gateway.HotspotResult
import com.smartsales.feature.connectivity.gateway.NetworkQueryResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/AndroidBleWifiProvisioner.kt
// 模块：:feature:connectivity
// 说明：使用真实 BLE 网关实现 Wi-Fi 配网流程
// 作者：创建于 2025-11-16，重构于 2025-11-19 以支持多 BLE profile
private const val DEFAULT_TIMEOUT_MS = 5_000L

@Singleton
class AndroidBleWifiProvisioner @Inject constructor(
    private val gateway: BleGateway,
    private val dispatchers: DispatcherProvider
) : WifiProvisioner {

    override suspend fun provision(
        session: BleSession,
        credentials: WifiCredentials
    ): Result<ProvisioningStatus> = withContext(dispatchers.io) {
        ConnectivityLogger.i(
            "provision start device=${session.peripheralName} ssid=${credentials.ssid}"
        )
        when (val result = gateway.provision(session, credentials)) {
            is BleGatewayResult.Success -> Result.Success(
                ProvisioningStatus(
                    wifiSsid = credentials.ssid,
                    handshakeId = result.handshakeId,
                    credentialsHash = result.credentialsHash
                )
            )

            is BleGatewayResult.PermissionDenied -> Result.Error(
                ProvisioningException.PermissionDenied(result.permissions)
            )

            is BleGatewayResult.TransportError -> Result.Error(
                ProvisioningException.Transport(result.reason)
            )

            is BleGatewayResult.CredentialRejected -> Result.Error(
                ProvisioningException.CredentialRejected(result.reason)
            )

            BleGatewayResult.Timeout -> Result.Error(
                ProvisioningException.Timeout(DEFAULT_TIMEOUT_MS)
            )
            is BleGatewayResult.DeviceMissing -> Result.Error(
                ProvisioningException.Transport("找不到设备 ${result.peripheralId}")
            )
        }.also { outcome ->
            val status = if (outcome is Result.Success) "success" else "failure"
            ConnectivityLogger.i(
                "provision finished status=$status device=${session.peripheralName}"
            )
        }
    }

    override suspend fun requestHotspotCredentials(session: BleSession): Result<WifiCredentials> =
        withContext(dispatchers.io) {
            ConnectivityLogger.i(
                "hotspot request device=${session.peripheralName}"
            )
            when (val result = gateway.requestHotspot(session)) {
                is HotspotResult.Success -> Result.Success(result.credentials)
                is HotspotResult.PermissionDenied -> Result.Error(
                    ProvisioningException.PermissionDenied(result.permissions)
                )

                is HotspotResult.Timeout -> Result.Error(
                    ProvisioningException.Timeout(result.timeoutMillis)
                )

                is HotspotResult.TransportError -> Result.Error(
                    ProvisioningException.Transport(result.reason)
                )

                is HotspotResult.DeviceMissing -> Result.Error(
                    ProvisioningException.Transport("找不到设备 ${result.peripheralId}")
                )
            }.also { outcome ->
                ConnectivityLogger.d(
                    "hotspot completed status=${outcome is Result.Success}"
                )
            }
        }

    override suspend fun queryNetworkStatus(session: BleSession): Result<DeviceNetworkStatus> =
        withContext(dispatchers.io) {
            ConnectivityLogger.d(
                "network query device=${session.peripheralName}"
            )
            when (val result = gateway.queryNetwork(session)) {
                is NetworkQueryResult.Success -> Result.Success(result.status)
                is NetworkQueryResult.PermissionDenied -> Result.Error(
                    ProvisioningException.PermissionDenied(result.permissions)
                )

                is NetworkQueryResult.Timeout -> Result.Error(
                    ProvisioningException.Timeout(result.timeoutMillis)
                )

                is NetworkQueryResult.TransportError -> Result.Error(
                    ProvisioningException.Transport(result.reason)
                )

                is NetworkQueryResult.DeviceMissing -> Result.Error(
                    ProvisioningException.Transport("找不到设备 ${result.peripheralId}")
                )
            }
        }

}
