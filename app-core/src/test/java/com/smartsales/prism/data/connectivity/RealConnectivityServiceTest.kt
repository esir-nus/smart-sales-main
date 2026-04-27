package com.smartsales.prism.data.connectivity

import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.ConnectionState
import com.smartsales.prism.data.connectivity.legacy.ConnectivityError
import com.smartsales.prism.data.connectivity.legacy.FakeDeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.FakeWifiProvisioner
import com.smartsales.prism.data.connectivity.legacy.InMemorySessionStore
import com.smartsales.prism.data.connectivity.legacy.ProvisioningStatus
import com.smartsales.prism.data.connectivity.legacy.WifiCredentials
import com.smartsales.prism.data.connectivity.legacy.WifiDisconnectedReason
import com.smartsales.prism.data.connectivity.registry.DeviceRegistryManager
import com.smartsales.prism.data.connectivity.registry.RegisteredDevice
import com.smartsales.prism.domain.connectivity.ReconnectResult
import com.smartsales.prism.domain.connectivity.WifiConfigResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealConnectivityServiceTest {

    @Test
    fun `updateWifiConfig can recover from reconnect repair flow using stored session`() = runTest {
        val session = BleSession.fromPeripheral(
            BlePeripheral("badge-1", "Badge", -40)
        )
        val manager = FakeDeviceConnectionManager().apply {
            stubConfirmManualWifiProvisionResult = ConnectionState.WifiProvisioned(
                session = session,
                status = ProvisioningStatus(
                    wifiSsid = "OfficeGuest",
                    handshakeId = "manual-confirm-ok",
                    credentialsHash = "hash"
                )
            )
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubProvisionResult = Result.Success(
                ProvisioningStatus(
                    wifiSsid = "OfficeGuest",
                    handshakeId = "manual-update",
                    credentialsHash = "hash"
                )
            )
        }
        val sessionStore = InMemorySessionStore().apply {
            saveSession(session)
        }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = provisioner,
            sessionStore = sessionStore,
            registryManager = noOpRegistry()
        )

        val result = service.updateWifiConfig(ssid = "OfficeGuest", password = "secret-2")

        assertEquals(WifiConfigResult.Success, result)
        assertEquals(1, provisioner.provisionCalls.size)
        assertEquals("OfficeGuest", provisioner.provisionCalls.single().second.ssid)
        assertTrue(sessionStore.findKnownNetworkBySsid("OfficeGuest") != null)
        assertEquals(1, manager.confirmManualWifiProvisionCalls.size)
        assertEquals(0, manager.reconnectAndWaitCalls)
    }

    @Test
    fun `updateWifiConfig rejects blank ssid before transport writes`() = runTest {
        val session = BleSession.fromPeripheral(
            BlePeripheral("badge-1", "Badge", -40)
        )
        val manager = FakeDeviceConnectionManager().apply {
            setState(ConnectionState.Connected(session))
        }
        val provisioner = FakeWifiProvisioner()
        val sessionStore = InMemorySessionStore().apply {
            saveSession(session)
        }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = provisioner,
            sessionStore = sessionStore,
            registryManager = noOpRegistry()
        )

        val result = service.updateWifiConfig(ssid = "   ", password = "secret-2")

        assertEquals(WifiConfigResult.Error("Wi-Fi 名称和密码不能为空"), result)
        assertTrue(provisioner.provisionCalls.isEmpty())
        assertTrue(manager.confirmManualWifiProvisionCalls.isEmpty())
        assertTrue(sessionStore.findKnownNetworkBySsid("OfficeGuest") == null)
    }

    @Test
    fun `updateWifiConfig rejects blank password before transport writes`() = runTest {
        val session = BleSession.fromPeripheral(
            BlePeripheral("badge-1", "Badge", -40)
        )
        val manager = FakeDeviceConnectionManager().apply {
            setState(ConnectionState.Connected(session))
        }
        val provisioner = FakeWifiProvisioner()
        val sessionStore = InMemorySessionStore().apply {
            saveSession(session)
        }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = provisioner,
            sessionStore = sessionStore,
            registryManager = noOpRegistry()
        )

        val result = service.updateWifiConfig(ssid = "OfficeGuest", password = "   ")

        assertEquals(WifiConfigResult.Error("Wi-Fi 名称和密码不能为空"), result)
        assertTrue(provisioner.provisionCalls.isEmpty())
        assertTrue(manager.confirmManualWifiProvisionCalls.isEmpty())
        assertTrue(sessionStore.findKnownNetworkBySsid("OfficeGuest") == null)
    }

    @Test
    fun `updateWifiConfig trims credentials before provisioning`() = runTest {
        val session = BleSession.fromPeripheral(
            BlePeripheral("badge-1", "Badge", -40)
        )
        val manager = FakeDeviceConnectionManager().apply {
            stubConfirmManualWifiProvisionResult = ConnectionState.WifiProvisioned(
                session = session,
                status = ProvisioningStatus(
                    wifiSsid = "OfficeGuest",
                    handshakeId = "manual-confirm-ok",
                    credentialsHash = "hash"
                )
            )
            setState(ConnectionState.Connected(session))
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubProvisionResult = Result.Success(
                ProvisioningStatus(
                    wifiSsid = "OfficeGuest",
                    handshakeId = "manual-update",
                    credentialsHash = "hash"
                )
            )
        }
        val sessionStore = InMemorySessionStore().apply {
            saveSession(session)
        }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = provisioner,
            sessionStore = sessionStore,
            registryManager = noOpRegistry()
        )

        val result = service.updateWifiConfig(ssid = "  OfficeGuest  ", password = "  secret-2  ")

        assertEquals(WifiConfigResult.Success, result)
        assertEquals("OfficeGuest", provisioner.provisionCalls.single().second.ssid)
        assertEquals("secret-2", provisioner.provisionCalls.single().second.password)
        assertEquals("secret-2", manager.confirmManualWifiProvisionCalls.single().password)
    }

    @Test
    fun `updateWifiConfig returns wifi mismatch only when manual repair confirms badge on different ssid`() = runTest {
        val session = BleSession.fromPeripheral(
            BlePeripheral("badge-1", "Badge", -40)
        )
        val manager = FakeDeviceConnectionManager().apply {
            stubConfirmManualWifiProvisionResult = ConnectionState.Error(
                ConnectivityError.WifiDisconnected(
                    reason = WifiDisconnectedReason.BADGE_PHONE_NETWORK_MISMATCH,
                    phoneSsid = "OfficeGuest",
                    badgeSsid = "OtherWifi"
                )
            )
            setState(ConnectionState.Connected(session))
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubProvisionResult = Result.Success(
                ProvisioningStatus(
                    wifiSsid = "OfficeGuest",
                    handshakeId = "manual-update",
                    credentialsHash = "hash"
                )
            )
        }
        val sessionStore = InMemorySessionStore().apply {
            saveSession(session)
        }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = provisioner,
            sessionStore = sessionStore,
            registryManager = noOpRegistry()
        )

        val result = service.updateWifiConfig(ssid = "OfficeGuest", password = "secret-2")

        assertEquals(
            WifiConfigResult.Error("设备与输入的 Wi‑Fi 不匹配，请重新检查配置"),
            result
        )
        assertEquals(1, manager.confirmManualWifiProvisionCalls.size)
        assertEquals(0, manager.reconnectAndWaitCalls)
    }

    @Test
    fun `updateWifiConfig returns error for non mismatch confirmation failure`() = runTest {
        val session = BleSession.fromPeripheral(
            BlePeripheral("badge-1", "Badge", -40)
        )
        val manager = FakeDeviceConnectionManager().apply {
            stubConfirmManualWifiProvisionResult = ConnectionState.Error(
                ConnectivityError.WifiDisconnected(
                    reason = WifiDisconnectedReason.BADGE_WIFI_OFFLINE,
                    badgeSsid = "OfficeGuest"
                )
            )
            setState(ConnectionState.Connected(session))
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubProvisionResult = Result.Success(
                ProvisioningStatus(
                    wifiSsid = "OfficeGuest",
                    handshakeId = "manual-update",
                    credentialsHash = "hash"
                )
            )
        }
        val sessionStore = InMemorySessionStore().apply {
            saveSession(session)
        }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = provisioner,
            sessionStore = sessionStore,
            registryManager = noOpRegistry()
        )

        val result = service.updateWifiConfig(ssid = "OfficeGuest", password = "secret-2")

        assertTrue(result is WifiConfigResult.Error)
        assertEquals(1, manager.confirmManualWifiProvisionCalls.size)
        assertEquals(0, manager.reconnectAndWaitCalls)
    }

    @Test
    fun `updateWifiConfig returns readable credential replay failure copy`() = runTest {
        val session = BleSession.fromPeripheral(
            BlePeripheral("badge-1", "Badge", -40)
        )
        val manager = FakeDeviceConnectionManager().apply {
            stubConfirmManualWifiProvisionResult = ConnectionState.Error(
                ConnectivityError.WifiDisconnected(
                    reason = WifiDisconnectedReason.CREDENTIAL_REPLAY_FAILED,
                    badgeSsid = "OfficeGuest"
                )
            )
            setState(ConnectionState.Connected(session))
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubProvisionResult = Result.Success(
                ProvisioningStatus(
                    wifiSsid = "OfficeGuest",
                    handshakeId = "manual-update",
                    credentialsHash = "hash"
                )
            )
        }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = provisioner,
            sessionStore = InMemorySessionStore().apply { saveSession(session) },
            registryManager = noOpRegistry()
        )

        val result = service.updateWifiConfig(ssid = "OfficeGuest", password = "secret-2")

        assertEquals(
            WifiConfigResult.Error("已尝试恢复已保存 Wi‑Fi，但设备仍未接入网络，请重新输入凭据"),
            result
        )
    }

    @Test
    fun `reconnect routes badge wifi offline to wifi mismatch with diagnostic message`() = runTest {
        val manager = FakeDeviceConnectionManager().apply {
            stubReconnectAndWaitResult = ConnectionState.Error(
                ConnectivityError.WifiDisconnected(
                    reason = WifiDisconnectedReason.BADGE_WIFI_OFFLINE,
                    phoneSsid = "OfficeGuest"
                )
            )
        }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = FakeWifiProvisioner(),
            sessionStore = InMemorySessionStore(),
            registryManager = noOpRegistry()
        )

        val result = service.reconnect()

        assertEquals(
            ReconnectResult.WifiMismatch(
                currentPhoneSsid = "OfficeGuest",
                errorMessage = "设备当前未接入可用 Wi‑Fi，请重新输入凭据"
            ),
            result
        )
    }

    @Test
    fun `reconnect routes credential replay failure to wifi mismatch with diagnostic message`() = runTest {
        val manager = FakeDeviceConnectionManager().apply {
            stubReconnectAndWaitResult = ConnectionState.Error(
                ConnectivityError.WifiDisconnected(
                    reason = WifiDisconnectedReason.CREDENTIAL_REPLAY_FAILED,
                    phoneSsid = "OfficeGuest"
                )
            )
        }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = FakeWifiProvisioner(),
            sessionStore = InMemorySessionStore(),
            registryManager = noOpRegistry()
        )

        val result = service.reconnect()

        assertEquals(
            ReconnectResult.WifiMismatch(
                currentPhoneSsid = "OfficeGuest",
                errorMessage = "已尝试恢复已保存 Wi‑Fi，但设备仍未接入网络，请重新输入凭据"
            ),
            result
        )
    }

    @Test
    fun `updateWifiConfig returns TransportConfirmedHttpDelayed when manager returns WifiProvisionedHttpDelayed`() = runTest {
        val session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40))
        val manager = FakeDeviceConnectionManager().apply {
            stubConfirmManualWifiProvisionResult = ConnectionState.WifiProvisionedHttpDelayed(
                session = session,
                status = ProvisioningStatus(
                    wifiSsid = "OfficeGuest",
                    handshakeId = "manual-confirm-delayed",
                    credentialsHash = "hash"
                ),
                baseUrl = "http://192.168.0.9:8088"
            )
            setState(ConnectionState.Connected(session))
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubProvisionResult = Result.Success(
                ProvisioningStatus(
                    wifiSsid = "OfficeGuest",
                    handshakeId = "manual-update",
                    credentialsHash = "hash"
                )
            )
        }
        val sessionStore = InMemorySessionStore().apply { saveSession(session) }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = provisioner,
            sessionStore = sessionStore,
            registryManager = noOpRegistry()
        )

        val result = service.updateWifiConfig(ssid = "OfficeGuest", password = "secret-2")

        assertEquals(
            WifiConfigResult.TransportConfirmedHttpDelayed(
                badgeSsid = "OfficeGuest",
                baseUrl = "http://192.168.0.9:8088"
            ),
            result
        )
        assertEquals(1, manager.confirmManualWifiProvisionCalls.size)
    }

    @Test
    fun `updateWifiConfig returns Success when manager returns WifiProvisioned regression check`() = runTest {
        val session = BleSession.fromPeripheral(BlePeripheral("badge-1", "Badge", -40))
        val manager = FakeDeviceConnectionManager().apply {
            stubConfirmManualWifiProvisionResult = ConnectionState.WifiProvisioned(
                session = session,
                status = ProvisioningStatus(
                    wifiSsid = "OfficeGuest",
                    handshakeId = "manual-confirm-ok",
                    credentialsHash = "hash"
                )
            )
            setState(ConnectionState.Connected(session))
        }
        val provisioner = FakeWifiProvisioner().apply {
            stubProvisionResult = Result.Success(
                ProvisioningStatus(
                    wifiSsid = "OfficeGuest",
                    handshakeId = "manual-update",
                    credentialsHash = "hash"
                )
            )
        }
        val sessionStore = InMemorySessionStore().apply { saveSession(session) }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = provisioner,
            sessionStore = sessionStore,
            registryManager = noOpRegistry()
        )

        val result = service.updateWifiConfig(ssid = "OfficeGuest", password = "secret-2")

        assertEquals(WifiConfigResult.Success, result)
    }

    @Test
    fun `disconnect calls markManuallyDisconnected true for active device before disconnectBle`() = runTest {
        val activeMac = "AA:BB:CC:DD:EE:FF"
        val session = BleSession.fromPeripheral(BlePeripheral(activeMac, "Badge", -40))
        val manager = FakeDeviceConnectionManager().apply {
            setState(ConnectionState.Connected(session))
        }
        val trackingRegistry = TrackingDeviceRegistryManager(activeMac)
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = FakeWifiProvisioner(),
            sessionStore = InMemorySessionStore(),
            registryManager = trackingRegistry
        )

        service.disconnect()

        val calls = trackingRegistry.markManuallyDisconnectedCalls
        assertEquals(1, calls.size)
        assertEquals(activeMac, calls[0].first)
        assertEquals(true, calls[0].second)
        assertEquals(1, manager.disconnectCalls)
    }

    @Test
    fun `connect calls markManuallyDisconnected false before switching device`() = runTest {
        val targetMac = "AA:BB:CC:DD:EE:FF"
        val trackingRegistry = TrackingDeviceRegistryManager(targetMac)
        val service = RealConnectivityService(
            deviceManager = FakeDeviceConnectionManager(),
            wifiProvisioner = FakeWifiProvisioner(),
            sessionStore = InMemorySessionStore(),
            registryManager = trackingRegistry
        )

        service.connect(targetMac)

        val markCalls = trackingRegistry.markManuallyDisconnectedCalls
        assertEquals(1, markCalls.size)
        assertEquals(targetMac, markCalls[0].first)
        assertEquals(false, markCalls[0].second)
        assertEquals(1, trackingRegistry.switchToDeviceCalls)
    }
}

