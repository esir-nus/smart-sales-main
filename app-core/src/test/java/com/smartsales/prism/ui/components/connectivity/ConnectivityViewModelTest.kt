package com.smartsales.prism.ui.components.connectivity

import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.FakeDeviceConnectionManager
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
import com.smartsales.prism.ui.settings.VoiceVolumePreferenceStore
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
        voiceVolumeStore: VoiceVolumePreferenceStore = VoiceVolumePreferenceStore(InMemorySharedPreferences()),
        connectionManager: FakeDeviceConnectionManager = FakeDeviceConnectionManager()
    ) = ConnectivityViewModel(
        connectivityService = service,
        connectivityBridge = bridge,
        registryManager = FakeDeviceRegistryManager(),
        voiceVolumeStore = voiceVolumeStore,
        connectionManager = connectionManager
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

    @Test
    fun `voice volume commit persists desired volume and marks applied on successful send`() = runTest {
        val bridge = FakeConnectivityBridge(
            connection = BadgeConnectionState.Connected(
                badgeIp = "192.168.0.9",
                ssid = "Office"
            ),
            manager = BadgeManagerStatus.Ready(
                badgeIp = "192.168.0.9",
                ssid = "Office"
            )
        )
        val store = VoiceVolumePreferenceStore(InMemorySharedPreferences())
        val connectionManager = FakeDeviceConnectionManager()
        val viewModel = createViewModel(
            bridge = bridge,
            voiceVolumeStore = store,
            connectionManager = connectionManager
        )

        viewModel.onVoiceVolumeDrag(64)
        viewModel.onVoiceVolumeCommitted()
        advanceUntilIdle()

        assertEquals(64, store.desiredVolume.value)
        assertEquals(64, store.lastAppliedVolume.value)
        assertEquals(listOf(64), connectionManager.voiceVolumeCalls)
    }

    @Test
    fun `voice volume commit retries same value after no-op send and skips after success`() = runTest {
        val store = VoiceVolumePreferenceStore(InMemorySharedPreferences())
        val connectionManager = FakeDeviceConnectionManager().apply {
            setVoiceVolumeShouldSucceed = false
        }
        val viewModel = createViewModel(
            voiceVolumeStore = store,
            connectionManager = connectionManager
        )

        viewModel.onVoiceVolumeDrag(41)
        viewModel.onVoiceVolumeCommitted()
        advanceUntilIdle()

        assertEquals(41, store.desiredVolume.value)
        assertNull(store.lastAppliedVolume.value)
        assertEquals(listOf(41), connectionManager.voiceVolumeCalls)

        connectionManager.setVoiceVolumeShouldSucceed = true
        viewModel.onVoiceVolumeCommitted()
        advanceUntilIdle()
        viewModel.onVoiceVolumeCommitted()
        advanceUntilIdle()

        assertEquals(41, store.lastAppliedVolume.value)
        assertEquals(listOf(41, 41), connectionManager.voiceVolumeCalls)
    }

    private class FakeConnectivityBridge(
        connection: BadgeConnectionState = BadgeConnectionState.Disconnected,
        manager: BadgeManagerStatus = BadgeManagerStatus.Disconnected
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

        override fun audioRecordingNotifications(): Flow<RecordingNotification.AudioRecordingReady> =
            emptyFlow()

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

        override fun scheduleAutoReconnect() = Unit
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

    private class InMemorySharedPreferences : android.content.SharedPreferences {
        private val values = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()

        override fun getString(key: String?, defValue: String?): String? {
            return values[key] as? String ?: defValue
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return (values[key] as? Set<String>)?.toMutableSet() ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue

        override fun contains(key: String?): Boolean = values.containsKey(key)

        override fun edit(): android.content.SharedPreferences.Editor = Editor(values)

        override fun registerOnSharedPreferenceChangeListener(
            listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        private class Editor(
            private val values: MutableMap<String, Any?>
        ) : android.content.SharedPreferences.Editor {
            private val pending = linkedMapOf<String, Any?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?): android.content.SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = value
            }

            override fun putStringSet(
                key: String?,
                values: MutableSet<String>?
            ): android.content.SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = values?.toSet()
            }

            override fun putInt(key: String?, value: Int): android.content.SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = value
            }

            override fun putLong(key: String?, value: Long): android.content.SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = value
            }

            override fun putFloat(key: String?, value: Float): android.content.SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = value
            }

            override fun putBoolean(key: String?, value: Boolean): android.content.SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = value
            }

            override fun remove(key: String?): android.content.SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = null
            }

            override fun clear(): android.content.SharedPreferences.Editor = apply {
                clearRequested = true
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearRequested) {
                    values.clear()
                }
                pending.forEach { (key, value) ->
                    if (value == null) {
                        values.remove(key)
                    } else {
                        values[key] = value
                    }
                }
                pending.clear()
                clearRequested = false
            }
        }
    }
}
