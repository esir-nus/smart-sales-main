package com.smartsales.feature.connectivity.setup

import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.connectivity.scan.BleScanner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// 文件：feature/connectivity/src/test/java/com/smartsales/feature/connectivity/setup/DeviceSetupViewModelTest.kt
// 模块：:feature:connectivity
// 说明：覆盖 DeviceSetupViewModel 的扫描与配网状态机
// 作者：创建于 2025-11-20
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceSetupViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var fakeScanner: FakeBleScanner
    private lateinit var fakeManager: FakeDeviceConnectionManager
    private lateinit var viewModel: DeviceSetupViewModel

    @Before
    fun setup() {
        fakeScanner = FakeBleScanner()
        fakeManager = FakeDeviceConnectionManager()
        viewModel = DeviceSetupViewModel(fakeManager, fakeScanner)
    }

    @Test
    fun `start scan populates devices`() = runTest(dispatcher) {
        viewModel.onStartScan()
        fakeScanner.emitDevices(listOf(BlePeripheral("1", "BT311", -50)))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DeviceSetupStep.SELECT_DEVICE, state.step)
        assertEquals(1, state.devices.size)
        assertEquals(true, state.isScanning)
    }

    @Test
    fun `select device advances to EnterWifi`() = runTest(dispatcher) {
        viewModel.onStartScan()
        fakeScanner.emitDevices(listOf(BlePeripheral("dev-1", "BT311", -45)))
        advanceUntilIdle()

        viewModel.onSelectDevice("dev-1")

        val state = viewModel.uiState.value
        assertEquals(DeviceSetupStep.ENTER_WIFI, state.step)
        assertEquals("dev-1", state.selectedDeviceId)
    }

    @Test
    fun `submit wifi success completes flow`() = runTest(dispatcher) {
        val device = BlePeripheral("dev-2", "BT311", -40)
        fakeScanner.emitDevices(listOf(device))
        viewModel.onSelectDevice("dev-2")
        viewModel.onWifiSsidChanged("OfficeWiFi")
        viewModel.onWifiPasswordChanged("password123")
        fakeManager.startResult = Result.Success(Unit)
        fakeManager.networkResult = Result.Success(
            DeviceNetworkStatus("192.168.1.88", "OfficeWiFi", "OfficeWiFi", "raw")
        )

        viewModel.onSubmitWifiCredentials()
        fakeManager.emitState(ConnectionState.Pairing(device.name, 20, -40))
        fakeManager.emitState(
            ConnectionState.WifiProvisioned(
                session = com.smartsales.feature.connectivity.BleSession.fromPeripheral(device),
                status = com.smartsales.feature.connectivity.ProvisioningStatus("OfficeWiFi", "h1", "hash")
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DeviceSetupStep.COMPLETED, state.step)
        assertEquals(false, state.isSubmitting)
        assertEquals("192.168.1.88", state.networkStatus?.ipAddress)
    }

    @Test
    fun `submit wifi failure exposes error and retry resets`() = runTest(dispatcher) {
        val device = BlePeripheral("dev-3", "BT311", -42)
        fakeScanner.emitDevices(listOf(device))
        viewModel.onSelectDevice("dev-3")
        viewModel.onWifiSsidChanged("CafeWiFi")
        viewModel.onWifiPasswordChanged("password123")
        fakeManager.startResult = Result.Error(IllegalStateException("credentials rejected"))

        viewModel.onSubmitWifiCredentials()
        advanceUntilIdle()

        val failedState = viewModel.uiState.value
        assertEquals(DeviceSetupStep.FAILED, failedState.step)
        assertEquals("credentials rejected", failedState.errorMessage)

        viewModel.onRetry()
        val retryState = viewModel.uiState.value
        assertEquals(DeviceSetupStep.ENTER_WIFI, retryState.step)
        assertNull(retryState.errorMessage)
    }

    private class FakeBleScanner : BleScanner {
        private val _devices = MutableStateFlow<List<BlePeripheral>>(emptyList())
        private val _isScanning = MutableStateFlow(false)
        override val devices: StateFlow<List<BlePeripheral>> = _devices
        override val isScanning: StateFlow<Boolean> = _isScanning

        override fun start() {
            _isScanning.value = true
        }

        override fun stop() {
            _isScanning.value = false
        }

        fun emitDevices(devices: List<BlePeripheral>) {
            _devices.value = devices
        }
    }

    private class FakeDeviceConnectionManager : DeviceConnectionManager {
        private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        override val state: StateFlow<ConnectionState> = _state
        var startResult: Result<Unit> = Result.Success(Unit)
        var networkResult: Result<DeviceNetworkStatus> = Result.Error(IllegalStateException("未查询"))

        override fun selectPeripheral(peripheral: BlePeripheral) = Unit
        override suspend fun startPairing(peripheral: BlePeripheral, credentials: WifiCredentials): Result<Unit> = startResult
        override suspend fun retry(): Result<Unit> = Result.Success(Unit)
        override fun forgetDevice() = Unit
        override suspend fun requestHotspotCredentials(): Result<WifiCredentials> = Result.Error(IllegalStateException())
        override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> = networkResult

        fun emitState(state: ConnectionState) {
            _state.value = state
        }
    }
}
