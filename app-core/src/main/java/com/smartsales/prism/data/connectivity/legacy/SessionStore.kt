package com.smartsales.prism.data.connectivity.legacy

/**
 * Persistence layer for BLE session and WiFi credentials
 * 
 * Enables session restoration after app restart.
 */
interface SessionStore {
    /**
     * Persist session and credentials
     */
    fun save(session: BleSession, credentials: WifiCredentials) {
        saveSession(session)
        upsertKnownNetwork(credentials)
    }
    
    /**
     * Load saved session and credentials
     * 
     * @return Pair of session and credentials, or null if nothing saved
     */
    fun load(): Pair<BleSession, WifiCredentials>? {
        val session = loadSession() ?: return null
        val latestKnownNetwork = loadKnownNetworks()
            .maxByOrNull { it.lastUsedAtMillis }
            ?: return null
        return session to latestKnownNetwork.credentials
    }

    /**
     * Persist only the BLE session identity.
     */
    fun saveSession(session: BleSession)

    /**
     * Load the stored BLE session, if any.
     */
    fun loadSession(): BleSession?

    /**
     * Upsert a known Wi‑Fi network by exact normalized SSID.
     */
    fun upsertKnownNetwork(
        credentials: WifiCredentials,
        lastUsedAtMillis: Long = System.currentTimeMillis()
    )

    /**
     * Load all remembered Wi‑Fi networks.
     */
    fun loadKnownNetworks(): List<KnownWifiCredential>

    /**
     * Find the best known network match for a normalized SSID.
     */
    fun findKnownNetworkBySsid(normalizedSsid: String): KnownWifiCredential? =
        loadKnownNetworks()
            .filter { it.normalizedSsid == normalizedSsid }
            .maxByOrNull { it.lastUsedAtMillis }
    
    /**
     * Clear saved session (called on unpair)
     */
    fun clear()
}
