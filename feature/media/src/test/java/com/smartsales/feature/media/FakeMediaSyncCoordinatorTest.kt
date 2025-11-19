package com.smartsales.feature.media

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.ProvisioningStatus
import com.smartsales.feature.connectivity.WifiCredentials
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 文件：feature/media/src/test/java/com/smartsales/feature/media/FakeMediaSyncCoordinatorTest.kt
// 模块：:feature:media
// 说明：验证 FakeMediaSyncCoordinator 的连接依赖与同步流程
// 作者：创建于 2025-11-15
@OptIn(ExperimentalCoroutinesApi::class)
class FakeMediaSyncCoordinatorTest {

    @Test
    fun triggerSyncAppendsClipWhenReady() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val connectionManager = StubConnectionManager(readyState())
        val coordinator = FakeMediaSyncCoordinator(connectionManager, FakeDispatcherProvider(dispatcher))

        val initialSize = coordinator.state.value.items.size
        val result = coordinator.triggerSync()

        assertTrue(result is Result.Success)
        assertEquals(initialSize + 1, coordinator.state.value.items.size)
        assertNotNull(coordinator.state.value.lastSyncedAtMillis)
    }

    @Test
    fun triggerSyncFailsWhenDisconnected() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val connectionManager = StubConnectionManager(ConnectionState.Disconnected)
        val coordinator = FakeMediaSyncCoordinator(connectionManager, FakeDispatcherProvider(dispatcher))

        val result = coordinator.triggerSync()

        assertTrue(result is Result.Error)
        assertTrue(coordinator.state.value.errorMessage?.contains("设备未连接") == true)
    }

    @Test
    fun errorClearsAfterConnectionRecovers() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val connectionManager = StubConnectionManager(ConnectionState.Disconnected)
        val coordinator = FakeMediaSyncCoordinator(connectionManager, FakeDispatcherProvider(dispatcher))

        coordinator.triggerSync()
        assertTrue(coordinator.state.value.errorMessage != null)

        connectionManager.emit(readyState())
        advanceUntilIdle()

        assertNull(coordinator.state.value.errorMessage)
    }

    @Test
    fun secondSyncRequestWhileRunningReturnsError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val connectionManager = StubConnectionManager(readyState())
        val coordinator = FakeMediaSyncCoordinator(connectionManager, FakeDispatcherProvider(dispatcher))

        val first = backgroundScope.launch { coordinator.triggerSync() }
        runCurrent()
        val result = coordinator.triggerSync()
        advanceUntilIdle()

        assertTrue(result is Result.Error)
        first.cancel()
    }

    private fun readyState(): ConnectionState.WifiProvisioned {
        val session = BleSession.fromPeripheral(BlePeripheral(id = "1", name = "SmartSales", signalStrengthDbm = -55))
        val status = ProvisioningStatus(wifiSsid = "DemoWifi", handshakeId = "handshake", credentialsHash = "hash")
        return ConnectionState.WifiProvisioned(session = session, status = status)
    }

    private class StubConnectionManager(initial: ConnectionState) : DeviceConnectionManager {
        private val _state = MutableStateFlow(initial)
        override val state: StateFlow<ConnectionState> = _state.asStateFlow()

        fun emit(state: ConnectionState) {
            _state.value = state
        }

        override fun selectPeripheral(peripheral: BlePeripheral) = Unit

        override suspend fun startPairing(
            peripheral: BlePeripheral,
            credentials: WifiCredentials
        ): Result<Unit> = throw UnsupportedOperationException()

        override suspend fun retry(): Result<Unit> = throw UnsupportedOperationException()

        override fun forgetDevice() = Unit

        override suspend fun requestHotspotCredentials(): Result<WifiCredentials> =
            throw UnsupportedOperationException()

        override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> =
            throw UnsupportedOperationException()
    }
}
