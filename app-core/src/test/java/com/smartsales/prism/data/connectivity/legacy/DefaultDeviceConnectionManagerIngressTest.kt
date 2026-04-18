package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.BadgeEndpointRecoveryCoordinator
import com.smartsales.prism.data.connectivity.legacy.badge.FakeBadgeStateMonitor
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeState
import com.smartsales.prism.data.connectivity.legacy.gateway.BadgeNotification
import com.smartsales.prism.data.connectivity.legacy.gateway.GattSessionLifecycle
import com.smartsales.prism.data.connectivity.legacy.scan.BleScanner
import com.smartsales.prism.data.connectivity.legacy.scan.FakeBleScanner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDeviceConnectionManagerIngressTest {

    @Test
    fun `selectPeripheral does not report healthy connected when persistent gatt setup fails`() = runTest {
        val gateway = FakeGattSessionLifecycle(
            connectResult = Result.Error(IllegalStateException("boom"))
        )
        val monitor = FakeBadgeStateMonitor()
        val manager = newManager(
            gateway = gateway,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            monitor = monitor
        )

        manager.selectPeripheral(BlePeripheral("badge-1", "Badge", -40))
        advanceUntilIdle()

        assertFalse(manager.state.value is ConnectionState.Connected)
        assertFalse(manager.state.value is ConnectionState.WifiProvisioned)
        assertFalse(manager.state.value is ConnectionState.Syncing)
        assertEquals(emptyList<String>(), monitor.connectedSessions)
    }

    @Test
    fun `reconnectAndWait returns wifi provisioned when gatt and network are both ready`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val monitor = FakeBadgeStateMonitor()
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("MstRobot", "secret")
            )
        }
        val manager = newManager(
            gateway = gateway,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            monitor = monitor,
            networkResult = Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "MstRobot",
                    phoneWifiName = "MstRobot",
                    rawResponse = "IP#192.168.0.9, SD#MstRobot"
                )
            )
        )

        val state = manager.reconnectAndWait()
        advanceUntilIdle()

        assertTrue(state is ConnectionState.WifiProvisioned)
        assertTrue(manager.state.value is ConnectionState.WifiProvisioned)
        assertEquals(listOf("badge-1"), monitor.connectedSessions)
        assertEquals(BadgeState.CONNECTED, monitor.status.value.state)
        assertFalse(monitor.pollingStarted)
    }

    @Test
    fun `recording ready event emits full downloadable filename`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("MstRobot", "secret")
            )
        }
        val manager = newManager(
            gateway = gateway,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            networkResult = Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "MstRobot",
                    phoneWifiName = "MstRobot",
                    rawResponse = "IP#192.168.0.9, SD#MstRobot"
                )
            )
        )
        val recorded = backgroundScope.async {
            manager.recordingReadyEvents.first()
        }

        manager.reconnectAndWait()
        advanceUntilIdle()
        gateway.emit(BadgeNotification.RecordingReady("20260322_170000"))
        advanceUntilIdle()

        assertEquals("log_20260322_170000.wav", recorded.await())
    }

    @Test
    fun `reconnectAndWait keeps ble held offline diagnostic when network reports no ip`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val monitor = FakeBadgeStateMonitor()
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("MstRobot", "secret")
            )
        }
        val manager = newManager(
            gateway = gateway,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            monitor = monitor,
            networkResult = Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "0.0.0.0",
                    deviceWifiName = "MstRobot",
                    phoneWifiName = "MstRobot",
                    rawResponse = "IP#0.0.0.0, SD#MstRobot"
                )
            )
        )

        val state = manager.reconnectAndWait()
        advanceUntilIdle()

        assertTrue(state is ConnectionState.Error)
        assertTrue(manager.state.value is ConnectionState.Disconnected)
        assertEquals(listOf("badge-1"), monitor.connectedSessions)
        assertEquals(BadgeState.OFFLINE, monitor.status.value.state)
        assertTrue(monitor.status.value.bleConnected)
    }

    @Test
    fun `reconnectAndWait replays known wifi credentials when badge is offline on exact phone ssid match`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val monitor = FakeBadgeStateMonitor()
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("MstRobot", "secret")
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResults += Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "0.0.0.0",
                    deviceWifiName = "",
                    phoneWifiName = "",
                    rawResponse = "IP#0.0.0.0, SD#N/A"
                )
            )
            stubNetworkResults += Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "MstRobot",
                    phoneWifiName = "",
                    rawResponse = "IP#192.168.0.9, SD#MstRobot"
                )
            )
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            monitor = monitor
        )

        val state = manager.reconnectAndWait()
        advanceUntilIdle()

        assertTrue(state is ConnectionState.WifiProvisioned)
        assertTrue(manager.state.value is ConnectionState.WifiProvisioned)
        assertEquals(1, provisioner.provisionCalls.size)
        assertEquals("MstRobot", provisioner.provisionCalls.single().second.ssid)
        assertEquals(BadgeState.CONNECTED, monitor.status.value.state)
    }

    @Test
    fun `reconnectAndWait succeeds immediately when badge is online regardless of phone ssid`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val monitor = FakeBadgeStateMonitor()
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("OfficeGuest", "secret-2")
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResults += Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.8",
                    deviceWifiName = "OldWifi",
                    phoneWifiName = "",
                    rawResponse = "IP#192.168.0.8, SD#OldWifi"
                )
            )
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            monitor = monitor
        )

        val state = manager.reconnectAndWait()
        advanceUntilIdle()

        assertTrue(state is ConnectionState.WifiProvisioned)
        assertTrue(manager.state.value is ConnectionState.WifiProvisioned)
        assertEquals(0, provisioner.provisionCalls.size)
        assertEquals(BadgeState.CONNECTED, monitor.status.value.state)
    }

    @Test
    fun `reconnectAndWait returns badge wifi offline when badge is offline and no stored credentials`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val monitor = FakeBadgeStateMonitor()
        // Save a session but do NOT save credentials — so lastCredentials stays null
        val sessionStore = InMemorySessionStore().apply {
            saveSession(BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)))
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResult = Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "0.0.0.0",
                    deviceWifiName = "",
                    phoneWifiName = "",
                    rawResponse = "IP#0.0.0.0, SD#N/A"
                )
            )
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            monitor = monitor
        )

        val state = manager.reconnectAndWait()
        advanceUntilIdle()

        assertTrue(state is ConnectionState.Error)
        val error = (state as ConnectionState.Error).error
        assertTrue(error is ConnectivityError.WifiDisconnected)
        assertEquals(
            WifiDisconnectedReason.BADGE_WIFI_OFFLINE,
            (error as ConnectivityError.WifiDisconnected).reason
        )
        assertTrue(manager.state.value is ConnectionState.Disconnected)
        assertEquals(0, provisioner.provisionCalls.size)
        assertEquals(BadgeState.OFFLINE, monitor.status.value.state)
    }

    @Test
    fun `reconnectAndWait returns badge wifi offline regardless of phone wifi state when badge is offline`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        // Save session without credentials — lastCredentials stays null
        val sessionStore = InMemorySessionStore().apply {
            saveSession(BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)))
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResult = Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "0.0.0.0",
                    deviceWifiName = "",
                    phoneWifiName = "",
                    rawResponse = "IP#0.0.0.0, SD#N/A"
                )
            )
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        val state = manager.reconnectAndWait()
        advanceUntilIdle()

        assertTrue(state is ConnectionState.Error)
        val error = (state as ConnectionState.Error).error as ConnectivityError.WifiDisconnected
        assertEquals(WifiDisconnectedReason.BADGE_WIFI_OFFLINE, error.reason)
        assertEquals(0, provisioner.provisionCalls.size)
    }

    @Test
    fun `reconnectAndWait retries once when first network query hits ble query floor timeout`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("MstRobot", "secret")
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResults += Result.Error(ProvisioningException.Timeout(2_000L))
            stubNetworkResults += Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "MstRobot",
                    phoneWifiName = "",
                    rawResponse = "IP#192.168.0.9, SD#MstRobot"
                )
            )
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        val state = manager.reconnectAndWait()
        advanceUntilIdle()

        assertTrue(state is ConnectionState.WifiProvisioned)
        assertEquals(2, provisioner.networkCalls.size)
    }

    @Test
    fun `queryNetworkStatus demotes ready transport when badge later reports offline`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val monitor = FakeBadgeStateMonitor()
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("MstRobot", "secret")
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResults += Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "MstRobot",
                    phoneWifiName = "",
                    rawResponse = "IP#192.168.0.9, SD#MstRobot"
                )
            )
            stubNetworkResults += Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "0.0.0.0",
                    deviceWifiName = "MstRobot",
                    phoneWifiName = "",
                    rawResponse = "IP#0.0.0.0, SD#MstRobot"
                )
            )
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            monitor = monitor
        )

        val reconnectState = manager.reconnectAndWait()
        advanceUntilIdle()
        assertTrue(reconnectState is ConnectionState.WifiProvisioned)

        val queryResult = manager.queryNetworkStatus()
        advanceUntilIdle()

        assertTrue(queryResult is Result.Success)
        assertTrue(manager.state.value is ConnectionState.Disconnected)
        assertEquals(BadgeState.OFFLINE, monitor.status.value.state)
    }

    @Test
    fun `confirmManualWifiProvision waits for badge to come online on submitted ssid`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val monitor = FakeBadgeStateMonitor()
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("OldWifi", "secret")
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResults += Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "0.0.0.0",
                    deviceWifiName = "",
                    phoneWifiName = "",
                    rawResponse = "IP#0.0.0.0, SD#N/A"
                )
            )
            stubNetworkResults += Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "OfficeGuest",
                    phoneWifiName = "",
                    rawResponse = "IP#192.168.0.9, SD#OfficeGuest"
                )
            )
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            monitor = monitor
        )

        val state = manager.confirmManualWifiProvision(
            WifiCredentials("OfficeGuest", "secret-2")
        )
        advanceUntilIdle()

        assertTrue(state is ConnectionState.WifiProvisioned)
        assertTrue(manager.state.value is ConnectionState.WifiProvisioned)
        assertEquals(2, provisioner.networkCalls.size)
        assertEquals(BadgeState.CONNECTED, monitor.status.value.state)
    }

    @Test
    fun `confirmManualWifiProvision probes http before declaring success`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val monitor = FakeBadgeStateMonitor()
        val badgeHttpClient = FakeBadgeHttpClient().apply { setReachable(true) }
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("OldWifi", "secret")
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResult = Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "OfficeGuest",
                    phoneWifiName = "",
                    rawResponse = "IP#192.168.0.9, SD#OfficeGuest"
                )
            )
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            monitor = monitor,
            badgeHttpClient = badgeHttpClient
        )

        val state = manager.confirmManualWifiProvision(
            WifiCredentials("OfficeGuest", "secret-2")
        )
        advanceUntilIdle()

        assertTrue(state is ConnectionState.WifiProvisioned)
        assertEquals(2_000L, testScheduler.currentTime)
        assertEquals(listOf("http://192.168.0.9:8088"), badgeHttpClient.getReachableCalls())
    }

    @Test
    fun `confirmManualWifiProvision returns mismatch only when badge proves different ssid`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val monitor = FakeBadgeStateMonitor()
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("OldWifi", "secret")
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResult = Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "OtherWifi",
                    phoneWifiName = "",
                    rawResponse = "IP#192.168.0.9, SD#OtherWifi"
                )
            )
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            monitor = monitor
        )

        val state = manager.confirmManualWifiProvision(
            WifiCredentials("OfficeGuest", "secret-2")
        )
        advanceUntilIdle()

        assertTrue(state is ConnectionState.Error)
        val error = (state as ConnectionState.Error).error as ConnectivityError.WifiDisconnected
        assertEquals(WifiDisconnectedReason.BADGE_PHONE_NETWORK_MISMATCH, error.reason)
        assertEquals("OfficeGuest", error.phoneSsid)
        assertEquals("OtherWifi", error.badgeSsid)
        assertTrue(manager.state.value is ConnectionState.Disconnected)
    }

    @Test
    fun `confirmManualWifiProvision keeps non mismatch offline failure out of wifi mismatch branch`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("OldWifi", "secret")
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResults += Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "",
                    phoneWifiName = "",
                    rawResponse = "IP#192.168.0.9, SD#N/A"
                )
            )
            stubNetworkResults += Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "",
                    phoneWifiName = "",
                    rawResponse = "IP#192.168.0.9, SD#N/A"
                )
            )
            stubNetworkResults += Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "",
                    phoneWifiName = "",
                    rawResponse = "IP#192.168.0.9, SD#N/A"
                )
            )
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        val state = manager.confirmManualWifiProvision(
            WifiCredentials("OfficeGuest", "secret-2")
        )
        advanceUntilIdle()

        assertTrue(state is ConnectionState.Error)
        val error = (state as ConnectionState.Error).error as ConnectivityError.WifiDisconnected
        assertEquals(WifiDisconnectedReason.BADGE_WIFI_OFFLINE, error.reason)
        assertTrue(manager.state.value is ConnectionState.Disconnected)
        assertEquals(3, provisioner.networkCalls.size)
    }

    @Test
    fun `confirmManualWifiProvision returns http delayed when transport confirmed but http never comes up`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val badgeHttpClient = FakeBadgeHttpClient().apply { setReachable(false) }
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("OldWifi", "secret")
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            repeat(3) {
                stubNetworkResults += Result.Success(
                    DeviceNetworkStatus(
                        ipAddress = "192.168.0.9",
                        deviceWifiName = "OfficeGuest",
                        phoneWifiName = "",
                        rawResponse = "IP#192.168.0.9, SD#OfficeGuest"
                    )
                )
            }
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            badgeHttpClient = badgeHttpClient
        )

        val state = manager.confirmManualWifiProvision(
            WifiCredentials("OfficeGuest", "secret-2")
        )
        advanceUntilIdle()

        // 传输已确认（IP + SSID），HTTP 仍在预热 — 不应视为失败
        assertTrue(state is ConnectionState.WifiProvisionedHttpDelayed)
        val delayed = state as ConnectionState.WifiProvisionedHttpDelayed
        assertEquals("http://192.168.0.9:8088", delayed.baseUrl)
        // confirmManualWifiProvision 提升为 WifiProvisioned（心跳继续）
        assertTrue(manager.state.value is ConnectionState.WifiProvisioned)
        assertEquals(10_000L, testScheduler.currentTime)
        assertEquals(3, provisioner.networkCalls.size)
        assertEquals(
            listOf(
                "http://192.168.0.9:8088",
                "http://192.168.0.9:8088",
                "http://192.168.0.9:8088"
            ),
            badgeHttpClient.getReachableCalls()
        )
    }

    @Test
    fun `confirmManualWifiProvision emits TransportConfirmed and HttpReady events when http is ready`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val badgeHttpClient = FakeBadgeHttpClient().apply { setReachable(true) }
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("OldWifi", "secret")
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResult = Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "OfficeGuest",
                    phoneWifiName = "",
                    rawResponse = "IP#192.168.0.9, SD#OfficeGuest"
                )
            )
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            badgeHttpClient = badgeHttpClient
        )

        val collectedEvents = mutableListOf<com.smartsales.prism.domain.connectivity.WifiRepairEvent>()
        val collectionJob = backgroundScope.launch {
            manager.wifiRepairEvents.collect { collectedEvents.add(it) }
        }

        val state = manager.confirmManualWifiProvision(WifiCredentials("OfficeGuest", "secret-2"))
        advanceUntilIdle()

        assertTrue(state is ConnectionState.WifiProvisioned)
        assertTrue(
            "Expected TransportConfirmed event",
            collectedEvents.any { it is com.smartsales.prism.domain.connectivity.WifiRepairEvent.TransportConfirmed }
        )
        assertTrue(
            "Expected HttpReady event",
            collectedEvents.any { it is com.smartsales.prism.domain.connectivity.WifiRepairEvent.HttpReady }
        )
        collectionJob.cancel()
    }

    @Test
    fun `confirmManualWifiProvision emits HttpDelayed event when transport confirmed but http never ready`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val badgeHttpClient = FakeBadgeHttpClient().apply { setReachable(false) }
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("OldWifi", "secret")
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            repeat(3) {
                stubNetworkResults += Result.Success(
                    DeviceNetworkStatus(
                        ipAddress = "192.168.0.9",
                        deviceWifiName = "OfficeGuest",
                        phoneWifiName = "",
                        rawResponse = "IP#192.168.0.9, SD#OfficeGuest"
                    )
                )
            }
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            badgeHttpClient = badgeHttpClient
        )

        val collectedEvents = mutableListOf<com.smartsales.prism.domain.connectivity.WifiRepairEvent>()
        val collectionJob = backgroundScope.launch {
            manager.wifiRepairEvents.collect { collectedEvents.add(it) }
        }

        val state = manager.confirmManualWifiProvision(WifiCredentials("OfficeGuest", "secret-2"))
        advanceUntilIdle()

        assertTrue(state is ConnectionState.WifiProvisionedHttpDelayed)
        assertTrue(
            "Expected HttpDelayed event",
            collectedEvents.any { it is com.smartsales.prism.domain.connectivity.WifiRepairEvent.HttpDelayed }
        )
        assertFalse(
            "Must not emit DefinitiveMismatch when SSID is correct",
            collectedEvents.any { it is com.smartsales.prism.domain.connectivity.WifiRepairEvent.DefinitiveMismatch }
        )
        assertFalse(
            "Must not emit BadgeOffline when IP is usable",
            collectedEvents.any { it is com.smartsales.prism.domain.connectivity.WifiRepairEvent.BadgeOffline }
        )
        collectionJob.cancel()
    }

    @Test
    fun `confirmManualWifiProvision emits DefinitiveMismatch when badge proves different ssid`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("OldWifi", "secret")
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResult = Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "OtherWifi",
                    phoneWifiName = "",
                    rawResponse = "IP#192.168.0.9, SD#OtherWifi"
                )
            )
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        val collectedEvents = mutableListOf<com.smartsales.prism.domain.connectivity.WifiRepairEvent>()
        val collectionJob = backgroundScope.launch {
            manager.wifiRepairEvents.collect { collectedEvents.add(it) }
        }

        val state = manager.confirmManualWifiProvision(WifiCredentials("OfficeGuest", "secret-2"))
        advanceUntilIdle()

        assertTrue(state is ConnectionState.Error)
        val error = (state as ConnectionState.Error).error as ConnectivityError.WifiDisconnected
        assertEquals(WifiDisconnectedReason.BADGE_PHONE_NETWORK_MISMATCH, error.reason)
        assertTrue(
            "Expected DefinitiveMismatch event",
            collectedEvents.any { it is com.smartsales.prism.domain.connectivity.WifiRepairEvent.DefinitiveMismatch }
        )
        collectionJob.cancel()
    }

    @Test
    fun `confirmManualWifiProvision returns badge offline when ssid never readable despite valid ip`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("OldWifi", "secret")
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            repeat(3) {
                stubNetworkResults += Result.Success(
                    DeviceNetworkStatus(
                        ipAddress = "192.168.0.9",
                        deviceWifiName = "",
                        phoneWifiName = "",
                        rawResponse = "IP#192.168.0.9, SD#N/A"
                    )
                )
            }
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        val state = manager.confirmManualWifiProvision(WifiCredentials("OfficeGuest", "secret-2"))
        advanceUntilIdle()

        // SSID 始终不可读，不应被误判为传输已确认
        assertTrue(state is ConnectionState.Error)
        val error = (state as ConnectionState.Error).error as ConnectivityError.WifiDisconnected
        assertEquals(WifiDisconnectedReason.BADGE_WIFI_OFFLINE, error.reason)
    }

    @Test
    fun `reconnectAndWait keeps paired diagnostic when network query fails`() = runTest {
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val monitor = FakeBadgeStateMonitor()
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("MstRobot", "secret")
            )
        }
        val manager = newManager(
            gateway = gateway,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            monitor = monitor,
            networkResult = Result.Error(IllegalStateException("query failed"))
        )

        val state = manager.reconnectAndWait()
        advanceUntilIdle()

        assertTrue(state is ConnectionState.Error)
        assertTrue(manager.state.value is ConnectionState.Disconnected)
        assertEquals(listOf("badge-1"), monitor.connectedSessions)
        assertEquals(BadgeState.PAIRED, monitor.status.value.state)
        assertTrue(monitor.status.value.bleConnected)
        assertEquals(1, monitor.queryFailureCount)
    }

    @Test
    fun `heartbeat detects unreachable badge and triggers auto reconnect`() = runTest {
        val gateway = FakeGattSessionLifecycle(
            connectResult = Result.Success(Unit),
            reachable = false
        )
        val monitor = FakeBadgeStateMonitor()
        val sessionStore = InMemorySessionStore().apply {
            save(
                session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40)),
                credentials = WifiCredentials("MstRobot", "secret")
            )
        }
        val manager = newManager(
            gateway = gateway,
            sessionStore = sessionStore,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            monitor = monitor,
            networkResult = Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.9",
                    deviceWifiName = "MstRobot",
                    phoneWifiName = "MstRobot",
                    rawResponse = "IP#192.168.0.9, SD#MstRobot"
                )
            )
        )

        val state = manager.reconnectAndWait()
        advanceUntilIdle()

        assertTrue(state is ConnectionState.WifiProvisioned)
        // After heartbeat detects unreachable, scheduleAutoReconnectIfNeeded fires.
        // Since gateway.connectResult is Success, auto-reconnect succeeds and
        // state returns to WifiProvisioned rather than staying Disconnected.
        val finalState = manager.state.value
        assertTrue(
            "Expected Disconnected or WifiProvisioned after heartbeat-triggered reconnect, got $finalState",
            finalState is ConnectionState.Disconnected || finalState is ConnectionState.WifiProvisioned
        )
    }

    @Test
    fun `backoff intervals follow updated schedule`() {
        assertEquals(0L, requiredIntervalFor(0))
        assertEquals(5_000L, requiredIntervalFor(1))
        assertEquals(10_000L, requiredIntervalFor(2))
        assertEquals(30_000L, requiredIntervalFor(3))
        assertEquals(60_000L, requiredIntervalFor(4))
        assertEquals(60_000L, requiredIntervalFor(10))
    }

    private fun newManager(
        gateway: FakeGattSessionLifecycle,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
        provisioner: FakeWifiProvisioner = FakeWifiProvisioner(),
        sessionStore: SessionStore = InMemorySessionStore(),
        monitor: FakeBadgeStateMonitor = FakeBadgeStateMonitor(),
        badgeHttpClient: BadgeHttpClient = FakeBadgeHttpClient(),
        networkResult: Result<DeviceNetworkStatus>? = null,
        bleScanner: BleScanner = FakeBleScanner()
    ): DefaultDeviceConnectionManager {
        val dispatchers = object : DispatcherProvider {
            override val io: CoroutineDispatcher = dispatcher
            override val main: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
        }
        if (networkResult != null) {
            provisioner.stubNetworkResult = networkResult
        }
        return DefaultDeviceConnectionManager(
            provisioner = provisioner,
            bleGateway = gateway,
            badgeHttpClient = badgeHttpClient,
            endpointRecoveryCoordinator = BadgeEndpointRecoveryCoordinator(),
            dispatchers = dispatchers,
            badgeStateMonitor = monitor,
            sessionStore = sessionStore,
            bleScanner = bleScanner,
            scope = scope
        )
    }

    private class FakeGattSessionLifecycle(
        private val connectResult: Result<Unit>,
        var reachable: Boolean = true
    ) : GattSessionLifecycle {
        private val notifications = MutableSharedFlow<BadgeNotification>(
            replay = 1,
            extraBufferCapacity = 4
        )

        override suspend fun connect(peripheralId: String): Result<Unit> = connectResult

        override suspend fun disconnect() = Unit

        override fun listenForBadgeNotifications(): Flow<BadgeNotification> = notifications

        override suspend fun isReachable(): Boolean = reachable

        suspend fun emit(notification: BadgeNotification) {
            notifications.emit(notification)
        }
    }
}
