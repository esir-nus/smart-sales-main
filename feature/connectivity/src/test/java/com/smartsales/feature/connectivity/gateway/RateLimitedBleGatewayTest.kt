package com.smartsales.feature.connectivity.gateway

import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RateLimitedBleGatewayTest {

    private lateinit var fakeGateway: RecordingBleGateway
    private lateinit var rateLimitedGateway: RateLimitedBleGateway
    private val testSession = BleSession(
        peripheralId = "test-device",
        peripheralName = "TestBadge",
        signalStrengthDbm = -50,
        profileId = null,
        secureToken = "token",
        establishedAtMillis = 0L
    )

    @Before
    fun setup() {
        fakeGateway = RecordingBleGateway()
        rateLimitedGateway = RateLimitedBleGateway(
            delegate = fakeGateway,
            config = RateLimitedBleGateway.RateLimitConfig(networkQueryTtlMs = 100)
        )
    }

    @Test
    fun `first query calls delegate`() = runTest {
        fakeGateway.networkResult = NetworkQueryResult.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.1.100",
                deviceWifiName = "TestWiFi",
                phoneWifiName = "",
                rawResponse = "IP#192.168.1.100"
            )
        )

        val result = rateLimitedGateway.queryNetwork(testSession)

        assertEquals(1, fakeGateway.queryNetworkCallCount)
        assertTrue(result is NetworkQueryResult.Success)
        assertEquals("192.168.1.100", (result as NetworkQueryResult.Success).status.ipAddress)
    }

    @Test
    fun `second query within TTL returns cached result`() = runTest {
        fakeGateway.networkResult = NetworkQueryResult.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.1.100",
                deviceWifiName = "TestWiFi",
                phoneWifiName = "",
                rawResponse = "IP#192.168.1.100"
            )
        )

        rateLimitedGateway.queryNetwork(testSession)
        val result = rateLimitedGateway.queryNetwork(testSession)

        // Only 1 call to delegate, second was cached
        assertEquals(1, fakeGateway.queryNetworkCallCount)
        assertTrue(result is NetworkQueryResult.Success)
    }

    @Test
    fun `query after TTL expiry calls delegate again`() = runTest {
        // Use real time for this test since RateLimitedBleGateway uses System.currentTimeMillis()
        val realTimeGateway = RateLimitedBleGateway(
            delegate = fakeGateway,
            config = RateLimitedBleGateway.RateLimitConfig(networkQueryTtlMs = 50)
        )
        fakeGateway.networkResult = NetworkQueryResult.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.1.100",
                deviceWifiName = "TestWiFi",
                phoneWifiName = "",
                rawResponse = "IP#192.168.1.100"
            )
        )

        realTimeGateway.queryNetwork(testSession)
        
        // Wait for TTL to expire using real time
        Thread.sleep(60)
        
        realTimeGateway.queryNetwork(testSession)

        // 2 calls to delegate after TTL expired
        assertEquals(2, fakeGateway.queryNetworkCallCount)
    }

    @Test
    fun `error results are not cached`() = runTest {
        fakeGateway.networkResult = NetworkQueryResult.Timeout(5000)

        rateLimitedGateway.queryNetwork(testSession)
        
        // Second query should still hit delegate (error not cached)
        fakeGateway.networkResult = NetworkQueryResult.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.1.100",
                deviceWifiName = "TestWiFi",
                phoneWifiName = "",
                rawResponse = "IP#192.168.1.100"
            )
        )
        val result = rateLimitedGateway.queryNetwork(testSession)

        assertEquals(2, fakeGateway.queryNetworkCallCount)
        assertTrue(result is NetworkQueryResult.Success)
    }

    @Test
    fun `clearCache forces next query to hit delegate`() = runTest {
        fakeGateway.networkResult = NetworkQueryResult.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.1.100",
                deviceWifiName = "TestWiFi",
                phoneWifiName = "",
                rawResponse = "IP#192.168.1.100"
            )
        )

        rateLimitedGateway.queryNetwork(testSession)
        rateLimitedGateway.clearCache()
        rateLimitedGateway.queryNetwork(testSession)

        // 2 calls: first normal, second after cache clear
        assertEquals(2, fakeGateway.queryNetworkCallCount)
    }

    @Test
    fun `other methods delegate directly`() = runTest {
        val creds = WifiCredentials(ssid = "Test", password = "pass")
        
        rateLimitedGateway.provision(testSession, creds)
        rateLimitedGateway.requestHotspot(testSession)

        assertEquals(1, fakeGateway.provisionCallCount)
        assertEquals(1, fakeGateway.requestHotspotCallCount)
    }

    // Recording fake for testing
    private class RecordingBleGateway : BleGateway {
        var networkResult: NetworkQueryResult = NetworkQueryResult.Timeout(5000)
        var queryNetworkCallCount = 0
        var provisionCallCount = 0
        var requestHotspotCallCount = 0

        override suspend fun provision(session: BleSession, credentials: WifiCredentials): BleGatewayResult {
            provisionCallCount++
            return BleGatewayResult.Success("handshake", "hash")
        }

        override suspend fun requestHotspot(session: BleSession): HotspotResult {
            requestHotspotCallCount++
            return HotspotResult.Timeout(5000)
        }

        override suspend fun queryNetwork(session: BleSession): NetworkQueryResult {
            queryNetworkCallCount++
            return networkResult
        }

        override suspend fun sendGifCommand(session: BleSession, command: GifCommand): GifCommandResult =
            GifCommandResult.Timeout(5000)

        override suspend fun sendWavCommand(session: BleSession, command: WavCommand): WavCommandResult =
            WavCommandResult.Timeout(5000)

        override fun listenForTimeSync(session: BleSession): Flow<TimeSyncEvent> = emptyFlow()

        override fun forget(peripheral: BlePeripheral) {}
    }
}
