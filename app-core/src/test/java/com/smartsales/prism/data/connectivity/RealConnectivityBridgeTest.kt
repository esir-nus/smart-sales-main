package com.smartsales.prism.data.connectivity

import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.ConnectionState
import com.smartsales.prism.data.connectivity.legacy.BadgeHttpClient
import com.smartsales.prism.data.connectivity.legacy.DeviceNetworkStatus
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.FakeBadgeHttpClient
import com.smartsales.prism.data.connectivity.legacy.FakeDeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.FakePhoneWifiProvider
import com.smartsales.prism.data.connectivity.legacy.ProvisioningStatus
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeState
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeStatus
import com.smartsales.prism.data.connectivity.legacy.badge.FakeBadgeStateMonitor
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityPrompt
import com.smartsales.prism.domain.connectivity.IsolationTriggerContext
import com.smartsales.prism.domain.connectivity.RecordingNotification
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class RealConnectivityBridgeTest {

    @Test
    fun `bridge initializes connectionState before startup collectors begin`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()

        val bridge = newBridge(manager, mock(), monitor)

        assertEquals(BadgeConnectionState.Disconnected, bridge.connectionState.value)
    }

    @Test
    fun `recordingNotifications ignores events while transport is not ready`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val bridge = newBridge(manager, mock(), monitor)
        val unexpected = backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
            bridge.recordingNotifications().first()
        }

        manager.setState(ConnectionState.Disconnected)
        manager.emitRecordingReadyEvent("log_20260322_170000.wav")
        Thread.sleep(200)
        assertFalse(unexpected.isCompleted)
        unexpected.cancel()

        val expected = backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
            bridge.recordingNotifications().first()
        }

        manager.setState(
            ConnectionState.WifiProvisioned(
                session = BlePeripheral("badge-1", "Badge", -40).let { ble ->
                    com.smartsales.prism.data.connectivity.legacy.BleSession.fromPeripheral(ble)
                },
                status = ProvisioningStatus(
                    wifiSsid = "MstRobot",
                    handshakeId = "h1",
                    credentialsHash = "c1"
                )
            )
        )
        monitor.simulateConnected(ip = "192.168.0.115", wifiName = "MstRobot")
        manager.emitRecordingReadyEvent("log_20260322_170001.wav")
        assertEquals(
            RecordingNotification.RecordingReady("log_20260322_170001.wav"),
            expected.await()
        )
    }

    @Test
    fun `recordingNotifications forwards full filename when wifi provisioned`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val bridge = newBridge(manager, mock(), FakeBadgeStateMonitor())
        val recorded = backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
            bridge.recordingNotifications().first()
        }

        manager.setState(
            ConnectionState.WifiProvisioned(
                session = BlePeripheral("badge-1", "Badge", -40).let { ble ->
                    com.smartsales.prism.data.connectivity.legacy.BleSession.fromPeripheral(ble)
                },
                status = ProvisioningStatus(
                    wifiSsid = "MstRobot",
                    handshakeId = "h1",
                    credentialsHash = "c1"
                )
            )
        )
        manager.emitRecordingReadyEvent("log_20260322_170000.wav")
        advanceUntilIdle()

        assertEquals(
            RecordingNotification.RecordingReady("log_20260322_170000.wav"),
            recorded.await()
        )
    }

    @Test
    fun `firmwareVersionNotifications forwards manager version events`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val bridge = newBridge(manager, mock(), FakeBadgeStateMonitor())
        val recorded = backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
            bridge.firmwareVersionNotifications().first()
        }

        manager.emitFirmwareVersion("1.0.0.1")
        advanceUntilIdle()

        assertEquals("1.0.0.1", recorded.await())
    }

    @Test
    fun `sdCardSpaceNotifications forwards manager space events`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val bridge = newBridge(manager, mock(), FakeBadgeStateMonitor())
        val recorded = backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
            bridge.sdCardSpaceNotifications().first()
        }

        manager.emitSdCardSpace("27.23GB")
        advanceUntilIdle()

        assertEquals("27.23GB", recorded.await())
    }

    @Test
    fun `requestFirmwareVersion delegates to device manager`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val bridge = newBridge(manager, mock(), FakeBadgeStateMonitor())

        assertEquals(true, bridge.requestFirmwareVersion())
        assertEquals(1, manager.requestFirmwareVersionCalls)
    }

    @Test
    fun `requestSdCardSpace delegates to device manager`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val bridge = newBridge(manager, mock(), FakeBadgeStateMonitor())

        assertEquals(true, bridge.requestSdCardSpace())
        assertEquals(1, manager.requestSdCardSpaceCalls)
    }

    @Test
    fun `notifyCommandEnd delegates to device manager`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val bridge = newBridge(manager, mock(), FakeBadgeStateMonitor())

        bridge.notifyCommandEnd()

        assertEquals(1, manager.notifyCommandEndCalls)
    }

    @Test
    fun `managerStatus refines disconnected into ble paired network unknown`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val bridge = newBridge(manager, mock(), monitor)

        manager.setState(ConnectionState.Disconnected)
        monitor.setStatus(
            BadgeStatus(
                state = BadgeState.PAIRED,
                bleConnected = true,
                lastCheckMs = 1L
            )
        )

        assertEquals(
            BadgeManagerStatus.BlePairedNetworkUnknown,
            awaitManagerStatus(bridge, BadgeManagerStatus.BlePairedNetworkUnknown)
        )
    }

    @Test
    fun `managerStatus refines disconnected into ble paired network offline`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val bridge = newBridge(manager, mock(), monitor)

        manager.setState(ConnectionState.Disconnected)
        monitor.setStatus(
            BadgeStatus(
                state = BadgeState.OFFLINE,
                bleConnected = true,
                lastCheckMs = 1L
            )
        )

        assertEquals(
            BadgeManagerStatus.BlePairedNetworkOffline,
            awaitManagerStatus(bridge, BadgeManagerStatus.BlePairedNetworkOffline)
        )
    }

    @Test
    fun `managerStatus does not override needs setup with paired offline monitor state`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val bridge = newBridge(manager, mock(), monitor)

        manager.setState(ConnectionState.NeedsSetup)
        monitor.setStatus(
            BadgeStatus(
                state = BadgeState.OFFLINE,
                bleConnected = true,
                lastCheckMs = 1L
            )
        )

        assertEquals(
            BadgeManagerStatus.NeedsSetup,
            awaitManagerStatus(bridge, BadgeManagerStatus.NeedsSetup)
        )
    }

    @Test
    fun `managerStatus exposes wifi provisioned http delayed separately from ready`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val bridge = newBridge(manager, mock(), monitor)
        val session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40))

        manager.setState(
            ConnectionState.WifiProvisionedHttpDelayed(
                session = session,
                status = ProvisioningStatus(
                    wifiSsid = "MstRobot",
                    handshakeId = "h1",
                    credentialsHash = "c1"
                ),
                baseUrl = "http://192.168.0.115:8088"
            )
        )
        monitor.simulateConnected(ip = "192.168.0.115", wifiName = "MstRobot")

        assertEquals(
            BadgeManagerStatus.HttpDelayed(
                badgeIp = "192.168.0.115",
                ssid = "MstRobot",
                baseUrl = "http://192.168.0.115:8088"
            ),
            awaitManagerStatus(
                bridge,
                BadgeManagerStatus.HttpDelayed(
                    badgeIp = "192.168.0.115",
                    ssid = "MstRobot",
                    baseUrl = "http://192.168.0.115:8088"
                )
            )
        )
    }

    @Test
    fun `connectionState promotes reconnect success to connected as soon as monitor reports transport ready`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val bridge = newBridge(manager, mock(), monitor)

        manager.setState(ConnectionState.AutoReconnecting(attempt = 1))
        monitor.setStatus(
            BadgeStatus(
                state = BadgeState.CONNECTED,
                ipAddress = "192.168.0.115",
                wifiName = "MstRobot",
                bleConnected = true,
                lastCheckMs = 1L
            )
        )

        assertEquals(
            BadgeConnectionState.Connected(
                badgeIp = "192.168.0.115",
                ssid = "MstRobot"
            ),
            awaitConnectionState(
                bridge,
                BadgeConnectionState.Connected(
                    badgeIp = "192.168.0.115",
                    ssid = "MstRobot"
                )
            )
        )
    }

    @Test
    fun `connectionState keeps ble only reconnect in connecting until monitor reports usable ip`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val bridge = newBridge(manager, mock(), monitor)

        manager.setState(ConnectionState.AutoReconnecting(attempt = 1))
        monitor.setStatus(
            BadgeStatus(
                state = BadgeState.PAIRED,
                bleConnected = true,
                lastCheckMs = 1L
            )
        )

        assertEquals(
            BadgeConnectionState.Connecting,
            awaitConnectionState(bridge, BadgeConnectionState.Connecting)
        )
    }

    @Test
    fun `isReady returns false when badge reports no usable ip`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val httpClient = FakeBadgeHttpClient()
        val bridge = newBridge(manager, httpClient, FakeBadgeStateMonitor())
        manager.stubNetworkResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "0.0.0.0",
                deviceWifiName = "N/A",
                phoneWifiName = "MstRobot",
                rawResponse = "wifi#address#ip#name"
            )
        )

        assertFalse(bridge.isReady())
        assertEquals(emptyList<String>(), httpClient.getReachableCalls())
        assertEquals(0, manager.replayLatestSavedWifiCredentialForMediaFailureCalls)
    }

    @Test
    fun `isReady resolves base url and still fails when http reachability is false`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val httpClient = FakeBadgeHttpClient().apply { setReachable(false) }
        val bridge = newBridge(manager, httpClient, FakeBadgeStateMonitor())
        val session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40))
        manager.setState(
            ConnectionState.WifiProvisioned(
                session = session,
                status = ProvisioningStatus(
                    wifiSsid = "MstRobot",
                    handshakeId = "h1",
                    credentialsHash = "c1"
                )
            )
        )
        manager.stubNetworkResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.0.115",
                deviceWifiName = "MstRobot",
                phoneWifiName = "MstRobot",
                rawResponse = "IP#192.168.0.115 SD#MstRobot"
            )
        )
        manager.stubReplayLatestSavedWifiCredentialForMediaFailureResult =
            ConnectionState.WifiProvisioned(
                session = session,
                status = ProvisioningStatus(
                    wifiSsid = "MstRobot",
                    handshakeId = "h1",
                    credentialsHash = "c1"
                )
            )

        assertFalse(bridge.isReady())
        assertEquals(
            BadgeManagerStatus.HttpDelayed(
                badgeIp = "192.168.0.115",
                ssid = "MstRobot",
                baseUrl = "http://192.168.0.115:8088"
            ),
            awaitManagerStatus(
                bridge,
                BadgeManagerStatus.HttpDelayed(
                    badgeIp = "192.168.0.115",
                    ssid = "MstRobot",
                    baseUrl = "http://192.168.0.115:8088"
                )
            )
        )
        assertEquals(
            listOf(
                "http://192.168.0.115:8088",
                "http://192.168.0.115:8088",
                "http://192.168.0.115:8088",
                "http://192.168.0.115:8088",
                "http://192.168.0.115:8088"
            ),
            httpClient.getReachableCalls()
        )
        assertEquals(1, manager.replayLatestSavedWifiCredentialForMediaFailureCalls)
    }

    @Test
    fun `isReady marks connected transport as http delayed when media endpoint is unreachable`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val httpClient = FakeBadgeHttpClient().apply { setReachable(false) }
        val bridge = newBridge(manager, httpClient, monitor)
        val session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40))
        manager.setState(ConnectionState.Connected(session))
        monitor.simulateConnected(ip = "192.168.0.115", wifiName = "MstRobot")
        manager.stubNetworkResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.0.115",
                deviceWifiName = "MstRobot",
                phoneWifiName = "MstRobot",
                rawResponse = "IP#192.168.0.115 SD#MstRobot"
            )
        )

        assertFalse(bridge.isReady())
        assertEquals(
            BadgeManagerStatus.HttpDelayed(
                badgeIp = "192.168.0.115",
                ssid = "MstRobot",
                baseUrl = "http://192.168.0.115:8088"
            ),
            awaitManagerStatus(
                bridge,
                BadgeManagerStatus.HttpDelayed(
                    badgeIp = "192.168.0.115",
                    ssid = "MstRobot",
                    baseUrl = "http://192.168.0.115:8088"
                )
            )
        )
    }

    @Test
    fun `isReady joins concurrent media failure credential replay for same runtime`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val httpClient = FakeBadgeHttpClient().apply { setReachable(false) }
        val bridge = newBridge(manager, httpClient, monitor)
        val session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40))
        manager.setState(
            ConnectionState.WifiProvisioned(
                session = session,
                status = ProvisioningStatus(
                    wifiSsid = "MstRobot",
                    handshakeId = "h1",
                    credentialsHash = "c1"
                )
            )
        )
        manager.stubNetworkResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.0.115",
                deviceWifiName = "MstRobot",
                phoneWifiName = "MstRobot",
                rawResponse = "IP#192.168.0.115 SD#MstRobot"
            )
        )
        manager.stubReplayLatestSavedWifiCredentialForMediaFailureResult =
            ConnectionState.WifiProvisioned(
                session = session,
                status = ProvisioningStatus(
                    wifiSsid = "MstRobot",
                    handshakeId = "h1",
                    credentialsHash = "c1"
                )
            )
        manager.replayLatestSavedWifiCredentialForMediaFailureHandler = {
            delay(1_000L)
            manager.stubReplayLatestSavedWifiCredentialForMediaFailureResult
        }

        val first = async { bridge.isReady() }
        val second = async { bridge.isReady() }
        advanceUntilIdle()

        assertFalse(first.await())
        assertFalse(second.await())
        assertEquals(1, manager.replayLatestSavedWifiCredentialForMediaFailureCalls)
    }

    @Test
    fun `isReady accepts reachable endpoint without phone ssid alignment`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val httpClient = FakeBadgeHttpClient().apply { setReachable(true) }
        val bridge = newBridge(manager, httpClient, FakeBadgeStateMonitor())
        manager.stubNetworkResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.0.115",
                deviceWifiName = "OtherWifi",
                phoneWifiName = "",
                rawResponse = "IP#192.168.0.115 SD#OtherWifi"
            )
        )

        assertEquals(true, bridge.isReady())
        assertEquals(
            listOf("http://192.168.0.115:8088"),
            httpClient.getReachableCalls()
        )
    }

    @Test
    fun `list and download reuse active endpoint without repeated network query`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val httpClient = FakeBadgeHttpClient().apply {
            setListResult(Result.Success(listOf("log_20260402_094256.wav")))
        }
        val bridge = newBridge(manager, httpClient, monitor)
        manager.setState(
            ConnectionState.WifiProvisioned(
                session = BlePeripheral("badge-1", "Badge", -40).let { ble ->
                    com.smartsales.prism.data.connectivity.legacy.BleSession.fromPeripheral(ble)
                },
                status = ProvisioningStatus(
                    wifiSsid = "MstRobot",
                    handshakeId = "h1",
                    credentialsHash = "c1"
                )
            )
        )
        monitor.simulateConnected(ip = "192.168.0.115", wifiName = "MstRobot")

        assertEquals(Result.Success(listOf("log_20260402_094256.wav")), bridge.listRecordings())
        bridge.downloadRecording("log_20260402_094256.wav")

        assertEquals(0, manager.queryNetworkStatusCalls)
        assertEquals(listOf("http://192.168.0.115:8088"), httpClient.getListCalls())
        assertEquals(1, httpClient.getDownloadCalls().size)
        assertEquals("http://192.168.0.115:8088", httpClient.getDownloadCalls().single().baseUrl)
    }

    @Test
    fun `list failure replays saved credential once and retries list without phone ssid`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val httpClient = FakeBadgeHttpClient().apply {
            setListResults(
                listOf(
                    Result.Error(IllegalStateException("route isolated")),
                    Result.Success(listOf("log_20260402_094256.wav"))
                )
            )
        }
        val bridge = newBridge(manager, httpClient, monitor)
        val session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40))
        manager.setState(
            ConnectionState.WifiProvisioned(
                session = session,
                status = ProvisioningStatus(
                    wifiSsid = "MstRobot",
                    handshakeId = "h1",
                    credentialsHash = "c1"
                )
            )
        )
        monitor.simulateConnected(ip = "192.168.0.115", wifiName = "MstRobot")
        manager.stubNetworkResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.0.115",
                deviceWifiName = "MstRobot",
                phoneWifiName = "",
                rawResponse = "IP#192.168.0.115 SD#MstRobot"
            )
        )
        manager.stubReplayLatestSavedWifiCredentialForMediaFailureResult =
            ConnectionState.WifiProvisioned(
                session = session,
                status = ProvisioningStatus(
                    wifiSsid = "MstRobot",
                    handshakeId = "h1",
                    credentialsHash = "c1"
                )
            )

        assertEquals(Result.Success(listOf("log_20260402_094256.wav")), bridge.listRecordings())
        assertEquals(1, manager.replayLatestSavedWifiCredentialForMediaFailureCalls)
        assertEquals(
            listOf("http://192.168.0.115:8088", "http://192.168.0.115:8088"),
            httpClient.getListCalls()
        )
    }

    @Test
    fun `http failure invalidates active endpoint and refreshes network status during readiness retry`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val httpClient = FakeBadgeHttpClient().apply {
            setReachable(false)
            setListResult(Result.Success(listOf("log_20260402_094256.wav")))
        }
        val bridge = newBridge(manager, httpClient, monitor)
        manager.setState(
            ConnectionState.WifiProvisioned(
                session = BlePeripheral("badge-1", "Badge", -40).let { ble ->
                    com.smartsales.prism.data.connectivity.legacy.BleSession.fromPeripheral(ble)
                },
                status = ProvisioningStatus(
                    wifiSsid = "MstRobot",
                    handshakeId = "h1",
                    credentialsHash = "c1"
                )
            )
        )
        monitor.simulateConnected(ip = "192.168.0.115", wifiName = "MstRobot")
        manager.stubNetworkResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.0.115",
                deviceWifiName = "MstRobot",
                phoneWifiName = "MstRobot",
                rawResponse = "IP#192.168.0.115 SD#MstRobot"
            )
        )

        assertFalse(bridge.isReady())
        assertEquals(1, manager.queryNetworkStatusCalls)

        bridge.listRecordings()

        assertEquals(2, manager.queryNetworkStatusCalls)
    }

    @Test
    fun `isReady retries once with fresh network status after transient http miss`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val httpClient = FakeBadgeHttpClient().apply {
            setReachableResults(listOf(false, true))
        }
        val bridge = newBridge(manager, httpClient, monitor)
        manager.setState(
            ConnectionState.WifiProvisioned(
                session = BlePeripheral("badge-1", "Badge", -40).let { ble ->
                    BleSession.fromPeripheral(ble)
                },
                status = ProvisioningStatus(
                    wifiSsid = "MstRobot",
                    handshakeId = "h1",
                    credentialsHash = "c1"
                )
            )
        )
        monitor.simulateConnected(ip = "192.168.0.115", wifiName = "MstRobot")
        manager.stubNetworkResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.0.115",
                deviceWifiName = "MstRobot",
                phoneWifiName = "MstRobot",
                rawResponse = "IP#192.168.0.115 SD#MstRobot"
            )
        )

        assertEquals(true, bridge.isReady())
        assertEquals(1, manager.queryNetworkStatusCalls)
        assertEquals(
            listOf(
                "http://192.168.0.115:8088",
                "http://192.168.0.115:8088"
            ),
            httpClient.getReachableCalls()
        )
    }

    @Test
    fun `isReady applies post credential grace schedule before giving up`() = runTest {
        val manager = FakeDeviceConnectionManager()
        val monitor = FakeBadgeStateMonitor()
        val httpClient = FakeBadgeHttpClient().apply { setReachable(false) }
        val coordinator = BadgeEndpointRecoveryCoordinator()
        val bridge = newBridge(manager, httpClient, monitor, coordinator)
        val session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40))
        manager.setState(
            ConnectionState.WifiProvisioned(
                session = session,
                status = ProvisioningStatus(
                    wifiSsid = "MstRobot",
                    handshakeId = "h1",
                    credentialsHash = "c1"
                )
            )
        )
        manager.stubNetworkResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.0.115",
                deviceWifiName = "MstRobot",
                phoneWifiName = "MstRobot",
                rawResponse = "IP#192.168.0.115 SD#MstRobot"
            )
        )
        val runtimeKey = BadgeRuntimeKey(
            peripheralId = session.peripheralId,
            secureToken = session.secureToken
        )
        coordinator.noteCurrentRuntimeKey(runtimeKey)
        coordinator.armPostCredentialGrace(runtimeKey)

        assertFalse(bridge.isReady())
        assertEquals(10_000L, testScheduler.currentTime)
        assertEquals(
            listOf(
                "http://192.168.0.115:8088",
                "http://192.168.0.115:8088",
                "http://192.168.0.115:8088"
            ),
            httpClient.getReachableCalls()
        )
    }

    private suspend fun awaitManagerStatus(
        bridge: RealConnectivityBridge,
        expected: BadgeManagerStatus
    ): BadgeManagerStatus {
        val deadline = System.currentTimeMillis() + 1_000
        while (System.currentTimeMillis() < deadline) {
            val current = bridge.managerStatus.value
            if (current == expected) {
                return current
            }
            Thread.sleep(20)
        }
        fail("Timed out waiting for managerStatus=$expected actual=${bridge.managerStatus.value}")
        throw IllegalStateException("unreachable")
    }

    private suspend fun awaitConnectionState(
        bridge: RealConnectivityBridge,
        expected: BadgeConnectionState
    ): BadgeConnectionState {
        val deadline = System.currentTimeMillis() + 1_000
        while (System.currentTimeMillis() < deadline) {
            val current = bridge.connectionState.value
            if (current == expected) {
                return current
            }
            Thread.sleep(20)
        }
        fail("Timed out waiting for connectionState=$expected actual=${bridge.connectionState.value}")
        throw IllegalStateException("unreachable")
    }

    private fun newBridge(
        manager: FakeDeviceConnectionManager,
        httpClient: BadgeHttpClient,
        monitor: FakeBadgeStateMonitor,
        coordinator: BadgeEndpointRecoveryCoordinator = BadgeEndpointRecoveryCoordinator()
    ): RealConnectivityBridge {
        return RealConnectivityBridge(
            deviceManager = manager,
            httpClient = httpClient,
            badgeStateMonitor = monitor,
            endpointRecoveryCoordinator = coordinator,
            phoneWifiProvider = FakePhoneWifiProvider(null),
            connectivityPrompt = NoOpConnectivityPrompt
        )
    }

    private object NoOpConnectivityPrompt : ConnectivityPrompt {
        override suspend fun promptWifiMismatch(suggestedSsid: String?) = Unit

        override suspend fun promptSuspectedIsolation(
            badgeIp: String,
            triggerContext: IsolationTriggerContext,
            suggestedSsid: String?
        ) = Unit
    }

}
