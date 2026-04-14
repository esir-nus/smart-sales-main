# Connectivity Bridge Interface

> **Blackbox contract** — For consumers (Scheduler, Badge Audio Pipeline). Don't read implementation.
> **Status**: Active supporting interface
> **Last Updated**: 2026-04-14

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
     * Connectivity-manager-only richer state.
     * Keeps BLE-held / Wi‑Fi-offline diagnostics out of shared shell routing.
     */
    val managerStatus: StateFlow<BadgeManagerStatus>
    
    /**
     * Download a WAV file from the badge SD card.
     * Rate-limited internally.
     *
     * @param filename File name on badge (e.g., "log_20260205_143000.wav")
     * @param onProgress Optional callback invoked with (bytesRead, totalBytes) during download
     * @return Downloaded file or error
     */
    suspend fun downloadRecording(
        filename: String,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
    ): WavDownloadResult
    
    /**
     * List all WAV files currently stored on the badge.
     * 
     * Reuses the current runtime endpoint when one has already been confirmed;
     * normal HTTP sync must not trigger repeated BLE Wi‑Fi status queries.
     *
     * @return List of filenames (e.g., ["rec1.wav", "rec2.wav"]) or error
     */
    suspend fun listRecordings(): Result<List<String>>

    /**
     * Stream of recording notifications from badge.
     *
     * Current production path: BLE `log#YYYYMMDD_HHMMSS` notification on the
     * persistent badge GATT session. Consumers receive the full downloadable
     * filename `log_YYYYMMDD_HHMMSS.wav`.
     *
     * The bridge only emits when transport is truly ready: persistent GATT
     * notification listening is alive and badge network reachability has been
     * established.
     *
     * @return Flow (hot, buffered, no replay)
     * @see esp32-protocol.md §6
     */
    fun recordingNotifications(): Flow<RecordingNotification>
    
    /**
     * Check if badge is connected and reachable.
     * Performs pre-flight check (BLE + HTTP).
     * Reuses the current runtime endpoint unless that snapshot has been invalidated.
     */
    suspend fun isReady(): Boolean
    
    /**
     * Delete a WAV file from badge after successful processing.
     * Called after transcription completes.
     * Reuses the current runtime endpoint unless that snapshot has been invalidated.
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

### BadgeManagerStatus

```kotlin
sealed class BadgeManagerStatus {
    object Unknown : BadgeManagerStatus()
    object NeedsSetup : BadgeManagerStatus()
    object Disconnected : BadgeManagerStatus()
    object Connecting : BadgeManagerStatus()
    object BlePairedNetworkUnknown : BadgeManagerStatus()
    object BlePairedNetworkOffline : BadgeManagerStatus()
    data class Ready(val badgeIp: String? = null, val ssid: String? = null) : BadgeManagerStatus()
    data class Error(val message: String) : BadgeManagerStatus()
}
```

### RecordingNotification

```kotlin
sealed class RecordingNotification {
    data class RecordingReady(val filename: String) : RecordingNotification()
}
```

`RecordingReady.filename` uses the full downloadable badge filename:
`log_YYYYMMDD_HHMMSS.wav`.

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
    // Rejects blank/whitespace-only credentials locally; no transport write on that path.
    suspend fun updateWifiConfig(ssid: String, password: String): WifiConfigResult
}

sealed class UpdateResult {
    data class Found(val version: String, val changelog: String = "") : UpdateResult()
    object None : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

sealed class ReconnectResult {
    object Connected : ReconnectResult()
    data class WifiMismatch(val currentPhoneSsid: String? = null) : ReconnectResult()
    object DeviceNotFound : ReconnectResult()
    data class Error(val message: String) : ReconnectResult()
}

sealed class WifiConfigResult {
    object Success : WifiConfigResult()
    data class Error(val message: String) : WifiConfigResult()
}
```

---


## Guarantees

| Operation | Guarantee |
|-----------|-----------|
| `connectionState` | Always emits current state immediately on collect |
| `managerStatus` | Manager-only richer BLE/Wi‑Fi diagnostic state; no shell-routing authority |
| `downloadRecording` | Rate-limited, max 1 concurrent download; `onProgress` callback (if provided) is invoked on the IO dispatcher with (bytesRead, totalBytes) — consumers must throttle UI updates themselves |
| `listRecordings` | Reuses the active runtime endpoint; no repeated BLE Wi‑Fi query in the normal happy path |
| `recordingNotifications` | Hot flow, buffered (1), no replay |
| `isReady()` | Pre-flight check with 3s timeout; may refresh endpoint only when the active snapshot is missing or invalidated |
| `deleteRecording` | Idempotent, returns true if file removed or didn't exist; reuses the active runtime endpoint when valid |
| `updateWifiConfig` | Rejects blank/whitespace-only SSID or password before any BLE provision/write attempt |

`BadgeConnectionState.Connected` means the badge session is actually usable:
persistent GATT notification listening is active and the badge has valid network reachability.

Under the current reconnect contract, the bridge may surface this connected state as soon as BLE is restored and the badge reports a usable IP / transport-ready network result. Normal HTTP file work still performs its own preflight / endpoint validation without delaying shell state reflection.

`managerStatus` may refine a plain disconnected manager view into:

- `BlePairedNetworkUnknown`: BLE is still held, but Wi‑Fi readiness is not yet confirmed
- `BlePairedNetworkOffline`: BLE is still held, but the badge reported no usable IP / network

Normal badge HTTP work (`/list`, `/download`, `/delete`) should reuse the current runtime endpoint snapshot.
Repeated BLE `wifi#address#ip#name` querying is not part of the normal sync path.

`ConnectivityService.reconnect()` may return `WifiMismatch` in either of these deterministic reconnect cases:

- the phone's current Wi‑Fi SSID has no exact remembered credential to replay
- the phone is on Wi‑Fi but the app cannot read the SSID, so exact-match replay cannot be proven safely
- credential replay completes, but the badge confirms it is on a different Wi‑Fi than the phone

When reconnect can read the phone's current Wi‑Fi SSID, `ReconnectResult.WifiMismatch.currentPhoneSsid`
must carry that suggestion so the repair form can prefill it while keeping the SSID editable.

`ConnectivityService.updateWifiConfig()` must treat `trim().isEmpty()` on either field as an immediate local error.
That rejection is a guard rail only: it must not call BLE provisioning, manual repair confirmation, or remembered-network persistence.

This richer state is for connectivity manager presentation only. Shared shell/history routing must continue to use `connectionState`.

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
