# ESP32 Live Capture Findings

Date: 2026-03-22

Purpose: record the current real-device connectivity evidence after the Wave 10 ingress hardening work and before reopening Wave 9 hardware validation.

## Capture Context

- target: real Android device plus physical ESP32 badge
- focus: badge mic repro during a live `SmartSalesConn` and `AudioPipeline` capture
- capture intent: verify whether recording stop on the badge emits the expected ingress signals into the app

## Observed Evidence

BLE traffic was alive in general during the session.

Repeated traffic observed:

- TX: `wifi#address#ip#name`
- RX: `IP#192.168.0.103`
- RX: `SD#MstRobot`

Summary counts from the saved live capture:

- TX packets: 55
- RX packets: 220
- network query traffic: 55
- unknown notifications: 110
- time sync requests: 0
- time sync responses: 0
- recording log tokens: 0
- manager recording-ready logs: 0
- audio pipeline ingress logs: 0

Missing during the badge mic repro:

- `tim#get`
- `Time sync responded`
- `log#YYYYMMDD_HHMMSS`
- `Badge recording ready`
- `AudioPipeline` recording-detected or download-start logs

## Interpretation

Current evidence does not support an app-side total BLE failure.

What this evidence does support:

- the Android app can still send and receive BLE traffic
- the persistent listener is still seeing at least some notification traffic
- the expected recording-end notification path was not triggered during the real repro

Current best interpretation:

- the blocker is likely upstream of the app ingress pipeline
- Wave 10 device re-entry remains unproven
- Wave 9 must stay blocked until a capture shows real recording-end ingress

## Secondary Observation

Network query fragments such as `IP#...` and `SD#...` were also seen by the persistent notification listener and logged as unknown notifications.

That is useful observability debt, but it is not enough to explain the missing recording-end signal by itself.

## Formal Debug Path

This evidence is now formalized into the repository debug workflow:

- tool: `scripts/esp32_connectivity_debug.sh`
- SOP: `docs/sops/esp32-connectivity-debug.md`

Use that path for all further Wave 10 and Wave 9 re-entry captures so future evidence is comparable.
