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

        assertEquals(ReconnectResult.WifiMismatch, result)
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

        assertEquals(ReconnectResult.WifiMismatch, result)
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

        assertTrue(result is WifiConfigResult.Error)
        assertEquals(1, manager.confirmManualWifiProvisionCalls.size)
        assertEquals(0, manager.reconnectAndWaitCalls)
    }

    @Test
    fun `updateWifiConfig falls back to live manager state for non mismatch confirmation failure`() = runTest {
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

        assertEquals(WifiConfigResult.Success, result)
        assertEquals(1, manager.confirmManualWifiProvisionCalls.size)
        assertEquals(0, manager.reconnectAndWaitCalls)
    }
}
