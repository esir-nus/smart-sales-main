package com.smartsales.prism.data.pairing

import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.DeviceNetworkStatus
import com.smartsales.prism.data.connectivity.legacy.FakeDeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.scan.FakeBleScanner
import com.smartsales.prism.domain.pairing.ErrorReason
import com.smartsales.prism.domain.pairing.PairingResult
import com.smartsales.prism.domain.pairing.PairingState
import com.smartsales.prism.domain.pairing.WifiCredentials
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealPairingServiceTest {

    private lateinit var bleScanner: FakeBleScanner
    private lateinit var connectionManager: FakeDeviceConnectionManager
    private lateinit var service: RealPairingService

    @Before
    fun setUp() {
        bleScanner = FakeBleScanner()
        connectionManager = FakeDeviceConnectionManager()
        connectionManager.stubNetworkResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.0.8",
                deviceWifiName = "OfficeWifi",
                phoneWifiName = "OfficeWifi",
                rawResponse = "ok"
            )
        )
        service = RealPairingService(bleScanner, connectionManager)
    }

    @Test
    fun `startScan emits plain discovered badge data`() = runTest {
        val peripheral = BlePeripheral(
            id = "badge-1",
            name = "SmartBadge Pro",
            signalStrengthDbm = -42,
            profileId = "bt311"
        )

        service.startScan()
        bleScanner.setDevices(listOf(peripheral))

        val found = awaitDeviceFound()

        assertEquals("badge-1", found.badge.id)
        assertEquals("SmartBadge Pro", found.badge.name)
        assertEquals(-42, found.badge.signalStrengthDbm)
    }

    @Test
    fun `pairBadge resolves current peripheral from scanner snapshot`() = runTest {
        val peripheral = BlePeripheral("badge-1", "SmartBadge Pro", -42, "bt311")
        service.startScan()
        bleScanner.setDevices(listOf(peripheral))
        val badge = awaitDeviceFound().badge

        val result = service.pairBadge(badge, WifiCredentials("OfficeWifi", "secret"))

        assertTrue(result is PairingResult.Success)
        assertEquals(peripheral, connectionManager.selectCalls.single())
        assertEquals(peripheral, connectionManager.pairingCalls.single().first)
    }

    @Test
    fun `pairBadge fails when badge disappears from latest scanner snapshot`() = runTest {
        val peripheral = BlePeripheral("badge-1", "SmartBadge Pro", -42, "bt311")
        service.startScan()
        bleScanner.setDevices(listOf(peripheral))
        val badge = awaitDeviceFound().badge

        bleScanner.setDevices(emptyList())
        waitForBackgroundPropagation()

        val result = service.pairBadge(badge, WifiCredentials("OfficeWifi", "secret"))

        assertTrue(result is PairingResult.Error)
        assertEquals(ErrorReason.DEVICE_NOT_FOUND, (result as PairingResult.Error).reason)
        assertTrue(service.state.value is PairingState.Error)
        assertEquals(0, connectionManager.selectCalls.size)
        assertEquals(0, connectionManager.pairingCalls.size)
    }

    @Test
    fun `cancelPairing clears discovered peripheral snapshot`() = runTest {
        val peripheral = BlePeripheral("badge-1", "SmartBadge Pro", -42, "bt311")
        service.startScan()
        bleScanner.setDevices(listOf(peripheral))
        val badge = awaitDeviceFound().badge

        service.cancelPairing()

        val result = service.pairBadge(badge, WifiCredentials("OfficeWifi", "secret"))

        assertTrue(result is PairingResult.Error)
        assertEquals(ErrorReason.DEVICE_NOT_FOUND, (result as PairingResult.Error).reason)
    }

    @Test
    fun `pairBadge preserves immediate transport failure reason in message`() = runTest {
        val peripheral = BlePeripheral("badge-1", "SmartBadge Pro", -42, "bt311")
        service.startScan()
        bleScanner.setDevices(listOf(peripheral))
        val badge = awaitDeviceFound().badge
        connectionManager.stubPairingResult = Result.Error(
            IllegalStateException("读取特征失败：6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        )

        val result = service.pairBadge(badge, WifiCredentials("OfficeWifi", "secret"))

        assertTrue(result is PairingResult.Error)
        assertEquals(
            "WiFi 配网失败: 读取特征失败：6e400003-b5a3-f393-e0a9-e50e24dcca9e",
            (result as PairingResult.Error).message
        )
        val state = service.state.value as PairingState.Error
        assertEquals(
            "WiFi 配网失败: 读取特征失败：6e400003-b5a3-f393-e0a9-e50e24dcca9e",
            state.message
        )
        assertEquals(ErrorReason.WIFI_PROVISIONING_FAILED, state.reason)
    }

    private fun awaitDeviceFound(timeoutMs: Long = 2_000): PairingState.DeviceFound {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val current = service.state.value
            if (current is PairingState.DeviceFound) {
                return current
            }
            waitForBackgroundPropagation()
        }
        throw AssertionError("Timed out waiting for DeviceFound. Current state=${service.state.value}")
    }

    private fun waitForBackgroundPropagation() {
        Thread.sleep(25)
    }
}
