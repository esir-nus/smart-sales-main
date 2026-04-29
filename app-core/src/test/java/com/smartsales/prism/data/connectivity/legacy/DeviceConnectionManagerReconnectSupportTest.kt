// File: app-core/src/test/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerReconnectSupportTest.kt
// Module: :app-core
// Summary: 测试 scheduleAutoReconnectIfNeeded 在手动断开时跳过自动重连
// Author: created on 2026-04-25
package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.BadgeEndpointRecoveryCoordinator
import com.smartsales.prism.data.connectivity.legacy.badge.FakeBadgeStateMonitor
import com.smartsales.prism.data.connectivity.legacy.gateway.BadgeNotification
import com.smartsales.prism.data.connectivity.legacy.gateway.FakeBleGateway
import com.smartsales.prism.data.connectivity.legacy.gateway.GattSessionLifecycle
import com.smartsales.prism.data.connectivity.legacy.scan.FakeBleScanner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceConnectionManagerReconnectSupportTest {

    @Test
    fun `scheduleAutoReconnectIfNeeded does not transition to AutoReconnecting when device is manually disconnected`() = runTest {
        val session = BleSession.fromPeripheral(BlePeripheral("AA:BB:CC:DD:EE:FF", "Badge", -50))
        val sessionStore = InMemorySessionStore().apply { saveSession(session) }
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Error(IllegalStateException("irrelevant")))
        val manager = newManager(
            gateway = gateway,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            sessionStore = sessionStore
        )

        manager.setManuallyDisconnected(true)
        manager.scheduleAutoReconnectIfNeeded()
        advanceUntilIdle()

        assertFalse(
            "state must not become AutoReconnecting after manual disconnect",
            manager.state.value is ConnectionState.AutoReconnecting
        )
    }

    @Test
    fun `scheduleAutoReconnectIfNeeded transitions to AutoReconnecting when device is not manually disconnected`() = runTest {
        val session = BleSession.fromPeripheral(BlePeripheral("AA:BB:CC:DD:EE:FF", "Badge", -50))
        val sessionStore = InMemorySessionStore().apply { saveSession(session) }
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Error(IllegalStateException("out of range")))
        val manager = newManager(
            gateway = gateway,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            sessionStore = sessionStore
        )

        manager.setManuallyDisconnected(false)
        manager.scheduleAutoReconnectIfNeeded()
        advanceUntilIdle()

        val state = manager.state.value
        assertFalse("state should NOT remain Disconnected — reconnect should have attempted",
            state == ConnectionState.NeedsSetup)
    }

    @Test
    fun `setManuallyDisconnected clears flag — subsequent scheduleAutoReconnect can proceed`() = runTest {
        val session = BleSession.fromPeripheral(BlePeripheral("AA:BB:CC:DD:EE:FF", "Badge", -50))
        val sessionStore = InMemorySessionStore().apply { saveSession(session) }
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Error(IllegalStateException("irrelevant")))
        val manager = newManager(
            gateway = gateway,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            sessionStore = sessionStore
        )

        manager.setManuallyDisconnected(true)
        manager.scheduleAutoReconnectIfNeeded()
        advanceUntilIdle()
        assertFalse(manager.state.value is ConnectionState.AutoReconnecting)

        manager.setManuallyDisconnected(false)
        manager.scheduleAutoReconnectIfNeeded()
        advanceUntilIdle()

        assertFalse(
            "after clearing flag, NeedsSetup should not be the state (session exists)",
            manager.state.value is ConnectionState.NeedsSetup
        )
    }

    @Test
    fun `scheduleAutoReconnectIfNeeded refreshes cleared SessionStore before connecting`() = runTest {
        val staleSession = BleSession.fromPeripheral(BlePeripheral("AA:BB:CC:DD:EE:FF", "Removed", -50))
        val sessionStore = InMemorySessionStore().apply { saveSession(staleSession) }
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit))
        val manager = newManager(
            gateway = gateway,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            sessionStore = sessionStore
        )
        sessionStore.clear()

        manager.scheduleAutoReconnectIfNeeded()
        advanceUntilIdle()

        assertEquals(ConnectionState.NeedsSetup, manager.state.value)
        assertTrue(gateway.connectCalls.isEmpty())
    }

    @Test
    fun `reconnect with badge offline prompts repair without silent credential replay`() = runTest {
        val session = BleSession.fromPeripheral(BlePeripheral("AA:BB:CC:DD:EE:FF", "Badge", -50))
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResult = Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "0.0.0.0",
                    deviceWifiName = "N/A",
                    phoneWifiName = "",
                    rawResponse = "IP#0.0.0.0 SD#N/A"
                )
            )
        }
        val sessionStore = InMemorySessionStore().apply {
            save(session, WifiCredentials("Office", "secret"))
        }
        val manager = newManager(
            gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit)),
            provisioner = provisioner,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            sessionStore = sessionStore
        )

        val result = manager.reconnectAndWait()

        assertTrue(result is ConnectionState.Error)
        assertEquals(
            WifiDisconnectedReason.BADGE_WIFI_OFFLINE,
            ((result as ConnectionState.Error).error as ConnectivityError.WifiDisconnected).reason
        )
        assertTrue(provisioner.provisionCalls.isEmpty())
    }

    @Test
    fun `reconnect fallback does not rewrite session when only another badge is found`() = runTest {
        val activeSession = BleSession.fromPeripheral(BlePeripheral("BB:BB:BB:BB:BB:02", "Active", -50))
        val defaultPeripheral = BlePeripheral("AA:AA:AA:AA:AA:01", "Default", -40)
        val sessionStore = InMemorySessionStore().apply { saveSession(activeSession) }
        val scanner = FakeBleScanner().apply {
            scanForFirstResult = defaultPeripheral
            scanForMacResult = defaultPeripheral
        }
        val gateway = FakeGattSessionLifecycle(connectResult = Result.Error(IllegalStateException("active absent")))
        val manager = newManager(
            gateway = gateway,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            sessionStore = sessionStore,
            bleScanner = scanner
        )

        val result = manager.reconnectAndWait()

        assertTrue(result is ConnectionState.Error)
        assertEquals(activeSession.peripheralId, sessionStore.loadSession()?.peripheralId)
        assertEquals(listOf(activeSession.peripheralId to 60_000L), scanner.scanForMacCalls)
        assertEquals(listOf(activeSession.peripheralId), gateway.connectCalls)
    }

    @Test
    fun `reconnect fallback reconnects active badge when it appears within target scan window`() = runTest {
        val activeSession = BleSession.fromPeripheral(BlePeripheral("BB:BB:BB:BB:BB:02", "Active", -50))
        val returnedActive = BlePeripheral(activeSession.peripheralId, "Active Fresh", -36)
        val sessionStore = InMemorySessionStore().apply { saveSession(activeSession) }
        val scanner = FakeBleScanner().apply { scanForMacResult = returnedActive }
        val gateway = SequenceGattSessionLifecycle(
            Result.Error(IllegalStateException("initial miss")),
            Result.Success(Unit)
        )
        val provisioner = FakeWifiProvisioner().apply {
            stubNetworkResult = Result.Success(
                DeviceNetworkStatus(
                    ipAddress = "192.168.0.101",
                    deviceWifiName = "Office",
                    phoneWifiName = "",
                    rawResponse = "IP#192.168.0.101 SD#Office"
                )
            )
        }
        val manager = newManager(
            gateway = gateway,
            provisioner = provisioner,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            sessionStore = sessionStore,
            bleScanner = scanner
        )

        val result = manager.reconnectAndWait()

        assertTrue(result is ConnectionState.WifiProvisioned)
        assertEquals(activeSession.peripheralId, sessionStore.loadSession()?.peripheralId)
        assertEquals("Active Fresh", sessionStore.loadSession()?.peripheralName)
        assertEquals(listOf(activeSession.peripheralId to 60_000L), scanner.scanForMacCalls)
        assertEquals(listOf(activeSession.peripheralId, activeSession.peripheralId), gateway.connectCalls)
    }

    @Test
    fun `media failure credential replay is bounded to three attempts`() = runTest {
        val session = BleSession.fromPeripheral(BlePeripheral("AA:BB:CC:DD:EE:FF", "Badge", -50))
        val provisioner = FakeWifiProvisioner().apply {
            stubProvisionResult = Result.Error(IllegalStateException("write failed"))
        }
        val sessionStore = InMemorySessionStore().apply {
            save(session, WifiCredentials("Office", "secret"))
        }
        val manager = newManager(
            gateway = FakeGattSessionLifecycle(connectResult = Result.Success(Unit)),
            provisioner = provisioner,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            sessionStore = sessionStore
        )

        val result = manager.replayLatestSavedWifiCredentialForMediaFailure()

        assertTrue(result is ConnectionState.Error)
        assertEquals(3, provisioner.provisionCalls.size)
    }

    private fun newManager(
        gateway: GattSessionLifecycle,
        provisioner: WifiProvisioner = FakeWifiProvisioner(),
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
        sessionStore: SessionStore = InMemorySessionStore(),
        bleScanner: FakeBleScanner = FakeBleScanner()
    ): DefaultDeviceConnectionManager {
        val dispatchers = object : DispatcherProvider {
            override val io = dispatcher
            override val main = dispatcher
            override val default = dispatcher
        }
        return DefaultDeviceConnectionManager(
            provisioner = provisioner,
            bleGateway = gateway,
            badgeGateway = FakeBleGateway(),
            badgeHttpClient = FakeBadgeHttpClient(),
            endpointRecoveryCoordinator = BadgeEndpointRecoveryCoordinator(),
            dispatchers = dispatchers,
            badgeStateMonitor = FakeBadgeStateMonitor(),
            sessionStore = sessionStore,
            bleScanner = bleScanner,
            scope = scope
        )
    }

    private class FakeGattSessionLifecycle(
        private val connectResult: Result<Unit>
    ) : GattSessionLifecycle {
        private val notifications = MutableSharedFlow<BadgeNotification>()
        val connectCalls = mutableListOf<String>()
        override suspend fun connect(peripheralId: String): Result<Unit> {
            connectCalls += peripheralId
            return connectResult
        }
        override suspend fun disconnect() = Unit
        override fun listenForBadgeNotifications(): Flow<BadgeNotification> = notifications
        override suspend fun isReachable(): Boolean = false
    }

    private class SequenceGattSessionLifecycle(
        private vararg val results: Result<Unit>
    ) : GattSessionLifecycle {
        private val notifications = MutableSharedFlow<BadgeNotification>()
        val connectCalls = mutableListOf<String>()
        private var index = 0
        override suspend fun connect(peripheralId: String): Result<Unit> {
            connectCalls += peripheralId
            return results.getOrElse(index++) { results.last() }
        }
        override suspend fun disconnect() = Unit
        override fun listenForBadgeNotifications(): Flow<BadgeNotification> = notifications
        override suspend fun isReachable(): Boolean = false
    }
}
