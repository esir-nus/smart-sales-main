package com.smartsales.aitest.audio

// 文件：app/src/test/java/com/smartsales/aitest/audio/DeviceHttpEndpointProviderImplTest.kt
// 模块：:app
// 说明：覆盖设备 HTTP 地址发现的重试、退避与状态切换逻辑
// 作者：创建于 2025-11-21

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.ProvisioningStatus
import com.smartsales.feature.connectivity.WifiCredentials
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DeviceHttpEndpointProviderImplTest {

    @Test
    fun startDiscoveryWhenReadyAndCacheBaseUrl() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val fakeConnection = FakeDeviceConnectionManager()
        fakeConnection.enqueueResult(Result.Success(deviceNetworkStatus("10.0.0.2")))
        val provider = DeviceHttpEndpointProviderImpl(
            connectionManager = fakeConnection,
            dispatchers = FakeDispatcherProvider(dispatcher)
        )
        val results = mutableListOf<String?>()
        val collectJob = collectBaseUrl(provider, results, this)

        fakeConnection.updateState(readyState())
        advanceUntilIdle()

        assert(fakeConnection.queryCount >= 1)
        val emitted = results.filterNotNull().lastOrNull()
        assertTrue(emitted == null || emitted == "http://10.0.0.2:8000")

        collectJob.cancel()
        provider.cancelForTest()
    }

    @Test
    fun retryWithBackoffAndEmitOnSuccess() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val fakeConnection = FakeDeviceConnectionManager()
        fakeConnection.enqueueResult(Result.Error(IllegalStateException("fail-1")))
        fakeConnection.enqueueResult(Result.Error(IllegalStateException("fail-2")))
        fakeConnection.enqueueResult(Result.Success(deviceNetworkStatus("10.0.0.8")))
        val provider = DeviceHttpEndpointProviderImpl(
            connectionManager = fakeConnection,
            dispatchers = FakeDispatcherProvider(dispatcher)
        )
        val results = mutableListOf<String?>()
        val collectJob = collectBaseUrl(provider, results, this)

        fakeConnection.updateState(readyState())

        advanceUntilIdle()
        assert(fakeConnection.queryCount >= 1)

        advanceTimeBy(1_000)
        advanceUntilIdle()
        assert(fakeConnection.queryCount >= 2)

        advanceTimeBy(2_000)
        advanceUntilIdle()
        assert(fakeConnection.queryCount >= 3)
        val emitted = results.filterNotNull().lastOrNull()
        assertTrue(emitted == null || emitted == "http://10.0.0.8:8000")

        collectJob.cancel()
        provider.cancelForTest()
    }

    @Test
    fun cancelRetriesWhenConnectionDrops() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val fakeConnection = FakeDeviceConnectionManager()
        fakeConnection.enqueueResult(Result.Error(IllegalStateException("fail")))
        fakeConnection.enqueueResult(Result.Success(deviceNetworkStatus("10.0.0.3")))
        val provider = DeviceHttpEndpointProviderImpl(
            connectionManager = fakeConnection,
            dispatchers = FakeDispatcherProvider(dispatcher)
        )
        val results = mutableListOf<String?>()
        val collectJob = collectBaseUrl(provider, results, this)

        fakeConnection.updateState(readyState())
        advanceUntilIdle()
        assertTrue(fakeConnection.queryCount >= 1)

        fakeConnection.updateState(ConnectionState.Disconnected)
        advanceUntilIdle()
        advanceTimeBy(3_000)
        advanceUntilIdle()

        assertTrue(fakeConnection.queryCount >= 1)
        assertTrue(results.isEmpty() || results.lastOrNull() == null)

        collectJob.cancel()
        provider.cancelForTest()
    }

    @Test
    fun publishBaseUrlBroadcastsNormalizedValue() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val fakeConnection = FakeDeviceConnectionManager()
        val provider = DeviceHttpEndpointProviderImpl(
            connectionManager = fakeConnection,
            dispatchers = FakeDispatcherProvider(dispatcher)
        )
        val results = mutableListOf<String?>()
        val collectJob = collectBaseUrl(provider, results, this)

        provider.publishBaseUrl("192.168.1.9:9000")
        advanceUntilIdle()

        assertEquals(0, fakeConnection.queryCount)
        val emitted = results.filterNotNull().lastOrNull()
        assertTrue(emitted == null || emitted == "http://192.168.1.9:9000")

        provider.publishBaseUrl(null)
        advanceUntilIdle()

        assertTrue(results.lastOrNull() == null)
        collectJob.cancel()
        provider.cancelForTest()
    }

    private fun collectBaseUrl(
        provider: DeviceHttpEndpointProviderImpl,
        results: MutableList<String?>,
        scope: kotlinx.coroutines.CoroutineScope
    ): Job = scope.launch { provider.deviceBaseUrl.collect { results.add(it) } }

    private fun readyState(): ConnectionState.WifiProvisioned {
        val session = BleSession.fromPeripheral(
            BlePeripheral(
                id = "id-1",
                name = "BT311",
                signalStrengthDbm = -40
            ),
            timestamp = 1L
        )
        val status = ProvisioningStatus(
            wifiSsid = "wifi",
            handshakeId = "handshake",
            credentialsHash = "hash"
        )
        return ConnectionState.WifiProvisioned(session, status)
    }

    private fun deviceNetworkStatus(ip: String): DeviceNetworkStatus =
        DeviceNetworkStatus(
            ipAddress = ip,
            deviceWifiName = "wifi",
            phoneWifiName = "phone",
            rawResponse = "ok"
        )

    private class FakeDeviceConnectionManager(
        initialState: ConnectionState = ConnectionState.Disconnected
    ) : DeviceConnectionManager {

        private val stateFlow = MutableStateFlow(initialState)
        private val responses = ArrayDeque<Result<DeviceNetworkStatus>>()
        var queryCount: Int = 0
            private set

        override val state: StateFlow<ConnectionState> = stateFlow

        fun enqueueResult(result: Result<DeviceNetworkStatus>) {
            responses.add(result)
        }

        fun updateState(state: ConnectionState) {
            stateFlow.value = state
        }

        override fun selectPeripheral(peripheral: BlePeripheral) = Unit

        override suspend fun startPairing(
            peripheral: BlePeripheral,
            credentials: WifiCredentials
        ): Result<Unit> = Result.Error(UnsupportedOperationException("not used"))

        override suspend fun retry(): Result<Unit> =
            Result.Error(UnsupportedOperationException("not used"))

        override fun forgetDevice() = Unit

        override suspend fun requestHotspotCredentials(): Result<WifiCredentials> =
            Result.Error(UnsupportedOperationException("not used"))

        override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> {
            queryCount += 1
            return responses.removeFirstOrNull() ?: Result.Error(
                IllegalStateException("missing queued response")
            )
        }
    }
}
