# ESP32 Communication Protocol (Definitive Spec)

> **Purpose**: BLE + HTTP protocol between Android app and ESP32 badge  
> **Source of Truth**: `reference-source/webserver.c`  
> **Last Updated**: 2026-01-09

---

## Protocol Overview

Communication happens via:
1. **BLE** — Command/control messages (text-based, `#`-delimited)
2. **HTTP** — File transfers over shared WiFi network (port **8088**)

---

## Implementation Status

| # | Protocol | Status | App-Side Notes |
|---|----------|--------|----------------|
| 1 | WiFi Status Query | ✅ Implemented | `GattBleGateway.queryNetwork()` |
| 2 | WiFi Connect | ⚠️ Format Mismatch | Need to verify with hardware |
| 3 | GIF/JPG Upload | ✅ Implemented | `GifTransferCoordinator` + `BadgeHttpClient.uploadJpg()` |
| 4 | WAV Download | ✅ Implemented | `WavDownloadCoordinator` + `BadgeHttpClient.downloadWav()` |
| 5 | WAV Delete | ✅ Implemented | `BadgeHttpClient.deleteWav()` |
| 6 | Time Sync | ✅ Implemented | `GattBleGateway.listenForTimeSync()` |

---

## HTTP API Contract (Definitive)

**Base URL**: `http://<badge-ip>:8088`

### POST `/upload` — Upload JPG Frame

```
Content-Type: multipart/form-data
Body: file=<binary> (field name must be "file")
```

**Constraints** (from `webserver.c`):
- ✅ Extension: `.jpg` ONLY (L237-240)
- ✅ Max size: 10MB (L203)
- ✅ Filename: alphanumeric, `_`, `-`, `.`, `~` only (L36-44)
- ✅ Max filename: 128 chars (L24)
- ✅ Saves to: `/sdcard/<filename>` (L255)

**Response**:
- Success: `200 OK` with HTML body "文件上传成功！"
- Error: `400 Bad Request` with error message

> [!NOTE]
> **Response is HTML, not JSON.** App should check HTTP status code only.

---

### GET `/list` — List WAV Files

```
GET /list
```

**Response**:
```json
["recording1.wav", "rec_20260109.wav", ...]
```

**Constraints**:
- Returns `.wav` files from `/sdcard/` (L407, L433)
- Max 50 files (L28)
- Returns `[]` if SD card not mounted (L401-404)

---

### GET `/download` — Download WAV File

```
GET /download?file=recording1.wav
```

**Response**:
- Success: Binary WAV data with `Content-Disposition: attachment`
- 404: File not found
- 403: Invalid filename (non-.wav or unsafe chars)

**Also supports**: `HEAD /download?file=...` for file existence check

---

### POST `/delete` — Delete WAV File

```
POST /delete
Content-Type: application/x-www-form-urlencoded
Body: filename=recording1.wav
```

**Response**:
```json
{"status":"success"}
```

> [!WARNING]  
> Delete uses **body parameter** `filename=...`, NOT query string like download.

---

## BLE Protocol

### 1. WiFi Status Query ✅

```
App sends:    wifi#address#ip#name
Badge returns:
  1. IP#192.168.0.101
  2. SD#MstRobot
```

### 2. WiFi Connect ⚠️

**Current implementation**: `wifi#connect#<ssid>#<password>`  
**Colleague spec**: `SD#<name>` + `PD#<password>`

> **Action needed**: Verify with hardware team.

### 3. GIF Transfer Flow ❌

```
1. App sends:     jpg#send
2. Badge returns: jpg#receive
3. App uploads:   POST /upload (for each frame: 1.jpg, 2.jpg, ...)
4. App sends:     jpg#end
5. Badge returns: jpg#display
```

**Frame requirements**:
- Resolution: 240×280
- Format: JPEG
- Naming: `1.jpg`, `2.jpg`, `3.jpg`...
- Location: Saved to `/sdcard/`

### 4. WAV Download Flow ❌

```
1. App sends:     wav#get
2. Badge returns: wav#send
3. App calls:     GET /list → GET /download?file=...
4. App sends:     wav#end
5. Badge returns: wav#ok
```

### 5. Time Sync ❌

```
Badge sends:  time#get
App returns:  time#20260109165832   (YYYYMMDDHHMMSS)
```

---

## File System Structure

```
/sdcard/
├── 1.jpg              # GIF frame 1
├── 2.jpg              # GIF frame 2
├── ...
├── recording1.wav     # Audio recording
└── rec_20260109.wav   # Audio recording
```

> [!NOTE]  
> JPGs and WAVs are in the same directory. No subdirectory separation.

---

## Error Handling

| HTTP Status | Meaning | User Message |
|-------------|---------|--------------|
| 200 | Success | - |
| 400 | Bad Request | "Invalid file or format" |
| 403 | Forbidden | "File type not allowed" |
| 404 | Not Found | "File not found" |
| 500 | Server Error | "Badge error, try again" |

---

## Operational Policy

### Timeouts

| Operation | Timeout | Notes |
|-----------|---------|-------|
| BLE command send | 5s | Wait for ACK |
| BLE response wait | 10s | Wait for `jpg#receive`, `wav#send`, etc. |
| HTTP upload per frame | 30s | Single JPG upload |
| HTTP download per file | 60s | Large WAV files |
| Total GIF transfer | 5min | All frames combined |

### Retry Policy

| Failure | Retries | Backoff |
|---------|---------|---------|
| BLE command timeout | 2 | 1s, 2s |
| HTTP upload fail (5xx) | 3 | 1s, 2s, 4s |
| HTTP upload fail (4xx) | 0 | No retry (client error) |
| HTTP download fail | 3 | 1s, 2s, 4s |

### Pre-flight Checks

Before any transfer, verify:

```kotlin
fun canTransfer(): Result<String> {
    // 1. BLE connected?
    if (!bleConnected) return Error("请先连接徽章")
    
    // 2. Badge IP known?
    if (badgeIp == null) return Error("请先查询网络状态")
    
    // 3. Same network? (optional but recommended)
    // Compare badge IP subnet with phone IP
    
    // 4. Badge reachable?
    // HEAD /list with 3s timeout
    
    return Success(badgeIp)
}
```

### jpg#end Timing (Resolved)

> [!NOTE]
> `jpg#end` is sent **after ALL frames are uploaded**, not after each frame.  
> The badge waits for `jpg#end` before starting display animation.

---

## Implementation Checklist

### BadgeHttpClient

```kotlin
interface BadgeHttpClient {
    suspend fun uploadJpg(baseUrl: String, file: File): Result<Unit>
    suspend fun listWavFiles(baseUrl: String): Result<List<String>>
    suspend fun downloadWav(baseUrl: String, filename: String, dest: File): Result<Unit>
    suspend fun deleteWav(baseUrl: String, filename: String): Result<Unit>
}
```

### GifFrameExtractor

```kotlin
interface GifFrameExtractor {
    suspend fun extractFrames(
        gifUri: Uri,
        outputDir: File,
        targetWidth: Int = 240,
        targetHeight: Int = 280
    ): Result<List<File>>  // Returns [1.jpg, 2.jpg, ...]
}
```

---

## Source References

| Item | File | Line |
|------|------|------|
| Port | `webserver.c` | L550 |
| Upload handler | `webserver.c` | L179-329 |
| List handler | `webserver.c` | L398-464 |
| Download handler | `webserver.c` | L353-397 |
| Delete handler | `webserver.c` | L465-530 |
| Filename validation | `webserver.c` | L36-45 |
