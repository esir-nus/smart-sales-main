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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
    ) = ConnectivityViewModel(
        connectivityService = service,
        connectivityBridge = bridge,
        registryManager = FakeDeviceRegistryManager()
    )

    @Test
    fun `managerState shows ble paired offline while shared shell state stays disconnected`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Disconnected,
            manager = BadgeManagerStatus.BlePairedNetworkOffline
        )
        val viewModel = createViewModel(bridge = bridge)
        advanceUntilIdle()

        assertEquals(ConnectionState.DISCONNECTED, viewModel.connectionState.value)
        assertEquals(ConnectionState.DISCONNECTED, viewModel.effectiveState.value)
        assertEquals(
            ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE,
            viewModel.managerState.value
        )
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
        connection: BadgeConnectionState,
        manager: BadgeManagerStatus
    ) : ConnectivityBridge {
        private val _connectionState = MutableStateFlow(connection)
        private val _managerStatus = MutableStateFlow(manager)

        override val connectionState: StateFlow<BadgeConnectionState> = _connectionState.asStateFlow()
        override val managerStatus: StateFlow<BadgeManagerStatus> = _managerStatus.asStateFlow()

        override suspend fun downloadRecording(
            filename: String,
            onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
        ): WavDownloadResult {
            error("Not used in ConnectivityViewModelTest")
        }

        override suspend fun listRecordings(): Result<List<String>> = Result.Success(emptyList())

        override fun recordingNotifications(): Flow<RecordingNotification> = emptyFlow()

        override suspend fun isReady(): Boolean = false

        override suspend fun deleteRecording(filename: String): Boolean = false
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
    }
}
