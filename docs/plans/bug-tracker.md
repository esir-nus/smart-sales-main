# Connectivity Bug Tracker

> Purpose: active working board and evidence ledger for connectivity-module bugs, hypotheses, fix status, and next actions.
> Scope: connectivity-specific for now; this is not a repo-wide generic bug tracker.
> Last Updated: 2026-03-27

## Operating Rules

- Use this file as the source of truth for active and recently resolved connectivity bug state.
- Keep one entry per active or recently resolved connectivity bug.
- Record meaningful dated repro evidence directly in the owning bug entry instead of maintaining a separate connectivity log.
- For any connectivity-module troubleshooting, review, or fix validation, capture `adb logcat` evidence first via `scripts/esp32_connectivity_debug.sh`; screenshots alone are not sufficient proof.
- Link deeper reports instead of copying raw captures here.
- Only add a lesson after the user confirms the problem is fixed; when that happens, update both:
  - `.agent/rules/lessons-learned.md`
  - `docs/reference/agent-lessons-details.md`

## Primary References

- Protocol SOT: `docs/specs/esp32-protocol.md`
- Connectivity shard: `docs/cerb/connectivity-bridge/spec.md`
- Pairing shard: `docs/cerb/device-pairing/spec.md`
- Live-capture SOP: `docs/sops/esp32-connectivity-debug.md`
- Current Wave 10 tracker: `docs/plans/sim-tracker.md`

## Status Model

- `Open`
- `Investigating`
- `Fix in progress`
- `Blocked`
- `Resolved`
- `Lesson extracted`

## Current Stable Facts

- BLE recording ingress is expected to arrive as `log#YYYYMMDD_HHMMSS`.
- The app-side downloadable filename contract is `log_YYYYMMDD_HHMMSS.wav`.
- Badge Wi-Fi status query uses `wifi#address#ip#name` and may return fragmented `IP#...` plus `SD#...` notifications.
- `IP#0.0.0.0` is a valid badge-offline result, not a parser error.
- Time sync uses badge `tim#get` and app response `tim#YYYYMMDDHHMMSS`.
- Wave 10 already hardened connection-truth reporting so BLE-only connection success should not be treated as fully healthy transport when persistent notification listening is absent.
- Firmware team confirmed the badge never stores Wi‑Fi credentials; deterministic reconnect must therefore replay app-stored credentials when possible.

## Current Active Bugs

### OI-01: Real badge recording-end ingress still unproven on device

- Status: `Investigating`
- Affected layer: BLE notification ingress, connectivity bridge, audio pipeline handoff
- First seen: 2026-03-22
- Last updated: 2026-03-26
- Current hypothesis:
  - BLE traffic exists, but real `log#YYYYMMDD_HHMMSS` recording-end ingress has still not been captured in the app pipeline on the blocked repro path.
- Latest attempted fix:
  - Wave 10 hardened parser ingress, connection-truth gating, and the formal live-capture workflow, but real-device re-entry proof is still missing.
- Next action:
  - Run a fresh device capture with `scripts/esp32_connectivity_debug.sh`, then confirm whether one real badge recording shows connectivity ingress plus `AudioPipeline` recording detection.
- Linked evidence:
  - `docs/sops/esp32-connectivity-debug.md`
  - `docs/reports/20260322-esp32-live-capture-findings.md`
  - `docs/reports/20260322-esp32-connectivity-hardening-notes.md`
  - `docs/plans/sim-tracker.md`
- Hardware-team question:
  - On the failing badge/firmware build, when recording stops, is `log#YYYYMMDD_HHMMSS` always emitted over the same BLE notification path used for `tim#get` and Wi-Fi query fragments?
- Resolution note:
  - Open

#### Evidence History

##### 2026-03-26: Connectivity manager now separates BLE-held vs transport-ready UI

- Scenario:
  - Follow-up fix after physical-badge reconnects appeared healthy at the BLE layer while the manager still showed a generic offline state.
- Observed evidence:
  - Shared `connectionState` already kept the strict contract: only transport-ready BLE + network counts as connected.
  - Legacy `BadgeStateMonitor` already exposed richer manager-facing truth (`PAIRED`, `OFFLINE`, `CONNECTED`) but the UI did not consume it.
  - The connectivity manager/modal now reads a dedicated `managerStatus` bridge flow so it can show:
    - BLE paired, network status pending
    - BLE paired, network offline
- App interpretation:
  - This is a UI observability fix, not a transport-contract rewrite.
  - SIM shell/history/global routing still use the stricter shared connection state.
- Linked evidence:
  - `docs/cerb/connectivity-bridge/interface.md`
  - `docs/specs/modules/ConnectivityModal.md`
- Next action:
  - Validate on device that the manager no longer falsely implies full disconnect when BLE is still occupied by the app.

##### 2026-03-26: Reconnect path was not feeding manager BLE/Wi‑Fi diagnostics

- Scenario:
  - Follow-up review after device screenshots still showed `正在重新连接...` and then fell back to generic `设备离线`.
- Observed evidence:
  - The reconnect flow established/attempted persistent GATT and queried network status directly.
  - But the reconnect path did not publish that BLE-held / offline-query result into `BadgeStateMonitor`.
  - Manager UI therefore lost the richer diagnostic state once the temporary reconnect spinner override cleared.
- App interpretation:
  - This was reconnect-path state drift, not proof that the earlier manager-state UI mapping was wrong.
  - The fix is to update the monitor synchronously from reconnect success/failure query results instead of waiting for the next polling cycle.
- Next action:
  - Revalidate on device that a BLE-held but network-offline reconnect now stays on the manager-specific diagnostic state instead of collapsing to generic offline.

##### 2026-03-22: Live capture showed BLE traffic but no recording-end ingress

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

##### 2026-03-22: Wave 10 hardening aligned connection truth and filename docs/code

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

##### 2026-03-21: SIM Wave 5 physical-badge connectivity flow accepted

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

### OI-02: Pairing/provisioning path likely assumed a BLE ack that the protocol does not guarantee

- Status: `Fix in progress`
- Affected layer: onboarding pairing, provisioning, NUS notification/status handling
- First seen: 2026-03-26
- Last updated: 2026-03-26
- Current hypothesis:
  - The earlier failure was caused by app-side provisioning completion logic expecting an immediate BLE ack and falling back to a direct `6E400003...` read that the target firmware may not support.
- Latest attempted fix:
  - The app now follows the documented two-step `SD#...` -> `PD#...` write path and relies on later `wifi#address#ip#name` polling instead of an immediate provisioning-status read.
- Next action:
  - Re-run physical-badge pairing/provisioning and capture whether the path now succeeds without the `读取特征失败: 6e400003...` signature.
- Linked evidence:
  - `docs/specs/esp32-protocol.md`
  - `docs/cerb/connectivity-bridge/spec.md`
  - `docs/plans/sim-tracker.md`
- Hardware-team question:
  - After receiving `SD#<ssid>` and `PD#<password>`, does firmware emit any immediate BLE success/failure payload, or should the app rely only on subsequent `wifi#address#ip#name` polling?
- Resolution note:
  - App-side mitigation shipped; device revalidation still required before closure.

### OI-03: BLE reconnect succeeds, but badge stays offline until Wi‑Fi credentials are replayed

- Status: `Fix in progress`
- Affected layer: reconnect state machine, session persistence, manager UI state mapping
- First seen: 2026-03-26
- Last updated: 2026-03-26
- Current hypothesis:
  - Reconnect is technically succeeding at the BLE layer, but the badge legitimately returns `IP#0.0.0.0` because firmware never stores Wi‑Fi credentials.
  - The app therefore needs deterministic credential replay rather than assuming badge-side Wi‑Fi memory.
- Latest attempted fix:
  - `SessionStore` now keeps multiple known Wi‑Fi credentials.
  - Reconnect now silently replays the exact remembered credential for the phone's current SSID when the badge reports `0.0.0.0`.
  - If no exact known match exists, the manager routes to `WIFI_MISMATCH` for manual credential repair.
  - Parser handling now accepts `IP#0.0.0.0` as valid offline status.
  - Reconnect now distinguishes `phone not on Wi‑Fi` from `phone Wi‑Fi connected but SSID unreadable`.
  - Reconnect foreground query now retries once after the BLE 2s query-floor timeout instead of failing immediately on throttle.
- Next action:
  - Revalidate on device with `adb logcat` capture that:
    1. BLE reconnect succeeds
    2. offline `IP#0.0.0.0` is classified as offline, not pending/parser-failed
    3. exact-match remembered Wi‑Fi is replayed automatically
    4. non-matching phone Wi‑Fi routes to the repair form
- Linked evidence:
  - `docs/cerb/connectivity-bridge/spec.md`
  - `docs/cerb/device-pairing/spec.md`
  - `docs/specs/modules/ConnectivityModal.md`
  - `/tmp/esp32_connectivity_live.log`

#### Evidence History

##### 2026-03-26: Live capture proved BLE reconnect and badge-offline split

- Scenario:
  - On-device reconnect, disconnect/reconnect, and sync attempts were captured through `adb logcat`.
- Observed evidence:
  - BLE reconnect completed and time-sync traffic remained active.
  - Badge Wi‑Fi query returned `IP#0.0.0.0` and `SD#N/A`.
  - App logs then failed sync preflight because the badge had no usable IP.
- App interpretation:
  - Sync failure was not a fake-disconnected UI only.
  - The real remaining gap was deterministic Wi‑Fi credential replay plus correct offline classification.

##### 2026-03-27: Reconnect replay was skipped by app-side phone Wi‑Fi detection

- Scenario:
  - User performed disconnect/reconnect and captured `adb logcat`.
- Observed evidence:
  - BLE reconnect succeeded and `tim#get` / time sync traffic still worked.
  - Badge repeatedly returned `IP#0.0.0.0` and `SD#N/A`.
  - App then logged `replay skipped: phone Wi‑Fi unavailable` even while Android system connectivity logs still showed Wi‑Fi connected.
  - One reconnect attempt also hit the BLE query floor and failed on `THROTTLED (... < 2000ms floor)`.
- App interpretation:
  - The badge remained truly offline at the Wi‑Fi layer.
  - But reconnect also had an app-side gate bug: phone Wi‑Fi detection was too weak, and reconnect could false-fail on the BLE query floor before recovery logic settled.

#### Evidence History

##### 2026-03-26: App-side Wi-Fi connect path aligned to two-step write protocol

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

##### 2026-03-26: Pairing failed with `读取特征失败` on `6E400003`

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
