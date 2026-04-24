// File: app-core/src/test/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModelRepairTest.kt
// Module: :app-core
// Summary: Wi-Fi 修复状态机相关 ViewModel 测试
// Author: created on 2026-04-17

package com.smartsales.prism.ui.components.connectivity

import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.registry.DeviceRegistryManager
import com.smartsales.prism.data.connectivity.registry.RegisteredDevice
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.ConnectivityService
import com.smartsales.prism.domain.connectivity.ReconnectResult
import com.smartsales.prism.domain.connectivity.RecordingNotification
import com.smartsales.prism.domain.connectivity.UpdateResult
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import com.smartsales.prism.domain.connectivity.WifiConfigResult
import com.smartsales.prism.domain.connectivity.WifiRepairEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityViewModelRepairTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        service: ConnectivityService = StubConnectivityService(),
        bridge: StubRepairBridge = StubRepairBridge(),
    ) = ConnectivityViewModel(
        connectivityService = service,
        connectivityBridge = bridge,
        registryManager = NoOpDeviceRegistryManager(),
        promptCoordinator = ConnectivityPromptCoordinator()
    )

    @Test
    fun `repairState transitions to HttpDelayed when service returns TransportConfirmedHttpDelayed`() = runTest {
        val bridge = StubRepairBridge()
        val service = StubConnectivityService(
            updateResult = WifiConfigResult.TransportConfirmedHttpDelayed(
                badgeSsid = "OfficeGuest",
                baseUrl = "http://192.168.0.9:8088"
            )
        )
        val viewModel = createViewModel(service = service, bridge = bridge)
        advanceUntilIdle()

        viewModel.updateWifiConfig(ssid = "OfficeGuest", password = "secret")
        advanceUntilIdle()

        val state = viewModel.repairState.value
        assertEquals(
            WifiRepairState.HttpDelayed(
                badgeSsid = "OfficeGuest",
                baseUrl = "http://192.168.0.9:8088"
            ),
            state
        )
        // effectiveState 保持 WIFI_MISMATCH — 修复表单区域仍展示
        assertEquals(ConnectionState.WIFI_MISMATCH, viewModel.effectiveState.value)
    }

    @Test
    fun `repairState goes to Idle when service returns Success`() = runTest {
        val service = StubConnectivityService(updateResult = WifiConfigResult.Success)
        val viewModel = createViewModel(service = service)
        advanceUntilIdle()

        viewModel.updateWifiConfig(ssid = "OfficeGuest", password = "secret")
        advanceUntilIdle()

        assertEquals(WifiRepairState.Idle, viewModel.repairState.value)
    }

    @Test
    fun `repairState goes to RetryableFailure when service returns Error`() = runTest {
        val service = StubConnectivityService(
            updateResult = WifiConfigResult.Error("密码错误，请重新输入")
        )
        val viewModel = createViewModel(service = service)
        advanceUntilIdle()

        viewModel.updateWifiConfig(ssid = "OfficeGuest", password = "wrongpass")
        advanceUntilIdle()

        assertEquals(
            WifiRepairState.RetryableFailure("密码错误，请重新输入"),
            viewModel.repairState.value
        )
    }

    @Test
    fun `repairState goes to HardFailure SSID_MISMATCH when bridge emits DefinitiveMismatch`() = runTest {
        val bridge = StubRepairBridge()
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        bridge.emitRepairEvent(
            WifiRepairEvent.DefinitiveMismatch(
                expectedSsid = "OfficeGuest",
                badgeSsid = "OtherWifi"
            )
        )
        advanceUntilIdle()

        assertEquals(
            WifiRepairState.HardFailure(WifiRepairState.HardFailure.HardFailureReason.SSID_MISMATCH),
            viewModel.repairState.value
        )
    }

    @Test
    fun `repairState goes to HardFailure SUSPECTED_ISOLATION and badgeIp set when isolation prompt fires`() = runTest {
        val coordinator = ConnectivityPromptCoordinator()
        val viewModel = ConnectivityViewModel(
            connectivityService = StubConnectivityService(),
            connectivityBridge = StubRepairBridge(),
            registryManager = NoOpDeviceRegistryManager(),
            promptCoordinator = coordinator
        )
        advanceUntilIdle()

        coordinator.promptSuspectedIsolation("192.168.1.18")
        advanceUntilIdle()

        assertEquals(
            WifiRepairState.HardFailure(WifiRepairState.HardFailure.HardFailureReason.SUSPECTED_ISOLATION),
            viewModel.repairState.value
        )
        assertEquals(ConnectionState.WIFI_MISMATCH, viewModel.effectiveState.value)
        assertEquals("192.168.1.18", viewModel.isolationBadgeIp.value)
    }

    @Test
    fun `effectiveState stays WIFI_MISMATCH and is not cleared when result is HttpDelayed`() = runTest {
        val bridge = StubRepairBridge()
        val service = StubConnectivityService(
            updateResult = WifiConfigResult.TransportConfirmedHttpDelayed(
                badgeSsid = "OfficeGuest",
                baseUrl = "http://192.168.0.9:8088"
            )
        )
        val viewModel = createViewModel(service = service, bridge = bridge)
        advanceUntilIdle()

        viewModel.updateWifiConfig(ssid = "OfficeGuest", password = "secret")
        advanceUntilIdle()

        // WIFI_MISMATCH 覆盖不清除 — UI 仍展示修复区域
        assertEquals(ConnectionState.WIFI_MISMATCH, viewModel.effectiveState.value)
        assertNotEquals(WifiRepairState.Idle, viewModel.repairState.value)
    }

    // ── Fakes ──────────────────────────────────────────────────

    private class StubRepairBridge(
        connection: BadgeConnectionState = BadgeConnectionState.Disconnected,
        manager: BadgeManagerStatus = BadgeManagerStatus.Disconnected
    ) : ConnectivityBridge {
        private val _connectionState = MutableStateFlow(connection)
        private val _managerStatus = MutableStateFlow(manager)
        private val _repairEvents = MutableSharedFlow<WifiRepairEvent>(
            replay = 0,
            extraBufferCapacity = 16
        )

        override val connectionState: StateFlow<BadgeConnectionState> = _connectionState.asStateFlow()
        override val managerStatus: StateFlow<BadgeManagerStatus> = _managerStatus.asStateFlow()

        override suspend fun downloadRecording(
            filename: String,
            onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
        ): WavDownloadResult = error("not used")

        override suspend fun listRecordings(): Result<List<String>> = Result.Success(emptyList())
        override fun recordingNotifications(): Flow<RecordingNotification> = emptyFlow()
        override fun audioRecordingNotifications(): Flow<RecordingNotification.AudioRecordingReady> = emptyFlow()
        override fun batteryNotifications(): Flow<Int> = emptyFlow()
        override suspend fun isReady(): Boolean = false
        override suspend fun deleteRecording(filename: String): Boolean = false
        override fun wifiRepairEvents(): Flow<WifiRepairEvent> = _repairEvents

        suspend fun emitRepairEvent(event: WifiRepairEvent) = _repairEvents.emit(event)
    }

    private class StubConnectivityService(
        private val updateResult: WifiConfigResult = WifiConfigResult.Success
    ) : ConnectivityService {
        override suspend fun checkForUpdate(): UpdateResult = UpdateResult.None
        override suspend fun reconnect(): ReconnectResult = ReconnectResult.DeviceNotFound
        override suspend fun disconnect() = Unit
        override suspend fun unpair() = Unit
        override suspend fun updateWifiConfig(ssid: String, password: String): WifiConfigResult = updateResult
        override fun scheduleAutoReconnect() = Unit
    }

    private class NoOpDeviceRegistryManager : DeviceRegistryManager {
        private val _registeredDevices = MutableStateFlow<List<RegisteredDevice>>(emptyList())
        private val _activeDevice = MutableStateFlow<RegisteredDevice?>(null)
        override val registeredDevices: StateFlow<List<RegisteredDevice>> = _registeredDevices.asStateFlow()
        override val activeDevice: StateFlow<RegisteredDevice?> = _activeDevice.asStateFlow()
        override fun registerDevice(peripheral: BlePeripheral, session: BleSession) = Unit
        override fun renameDevice(macAddress: String, newName: String) = Unit
        override fun setDefault(macAddress: String) = Unit
        override suspend fun switchToDevice(macAddress: String) = Unit
        override fun removeDevice(macAddress: String) = Unit
        override fun initializeOnLaunch() = Unit
    }
}
