package com.smartsales.feature.connectivity.setup

// 文件：feature/connectivity/src/test/java/com/smartsales/feature/connectivity/setup/DeviceSetupViewModelRobustnessTest.kt
// 模块：:feature:connectivity
// 说明：验证设备配网流程的超时、失败与迟到回调健壮性
// 作者：创建于 2025-11-25

import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleProfileConfig
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.connectivity.scan.BleScanner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceSetupViewModelRobustnessTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `scan timeout moves to error with retryable reason`() = runTest(dispatcher) {
        val vm = createViewModel()

        vm.onStartScan()
        advanceTimeBy(13_000)

        val state = vm.uiState.value
        assertEquals(DeviceSetupStep.Error, state.step)
        assertEquals(DeviceSetupErrorReason.ScanTimeout, state.errorReason)
    }

    @Test
    fun `provisioning failure surfaces error reason`() = runTest(dispatcher) {
        val connection = FakeConnectionManager()
        connection.state.value = ConnectionState.Connected(
            session = ConnectionState.Connected.Session(
                peripheralId = "id",
                peripheralName = "dev",
                signalStrengthDbm = -40,
                profileId = "p1"
            ),
            status = ConnectionState.DeviceStatus("wifi", true)
        )
        connection.pairResult = Result.Error(IllegalStateException("fail"))
        val vm = createViewModel(connectionManager = connection)

        vm.onProvisionWifi("ssid", "pwd")

        val state = vm.uiState.value
        assertEquals(DeviceSetupStep.Error, state.step)
        assertEquals(DeviceSetupErrorReason.ProvisioningFailed, state.errorReason)
    }

    @Test
    fun `waiting for device online times out`() = runTest(dispatcher) {
        val connection = FakeConnectionManager()
        connection.state.value = ConnectionState.WifiProvisioned(
            session = ConnectionState.Connected.Session(
                peripheralId = "id",
                peripheralName = "dev",
                signalStrengthDbm = -40,
                profileId = "p1"
            ),
            status = ConnectionState.DeviceStatus("wifi", true)
        )
        connection.queryResult = Result.Error(IllegalStateException("offline"))
        val vm = createViewModel(connectionManager = connection)

        vm.onProvisionWifi("ssid", "pwd")
        advanceTimeBy(5_000)

        val state = vm.uiState.value
        assertEquals(DeviceSetupErrorReason.DeviceNotOnline, state.errorReason)
    }

    @Test
    fun `late callbacks after error do not change state`() = runTest(dispatcher) {
        val connection = FakeConnectionManager()
        val vm = createViewModel(connectionManager = connection)
        vm.onStartScan()
        advanceTimeBy(13_000)
        assertEquals(DeviceSetupStep.Error, vm.uiState.value.step)

        connection.state.value = ConnectionState.WifiProvisioned(
            session = ConnectionState.Connected.Session(
                peripheralId = "id",
                peripheralName = "dev",
                signalStrengthDbm = -40,
                profileId = "p1"
            ),
            status = ConnectionState.DeviceStatus("wifi", true)
        )
        advanceTimeBy(2_000)

        assertEquals(DeviceSetupStep.Error, vm.uiState.value.step)
    }

    private fun createViewModel(
        connectionManager: FakeConnectionManager = FakeConnectionManager(),
        scanner: FakeBleScanner = FakeBleScanner()
    ): DeviceSetupViewModel {
        return DeviceSetupViewModel(
            connectionManager = connectionManager,
            bleScanner = scanner,
            bleProfiles = listOf(BleProfileConfig("p1", "设备"))
        )
    }

    private class FakeBleScanner : BleScanner {
        override val devices: MutableStateFlow<List<BlePeripheral>> = MutableStateFlow(emptyList())
        override val isScanning: MutableStateFlow<Boolean> = MutableStateFlow(false)

        override fun start() {
            isScanning.value = true
        }

        override fun stop() {
            isScanning.value = false
        }
    }

    private class FakeConnectionManager : DeviceConnectionManager {
        override val state: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
        var pairResult: Result<Unit> = Result.Success(Unit)
        var queryResult: Result<DeviceNetworkStatus> = Result.Error(IllegalStateException("offline"))

        override fun selectPeripheral(peripheral: BlePeripheral) = Unit

        override suspend fun startPairing(peripheral: BlePeripheral, credentials: WifiCredentials): Result<Unit> =
            pairResult

        override suspend fun retry(): Result<Unit> = Result.Success(Unit)

        override fun forgetDevice() = Unit

        override suspend fun requestHotspotCredentials(): Result<WifiCredentials> =
            Result.Error(IllegalStateException("no creds"))

        override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> = queryResult
    }
}
