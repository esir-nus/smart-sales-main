# Connectivity Bug Tracker

> Purpose: active working board and evidence ledger for connectivity-module bugs, hypotheses, fix status, and next actions.
> Scope: connectivity-specific for now; this is not a repo-wide generic bug tracker.
> Last Updated: 2026-04-17

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
- Firmware behavior for the active ESP32 badge: the badge stores one last-working Wi‑Fi credential and auto-reconnects itself; the app still owns the multi-network remembered credential list.

## Current Active Bugs

### OI-04: Onboarding scan surfaced unrelated BLE devices that shared generic UART service signatures

- Status: `Resolved`
- Affected layer: BLE discovery admission, onboarding pairing scan UI
- First seen: 2026-03-31
- Last updated: 2026-03-31
- Current hypothesis:
  - The scan matcher admitted devices when either the advertised name matched or a generic UART/NUS service UUID matched, which let non-badge peripherals appear in the onboarding `发现设备` card.
- Delivered fix:
  - The current production profile now runs onboarding scan admission in trusted-name mode.
  - Only the CHLE badge-name family (`CHLE_Intelligent` plus spacing/case variants) is allowed to surface as a discovered badge.
  - Generic UART/NUS service UUID presence alone no longer makes a device pairable in onboarding.
- Verification target:
  - Focused unit coverage proves a foreign device name is rejected even when it advertises the same UART UUID, while `CHLE Intelligent` still matches.
- Resolution note:
  - Resolved in code and docs on 2026-03-31; hardware revalidation can be captured later if needed, but the regression source was app-side scan filtering logic rather than badge transport.

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
- Last updated: 2026-04-02
- Current hypothesis:
  - The earlier failure was caused by app-side provisioning completion logic expecting an immediate BLE ack and falling back to a direct `6E400003...` read that the target firmware may not support.
- Latest attempted fix:
  - The app now follows the documented two-step `SD#...` -> `PD#...` write path and relies on bounded foreground `wifi#address#ip#name` confirmation instead of an immediate provisioning-status read.
  - Continuous BLE Wi‑Fi polling is no longer part of the normal post-provisioning runtime.
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

- Status: `Blocked`
- Affected layer: reconnect state machine, session persistence, manager UI state mapping
- First seen: 2026-03-26
- Last updated: 2026-04-17
- Current hypothesis:
  - Reconnect is technically succeeding at the BLE layer, but the badge may still be offline or on the wrong SSID even though firmware retains one last-working Wi‑Fi credential.
  - The app therefore still needs deterministic SSID alignment using its own multi-network credential store when the badge is offline or on a different network.
- Latest attempted fix:
  - `SessionStore` now keeps multiple known Wi‑Fi credentials.
  - Reconnect now silently replays the exact remembered credential for the phone's current SSID when the badge reports `0.0.0.0`.
  - If no exact known match exists, the manager routes to `WIFI_MISMATCH` for manual credential repair.
  - Parser handling now accepts `IP#0.0.0.0` as valid offline status.
  - Reconnect now distinguishes `phone not on Wi‑Fi` from `phone Wi‑Fi connected but SSID unreadable`.
  - Reconnect foreground query now retries once after the BLE 2s query-floor timeout instead of failing immediately on throttle.
  - Manual Wi‑Fi repair no longer falls back into generic reconnect after `SD#...` / `PD#...`; it now uses a setup-aligned bounded online-confirmation loop against the submitted SSID.
  - Manual `WIFI_MISMATCH` repair now blocks blank/whitespace SSID or password locally and at the service seam so empty credentials cannot overwrite the badge's stored Wi‑Fi profile.
  - Manual `WIFI_MISMATCH` repair now requires an explicit send-confirm dialog before valid credentials are written to the badge.
  - Badge network status is now passive/event-driven instead of background-polled, and HTTP `/list` / `/download` / `/delete` reuse the active runtime endpoint snapshot instead of re-querying BLE before every call.
  - Connected-vs-ready evidence diagnostics now log the manual-sync gate branch, `ConnectivityBridge.isReady()` preflight, badge network query result, resolved base URL, and HTTP reachability outcome under the existing `SmartSalesConn` / `AudioPipeline` capture tags.
  - `RealConnectivityBridge` no longer uses phone SSID alignment to accept, reuse, or refresh the active badge HTTP endpoint snapshot.
  - Manual `WIFI_MISMATCH` repair now probes `http://<badge-ip>:8088` before declaring success; bounded probe timeout remains the existing `BadgeHttpClient.isReachable()` 3s preflight budget.
  - Manual repair now returns explicit `HTTP_UNREACHABLE` diagnostics when BLE confirmation reaches a usable IP and matching submitted SSID but badge HTTP service still cannot be reached.
  - Runtime shell auto-reconnect no longer calls immediate foreground `reconnect()` through the old compat shim; it now respects `effectiveState` so `WIFI_MISMATCH` and in-flight manual repair are not overwritten by a background reconnect loop.
  - Post-credential readiness proof now uses a graduated `2s -> 3s -> 5s` grace budget before the app concludes that badge HTTP `:8088` is still unreachable.
  - SIM audio AUTO `/list` sync now arms an in-memory known-HTTP-unreachable latch for the current runtime + badge endpoint and suppresses repeated AUTO churn until runtime/IP/readiness evidence changes.
  - The bridge diagnostics log now labels phone SSID as `phoneSsidForDiag=` to make clear that it is operator context only, not an endpoint gate.
- App-side improvement landed (2026-04-18):
  - Pre-sync isolation gate: MANUAL sync preflight failure now checks `isValidated + lastKnownIp` before falling back to WiFi-mismatch prompt. If isolation is suspected, `ConnectivityModal` shows the isolation card (`PRE_SYNC` context) and sync is blocked cleanly — no upstream batch failure, no misleading WiFi-mismatch prompt.
  - On-disconnect probe: `RealConnectivityBridge` now runs a deferred 1s HTTP probe when badge drops from `WifiProvisioned|Syncing` to `Disconnected`; suspected isolation surfaces immediately as `ON_DISCONNECT` card.
  - These changes address the upstream batch-failure UX problem. The underlying `MotionTime1` HTTP unreachability remains firmware/AP-isolation ownership.
- Next action:
  - Treat `MotionTime1` `:8088` failure as a firmware / AP-isolation / badge
    HTTP-service investigation unless new app evidence appears.
  - If hardware follow-up is needed, compare badge-side HTTP listener behavior
    on `MotionTime1` against the same APK + device baseline that reached
    `http://192.168.0.108:8088` on `MstRobot`.
  - Keep the current app telemetry in place for any future rerun; this L3 did
    not indicate another app-side reconnect or AUTO-churn fix is needed.
- Linked evidence:
  - `docs/cerb/connectivity-bridge/spec.md`
  - `docs/cerb/device-pairing/spec.md`
  - `docs/specs/modules/ConnectivityModal.md`
  - `docs/reports/tests/L3-20260417-connectivity-wifi-mismatch-manual-repair-loop.md`
  - `docs/reports/tests/L3-20260417-connectivity-oi03-rerun-after-churn-suppression.md`
  - `/tmp/esp32_connectivity_live.log`
  - `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log`
  - `/tmp/l3-20260417-oi03-rerun.log`

#### Evidence History

##### 2026-04-17: Rerun validated the app-side churn suppression slice, but `MotionTime1` still failed badge HTTP

- Scenario:
  - Fresh on-device L3 rerun after the manual-repair copy normalization,
    graduated `2s -> 3s -> 5s` readiness grace budget, `phoneSsidForDiag=`
    log rename, and SIM audio AUTO known-HTTP-unreachable latch shipped.
