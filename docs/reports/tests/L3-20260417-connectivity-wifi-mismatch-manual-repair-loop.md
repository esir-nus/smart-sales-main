# L3 Connectivity Wi-Fi Mismatch Manual Repair Loop

Date: 2026-04-17
Branch: `feature/agent-coalition-governance-sync`
Device: `fc8ede3e` (`2410DPN6CC`)
Evidence class: on-device `adb logcat`

## Scope

This L3 run targeted the recently added Android manual-sync to `WIFI_MISMATCH`
handoff plus the manual Wi-Fi repair path on a physical badge.

Checked behaviors:

- manual sync failure auto-opens the existing Wi-Fi mismatch modal
- manual Wi-Fi repair sends BLE credentials and runs the bounded confirmation loop
- post-repair sync uses the refreshed badge IP and `:8088` endpoint
- recording-ready ingress still works after repair

Note: the active run used direct `adb logcat` capture filtered to
`VALVE_PROTOCOL`, `AudioPipeline`, `ConnectivityService`, `ConnectivityVM`,
`SmartSalesConn`, and `BluetoothGatt`. The filtered capture was saved to
`/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log`.

## Sources Used

- `docs/core-flow/base-runtime-ux-surface-governance-flow.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/plans/bug-tracker.md`
- `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log`

No dedicated connectivity Core Flow document was found for this slice.

## What Was Verified

- `com.smartsales.prism/.MainActivity` was installed and launchable on device
- manual sync hit the current `AudioPipeline` strict-precheck path
- manual repair wrote both `SD#...` and `PD#...` over BLE for two submitted SSIDs
- manual repair no longer reuses generic reconnect; it waited for bounded network
  confirmation and logged explicit confirm attempts
- the app refreshed the badge endpoint IP after repair instead of reusing a stale
  pre-repair IP
- `rec#` notification ingress still reached the app after the second repair

## Key Runtime Findings

### 1. The app is not stuck on the old badge IP

Evidence:

- `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log:252`
- `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log:481`

Observed behavior:

- after the first submitted repair, the app refreshed the endpoint to
  `http://192.168.0.109:8088`
- after the second submitted repair, the app refreshed the endpoint to
  `http://192.168.1.18:8088`

Interpretation:

- the failure is not a stale-IP cache bug
- endpoint refresh is happening, but the refreshed endpoint still cannot be
  reached over HTTP

### 2. Manual repair can still declare success while the phone remains on a different network

Evidence:

- `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log:226`
- `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log:251`
- `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log:256`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt:444`

Observed behavior:

- the first submitted repair wrote `SD#MstRobot` and `PD#Cai123456`
- the bounded confirm loop then marked the repair as confirmed online at
  `192.168.0.109`
- the next manual sync immediately failed HTTP reachability to that endpoint
  from phone address `192.168.1.35`
- the same log line that refreshed the endpoint recorded `phoneSsid=` (blank)

Interpretation:

- because current phone SSID was unreadable, bridge alignment allowed the app to
  accept the badge endpoint without proving the phone and badge were on the same
  Wi-Fi
- this creates a false-positive "repair successful" outcome: BLE confirms the
  badge got onto some Wi-Fi, but not that the phone can actually reach the badge

### 3. Correct credentials also refreshed the IP but HTTP `:8088` was still unreachable

Evidence:

- `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log:455`
- `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log:480`
- `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log:485`

Observed behavior:

- the second submitted repair wrote `SD#MotionTime1` and `PD#MT123456`
- the bounded confirm loop marked the badge online at `192.168.1.18`
- the next sync refreshed `http://192.168.1.18:8088`
- HTTP reachability still failed with `ConnectException:Failed to connect to /192.168.1.18:8088`

Interpretation:

- the manual repair path succeeded at BLE credential dispatch and badge IP
  refresh, but that still did not prove the badge HTTP service was listening on
  `:8088`
- this is a separate failure from the first false-positive cross-network success

### 4. Recording-ready ingress survived the repair, but download still failed on the refreshed endpoint

Evidence:

- `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log:552`
- `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log:570`
- `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log:581`

Observed behavior:

- the app received `rec#20260417_143440`
- auto-download queried network status again and refreshed
  `http://192.168.1.18:8088`
- the WAV download still failed with `Failed to connect to /192.168.1.18:8088`

Interpretation:

- the badge-to-app BLE notification path is alive after repair
- the blocked lane is specifically badge HTTP reachability, not recording-ready
  ingress

## What Remains Unverified

- this run did not capture a clean close/reopen proof for stale mismatch override
- there is still no dedicated `VALVE_PROTOCOL` telemetry for the post-submit
  manual-repair confirmation path
- this capture does not prove whether the `:8088` failure is badge firmware, AP
  isolation, or an app-side readiness contract that is still too weak

## Verdict

Status: blocked

Reason:

- the app refreshes IP correctly, so the original stale-IP hypothesis is not
  supported by the evidence
- when phone SSID is unreadable, the app can still accept a badge endpoint on a
  different network and report repair success too early
- even after the badge later moves onto the intended network, HTTP `:8088`
  remains unreachable while BLE recording notifications continue to arrive

## Recommended Fix Slice

1. tighten the app-side readiness contract so manual repair success requires more
   than "badge has a usable IP"
2. stop treating unreadable phone SSID as implicitly aligned for HTTP endpoint
   reuse/refresh, or add a stronger alternate proof before declaring repair
   success
3. add explicit telemetry/logging on the post-submit repair path so UI success,
   confirmed badge IP, and HTTP readiness can be correlated in one route
4. rerun L3 after the app-side change with one capture that proves:
   - submitted repair does not report success on a cross-network endpoint
   - same-network repair either reaches `:8088` or emits a clearer ownership
     diagnosis
   - close/reopen does not revive stale mismatch UI state
