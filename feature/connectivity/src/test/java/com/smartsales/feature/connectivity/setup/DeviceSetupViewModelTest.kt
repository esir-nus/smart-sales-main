package com.smartsales.feature.connectivity.setup

// 文件：feature/connectivity/src/test/java/com/smartsales/feature/connectivity/setup/DeviceSetupViewModelTest.kt
// 模块：:feature:connectivity
// 说明：验证设备配网视图模型的步骤映射与错误重试
// 作者：创建于 2025-11-21

import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleProfileConfig
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.connectivity.scan.BleScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceSetupViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var connectionManager: FakeDeviceConnectionManager
    private lateinit var scanner: FakeBleScanner
    private lateinit var viewModel: DeviceSetupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        connectionManager = FakeDeviceConnectionManager()
        scanner = FakeBleScanner()
        viewModel = DeviceSetupViewModel(
            connectionManager,
            scanner,
            listOf(BleProfileConfig(id = "bt311", displayName = "BT311", nameKeywords = emptyList(), scanServiceUuids = emptyList()))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startScan_updatesScanningStep() = runTest(dispatcher) {
        viewModel.onStartScan()

        assertEquals(DeviceSetupStep.Scanning, viewModel.uiState.value.step)
        scanner.emitDevice(BlePeripheral("1", "BT311", -50))
        advanceUntilIdle()
        assertEquals(DeviceSetupStep.Pairing, viewModel.uiState.value.step)
    }

    @Test
    fun connectionProgress_mapsToReady() = runTest(dispatcher) {
        connectionManager.state.value = ConnectionState.WifiProvisioned(connectionManager.session, connectionManager.provisioning)
        connectionManager.emitNetworkStatus(Result.Success(connectionManager.networkStatus))
        advanceUntilIdle()

        assertEquals(DeviceSetupStep.Ready, viewModel.uiState.value.step)
        assertEquals("192.168.0.2", viewModel.uiState.value.deviceIp)
        connectionManager.emitNetworkStatus(Result.Error(IllegalStateException("offline")))
        advanceUntilIdle()
        assertEquals(DeviceSetupStep.Ready, viewModel.uiState.value.step)
        assertEquals("192.168.0.2", viewModel.uiState.value.deviceIp)
    }

    @Test
    fun error_setsErrorMessage() = runTest(dispatcher) {
        connectionManager.state.value = ConnectionState.Error(ConnectivityError.Timeout(1_000))
        advanceUntilIdle()

        assertEquals(DeviceSetupStep.Error, viewModel.uiState.value.step)
        assertTrue(viewModel.uiState.value.errorMessage?.isNotBlank() == true)
    }

    @Test
    fun retry_resetsToScanning() = runTest(dispatcher) {
        viewModel.onRetry()

        assertEquals(DeviceSetupStep.Scanning, viewModel.uiState.value.step)
    }

    private class FakeDeviceConnectionManager : DeviceConnectionManager {
        val session = com.smartsales.feature.connectivity.BleSession.fromPeripheral(
            BlePeripheral(id = "1", name = "设备", signalStrengthDbm = -50)
        )
        val provisioning = com.smartsales.feature.connectivity.ProvisioningStatus("ssid", "handshake", "hash")
        val networkStatus = DeviceNetworkStatus("192.168.0.2", "ssid", "phone", "raw")
        override val state: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
        private var lastNetworkResult: Result<DeviceNetworkStatus> = Result.Error(IllegalStateException("no status"))
        var queryCount = 0

        override fun selectPeripheral(peripheral: BlePeripheral) {
            state.value = ConnectionState.Connected(
                session.copy(
                    peripheralId = peripheral.id,
                    peripheralName = peripheral.name,
                    signalStrengthDbm = peripheral.signalStrengthDbm,
                    profileId = peripheral.profileId
                )
            )
        }

        override suspend fun startPairing(peripheral: BlePeripheral, credentials: WifiCredentials): Result<Unit> =
            Result.Success(Unit).also {
                state.value = ConnectionState.WifiProvisioned(
                    session.copy(
                        peripheralId = peripheral.id,
                        peripheralName = peripheral.name,
                        signalStrengthDbm = peripheral.signalStrengthDbm,
                        profileId = peripheral.profileId
                    ),
                    provisioning
                )
            }

        override suspend fun retry(): Result<Unit> = Result.Success(Unit)

        override fun forgetDevice() = Unit

        override suspend fun requestHotspotCredentials(): Result<WifiCredentials> =
            Result.Success(WifiCredentials("ssid", "pwd"))

        override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> {
            queryCount += 1
            return lastNetworkResult
        }

        fun emitNetworkStatus(result: Result<DeviceNetworkStatus>) {
            lastNetworkResult = result
        }
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

        fun emitDevice(peripheral: BlePeripheral) {
            devices.value = listOf(peripheral)
            isScanning.value = false
        }
    }
}
