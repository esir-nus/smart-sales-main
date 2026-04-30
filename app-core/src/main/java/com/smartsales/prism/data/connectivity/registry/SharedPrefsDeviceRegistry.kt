package com.smartsales.prism.data.connectivity.registry

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * SharedPreferences-backed device registry.
 *
 * Stores registered devices as a JSON array.
 * Default device tracked via a separate key for fast lookup.
 * Follows the same persistence pattern as SharedPrefsSessionStore.
 */
class SharedPrefsDeviceRegistry(context: Context) : DeviceRegistry {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadAll(): List<RegisteredDevice> {
        val defaultMac = prefs.getString(KEY_DEFAULT_DEVICE_MAC, null)
        val raw = prefs.getString(KEY_DEVICES_JSON, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching { decodeDevices(raw, defaultMac) }.getOrDefault(emptyList())
    }

    override fun findByMac(macAddress: String): RegisteredDevice? =
        loadAll().find { it.macAddress == macAddress }

    override fun getDefault(): RegisteredDevice? =
        loadAll().find { it.isDefault }

    override fun register(device: RegisteredDevice) {
        val existing = loadAllRaw().filterNot { it.macAddress == device.macAddress }.toMutableList()
        existing += device.toRaw()
        if (device.isDefault || existing.size == 1) {
            prefs.edit()
                .putString(KEY_DEVICES_JSON, encodeDevices(existing))
                .putString(KEY_DEFAULT_DEVICE_MAC, device.macAddress)
                .apply()
        } else {
            prefs.edit()
                .putString(KEY_DEVICES_JSON, encodeDevices(existing))
                .apply()
        }
    }

    override fun rename(macAddress: String, newDisplayName: String) {
        val devices = loadAllRaw().toMutableList()
        val index = devices.indexOfFirst { it.macAddress == macAddress }
        if (index < 0) return
        devices[index] = devices[index].copy(displayName = newDisplayName)
        prefs.edit().putString(KEY_DEVICES_JSON, encodeDevices(devices)).apply()
    }

    override fun updateLastConnected(macAddress: String, timestampMillis: Long) {
        val devices = loadAllRaw().toMutableList()
        val index = devices.indexOfFirst { it.macAddress == macAddress }
        if (index < 0) return
        devices[index] = devices[index].copy(lastConnectedAtMillis = timestampMillis)
        prefs.edit().putString(KEY_DEVICES_JSON, encodeDevices(devices)).apply()
    }

    override fun updateLastUserIntent(macAddress: String, timestampMillis: Long) {
        val devices = loadAllRaw().toMutableList()
        val index = devices.indexOfFirst { it.macAddress == macAddress }
        if (index < 0) return
        devices[index] = devices[index].copy(lastUserIntentAtMillis = timestampMillis)
        prefs.edit().putString(KEY_DEVICES_JSON, encodeDevices(devices)).apply()
    }

    override fun updateMacAddress(oldMac: String, newMac: String) {
        val devices = loadAllRaw().toMutableList()
        val index = devices.indexOfFirst { it.macAddress == oldMac }
        if (index < 0) return
        devices[index] = devices[index].copy(macAddress = newMac)
        val defaultMac = prefs.getString(KEY_DEFAULT_DEVICE_MAC, null)
        val editor = prefs.edit().putString(KEY_DEVICES_JSON, encodeDevices(devices))
        if (defaultMac == oldMac) {
            editor.putString(KEY_DEFAULT_DEVICE_MAC, newMac)
        }
        editor.apply()
    }

    override fun setDefault(macAddress: String) {
        val devices = loadAllRaw()
        if (devices.none { it.macAddress == macAddress }) return
        prefs.edit().putString(KEY_DEFAULT_DEVICE_MAC, macAddress).apply()
    }

    override fun remove(macAddress: String) {
        val devices = loadAllRaw().filterNot { it.macAddress == macAddress }
        val defaultMac = prefs.getString(KEY_DEFAULT_DEVICE_MAC, null)
        val editor = prefs.edit().putString(KEY_DEVICES_JSON, encodeDevices(devices))
        if (defaultMac == macAddress) {
            val newDefault = devices.maxByOrNull { it.lastConnectedAtMillis }?.macAddress
            if (newDefault != null) {
                editor.putString(KEY_DEFAULT_DEVICE_MAC, newDefault)
            } else {
                editor.remove(KEY_DEFAULT_DEVICE_MAC)
            }
        }
        editor.apply()
    }