- Observed evidence:
  - Manual repair on `MotionTime1` reached `IP#192.168.1.18`, spent the full
    grace budget, and ended with `🛜 repair outcome=http_unreachable`.
  - The follow-up sync preflight stayed on
    `🔎 isReady preflight: result=not-ready baseUrl=http://192.168.1.18:8088`;
    this rerun did not capture the old `ReconnectResult.Connected` overwrite
    symptom after manual failure.
  - Manual repair on `MstRobot` later reached
    `BadgeHttpClient.isReachable response ... code=200 reachable=true`,
    `🛜 repair outcome=wifi_provisioned`, and subsequent
    `🔎 isReady preflight: result=ready baseUrl=http://192.168.0.108:8088`.
  - After switching back to `MotionTime1`, the app refreshed
    `http://192.168.1.18:8088`, BLE ingress stayed active, repeated
    `/download` attempts still failed with `Failed to connect to
    /192.168.1.18:8088`, and the new
    `SIM badge auto sync suppressed ... reason=known_http_unreachable`
    telemetry fired repeatedly for the same runtime + endpoint.
- App interpretation:
  - The app-side stale-IP hypothesis is now closed by stronger evidence than the
    prior report, because the same build and phone reached badge HTTP `200` on
    one network.
  - The reconnect-overwrite symptom was not observed in this rerun, and AUTO
    churn suppression behaved as designed.
  - Remaining ownership is now network-specific to `MotionTime1` or the badge
    HTTP service on that network, rather than phone-SSID endpoint gating or app
    endpoint staleness.
- Linked evidence:
  - `docs/reports/tests/L3-20260417-connectivity-oi03-rerun-after-churn-suppression.md`
  - `/tmp/l3-20260417-oi03-rerun.log`

##### 2026-04-17: OI-03 app-side churn suppression slice landed

- Scenario:
  - Code/doc sync slice after the 2026-04-17 device captures confirmed stale badge IP was no longer the app-side problem but same-network `:8088` reachability still failed.
- Observed evidence:
  - Manual repair failure copy is now normalized through human-readable Chinese mapping instead of leaking raw `WifiDisconnected(...)` text.
  - Manual repair confirmation and reconnect-side first HTTP readiness probe now share the graduated `2s -> 3s -> 5s` post-credential grace budget.
  - SIM audio AUTO `/list` now suppresses repeated retries against a known-unreachable runtime + endpoint and logs latch arm/suppress/clear lines for capture correlation.
  - Bridge diagnostics now log `phoneSsidForDiag=` instead of the ambiguous `phoneSsid=`.
- App interpretation:
  - App-side IP staleness and phone-SSID endpoint gating remain closed for this slice.
  - If the badge still reaches BLE online state but `:8088` stays unreachable after the bounded grace budget, remaining ownership is firmware / HTTP service / AP isolation rather than sync-loop churn in the app.
- Linked evidence:
  - `docs/cerb/connectivity-bridge/spec.md`
  - `docs/cerb/audio-management/spec.md`

##### 2026-04-17: Manual repair refreshed the badge IP, but sync still failed on the new endpoint

- Scenario:
  - Physical-device L3 run of the new manual-sync to `WIFI_MISMATCH` handoff and
    manual Wi-Fi repair flow using two submitted credential sets.
- Observed evidence:
  - The app blocked manual sync first at `http://192.168.1.18:8088` after
    `isReady()` reachability failure.
  - Repair with old credentials (`MstRobot`) wrote `SD#...` / `PD#...`, then
    confirmed the badge online at `192.168.0.109`.
  - The app refreshed to `http://192.168.0.109:8088`, but the next sync still
    failed from phone address `192.168.1.35`, while the log recorded
    `phoneSsid=` as blank.
  - Repair with new credentials (`MotionTime1`) then wrote `SD#...` / `PD#...`,
    confirmed the badge online at `192.168.1.18`, and refreshed
    `http://192.168.1.18:8088`.
  - Sync and later `rec#` auto-download still failed to connect to
    `192.168.1.18:8088` even though BLE recording-ready ingress remained alive.
