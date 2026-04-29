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
import com.smartsales.prism.ui.debug.DebugModeStore
import com.smartsales.prism.ui.theme.InMemorySharedPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityViewModelTest {

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
        service: ConnectivityService = FakeConnectivityService(),
        bridge: ConnectivityBridge = FakeConnectivityBridge(),
        promptCoordinator: ConnectivityPromptCoordinator = ConnectivityPromptCoordinator(),
    ) = ConnectivityViewModel(
        connectivityService = service,
        connectivityBridge = bridge,
        registryManager = FakeDeviceRegistryManager(),
        promptCoordinator = promptCoordinator,
        debugModeStore = DebugModeStore(InMemorySharedPreferences())
    )

    @Test
    fun `shell state surfaces partial when manager reports ble paired network offline`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        assertEquals(ConnectionState.PARTIAL_WIFI_DOWN, viewModel.connectionState.value)
        assertEquals(ConnectionState.PARTIAL_WIFI_DOWN, viewModel.effectiveState.value)
        assertEquals(
            ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE,
            viewModel.managerState.value
        )
    }

    @Test
    fun `shell state surfaces partial when manager reports ble paired network unknown`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkUnknown
        )
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        assertEquals(ConnectionState.PARTIAL_WIFI_DOWN, viewModel.connectionState.value)
    }

    @Test
    fun `shell state keeps disconnected when neither ble nor wifi are up`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.Disconnected
        )
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        assertEquals(ConnectionState.DISCONNECTED, viewModel.connectionState.value)
    }

    @Test
    fun `connected badge state wins over diagnostic manager status`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Connected(badgeIp = "192.168.1.10", ssid = "OfficeGuest"),
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        assertEquals(ConnectionState.CONNECTED, viewModel.connectionState.value)
    }

    @Test
    fun `managerState maps http delayed while shell transport remains connected`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Connected(badgeIp = "192.168.1.10", ssid = "OfficeGuest"),
            manager = BadgeManagerStatus.HttpDelayed(
                badgeIp = "192.168.1.10",
                ssid = "OfficeGuest",
                baseUrl = "http://192.168.1.10:8088"
            )
        )
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        assertEquals(ConnectionState.CONNECTED, viewModel.connectionState.value)
        assertEquals(ConnectivityManagerState.HTTP_DELAYED, viewModel.managerState.value)
    }

    @Test
    fun `managerState keeps needs setup semantics when bridge says setup is required`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.NeedsSetup,
            manager = BadgeManagerStatus.NeedsSetup
        )
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        assertEquals(ConnectionState.NEEDS_SETUP, viewModel.connectionState.value)
        assertEquals(ConnectivityManagerState.NEEDS_SETUP, viewModel.managerState.value)
    }

    @Test
    fun `batteryLevel updates from bridge battery notifications`() = runTest {
        val bridge = FakeConnectivityBridge()
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        assertNull(viewModel.batteryLevel.value)

        bridge.emitBatteryLevel(42)
        advanceUntilIdle()

        assertEquals(42, viewModel.batteryLevel.value)
    }

    @Test
    fun `firmwareVersion auto-requests on connect and resets on disconnect`() = runTest {
        val bridge = FakeConnectivityBridge()
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        assertNull(viewModel.firmwareVersion.value)
        assertEquals(0, bridge.requestFirmwareVersionCalls)

        bridge.setConnectionState(BadgeConnectionState.Connected("192.168.0.9", "MstRobot"))
        advanceUntilIdle()
        assertEquals(1, bridge.requestFirmwareVersionCalls)

        bridge.emitFirmwareVersion("1.0.0.1")
        advanceUntilIdle()
        assertEquals("1.0.0.1", viewModel.firmwareVersion.value)

        bridge.setConnectionState(BadgeConnectionState.Disconnected)
        advanceUntilIdle()
        assertNull(viewModel.firmwareVersion.value)
    }

    @Test
    fun `requestFirmwareVersion forwards manual refresh to bridge`() = runTest {
        val bridge = FakeConnectivityBridge()
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        viewModel.requestFirmwareVersion()
        advanceUntilIdle()

        assertEquals(1, bridge.requestFirmwareVersionCalls)
    }

    @Test
    fun `sdCardSpace updates from bridge notifications and resets on disconnect`() = runTest {
        val bridge = FakeConnectivityBridge()
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        assertNull(viewModel.sdCardSpace.value)

        bridge.setConnectionState(BadgeConnectionState.Connected("192.168.0.9", "MstRobot"))
        advanceUntilIdle()

        bridge.emitSdCardSpace("27.23GB")
        advanceUntilIdle()
        assertEquals("27.23GB", viewModel.sdCardSpace.value)

        bridge.setConnectionState(BadgeConnectionState.Disconnected)
        advanceUntilIdle()
        assertNull(viewModel.sdCardSpace.value)
    }

    @Test
    fun `requestSdCardSpace forwards manual query to bridge`() = runTest {
        val bridge = FakeConnectivityBridge()
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        viewModel.requestSdCardSpace()
        advanceUntilIdle()

        assertEquals(1, bridge.requestSdCardSpaceCalls)
    }

    @Test
    fun `debug rec notification picks rec file from list and emits bridge ingress`() = runTest {
        val bridge = FakeConnectivityBridge(
            listResult = Result.Success(
                listOf("log_20260429_120000.wav", "rec_20260429_120001.wav")
            )
        )
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        viewModel.debugEmitRecNotification()
        advanceUntilIdle()

        assertEquals(listOf("rec_20260429_120001.wav"), bridge.debugRecEmits)
        assertEquals("debug rec emitted: rec_20260429_120001.wav", viewModel.debugProbeText.value)
    }

    @Test
    fun `managerState lets active reconnect override paired offline diagnostic state`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val reconnectGate = CompletableDeferred<ReconnectResult>()
        val viewModel = createViewModel(
            service = FakeConnectivityService(reconnectResults = listOf(reconnectGate)),
            bridge = bridge
        )
        advanceUntilIdle()

        viewModel.reconnect()
        advanceUntilIdle()
        assertEquals(ConnectivityManagerState.RECONNECTING, viewModel.managerState.value)

        reconnectGate.complete(ReconnectResult.DeviceNotFound)
        advanceUntilIdle()
        assertEquals(
            ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE,
            viewModel.managerState.value
        )
    }

    @Test
    fun `connectDevice preempts active reconnect and shows reconnecting override`() = runTest {
        val bridge = FakeConnectivityBridge()
        val reconnectGate = CompletableDeferred<ReconnectResult>()
        val service = FakeConnectivityService(reconnectResults = listOf(reconnectGate))
        val viewModel = createViewModel(service = service, bridge = bridge)
        advanceUntilIdle()

        viewModel.reconnect()
        advanceUntilIdle()
        assertEquals(ConnectivityManagerState.RECONNECTING, viewModel.managerState.value)

        viewModel.connectDevice("AA:BB:CC:DD:EE:02")

        assertEquals(listOf("AA:BB:CC:DD:EE:02"), service.connectCalls)
        assertEquals(1, service.reconnectCalls)
        assertEquals(ConnectivityManagerState.RECONNECTING, viewModel.managerState.value)
    }

    @Test
    fun `connectDevice holds reconnecting until manager state moves`() = runTest {
        val bridge = FakeConnectivityBridge()
        val service = FakeConnectivityService()
        val viewModel = createViewModel(service = service, bridge = bridge)
        advanceUntilIdle()

        viewModel.connectDevice("AA:BB:CC:DD:EE:03")

        assertEquals(listOf("AA:BB:CC:DD:EE:03"), service.connectCalls)
        assertEquals(ConnectivityManagerState.RECONNECTING, viewModel.managerState.value)

        bridge.setManagerStatus(BadgeManagerStatus.Connecting)
        advanceUntilIdle()

        assertEquals(ConnectivityManagerState.RECONNECTING, viewModel.managerState.value)
    }

    @Test
    fun `updateWifiConfig shows reconnecting while repair is in progress and clears on success`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val updateGate = CompletableDeferred<WifiConfigResult>()
        val service = FakeConnectivityService(updateWifiConfigResults = listOf(updateGate))
        val viewModel = createViewModel(service = service, bridge = bridge)
        advanceUntilIdle()

        viewModel.updateWifiConfig(ssid = "OfficeGuest", password = "secret")
        advanceUntilIdle()
        assertEquals(ConnectivityManagerState.RECONNECTING, viewModel.managerState.value)

        updateGate.complete(WifiConfigResult.Success)
        advanceUntilIdle()
        assertEquals(
            ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE,
            viewModel.managerState.value
        )
        assertEquals(1, service.updateWifiConfigCalls.size)
    }

    @Test
    fun `updateWifiConfig returns to wifi mismatch on repair failure`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val updateGate = CompletableDeferred<WifiConfigResult>()
        val viewModel = createViewModel(
            service = FakeConnectivityService(updateWifiConfigResults = listOf(updateGate)),
            bridge = bridge
        )
        advanceUntilIdle()

        viewModel.updateWifiConfig(ssid = "OfficeGuest", password = "secret")
        advanceUntilIdle()
        assertEquals(ConnectivityManagerState.RECONNECTING, viewModel.managerState.value)

        updateGate.complete(WifiConfigResult.Error("repair failed"))
        advanceUntilIdle()
        assertEquals(ConnectivityManagerState.WIFI_MISMATCH, viewModel.managerState.value)
        assertEquals("repair failed", viewModel.wifiMismatchErrorMessage.value)
    }

    @Test
    fun `updateWifiConfig blocks blank ssid locally without entering reconnecting`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val service = FakeConnectivityService()
        val viewModel = createViewModel(service = service, bridge = bridge)
        advanceUntilIdle()

        viewModel.updateWifiConfig(ssid = "   ", password = "secret")
        advanceUntilIdle()

        assertTrue(service.updateWifiConfigCalls.isEmpty())
        assertEquals(ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE, viewModel.managerState.value)
        assertEquals(WIFI_MISMATCH_EMPTY_CREDENTIALS_ERROR, viewModel.wifiMismatchErrorMessage.value)
    }

    @Test
    fun `updateWifiConfig blocks blank password locally without entering reconnecting`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val service = FakeConnectivityService()
        val viewModel = createViewModel(service = service, bridge = bridge)
        advanceUntilIdle()

        viewModel.updateWifiConfig(ssid = "OfficeGuest", password = "   ")
        advanceUntilIdle()

        assertTrue(service.updateWifiConfigCalls.isEmpty())
        assertEquals(ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE, viewModel.managerState.value)
        assertEquals(WIFI_MISMATCH_EMPTY_CREDENTIALS_ERROR, viewModel.wifiMismatchErrorMessage.value)
    }

    @Test
    fun `updateWifiConfig trims valid credentials before calling service`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val updateGate = CompletableDeferred<WifiConfigResult>()
        val service = FakeConnectivityService(updateWifiConfigResults = listOf(updateGate))
        val viewModel = createViewModel(service = service, bridge = bridge)
        advanceUntilIdle()

        viewModel.updateWifiConfig(ssid = "  OfficeGuest  ", password = "  secret  ")
        advanceUntilIdle()

        assertEquals(listOf("OfficeGuest" to "secret"), service.updateWifiConfigCalls)
        updateGate.complete(WifiConfigResult.Success)
        advanceUntilIdle()
        assertNull(viewModel.wifiMismatchErrorMessage.value)
    }

    @Test
    fun `resetTransientState clears stale wifi mismatch override`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val viewModel = createViewModel(
            service = FakeConnectivityService(
                reconnectResults = listOf(
                    CompletableDeferred<ReconnectResult>().apply {
                        complete(ReconnectResult.WifiMismatch(currentPhoneSsid = "OfficeGuest"))
                    }
                )
            ),
            bridge = bridge
        )
        advanceUntilIdle()

        viewModel.reconnect()
        advanceUntilIdle()
        assertEquals(ConnectivityManagerState.WIFI_MISMATCH, viewModel.managerState.value)
        assertEquals("OfficeGuest", viewModel.wifiMismatchSuggestedSsid.value)

        viewModel.resetTransientState()
        advanceUntilIdle()
        assertEquals(
            ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE,
            viewModel.managerState.value
        )
        assertNull(viewModel.wifiMismatchSuggestedSsid.value)
        assertNull(viewModel.wifiMismatchErrorMessage.value)
    }

    @Test
    fun `resetTransientState cancels reconnect and allows a fresh retry`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val reconnectGate = CompletableDeferred<ReconnectResult>()
        val service = FakeConnectivityService(
            reconnectResults = listOf(
                reconnectGate,
                CompletableDeferred<ReconnectResult>().apply {
                    complete(ReconnectResult.DeviceNotFound)
                }
            )
        )
        val viewModel = createViewModel(service = service, bridge = bridge)
        advanceUntilIdle()

        viewModel.reconnect()
        advanceUntilIdle()
        assertEquals(ConnectivityManagerState.RECONNECTING, viewModel.managerState.value)

        viewModel.resetTransientState()
        advanceUntilIdle()
        assertEquals(
            ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE,
            viewModel.managerState.value
        )

        viewModel.reconnect()
        advanceUntilIdle()
        assertEquals(2, service.reconnectCalls)
        assertEquals(
            ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE,
            viewModel.managerState.value
        )
    }

    @Test
    fun `duplicate wifi repair taps are ignored while repair is active`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val updateGate = CompletableDeferred<WifiConfigResult>()
        val service = FakeConnectivityService(updateWifiConfigResults = listOf(updateGate))
        val viewModel = createViewModel(service = service, bridge = bridge)
        advanceUntilIdle()

        viewModel.updateWifiConfig(ssid = "OfficeGuest", password = "secret")
        viewModel.updateWifiConfig(ssid = "OfficeGuest", password = "secret")
        advanceUntilIdle()

        assertEquals(1, service.updateWifiConfigCalls.size)
        assertEquals(ConnectivityManagerState.RECONNECTING, viewModel.managerState.value)
    }

    @Test
    fun `reconnect wifi mismatch exposes suggested phone ssid for repair form`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val viewModel = createViewModel(
            service = FakeConnectivityService(
                reconnectResults = listOf(
                    CompletableDeferred<ReconnectResult>().apply {
                        complete(ReconnectResult.WifiMismatch(currentPhoneSsid = "OfficeGuest"))
                    }
                )
            ),
            bridge = bridge
        )
        advanceUntilIdle()

        viewModel.reconnect()
        advanceUntilIdle()

        assertEquals(ConnectivityManagerState.WIFI_MISMATCH, viewModel.managerState.value)
        assertEquals("OfficeGuest", viewModel.wifiMismatchSuggestedSsid.value)
    }

    @Test
    fun `reconnect wifi mismatch surfaces diagnostic error message on repair form`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val viewModel = createViewModel(
            service = FakeConnectivityService(
                reconnectResults = listOf(
                    CompletableDeferred<ReconnectResult>().apply {
                        complete(
                            ReconnectResult.WifiMismatch(
                                currentPhoneSsid = "OfficeGuest",
                                errorMessage = "设备当前未接入可用 Wi‑Fi，请重新输入凭据"
                            )
                        )
                    }
                )
            ),
            bridge = bridge
        )
        advanceUntilIdle()

        viewModel.reconnect()
        advanceUntilIdle()

        assertEquals(ConnectivityManagerState.WIFI_MISMATCH, viewModel.managerState.value)
        assertEquals("OfficeGuest", viewModel.wifiMismatchSuggestedSsid.value)
        assertEquals(
            "设备当前未接入可用 Wi‑Fi，请重新输入凭据",
            viewModel.wifiMismatchErrorMessage.value
        )
    }

    @Test
    fun `scheduleAutoReconnect delegates to service without entering reconnect override`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val service = FakeConnectivityService()
        val viewModel = createViewModel(service = service, bridge = bridge)
        advanceUntilIdle()

        viewModel.scheduleAutoReconnect()
        advanceUntilIdle()

        assertEquals(1, service.scheduleAutoReconnectCalls)
        assertEquals(0, service.reconnectCalls)
        assertEquals(
            ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE,
            viewModel.managerState.value
        )
    }

    @Test
    fun `scheduleAutoReconnect is ignored while wifi mismatch override is active`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val service = FakeConnectivityService(
            reconnectResults = listOf(
                CompletableDeferred<ReconnectResult>().apply {
                    complete(ReconnectResult.WifiMismatch(currentPhoneSsid = "OfficeGuest"))
                }
            )
        )
        val viewModel = createViewModel(service = service, bridge = bridge)
        advanceUntilIdle()

        viewModel.reconnect()
        advanceUntilIdle()
        viewModel.scheduleAutoReconnect()
        advanceUntilIdle()

        assertEquals(1, service.reconnectCalls)
        assertEquals(0, service.scheduleAutoReconnectCalls)
        assertEquals(ConnectivityManagerState.WIFI_MISMATCH, viewModel.managerState.value)
    }

    @Test
    fun `wifi repair failure keeps submitted ssid as next mismatch suggestion`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val updateGate = CompletableDeferred<WifiConfigResult>()
        val viewModel = createViewModel(
            service = FakeConnectivityService(updateWifiConfigResults = listOf(updateGate)),
            bridge = bridge
        )
        advanceUntilIdle()

        viewModel.updateWifiConfig(ssid = "OfficeGuest", password = "secret")
        advanceUntilIdle()
        updateGate.complete(WifiConfigResult.Error("repair failed"))
        advanceUntilIdle()

        assertEquals(ConnectivityManagerState.WIFI_MISMATCH, viewModel.managerState.value)
        assertEquals("OfficeGuest", viewModel.wifiMismatchSuggestedSsid.value)
    }

    @Test
    fun `external wifi mismatch prompt updates modal state and emits shell prompt effect`() = runTest {
        val promptCoordinator = ConnectivityPromptCoordinator()
        val viewModel = createViewModel(promptCoordinator = promptCoordinator)
        val receivedRequests = mutableListOf<WifiMismatchPromptRequest>()
        val collectJob = launch {
            viewModel.promptRequests.collect { request ->
                receivedRequests += request
            }
        }
        advanceUntilIdle()

        promptCoordinator.promptWifiMismatch("OfficeGuest")
        advanceUntilIdle()

        assertEquals(ConnectivityManagerState.WIFI_MISMATCH, viewModel.managerState.value)
        assertEquals("OfficeGuest", viewModel.wifiMismatchSuggestedSsid.value)
        assertEquals(listOf(WifiMismatchPromptRequest("OfficeGuest")), receivedRequests)

        collectJob.cancel()
    }

    @Test
    fun `clearWifiMismatchError clears current repair error message`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        viewModel.updateWifiConfig(ssid = "", password = "")
        advanceUntilIdle()
        assertEquals(WIFI_MISMATCH_EMPTY_CREDENTIALS_ERROR, viewModel.wifiMismatchErrorMessage.value)

        viewModel.clearWifiMismatchError()
        advanceUntilIdle()
        assertNull(viewModel.wifiMismatchErrorMessage.value)
    }

    private class FakeConnectivityBridge(
        connection: BadgeConnectionState = BadgeConnectionState.Disconnected,
        manager: BadgeManagerStatus = BadgeManagerStatus.Disconnected,
        private val listResult: Result<List<String>> = Result.Success(emptyList())
    ) : ConnectivityBridge {
        private val _connectionState = MutableStateFlow(connection)
        private val _managerStatus = MutableStateFlow(manager)
        private val _repairEvents = MutableSharedFlow<WifiRepairEvent>(
            replay = 0,
            extraBufferCapacity = 16
        )
        private val _batteryNotifications = MutableSharedFlow<Int>(
            replay = 0,
            extraBufferCapacity = 4
        )
        private val _firmwareVersionNotifications = MutableSharedFlow<String>(
            replay = 0,
            extraBufferCapacity = 4
        )
        private val _sdCardSpaceNotifications = MutableSharedFlow<String>(
            replay = 0,
            extraBufferCapacity = 4
        )

        override val connectionState: StateFlow<BadgeConnectionState> = _connectionState.asStateFlow()
        override val managerStatus: StateFlow<BadgeManagerStatus> = _managerStatus.asStateFlow()
        var requestFirmwareVersionCalls = 0
        var requestSdCardSpaceCalls = 0
        val debugRecEmits = mutableListOf<String>()

        override suspend fun downloadRecording(
            filename: String,
            onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
        ): WavDownloadResult {
            error("Not used in ConnectivityViewModelTest")
        }

        override suspend fun listRecordings(): Result<List<String>> = listResult

        override fun recordingNotifications(): Flow<RecordingNotification> = emptyFlow()

        override fun audioRecordingNotifications(): Flow<RecordingNotification.AudioRecordingReady> =
            emptyFlow()

        override suspend fun debugEmitAudioRecordingReady(filename: String): Boolean {
            debugRecEmits += filename
            return true
        }

        override fun batteryNotifications(): Flow<Int> = _batteryNotifications

        override fun firmwareVersionNotifications(): Flow<String> = _firmwareVersionNotifications

        override fun sdCardSpaceNotifications(): Flow<String> = _sdCardSpaceNotifications

        override suspend fun isReady(): Boolean = false

        override suspend fun deleteRecording(filename: String): Boolean = false

        override suspend fun requestFirmwareVersion(): Boolean {
            requestFirmwareVersionCalls += 1
            return true
        }

        override suspend fun requestSdCardSpace(): Boolean {
            requestSdCardSpaceCalls += 1
            return true
        }

        override suspend fun notifyCommandEnd() = Unit

        override fun wifiRepairEvents(): Flow<WifiRepairEvent> = _repairEvents

        suspend fun emitRepairEvent(event: WifiRepairEvent) = _repairEvents.emit(event)

        suspend fun emitBatteryLevel(level: Int) = _batteryNotifications.emit(level)

        suspend fun emitFirmwareVersion(version: String) = _firmwareVersionNotifications.emit(version)

        suspend fun emitSdCardSpace(size: String) = _sdCardSpaceNotifications.emit(size)

        fun setConnectionState(state: BadgeConnectionState) {
            _connectionState.value = state
        }

        fun setManagerStatus(status: BadgeManagerStatus) {
            _managerStatus.value = status
        }
    }

    private class FakeConnectivityService(
        reconnectResults: List<CompletableDeferred<ReconnectResult>> = listOf(
            CompletableDeferred<ReconnectResult>().apply {
                complete(ReconnectResult.DeviceNotFound)
            }
        ),
        updateWifiConfigResults: List<CompletableDeferred<WifiConfigResult>> = listOf(
            CompletableDeferred<WifiConfigResult>().apply {
                complete(WifiConfigResult.Success)
            }
        )
    ) : ConnectivityService {
        private val reconnectQueue = ArrayDeque(reconnectResults)
        private val updateWifiConfigQueue = ArrayDeque(updateWifiConfigResults)
        var reconnectCalls = 0
            private set
        var scheduleAutoReconnectCalls = 0
            private set
        val connectCalls = mutableListOf<String>()
        val updateWifiConfigCalls = mutableListOf<Pair<String, String>>()

        override suspend fun checkForUpdate(): UpdateResult = UpdateResult.None

        override suspend fun reconnect(): ReconnectResult {
            reconnectCalls += 1
            val gate = reconnectQueue.removeFirstOrNull()
                ?: CompletableDeferred<ReconnectResult>().apply {
                    complete(ReconnectResult.DeviceNotFound)
                }
            return gate.await()
        }

        override suspend fun disconnect() = Unit

        override suspend fun unpair() = Unit

        override suspend fun updateWifiConfig(ssid: String, password: String): WifiConfigResult {
            updateWifiConfigCalls += ssid to password
            val gate = updateWifiConfigQueue.removeFirstOrNull()
                ?: CompletableDeferred<WifiConfigResult>().apply {
                    complete(WifiConfigResult.Success)
                }
            return gate.await()
        }

        override fun scheduleAutoReconnect() {
            scheduleAutoReconnectCalls += 1
        }

        override suspend fun connect(macAddress: String) {
            connectCalls += macAddress
        }
    }

    private class FakeDeviceRegistryManager : DeviceRegistryManager {
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
        override fun markManuallyDisconnected(macAddress: String, value: Boolean) = Unit
        override fun updateBleDetected(macAddress: String, value: Boolean) = Unit
    }
}
