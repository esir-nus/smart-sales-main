# ESP32 Communication Protocol (Definitive Spec)

> **Purpose**: BLE + HTTP protocol between Android app and ESP32 badge  
> **Source of Truth**: `reference-source/webserver-test.c` + `reference-source/bluetooch.py`  
> **Last Updated**: 2026-01-12

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
| 2 | WiFi Connect | ✅ Implemented | `wifi#connect#ssid#password` format |
| 3 | GIF/JPG Upload | ⚠️ DEPRECATED | Hardware no longer supports image transfer (audio only) |
| 4 | WAV Download | ✅ Implemented | `WavDownloadCoordinator` + `BadgeHttpClient.downloadWav()` |
| 5 | WAV Delete | ✅ Implemented | `BadgeHttpClient.deleteWav()` |
| 6 | Time Sync | ✅ Implemented | `GattBleGateway.listenForTimeSync()` |
| 7 | Recording End | 🔲 Planned | `ConnectivityBridge.recordingNotifications()` (Prism) |

---

## Rate Limiting Policy

> **Purpose**: Prevent ESP32 freeze from BLE command overload

| Parameter | Value | Enforced By |
|-----------|-------|-------------|
| **Network query TTL** | 2s | `DeviceConnectionManager.queryNetworkStatus()` |
| **Polling interval** | 10s | `BadgeStateMonitor.POLL_INTERVAL_MS` |
| **Min poll gap** | 5s | `BadgeStateMonitor.MIN_POLL_GAP_MS` |
| **Max consecutive failures** | 3 | `BadgeStateMonitor.MAX_CONSECUTIVE_FAILURES` |
| **Inter-command gap** | 300ms | `GattBleGateway` (WiFi provision steps) |

**Offline detection**: Response of `wifi#address#0.0.0.0#...` indicates badge has no WiFi.

## BLE Protocol

### 1. WiFi Status Query

```
App sends:    wifi#address#ip#name         (literal fixed string)
Badge sends:  IP#<IP_ADDRESS>              (BLE notification 1)
Badge sends:  SD#<SSID>                    (BLE notification 2)
```

**Notes**:
- Badge sends TWO separate BLE notifications (fragmented response)
- Order may vary; app must collect both fragments
- App implementation collects up to 3 fragments with 2s timeout per fragment

**Example:**
```
App:   wifi#address#ip#name
Badge: IP#192.168.0.101      (arrives first or second)
Badge: SD#MstRobot           (arrives first or second)
```

> **SOT**: `bluetooch.py` lines 320-324 (spec said single response, but actual firmware sends fragmented)

### 2. WiFi Connect (Two-Step Protocol)

```
Step 1: App sends:  SD#<ssid>
Step 2: App sends:  PD#<password>
Badge:              (connects to WiFi, then responds to query)
```

**Example:**
```
App: SD#MstRobot
App: PD#Cai123456
```

> **Note**: `bluetooch.py` is legacy. Two-step protocol confirmed by hardware team.

### 3. ~~GIF Transfer Flow~~ (DEPRECATED)

> [!WARNING]
> **Hardware no longer supports GIF/JPG transfer.** Badge now only supports:
> - Long audio recording (badge → app)
> - Short audio commands (app → badge)
>
> Image upload from gallery/camera goes through a different app pipeline (not hardware).

### 4. WAV Download Flow

```
1. App sends:     wav#get
2. Badge returns: wav#send
3. App calls:     GET /list → GET /download?file=...
4. App sends:     wav#end
5. Badge returns: wav#ok
```

### 5. Time Sync

```
Badge sends:  tim#get
App returns:  tim#20260112175600   (YYYYMMDDHHMMSS)
```

### 6. Recording End Notification

> **Added**: 2026-02-05 (firmware update from hardware team)
> **Updated**: 2026-02-08 — corrected command from `record#end` to `log#YYYYMMDD_HHMMSS`