    override fun isEmpty(): Boolean =
        prefs.getString(KEY_DEVICES_JSON, null).isNullOrBlank()

    override fun updateManuallyDisconnected(macAddress: String, value: Boolean) {
        val devices = loadAllRaw().toMutableList()
        val index = devices.indexOfFirst { it.macAddress == macAddress }
        if (index < 0) return
        devices[index] = devices[index].copy(manuallyDisconnected = value)
        prefs.edit().putString(KEY_DEVICES_JSON, encodeDevices(devices)).apply()
    }

    override fun updateBleDetected(macAddress: String, value: Boolean) {
        val devices = loadAllRaw().toMutableList()
        val index = devices.indexOfFirst { it.macAddress == macAddress }
        if (index < 0) return
        devices[index] = devices[index].copy(bleDetected = value)
        prefs.edit().putString(KEY_DEVICES_JSON, encodeDevices(devices)).apply()
    }

    private fun loadAllRaw(): List<RegisteredDeviceRaw> {
        val raw = prefs.getString(KEY_DEVICES_JSON, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching { decodeDevicesRaw(raw) }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS_NAME = "device_registry"
        const val KEY_DEVICES_JSON = "registered_devices_json"
        const val KEY_DEFAULT_DEVICE_MAC = "default_device_mac"
    }
}

private data class RegisteredDeviceRaw(
    val macAddress: String,
    val displayName: String,
    val profileId: String?,
    val registeredAtMillis: Long,
    val lastConnectedAtMillis: Long,
    val lastUserIntentAtMillis: Long = lastConnectedAtMillis,
    val manuallyDisconnected: Boolean = false,
    val bleDetected: Boolean = false
)

private fun RegisteredDevice.toRaw() = RegisteredDeviceRaw(
    macAddress = macAddress,
    displayName = displayName,
    profileId = profileId,
    registeredAtMillis = registeredAtMillis,
    lastConnectedAtMillis = lastConnectedAtMillis,
    lastUserIntentAtMillis = lastUserIntentAtMillis,
    manuallyDisconnected = manuallyDisconnected,
    bleDetected = bleDetected
)

private fun encodeDevices(devices: List<RegisteredDeviceRaw>): String {
    val array = JSONArray()
    devices.forEach { device ->
        array.put(JSONObject().apply {
            put("macAddress", device.macAddress)
            put("displayName", device.displayName)
            put("profileId", device.profileId ?: JSONObject.NULL)
            put("registeredAtMillis", device.registeredAtMillis)
            put("lastConnectedAtMillis", device.lastConnectedAtMillis)
            put("lastUserIntentAtMillis", device.lastUserIntentAtMillis)
            put("manuallyDisconnected", device.manuallyDisconnected)
            put("bleDetected", device.bleDetected)
        })
    }
    return array.toString()
}

private fun decodeDevicesRaw(raw: String): List<RegisteredDeviceRaw> {
    val array = JSONArray(raw)
    return buildList {
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val mac = obj.optString("macAddress").takeIf { it.isNotBlank() } ?: continue
            add(RegisteredDeviceRaw(
                macAddress = mac,
                displayName = obj.optString("displayName", mac),
                profileId = obj.optString("profileId").takeIf { it.isNotBlank() && it != "null" },
                registeredAtMillis = obj.optLong("registeredAtMillis", 0L),
                lastConnectedAtMillis = obj.optLong("lastConnectedAtMillis", 0L),
                lastUserIntentAtMillis = obj.optLong(
                    "lastUserIntentAtMillis",
                    obj.optLong("lastConnectedAtMillis", 0L)
                ),
                manuallyDisconnected = obj.optBoolean("manuallyDisconnected", false),
                bleDetected = obj.optBoolean("bleDetected", false)
            ))
        }
    }
}

private fun decodeDevices(raw: String, defaultMac: String?): List<RegisteredDevice> {
    return decodeDevicesRaw(raw).map { device ->
        RegisteredDevice(
            macAddress = device.macAddress,
            displayName = device.displayName,
            profileId = device.profileId,
            registeredAtMillis = device.registeredAtMillis,
            lastConnectedAtMillis = device.lastConnectedAtMillis,
            lastUserIntentAtMillis = device.lastUserIntentAtMillis,
            isDefault = device.macAddress == defaultMac,
            manuallyDisconnected = device.manuallyDisconnected,
            bleDetected = device.bleDetected
        )
    }
}
