// File: app-core/src/test/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManagerTest.kt
// Module: :app-core
// Summary: 测试手动断开标记的持久化、流更新及启动跳过自动重连逻辑
// Author: created on 2026-04-25
package com.smartsales.prism.data.connectivity.registry

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.ConnectionState
import com.smartsales.prism.data.connectivity.legacy.FakeDeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.InMemorySessionStore
import com.smartsales.prism.data.connectivity.legacy.ProvisioningStatus
import com.smartsales.prism.data.connectivity.legacy.scan.FakeBleScanner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
    fun `initializeOnLaunch keeps registered stored session active instead of reseeding default`() = runTest(dispatcher) {
        val defaultMac = "AA:AA:AA:AA:AA:01"
        val storedActiveMac = "BB:BB:BB:BB:BB:02"
        registry.register(device(defaultMac, "Default", isDefault = true, lastConnectedAtMillis = 1_000L))
        registry.register(device(storedActiveMac, "Stored Active", isDefault = false, lastConnectedAtMillis = 2_000L))
        sessionStore.saveSession(BleSession.fromPeripheral(BlePeripheral(storedActiveMac, "Stored Active", -45)))

        manager.initializeOnLaunch()

        assertEquals(storedActiveMac, manager.activeDevice.value?.macAddress)
        assertEquals(storedActiveMac, sessionStore.loadSession()?.peripheralId)
        assertEquals(defaultMac, registry.getDefault()?.macAddress)
        assertEquals(1, deviceManager.autoReconnectCalls)
    }

    @Test
    fun `initializeOnLaunch falls back to latest intended badge instead of default when session is missing`() = runTest(dispatcher) {
        val defaultMac = "AA:AA:AA:AA:AA:01"
        val latestMac = "BB:BB:BB:BB:BB:02"
        registry.register(
            device(
                defaultMac,
                "Default",
                isDefault = true,
                lastConnectedAtMillis = 9_000L,
                lastUserIntentAtMillis = 1_000L
            )
        )
        registry.register(
            device(
                latestMac,
                "Latest",
                isDefault = false,
                lastConnectedAtMillis = 2_000L,
                lastUserIntentAtMillis = 8_000L
            )
        )

        manager.initializeOnLaunch()

        assertEquals(latestMac, manager.activeDevice.value?.macAddress)
        assertEquals(latestMac, sessionStore.loadSession()?.peripheralId)
        assertEquals(defaultMac, registry.getDefault()?.macAddress)
        assertEquals(1, deviceManager.autoReconnectCalls)
    }

    @Test
    fun `switchToDevice stamps explicit intent without mutating last transport success`() = runTest(dispatcher) {
        val targetMac = "BB:BB:BB:BB:BB:02"
        registry.register(
            device(
                macAddress = targetMac,
                name = "Target",
                isDefault = true,
                lastConnectedAtMillis = 1_000L,
                lastUserIntentAtMillis = 500L,
                manuallyDisconnected = true
            )
        )
        manager.initializeOnLaunch()
        val before = registry.findByMac(targetMac)!!

        manager.switchToDevice(targetMac)

        val after = registry.findByMac(targetMac)!!
        assertFalse(after.manuallyDisconnected)
        assertEquals(before.lastConnectedAtMillis, after.lastConnectedAtMillis)
        assertTrue(after.lastUserIntentAtMillis > before.lastUserIntentAtMillis)
        assertEquals(false, deviceManager.manuallyDisconnectedValue)
        assertEquals(targetMac, deviceManager.forceReconnectSession?.peripheralId)
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
    fun `BLE detection marks non-active default but reconnects only active non-default when both advertise`() = runTest {
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

        assertEquals(activeMac, manager.activeDevice.value?.macAddress)
        assertEquals(1, deviceManager.forceReconnectCalls)
        assertNull(deviceManager.forceReconnectSession)
        assertEquals(activeMac, sessionStore.loadSession()?.peripheralId)
        assertTrue(registry.findByMac(defaultMac)!!.bleDetected)
        assertTrue(registry.findByMac(activeMac)!!.bleDetected)
        assertEquals(
            "[ActiveOnly] non-active BLE candidates marked only; active remains $activeMac " +
                "source=direct-candidate candidates=$defaultMac,$activeMac " +
                "nonActiveCandidates=$defaultMac",
            manager.nonActiveBleCandidatesEvidenceLine(
                candidateMacs = listOf(defaultMac, activeMac),
                source = "direct-candidate"
            )
        )
    }

    @Test
    fun `BLE detection of non-active default only marks proximity when active is absent`() = runTest {
        val defaultMac = "AA:AA:AA:AA:AA:01"
        val activeMac = "BB:BB:BB:BB:BB:02"
        registry.register(device(defaultMac, "Default", isDefault = true))
        registry.register(device(activeMac, "Active", isDefault = false, lastConnectedAtMillis = 2_000L))
        manager.initializeOnLaunch()
        manager.switchToDevice(activeMac)
        deviceManager.reset()

        manager.handleBleDetectionCandidates(listOf(BlePeripheral(defaultMac, "Default", -44)))

        assertEquals(activeMac, manager.activeDevice.value?.macAddress)
        assertEquals(0, deviceManager.forceReconnectCalls)
        assertEquals(activeMac, sessionStore.loadSession()?.peripheralId)
        assertTrue(registry.findByMac(defaultMac)!!.bleDetected)
    }

    @Test
    fun `manual disconnect of active badge suppresses auto reconnect when active or default is detected`() = runTest {
        val defaultMac = "AA:AA:AA:AA:AA:01"
        val activeMac = "BB:BB:BB:BB:BB:02"
        registry.register(device(defaultMac, "Default", isDefault = true, lastConnectedAtMillis = 1_000L))
        registry.register(device(activeMac, "Active", isDefault = false, lastConnectedAtMillis = 2_000L))
        manager.initializeOnLaunch()
        manager.markManuallyDisconnected(activeMac, true)
        deviceManager.reset()

        manager.handleBleDetectionCandidates(listOf(
            BlePeripheral(defaultMac, "Default", -44),
            BlePeripheral(activeMac, "Active", -40)
        ))

        assertEquals(activeMac, manager.activeDevice.value?.macAddress)
        assertEquals(0, deviceManager.forceReconnectCalls)
        assertEquals(activeMac, sessionStore.loadSession()?.peripheralId)
        assertTrue(registry.findByMac(defaultMac)!!.bleDetected)
        assertTrue(registry.findByMac(activeMac)!!.bleDetected)
    }

    @Test
    fun `passive proximity seeing inactive badge only marks that badge without active reconnect`() = runTest {
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
        val inactiveMac = "AA:AA:AA:AA:AA:01"
        val activeMac = "BB:BB:BB:BB:BB:02"
        registry.register(device(inactiveMac, "Inactive", isDefault = true))
        registry.register(device(activeMac, "Active", isDefault = false, lastConnectedAtMillis = 2_000L))
        scopedManager.initializeOnLaunch()
        runCurrent()
        scopedManager.switchToDevice(activeMac)
        deviceManager.reset()

        scanner.setDevices(listOf(BlePeripheral(inactiveMac, "Inactive", -44)))
        advanceTimeBy(2_001L)
        advanceUntilIdle()

        assertEquals(activeMac, scopedManager.activeDevice.value?.macAddress)
        assertEquals(activeMac, sessionStore.loadSession()?.peripheralId)
        assertEquals(0, deviceManager.forceReconnectCalls)
        assertTrue(registry.findByMac(inactiveMac)!!.bleDetected)
        assertFalse(registry.findByMac(activeMac)!!.bleDetected)
        assertEquals(
            "[ActiveOnly] non-active BLE candidates marked only; active remains $activeMac " +
                "source=passive candidates=$inactiveMac nonActiveCandidates=$inactiveMac",
            scopedManager.nonActiveBleCandidatesEvidenceLine(
                candidateMacs = listOf(inactiveMac),
                source = "passive"
            )
        )
    }

    @Test
    fun `passive proximity seeing active and inactive badges can reconnect only active target`() = runTest {
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
        val inactiveMac = "AA:AA:AA:AA:AA:01"
        val activeMac = "BB:BB:BB:BB:BB:02"
        registry.register(device(inactiveMac, "Inactive", isDefault = true))
        registry.register(device(activeMac, "Active", isDefault = false, lastConnectedAtMillis = 2_000L))
        scopedManager.initializeOnLaunch()
        runCurrent()
        scopedManager.switchToDevice(activeMac)
        deviceManager.reset()

        scanner.setDevices(listOf(
            BlePeripheral(inactiveMac, "Inactive", -44),
            BlePeripheral(activeMac, "Active", -40)
        ))
        advanceTimeBy(2_001L)
        advanceUntilIdle()

        assertEquals(activeMac, scopedManager.activeDevice.value?.macAddress)
        assertEquals(activeMac, sessionStore.loadSession()?.peripheralId)
        assertEquals(1, deviceManager.forceReconnectCalls)
        assertTrue(registry.findByMac(activeMac)!!.bleDetected)
        assertTrue(registry.findByMac(inactiveMac)!!.bleDetected)
        assertEquals(
            "[ActiveOnly] non-active BLE candidates marked only; active remains $activeMac " +
                "source=passive candidates=$inactiveMac,$activeMac nonActiveCandidates=$inactiveMac",
            scopedManager.nonActiveBleCandidatesEvidenceLine(
                candidateMacs = listOf(inactiveMac, activeMac),
                source = "passive"
            )
        )
    }

    @Test
    fun `passive proximity clears missing badge after grace window`() = runTest {
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
        val inactiveMac = "AA:AA:AA:AA:AA:01"
        val activeMac = "BB:BB:BB:BB:BB:02"
        registry.register(device(inactiveMac, "Inactive", isDefault = true))
        registry.register(device(activeMac, "Active", isDefault = false, lastConnectedAtMillis = 2_000L))
        scopedManager.initializeOnLaunch()
        runCurrent()
        scopedManager.switchToDevice(activeMac)
        deviceManager.reset()

        scanner.setDevices(listOf(BlePeripheral(inactiveMac, "Inactive", -44)))
        advanceTimeBy(2_001L)
        advanceUntilIdle()
        assertTrue(registry.findByMac(inactiveMac)!!.bleDetected)

        scanner.setDevices(emptyList())
        advanceTimeBy(8_000L)
        advanceUntilIdle()

        assertFalse(registry.findByMac(inactiveMac)!!.bleDetected)
        assertEquals(activeMac, scopedManager.activeDevice.value?.macAddress)
        assertEquals(activeMac, sessionStore.loadSession()?.peripheralId)
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
        registry.register(
            device(
                defaultMac,
                "Default",
                isDefault = true,
                lastConnectedAtMillis = 1_000L,
                lastUserIntentAtMillis = 1_000L
            )
        )
        registry.register(
            device(
                otherMac,
                "Other",
                isDefault = false,
                lastConnectedAtMillis = 2_000L,
                lastUserIntentAtMillis = 2_000L
            )
        )
        manager.initializeOnLaunch()
        val reconnectCallsAfterLaunch = deviceManager.forceReconnectCalls
        val lastUserIntentBefore = registry.findByMac(otherMac)?.lastUserIntentAtMillis

        manager.setDefault(defaultMac)

        assertEquals(defaultMac, registry.getDefault()?.macAddress)
        assertEquals(otherMac, manager.activeDevice.value?.macAddress)
        assertEquals(otherMac, sessionStore.loadSession()?.peripheralId)
        assertEquals(reconnectCallsAfterLaunch, deviceManager.forceReconnectCalls)
        assertEquals(lastUserIntentBefore, registry.findByMac(otherMac)?.lastUserIntentAtMillis)
    }

    @Test
    fun `active device follows registered runtime session without changing default`() = runTest(dispatcher) {
        val defaultMac = "AA:AA:AA:AA:AA:01"
        val otherMac = "BB:BB:BB:BB:BB:02"
        registry.register(device(defaultMac, "Default", isDefault = true, lastConnectedAtMillis = 1_000L))
        registry.register(device(otherMac, "Other", isDefault = false, lastConnectedAtMillis = 2_000L))
        manager.initializeOnLaunch()
        manager.switchToDevice(otherMac)
        sessionStore.saveSession(BleSession.fromPeripheral(BlePeripheral(defaultMac, "Default", -40)))

        manager.syncActiveDeviceToConnectionState(ConnectionState.WifiProvisioned(
            session = BleSession.fromPeripheral(BlePeripheral(defaultMac, "Default", -40)),
            status = ProvisioningStatus(
                wifiSsid = "Office",
                handshakeId = "handshake",
                credentialsHash = "credentials"
            )
        ))

        assertEquals(defaultMac, manager.activeDevice.value?.macAddress)
        assertEquals(defaultMac, registry.getDefault()?.macAddress)
        assertEquals(defaultMac, sessionStore.loadSession()?.peripheralId)
    }

    @Test
    fun `debug L25 default-priority scenario returns deterministic pass result`() = runTest(dispatcher) {
        val result = manager.debugRunBleDetectionL25Scenario(
            DebugBleDetectionL25Scenario.ActiveOnlyDualAdvertise
        )

        requireNotNull(result)
        assertEquals("L2.5", result.evidenceClass)
        assertEquals("CONNECTIVITY_ACTIVE_ONLY_DUAL_ADVERTISE", result.scenarioId)
        assertEquals(result.activeMac, result.expectedSelectedMac)
        assertEquals(result.activeMac, result.selectedMac)
        assertTrue(result.defaultBleDetected)
        assertTrue(result.activeBleDetected)
        assertFalse(result.manuallyDisconnectedDefault)
        assertTrue(result.passed)
        assertEquals(result.activeMac, manager.activeDevice.value?.macAddress)
        assertEquals(1, deviceManager.forceReconnectCalls)
        assertNull(deviceManager.forceReconnectSession)
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
        lastUserIntentAtMillis: Long = lastConnectedAtMillis,
        manuallyDisconnected: Boolean = false
    ) = RegisteredDevice(
        macAddress = macAddress,
        displayName = name,
        profileId = null,
        registeredAtMillis = 1_000L,
        lastConnectedAtMillis = lastConnectedAtMillis,
        lastUserIntentAtMillis = lastUserIntentAtMillis,
        isDefault = isDefault,
        manuallyDisconnected = manuallyDisconnected
    )
}
