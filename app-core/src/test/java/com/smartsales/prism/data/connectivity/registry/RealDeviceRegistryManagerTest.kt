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

    @Test
    fun `BLE detection prefers eligible default over active non-default when both advertise`() = runTest {
        val defaultMac = "AA:AA:AA:AA:AA:01"
        val activeMac = "BB:BB:BB:BB:BB:02"
        registry.register(device(defaultMac, "Default", isDefault = true, lastConnectedAtMillis = 1_000L))
        registry.register(device(activeMac, "Active", isDefault = false, lastConnectedAtMillis = 2_000L))
        manager.initializeOnLaunch()
        manager.switchToDevice(activeMac)
        deviceManager.reset()

        manager.handleBleDetectionCandidates(listOf(
            BlePeripheral(defaultMac, "Default", -44),
            BlePeripheral(activeMac, "Active", -40)
        ))

        assertEquals(defaultMac, manager.activeDevice.value?.macAddress)
        assertEquals(defaultMac, deviceManager.forceReconnectSession?.peripheralId)
        assertTrue(registry.findByMac(defaultMac)!!.bleDetected)
        assertTrue(registry.findByMac(activeMac)!!.bleDetected)
    }

    @Test
    fun `BLE detection skips manually disconnected default and keeps active candidate`() = runTest {
        val defaultMac = "AA:AA:AA:AA:AA:01"
        val activeMac = "BB:BB:BB:BB:BB:02"
        registry.register(device(defaultMac, "Default", isDefault = true))
        registry.register(device(activeMac, "Active", isDefault = false, lastConnectedAtMillis = 2_000L))
        manager.initializeOnLaunch()
        manager.switchToDevice(activeMac)
        manager.markManuallyDisconnected(defaultMac, true)
        deviceManager.reset()

        manager.handleBleDetectionCandidates(listOf(
            BlePeripheral(defaultMac, "Default", -44),
            BlePeripheral(activeMac, "Active", -40)
        ))

        assertEquals(activeMac, manager.activeDevice.value?.macAddress)
        assertEquals(1, deviceManager.forceReconnectCalls)
        assertTrue(registry.findByMac(defaultMac)!!.bleDetected)
    }

    @Test
    fun `BLE detection reconnects single eligible registered candidate`() = runTest {
        val defaultMac = "AA:AA:AA:AA:AA:01"
        val activeMac = "BB:BB:BB:BB:BB:02"
        registry.register(device(defaultMac, "Default", isDefault = true))
        registry.register(device(activeMac, "Active", isDefault = false, lastConnectedAtMillis = 2_000L))
        manager.initializeOnLaunch()
        manager.switchToDevice(activeMac)
        manager.markManuallyDisconnected(defaultMac, true)
        deviceManager.reset()

        manager.handleBleDetectionCandidates(listOf(BlePeripheral(activeMac, "Active", -40)))

        assertEquals(activeMac, manager.activeDevice.value?.macAddress)
        assertEquals(1, deviceManager.forceReconnectCalls)
        assertTrue(registry.findByMac(activeMac)!!.bleDetected)
    }

    @Test
    fun `setDefault remains passive and does not switch seed session or reconnect`() = runTest(dispatcher) {
        val defaultMac = "AA:AA:AA:AA:AA:01"
        val otherMac = "BB:BB:BB:BB:BB:02"
        registry.register(device(defaultMac, "Default", isDefault = true, lastConnectedAtMillis = 1_000L))
        registry.register(device(otherMac, "Other", isDefault = false, lastConnectedAtMillis = 2_000L))
        manager.initializeOnLaunch()
        val reconnectCallsAfterLaunch = deviceManager.forceReconnectCalls

        manager.setDefault(otherMac)

        assertEquals(otherMac, registry.getDefault()?.macAddress)
        assertEquals(defaultMac, manager.activeDevice.value?.macAddress)
        assertEquals(defaultMac, sessionStore.loadSession()?.peripheralId)
        assertEquals(reconnectCallsAfterLaunch, deviceManager.forceReconnectCalls)
    }

    @Test
    fun `debug L25 default-priority scenario returns deterministic pass result`() = runTest(dispatcher) {
        val result = manager.debugRunBleDetectionL25Scenario(
            DebugBleDetectionL25Scenario.DefaultPriorityDualAdvertise
        )

        requireNotNull(result)
        assertEquals("L2.5", result.evidenceClass)
        assertEquals("CONNECTIVITY_DEFAULT_PRIORITY_DUAL_ADVERTISE", result.scenarioId)
        assertEquals(result.defaultMac, result.expectedSelectedMac)
        assertEquals(result.defaultMac, result.selectedMac)
        assertTrue(result.defaultBleDetected)
        assertTrue(result.activeBleDetected)
        assertFalse(result.manuallyDisconnectedDefault)
        assertTrue(result.passed)
        assertEquals(result.defaultMac, manager.activeDevice.value?.macAddress)
        assertEquals(result.defaultMac, deviceManager.forceReconnectSession?.peripheralId)
    }

    @Test
    fun `debug L25 manual-default scenario suppresses default deterministically`() = runTest(dispatcher) {
        val result = manager.debugRunBleDetectionL25Scenario(
            DebugBleDetectionL25Scenario.ManualDefaultSuppression
        )

        requireNotNull(result)
        assertEquals("L2.5", result.evidenceClass)
        assertEquals("CONNECTIVITY_MANUAL_DEFAULT_SUPPRESSION", result.scenarioId)
        assertEquals(result.activeMac, result.expectedSelectedMac)
        assertEquals(result.activeMac, result.selectedMac)
        assertTrue(result.defaultBleDetected)
        assertTrue(result.activeBleDetected)
        assertTrue(result.manuallyDisconnectedDefault)
        assertTrue(result.passed)
        assertEquals(result.activeMac, manager.activeDevice.value?.macAddress)
        assertEquals(1, deviceManager.forceReconnectCalls)
    }

    private fun device(
        macAddress: String,
        name: String,
        isDefault: Boolean,
        lastConnectedAtMillis: Long = 1_000L,
        manuallyDisconnected: Boolean = false
    ) = RegisteredDevice(
        macAddress = macAddress,
        displayName = name,
        profileId = null,
        registeredAtMillis = 1_000L,
        lastConnectedAtMillis = lastConnectedAtMillis,
        isDefault = isDefault,
        manuallyDisconnected = manuallyDisconnected
    )
}
