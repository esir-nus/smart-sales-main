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
import com.smartsales.prism.domain.connectivity.ReconnectResult
import com.smartsales.prism.domain.connectivity.WifiConfigResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealConnectivityServiceTest {

    @Test
    fun `reconnect returns wifi mismatch when phone wifi has no saved deterministic replay credential`() = runTest {
        val manager = FakeDeviceConnectionManager().apply {
            stubReconnectAndWaitResult = ConnectionState.Error(
                ConnectivityError.WifiDisconnected(
                    reason = WifiDisconnectedReason.NO_KNOWN_CREDENTIAL_FOR_PHONE_WIFI,
                    phoneSsid = "OfficeGuest"
                )
            )
        }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = FakeWifiProvisioner(),
            sessionStore = InMemorySessionStore()
        )

        val result = service.reconnect()

        assertEquals(ReconnectResult.WifiMismatch(currentPhoneSsid = "OfficeGuest"), result)
    }

    @Test
    fun `reconnect returns wifi mismatch when phone wifi is connected but ssid is unreadable`() = runTest {
        val manager = FakeDeviceConnectionManager().apply {
            stubReconnectAndWaitResult = ConnectionState.Error(
                ConnectivityError.WifiDisconnected(
                    reason = WifiDisconnectedReason.PHONE_WIFI_SSID_UNREADABLE
                )
            )
        }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = FakeWifiProvisioner(),
            sessionStore = InMemorySessionStore()
        )

        val result = service.reconnect()

        assertEquals(ReconnectResult.WifiMismatch(currentPhoneSsid = null), result)
    }

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
            sessionStore = sessionStore
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
            sessionStore = sessionStore
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
            sessionStore = sessionStore
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
            sessionStore = sessionStore
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
            sessionStore = sessionStore
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
            sessionStore = sessionStore
        )

        val result = service.updateWifiConfig(ssid = "OfficeGuest", password = "secret-2")

        assertTrue(result is WifiConfigResult.Error)
        assertEquals(1, manager.confirmManualWifiProvisionCalls.size)
        assertEquals(0, manager.reconnectAndWaitCalls)
    }

    @Test
    fun `updateWifiConfig returns explicit http unreachable error when repair reaches wifi but not service`() = runTest {
        val session = BleSession.fromPeripheral(
            BlePeripheral("badge-1", "Badge", -40)
        )
        val manager = FakeDeviceConnectionManager().apply {
            stubConfirmManualWifiProvisionResult = ConnectionState.Error(
                ConnectivityError.WifiDisconnected(
                    reason = WifiDisconnectedReason.HTTP_UNREACHABLE,
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
            sessionStore = sessionStore
        )

        val result = service.updateWifiConfig(ssid = "OfficeGuest", password = "secret-2")

        assertEquals(
            WifiConfigResult.Error("设备已接入 Wi‑Fi，但设备服务不可达，请确认网络后重新输入"),
            result
        )
        assertEquals(1, manager.confirmManualWifiProvisionCalls.size)
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
            sessionStore = InMemorySessionStore().apply { saveSession(session) }
        )

        val result = service.updateWifiConfig(ssid = "OfficeGuest", password = "secret-2")

        assertEquals(
            WifiConfigResult.Error("已尝试恢复已保存 Wi‑Fi，但设备仍未接入网络，请重新输入凭据"),
            result
        )
    }

    @Test
    fun `updateWifiConfig returns readable phone wifi unavailable copy`() = runTest {
        val session = BleSession.fromPeripheral(
            BlePeripheral("badge-1", "Badge", -40)
        )
        val manager = FakeDeviceConnectionManager().apply {
            stubConfirmManualWifiProvisionResult = ConnectionState.Error(
                ConnectivityError.WifiDisconnected(
                    reason = WifiDisconnectedReason.PHONE_WIFI_UNAVAILABLE
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
            sessionStore = InMemorySessionStore().apply { saveSession(session) }
        )

        val result = service.updateWifiConfig(ssid = "OfficeGuest", password = "secret-2")

        assertEquals(
            WifiConfigResult.Error("手机当前未连接可用 Wi‑Fi，请先连接 Wi‑Fi 后重新输入凭据"),
            result
        )
    }

    @Test
    fun `reconnect routes http unreachable to wifi mismatch with diagnostic message`() = runTest {
        val manager = FakeDeviceConnectionManager().apply {
            stubReconnectAndWaitResult = ConnectionState.Error(
                ConnectivityError.WifiDisconnected(
                    reason = WifiDisconnectedReason.HTTP_UNREACHABLE,
                    phoneSsid = "OfficeGuest",
                    badgeSsid = "OfficeGuest"
                )
            )
        }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = FakeWifiProvisioner(),
            sessionStore = InMemorySessionStore()
        )

        val result = service.reconnect()

        assertEquals(
            ReconnectResult.WifiMismatch(
                currentPhoneSsid = "OfficeGuest",
                errorMessage = "设备已接入 Wi‑Fi，但设备服务不可达，请确认网络后重新输入"
            ),
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
            sessionStore = InMemorySessionStore()
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
            sessionStore = InMemorySessionStore()
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
    fun `reconnect routes phone wifi unavailable to wifi mismatch with diagnostic message`() = runTest {
        val manager = FakeDeviceConnectionManager().apply {
            stubReconnectAndWaitResult = ConnectionState.Error(
                ConnectivityError.WifiDisconnected(
                    reason = WifiDisconnectedReason.PHONE_WIFI_UNAVAILABLE
                )
            )
        }
        val service = RealConnectivityService(
            deviceManager = manager,
            wifiProvisioner = FakeWifiProvisioner(),
            sessionStore = InMemorySessionStore()
        )

        val result = service.reconnect()

        assertEquals(
            ReconnectResult.WifiMismatch(
                currentPhoneSsid = null,
                errorMessage = "手机当前未连接可用 Wi‑Fi，请先连接 Wi‑Fi 后重新输入凭据"
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
            sessionStore = sessionStore
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
            sessionStore = sessionStore
        )

        val result = service.updateWifiConfig(ssid = "OfficeGuest", password = "secret-2")

        assertEquals(WifiConfigResult.Success, result)
    }
}
