# ESP32 Connectivity Debug SOP

> Purpose: provide the formal live-capture workflow for ESP32 badge connectivity and recording-ingress debugging.
> Last Updated: 2026-03-26

## When to Use

Use this SOP when the badge appears connected but:

- badge mic recording does not trigger SIM follow-up behavior
- `ConnectivityBridge.recordingNotifications()` is suspected to be silent
- BLE traffic exists but the app does not react downstream
- Wave 9 or Wave 10 needs device evidence instead of assumptions

For connectivity-module troubleshooting, this SOP is mandatory.

- start from `adb logcat` capture through `scripts/esp32_connectivity_debug.sh`
- do not treat screenshots, transient UI text, or recollection as sufficient proof on their own

Working companion:

- `docs/plans/bug-tracker.md`

## Formal Tool

Authoritative debug entrypoint:

- `scripts/esp32_connectivity_debug.sh`
- the script is the required `adb logcat` path for connectivity investigations unless a narrower adb command is explicitly needed for the same tags

The tool captures the two runtime tags that matter for the current bridge path:

- `SmartSalesConn`
- `AudioPipeline`

## Live Capture

Default device:

```bash
scripts/esp32_connectivity_debug.sh capture
```

Specific adb device:

```bash
scripts/esp32_connectivity_debug.sh capture --device <serial>
```

Explicit output path:

```bash
scripts/esp32_connectivity_debug.sh capture --output /tmp/esp32_wave10.log
```

Behavior:

- clears logcat by default for a clean repro window
- streams live logs to terminal
- saves the same capture to a file for later summary

## Post-Capture Summary

```bash
scripts/esp32_connectivity_debug.sh summary /tmp/esp32_wave10.log
```

The summary reports:

- BLE TX and RX counts
- network query traffic
- `tim#get` and time-sync response evidence
- `log#YYYYMMDD_HHMMSS` evidence
- manager-level `Badge recording ready` evidence
- `AudioPipeline` ingress evidence
- unknown notification lines

## After-Capture Sync

After any meaningful repro or validation attempt:

1. append the dated evidence summary to the corresponding issue entry in `docs/plans/bug-tracker.md`
2. if the issue is genuinely fixed and the user explicitly confirms it, extract only the critical reusable lesson into:
   - `.agent/rules/lessons-learned.md`
   - `docs/reference/agent-lessons-details.md`

## Re-entry Gate Signals

Wave 10 device re-entry is only satisfied when one capture shows all of the following in the real path:

1. connectivity ingress traffic
2. `Badge recording ready`
3. `AudioPipeline` recording-detected logs

Helpful extra evidence:

- `tim#get`
- `Time sync responded`
- visible `log#YYYYMMDD_HHMMSS`

## Current Known Failure Signature

The current blocked signature is:

- BLE TX/RX exists
- repeated `wifi#address#ip#name` queries may appear
- `IP#...` and `SD#...` fragments may appear
- no `tim#get`
- no `Time sync responded`
- no `log#YYYYMMDD_HHMMSS`
- no `Badge recording ready`
- no `AudioPipeline` ingress

Interpretation:

- the Android app is still receiving BLE traffic in general
- the app-side listener is not proven dead by this signature alone
- the badge recording path is likely failing upstream of the app ingress contract

See:

- `docs/reports/20260322-esp32-live-capture-findings.md`
- `docs/plans/bug-tracker.md`

## Known Side Observation

Network-query fragments such as `IP#...` and `SD#...` can currently appear on the persistent notification listener and be logged as unknown notifications.

Treat that as observability debt unless it directly breaks recording ingress. Do not confuse it with proof that the recording-end notification arrived.
