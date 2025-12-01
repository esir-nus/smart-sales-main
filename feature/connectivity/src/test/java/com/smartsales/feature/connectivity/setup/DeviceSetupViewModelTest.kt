package com.smartsales.feature.connectivity.setup

// 文件：feature/connectivity/src/test/java/com/smartsales/feature/connectivity/setup/DeviceSetupViewModelTest.kt
// 模块：:feature:connectivity
// 说明：验证设备配网视图模型的步骤映射与错误重试
// 作者：创建于 2025-11-30

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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceSetupViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val connectionManager = FakeDeviceConnectionManager()
    private val scanner = FakeBleScanner()
    private val profiles = listOf(BleProfileConfig(id = "bt311", displayName = "BT311", nameKeywords = emptyList(), scanServiceUuids = emptyList()))
    private lateinit var viewModel: DeviceSetupViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        viewModel = DeviceSetupViewModel(connectionManager, scanner, profiles)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun autoScan_then_found_then_wifi_input() = runTest(dispatcher) {
        dispatcher.scheduler.runCurrent()
        assertEquals(DeviceSetupStep.Scanning, viewModel.uiState.value.step)
        assertEquals(SetupStep.Scanning, viewModel.uiState.value.setupStep)
        assertTrue(viewModel.uiState.value.isScanning)
        assertTrue(viewModel.uiState.value.canRetryScan.not())

        scanner.emitDevice(BlePeripheral("1", "BT311", -50))
        dispatcher.scheduler.runCurrent()
        assertEquals(DeviceSetupStep.Found, viewModel.uiState.value.step)
        assertEquals(SetupStep.ConnectingBle, viewModel.uiState.value.setupStep)

        viewModel.onPrimaryClick() // 配置网络 -> Wi-Fi 输入
        dispatcher.scheduler.runCurrent()
        assertEquals(DeviceSetupStep.WifiInput, viewModel.uiState.value.step)
        assertEquals(SetupStep.WifiForm, viewModel.uiState.value.setupStep)
    }

    @Test
    fun connectionProgress_mapsToReady() = runTest(dispatcher) {
        connectionManager.state.value = ConnectionState.WifiProvisioned(connectionManager.session, connectionManager.provisioning)
        connectionManager.emitNetworkStatus(Result.Success(connectionManager.networkStatus))
        advanceUntilIdle()

        assertEquals(DeviceSetupStep.Ready, viewModel.uiState.value.step)
        assertEquals(SetupStep.Completed, viewModel.uiState.value.setupStep)
        assertEquals("192.168.0.2", viewModel.uiState.value.deviceIp)
        connectionManager.emitNetworkStatus(Result.Error(IllegalStateException("offline")))
        advanceUntilIdle()
        assertEquals(DeviceSetupStep.Ready, viewModel.uiState.value.step)
        assertEquals(SetupStep.Completed, viewModel.uiState.value.setupStep)
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
    fun wifi_input_requires_credentials() = runTest(dispatcher) {
        connectionManager.state.value = ConnectionState.Connected(connectionManager.session)
        advanceUntilIdle()

        assertEquals(DeviceSetupStep.WifiInput, viewModel.uiState.value.step)
        assertTrue(viewModel.uiState.value.isPrimaryEnabled.not())

        viewModel.onWifiSsidChanged("ssid")
        viewModel.onWifiPasswordChanged("pwd")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isPrimaryEnabled)
    }

    @Test
    fun retry_resetsToScanning() = runTest(dispatcher) {
        viewModel.onRetry()

        assertEquals(DeviceSetupStep.Scanning, viewModel.uiState.value.step)
        assertEquals(SetupStep.Scanning, viewModel.uiState.value.setupStep)
        assertEquals("", viewModel.uiState.value.wifiPassword)
    }

    @Test
    fun scan_start_failure_allows_retry() = runTest(dispatcher) {
        val failingScanner = FakeBleScanner().apply { startSucceeds = false }
        viewModel = DeviceSetupViewModel(connectionManager, failingScanner, profiles)
        dispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(DeviceSetupStep.Scanning, state.step)
        assertTrue(state.canRetryScan)
        assertTrue(state.isScanning.not())
    }

    @Test
    fun setupStep_maps_from_deviceSetupStep() = runTest(dispatcher) {
        dispatcher.scheduler.runCurrent()
        assertEquals(SetupStep.Scanning, viewModel.uiState.value.setupStep)

        scanner.emitDevice(BlePeripheral("3", "BT311", -55))
        dispatcher.scheduler.runCurrent()
        assertEquals(SetupStep.ConnectingBle, viewModel.uiState.value.setupStep)

        viewModel.onPrimaryClick()
        dispatcher.scheduler.runCurrent()
        assertEquals(SetupStep.WifiForm, viewModel.uiState.value.setupStep)
    }

    @Test
    fun scan_allows_needsSetup_state() = runTest(dispatcher) {
        val connection = FakeDeviceConnectionManager()
        connection.state.value = ConnectionState.NeedsSetup
        val vm = DeviceSetupViewModel(connection, scanner, profiles)
        dispatcher.scheduler.runCurrent()

        scanner.emitDevice(BlePeripheral("2", "BT311-2", -60))
        dispatcher.scheduler.runCurrent()

        assertEquals(DeviceSetupStep.Found, vm.uiState.value.step)
        assertEquals(SetupStep.ConnectingBle, vm.uiState.value.setupStep)
    }

    @Test
    fun scan_timeout_enables_retry() = runTest(dispatcher) {
        dispatcher.scheduler.runCurrent()
        advanceUntilIdle()

        advanceUntilIdle()
        dispatcher.scheduler.advanceTimeBy(12_500)
        dispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(DeviceSetupStep.Scanning, state.step)
        assertTrue(state.canRetryScan)
        assertTrue(state.isScanning.not())
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

        override fun scheduleAutoReconnectIfNeeded() {}
        override fun forceReconnectNow() {}
    }

    private class FakeBleScanner : BleScanner {
        override val devices: MutableStateFlow<List<BlePeripheral>> = MutableStateFlow(emptyList())
        override val isScanning: MutableStateFlow<Boolean> = MutableStateFlow(false)
        var startSucceeds: Boolean = true

        override fun start() {
            isScanning.value = startSucceeds
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
