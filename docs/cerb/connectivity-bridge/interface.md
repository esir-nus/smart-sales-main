# Connectivity Bridge Interface

> **Blackbox contract** — For consumers (Scheduler, Badge Audio Pipeline). Don't read implementation.

---

## You Can Call

### ConnectivityBridge

```kotlin
interface ConnectivityBridge {
    /**
     * Current badge connection state. 
     * Emits on connection changes.
     */
    val connectionState: StateFlow<BadgeConnectionState>
    
    /**
     * Download a WAV file from the badge SD card.
     * Rate-limited internally.
     *
     * @param filename File name on badge (e.g., "20260205143000.wav")
     * @return Downloaded file or error
     */
    suspend fun downloadRecording(filename: String): WavDownloadResult
    
    /**
     * Stream of recording notifications from badge.
     * 
     * **Current**: HTTP polling on `/list` detects new WAV files.  
     * **Future**: BLE `record#end` command triggers immediate notification.
     * 
     * @return Flow (hot, starts polling on collection)
     * @see esp32-protocol.md §6
     */
    fun recordingNotifications(): Flow<RecordingNotification>
    
    /**
     * Check if badge is connected and reachable.
     * Performs pre-flight check (BLE + HTTP).
     */
    suspend fun isReady(): Boolean
    
    /**
     * Delete a WAV file from badge after successful processing.
     * Called after transcription completes.
     */
    suspend fun deleteRecording(filename: String): Boolean
}
```

---

## Input/Output Types

### BadgeConnectionState

```kotlin
sealed class BadgeConnectionState {
    object Disconnected : BadgeConnectionState()
    object NeedsSetup : BadgeConnectionState()
    object Connecting : BadgeConnectionState()
    data class Connected(val badgeIp: String, val ssid: String) : BadgeConnectionState()
    data class Error(val message: String) : BadgeConnectionState()
}
```

### RecordingNotification

```kotlin
sealed class RecordingNotification {
    data class RecordingReady(val filename: String) : RecordingNotification()
}
```

### WavDownloadResult

```kotlin
sealed class WavDownloadResult {
    data class Success(val localFile: File, val originalFilename: String, val sizeBytes: Long) : WavDownloadResult()
    data class Error(val code: ErrorCode, val message: String) : WavDownloadResult()
    
    enum class ErrorCode {
        NOT_CONNECTED, BADGE_OFFLINE, FILE_NOT_FOUND, DOWNLOAD_FAILED, DISK_FULL
    }
}
```

### ConnectivityService

```kotlin
interface ConnectivityService {
    suspend fun checkForUpdate(): UpdateResult
    suspend fun reconnect(): ReconnectResult
    suspend fun disconnect()
    suspend fun unpair()
    suspend fun updateWifiConfig(ssid: String, password: String): WifiConfigResult
}

sealed class UpdateResult {
    object None : UpdateResult()
    data class Available(val version: String) : UpdateResult()
}

sealed class ReconnectResult {
    object Success : ReconnectResult()
    data class Failed(val reason: String) : ReconnectResult()
}

sealed class WifiConfigResult {
    object Success : WifiConfigResult()
    data class Failed(val reason: String) : WifiConfigResult()
}
```

---


## Guarantees

| Operation | Guarantee |
|-----------|-----------|
| `connectionState` | Always emits current state immediately on collect |
| `downloadRecording` | Rate-limited, max 1 concurrent download |
| `recordingNotifications` | Hot flow, buffered (1), no replay |
| `isReady()` | Pre-flight check with 3s timeout |
| `deleteRecording` | Idempotent, returns true if file removed or didn't exist |

---

## You Should NOT

- ❌ Import from `feature:connectivity` directly
- ❌ Access `DeviceConnectionManager` or `BleGateway` 
- ❌ Make HTTP calls to badge directly
- ❌ Implement rate limiting in consumer code (bridge handles it)

---

## When to Read Full Spec

Read `spec.md` only if:
- You are implementing `RealConnectivityBridge`
- You need to understand ESP32 protocol details
- You are modifying rate limiting policy

Otherwise, **trust this interface**.