- App interpretation:
  - The app is not stuck on a stale pre-repair IP.
  - The remaining drift is twofold:
    1. unreadable phone SSID still weakens endpoint-alignment protection enough
       to allow false-positive repair success on a different network
    2. even after the badge moves onto the intended network, badge HTTP
       readiness is still not proven by the current manual-repair success
       contract
- Linked evidence:
  - `docs/reports/tests/L3-20260417-connectivity-wifi-mismatch-manual-repair-loop.md`
  - `/tmp/l3-20260417-connectivity-wifi-mismatch-loop.log`

##### 2026-04-17: HTTP proof failure was still being overwritten by shell auto-reconnect

- Scenario:
  - Live on-device monitoring against debug build
    `0.1.0-prism-debug.26107.1735.92cac1d` while reproducing the
    `WIFI_MISMATCH` manual repair flow with `MstRobot`.
- Observed evidence:
  - Manual repair wrote `SD#MstRobot` and `PD#...`, then ran three bounded
    HTTP probes against `http://192.168.0.108:8088`.
  - All three probes failed from phone address `192.168.1.35`, ending with
    `🛜 repair outcome=http_unreachable`.
  - During the same window, `ConnectivityViewModel.reconnect()` was triggered
    again from the runtime shell auto-reconnect collector and
    `ConnectivityService.reconnect()` returned `ReconnectResult.Connected`,
    clearing the modal override back to connected.
- App interpretation:
  - The shipped HTTP proof contract is active and correctly classifies the
    repair as failed.
  - The remaining false-success symptom is caused by an app-side shell
    auto-reconnect loop overriding the repair result, not by a stale APK.
- Linked evidence:
  - `/tmp/connectivity-debug-20260417-173857/logcat-app.log`
  - `/tmp/connectivity-debug-20260417-173857/logcat-system-focus.log`

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

##### 2026-03-27: SIM manager retained stale Wi‑Fi mismatch override across close/reopen

- Scenario:
  - User entered manual Wi‑Fi credentials from the SIM connectivity repair form, then closed and reopened badge connection after the panel appeared stuck.
- Observed evidence:
  - `ConnectivityViewModel` kept `WIFI_MISMATCH` as a transient UI override independent of live `managerStatus`.
  - SIM shell reused the same `ConnectivityViewModel` instance across connectivity-surface close/reopen.
  - Closing the SIM connectivity surface previously cleared only the shell route and did not clear or cancel transient reconnect/repair state.
- App interpretation:
  - The stuck repair screen was a real app-side state-retention bug, not only a hardware/network failure.
  - The fix now clears transient reconnect/mismatch override state on close and starts reconnect/progress immediately when the user submits manual Wi‑Fi repair credentials.

##### 2026-03-27: Manual repair path had drifted from setup by reusing generic reconnect

- Scenario:
  - After the stale-override fix, manual Wi‑Fi repair still looped back to `网络环境已变更` even after the user submitted credentials.
- Observed evidence:
  - Protocol code still used the correct two-step `SD#<ssid>` then `PD#<password>` write path.
  - But `RealConnectivityService.updateWifiConfig()` immediately reused generic `reconnect()`.
  - Setup/onboarding does not validate post-provision success this way; it waits for the badge to come online through a bounded query loop.
  - Generic reconnect also reintroduces phone-SSID-dependent mismatch logic that is inappropriate once the user has explicitly supplied the target SSID/password.
- App interpretation:
  - The remaining loop was likely service-flow drift rather than a fresh protocol-write bug.
  - The repair path now confirms online state against the submitted SSID with setup-aligned bounded polling and only returns to `WIFI_MISMATCH` for a proven SSID mismatch.

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
