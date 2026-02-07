package com.smartsales.feature.connectivity

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences-backed implementation of SessionStore
 * 
 * Persists BLE session and WiFi credentials to disk.
 */
class SharedPrefsSessionStore(context: Context) : SessionStore {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    override fun save(session: BleSession, credentials: WifiCredentials) {
        prefs.edit().apply {
            // BLE Session fields
            putString(KEY_PERIPHERAL_ID, session.peripheralId)
            putString(KEY_PERIPHERAL_NAME, session.peripheralName)
            putInt(KEY_SIGNAL_STRENGTH, session.signalStrengthDbm)
            putString(KEY_PROFILE_ID, session.profileId)
            putString(KEY_SECURE_TOKEN, session.secureToken)
            putLong(KEY_ESTABLISHED_AT, session.establishedAtMillis)
            
            // WiFi credentials
            putString(KEY_WIFI_SSID, credentials.ssid)
            putString(KEY_WIFI_PASSWORD, credentials.password)
            
            apply()
        }
    }
    
    override fun load(): Pair<BleSession, WifiCredentials>? {
        val peripheralId = prefs.getString(KEY_PERIPHERAL_ID, null) ?: return null
        
        val session = BleSession(
            peripheralId = peripheralId,
            peripheralName = prefs.getString(KEY_PERIPHERAL_NAME, null) ?: return null,
            signalStrengthDbm = prefs.getInt(KEY_SIGNAL_STRENGTH, 0),
            profileId = prefs.getString(KEY_PROFILE_ID, null),
            secureToken = prefs.getString(KEY_SECURE_TOKEN, null) ?: return null,
            establishedAtMillis = prefs.getLong(KEY_ESTABLISHED_AT, 0L)
        )
        
        val credentials = WifiCredentials(
            ssid = prefs.getString(KEY_WIFI_SSID, null) ?: return null,
            password = prefs.getString(KEY_WIFI_PASSWORD, null) ?: return null
        )
        
        return session to credentials
    }
    
    override fun clear() {
        prefs.edit().clear().apply()
    }
    
    companion object {
        private const val PREFS_NAME = "ble_session_store"
        
        private const val KEY_PERIPHERAL_ID = "peripheral_id"
        private const val KEY_PERIPHERAL_NAME = "peripheral_name"
        private const val KEY_SIGNAL_STRENGTH = "signal_strength_dbm"
        private const val KEY_PROFILE_ID = "profile_id"
        private const val KEY_SECURE_TOKEN = "secure_token"
        private const val KEY_ESTABLISHED_AT = "established_at_millis"
        private const val KEY_WIFI_SSID = "wifi_ssid"
        private const val KEY_WIFI_PASSWORD = "wifi_password"
    }
}

/**
 * In-memory fake for testing (no Robolectric needed)
 */
class InMemorySessionStore : SessionStore {
    private var stored: Pair<BleSession, WifiCredentials>? = null
    
    override fun save(session: BleSession, credentials: WifiCredentials) {
        stored = session to credentials
    }
    
    override fun load(): Pair<BleSession, WifiCredentials>? = stored
    
    override fun clear() {
        stored = null
    }
}
