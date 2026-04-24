# Sprint 01 — bat-listener-wiring

## Header

- Project: firmware-protocol-intake
- Sprint: 01
- Slug: bat-listener-wiring
- Date authored: 2026-04-24
- Author: Claude
- Operator: **Codex** (default operator per project tracker)
- Lane: `develop` (Android-side wiring only; Harmony-native port is a separate future sprint under `platform/harmony`)
- Branch: `develop` (direct-to-develop per project Cross-Sprint Decisions; no feature branch required)
- Worktree: none (operator may use main tree; escape to a worktree only if parallel work demands it)
- Ship path: one commit on `develop` at sprint close containing code + docs + contract closeout

## Demand

**User ask (verbatim from project tracker sprint 01 row):** "Wire `Bat#<0..100>` BLE listener into `ConnectivityBridge` and replace provisional `ConnectivityViewModel.batteryLevel` source per `docs/specs/esp32-protocol.md` §9"

**Interpretation:** Follow the existing `log#` / `rec#` listener pattern. Add `Bat#<percent>` parsing to the gateway protocol parser, extend the `BadgeNotification` sealed type, expose a `batteryNotifications(): Flow<Int>` seam on `ConnectivityBridge`, and replace the hardcoded `MutableStateFlow(85)` at `ConnectivityViewModel.kt:73-75` with real collection from that seam. Retire the "real bridge-backed battery sourcing remains explicit follow-up debt" note at `docs/cerb/interface-map.md:147`. No UI changes — the dynamic island / connectivity modal already consume `ConnectivityViewModel.batteryLevel`; flipping the source is enough.

## Scope

**In scope** (file-level list; all paths rooted at repo root):

1. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewayProtocolSupport.kt` — add parser case for `Bat#<0..100>` in `parseBadgeNotificationPayload()` near the existing `log#` / `rec#` cases (~line 102-116).
2. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/BleGateway.kt` — add `BadgeNotification.BatteryLevel(percent: Int)` variant to the sealed class at line 89 (same file as GattBleGateway interface declarations).
3. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerIngressSupport.kt` — add ingress case emitting to a new `batteryEvents` SharedFlow<Int>, mirroring the `RecordingReady` / `AudioRecordingReady` branches at lines 25-34.
4. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt` — add `batteryEvents: SharedFlow<Int>` (or `Flow<Int>`) to the interface and implementation.
5. `data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/ConnectivityBridge.kt` — add `fun batteryNotifications(): Flow<Int>` to the interface (near the existing `recordingNotifications()` / `audioRecordingNotifications()` at lines 54-70).
6. `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt` — implement `batteryNotifications()` by bridging from `DeviceConnectionManager.batteryEvents`, mirroring the init-block pattern at lines 75-82 / 218-222.
7. `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt` — replace `MutableStateFlow(85)` at lines 73-75 with a `StateFlow<Int>` sourced from `connectivityBridge.batteryNotifications()`. Initial value: `85` — preserves the current UX exactly (the existing `MutableStateFlow(85)` seed was historically user-visible during the window before any push). "No reading yet" (`Int?` / `hasBatteryReading` flag) semantics is explicitly a future UI sprint; scope this sprint to source replacement only. The success-criterion grep for `MutableStateFlow(85)` returning `0` is still met because the replacement is `stateIn(scope, SharingStarted.Eagerly, 85)` (or equivalent), not a `MutableStateFlow(85)` mock.
8. Unit tests — add parametrized cases:
   - `app-core/src/test/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewayNotificationParsingTest.kt` — add cases for valid `Bat#50`, boundary `Bat#0` / `Bat#100`, invalid `Bat#-1` / `Bat#101` / `Bat#abc` / `Bat#` (empty).
   - `app-core/src/test/java/com/smartsales/prism/data/connectivity/legacy/DefaultDeviceConnectionManagerIngressTest.kt` — one case feeding `BadgeNotification.BatteryLevel(42)` through the fake gateway and asserting `batteryEvents` emits `42`.
   - `app-core/src/test/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModelTest.kt` — one case asserting `batteryLevel` StateFlow updates when a fake `ConnectivityBridge` emits a battery value.
9. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/FakeDeviceConnectionManager.kt` — implement the new `batteryEvents` flow on the fake (empty / no-op emit is acceptable).
10. `app-core/src/test/java/com/smartsales/prism/data/audio/RealBadgeAudioPipelineIngressTest.kt` — extend the private `FakeConnectivityBridge` class to implement the new `batteryNotifications()` method (`emptyFlow()` is sufficient).
11. `app-core/src/test/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModelRepairTest.kt` — extend the anonymous `ConnectivityBridge` impl to implement `batteryNotifications()` (`emptyFlow()` is sufficient).
12. `app-core/src/test/java/com/smartsales/prism/data/real/L2DualEngineBridgeTest.kt` — extend the object `: ConnectivityBridge` literal to implement `batteryNotifications()` (`emptyFlow()` is sufficient).
13. `docs/cerb/interface-map.md` — rewrite line 147 to reflect that `Bat#` ingest is now shipped (remove the "follow-up debt" framing; keep the pointer to `docs/specs/esp32-protocol.md` §9).
14. `docs/specs/esp32-protocol.md` — flip the Implementation Status row 10 (Battery Level Push) from `⏳ Pending bridge wiring` to `✅ Implemented` and update the App-Side Notes to point at the real call chain (`RealConnectivityBridge.batteryNotifications()` → `ConnectivityViewModel.batteryLevel`).
15. `docs/cerb/connectivity-bridge/interface.md` — add one row for `batteryNotifications(): Flow<Int>` in the interface enumeration. Do NOT fix the pre-existing staleness (missing `audioRecordingNotifications()` / `wifiRepairEvents()`); that becomes a surfaced follow-up in the closeout lesson proposals.
16. `docs/projects/firmware-protocol-intake/tracker.md` — flip sprint 01 row to `done` at closeout with the one-line summary.

**Out of scope** (do not touch under any outcome):

- `platforms/harmony/**` — Harmony-native port is a separate future sprint
- `docs/cerb-ui/dynamic-island/spec.md:100` — the provisional note there stays accurate prose-wise because `batteryLevel` still has a seeded default until the first push; revisit only if the dynamic island needs `Int?` semantics (not this sprint)
- Persistence / last-known-value caching across BLE reconnects
- Value clamping beyond parser-level validation (parser rejects invalid strings; accepted values are already 0-100)
- UI changes (colors, icons, low-battery thresholds, copy)
- Any `Ver:` / `volume#` / other protocol item (scope is strictly `Bat#`)
- `ConnectivityViewModelTest` infrastructure overhaul — add one new case only
- Changes to the `BadgeNotification` sealed type's visibility or module — add a variant in place
- Nullable-battery type (`StateFlow<Int?>` or a `hasBatteryReading` flag) — future UI sprint; this sprint preserves `StateFlow<Int>` for binary compat with existing consumers in `ConnectivityModal`, `SimShellDynamicIslandCoordinator`, and the dynamic-island spec
- Pre-existing staleness in `docs/cerb/connectivity-bridge/interface.md` (missing `audioRecordingNotifications()` / `wifiRepairEvents()`) — record as a follow-up lesson proposal; do not bundle into this sprint

**Slug freedom:** none. Slug fixed to `bat-listener-wiring`.

## References

Operator reads these, not the full repo:

1. `docs/specs/sprint-contract.md` — contract schema
2. `docs/specs/esp32-protocol.md` — protocol SOT (especially §9)
3. `docs/cerb/interface-map.md` lines 140-148 — ownership edge being closed
4. `docs/projects/firmware-protocol-intake/tracker.md` — project context
5. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewayProtocolSupport.kt` — parser pattern to mirror
6. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerIngressSupport.kt` lines 25-34 — ingress pattern
7. `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt` lines 75-82, 218-222 — bridge exposure pattern
8. `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt` lines 73-75 — the provisional source being replaced
9. `docs/cerb/connectivity-bridge/interface.md` lines 10-18 and 80-95 — bridge interface contract doc (add the new method here; do not fix pre-existing omissions in this sprint)

## Success Exit Criteria

Literally checkable:

- `grep -n '"Bat#"' app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewayProtocolSupport.kt` returns at least one hit
- `grep -n "BatteryLevel" app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/` returns hits in both the sealed-type file and the parser file
- `grep -n "batteryNotifications" data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/ConnectivityBridge.kt app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt` returns hits in both files
- `grep -c "MutableStateFlow(85)" app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt` returns `0` (hardcoded provisional value removed)
- `grep -n "stateIn" app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt` returns the bridge-backed `batteryLevel` initialization with initial value `85` (or an equivalent non-`MutableStateFlow(85)` seeded `StateFlow<Int>` implementation is present)
- `./gradlew :app-core:testDebugUnitTest --tests "com.smartsales.prism.data.connectivity.legacy.gateway.GattBleGatewayNotificationParsingTest" --tests "com.smartsales.prism.data.connectivity.legacy.DefaultDeviceConnectionManagerIngressTest" --tests "com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelTest"` exits 0 with the new cases visible in the report
- `./gradlew :app:assembleDebug` exits 0
- `docs/cerb/interface-map.md:147` no longer contains the string "explicit follow-up debt" — the prose must reflect that `Bat#` ingest is shipped
- `docs/specs/esp32-protocol.md` row 10 status cell no longer contains "⏳ Pending bridge wiring"
- `grep -l "batteryNotifications\\|batteryEvents" app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/FakeDeviceConnectionManager.kt app-core/src/test/java/com/smartsales/prism/data/audio/RealBadgeAudioPipelineIngressTest.kt app-core/src/test/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModelRepairTest.kt app-core/src/test/java/com/smartsales/prism/data/real/L2DualEngineBridgeTest.kt` returns all four paths
- `grep -n "batteryNotifications" docs/cerb/connectivity-bridge/interface.md` returns the new `batteryNotifications(): Flow<Int>` row
- `docs/projects/firmware-protocol-intake/tracker.md` sprint 01 row shows status `done` with a one-line Closeout summary
- One commit on `develop` covers all the above; commit message references this contract file

## Stop Exit Criteria

Halt and surface:

