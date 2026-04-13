# L3 Connectivity Reconnect OEM SSID Drift

Date: 2026-04-13
Branch: `harmony/tingwu-container`
Device: `fc8ede3e`
Evidence class: on-device `adb logcat`

## Scope

This L3 rerun targeted the active Android connectivity runtime after the BLE hardening handoff.

Checked behaviors:

- install and launch of the production package `com.smartsales.prism`
- manual reconnect from the in-app connectivity surface
- badge sync gate behavior from the SIM audio drawer path
- reconnect/runtime readiness evidence under real badge BLE traffic

Note: the original ESP32 capture helper referenced by the SOP is absent in this checkout, so this rerun used direct `adb logcat` capture filtered to the active connectivity tags.

## Sources Used

- `docs/plans/tracker.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `/tmp/ble_reconnect_20260413.log`

No dedicated reconnect Core Flow document was found for this slice.

## What Was Verified

- `com.smartsales.prism/.MainActivity` launched on device successfully
- runtime BLE and connectivity permissions were granted on-device
- live log capture recorded `SmartSalesConn`, `AudioPipeline`, `ConnectivityService`, `ConnectivityVM`, and `BT311Scan`
- the badge was discovered and provisioned successfully
- the badge later reported usable network state (`IP#192.168.0.108`, `SD#MstRobot`) over BLE

## Key Runtime Findings

### 1. Manual reconnect is blocked by unreadable phone SSID even when badge IP is usable

Evidence:

- `/tmp/ble_reconnect_20260413.log:286`
- `/tmp/ble_reconnect_20260413.log:310`
- `/tmp/ble_reconnect_20260413.log:330`

Observed behavior:

- reconnect restored BLE
- the badge reported usable IP `192.168.0.108`
- the app still logged `reconnect blocked while badge has ip=192.168.0.108: phone Wi‑Fi connected but SSID unreadable raw=<unknown ssid>`
- UI service result became `WifiMismatch(currentPhoneSsid=null)` instead of connected

Why this is drift:

- the active tracker contract already says reconnect should trust the badge auto-reconnect path and treat BLE + usable badge IP as connected
- the live code still gates reconnect on readable phone SSID

### 2. Manual repair can still look successful after confirmation failure

Code-path review after the L3 run confirmed that `RealConnectivityService.updateWifiConfig()` still returns success for some confirmation-failure branches instead of surfacing a hard error.

Relevant code:

- `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityService.kt:108`

Impact:

- operator sees a repair flow that appears to succeed
- runtime may still be offline or unresolved underneath

### 3. SIM badge sync is blocked behind strict readiness preflight

Evidence:

- `/tmp/ble_reconnect_20260413.log:249`
- `/tmp/ble_reconnect_20260413.log:252`
- `/tmp/ble_reconnect_20260413.log:665`
- `/tmp/ble_reconnect_20260413.log:669`

Observed behavior:

- bridge resolved `http://192.168.0.108:8088`
- HTTP reachability failed with `ConnectException`
- manual sync hit `strict_precheck_blocked`
- auto sync logged `reason=not-ready`

Interpretation:

- this capture proves the sync lane is blocked by runtime readiness not recovering cleanly
- this capture does not prove missing `rec#` ingress; it proves the app still considers the transport not ready for sync

### 4. Notification-listener state race is still present

Evidence:

- `/tmp/ble_reconnect_20260413.log:145`
- `/tmp/ble_reconnect_20260413.log:244`
- `/tmp/ble_reconnect_20260413.log:638`
- `/tmp/ble_reconnect_20260413.log:660`

Observed behavior:

- repeated `Skip connected-state promotion: notification listener inactive`
- this appears around reconnect/setup windows where a new listener had just been started

Code review points to a stale-job race:

- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerIngressSupport.kt:16`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerConnectionSupport.kt:318`

Likely failure mode:

- a canceled older listener job reaches `finally` and flips `notificationListenerActive` false
- the newer live listener is still running, but connected-state promotion now sees the stale false flag

## What Remains Unverified

- the automatic reconnect scheduler path after an unexpected disconnect is still not fully proven by this capture
- the user reported auto-reconnect failure, but this log window did not include the full `unexpectedDisconnects()` to backoff-to-reconnect evidence chain

## Verdict

Status: blocked

Reason:

- the live runtime still drifts from the intended reconnect contract on target OEM devices
- manual reconnect is blocked by unreadable SSID
- manual repair error handling is still too soft
- listener readiness race still undermines connected-state promotion
- sync remains blocked by the resulting not-ready state

## Proposed Fix Slice

1. update shared docs so unreadable phone SSID is treated as normal OEM reality, not reconnect failure
2. change reconnect semantics so BLE + usable badge IP becomes connected immediately without SSID gating
3. remove reconnect-time automatic credential replay and reserve remembered credentials for explicit manual repair only
4. make manual repair confirmation failures return explicit errors instead of synthetic success
5. fix the notification-listener active-state race so stale canceled listeners cannot suppress connected-state promotion
6. rerun L3 with one clean capture covering:
   - manual reconnect
   - unexpected disconnect to auto reconnect
   - SIM badge sync after reconnect
