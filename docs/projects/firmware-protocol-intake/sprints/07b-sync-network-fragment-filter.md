# Sprint 07B — sync-network-fragment-filter

## Header

- Project: firmware-protocol-intake
- Sprint: 07B
- Slug: sync-network-fragment-filter
- Date authored: 2026-04-25
- Author: Codex (user-requested patch contract; default project author remains Claude for new planned sprints)
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none
- Ship path: one commit on `develop` at sprint close containing the patch, synced docs, evidence artifacts, and this contract closeout.
- Blocked-by: fresh on-device evidence is still required before closure

## Demand

**User ask:** "not yet, make it a spring contract for fixign this isse. as a patch sprint under firmware-protocal-intake and we reenter this spring tasks formerly"

**Interpretation:** Reopen the sync false-negative work as a formal patch sprint under `firmware-protocol-intake` instead of treating it as an ad-hoc addendum to Sprint 07. The bug is that tapping manual sync can show `网络环境已变更` even when phone and badge are on the same WiFi, because noisy BLE notifications may obscure the valid WiFi status response. The patch is not accepted until a fresh on-device capture proves the sync gate no longer fails as `strict_precheck_blocked` for that branch.

## Scope

**In scope:**

1. BLE WiFi status query parsing and collection:
   - `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewayProtocolSupport.kt`
   - `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewaySessionSupport.kt`

2. Focused parser/fragment tests:
   - `app-core/src/test/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewayNotificationParsingTest.kt`

3. Owning docs and tracker sync:
   - `docs/specs/esp32-protocol.md`
   - `docs/projects/connectivity-bug-triage/tracker.md`
   - `docs/projects/firmware-protocol-intake/tracker.md`
   - this contract file

4. Evidence artifacts:
   - `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/`

**Out of scope:**

- UI redesign of the `网络环境已变更` dialog
- Multi-device source label work from Sprint 08
- Retry queue / empty-file / friendly card polish from Sprint 09
- Fixing badge `/list` entries that later return HTTP 404, except to record it as separate evidence if observed
- Harmony-native changes

## References

1. `docs/specs/sprint-contract.md` — contract schema and evidence rules
2. `docs/projects/firmware-protocol-intake/tracker.md` — project context and sprint queue
3. `docs/specs/esp32-protocol.md` — WiFi query, `SD#space`, and BLE protocol source of truth
4. `docs/cerb/connectivity-bridge/spec.md` — reconnect/readiness and HTTP-preflight contract
5. `docs/cerb/connectivity-bridge/interface.md` — `ConnectivityBridge.isReady()` and `listRecordings()` contract
6. `docs/projects/connectivity-bug-triage/tracker.md` — active connectivity evidence ledger
7. `.agent/rules/lessons-learned.md` and `docs/reference/agent-lessons-details.md` — triggers: `HTTP Gate Conflating Connection Concerns`, `Reconnect Race Condition`, `Application-Level Coupling`
8. Code paths:
   - `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewaySessionSupport.kt`
   - `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewayProtocolSupport.kt`
   - `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`
   - `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`

## Success Exit Criteria

- `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.data.connectivity.legacy.gateway.GattBleGatewayNotificationParsingTest'` exits 0.
- `./gradlew :app-core:assembleDebug` exits 0.
- `adb -s fc8ede3e install -r app-core/build/outputs/apk/debug/app-core-debug.apk` returns `Success`.
- `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/sync-fragment-filter-logcat.txt` exists.
- The evidence log contains `SIM manual badge sync tapped` or `SIM badge sync preflight start`.
- The evidence log contains both `NetworkResponse]: IP#` and WiFi-name `NetworkResponse]: SD#` for the same repro window.
- The evidence log contains `isReady preflight: result=ready` after those fragments.
- The evidence log contains `SIM badge sync listRecordings success`.
- In the post-patch repro window, the evidence log does not contain `strict_precheck_blocked` after the manual sync tap.
- If the evidence reaches `File not found`, it is recorded as separate `/list` versus `/download` drift and does not count as this sprint failure unless it re-triggers `网络环境已变更`.

## Stop Exit Criteria

- `adb devices` does not show `fc8ede3e` as `device`.
- The patch build cannot install on the device.
- Manual sync still shows `网络环境已变更` and logcat shows valid `IP#...` / `SD#<SSID>` fragments were captured as `NetworkResponse`.
- The collector still logs `网络响应缺少 IP` while the same window contains valid `IP#...`.
- Focused tests or `:app-core:assembleDebug` fail and the failure is not resolved within the iteration bound.
- Iteration bound is hit without fresh on-device evidence.