When user finishes recording on badge, ESP32 notifies app with the filename.

```
Badge sends:  log#20260208_201345    (YYYYMMDD_HHMMSS, ESP32 local time)
App:          (downloads /download?file=log_20260208_201345.wav)
```

**Workflow**:
1. User presses record button on badge
2. Badge sends `tim#get` → App responds with timestamp following pattern `log#YYYYMMDDHHMMSS` (used for calibrating the ESP32 local time)
3. User records audio on the badge → saved as `YYYYMMDD_HHMMSS.wav` (ESP32 local time; the timestamp sent by app is for clock calibration only, not prescriptive of the filename)
4. User stops recording
5. Badge sends `log#YYYYMMDD_HHMMSS` (ESP32 local time)
6. App downloads WAV via HTTP `/download?file=log_YYYYMMDD_HHMMSS.wav` (app learns filename from `log#` command)

---

## HTTP API Contract (Definitive)

**Base URL**: `http://<badge-ip>:8088`

> **SOT**: `webserver-test.c` line 492: `config.server_port = 8088`

### GET `/` — Health Check

**Response**:
```json
{"status":"ok","version":"1.1.0"}
```

### ~~POST `/upload`~~ — Upload JPG Frame (DEPRECATED)

> [!WARNING]
> **DEPRECATED**: Hardware no longer supports image transfer. This endpoint remains documented for historical reference only.

```
POST /upload
Content-Type: multipart/form-data
Field: file (JPG binary)
```

**Response**:
- 200: HTML page "文件上传成功！"
- 400: Invalid request
- 403: File type not allowed

### GET `/list` — List WAV Files

```
GET /list
```

**Response**:
```json
["recording1.wav", "rec_20260109.wav", ...]
```

**Constraints**:
- Returns `.wav` files from `/sdcard/` (line 375)
- Max 50 files (line 28)
- Returns `[]` if SD card not mounted (line 343-346)

### GET `/download` — Download WAV File

```
GET /download?file=recording1.wav
```

**Response**:
- Success: Binary WAV data with `Content-Disposition: attachment`
- 404: File not found
- 403: Invalid filename (non-.wav or unsafe chars)

**Also supports**: `HEAD /download?file=...` for file existence check

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

---

## File System Structure

```
/sdcard/
├── recording1.wav     # Audio recording
├── rec_20260109.wav   # Audio recording
└── YYYYMMDD_HHMMSS.wav  # Timestamped recording
```

---

## Operational Policy

### Timeouts

| Operation | Timeout | Notes |
|-----------|---------|-------|
| BLE command send | 5s | Wait for ACK |
| BLE response wait | 10s | Wait for `wav#send`, etc. |
| HTTP download per file | 60s | Large WAV files |

### Retry Policy

| Failure | Retries | Backoff |
|---------|---------|---------|
| BLE command timeout | 2 | 1s, 2s |
| HTTP download fail | 3 | 1s, 2s, 4s |

### Pre-flight Checks

Before any transfer, verify:

```kotlin
fun canTransfer(): Result<String> {
    // 1. BLE connected?
    if (!bleConnected) return Error("请先连接徽章")
    
    // 2. Badge IP known?
    if (badgeIp == null) return Error("请先查询网络状态")
    
    // 3. Badge reachable?
    // HEAD /list with 3s timeout
    
    return Success(badgeIp)
}
```

---

## Source References

| Item | File | Line |
|------|------|------|
| Port | `webserver-test.c` | L492 |
| Upload handler | `webserver-test.c` | L121-271 |
| List handler | `webserver-test.c` | L340-406 |
| Download handler | `webserver-test.c` | L295-339 |
| Delete handler | `webserver-test.c` | L407-472 |
| Filename validation | `webserver-test.c` | L37-45 |
| BLE WiFi connect | `bluetooch.py` | L307-319 |
| BLE WiFi query | `bluetooch.py` | L320-324 |
