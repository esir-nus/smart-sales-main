# M1: Connectivity Completion

**Status:** IN PROGRESS  
**Started:** 2026-01-09  
**Spec:** [esp32-protocol.md](file:///home/cslh-frank/main_app/docs/specs/esp32-protocol.md)

---

## Current State

| Protocol | Status | Notes |
|----------|--------|-------|
| WiFi Status Query | ✅ Done | `wifi#address#ip#name` |
| WiFi Connect | ⚠️ Format Issue | Verify with hardware team |
| GIF Upload | ❌ Not Started | `POST :8088/upload` |
| WAV List/Download | ❌ Not Started | `GET :8088/list`, `/download` |
| WAV Delete | ❌ Not Started | `POST :8088/delete` |
| Time Sync | ❌ Not Started | `time#get` listener |

---

## Phases

### Phase 1: Verify WiFi Connect Protocol ⚠️

- [ ] Confirm format with hardware team
- [ ] Test on real BT311 device

### Phase 2: BadgeHttpClient Implementation

> Create HTTP client for badge communication

**File**: `feature/connectivity/src/.../BadgeHttpClient.kt`

```kotlin
interface BadgeHttpClient {
    suspend fun uploadJpg(baseUrl: String, file: File): Result<Unit>
    suspend fun listWavFiles(baseUrl: String): Result<List<String>>
    suspend fun downloadWav(baseUrl: String, filename: String, dest: File): Result<Unit>
    suspend fun deleteWav(baseUrl: String, filename: String): Result<Unit>
}
```

**Details**:
- [ ] `uploadJpg`: `POST /upload` multipart/form-data
- [ ] `listWavFiles`: `GET /list` → parse JSON array
- [ ] `downloadWav`: `GET /download?file=...` → stream to file
- [ ] `deleteWav`: `POST /delete` body `filename=...`

### Phase 3: GifFrameExtractor Implementation

> Split GIF into JPG frames for badge display

**File**: `feature/media/src/.../processing/GifFrameExtractor.kt`

**Requirements**:
- [ ] Decode GIF frames
- [ ] Resize each to 240×280
- [ ] Save as `1.jpg`, `2.jpg`, `3.jpg`...
- [ ] Return ordered list of files

### Phase 4: Wire GIF Transfer Flow

> Orchestrate BLE + HTTP for GIF upload

**Modify**: `MediaSyncCoordinator.kt`

```kotlin
suspend fun sendGifToBadge(gifUri: Uri, badgeIp: String) {
    // 1. BLE: jpg#send
    // 2. Wait for: jpg#receive
    // 3. Extract frames
    // 4. For each frame: POST /upload
    // 5. BLE: jpg#end
    // 6. Wait for: jpg#display
}
```

### Phase 5: Wire WAV Download Flow

> Orchestrate BLE + HTTP for WAV download

**Modify**: Audio file management

```kotlin
suspend fun downloadRecordingsFromBadge(badgeIp: String) {
    // 1. BLE: wav#get
    // 2. Wait for: wav#send
    // 3. GET /list
    // 4. For each file: GET /download
    // 5. BLE: wav#end
    // 6. Wait for: wav#ok
}
```

### Phase 6: Time Sync Listener

> Respond to badge time requests

- [ ] Listen for `time#get` on BLE characteristic
- [ ] Respond with `time#YYYYMMDDHHMMSS`

---

## Technical Constraints (from webserver.c)

| Constraint | Value |
|------------|-------|
| HTTP Port | 8088 |
| Max file size | 10MB |
| Upload format | `.jpg` only |
| Download format | `.wav` only |
| Filename chars | `[a-zA-Z0-9_.-~]` |
| Max filename | 128 chars |
| Max file listing | 50 files |

---

## Open Questions

1. **WiFi Connect format**: `wifi#connect#ssid#pwd` or `SD#`/`PD#`?
2. **jpg#end timing**: After each frame or after all frames?

---

## Reference

- Protocol Spec: [esp32-protocol.md](file:///home/cslh-frank/main_app/docs/specs/esp32-protocol.md)
- Backend Source: [webserver.c](file:///home/cslh-frank/main_app/reference-source/webserver.c)
- Connectivity Module: [README.md](file:///home/cslh-frank/main_app/feature/connectivity/README.md)
