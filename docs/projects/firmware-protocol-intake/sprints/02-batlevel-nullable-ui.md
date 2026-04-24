# Sprint 02 — batlevel-nullable-ui

## Header

- Project: firmware-protocol-intake
- Sprint: 02
- Slug: batlevel-nullable-ui
- Date authored: 2026-04-24
- Author: Claude
- Operator: **Codex** (default operator per project tracker)
- Lane: `develop` (Android-side UI follow-up to sprint 01; Harmony-native port remains a separate future sprint under `platform/harmony`)
- Branch: `develop` (direct-to-develop per project Cross-Sprint Decisions; no feature branch)
- Worktree: none
- Ship path: one commit on `develop` at sprint close containing code + docs + contract closeout
- Blocked-by: sprint 01 (`bat-listener-wiring`) must ship first — this sprint presumes `ConnectivityBridge.batteryNotifications(): Flow<Int>` exists and `ConnectivityViewModel.batteryLevel` is already bridge-backed with initial value `85`.

## Demand

**User ask (sprint 01 out-of-scope deferral, verbatim):** "Nullable-battery type (`StateFlow<Int?>` or a `hasBatteryReading` flag) — future UI sprint; this sprint preserves `StateFlow<Int>` for binary compat with existing consumers in `ConnectivityModal`, `SimShellDynamicIslandCoordinator`, and the dynamic-island spec."

**Interpretation:** Flip `ConnectivityViewModel.batteryLevel` from the bridge-backed `StateFlow<Int>` (seeded `85`) to `StateFlow<Int?>` with initial value `null`, so the UI can distinguish "no `Bat#` push received yet" from a real low reading. Propagate nullable through the three known consumers: `ConnectivityModal` (`ConnectedView` battery row), `HistoryDrawer` battery chip, and `SimShellDynamicIslandCoordinator` (already passes to `DynamicIslandItem.batteryPercentage`, which is itself `Int?`). UX rule: when null, render the text surfaces as `--%` in muted color; the dynamic-island ambient flank (`SimHomeHeroShell.ambientBatteryPercentage`) hides the battery chip entirely rather than falling back to the current `78` literal. No protocol changes, no new listeners.

## Scope

**In scope** (file-level list; all paths rooted at repo root):

1. `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt` — change `batteryLevel: StateFlow<Int>` to `StateFlow<Int?>`; change initial value of the `stateIn` / bridge-backed pipeline from `85` to `null`; no other behavior change.
2. `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt` line 146 — pass-through of `connectivityViewModel.batteryLevel` to whatever consumer it feeds; update the receiving type signature to accept `StateFlow<Int?>`.
3. `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt` — change constructor param `batteryLevel: StateFlow<Int>` to `StateFlow<Int?>`; change `currentBatteryLevel: Int` field to `Int?`; remove `.coerceIn(0, 100)` on `batteryPercentage` assignment (pass raw `Int?` — `DynamicIslandItem.batteryPercentage` is already `Int?`); update `buildConnectivityLaneItem(batteryLevel: Int?, ...)` signature.
4. `app-core/src/main/java/com/smartsales/prism/ui/sim/SimHomeHeroShell.kt` line 541 — replace `visibleItem?.batteryPercentage ?: 78` fallback with a null pathway that hides the ambient flank's battery chip (the existing `showAmbientFlanks` flag at line 540 already gates visibility; extend gating to also require non-null battery, or add a second flag `showAmbientBattery`).
5. `app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt` lines 82, 146, 228, 249, 287, 313 — thread `Int?` through the `ConnectivityModal` / `ConnectedView` signature; at line 313 render `"--%"` (muted) when null and `"$batteryLevel%"` otherwise.
6. `app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt` lines 73, 132, 154, 188 — same treatment: thread `Int?` through the drawer signature; render `"--%"` (muted) when null.
7. Unit tests — update:
   - `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinatorTest.kt` lines 229, 234, 434, 441 — change `MutableStateFlow(85)` seeds to `MutableStateFlow<Int?>(null)` for the "not yet received" baseline cases; add at least one case that emits a real value (`50`) and asserts `visibleItem?.batteryPercentage == 50`, and one baseline case that asserts `null` → `batteryPercentage == null`.
   - `app-core/src/test/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModelTest.kt` — update the sprint-01-added case to seed `null` as the initial value and assert `batteryLevel.value == null` before any emission; extend the emission case to go `null → 42`.
   - `app-core/src/androidTest/java/com/smartsales/prism/ui/sim/SimDrawerGestureTest.kt` line 454 — leave `batteryPercentage = 85` as-is (instrumentation fixture, still valid since `DynamicIslandItem.batteryPercentage` is already `Int?`).
