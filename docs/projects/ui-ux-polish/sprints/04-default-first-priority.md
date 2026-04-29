# Sprint 04 - default-first-priority

## Header

- Project: ui-ux-polish
- Sprint: 04
- Slug: default-first-priority
- Date authored: 2026-04-29
- Author: Claude
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none
- Dependency: blocked until `docs/projects/ui-ux-polish/sprints/03-registry-gated-reconnect.md` closes successfully

## Demand

**User ask (verbatim):**

> Author UI-UX Polish Sprints 03 and 04. Sequence Android `develop` work for registry-gated reconnect first, then default-first BLE priority, with Core Flow alignment made explicit.

**Interpretation:**

After stale removed-MAC reconnects are guarded, BLE detection should prefer the registered default badge when both the current active badge and the default badge are advertising. This sprint changes only that priority rule, preserves passive `setDefault()`, and syncs Core Flow wording if the rule intentionally relaxes the current active-only reconnect target.

## Scope

Files the operator may touch:

- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManager.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/DeviceRegistry.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/SharedPrefsDeviceRegistry.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/InMemoryDeviceRegistry.kt`
- `app-core/src/test/java/com/smartsales/prism/data/connectivity/registry/*`
- `docs/projects/ui-ux-polish/tracker.md`
- `docs/projects/ui-ux-polish/sprints/04-default-first-priority.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/bake-contracts/connectivity-badge-session.md`

Out of scope:

- Sprint 03 stale-session or removed-MAC guard work.
- Immediate active-device switching inside `setDefault()`.
- Harmony or `platform/harmony`.
- UI redesign.
- Public manager API changes unless implementation proves no contained internal alternative exists.

## References

- `docs/specs/sprint-contract.md`
- `docs/projects/ui-ux-polish/tracker.md`
- `docs/projects/ui-ux-polish/sprints/01-connectivity-modal-state.md`
- `docs/projects/ui-ux-polish/sprints/02-audio-card-hold-state-fix.md`
- `docs/projects/ui-ux-polish/sprints/03-registry-gated-reconnect.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/bake-contracts/connectivity-badge-session.md`
- `docs/sops/esp32-connectivity-debug.md`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManager.kt`

## Success Exit Criteria

All criteria must pass before close:

1. With active non-default badge B disconnected while default badge A and active badge B both advertise, the BLE detection monitor selects A and emits `[DefaultPriority] switch`.
2. A default device with `manuallyDisconnected=true` is not auto-selected.
3. Single-candidate behavior remains unchanged: when only one registered eligible candidate advertises, the monitor selects that candidate according to the preexisting reconnect rules.
4. `setDefault()` remains passive: calling it updates default metadata but does not immediately switch `activeDevice`, seed `SessionStore`, or call reconnect.
5. If this sprint changes the current Core Flow rule that auto reconnect targets only the active badge, `docs/core-flow/badge-connectivity-lifecycle.md` is updated in this sprint and lower docs cite the revised rule instead of contradicting it.
6. Focused tests cover default-first dual-candidate selection, manually-disconnected default suppression, single-candidate unchanged behavior, and passive `setDefault()`.
7. Runtime evidence uses a cleared logcat window and includes `[DefaultPriority] switch` for the dual-candidate case.
8. `./gradlew :app-core:testDebugUnitTest` exits 0.
9. `./gradlew :app:assembleDebug` exits 0.

## Stop Exit Criteria

- Sprint 03 is not closed successfully; keep this sprint blocked.
- Default-first priority conflicts with `docs/core-flow/badge-connectivity-lifecycle.md` and the user does not approve changing the active-only reconnect rule.
- BLE scanner evidence cannot distinguish dual-candidate advertising from single-candidate advertising after three iterations; stop with the missing evidence named.
- The fix would make `setDefault()` switch devices immediately or reconnect without a separate user or monitor event; stop because that violates the passive default requirement.
- `./gradlew :app-core:testDebugUnitTest` or `./gradlew :app:assembleDebug` fails in an unrelated area and the failure cannot be isolated or fixed within the iteration bound.

## Iteration Bound

3 implementation iterations or 90 minutes, whichever hits first.

Each iteration must:

1. Confirm Sprint 03 is closed successfully before editing code.
2. Re-read the Core Flow auto-reconnect wording and decide whether doc sync is required.
3. Implement or refine the default-first candidate selection rule.
4. Run focused registry-manager tests.
5. Run the required full test and assemble commands before close.
6. Capture cleared-window logcat for the dual-candidate priority scenario before claiming runtime success.

## Required Evidence Format

Closeout must include:

- Focused test command output for the new or changed default-priority tests.
- `./gradlew :app-core:testDebugUnitTest` result.
- `./gradlew :app:assembleDebug` result.
- Cleared-window runtime evidence using:

```bash
adb logcat -c
adb logcat -d -s SmartSalesConn
```

The excerpt must include `[DefaultPriority] switch` for the dual-candidate case and evidence that `manuallyDisconnected=true` default was skipped.

## Iteration Ledger

_Operator fills this section once per iteration._

## Closeout

_Operator fills at exit._

- Status: `success` | `stopped` | `blocked`
- One-liner for tracker:
- Evidence artifacts:
- Lesson proposals:
- CHANGELOG line:
