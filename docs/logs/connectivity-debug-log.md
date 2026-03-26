# Connectivity Debug Log

> Purpose: living BLE + Wi-Fi connectivity incident history for app engineers and the hardware team.
> Scope: pairing, provisioning, network query, notification ingress, and bridge/pipeline transport truth.
> Last Updated: 2026-03-26

## How to Use This Log

- Use this file as the running communication history for hardware-facing connectivity work.
- Keep dated entries in reverse chronological order.
- Link deeper evidence instead of copying raw logs here.
- Keep protocol/spec truth in the owning docs; use this file to record what actually happened on real runs.

Primary references:

- Protocol SOT: `docs/specs/esp32-protocol.md`
- Connectivity shard: `docs/cerb/connectivity-bridge/spec.md`
- Pairing shard: `docs/cerb/device-pairing/spec.md`
- Live-capture SOP: `docs/sops/esp32-connectivity-debug.md`
- Current Wave 10 tracker: `docs/plans/sim-tracker.md`

## Current Stable Facts

- BLE recording ingress is expected to arrive as `log#YYYYMMDD_HHMMSS`.
- The app-side downloadable filename contract is `log_YYYYMMDD_HHMMSS.wav`.
- Badge Wi-Fi status query uses `wifi#address#ip#name` and may return fragmented `IP#...` plus `SD#...` notifications.
- Time sync uses badge `tim#get` and app response `tim#YYYYMMDDHHMMSS`.
- Wave 10 already hardened connection-truth reporting so BLE-only connection success should not be treated as fully healthy transport when persistent notification listening is absent.

## Active Open Issues

### OI-01: Real badge recording-end ingress still unproven on device

- Status: Open
- Current interpretation: BLE traffic exists, but real `log#...` recording-end ingress has not yet been captured in the app pipeline on the blocked repro path.
- Primary evidence:
  - `docs/reports/20260322-esp32-live-capture-findings.md`
  - `docs/sops/esp32-connectivity-debug.md`
- Hardware-team question:
  - On the failing badge/firmware build, when recording stops, is `log#YYYYMMDD_HHMMSS` always emitted over the same BLE notification path used for `tim#get` and Wi-Fi query fragments?

### OI-02: Pairing/provisioning path likely assumes a BLE ack that the protocol does not guarantee

- Status: App-side mitigation shipped, device revalidation pending
- Current interpretation: the app previously waited for an immediate provisioning ack after `SD#...` and `PD#...`, then could fall back to reading `6E400003...`; code is now aligned to the repo protocol SOT and the remaining requirement is real-device revalidation.
- Primary evidence:
  - `docs/specs/esp32-protocol.md`
  - `docs/cerb/connectivity-bridge/spec.md`
  - manual repro on 2026-03-26 showing `读取特征失败: 6e400003-b5a3-f393-e0a9-e50e24dcca9e`
- Hardware-team question:
  - After receiving `SD#<ssid>` and `PD#<password>`, does firmware emit any immediate BLE success/failure payload, or should the app rely only on subsequent `wifi#address#ip#name` polling?

## Incident History

### 2026-03-26

#### Incident: App-side Wi-Fi connect path aligned to two-step write protocol

- Scenario:
  - Focused code repair after the `6E400003` pairing failure repro.
- Observed evidence:
  - The Wi-Fi connect path no longer waits for an immediate BLE provisioning ack.
  - The app no longer falls back to reading the provisioning status characteristic to decide Wi-Fi connect success.
  - Pairing docs and tests were updated to treat Wi-Fi connect as credential dispatch followed by later network query validation.
- App interpretation:
  - This removes the most likely app-side cause of the `读取特征失败: 6e400003...` pairing failure.
  - Real hardware confirmation is still required before treating the issue as fully closed.
- Linked evidence:
  - `docs/cerb/device-pairing/spec.md`
  - `docs/cerb/device-pairing/interface.md`
  - `docs/plans/sim-tracker.md`
- Next action:
  - Re-run physical-badge pairing and capture the outcome with the formal ESP32 debug workflow.

#### Incident: Pairing failed with `读取特征失败` on `6E400003`

