// File: app-core/src/test/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManagerTest.kt
// Module: :app-core
// Summary: 测试手动断开标记的持久化、流更新及启动跳过自动重连逻辑
// Author: created on 2026-04-25
package com.smartsales.prism.data.connectivity.registry

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.FakeDeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.InMemorySessionStore
import com.smartsales.prism.data.connectivity.legacy.scan.FakeBleScanner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealDeviceRegistryManagerTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var registry: InMemoryDeviceRegistry
    private lateinit var sessionStore: InMemorySessionStore
    private lateinit var deviceManager: FakeDeviceConnectionManager
    private lateinit var manager: RealDeviceRegistryManager

    private val mac = "AA:BB:CC:DD:EE:FF"

    @Before
    fun setUp() = runTest(dispatcher) {
        registry = InMemoryDeviceRegistry()
        sessionStore = InMemorySessionStore()
        deviceManager = FakeDeviceConnectionManager()
        manager = RealDeviceRegistryManager(
            registry = registry,
            sessionStore = sessionStore,
            deviceConnectionManager = deviceManager,
            dispatchers = object : DispatcherProvider {
                override val io: CoroutineDispatcher = dispatcher
                override val main: CoroutineDispatcher = dispatcher
                override val default: CoroutineDispatcher = dispatcher
            },
            scope = this
        )
    }

    private fun seedDevice(manuallyDisconnected: Boolean = false) {
        registry.register(RegisteredDevice(
            macAddress = mac,
            displayName = "Badge",
            profileId = null,
            registeredAtMillis = 1_000L,
            lastConnectedAtMillis = 1_000L,
            isDefault = true,
            manuallyDisconnected = manuallyDisconnected
        ))
    }

    @Test
    fun `markManuallyDisconnected true flips field on matching device and persists`() = runTest(dispatcher) {
        seedDevice()
        manager.initializeOnLaunch()

        manager.markManuallyDisconnected(mac, true)

        assertTrue(registry.findByMac(mac)!!.manuallyDisconnected)
        assertTrue(manager.registeredDevices.value.first { it.macAddress == mac }.manuallyDisconnected)
    }

    @Test
    fun `markManuallyDisconnected false clears the persisted flag`() = runTest(dispatcher) {
        seedDevice(manuallyDisconnected = true)
        manager.initializeOnLaunch()

        manager.markManuallyDisconnected(mac, false)

        assertFalse(registry.findByMac(mac)!!.manuallyDisconnected)
    }

    @Test
    fun `markManuallyDisconnected on active device propagates flag to DeviceConnectionManager`() = runTest(dispatcher) {
        seedDevice()
        manager.initializeOnLaunch()

        manager.markManuallyDisconnected(mac, true)
        assertTrue(deviceManager.manuallyDisconnectedValue)

        manager.markManuallyDisconnected(mac, false)
        assertFalse(deviceManager.manuallyDisconnectedValue)
    }

    @Test
    fun `initializeOnLaunch skips scheduleAutoReconnect when default device is manuallyDisconnected`() = runTest(dispatcher) {
        seedDevice(manuallyDisconnected = true)

        manager.initializeOnLaunch()

        assertEquals(0, deviceManager.autoReconnectCalls)
        assertTrue(deviceManager.manuallyDisconnectedValue)
    }

    @Test
    fun `initializeOnLaunch calls scheduleAutoReconnect when default device is not manuallyDisconnected`() = runTest(dispatcher) {
        seedDevice()

        manager.initializeOnLaunch()

        assertEquals(1, deviceManager.autoReconnectCalls)
    }

    @Test
    fun `remove non-active device reseeds stale stored session to remaining default`() = runTest(dispatcher) {
        val activeMac = "11:22:33:44:55:66"
        registry.register(device(activeMac, "Active", isDefault = true, lastConnectedAtMillis = 2_000L))
        registry.register(device(mac, "Removed", isDefault = false, lastConnectedAtMillis = 1_000L))
        manager.initializeOnLaunch()
        sessionStore.saveSession(BleSession.fromPeripheral(BlePeripheral(mac, "Removed", -50)))

        manager.removeDevice(mac)

        assertEquals(activeMac, sessionStore.loadSession()?.peripheralId)
        assertEquals(0, deviceManager.forgetCalls)
    }

    @Test
    fun `remove last registered device clears stored session`() = runTest(dispatcher) {
        seedDevice()
        sessionStore.saveSession(BleSession.fromPeripheral(BlePeripheral(mac, "Badge", -50)))
        manager.initializeOnLaunch()

        manager.removeDevice(mac)

        assertNull(sessionStore.loadSession())
        assertEquals(1, deviceManager.forgetCalls)
    }

    @Test
    fun `BLE detection skips removed candidate before switching device`() = runTest {
        val schedulerDispatcher = StandardTestDispatcher(testScheduler)
        val scanner = FakeBleScanner()
        val scopedManager = RealDeviceRegistryManager(
            registry = registry,
            sessionStore = sessionStore,
            deviceConnectionManager = deviceManager,
            dispatchers = object : DispatcherProvider {
                override val io: CoroutineDispatcher = schedulerDispatcher
                override val main: CoroutineDispatcher = schedulerDispatcher
                override val default: CoroutineDispatcher = schedulerDispatcher
            },
            scope = backgroundScope,
            bleScanner = scanner
        )
        registry.register(device(mac, "Badge", isDefault = true))
        scopedManager.initializeOnLaunch()
        advanceUntilIdle()

        scopedManager.removeDevice(mac)
        scanner.setDevices(listOf(BlePeripheral(mac, "Badge", -45)))
        advanceTimeBy(2_000L)
        advanceUntilIdle()

        assertNull(registry.findByMac(mac))
        assertEquals(0, deviceManager.forceReconnectCalls)
    }

    private fun device(
        macAddress: String,
        name: String,
        isDefault: Boolean,
        lastConnectedAtMillis: Long = 1_000L
    ) = RegisteredDevice(
        macAddress = macAddress,
        displayName = name,
        profileId = null,
        registeredAtMillis = 1_000L,
        lastConnectedAtMillis = lastConnectedAtMillis,
        isDefault = isDefault,
        manuallyDisconnected = false
    )
}
