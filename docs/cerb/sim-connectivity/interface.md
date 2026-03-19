# SIM Connectivity Interface

> **Blackbox contract** - For the standalone SIM shell and adjacent callers.

---

## Entry Surface

### Connectivity Entry

```kotlin
interface SimConnectivityEntry {
    fun open()
    fun close()
}
```

Meaning:

- SIM shell may open badge connection management from a shell-level entry/icon
- closing returns to normal SIM shell use

---

## Reused Connectivity Contracts

SIM should reuse the existing connectivity contracts as its behavioral backend:

```kotlin
interface ConnectivityBridge {
    val connectionState: StateFlow<BadgeConnectionState>
    suspend fun downloadRecording(filename: String): WavDownloadResult
    suspend fun listRecordings(): Result<List<String>>
    fun recordingNotifications(): Flow<RecordingNotification>
    suspend fun isReady(): Boolean
    suspend fun deleteRecording(filename: String): Boolean
}

interface ConnectivityService {
    suspend fun checkForUpdate(): UpdateResult
    suspend fun reconnect(): ReconnectResult
    suspend fun disconnect()
    suspend fun unpair()
    suspend fun updateWifiConfig(ssid: String, password: String): WifiConfigResult
}
```

Meaning:

- SIM shell owns the entry
- existing connectivity contracts own BLE/Wi-Fi behavior

---

## UI Flow Guarantees

- connectivity is reachable from SIM shell as a support action
- connectivity state continues to come from the existing connectivity contracts
- connectivity does not become a hidden dependency of SIM scheduler or SIM audio discussion flows

---

## You Should NOT

- ❌ redefine connectivity protocol behavior inside SIM docs or code
- ❌ couple connectivity entry to smart-agent shell/runtime assumptions
- ❌ treat connectivity as a third smart feature lane

---

## When to Read Full Spec

Read `spec.md` if:

- you are wiring the SIM shell connectivity entry
- you are deciding how much of the existing connectivity UI to reuse
- you are validating that connectivity stays decoupled from the two main SIM lanes
