package com.smartsales.prism.data.connectivity.legacy.gateway

import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.DeviceNetworkStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RateLimitedBleGatewayTest {

    @Test
    fun `queryNetwork caches valid IP within TTL`() = runTest {
        val fakeGateway = FakeBleGateway()
        var currentTime = 5000L
        val rateLimited = RateLimitedBleGateway(
            delegate = fakeGateway,
            config = RateLimitedBleGateway.RateLimitConfig(networkQueryTtlMs = 10_000L),
            timeSource = { currentTime }
        )
        val session = BleSession.fromPeripheral(BlePeripheral("id1", "name", 0))

        fakeGateway.stubNetworkResult = NetworkQueryResult.Success(
            DeviceNetworkStatus("192.168.1.100", "SSID", "SSID", "raw")
        )

        // First call
        val result1 = rateLimited.queryNetwork(session)
        assertEquals(1, fakeGateway.networkCalls.size)

        // Advance 5s (within TTL)
        currentTime += 5000L
        val result2 = rateLimited.queryNetwork(session)
        assertEquals(1, fakeGateway.networkCalls.size) // Cached, no new call
        assertEquals(result1, result2)

        // Advance 6s (TTL expired)
        currentTime += 6000L
        rateLimited.queryNetwork(session)
        assertEquals(2, fakeGateway.networkCalls.size) // Fetched again
    }

    @Test
    fun `queryNetwork does not reuse cached IP for a different badge`() = runTest {
        val fakeGateway = FakeBleGateway()
        var currentTime = 5000L
        val rateLimited = RateLimitedBleGateway(
            delegate = fakeGateway,
            config = RateLimitedBleGateway.RateLimitConfig(networkQueryTtlMs = 10_000L),
            timeSource = { currentTime }
        )
        val firstSession = BleSession.fromPeripheral(BlePeripheral("id1", "name1", 0))
        val secondSession = BleSession.fromPeripheral(BlePeripheral("id2", "name2", 0))

        fakeGateway.stubNetworkResult = NetworkQueryResult.Success(
            DeviceNetworkStatus("192.168.1.100", "SSID", "SSID", "raw")
        )
        rateLimited.queryNetwork(firstSession)

        currentTime += 500L
        val throttled = rateLimited.queryNetwork(secondSession)
        assertEquals(1, fakeGateway.networkCalls.size)
        assertTrue(throttled is NetworkQueryResult.Timeout)

        currentTime += RateLimitedBleGateway.MIN_QUERY_INTERVAL_MS
        fakeGateway.stubNetworkResult = NetworkQueryResult.Success(
            DeviceNetworkStatus("192.168.1.101", "SSID", "SSID", "raw")
        )
        val secondResult = rateLimited.queryNetwork(secondSession)

        assertEquals(2, fakeGateway.networkCalls.size)
        assertEquals(
            "192.168.1.101",
            (secondResult as NetworkQueryResult.Success).status.ipAddress
        )
    }

    @Test
    fun `queryNetwork does NOT cache 0_0_0_0 but enforces MIN_QUERY_INTERVAL floor`() = runTest {
        val fakeGateway = FakeBleGateway()
        var currentTime = 5000L
        val rateLimited = RateLimitedBleGateway(
            delegate = fakeGateway,
            config = RateLimitedBleGateway.RateLimitConfig(networkQueryTtlMs = 10_000L),
            timeSource = { currentTime }
        )
        val session = BleSession.fromPeripheral(BlePeripheral("id1", "name", 0))

        fakeGateway.stubNetworkResult = NetworkQueryResult.Success(
            DeviceNetworkStatus("0.0.0.0", "SSID", "SSID", "wifi#address#0.0.0.0#SSID")
        )

        // First call - gets 0.0.0.0
        rateLimited.queryNetwork(session)
        assertEquals(1, fakeGateway.networkCalls.size)

        // Advance 500ms (< 2s floor)
        currentTime += 500L
        val result2 = rateLimited.queryNetwork(session)
        assertEquals(1, fakeGateway.networkCalls.size) // Throttled!
        assertTrue(result2 is NetworkQueryResult.Timeout)
        val timeoutResult = result2 as NetworkQueryResult.Timeout
        assertEquals(RateLimitedBleGateway.MIN_QUERY_INTERVAL_MS, timeoutResult.timeoutMillis)

        // Advance past 2s floor (2000ms from the FIRST call)
        currentTime += 1500L
        rateLimited.queryNetwork(session)
        assertEquals(2, fakeGateway.networkCalls.size) // New call proceeds, doesn't use 10s TTL
    }

    @Test
    fun `queryNetwork does NOT cache blank IP and enforces floor`() = runTest {
        val fakeGateway = FakeBleGateway()
        var currentTime = 5000L
        val rateLimited = RateLimitedBleGateway(
            delegate = fakeGateway,
            config = RateLimitedBleGateway.RateLimitConfig(networkQueryTtlMs = 10_000L),
            timeSource = { currentTime }
        )
        val session = BleSession.fromPeripheral(BlePeripheral("id1", "name", 0))

        fakeGateway.stubNetworkResult = NetworkQueryResult.Success(
            DeviceNetworkStatus("", "SSID", "SSID", "wifi#address##SSID")
        )

        // First call
        rateLimited.queryNetwork(session)
        assertEquals(1, fakeGateway.networkCalls.size)

        // Advance 1s (< 2s floor)
        currentTime += 1000L
        val result2 = rateLimited.queryNetwork(session)
        assertEquals(1, fakeGateway.networkCalls.size)
        assertTrue(result2 is NetworkQueryResult.Timeout)

        // Advance past floor
        currentTime += 1100L
        rateLimited.queryNetwork(session)
        assertEquals(2, fakeGateway.networkCalls.size) // No 10s TTL for blank IP
    }
}
