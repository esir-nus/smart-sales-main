package com.smartsales.prism.data.connectivity.legacy.gateway

import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.ConnectivityLogger
import com.smartsales.prism.data.connectivity.legacy.DeviceNetworkStatus
import com.smartsales.prism.data.connectivity.legacy.WifiCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock





/**
 * Rate-limited decorator for [BleGateway].
 *
 * Wraps the real gateway and rate-limits [queryNetwork] calls to prevent
 * ESP32 overload. All other methods delegate directly to the underlying gateway.
 *
 * Rate limiting uses:
 * - Mutex for concurrency safety (one query at a time)
 * - TTL cache (return cached result within window)
 *
 * @param delegate The real BLE gateway to wrap
 * @param config Rate limiting configuration (TTL, etc.)
 */
class RateLimitedBleGateway(
    private val delegate: BleGateway,
    private val config: RateLimitConfig = RateLimitConfig(),
    private val timeSource: () -> Long = { System.currentTimeMillis() }
) : BleGateway {

    private val networkQueryMutex = Mutex()
    private var lastQueryMs = 0L
    private var cachedResult: DeviceNetworkStatus? = null

    // === Delegated methods (no rate limiting) ===

    override suspend fun provision(
        session: BleSession,
        credentials: WifiCredentials
    ): BleGatewayResult {
        // Clear cache before provisioning — old network status will be stale
        clearCache()
        return delegate.provision(session, credentials)
    }

    override suspend fun requestHotspot(session: BleSession): HotspotResult =
        delegate.requestHotspot(session)

    override suspend fun sendWavCommand(
        session: BleSession,
        command: WavCommand
    ): WavCommandResult = delegate.sendWavCommand(session, command)

    override suspend fun sendBadgeSignal(session: BleSession, payload: String) {
        delegate.sendBadgeSignal(session, payload)
    }



    override fun forget(peripheral: BlePeripheral) = delegate.forget(peripheral)

    // === Rate-limited method ===

    override suspend fun queryNetwork(session: BleSession): NetworkQueryResult {
        return networkQueryMutex.withLock {
            ConnectivityLogger.i("📡 queryNetwork ENTRY (in mutex)")
            val now = timeSource()
            val timeSinceLastQuery = now - lastQueryMs
            
            // Rate limiting: return cached result if within TTL
            val cached = cachedResult
            if (cached != null && timeSinceLastQuery < config.networkQueryTtlMs) {
                ConnectivityLogger.d(
                    "📡 CACHED (${timeSinceLastQuery}ms old)"
                )
                return@withLock NetworkQueryResult.Success(cached)
            }

            // Enforce minimum interval even for 0.0.0.0 to protect ESP32
            if (timeSinceLastQuery < MIN_QUERY_INTERVAL_MS) {
                ConnectivityLogger.d(
                    "📡 THROTTLED (${timeSinceLastQuery}ms < ${MIN_QUERY_INTERVAL_MS}ms floor)"
                )
                // Return last result or timeout if none
                val lastResult = cachedResult
                return@withLock if (lastResult != null) {
                    NetworkQueryResult.Success(lastResult)
                } else {
                    NetworkQueryResult.Timeout(MIN_QUERY_INTERVAL_MS)
                }
            }
            
            // Always update lastQueryMs to enforce minimum interval
            lastQueryMs = now
            
            when (val result = delegate.queryNetwork(session)) {
                is NetworkQueryResult.Success -> {
                    val ip = result.status.ipAddress
                    // Only cache valid IPs — don't cache 0.0.0.0 (device not connected yet)
                    if (!ip.isNullOrBlank() && ip != "0.0.0.0") {
                        cachedResult = result.status
                        ConnectivityLogger.d(
                            "📡 fresh query, caching IP=$ip"
                        )
                    } else {
                        ConnectivityLogger.d(
                            "📡 got $ip, NOT caching (retry after ${MIN_QUERY_INTERVAL_MS}ms)"
                        )
                    }
                    result
                }
                else -> {
                    // Don't cache errors
                    result
                }
            }
        }
    }

    /**
     * Clears the cached network status. Useful for testing or after
     * events that would invalidate the cache (e.g., WiFi provisioning).
     */
    fun clearCache() {
        cachedResult = null
        lastQueryMs = 0L
    }

    /**
     * Configuration for rate limiting behavior.
     */
    data class RateLimitConfig(
        /** Minimum time between actual BLE network queries */
        val networkQueryTtlMs: Long = DEFAULT_NETWORK_QUERY_TTL_MS
    )

    companion object {
        /** 10 seconds between network queries to protect ESP32 (matches poll interval) */
        const val DEFAULT_NETWORK_QUERY_TTL_MS = 10_000L
        
        /** Minimum 2s between ANY queries, even for 0.0.0.0 responses */
        const val MIN_QUERY_INTERVAL_MS = 2_000L
    }
}
