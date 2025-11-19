package com.smartsales.feature.connectivity

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.gateway.BleGateway
import com.smartsales.feature.connectivity.gateway.BleGatewayResult
import com.smartsales.feature.connectivity.gateway.HotspotResult
import com.smartsales.feature.connectivity.gateway.NetworkQueryResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 文件：feature/connectivity/src/test/java/com/smartsales/feature/connectivity/AndroidBleWifiProvisionerTest.kt
// 模块：:feature:connectivity
// 说明：验证真实 BLE Provisioner 的错误映射逻辑
// 作者：创建于 2025-11-16
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidBleWifiProvisionerTest {

    private val session = BleSession.fromPeripheral(
        BlePeripheral(
            id = "AA:BB:CC:DD",
            name = "Device-X",
            signalStrengthDbm = -50,
            profileId = "bt311"
        )
    )
    private val credentials = WifiCredentials(ssid = "Office", password = "strongPass12")

    @Test
    fun provisionSuccessReturnsStatus() = runTest {
        val gateway = RecordingGateway()
        gateway.provisionResult = BleGatewayResult.Success(
            handshakeId = "handshake-123",
            credentialsHash = "hash-456"
        )
        val provisioner = createProvisioner(gateway)

        val result = provisioner.provision(session, credentials)

        assertTrue(result is Result.Success)
        val status = (result as Result.Success).data
        assertEquals("Office", status.wifiSsid)
        assertEquals("handshake-123", status.handshakeId)
    }

    @Test
    fun provisionFailureMapsPermissionDenied() = runTest {
        val gateway = RecordingGateway()
        gateway.provisionResult = BleGatewayResult.PermissionDenied(setOf("BLUETOOTH_CONNECT"))
        val provisioner = createProvisioner(gateway)

        val result = provisioner.provision(session, credentials)

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable
        assertTrue(error is ProvisioningException.PermissionDenied)
    }

    @Test
    fun hotspotSuccessPassesThroughCredentials() = runTest {
        val gateway = RecordingGateway()
        val hotspotCredentials = WifiCredentials(ssid = "SmartSales", password = "87654321")
        gateway.hotspotResult = HotspotResult.Success(hotspotCredentials)
        val provisioner = createProvisioner(gateway)

        val result = provisioner.requestHotspotCredentials(session)

        assertTrue(result is Result.Success)
        assertEquals(hotspotCredentials, (result as Result.Success).data)
    }

    @Test
    fun hotspotTimeoutReturnsError() = runTest {
        val gateway = RecordingGateway()
        gateway.hotspotResult = HotspotResult.Timeout(3_000L)
        val provisioner = createProvisioner(gateway)

        val result = provisioner.requestHotspotCredentials(session)

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable
        assertTrue(error is ProvisioningException.Timeout)
    }

    @Test
    fun queryNetworkStatusSuccess() = runTest {
        val gateway = RecordingGateway()
        val status = DeviceNetworkStatus(
            ipAddress = "192.168.1.30",
            deviceWifiName = "BT311",
            phoneWifiName = "DemoPhone",
            rawResponse = "wifi#address#192.168.1.30#BT311#DemoPhone"
        )
        gateway.networkResult = NetworkQueryResult.Success(status)
        val provisioner = createProvisioner(gateway)

        val result = provisioner.queryNetworkStatus(session)

        assertTrue(result is Result.Success)
        assertEquals(status, (result as Result.Success).data)
    }

    @Test
    fun queryNetworkStatusTimeoutReturnsError() = runTest {
        val gateway = RecordingGateway()
        gateway.networkResult = NetworkQueryResult.Timeout(2_000L)
        val provisioner = createProvisioner(gateway)

        val result = provisioner.queryNetworkStatus(session)

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable
        assertTrue(error is ProvisioningException.Timeout)
    }

    private fun TestScope.createProvisioner(gateway: RecordingGateway): AndroidBleWifiProvisioner {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val provider = FakeDispatcherProvider(dispatcher)
        return AndroidBleWifiProvisioner(gateway, provider)
    }

    private class RecordingGateway : BleGateway {
        var provisionResult: BleGatewayResult = BleGatewayResult.TransportError("not configured")
        var hotspotResult: HotspotResult = HotspotResult.TransportError("not configured")
        var networkResult: NetworkQueryResult = NetworkQueryResult.TransportError("not configured")

        override suspend fun provision(
            session: BleSession,
            credentials: WifiCredentials
        ): BleGatewayResult = provisionResult

        override suspend fun requestHotspot(session: BleSession): HotspotResult = hotspotResult

        override suspend fun queryNetwork(session: BleSession): NetworkQueryResult = networkResult

        override fun forget(peripheral: BlePeripheral) = Unit
    }
}
