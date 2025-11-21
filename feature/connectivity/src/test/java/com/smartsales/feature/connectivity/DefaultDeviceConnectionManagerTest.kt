package com.smartsales.feature.connectivity

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 文件路径: feature/connectivity/src/test/java/com/smartsales/feature/connectivity/DefaultDeviceConnectionManagerTest.kt
// 文件作用: 验证默认连接管理器的状态流与重试逻辑
// 最近修改: 2025-11-14
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDeviceConnectionManagerTest {

    private val peripheral = BlePeripheral(id = "device-1", name = "Demo Device", signalStrengthDbm = -55)
    private val credentials = WifiCredentials(ssid = "SalesWiFi", password = "password123")

    @Test
    fun `selectPeripheral exposes connected state`() = runTest {
        val manager = createManager(QueueProvisioner(mutableListOf()))

        manager.selectPeripheral(peripheral)

        val state = manager.state.value
        assertTrue(state is ConnectionState.Connected)
        val session = (state as ConnectionState.Connected).session
        assertEquals(peripheral.name, session.peripheralName)
    }

    @Test
    fun `startPairing emits provisioning and syncing states`() = runTest {
        val provisionStatus = ProvisioningStatus("SalesWiFi", "handshake-1", "hash")
        val provisioner = QueueProvisioner(
            mutableListOf(Result.Success(provisionStatus))
        )
        val manager = createManager(provisioner)

        val result = manager.startPairing(peripheral, credentials)
        assertTrue(result is Result.Success)
        runCurrent()
        assertTrue(manager.state.value is ConnectionState.WifiProvisioned)

        advanceTimeBy(HEARTBEAT_INTERVAL_MS)
        runCurrent()
        assertTrue(manager.state.value is ConnectionState.Syncing)
    }

    @Test
    fun `retry reuses last attempt after failure`() = runTest {
        val provisionStatus = ProvisioningStatus("SalesWiFi", "handshake-2", "hash")
        val provisioner = QueueProvisioner(
            mutableListOf(
                Result.Error(IllegalStateException("pairing failed")),
                Result.Success(provisionStatus)
            )
        )
        val manager = createManager(provisioner)

        manager.startPairing(peripheral, credentials)
        runCurrent()
        assertTrue(manager.state.value is ConnectionState.Error)

        val retryResult = manager.retry()
        assertTrue(retryResult is Result.Success)
        runCurrent()
        assertTrue(manager.state.value is ConnectionState.WifiProvisioned)
    }

    @Test
    fun `requestHotspotCredentials fails without session`() = runTest {
        val manager = createManager(QueueProvisioner(mutableListOf()))
        val result = manager.requestHotspotCredentials()
        assertTrue(result is Result.Error)
    }

    @Test
    fun `permission error surfaces without auto retry`() = runTest {
        val provisioner = QueueProvisioner(
            mutableListOf(
                Result.Error(ProvisioningException.PermissionDenied(setOf("BLUETOOTH_SCAN")))
            )
        )
        val manager = createManager(provisioner)

        manager.startPairing(peripheral, credentials)
        runCurrent()
        val state = manager.state.value
        assertTrue(state is ConnectionState.Error)
        val error = (state as ConnectionState.Error).error
        assertTrue(error is ConnectivityError.PermissionDenied)

        advanceTimeBy(AUTO_RETRY_DELAY_MS)
        runCurrent()
        assertTrue(manager.state.value is ConnectionState.Error)
    }

    @Test
    fun `timeout error triggers auto retry`() = runTest {
        val provisionStatus = ProvisioningStatus("SalesWiFi", "handshake-3", "hash")
        val provisioner = QueueProvisioner(
            mutableListOf(
                Result.Error(ProvisioningException.Timeout(1_000L)),
                Result.Success(provisionStatus)
            )
        )
        val manager = createManager(provisioner)

        manager.startPairing(peripheral, credentials)
        runCurrent()
        assertTrue(manager.state.value is ConnectionState.Error)

        advanceTimeBy(AUTO_RETRY_DELAY_MS)
        runCurrent()
        assertTrue(manager.state.value is ConnectionState.WifiProvisioned)
    }

    @Test
    fun `queryNetworkStatus fails without session`() = runTest {
        val manager = createManager(QueueProvisioner(mutableListOf()))

        val result = manager.queryNetworkStatus()

        assertTrue(result is Result.Error)
    }

    @Test
    fun `queryNetworkStatus returns provisioner data`() = runTest {
        val networkStatus = DeviceNetworkStatus(
            ipAddress = "192.168.1.10",
            deviceWifiName = "BT311",
            phoneWifiName = "DemoPhone",
            rawResponse = "wifi#address#192.168.1.10#BT311#DemoPhone"
        )
        val provisionStatus = ProvisioningStatus("SalesWiFi", "handshake-4", "hash")
        val provisioner = QueueProvisioner(
            mutableListOf(Result.Success(provisionStatus)),
            networkResult = Result.Success(networkStatus)
        )
        val manager = createManager(provisioner)

        manager.startPairing(peripheral, credentials)
        runCurrent()

        val result = manager.queryNetworkStatus()

        assertTrue(result is Result.Success)
        assertEquals(networkStatus, (result as Result.Success).data)
        val state = manager.state.value
        assertTrue(state is ConnectionState.WifiProvisioned || state is ConnectionState.Syncing)
    }

    @Test
    fun `queryNetworkStatus after manual connect marks ready`() = runTest {
        val networkStatus = DeviceNetworkStatus(
            ipAddress = "192.168.1.30",
            deviceWifiName = "SalesWiFi",
            phoneWifiName = "DemoPhone",
            rawResponse = "wifi#address#192.168.1.30#SalesWiFi#DemoPhone"
        )
        val provisioner = QueueProvisioner(
            mutableListOf(),
            networkResult = Result.Success(networkStatus)
        )
        val manager = createManager(provisioner)

        manager.selectPeripheral(peripheral)
        val result = manager.queryNetworkStatus()

        assertTrue(result is Result.Success)
        val state = manager.state.value
        assertTrue(state is ConnectionState.WifiProvisioned || state is ConnectionState.Syncing)
    }

    private class QueueProvisioner(
        private val results: MutableList<Result<ProvisioningStatus>>,
        private val hotspotResult: Result<WifiCredentials> = Result.Success(
            WifiCredentials(ssid = "Hotspot", password = "87654321")
        ),
        private val networkResult: Result<DeviceNetworkStatus> = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.1.20",
                deviceWifiName = "BT311",
                phoneWifiName = "DemoPhone",
                rawResponse = "wifi#address#192.168.1.20#BT311#DemoPhone"
            )
        )
    ) : WifiProvisioner {
        override suspend fun provision(session: BleSession, credentials: WifiCredentials): Result<ProvisioningStatus> {
            if (results.isEmpty()) return Result.Error(IllegalStateException("No provision result configured"))
            return results.removeAt(0)
        }

        override suspend fun requestHotspotCredentials(session: BleSession): Result<WifiCredentials> =
            hotspotResult

        override suspend fun queryNetworkStatus(session: BleSession): Result<DeviceNetworkStatus> =
            networkResult
    }

    private fun TestScope.createManager(
        provisioner: WifiProvisioner,
        dispatcher: TestDispatcher = StandardTestDispatcher(testScheduler)
    ): DefaultDeviceConnectionManager {
        val dispatcherProvider = FakeDispatcherProvider(dispatcher)
        return DefaultDeviceConnectionManager(
            provisioner = provisioner,
            dispatchers = dispatcherProvider,
            externalScope = backgroundScope
        )
    }

    private companion object {
        const val HEARTBEAT_INTERVAL_MS = 1_500L
        const val AUTO_RETRY_DELAY_MS = 2_000L
    }
}
