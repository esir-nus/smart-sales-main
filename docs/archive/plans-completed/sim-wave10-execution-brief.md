# SIM Wave 10 Execution Brief

> Last Updated: 2026-03-22
> Status: In Progress

## Objective

Repair the real badge ingress blocker discovered during `T9.0` so SIM can return to honest physical-badge follow-up validation. This wave owns badge recording transport and connection-truth hardening only; it does not extend the SIM follow-up feature scope.

## Scope

- Restore BLE `log#YYYYMMDD_HHMMSS` ingress into `ConnectivityBridge.recordingNotifications()`
- Freeze `RecordingNotification.RecordingReady.filename` to `log_YYYYMMDD_HHMMSS.wav`
- Stop showing healthy connected/reconnect success when persistent GATT notification listening is not actually alive
- Add focused L1 verification for parsing, manager ingress, bridge gating, and pipeline notification entry

## Cautions

- Do not mask the transport defect with HTTP `/list` polling fallback
- Do not treat BLE-only session selection as a healthy connected state
- Do not reopen Wave 9 until real on-device badge ingress is visible again

## Deliverables

- Runtime repair in the legacy connectivity manager, bridge, reconnect service, and BLE notification parser
- Formal live-capture debug tool: `scripts/esp32_connectivity_debug.sh`
- Formal debug SOP: `docs/sops/esp32-connectivity-debug.md`
- Live-capture evidence note: `docs/reports/20260322-esp32-live-capture-findings.md`
- Focused verification:
  - `GattBleGatewayNotificationParsingTest`
  - `DefaultDeviceConnectionManagerIngressTest`
  - `RealConnectivityBridgeTest`
  - `RealBadgeAudioPipelineIngressTest`
- Tracker/spec sync for the repaired connection truth and filename contract

## Re-entry Gate

Wave 9 may resume only after a real badge recording again produces:

1. connectivity ingress logs
2. `AudioPipeline` recording-detected logs
3. observable SIM follow-up prompt/chip behavior from hardware-origin ingress

Use `scripts/esp32_connectivity_debug.sh` as the formal capture path for this gate.
