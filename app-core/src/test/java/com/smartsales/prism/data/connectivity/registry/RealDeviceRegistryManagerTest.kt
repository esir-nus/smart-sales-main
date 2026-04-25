// File: app-core/src/test/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManagerTest.kt
// Module: :app-core
// Summary: 测试手动断开标记的持久化、流更新及启动跳过自动重连逻辑
// Author: created on 2026-04-25
package com.smartsales.prism.data.connectivity.registry

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.prism.data.connectivity.legacy.FakeDeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.InMemorySessionStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
}
