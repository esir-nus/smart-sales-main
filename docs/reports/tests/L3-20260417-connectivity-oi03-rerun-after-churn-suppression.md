# L3 OI-03 Rerun After Churn Suppression

Date: 2026-04-17
Branch: `feature/agent-coalition-governance-sync`
Device: `fc8ede3e` (`2410DPN6CC`)
Evidence class: on-device `adb logcat`

## Scope

This L3 rerun targeted the OI-03 app-side stabilization slice after:

- manual-repair failure copy normalization
- graduated post-credential HTTP grace budget (`2s -> 3s -> 5s`)
- `phoneSsidForDiag=` diagnostic rename
- SIM audio AUTO known-HTTP-unreachable latch suppression

Checked behaviors:

- manual repair keeps an HTTP-unreachable result explicit after the grace budget
- same-build endpoint resolution can still reach badge HTTP when the network
  path is actually valid
- AUTO sync no longer churns on a known-unreachable runtime + endpoint
- the prior shell reconnect overwrite symptom does not reappear in the rerun

The filtered capture was saved to `/tmp/l3-20260417-oi03-rerun.log`.

## Sources Used

- `docs/core-flow/base-runtime-ux-surface-governance-flow.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/plans/bug-tracker.md`
- `/tmp/l3-20260417-oi03-rerun.log`

No dedicated connectivity Core Flow document was found for this slice.

## What Was Verified

- fresh debug APK install and app launch on the physical device
- manual repair on `MotionTime1` consumed the full grace budget and surfaced
  explicit `HTTP_UNREACHABLE`
- bridge diagnostics now log `phoneSsidForDiag=` instead of `phoneSsid=`
- manual repair on `MstRobot` reached real badge HTTP `200`
- AUTO sync suppression logs fired repeatedly once `MotionTime1`
  `http://192.168.1.18:8088` was known unreachable
- repeated `MotionTime1` download failures no longer required repeated AUTO
  preflight churn to reproduce

## Key Runtime Findings

### 1. `MotionTime1` still failed `:8088` after the full grace budget

Evidence:

- `/tmp/l3-20260417-oi03-rerun.log:27`
- `/tmp/l3-20260417-oi03-rerun.log:30`
- `/tmp/l3-20260417-oi03-rerun.log:36`
- `/tmp/l3-20260417-oi03-rerun.log:42`
- `/tmp/l3-20260417-oi03-rerun.log:44`

Observed behavior:

- manual repair progressed from `IP#0.0.0.0` to `IP#192.168.1.18`
- the repair loop probed `http://192.168.1.18:8088` on attempts 2 and 3
- both probes failed with `ConnectException:Failed to connect to /192.168.1.18:8088`
- the result stayed explicit as `🛜 repair outcome=http_unreachable`

Interpretation:

- the new grace schedule is active
- the app no longer reports manual repair success on this path just because the
  badge obtained a usable IP

### 2. The old reconnect-overwrite symptom was not reproduced in this rerun

Evidence:

- `/tmp/l3-20260417-oi03-rerun.log:44`
- `/tmp/l3-20260417-oi03-rerun.log:61`
- `/tmp/l3-20260417-oi03-rerun.log:68`
- `/tmp/l3-20260417-oi03-rerun.log:97`

Observed behavior:

- after `🛜 repair outcome=http_unreachable`, the next preflight stayed on
  `result=not-ready baseUrl=http://192.168.1.18:8088`
- the rerun did not capture the prior pattern where shell reconnect immediately
  re-established a generic connected result and cleared the failure diagnosis

Interpretation:

- this run supports the app-side reconnect guard change
- it is still evidence from one rerun, not a proof that the symptom is
  impossible on every path

### 3. `MstRobot` proved that the same build can still reach badge HTTP

Evidence:

- `/tmp/l3-20260417-oi03-rerun.log:674`
- `/tmp/l3-20260417-oi03-rerun.log:677`
- `/tmp/l3-20260417-oi03-rerun.log:681`
- `/tmp/l3-20260417-oi03-rerun.log:735`
- `/tmp/l3-20260417-oi03-rerun.log:742`
- `/tmp/l3-20260417-oi03-rerun.log:743`

Observed behavior:

- manual repair on `MstRobot` resolved `ip=192.168.0.108`
- `BadgeHttpClient.isReachable` returned `code=200 reachable=true`
- manual repair completed as `🛜 repair outcome=wifi_provisioned`
- subsequent `isReady` preflight returned `result=ready`

Interpretation:

- the blanket claim that same-network badge HTTP still fails is no longer true
- the app, phone, and current build can reach badge HTTP when the network path
  is healthy

### 4. Switching back to `MotionTime1` still failed downloads, and AUTO churn suppression engaged

Evidence:

- `/tmp/l3-20260417-oi03-rerun.log:1632`
- `/tmp/l3-20260417-oi03-rerun.log:1633`
- `/tmp/l3-20260417-oi03-rerun.log:2216`
- `/tmp/l3-20260417-oi03-rerun.log:2217`
- `/tmp/l3-20260417-oi03-rerun.log:2485`

Observed behavior:

- the endpoint refreshed back to `http://192.168.1.18:8088`
- repeated background downloads failed with `Failed to connect to /192.168.1.18:8088`
- the new `SIM badge auto sync suppressed ... reason=known_http_unreachable`
  line fired repeatedly for the same runtime and base URL

Interpretation:

- the latch is working as intended
- remaining failure ownership is now specific to `MotionTime1` reachability or
  the badge HTTP service on that network, not app-side retry churn

## What Remains Unverified

- this rerun did not prove why `MotionTime1` blocks badge HTTP; firmware,
  badge-side listener state, and AP isolation remain open
- this rerun did not exercise a dedicated manual-sync success path after the
  latch had already armed
- no packet-level network capture was taken, so the failure is localized by
  app evidence only

## Verdict

Status: conditionally accepted

Reason:

- the app-side OI-03 stabilization slice behaved as intended in L3:
  `HTTP_UNREACHABLE` stayed explicit, `phoneSsidForDiag=` shipped, same-build
  HTTP success was captured on `MstRobot`, and AUTO churn suppression emitted
  the expected telemetry on `MotionTime1`
- `MotionTime1` still failed `192.168.1.18:8088`, but the rerun narrows that
  remaining ownership to firmware / badge HTTP service / AP isolation rather
  than stale IP handling, phone-SSID endpoint gating, or repeated AUTO sync
  churn in the app
