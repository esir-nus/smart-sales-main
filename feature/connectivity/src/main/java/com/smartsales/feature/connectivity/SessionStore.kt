package com.smartsales.feature.connectivity

/**
 * Persistence layer for BLE session and WiFi credentials
 * 
 * Enables session restoration after app restart.
 */
interface SessionStore {
    /**
     * Persist session and credentials
     */
    fun save(session: BleSession, credentials: WifiCredentials)
    
    /**
     * Load saved session and credentials
     * 
     * @return Pair of session and credentials, or null if nothing saved
     */
    fun load(): Pair<BleSession, WifiCredentials>?
    
    /**
     * Clear saved session (called on unpair)
     */
    fun clear()
}
