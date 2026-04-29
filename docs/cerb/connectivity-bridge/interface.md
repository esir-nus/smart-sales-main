# Connectivity Bridge Interface

> **Blackbox contract** — For consumers (Scheduler, Badge Audio Pipeline). Don't read implementation.
> **Status**: Supporting reference beneath BAKE
> **Last Updated**: 2026-04-28
> **BAKE Implementation Contract**: [`docs/bake-contracts/connectivity-badge-session.md`](../../bake-contracts/connectivity-badge-session.md)
> **Behavioral Source Above This Interface**: [`docs/core-flow/badge-connectivity-lifecycle.md`](../../core-flow/badge-connectivity-lifecycle.md)

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
     * Stream of audio-only recording notifications from badge.
     *
     * Current production path: BLE `rec#YYYYMMDD_HHMMSS` notification on the
     * persistent badge GATT session. Consumers receive the full downloadable
     * filename as `RecordingNotification.AudioRecordingReady`.
     *
     * @return Flow (hot, buffered, no replay)
     */
    fun audioRecordingNotifications(): Flow<RecordingNotification.AudioRecordingReady>

    /**
     * Stream of battery notifications from badge.
     *
     * Current production path: BLE `Bat#<0..100>` notification on the
     * persistent badge GATT session. Consumers receive the integer percent
     * pushed by firmware without polling.
     *
     * @return Flow (hot, buffered, no replay)
     * @see esp32-protocol.md §9
     */
    fun batteryNotifications(): Flow<Int>

    /**
     * Stream of firmware-version replies from badge.
     *
     * Current production path: app sends BLE `Ver#get`; badge replies
     * `Ver#<project>.<major>.<minor>.<feature>` on the persistent
     * notification characteristic.
     *
     * @return Flow (hot, buffered, no replay)
     * @see esp32-protocol.md §10
     */
    fun firmwareVersionNotifications(): Flow<String>

    /**
     * Stream of SD-card-space replies from badge.
     *
     * Current production path: app sends BLE `SD#space`; badge replies
     * `SD#space#<size>` on the persistent notification characteristic.
     *
     * @return Flow (hot, buffered, no replay)
     * @see esp32-protocol.md §12
     */
    fun sdCardSpaceNotifications(): Flow<String>
    
    /**
     * Check if badge is connected and reachable.
     * Performs pre-flight check (BLE + HTTP).
     * Reuses the current runtime endpoint unless that snapshot has been invalidated.
     * If HTTP reachability fails once, invalidates the endpoint and retries once
     * after a fresh BLE network-status resolution. If usable IP/SSID still exists,
     * may run one bounded latest-saved-credential replay sequence before returning
     * not-ready.
     */
    suspend fun isReady(): Boolean
    
    /**
     * Delete a WAV file from badge after successful processing.
     * Called after transcription completes.
     * Reuses the current runtime endpoint unless that snapshot has been invalidated.
     */
    suspend fun deleteRecording(filename: String): Boolean

    /**
     * Request the badge firmware version over BLE.
     *
     * Sends `Ver#get` as a best-effort query on the active session.
     */
    suspend fun requestFirmwareVersion(): Boolean

    /**
     * Request the badge SD-card free-space string over BLE.
     *
     * Sends `SD#space` as a best-effort query on the active session.
     */
    suspend fun requestSdCardSpace(): Boolean

    /**
     * Notify the badge that downstream processing finished.
     *
     * Sends fire-and-forget `Command#end` on the active BLE session.
     */
    suspend fun notifyCommandEnd()

    /**
     * Stream of Wi-Fi repair milestones for the active manual repair window.
     *
     * UI repair state must consume these explicit events instead of inferring
     * repair progress from `managerStatus`.
     *
     * @return Flow (hot, no replay)
     */
    fun wifiRepairEvents(): Flow<WifiRepairEvent>
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
    object BleDetected : BadgeManagerStatus()
    data class Error(val message: String) : BadgeManagerStatus()
}
```

### RecordingNotification

```kotlin
sealed class RecordingNotification {
    data class RecordingReady(val filename: String) : RecordingNotification()
    data class AudioRecordingReady(val filename: String) : RecordingNotification()
}
```

`RecordingReady.filename` uses the full downloadable badge filename:
`log_YYYYMMDD_HHMMSS.wav`.

`AudioRecordingReady.filename` uses the full downloadable badge filename for
audio-only `rec#` notifications.

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

### ConnectivityPrompt

