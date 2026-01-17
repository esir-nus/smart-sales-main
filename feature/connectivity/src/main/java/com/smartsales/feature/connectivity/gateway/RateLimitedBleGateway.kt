package com.smartsales.feature.connectivity.gateway

import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.ConnectivityLogger
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for the real (undecorated) BLE gateway implementation.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RealGateway

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
@Singleton
class RateLimitedBleGateway @Inject constructor(
    @RealGateway private val delegate: BleGateway,
    private val config: RateLimitConfig = RateLimitConfig()
) : BleGateway {

    private val networkQueryMutex = Mutex()
    private var lastQueryMs = 0L
    private var cachedResult: DeviceNetworkStatus? = null

    // === Delegated methods (no rate limiting) ===

    override suspend fun provision(
        session: BleSession,
        credentials: WifiCredentials
    ): BleGatewayResult = delegate.provision(session, credentials)

    override suspend fun requestHotspot(session: BleSession): HotspotResult =
        delegate.requestHotspot(session)

    override suspend fun sendGifCommand(
        session: BleSession,
        command: GifCommand
    ): GifCommandResult = delegate.sendGifCommand(session, command)

    override suspend fun sendWavCommand(
        session: BleSession,
        command: WavCommand
    ): WavCommandResult = delegate.sendWavCommand(session, command)

    override fun listenForTimeSync(session: BleSession): Flow<TimeSyncEvent> =
        delegate.listenForTimeSync(session)

    override fun forget(peripheral: BlePeripheral) = delegate.forget(peripheral)

    // === Rate-limited method ===

    override suspend fun queryNetwork(session: BleSession): NetworkQueryResult {
        return networkQueryMutex.withLock {
            ConnectivityLogger.i(">>> RateLimitedBleGateway.queryNetwork ENTRY (in mutex)")
            val now = System.currentTimeMillis()
            val timeSinceLastQuery = now - lastQueryMs
            
            // Rate limiting: return cached result if within TTL
            val cached = cachedResult
            if (cached != null && timeSinceLastQuery < config.networkQueryTtlMs) {
                ConnectivityLogger.d(
                    "RateLimitedBleGateway: CACHED (${timeSinceLastQuery}ms old)"
                )
                return@withLock NetworkQueryResult.Success(cached)
            }

            when (val result = delegate.queryNetwork(session)) {
                is NetworkQueryResult.Success -> {
                    // Only update timestamp and cache on success
                    lastQueryMs = now
                    cachedResult = result.status
                    ConnectivityLogger.d(
                        "RateLimitedBleGateway: fresh query, caching result"
                    )
                    result
                }
                else -> {
                    // Don't cache errors, don't update lastQueryMs (allow retries)
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
    }
}