- Scenario:
  - User attempted physical-badge pairing/provisioning from the onboarding flow.
- Observed evidence:
  - UI showed `配网失败`.
  - Detailed error text showed `WiFi 配网失败: Transport(reason=读取特征失败: 6e400003-b5a3-f393-e0a9-e50e24dcca9e)`.
- App interpretation:
  - BLE likely connected well enough to reach the write/provision stage.
  - The failing operation is consistent with an app-side fallback read against the NUS notify/status characteristic rather than proof that all BLE transport is broken.
  - Current working hypothesis is protocol drift in the app provisioning-completion logic, not a generic BLE failure.
- Hardware-team ask:
  - Confirm whether `6E400003` is notify-only on the target firmware and should never be used for direct reads in the Wi-Fi connect path.
- Next action:
  - Keep this exact UUID/error string in future repro notes.
  - Compare firmware expectation for Wi-Fi-connect completion against the app’s current immediate-ack assumption.

### 2026-03-22

#### Incident: Live capture showed BLE traffic but no recording-end ingress

- Scenario:
  - Real Android device plus physical ESP32 badge.
  - Focused badge mic repro during live `SmartSalesConn` and `AudioPipeline` capture.
- Observed evidence:
  - BLE TX and RX traffic existed.
  - Repeated `wifi#address#ip#name`, `IP#...`, and `SD#...` traffic appeared.
  - No `tim#get`, `Time sync responded`, `log#...`, `Badge recording ready`, or `AudioPipeline` ingress logs were captured.
- App interpretation:
  - App-side BLE traffic was alive in general.
  - The capture did not prove total app-side BLE listener failure.
  - The missing recording-end signal likely remained upstream of the app ingress contract in that run.
- Linked evidence:
  - `docs/reports/20260322-esp32-live-capture-findings.md`
- Next action:
  - Use the formal SOP and capture path for all further Wave 10 / Wave 9 re-entry attempts so evidence stays comparable.

#### Incident: Wave 10 hardening aligned connection truth and filename docs/code

- Scenario:
  - Focused hardening/documentation pass for ESP32 recording ingress and connection-truth seams.
- Observed evidence:
  - Time-sync contract docs aligned to `tim#get` -> `tim#YYYYMMDDHHMMSS`.
  - Recording filename contract normalized to full downloadable filename form.
  - Promotion order was tightened so notification listening is in place before reporting a healthy provisioned state.
- App interpretation:
  - This reduced optimistic “healthy” states and made the transport truth closer to real bridge readiness.
- Linked evidence:
  - `docs/reports/20260322-esp32-connectivity-hardening-notes.md`
- Next action:
  - Revalidate on real hardware rather than treating doc/code hardening as proof of re-entry success.

### 2026-03-21

#### Incident: SIM Wave 5 physical-badge connectivity flow accepted

- Scenario:
  - Real badge setup from SIM connectivity route.
- Observed evidence:
  - Manager direct-entry worked from `DISCONNECTED`.
  - Onboarding-derived setup completed real BLE/Wi-Fi provisioning.
  - Success transitioned `SETUP -> MANAGER` and later configured entry reopened the manager directly.
- App interpretation:
  - The manager-backed setup route and basic real pairing/provisioning path were previously proven on device.
  - Later regressions should therefore be compared against this accepted baseline rather than treated as unexplained greenfield behavior.
- Linked evidence:
  - `docs/reports/tests/L3-20260321-sim-wave5-connectivity-validation.md`
- Next action:
  - When discussing regressions with hardware, always state whether the target badge/firmware still matches the Wave 5 accepted baseline assumptions.

## Communication Notes for Hardware Conversations

- Always include:
  - date
  - app build identifier if known
  - badge identifier if known
  - firmware version if known
  - exact BLE payloads seen
  - exact UUIDs or error strings seen
  - whether the evidence came from pairing, reconnect, network query, time sync, or recording stop
- Prefer literal statements over paraphrase, for example:
  - `tim#get was not seen`
  - `log#20260322_170000 was seen`
  - `6E400003 read failed`
  - `wifi#address#ip#name returned IP#... and SD#... only`
