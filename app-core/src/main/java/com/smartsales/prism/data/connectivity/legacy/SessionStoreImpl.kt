package com.smartsales.prism.data.connectivity.legacy

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * SharedPreferences-backed implementation of SessionStore
 * 
 * Persists BLE session and WiFi credentials to disk.
 */
class SharedPrefsSessionStore(context: Context) : SessionStore {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    override fun save(session: BleSession, credentials: WifiCredentials) {
        saveSession(session)
        upsertKnownNetwork(credentials)
    }

    override fun saveSession(session: BleSession) {
        prefs.edit().apply {
            // BLE Session fields
            putString(KEY_PERIPHERAL_ID, session.peripheralId)
            putString(KEY_PERIPHERAL_NAME, session.peripheralName)
            putInt(KEY_SIGNAL_STRENGTH, session.signalStrengthDbm)
            putString(KEY_PROFILE_ID, session.profileId)
            putString(KEY_SECURE_TOKEN, session.secureToken)
            putLong(KEY_ESTABLISHED_AT, session.establishedAtMillis)
            apply()
        }
    }
    
    override fun loadSession(): BleSession? {
        val peripheralId = prefs.getString(KEY_PERIPHERAL_ID, null) ?: return null
        
        return BleSession(
            peripheralId = peripheralId,
            peripheralName = prefs.getString(KEY_PERIPHERAL_NAME, null) ?: return null,
            signalStrengthDbm = prefs.getInt(KEY_SIGNAL_STRENGTH, 0),
            profileId = prefs.getString(KEY_PROFILE_ID, null),
            secureToken = prefs.getString(KEY_SECURE_TOKEN, null) ?: return null,
            establishedAtMillis = prefs.getLong(KEY_ESTABLISHED_AT, 0L)
        )
    }

    override fun upsertKnownNetwork(credentials: WifiCredentials, lastUsedAtMillis: Long) {
        val normalizedSsid = normalizeWifiSsid(credentials.ssid) ?: return
        val existing = loadKnownNetworks()
            .filterNot { it.normalizedSsid == normalizedSsid }
            .toMutableList()
        existing += KnownWifiCredential(
            credentials = credentials,
            normalizedSsid = normalizedSsid,
            lastUsedAtMillis = lastUsedAtMillis
        )

        prefs.edit().apply {
            putString(KEY_KNOWN_NETWORKS_JSON, encodeKnownNetworks(existing))
            // 保留旧字段，便于平滑迁移和回滚
            putString(KEY_WIFI_SSID, credentials.ssid)
            putString(KEY_WIFI_PASSWORD, credentials.password)
            apply()
        }
    }

    override fun loadKnownNetworks(): List<KnownWifiCredential> {
        migrateLegacySingleNetworkIfNeeded()
        val raw = prefs.getString(KEY_KNOWN_NETWORKS_JSON, null).orEmpty()
        if (raw.isBlank()) return emptyList()

        return runCatching {
            decodeKnownNetworks(raw)
        }.getOrDefault(emptyList())
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
        private const val KEY_KNOWN_NETWORKS_JSON = "known_networks_json"
        private const val KEY_KNOWN_WIFI_LAST_USED_AT = "known_wifi_last_used_at"
    }

    private fun migrateLegacySingleNetworkIfNeeded() {
        if (prefs.contains(KEY_KNOWN_NETWORKS_JSON)) {
            return
        }

        val legacySsid = prefs.getString(KEY_WIFI_SSID, null)
        val legacyPassword = prefs.getString(KEY_WIFI_PASSWORD, null)
        val normalizedSsid = normalizeWifiSsid(legacySsid) ?: return
        val credentials = WifiCredentials(
            ssid = legacySsid ?: return,
            password = legacyPassword ?: return
        )
        val migrated = listOf(
            KnownWifiCredential(
                credentials = credentials,
                normalizedSsid = normalizedSsid,
                lastUsedAtMillis = prefs.getLong(KEY_KNOWN_WIFI_LAST_USED_AT, System.currentTimeMillis())
            )
        )
        prefs.edit()
            .putString(KEY_KNOWN_NETWORKS_JSON, encodeKnownNetworks(migrated))
            .apply()
    }
}

private fun encodeKnownNetworks(networks: List<KnownWifiCredential>): String {
    val array = JSONArray()
    networks.sortedByDescending { it.lastUsedAtMillis }.forEach { known ->
        array.put(
            JSONObject().apply {
                put("ssid", known.credentials.ssid)
                put("password", known.credentials.password)
                put("security", known.credentials.security.name)
                put("normalizedSsid", known.normalizedSsid)
                put("lastUsedAtMillis", known.lastUsedAtMillis)
            }
        )
    }
    return array.toString()
}

private fun decodeKnownNetworks(raw: String): List<KnownWifiCredential> {
    val array = JSONArray(raw)
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val ssid = item.optString("ssid").takeIf { it.isNotBlank() } ?: continue
            val password = item.optString("password").takeIf { it.isNotBlank() } ?: continue
            val normalizedSsid = item.optString("normalizedSsid")
                .takeIf { it.isNotBlank() }
                ?: normalizeWifiSsid(ssid)
                ?: continue
            val security = runCatching {
                WifiCredentials.Security.valueOf(item.optString("security", WifiCredentials.Security.WPA2.name))
            }.getOrDefault(WifiCredentials.Security.WPA2)

            add(
                KnownWifiCredential(
                    credentials = WifiCredentials(ssid = ssid, password = password, security = security),
                    normalizedSsid = normalizedSsid,
                    lastUsedAtMillis = item.optLong("lastUsedAtMillis", 0L)
                )
            )
        }
    }
}

/**
 * In-memory fake for testing (no Robolectric needed)
 */
class InMemorySessionStore : SessionStore {
    private var storedSession: BleSession? = null
    private var knownNetworks: List<KnownWifiCredential> = emptyList()
    
    override fun save(session: BleSession, credentials: WifiCredentials) {
        saveSession(session)
        upsertKnownNetwork(credentials)
    }
    
    override fun saveSession(session: BleSession) {
        storedSession = session
    }

    override fun loadSession(): BleSession? = storedSession

    override fun upsertKnownNetwork(credentials: WifiCredentials, lastUsedAtMillis: Long) {
        val normalizedSsid = normalizeWifiSsid(credentials.ssid) ?: return
        knownNetworks = knownNetworks
            .filterNot { it.normalizedSsid == normalizedSsid }
            .plus(
                KnownWifiCredential(
                    credentials = credentials,
                    normalizedSsid = normalizedSsid,
                    lastUsedAtMillis = lastUsedAtMillis
                )
            )
    }

    override fun loadKnownNetworks(): List<KnownWifiCredential> = knownNetworks
    
    override fun clear() {
        storedSession = null
        knownNetworks = emptyList()
    }
}