## Iteration Bound

- Max 3 iterations or 60 minutes wall-clock, whichever hits first.
- One iteration = patch/build/install -> live logcat capture -> user/manual sync repro -> evidence save -> evaluator decision.

## Required Evidence Format

At close, operator appends:

1. **Build/test evidence**
   - Focused gateway test command and result.
   - `:app-core:assembleDebug` result.
   - `adb install` result.

2. **On-device evidence**
   - Exact `adb logcat` command.
   - Evidence file path under `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/`.
   - Grep excerpts proving `IP#...`, `SD#<SSID>`, `isReady preflight: result=ready`, and `SIM badge sync listRecordings success`.
   - Grep proof that `strict_precheck_blocked` is absent in the post-patch repro window.

3. **Residual issue note**
   - Whether sync completed, queued downloads, or reached a separate `/list` versus `/download` failure such as HTTP 404.

## Iteration Ledger

<!-- Operator appends one entry per iteration below this line -->

- **Iteration 1 — 2026-04-25 Codex**
  - Patch reviewed and tightened: WiFi status collection now filters notification payloads through `toNetworkStatusFragment()` and continues ignoring non-network pushes without shortening the remaining collection window after `IP#...`.
  - Focused parser coverage extended for noisy fragments and reverse-order `SD#...` / `IP#...` fragments.
  - `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.data.connectivity.legacy.gateway.GattBleGatewayNotificationParsingTest'` did not reach the focused test class because `:app-core:compileDebugUnitTestKotlin` fails in unrelated audio tests with missing `deviceRegistryManager` constructor arguments.
  - `./gradlew :app-core:assembleDebug` passed.
  - `adb -s fc8ede3e install -r app-core/build/outputs/apk/debug/app-core-debug.apk` returned `Success`.
  - On-device logcat capture produced the required clean manual-sync evidence window in `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/sync-fragment-filter-logcat.txt`; full raw captures are preserved beside it.
  - Evaluator decision: code/build/device behavior for this patch is green, but sprint closure is blocked by the required unit-test command failing in out-of-scope dirty test files.

- **Iteration 2 — 2026-04-25 Codex**
  - Re-entered after the out-of-scope app-core unit-test compile blocker was cleared.
  - `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.data.connectivity.legacy.gateway.GattBleGatewayNotificationParsingTest'` passed.
  - `./gradlew :app-core:assembleDebug` passed.
  - `adb -s fc8ede3e install -r app-core/build/outputs/apk/debug/app-core-debug.apk` returned `Success`.
  - Fresh logcat capture produced a clean manual-sync window at `14:25:00` in `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/sync-fragment-filter-logcat.txt`.
  - The clean window includes noisy `Bat#4095.00%` notifications ignored, valid `IP#192.168.0.110` and `SD#MstRobot` as `NetworkResponse`, `isReady preflight: result=ready`, `branch=strict_precheck_allowed blocked=false`, and `SIM badge sync listRecordings success count=2`.
  - Evaluator decision: sprint success criteria are met. Residual `File not found` remains separate `/list` versus `/download` drift.

- **Iteration 3 — 2026-04-25 Codex**
  - Re-entered after user rejected the earlier success closeout because the observed UI still needed prompt-path evidence.
  - Added narrow prompt telemetry around manual-sync WiFi-mismatch prompt emitters and the shared `ConnectivityPromptCoordinator`, using existing `AudioPipeline` and `SmartSalesConn` tags so the evidence window can prove prompt behavior directly.
  - Added one-readiness retry after a failed HTTP reachability check: the bridge invalidates the active endpoint, forces a fresh BLE network-status resolution, then retries HTTP once before returning not-ready.
  - Updated strict manual-sync gate behavior so `strict_precheck_blocked` no longer routes to the WiFi-mismatch prompt; only the manager-offline branch requests the mismatch prompt from the SIM drawer gate.
  - `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.data.connectivity.RealConnectivityBridgeTest' --tests 'com.smartsales.prism.data.connectivity.legacy.gateway.GattBleGatewayNotificationParsingTest' --tests 'com.smartsales.prism.ui.sim.SimBadgeSyncAvailabilityTest'` passed.
  - `./gradlew :app-core:assembleDebug` passed.
  - `adb -s fc8ede3e install -r app-core/build/outputs/apk/debug/app-core-debug.apk` returned `Success`.
  - Fresh prompt-evidence logcat capture at `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/sync-fragment-filter-logcat-prompt-evidence-20260425-144232.txt` contains repeated manual-sync windows with valid `IP#192.168.0.110` / `SD#MstRobot`, noisy fragments ignored, HTTP retry followed by `code=200`, `isReady preflight: result=ready`, `branch=strict_precheck_allowed blocked=false`, and `SIM badge sync listRecordings success count=2`.
  - Absence proof: the fresh prompt-evidence log contains no `prompt=wifi_mismatch`, no `SIM manual badge sync prompt wifi_mismatch requested`, no `SIM badge sync prompt wifi_mismatch requested`, no `网络环境已变更`, and no `strict_precheck_blocked`.
  - Supporting UI hierarchy snapshot at `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/ui-hierarchy-after-prompt-evidence-20260425-1445.xml` shows the recording list with synced/error cards, not the `网络环境已变更` dialog.
  - Evaluator decision: sprint success criteria are met after prompt-path evidence. Residual `File not found` remains separate `/list` versus `/download` drift.

