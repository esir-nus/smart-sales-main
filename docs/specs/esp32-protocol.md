# ESP32 Communication Protocol (Definitive Spec)

> **Purpose**: BLE + HTTP protocol between Android app and ESP32 badge  
> **Source of Truth**: `reference-source/webserver-test.c` + `reference-source/bluetooch.py`  
> **Last Updated**: 2026-04-24 (second drop — Ver# query/reply, Command#end, SD#space, §§6-7 semantic reconciliation)

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
| 7 | Recording End (scheduler) | ✅ Implemented | BLE `log#...` -> `ConnectivityBridge.recordingNotifications()` -> `BadgeAudioPipeline` |
| 8 | Audio Recording Ready (drawer) | ✅ Implemented | BLE `rec#...` -> `ConnectivityBridge.audioRecordingNotifications()` -> `SimBadgeAudioAutoDownloader` |
| 9 | Voice Volume Control | ✅ Implemented | `DeviceConnectionManager.setBadgeVolume()` -> `badgeGateway.sendBadgeSignal(session, "volume#$clamped")` |
| 10 | Battery Level Push | ✅ Implemented | BLE `Bat#...` -> `ConnectivityBridge.batteryNotifications()` -> `ConnectivityViewModel.batteryLevel` |
| 11 | Firmware Version Query | ✅ Implemented | `ConnectivityViewModel` auto-queries on connect and UserCenter refresh; reply flows via `ConnectivityBridge.firmwareVersionNotifications()` |
| 12 | Task Completion Signal | ⏳ Pending app-side wiring | App -> badge `Command#end` after short/long recording transcription-analysis completes; emitter not yet wired |
| 13 | SD Card Space Query | ⏳ Pending app-side wiring | App-initiated query `SD#space` -> badge reply `SD#space#<size>` (e.g., `SD#space#27.23GB`); handler not yet implemented |

---

## Rate Limiting Policy

> **Purpose**: Prevent ESP32 freeze from BLE command overload

| Parameter | Value | Enforced By |
|-----------|-------|-------------|
| **BLE query floor** | 2s | `RateLimitedBleGateway.MIN_QUERY_INTERVAL_MS` |
| **Foreground query mode** | Event-driven only | connect/reconnect/setup/manual repair/explicit refresh |
| **HTTP endpoint reuse** | Current runtime only | `RealConnectivityBridge` active endpoint snapshot |
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

> **Re-confirmed**: 2026-04-24 firmware drop restated this contract unchanged (`tim#get` / `tim#YYYYMMDDHHMMSS`).

### 6. Recording End Notification (Scheduler Pipeline)

> **Added**: 2026-02-05 (firmware update from hardware team)
> **Updated**: 2026-02-08 — corrected command from `record#end` to `log#YYYYMMDD_HHMMSS`
> **Updated**: 2026-04-24 — firmware team restated the payload with the `.wav` extension included (`log#YYYYMMDD_HHMMSS.wav`); firmware team also labels this notification as the **short-recording** path. See §6-7 Semantic Reconciliation note below.

When user finishes recording on badge, ESP32 notifies app with the filename. Triggers the full download + transcribe + schedule pipeline.

```
Badge sends:  log#20260423_111020.wav    (YYYYMMDD_HHMMSS.wav, ESP32 local time)
App:          (downloads /download?file=log_20260423_111020.wav, transcribes, schedules)
```

**Workflow**:
1. User presses record button on badge
2. Badge sends `tim#get` → App responds with timestamp following pattern `tim#YYYYMMDDHHMMSS` (used for calibrating the ESP32 local time)
3. User records audio on the badge -> saved as `log_YYYYMMDD_HHMMSS.wav` (ESP32 local time; the timestamp sent by app is for clock calibration only, not prescriptive of the filename)
4. User stops recording
5. Badge sends `log#YYYYMMDD_HHMMSS.wav` (ESP32 local time; `.wav` suffix per 2026-04-24 drop — older firmware shipped the token without the suffix)
6. App downloads WAV via HTTP `/download?file=log_YYYYMMDD_HHMMSS.wav` (app learns filename from `log#` command)

### 7. Audio Recording Ready Notification (Drawer Pipeline)

> **Added**: 2026-04-09
> **Updated**: 2026-04-24 — firmware team restated the payload with the `.wav` extension included (`rec#YYYYMMDD_HHMMSS.wav`); firmware team also labels this notification as the **long-recording** path. See §6-7 Semantic Reconciliation note below.

When user finishes recording on badge and the file is ready for download, ESP32 notifies app with a `rec#` command. The current app-side routing treats this as audio-only download into the drawer (no transcription, no scheduling).

```
Badge sends:  rec#20260423_111020.wav    (YYYYMMDD_HHMMSS.wav, ESP32 local time)
App:          (downloads /download?file=rec_20260423_111020.wav, stores in audio drawer)
```

**Differences from `log#`** (current app-side routing, subject to §6-7 Semantic Reconciliation):
- `log#` → full scheduler pipeline (download + transcribe + schedule)
- `rec#` → audio-only pipeline (download + drawer placeholder, no transcribe/schedule)
- Both share the same `ConnectivityBridge.downloadRecording()` HTTP transport
- `/list` fallback on reconnection covers files recorded while app was disconnected

**Filename convention**: `rec#YYYYMMDD_HHMMSS.wav` → file stored as `rec_YYYYMMDD_HHMMSS.wav`

---

#### §6-7 Semantic Reconciliation (2026-04-24 drop)

The 2026-04-24 firmware note labels the two notifications as:

| Physical trigger (firmware label) | Command on wire |
|-----------------------------------|-----------------|
| **长录音** (long recording)        | `rec#<ts>.wav`  |
| **短录音** (short recording)       | `log#<ts>.wav`  |

The app-side routing above (log# → scheduler/transcribe; rec# → drawer/audio-only) was authored assuming the opposite physical-to-pipeline mapping (log# = full meeting transcription, rec# = short audio memo). The firmware team's labels may be describing different axes than the app assumed (e.g., short-button-press vs long-button-press on the hardware, independent of app intent) or may indicate a genuine semantic flip that the app has not yet caught up to.

**Status**: unresolved. The wire-level commands are unchanged (`log#…` and `rec#…` both still flow); the open question is which physical badge action produces which command, and whether the app's pipeline-to-command mapping still matches user intent. Tracked as a pending sprint under `docs/projects/firmware-protocol-intake/tracker.md`. Do not swap the app-side routing based on this note alone — confirm with the firmware team first.

### 8. Voice Volume Control

> **Added**: 2026-04-24 (firmware update from hardware team)

App-side control over the badge's voice-playback volume. Best-effort, fire-and-forget; the badge does not acknowledge.

```
App sends:  volume#<0..100>     (integer percent, app clamps out-of-range values)
Badge:      (no response)
```

**App-side**:
- Payload built in `DeviceConnectionManager.setBadgeVolume()` -> `badgeGateway.sendBadgeSignal(session, "volume#$clamped")`
- Callers are `ConnectivityModal` and `UserCenter` per `docs/cerb/interface-map.md` §connectivity
- UX rule: slider fires one command on finger-up only, not while scrubbing (avoids BLE flooding)

### 9. Battery Level Push

> **Added**: 2026-04-24 (firmware update from hardware team)

Badge pushes its current battery level unsolicited while BLE is connected. No query command exists; app must not poll.

```
Badge sends:  Bat#<0..100>      (integer percent, unsolicited periodic push)
```

**App-side contract**:
- Listener pattern mirrors `log#` / `rec#` notifications via `ConnectivityBridge`
- The shipped call chain is `GattBleGatewayProtocolSupport.parseBadgeNotificationPayload()` -> `DeviceConnectionManager.batteryEvents` -> `RealConnectivityBridge.batteryNotifications()` -> `ConnectivityViewModel.batteryLevel`
- The UI still keeps the current seeded `85` initial value until the first push arrives; nullable/no-reading semantics remain a separate UI sprint

### 10. Firmware Version Query

> **Added**: 2026-04-24 (format reserved; trigger TBD)
> **Updated**: 2026-04-24 (second drop, same day) — trigger finalized as app-initiated query/reply; delimiter changed from `:` to `#` to match the rest of the BLE protocol family (`tim#get` / `Ver#get`).

App queries the badge for its firmware version; badge replies with the version string. This is a pull protocol — the badge does not push version unsolicited.

```
App sends:    Ver#get
Badge sends:  Ver#<project>.<major>.<minor>.<feature>    (e.g., Ver#1.0.0.1)
```

**Field semantics** (left to right):
- `project` — project number; differentiates same codebase deployed across project variants
- `major` — product-level function / overall architecture / intended use changed
- `minor` — module-level functional enhancement
- `feature` — small changes, typically defect fixes

**App-side contract**:
- App sends `Ver#get` through `DeviceConnectionManager.requestFirmwareVersion()`
- Badge reply is parsed by `GattBleGatewayProtocolSupport.parseBadgeNotificationPayload()` as `BadgeNotification.FirmwareVersion`
- The shipped call chain is `DeviceConnectionManager.firmwareVersionEvents` -> `RealConnectivityBridge.firmwareVersionNotifications()` -> `ConnectivityViewModel.firmwareVersion`
- `ConnectivityViewModel` auto-requests on a fresh `Connected` transition, resets the state back to `null` on disconnect, and UserCenter exposes a manual refresh affordance
- Distinct from the HTTP `GET /` `version` field, which reports the webserver version and not the badge firmware version

### 11. Task Completion Signal

> **Added**: 2026-04-24 (firmware update from hardware team)

App tells the badge that the downstream work triggered by a short-recording command or long-recording transcription-analysis has finished. Fire-and-forget; the badge does not acknowledge. Used by the badge to exit a "processing" UX state (LED/tone/icon cue).

```
App sends:  Command#end
Badge:      (no response)
```

**App-side contract**: emitter not yet wired. The natural emission points are:
- after the scheduler pipeline completes for a `log#` drop (download + transcribe + schedule finished, success or terminal-error)
- after the drawer pipeline completes for a `rec#` drop (download finished, success or terminal-error)

Exact placement (one site per pipeline vs a single sink driven by both pipelines) is a sprint-authoring decision; see `docs/projects/firmware-protocol-intake/tracker.md`. UX rule candidate: send exactly once per triggering command, do not retry on BLE failure (best-effort parity with `volume#`).

### 12. SD Card Space Query

> **Added**: 2026-04-24 (firmware update from hardware team)

App queries the badge for remaining SD card free space; badge replies with a human-readable size string. Pull protocol — the badge does not push SD space unsolicited.

```
App sends:    SD#space
Badge sends:  SD#space#<size>      (e.g., SD#space#27.23GB)
```

**Size format**: the badge returns the value as a pre-formatted human string (`27.23GB`, `512MB`, etc.). The app must parse this defensively (no guarantee of unit, decimal precision, or whitespace). If parsing fails, surface the raw string rather than blocking on it.

**Caveat — fragmented response shape**: this command shares the `SD#` prefix with the WiFi-status-query fragment (§1, where the badge emits `SD#<SSID>` as one of two notifications). The disambiguator is the `space` token after the first `#`:
- `SD#<SSID>` — WiFi status fragment (legacy, §1)
- `SD#space#<size>` — SD card space reply (this section)

App-side parsers must check the second segment before routing.

**App-side contract**: handler not yet implemented. When wired, the natural call site is a UserCenter / diagnostics surface (the value has no reactive-UI use case that would warrant a subscription-style listener).

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
└── log_YYYYMMDD_HHMMSS.wav  # Timestamped recording
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
