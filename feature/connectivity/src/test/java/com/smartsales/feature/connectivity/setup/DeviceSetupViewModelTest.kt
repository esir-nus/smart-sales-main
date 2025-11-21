package com.smartsales.feature.connectivity.setup

// 文件：feature/connectivity/src/test/java/com/smartsales/feature/connectivity/setup/DeviceSetupViewModelTest.kt
// 模块：:feature:connectivity
// 说明：验证设备配网视图模型的步骤映射与错误重试
// 作者：创建于 2025-11-21

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceSetupViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val connectionManager = FakeDeviceConnectionManager(dispatcher)
    private val viewModel = DeviceSetupViewModel(connectionManager)

    @Test
    fun startScan_updatesScanningStep() = runTest(dispatcher) {
        viewModel.onStartScan()

        assertEquals(DeviceSetupStep.Scanning, viewModel.uiState.value.step)
    }

    @Test
    fun connectionProgress_mapsToReady() = runTest(dispatcher) {
        connectionManager.state.value = ConnectionState.WifiProvisioned(connectionManager.session, connectionManager.provisioning)
        advanceUntilIdle()
        connectionManager.emitNetworkStatus(Result.Success(connectionManager.networkStatus))
        advanceUntilIdle()

        assertEquals(DeviceSetupStep.Ready, viewModel.uiState.value.step)
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

    private class FakeDeviceConnectionManager(private val dispatcher: kotlinx.coroutines.CoroutineDispatcher) : DeviceConnectionManager {
        val session = com.smartsales.feature.connectivity.BleSession.fromPeripheral(
            BlePeripheral(id = "1", name = "设备", signalStrengthDbm = -50)
        )
        val provisioning = com.smartsales.feature.connectivity.ProvisioningStatus("ssid", "handshake", "hash")
        val networkStatus = DeviceNetworkStatus("192.168.0.2", "ssid", "phone", "raw")
        override val state: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
        private var lastNetworkResult: Result<DeviceNetworkStatus> = Result.Error(IllegalStateException("no status"))

        override fun selectPeripheral(peripheral: BlePeripheral) = Unit

        override suspend fun startPairing(peripheral: BlePeripheral, credentials: WifiCredentials): Result<Unit> =
            Result.Success(Unit)

        override suspend fun retry(): Result<Unit> = Result.Success(Unit)

        override fun forgetDevice() = Unit

        override suspend fun requestHotspotCredentials(): Result<WifiCredentials> =
            Result.Success(WifiCredentials("ssid", "pwd"))

        override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> = lastNetworkResult

        fun emitNetworkStatus(result: Result<DeviceNetworkStatus>) {
            lastNetworkResult = result
        }
    }
}