## Closeout

<!-- Operator fills at sprint exit -->
- **Status:** success
- **Summary for project tracker:** Shipped WiFi-status fragment filtering plus prompt-path hardening so noisy BLE pushes no longer hide valid `IP#` / WiFi `SD#<SSID>` fragments and strict HTTP precheck blocks no longer masquerade as WiFi mismatch; focused tests, assemble, install, and fresh on-device prompt evidence are green.
- **Evidence artifacts:**
  - Focused test command: `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.data.connectivity.RealConnectivityBridgeTest' --tests 'com.smartsales.prism.data.connectivity.legacy.gateway.GattBleGatewayNotificationParsingTest' --tests 'com.smartsales.prism.ui.sim.SimBadgeSyncAvailabilityTest'`
    - Result: `BUILD SUCCESSFUL`.
  - Build command: `./gradlew :app-core:assembleDebug`
    - Result: `BUILD SUCCESSFUL`.
  - Install command: `adb -s fc8ede3e install -r app-core/build/outputs/apk/debug/app-core-debug.apk`
    - Result: `Success`.
  - Logcat command: `adb -s fc8ede3e logcat -v time -s SmartSalesConn:V AudioPipeline:V`
  - Prompt-evidence window: `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/sync-fragment-filter-logcat-prompt-evidence-20260425-144232.txt`
    - Contains repeated `SIM manual badge sync tapped` windows.
    - Contains `NetworkResponse]: IP#192.168.0.110` and `NetworkResponse]: SD#MstRobot`.
    - Contains ignored noisy `Bat#4095.00%`, `tim#get`, `Ver#1.0.0.1`, and `SD#space#0.94GB` fragments.
    - Contains `isReady preflight: retrying after endpoint refresh` followed by HTTP `code=200 reachable=true`.
    - Contains `isReady preflight: result=ready`.
    - Contains `SIM manual badge sync gate availability=ready branch=strict_precheck_allowed blocked=false`.
    - Contains `SIM badge sync listRecordings success count=2`.
    - Does not contain `prompt=wifi_mismatch`, `SIM manual badge sync prompt wifi_mismatch requested`, `SIM badge sync prompt wifi_mismatch requested`, `网络环境已变更`, or `strict_precheck_blocked`.
  - UI hierarchy snapshot: `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/ui-hierarchy-after-prompt-evidence-20260425-1445.xml`
    - Shows the recording list and badge-sync state after the repro window, not the `网络环境已变更` dialog.
  - Clean evidence window: `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/sync-fragment-filter-logcat.txt`
    - Contains `SIM manual badge sync tapped`.
    - Contains `isReady preflight: result=ready`.
    - Contains `SIM badge sync listRecordings success count=2`.
    - Contains `NetworkResponse]: IP#192.168.0.110` and `NetworkResponse]: SD#MstRobot`.
    - Contains ignored noisy `Bat#4095.00%` fragments.
    - Contains `SIM manual badge sync gate availability=ready branch=strict_precheck_allowed blocked=false`.
    - Does not contain `strict_precheck_blocked` in the clean evidence window.
    - Reaches `File not found` for badge `/list` entries during background download; recorded as separate `/list` versus `/download` drift, not this sprint's network-fragment failure.
  - Raw captures preserved:
    - `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/sync-fragment-filter-logcat-rerun-20260425-142325.txt`
    - `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/sync-fragment-filter-logcat-full-raw.txt`
    - `docs/projects/firmware-protocol-intake/evidence/07b-sync-network-fragment-filter/sync-fragment-filter-logcat-mixed.txt`
- **Lesson proposals:** none pending; this is an instance of existing connectivity evidence discipline.
- **CHANGELOG line:** pending user approval; suggested line if accepted: `Fixed manual badge sync WiFi status parsing so noisy BLE pushes no longer hide valid IP/SSID fragments.`
