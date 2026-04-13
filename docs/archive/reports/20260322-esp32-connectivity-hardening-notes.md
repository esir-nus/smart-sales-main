# ESP32 Connectivity Hardening Notes

Date: 2026-03-22

Purpose: record the minimal ESP32/connectivity hardening changes plus exact rollback seams if any change proves unhelpful on real hardware.

## Change 1: time-sync contract doc sync

- Change: aligned the definitive ESP32 protocol doc to `tim#get` -> `tim#YYYYMMDDHHMMSS`
- Evidence: shipped legacy gateway sends `tim#...`; Cerb connectivity shard already documents `tim#...`
- Expected benefit: removes doc-first drift around the badge clock-calibration path
- Risk: low; doc-only
- Rollback condition: hardware evidence proves firmware now requires a different response payload
- Rollback action: restore the prior wording in `docs/specs/esp32-protocol.md` and attach the dated hardware evidence

## Change 2: recording filename contract doc sync

- Change: normalized recording-ready docs to the full downloadable filename `log_YYYYMMDD_HHMMSS.wav`
- Evidence: current connectivity bridge, domain model, and ingress tests already use the full downloadable filename
- Expected benefit: removes confusion between BLE token payload and HTTP download filename
- Risk: low; doc-only
- Rollback condition: production code returns to raw timestamp tokens instead of full downloadable filenames
- Rollback action: restore the prior filename wording in connectivity docs

## Change 3: provisioning success promotion order

- Change: in `DefaultDeviceConnectionManager.handleProvisioningSuccess()`, start the notification listener before promoting to `WifiProvisioned`
- Evidence: the hardening goal for this slice is that a usable connection should not surface before persistent notification listening is in place
- Expected benefit: narrows the race window where UI/runtime could treat the bridge as fully usable before recording/time-sync ingress is listening
- Risk: medium-low; same flow, only promotion order changed
- Rollback condition: real-device pairing or reconnect becomes less stable after this change
- Rollback action: revert only the promotion-order change in `DeviceConnectionManager.kt`, keep the doc sync and parsing hardening intact

## Change 4: fake notification flow contract alignment

- Change: removed replay from `FakeDeviceConnectionManager.recordingReadyEvents`
- Evidence: public connectivity contract documents this as a hot buffered flow with no replay
- Expected benefit: keeps tests closer to the production contract and avoids fake-only semantics leaking into design assumptions
- Risk: low; tests may need collectors to subscribe before emit
- Rollback condition: targeted connectivity tests become flaky and the replay-free fake adds no real value
- Rollback action: reintroduce fake replay only in the test fake and document it as test stabilization behavior
