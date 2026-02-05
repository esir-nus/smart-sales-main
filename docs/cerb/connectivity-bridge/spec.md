# Connectivity Bridge

> **Cerb-compliant spec** — Prism-safe wrapper for legacy BLE + HTTP connectivity.

---

## Overview

Connectivity Bridge provides a **thin, Prism-compatible interface** to legacy `feature:connectivity` module. Preserves ESP32 rate limiting and business logic while exposing clean interfaces for Prism domain code.

**Key Principle**: Prism domain code MUST NOT import from `feature:connectivity` directly. All connectivity access goes through this bridge.

---

## Related Cerb Specs

| Spec | Responsibility |
|------|----------------|
| [Badge Audio Pipeline](../badge-audio-pipeline/spec.md) | Orchestrates recording → transcription → scheduler |
| [ASR Service](../asr-service/spec.md) | Audio transcription via FunASR |

---

## ESP32 Protocol Reference

> **SOT**: [`esp32-protocol.md`](../../specs/esp32-protocol.md)

### BLE Commands (Preserved)

| Command | Request | Response | Notes |
|---------|---------|----------|-------|
| **WiFi Query** | `wifi#address#ip#name` | `wifi#address#<IP>#<SSID>` | Network status |
| **WiFi Connect** | `SD#<ssid>` → `PD#<password>` | (connects) | Two-step protocol |
| **WAV Get** | `wav#get` | `wav#send` | Initiates download |
| **WAV End** | `wav#end` | `wav#ok` | Completes download |
| **Time Sync** | Badge: `time#get` | App: `time#YYYYMMDDHHMMSS` | Audio file naming |
| **Record End** | Badge: `record#end` | (none) | New: Recording ready |

### HTTP Endpoints (Port 8088)

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/list` | GET | List WAV files on SD card |
| `/download?file=xxx` | GET | Download specific WAV file |
| `/delete` | POST | Delete WAV file from SD card |

---

## Rate Limiting Policy (PRESERVED)

> [!CAUTION]
> **ESP32 overload protection**. Do NOT remove or weaken these limits.

| Parameter | Value | Enforcement |
|-----------|-------|-------------|
| **Network query TTL** | 2s | `queryNetworkStatus()` |
| **Polling interval** | 10s | Badge state monitor |
| **Min poll gap** | 5s | Prevents burst |
| **Max consecutive failures** | 3 | Circuit breaker |
| **Inter-command gap** | 300ms | BLE command spacing |

**Offline detection**: `wifi#address#0.0.0.0#...` indicates badge has no WiFi.

---

## Domain Models

### BadgeConnectionState

```kotlin
sealed class BadgeConnectionState {
    object Disconnected : BadgeConnectionState()
    object Connecting : BadgeConnectionState()
    data class Connected(
        val badgeIp: String,
        val ssid: String
    ) : BadgeConnectionState()
    data class Error(val message: String) : BadgeConnectionState()
}
```

### RecordingNotification

```kotlin
sealed class RecordingNotification {
    /**
     * Badge finished recording, file is ready for download.
     * Triggered by `record#end` BLE command from firmware.
     */
    data class RecordingReady(
        val filename: String  // YYYYMMDDHHMMSS.wav
    ) : RecordingNotification()
}
```

### WavDownloadResult

```kotlin
sealed class WavDownloadResult {
    data class Success(
        val localFile: File,
        val originalFilename: String,
        val sizeBytes: Long
    ) : WavDownloadResult()
    
    data class Error(
        val code: ErrorCode,
        val message: String
    ) : WavDownloadResult()
    
    enum class ErrorCode {
        NOT_CONNECTED,
        BADGE_OFFLINE,
        FILE_NOT_FOUND,
        DOWNLOAD_FAILED,
        DISK_FULL
    }
}
```

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Interface + Fake | 🔲 | `ConnectivityBridge` interface, `FakeConnectivityBridge` |
| **2** | Real Implementation | 🔲 | `RealConnectivityBridge` wrapping legacy |
| **3** | Record End Handler | 🔲 | `record#end` BLE listener, notification flow |

---

## Wave 1 Ship Criteria

**Goal**: Prism-safe interface that compiles without legacy imports in domain layer.

- **Exit Criteria**:
  - [ ] `ConnectivityBridge` interface in `domain/connectivity/`
  - [ ] `FakeConnectivityBridge` returns mock data
  - [ ] Zero imports from `feature:connectivity` in Prism domain
  - [ ] Build passes: `./gradlew :app-prism:compileDebugKotlin`

- **Test Cases**:
  - [ ] Fake returns Connected state
  - [ ] Fake simulates download with 500ms delay
  - [ ] Fake emits RecordingReady notification

---

## Wave 2 Ship Criteria

**Goal**: Real connectivity via legacy wrapper.

- **Exit Criteria**:
  - [x] `RealConnectivityBridge` delegates to `DeviceConnectionManager`
  - [x] Rate limiting enforced at bridge level (via legacy `RateLimitedBleGateway`)
  - [ ] WAV download works end-to-end (L2 test pending)

- **Test Cases**:
  - [ ] L2: Connect to real badge, query status
  - [ ] L2: Download WAV file, verify contents
  - [ ] Rate limit: Rapid queries throttled correctly

---

## Wave 3 Ship Criteria

**Goal**: `record#end` triggers automatic download.

- **Exit Criteria**:
  - [ ] BLE listener for `record#end` command
  - [ ] Filename extracted from time sync
  - [ ] `RecordingNotification.RecordingReady` emitted

- **Test Cases**:
  - [ ] Badge sends `record#end` → notification received
  - [ ] Time sync provides correct filename format
  - [ ] Multiple recordings handled sequentially

---

## File Map

| Layer | Files |
|-------|-------|
| **Domain** | `ConnectivityBridge.kt`, `BadgeConnectionState.kt`, `RecordingNotification.kt`, `WavDownloadResult.kt` |
| **Data** | `RealConnectivityBridge.kt`, `FakeConnectivityBridge.kt` |
| **DI** | `ConnectivityBridgeModule.kt` |

---

## Legacy Wrapping Strategy

```kotlin
// app-prism/data/connectivity/RealConnectivityBridge.kt
class RealConnectivityBridge @Inject constructor(
    private val legacyManager: DeviceConnectionManager,  // from :feature:connectivity
    private val httpClient: BadgeHttpClient,             // from :feature:connectivity
    private val bleGateway: BleGateway                   // from :feature:connectivity
) : ConnectivityBridge {
    
    override val connectionState: StateFlow<BadgeConnectionState>
        get() = legacyManager.connectionState.map { /* transform to Prism type */ }
    
    override suspend fun downloadRecording(filename: String): WavDownloadResult {
        // Delegate to httpClient.downloadWav()
        // Transform result to Prism type
    }
    
    override fun recordingNotifications(): Flow<RecordingNotification> {
        // Listen for "record#end" BLE command
        // Map to RecordingNotification.RecordingReady
    }
}
```

> [!IMPORTANT]
> Only `data/` layer imports from `:feature:connectivity`. Domain layer NEVER imports legacy.

---

## Verification Commands

```bash
# Check no legacy imports in domain
grep -rn "feature.connectivity" app-prism/src/main/java/**/domain/ && echo "FAIL"

# Build check
./gradlew :app-prism:compileDebugKotlin
```
