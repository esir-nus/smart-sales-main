package com.smartsales.feature.connectivity.badge

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.FakeWifiProvisioner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BadgeStateMonitorTest {

    private val testSession = BleSession(
        peripheralId = "test-device",
        peripheralName = "Test Badge",
        signalStrengthDbm = -50,
        profileId = null,
        secureToken = "token",
        establishedAtMillis = System.currentTimeMillis()
    )

    @Test
    fun `initial state is UNKNOWN`() = runTest {
        val monitor = createMonitor()
        assertEquals(BadgeState.UNKNOWN, monitor.status.value.state)
    }

    @Test
    fun `onBleConnected transitions to PAIRED`() = runTest {
        val monitor = createMonitor()

        monitor.onBleConnected(testSession)

        assertEquals(BadgeState.PAIRED, monitor.status.value.state)
        assertTrue(monitor.status.value.bleConnected)
    }

    @Test
    fun `onBleDisconnected transitions to UNKNOWN`() = runTest {
        val monitor = createMonitor()
        monitor.onBleConnected(testSession)

        monitor.onBleDisconnected()

        assertEquals(BadgeState.UNKNOWN, monitor.status.value.state)
    }

    @Test
    fun `polling detects CONNECTED state with valid IP`() = runTest {
        val provisioner = FakeWifiProvisioner()
        provisioner.stubNetworkResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.1.100",
                deviceWifiName = "TestWiFi",
                phoneWifiName = "TestPhone",
                rawResponse = "wifi#address#192.168.1.100#TestWiFi"
            )
        )
        val monitor = createMonitor(provisioner)

        monitor.onBleConnected(testSession)
        advanceTimeBy(BadgeStateMonitor.MIN_POLL_GAP_MS + 100)
        runCurrent()

        assertEquals(BadgeState.CONNECTED, monitor.status.value.state)
        assertEquals("192.168.1.100", monitor.status.value.ipAddress)
    }

    @Test
    fun `polling detects OFFLINE state with 0000 IP`() = runTest {
        val provisioner = FakeWifiProvisioner()
        provisioner.stubNetworkResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "0.0.0.0",
                deviceWifiName = "",
                phoneWifiName = "",
                rawResponse = "wifi#address#0.0.0.0#"
            )
        )
        val monitor = createMonitor(provisioner)

        monitor.onBleConnected(testSession)
        advanceTimeBy(BadgeStateMonitor.MIN_POLL_GAP_MS + 100)
        runCurrent()

        assertEquals(BadgeState.OFFLINE, monitor.status.value.state)
    }

    @Test
    fun `consecutive failures increment counter`() = runTest {
        val provisioner = FakeWifiProvisioner()
        provisioner.stubNetworkResult = Result.Error(IllegalStateException("No connection"))
        val monitor = createMonitor(provisioner)

        monitor.onBleConnected(testSession)
        advanceTimeBy(BadgeStateMonitor.MIN_POLL_GAP_MS + 100)
        runCurrent()

        assertTrue(monitor.status.value.consecutiveFailures >= 1)
    }

    private fun TestScope.createMonitor(
        provisioner: FakeWifiProvisioner = FakeWifiProvisioner()
    ): RealBadgeStateMonitor {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = FakeDispatcherProvider(dispatcher)
        return RealBadgeStateMonitor(
            provisioner = provisioner,
            dispatchers = dispatchers
        )
    }
}
