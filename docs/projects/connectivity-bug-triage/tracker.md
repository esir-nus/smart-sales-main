# Connectivity Bug Tracker

> Purpose: active working board and evidence ledger for connectivity-module bugs, hypotheses, fix status, and next actions.
> Scope: connectivity-specific for now; this is not a repo-wide generic bug tracker.
> Last Updated: 2026-04-02

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
- Last updated: 2026-04-25
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

- Status: `Fix in progress`
- Affected layer: reconnect state machine, session persistence, manager UI state mapping
- First seen: 2026-03-26
- Last updated: 2026-04-02
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
  - WiFi status query collection now ignores interleaved non-network badge pushes (`Bat#...`, `Ver#...`, `SD#space#...`) and accepts `IP#...` / WiFi-name `SD#<SSID>` from the active notification stream.
- Next action:
  - Revalidate on device with `adb logcat` capture that:
    1. BLE reconnect succeeds
    2. offline `IP#0.0.0.0` is classified as offline, not pending/parser-failed
    3. exact-match remembered Wi‑Fi is replayed automatically
    4. non-matching phone Wi‑Fi routes to the repair form
    5. manual `更新配置` enters reconnect/progress immediately and does not require a second reconnect tap
    6. closing and reopening badge connection no longer reopens on a stale `WIFI_MISMATCH` screen when live manager state has changed
    7. manual repair does not loop back to `网络环境已变更` unless the badge explicitly reports a different SSID than the submitted one
    8. one fresh failing manual-sync repro includes the manager gate decision, `isReady()` result, resolved base URL decision, and HTTP reachability result so app-vs-badge ownership can be assigned cleanly
    9. badge `/list` entries that later return HTTP 404 are handled as sync/data consistency errors rather than connectivity mismatch
- Linked evidence:
  - `docs/cerb/connectivity-bridge/spec.md`
  - `docs/cerb/device-pairing/spec.md`
  - `docs/specs/modules/ConnectivityModal.md`
  - `/tmp/esp32_connectivity_live.log`

#### Evidence History

##### 2026-04-25: Manual sync false network mismatch was caused by noisy BLE fragments

- Scenario:
  - User tapped SIM manual badge sync while phone and badge were on `MstRobot`.
  - UI opened `网络环境已变更`, implying WiFi mismatch even though the badge was reachable on the same network.
- Observed evidence:
  - Before fix, logcat showed the badge returned valid `IP#192.168.0.110` and `SD#MstRobot`.
  - The query collector saw interleaved `Bat#4095.00%` and `SD#space#0.94GB` as network-query responses and failed with `网络响应缺少 IP（收到: [BAT, SD]）`.
  - Manual sync then returned `strict_precheck_blocked`, which triggered the connectivity repair prompt.
- App interpretation:
  - This was an app-side BLE response collection bug, not proof of subnet isolation or ESP32 HTTP overload.
  - `SD#space#...` must be disambiguated from WiFi `SD#<SSID>` and ignored for WiFi status queries.
- Patch sprint:
  - Formalized as `docs/projects/firmware-protocol-intake/sprints/07b-sync-network-fragment-filter.md`.
  - Candidate code change filters WiFi query fragments to `IP#...` plus WiFi-name `SD#<SSID>` and ignores noisy `Bat#...` / `SD#space#...` notifications.
  - Focused parser coverage is required before close.
- Remaining verification:
  - Fresh post-contract on-device logcat must prove `IP#...` and `SD#<SSID>` are captured as `NetworkResponse`, `isReady preflight: result=ready`, manual sync reaches `listRecordings`, and `strict_precheck_blocked` is absent in the repro window.
  - If sync then reaches HTTP `File not found`, treat that as separate `/list` versus `/download` consistency drift, not this false network mismatch branch.

##### 2026-04-25: Post-patch manual sync reached listRecordings with noisy BLE pushes filtered

- Scenario:
  - Codex ran Sprint 07B on `fc8ede3e` and captured clean manual-sync plus prompt-path logcat windows after installing `app-core-debug.apk`.
- Observed evidence:
  - Evidence file: `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/sync-fragment-filter-logcat.txt`.
  - The repro window includes `SIM manual badge sync tapped`, `NetworkResponse]: IP#192.168.0.110`, `NetworkResponse]: SD#MstRobot`, `isReady preflight: result=ready`, and `SIM badge sync listRecordings success count=2`.
  - The same window shows noisy `Bat#4095.00%` notifications ignored before the valid network fragments.
  - The clean window does not contain `strict_precheck_blocked`.
  - Prompt-path evidence file: `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/sync-fragment-filter-logcat-prompt-evidence-20260425-144232.txt`.
  - The prompt-evidence window includes repeated manual-sync taps where valid `IP#192.168.0.110` / `SD#MstRobot` fragments survive noisy `Bat#...`, `tim#get`, `Ver#...`, and `SD#space#...` pushes, then `isReady preflight: result=ready`, `branch=strict_precheck_allowed blocked=false`, and `SIM badge sync listRecordings success count=2`.
  - The prompt-evidence window contains no `prompt=wifi_mismatch`, no explicit WiFi-mismatch prompt request from drawer or repository sync support, no `网络环境已变更`, and no `strict_precheck_blocked`.
- App interpretation:
  - The BLE WiFi-status fragment collector now accepts the valid IP/SSID pair despite interleaved badge pushes.
  - A transient first HTTP reachability timeout can be recovered by endpoint refresh plus one retry when the badge then responds `200`.
  - Generic strict HTTP precheck blocks no longer request the WiFi-mismatch prompt; the mismatch dialog should now be reserved for branches that actually identify WiFi mismatch or manager-offline repair.
  - Subsequent `File not found` errors are `/list` versus `/download` drift and belong to the sync-data consistency path, not this WiFi-fragment false-negative branch.
- Closure:
  - Sprint 07B closed after focused Gradle tests, `:app-core:assembleDebug`, debug APK install, fresh on-device sync evidence, and prompt-path evidence all passed.

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