8. `docs/cerb-ui/dynamic-island/spec.md` lines 95-101 — rewrite the "Current battery rule" block:
   - remove the "provisional" / "follow-up debt" framing
   - add: battery value is real and bridge-backed via `ConnectivityViewModel.batteryLevel: StateFlow<Int?>`, seeded `null` until the first `Bat#` push
   - add: ambient flank battery chip is hidden when the value is `null`, and text surfaces render `--%` placeholder
9. `docs/cerb/interface-map.md` line 147 — tighten the line to reflect that battery sourcing is shipped AND nullable semantics are in place; drop any remaining "provisional" language if sprint 01 left some behind. Keep the pointer to `docs/specs/esp32-protocol.md` §9.
10. `docs/projects/firmware-protocol-intake/tracker.md` — flip sprint 02 row to `done` at closeout with the one-line summary.

**Out of scope** (do not touch under any outcome):

- `platforms/harmony/**` — Harmony-native port of battery UX is a separate future sprint under `platform/harmony`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/**` — bridge/gateway/parser layers are sprint 01's territory; this sprint only touches UI viewmodel and presentation
- `DynamicIslandItem.batteryPercentage: Int?` — already nullable in `DynamicIsland.kt:69`, do not change
- Icon / visualState refinements for the "no reading yet" state beyond hiding the flank chip — a new visual state (e.g., `CONNECTIVITY_CONNECTED_NO_BATTERY`) is explicitly deferred to a future UX polish sprint
- Copy/style of the `--%` placeholder beyond "muted color, identical font size to the real reading" — do not introduce new typography tokens
- Low-battery thresholds, color-coded warnings, or charging-state indicators — the `Bat#` protocol has no charging-state signal; any such UX is out of firmware-protocol-intake scope
- Changes to `voiceVolume` / `Ver:` / any other protocol item
- Refactoring `SimHomeHeroShell`'s ambient flank architecture — one-line fallback replacement only, no component extraction

**Slug freedom:** none. Slug fixed to `batlevel-nullable-ui`.

## References

Operator reads these, not the full repo:

1. `docs/specs/sprint-contract.md` — contract schema
2. `docs/projects/firmware-protocol-intake/sprints/01-bat-listener-wiring.md` — predecessor sprint; inherits `batteryNotifications()` seam and the `StateFlow<Int>` baseline this sprint flips
3. `docs/specs/esp32-protocol.md` §9 — protocol SOT for `Bat#<0..100>`
4. `docs/cerb-ui/dynamic-island/spec.md` lines 88-108 — the spec block being rewritten
5. `docs/cerb/interface-map.md` lines 140-148 — ownership edge to tighten
6. `app-core/src/main/java/com/smartsales/prism/ui/components/DynamicIsland.kt` lines 65-72 — `DynamicIslandItem.batteryPercentage: Int?` is already nullable (do not touch; this is why the propagation is mostly a plumbing change)
7. `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt` lines 40-96, 304-355 — coordinator state plumbing and `buildConnectivityLaneItem`
8. `app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt` lines 82-313 — `ConnectedView` battery row
9. `app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt` lines 73-190 — drawer battery chip
10. `app-core/src/main/java/com/smartsales/prism/ui/sim/SimHomeHeroShell.kt` lines 535-545 — ambient flank fallback literal `78`

## Success Exit Criteria

Literally checkable:

- `grep -n "MutableStateFlow<Int?>\|StateFlow<Int?>" app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt` returns at least one hit on the `batteryLevel` declaration
- `grep -c "initialValue = 85\|, 85)\|(85)" app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt` returns `0` (the `85` seed is gone)
- `grep -n "?: 78" app-core/src/main/java/com/smartsales/prism/ui/sim/SimHomeHeroShell.kt` returns `0` hits (the `78` fallback is gone)
- `grep -n "batteryLevel: Int?" app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt` returns hits in both files
- `grep -n 'StateFlow<Int?>' app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt` returns at least one hit on the `batteryLevel` param
- `grep -n '"--%"' app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt` returns hits in both files (muted placeholder rendered when `batteryLevel == null`)
- `./gradlew :app-core:testDebugUnitTest --tests "com.smartsales.prism.ui.sim.SimShellDynamicIslandCoordinatorTest" --tests "com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelTest"` exits 0 with the updated / new cases visible in the report
- `./gradlew :app:assembleDebug` exits 0
- `docs/cerb-ui/dynamic-island/spec.md` lines 95-101 block no longer contains the strings "provisional" or "follow-up debt" in the battery context
- `grep -n "provisional" docs/cerb/interface-map.md` returns `0` hits on line 147 (provisional framing removed from the battery line)
- `docs/projects/firmware-protocol-intake/tracker.md` sprint 02 row shows status `done` with a one-line Closeout summary
- One commit on `develop` covers all the above; commit message references this contract file

## Stop Exit Criteria

Halt and surface:

- Sprint 01 has not shipped yet — `batteryNotifications()` absent on `ConnectivityBridge` or `batteryLevel` still initialized to `MutableStateFlow(85)` in `ConnectivityViewModel.kt`. Do not attempt to execute this sprint against pre-sprint-01 state; surface and wait
- A consumer of `batteryLevel` surfaces outside the four documented sites (Shell / Modal / Drawer / Coordinator) — stop, do not blanket-rewrite; surface the unknown consumer
- Removing the `?: 78` fallback in `SimHomeHeroShell` visibly breaks the ambient flank layout in a way that requires new composable work (not a one-line visibility flag) — stop; the null-flank rendering is a UX question for the user, not operator judgment
- A test fixture outside `SimShellDynamicIslandCoordinatorTest` / `ConnectivityViewModelTest` / `SimDrawerGestureTest` starts failing due to type flip from `Int` to `Int?` — stop; do not chase into out-of-scope test code
- `./gradlew :app:assembleDebug` fails with errors requiring touching out-of-scope files (e.g., Harmony, domain/, data/) — stop
- Iteration bound hit without all Success Exit Criteria green — stop

## Iteration Bound

- Max 3 iterations OR 60 minutes wall-clock, whichever hits first
- One iteration = all code edits + test updates + doc updates + one build + one focused test run + one commit
- Hitting the bound without green is a stop, not a force-close

## Required Evidence Format

At close, operator appends to Closeout:

1. **Code-change evidence:**
   - `git diff --stat` scoped to the commit — shows files touched and line deltas
   - `grep -n 'StateFlow<Int?>\|"--%"' <files-in-scope>` output proving the new shapes exist
   - `grep -c "?: 78\|MutableStateFlow(85)" app-core/src/main/java/com/smartsales/prism/ui/sim/SimHomeHeroShell.kt app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt` output = `0` for both
2. **Test evidence:**
   - `./gradlew :app-core:testDebugUnitTest --tests "...SimShellDynamicIslandCoordinatorTest" --tests "...ConnectivityViewModelTest"` full output (or `PASSED` lines for the null / non-null cases)
3. **Build evidence:**
   - `./gradlew :app:assembleDebug` tail showing `BUILD SUCCESSFUL`
4. **Doc-change evidence:**
   - `git diff docs/cerb-ui/dynamic-island/spec.md docs/cerb/interface-map.md docs/projects/firmware-protocol-intake/tracker.md` — shows the three doc edits

Agent narration without these artifacts is not acceptable evidence.

## Iteration Ledger

- Iteration 1 (2026-04-24, operator Codex): implemented the nullable battery pass across `ConnectivityViewModel`, `SimShellDynamicIslandCoordinator`, `SimHomeHeroShell`, `ConnectivityModal`, `HistoryDrawer`, and the two scoped unit-test files; rewrote the dynamic-island battery rule block and tightened the interface-map battery edge. Evaluator saw: all grep-based code-shape checks passed (`StateFlow<Int?>`, `batteryLevel: Int?`, `"--%"`, and removal of `?: 78` / `85` seed), but Gradle verification is blocked in this sandbox because socket creation is forbidden during both file-lock listener startup and daemon bootstrap (`NetworkInterface.getNetworkInterfaces` / `DatagramSocket` / `ServerSocketChannel`). Next action: user-run or unsandboxed rerun of the required scoped tests and `:app:assembleDebug`, then close or resume based on real build output.
- Iteration 2 (2026-04-24, operator Codex): user reran the required verification outside the sandbox. Evaluator saw: both the scoped unit test task and `:app:assembleDebug` completed successfully, satisfying the remaining stop condition. Next action: close sprint 02 as success and make the single develop commit once the user confirms the `CHANGELOG` choice.

## Closeout

- **Status:** `success`
- **Summary for project tracker:** Flipped `ConnectivityViewModel.batteryLevel` to `StateFlow<Int?>` seeded `null`, propagated nullable through Modal / Drawer / Island coordinator, and rendered `--%` / hidden ambient battery until first `Bat#` push.
- **Evidence artifacts:**
  - Code-change evidence:
    - `grep -n "MutableStateFlow<Int?>\|StateFlow<Int?>" app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt`
    - Result: `74:    val batteryLevel: StateFlow<Int?> = connectivityBridge.batteryNotifications()`
    - `grep -c "initialValue = 85\|, 85)\|(85)" app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt`
    - Result: `0`
    - `grep -n "?: 78" app-core/src/main/java/com/smartsales/prism/ui/sim/SimHomeHeroShell.kt`
    - Result: no hits
    - `grep -n "batteryLevel: Int?" app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt`
    - Results:
      - `ConnectivityModal.kt:228:    batteryLevel: Int?,`
      - `ConnectivityModal.kt:287:    batteryLevel: Int?,`
      - `HistoryDrawer.kt:154:    batteryLevel: Int?,`
    - `grep -n 'StateFlow<Int?>' app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt`
    - Result: `40:    batteryLevel: StateFlow<Int?>,`
    - `grep -n '"--%"' app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt`
    - Results:
      - `ConnectivityModal.kt:314:                text = batteryLevel?.let { "$it%" } ?: "--%",`
      - `HistoryDrawer.kt:188:                    batteryLevel?.let { "$it%" } ?: "--%",`
    - `git diff --stat`
    - Result: `11 files changed, 111 insertions(+), 32 deletions(-)`
  - Test evidence:
    - Command:
      - `./gradlew :app-core:testDebugUnitTest --tests "com.smartsales.prism.ui.sim.SimShellDynamicIslandCoordinatorTest" --tests "com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelTest"`
    - Result:
      - `BUILD SUCCESSFUL in 10s`
      - `367 actionable tasks: 44 executed, 323 up-to-date`
  - Build evidence:
    - Command:
      - `./gradlew :app:assembleDebug`
    - Result:
      - `BUILD SUCCESSFUL in 4s`
      - `109 actionable tasks: 24 executed, 85 up-to-date`
  - Doc-change evidence:
    - `git diff docs/cerb-ui/dynamic-island/spec.md docs/cerb/interface-map.md docs/projects/firmware-protocol-intake/tracker.md`
- **Lesson proposals:** candidate for operator to surface at close: "firmware-drop follow-up sprints naturally split into two passes — first the data-plumbing pass (sprint 01 shape: wire the seam, keep the existing non-null type for binary compat), then the semantics pass (sprint 02 shape: widen the type to nullable once all consumers are mapped). The two-pass split is cheaper than trying to land both in one sprint because the consumer audit is done with the seam already in place."
- **CHANGELOG line:** candidate `- **android / connectivity**: Battery level UI now distinguishes "no reading yet" from a real reading — ConnectivityViewModel.batteryLevel is StateFlow<Int?> seeded null; text surfaces render --% placeholder until the first Bat# push arrives.` — operator confirms with user at close before committing