- The `BadgeNotification` sealed type lives in a module or package whose visibility forbids adding a variant from the planned location — stop, do not restructure modules
- `ConnectivityBridge` interface sits behind a binary-compat constraint (e.g., it's published as part of a stable API surface consumed by `:connectivity-debug-app` or Harmony-native) that forbids adding a method without coordinated release — stop and surface
- A second unknown consumer of `ConnectivityViewModel.batteryLevel` surfaces that expects a default value semantics different from `85` — stop; the type/default decision needs explicit operator arbitration
- `./gradlew :app:assembleDebug` fails with errors that are not straightforward (e.g., KAPT stub regeneration chain, Hilt binding conflict) and require touching out-of-scope files — stop; do not chase into Hilt module edits beyond a one-line binding addition
- Existing unrelated unit test starts failing after the change — stop, do not chase into out-of-scope test code
- Iteration bound hit without all Success Exit Criteria green — stop per above

## Iteration Bound

- Max 3 iterations OR 90 minutes wall-clock, whichever hits first
- One iteration = all code edits + test additions + doc updates + one build + one focused test run + one commit
- Hitting the bound without green is a stop, not a force-close

## Required Evidence Format

At close, operator appends to Closeout:

1. **Code-change evidence:**
   - `git diff --stat` scoped to the commit — shows files touched and line deltas
   - `grep -n '"Bat#"\|BatteryLevel\|batteryNotifications' <files-in-scope>` output proving the new symbols exist
   - `grep -c "MutableStateFlow(85)" app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt` output = `0`
2. **Test evidence:**
   - `./gradlew :app-core:testDebugUnitTest --tests "...NotificationParsingTest" --tests "...DefaultDeviceConnectionManagerIngressTest" --tests "...ConnectivityViewModelTest"` full output (or the three `PASSED` lines for the new cases)
3. **Build evidence:**
   - `./gradlew :app:assembleDebug` tail showing `BUILD SUCCESSFUL`
4. **Doc-change evidence:**
   - `git diff docs/cerb/interface-map.md docs/specs/esp32-protocol.md docs/cerb/connectivity-bridge/interface.md docs/projects/firmware-protocol-intake/tracker.md` — shows the four doc edits

Agent narration without these artifacts is not acceptable evidence.

## Iteration Ledger

- Authoring revision (2026-04-24, operator Claude): Codex review surfaced incomplete scope for interface implementer/test fallout, wrong sealed-type path, and unsafe initial battery value; all incorporated before handoff. Next action: execution iteration by Codex.
- Iteration 1 (2026-04-24, operator Codex): implemented the `Bat#<0..100>` parser branch, `BadgeNotification.BatteryLevel` sealed variant, `DeviceConnectionManager.batteryEvents`, `ConnectivityBridge.batteryNotifications()`, and the `ConnectivityViewModel.batteryLevel` bridge-backed `stateIn(..., 85)` replacement; added parser/ingress/viewmodel tests and updated the protocol/interface-map/interface docs. Evaluator saw: initial local verification was blocked first by sandbox netlink limits, then by missing Java toolchain wiring in the escalated environment, and then by a real compile fallout in `GattBleGatewaySessionSupport.kt` where the new sealed variant made a `when` non-exhaustive. Next action: patch the session callback, rerun scoped tests with explicit Java 17, then rerun `:app:assembleDebug`.
- Iteration 2 (2026-04-24, operator Codex): patched `GattBleGatewaySessionSupport.onCharacteristicChanged()` to forward `BadgeNotification.BatteryLevel`, reran the scoped tests with explicit `JAVA_HOME=/home/cslh-frank/jdks/jdk-17.0.11+9`, and reran `:app:assembleDebug`. Evaluator saw: `:app:assembleDebug` passed; the first scoped test rerun exposed the missing sealed-branch compile error; the second rerun passed cleanly once that branch was added. Next action: close sprint 01 as success, flip the project tracker row to `done`, and commit the sprint closeout.

## Closeout

- **Status:** `success`
- **Summary for project tracker:** Wired `Bat#<0..100>` through parser -> bridge -> `ConnectivityViewModel.batteryLevel`, added focused tests, and verified with scoped `testDebugUnitTest` plus `:app:assembleDebug`.
- **Evidence artifacts:**
  - Code-change evidence:
    - `grep -n '"Bat#"\|BatteryLevel\|batteryNotifications' app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewayProtocolSupport.kt app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/BleGateway.kt data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/ConnectivityBridge.kt app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt docs/cerb/connectivity-bridge/interface.md`
    - Hits included:
      - `GattBleGatewayProtocolSupport.kt:117` `raw.startsWith("Bat#", ignoreCase = true)`
      - `BleGateway.kt:100` `data class BatteryLevel(val percent: Int)`
      - `ConnectivityBridge.kt:76` `fun batteryNotifications(): Flow<Int>`
      - `RealConnectivityBridge.kt:224` `override fun batteryNotifications(): Flow<Int> = deviceManager.batteryEvents`
    - `grep -c "MutableStateFlow(85)" app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt` -> `0`
    - `git diff --stat` -> `22 files changed, 161 insertions(+), 11 deletions(-)` at close
  - Test evidence:
    - `JAVA_HOME=/home/cslh-frank/jdks/jdk-17.0.11+9 OPENSHIFT_INTERNAL_IP=127.0.0.1 GRADLE_USER_HOME=/tmp/gradle-user-home /home/cslh-frank/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13/bin/gradle :app-core:testDebugUnitTest --tests "com.smartsales.prism.data.connectivity.legacy.gateway.GattBleGatewayNotificationParsingTest" --tests "com.smartsales.prism.data.connectivity.legacy.DefaultDeviceConnectionManagerIngressTest" --tests "com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelTest"`
    - Result: `BUILD SUCCESSFUL in 1m 52s`
  - Build evidence:
    - `JAVA_HOME=/home/cslh-frank/jdks/jdk-17.0.11+9 OPENSHIFT_INTERNAL_IP=127.0.0.1 GRADLE_USER_HOME=/tmp/gradle-user-home /home/cslh-frank/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13/bin/gradle :app:assembleDebug`
    - Result: `BUILD SUCCESSFUL in 1m 12s`
  - Doc-change evidence:
    - `git diff docs/cerb/interface-map.md docs/specs/esp32-protocol.md docs/cerb/connectivity-bridge/interface.md docs/projects/firmware-protocol-intake/tracker.md`
- **Lesson proposals:** surface both candidate lessons at close for user approval: firmware drops often land docs-first while app-side wiring follows in a later sprint; `docs/cerb/connectivity-bridge/interface.md` staleness (missing `audioRecordingNotifications()` / `wifiRepairEvents()` method rows) should be a separate follow-up doc-sync sprint under an appropriate project (likely `firmware-protocol-intake` if future drops touch those seams, otherwise a one-off docs sprint under `develop-protocol-migration`)
- **CHANGELOG line:** candidate `- **android / connectivity**: Battery level now sourced from real badge push (`Bat#<0..100>`) via ConnectivityBridge, replacing the provisional hardcoded value. Protocol contract: docs/specs/esp32-protocol.md §9.` — operator confirms with user at close before committing
