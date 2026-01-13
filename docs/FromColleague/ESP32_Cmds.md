# ESP32 Communication Protocol (Colleague Notes)

> **⚠️ This doc updated 2026-01-12 to match actual firmware behavior.**
> **Authoritative source**: `docs/specs/esp32-protocol.md`

ESP32 doesn't have a UI, it's only backend.

## 1. App queries WiFi status from badge

```
App sends (BLE):     wifi#address#ip#name     // literal fixed string
Badge returns (BLE): wifi#address#<IP>#<SSID>  // single response with actual values
```

Example:
```
App:   wifi#address#ip#name
Badge: wifi#address#192.168.0.101#MstRobot
```

## 2. App tells badge to connect to new WiFi

**Two-step protocol:**

```
Step 1: App sends (BLE): SD#<ssid>
Step 2: App sends (BLE): PD#<password>
```

Example:
```
App: SD#MstRobot
App: PD#Cai123456
```

After sending both commands, poll `wifi#address#ip#name` until badge returns an IP.

## 3. GIF/JPG transfer flow

```
1. App sends:     jpg#send
2. Badge returns: jpg#receive
3. App uploads via HTTP POST /upload (multipart, .jpg only, 240x280)
4. App sends:     jpg#end          // after ALL frames uploaded
5. Badge returns: jpg#display
```

Frame naming: `1.jpg`, `2.jpg`, `3.jpg`...

## 4. WAV download flow

```
1. App sends:     wav#get
2. Badge returns: wav#send
3. App calls HTTP: GET /list → GET /download?file=xxx.wav
4. App sends:     wav#end
5. Badge returns: wav#ok
```

## 5. Time sync (badge-initiated)

```
Badge sends:  time#get
App returns:  time#YYYYMMDDHHMMSS
```

Example: `time#20260112175600`

## HTTP Server

- Port: **8088**
- Endpoints:
  - `GET /` → health check
  - `POST /upload` → upload JPG (multipart)
  - `GET /list` → list WAV files
  - `GET /download?file=xxx.wav` → download WAV
  - `POST /delete` body `filename=xxx.wav` → delete WAV

## Notes

- GIF frames must be 240×280 resolution
- Frame files: `1.jpg`, `2.jpg`, etc.
- All files stored in `/sdcard/`