private fun noOpRegistry(): DeviceRegistryManager = NoOpDeviceRegistryManager()

private class NoOpDeviceRegistryManager : DeviceRegistryManager {
    private val _devices = MutableStateFlow<List<RegisteredDevice>>(emptyList())
    private val _active = MutableStateFlow<RegisteredDevice?>(null)
    override val registeredDevices: StateFlow<List<RegisteredDevice>> = _devices.asStateFlow()
    override val activeDevice: StateFlow<RegisteredDevice?> = _active.asStateFlow()
    override fun registerDevice(p: com.smartsales.prism.data.connectivity.legacy.BlePeripheral, s: com.smartsales.prism.data.connectivity.legacy.BleSession) = Unit
    override fun renameDevice(mac: String, name: String) = Unit
    override fun setDefault(mac: String) = Unit
    override suspend fun switchToDevice(mac: String) = Unit
    override fun removeDevice(mac: String) = Unit
    override fun initializeOnLaunch() = Unit
    override fun markManuallyDisconnected(mac: String, value: Boolean) = Unit
    override fun updateBleDetected(mac: String, value: Boolean) = Unit
}

private class TrackingDeviceRegistryManager(activeMac: String) : DeviceRegistryManager {
    private val activeDevice0 = RegisteredDevice(
        macAddress = activeMac, displayName = "Badge", profileId = null,
        registeredAtMillis = 1_000L, lastConnectedAtMillis = 1_000L, isDefault = true
    )
    private val _devices = MutableStateFlow(listOf(activeDevice0))
    private val _active = MutableStateFlow<RegisteredDevice?>(activeDevice0)
    override val registeredDevices: StateFlow<List<RegisteredDevice>> = _devices.asStateFlow()
    override val activeDevice: StateFlow<RegisteredDevice?> = _active.asStateFlow()

    val markManuallyDisconnectedCalls = mutableListOf<Pair<String, Boolean>>()
    var switchToDeviceCalls = 0

    override fun registerDevice(p: com.smartsales.prism.data.connectivity.legacy.BlePeripheral, s: com.smartsales.prism.data.connectivity.legacy.BleSession) = Unit
    override fun renameDevice(mac: String, name: String) = Unit
    override fun setDefault(mac: String) = Unit
    override suspend fun switchToDevice(mac: String) { switchToDeviceCalls++ }
    override fun removeDevice(mac: String) = Unit
    override fun initializeOnLaunch() = Unit
    override fun markManuallyDisconnected(mac: String, value: Boolean) {
        markManuallyDisconnectedCalls += mac to value
    }
    override fun updateBleDetected(mac: String, value: Boolean) = Unit
}