```kotlin
interface ConnectivityPrompt {
    suspend fun promptWifiMismatch(suggestedSsid: String?)
    suspend fun promptSuspectedIsolation(
        badgeIp: String,
        triggerContext: IsolationTriggerContext = IsolationTriggerContext.POST_PAIRING,
        suggestedSsid: String? = null
    )
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
| `audioRecordingNotifications` | Hot flow, buffered, no replay; forwards audio-only `rec#...` recording-ready notifications |
| `batteryNotifications` | Hot flow, buffered, no replay; forwards unsolicited BLE `Bat#<0..100>` pushes as integer percent |
| `firmwareVersionNotifications` | Hot flow, buffered, no replay; forwards `Ver#...` replies from the badge as display-ready strings |
| `sdCardSpaceNotifications` | Hot flow, buffered, no replay; forwards `SD#space#<size>` replies from the badge as firmware-formatted display strings |
| `isReady()` | Pre-flight check with 3s timeout; may refresh endpoint only when the active snapshot is missing or invalidated, and may trigger one bounded saved-credential replay sequence after solid-IP HTTP failure |
| `deleteRecording` | Idempotent, returns true if file removed or didn't exist; reuses the active runtime endpoint when valid |
| `requestFirmwareVersion()` | Best-effort BLE query; sends `Ver#get` on the active session and returns whether the request was queued successfully |
| `wifiRepairEvents` | Hot flow, no replay; emits manual Wi-Fi repair milestones during the active repair window |
| `requestSdCardSpace()` | Best-effort BLE query; sends `SD#space` on the active session and returns whether the request was queued successfully |
| `notifyCommandEnd()` | Best-effort BLE completion signal; sends `Command#end` on the active session when a `log#` or `rec#` pipeline reaches a terminal state |
| `updateWifiConfig` | Rejects blank/whitespace-only SSID or password before any BLE provision/write attempt |

`BadgeConnectionState.Connected` means shared transport readiness:
persistent GATT notification listening is active and the badge has usable network status.

It does not mean HTTP `:8088` is ready. Normal HTTP file work still performs its own `isReady()` preflight / endpoint validation without delaying shell state reflection.

`managerStatus` may refine a plain disconnected manager view into:

- `BleDetected`: a registered badge was seen by scanning, but GATT is not established
- `BlePairedNetworkUnknown`: BLE is still held, but Wi‑Fi readiness is not yet confirmed
- `BlePairedNetworkOffline`: BLE is still held, but the badge reported no usable IP / network
- `Ready`: manager transport readiness; must not be used as proof that `/list`, `/download`, or `/delete` is ready

Internal `ConnectionState.WifiProvisionedHttpDelayed` means badge transport is confirmed, but HTTP `:8088` is still warming or unreachable inside the bounded grace window. It must not be semantically treated as full feature readiness. If a future UI needs to expose that state directly, add a public manager state in code and sync this interface in the same sprint.

Normal badge HTTP work (`/list`, `/download`, `/delete`) should reuse the current runtime endpoint snapshot.
Repeated BLE `wifi#address#ip#name` querying is not part of the normal sync path.

If `/list` fails while the active badge has a usable runtime endpoint, the bridge may attempt one latest-saved-credential replay sequence before retrying `/list` once. This recovery branch must not use the phone's current SSID, must not unregister the badge, and must remain bounded to ESP32-safe foreground attempts.

For `/download`, `totalBytes` is derived from HTTP `Content-Length`.
When the badge omits that header or uses chunked/unknown-length transfer, the bridge must still invoke
`onProgress(bytesRead, totalBytes)` with `totalBytes <= 0` so downstream consumers can render
indeterminate download progress instead of suppressing progress updates entirely.

`ConnectivityService.reconnect()` may return `WifiMismatch` in these deterministic reconnect cases:

- the badge reports offline and no latest saved user-confirmed credential is available
- the badge reports an SSID different from the latest saved user-confirmed credential
- credential replay completes, but the badge confirms it is on a different Wi‑Fi than the replayed/submitted credential
- credential replay fails to produce usable badge Wi‑Fi transport

Phone current SSID must not be used as a core reconnect or repair decision input. The `ReconnectResult.WifiMismatch.currentPhoneSsid` field is retained for compatibility, but new recovery behavior should treat it as an optional UI hint only. Prefer prefill from the latest saved user-confirmed credential when available.

`ConnectivityService.updateWifiConfig()` must treat `trim().isEmpty()` on either field as an immediate local error.
That rejection is a guard rail only: it must not call BLE provisioning, manual repair confirmation, or remembered-network persistence.

`DeviceRegistryManager.setDefault()` is passive/cosmetic registry metadata. The BLE detection monitor may mark each live registered candidate's own `bleDetected` proximity flag, and it must clear missing candidates after a short grace window, but automatic reconnect may target only the latest connected/current active badge after non-manual loss. A non-active/default badge must not become active, reseed `SessionStore`, or reconnect unless the user explicitly switches to it. Manual disconnect of the active badge suppresses both auto-reconnect to that badge and auto-connect to any nearby non-active badge until explicit user action.

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
