# Sprint 03 - registry-gated-reconnect

## Header

- Project: ui-ux-polish
- Sprint: 03
- Slug: registry-gated-reconnect
- Date authored: 2026-04-29
- Author: Claude
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none

## Demand

**User ask (verbatim):**

> Author UI-UX Polish Sprints 03 and 04. Sequence Android `develop` work for registry-gated reconnect first, then default-first BLE priority, with Core Flow alignment made explicit.

**Interpretation:**

The multi-device connectivity path must stop reconnecting to a badge after the user removes that badge from the registry. This sprint gates session restore, auto reconnect, and BLE-detection reconnect against current registry membership before any stale session can call `connectUsingSession` or `switchToDevice`.

## Scope

Files the operator may touch:

- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManager.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/DeviceRegistry.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/SharedPrefsDeviceRegistry.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/InMemoryDeviceRegistry.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DefaultDeviceConnectionManager.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt`
- `app-core/src/test/java/com/smartsales/prism/data/connectivity/registry/*`
- `app-core/src/test/java/com/smartsales/prism/data/connectivity/legacy/*`
- `docs/projects/ui-ux-polish/tracker.md`
- `docs/projects/ui-ux-polish/sprints/03-registry-gated-reconnect.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/bake-contracts/connectivity-badge-session.md`

Out of scope:

- Default-first BLE priority changes; those belong to Sprint 04.
- Harmony or `platform/harmony`.
- Public manager API changes unless no contained internal alternative can enforce the registry gate.
- UI restyling or modal layout changes.

## References

- `docs/specs/sprint-contract.md`
- `docs/projects/ui-ux-polish/tracker.md`
- `docs/projects/ui-ux-polish/sprints/01-connectivity-modal-state.md`
- `docs/projects/ui-ux-polish/sprints/02-audio-card-hold-state-fix.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/bake-contracts/connectivity-badge-session.md`
- `docs/sops/esp32-connectivity-debug.md`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManager.kt`

## Success Exit Criteria

All criteria must pass before close:

1. Removing a non-active registered device whose MAC matches the stored `SessionStore` session leaves no stored session targeting that removed MAC.
2. Session cleanup is conservative: if another registered device remains, the stored session is seeded to the remaining active/default device; the session is cleared only when no registered devices remain.
3. Auto reconnect refuses an unregistered stored session and reaches `NeedsSetup` or `Disconnected` without calling `connectUsingSession`.
4. BLE detection revalidates registry membership immediately before `switchToDevice`, so a removed MAC found by an older scan candidate cannot become active.
5. Runtime log evidence contains `[ReconnectGuard] aborted` when a stale removed-MAC reconnect is refused.
6. Cleared-window logcat proves no connect attempt to the removed MAC for at least 60 seconds after removal.
7. Focused tests cover stale stored session cleanup, unregistered auto reconnect refusal, BLE detection revalidation, and remaining-device session seeding.
8. `./gradlew :app-core:testDebugUnitTest` exits 0.
9. `./gradlew :app:assembleDebug` exits 0.
10. Any changed Core Flow, Cerb, interface, or BAKE wording keeps `docs/core-flow/badge-connectivity-lifecycle.md` as the source above lower implementation docs.

## Stop Exit Criteria

- The only viable fix requires changing public `DeviceRegistryManager` or `DeviceConnectionManager` API shape beyond contained internal guards; stop and ask for approval before broadening the contract.
- Current code cannot distinguish stale stored sessions from valid registered sessions without adding a new persistence migration; stop with the proposed migration shape.
- Hardware or adb is unavailable for the required 60-second no-connect evidence after three implementation iterations; stop rather than closing on unit tests alone.
- `./gradlew :app-core:testDebugUnitTest` or `./gradlew :app:assembleDebug` fails in an unrelated area and the failure cannot be isolated or fixed within the iteration bound.

## Iteration Bound

3 implementation iterations or 90 minutes, whichever hits first.

Each iteration must:

1. Capture the current failing behavior or explain why it cannot be reproduced.
2. Apply the smallest registry/session guard change that addresses the observed stale path.
3. Run focused tests for the changed owner.
4. Run the required full test and assemble commands before close.
5. Capture cleared-window logcat for the stale removed-MAC scenario before claiming runtime success.

## Required Evidence Format

Closeout must include:

- Focused test command output for the new or changed registry/reconnect tests.
- `./gradlew :app-core:testDebugUnitTest` result.
- `./gradlew :app:assembleDebug` result.
- Cleared-window runtime evidence using:

```bash
adb logcat -c
adb logcat -d -s SmartSalesConn
```

The excerpt must include `[ReconnectGuard] aborted` and must show no connect attempt to the removed MAC for at least 60 seconds after removal. If a narrower tag is added for the guard, list the exact tag and grep pattern in Closeout.

## Iteration Ledger

_Operator fills this section once per iteration._

## Closeout

_Operator fills at exit._

- Status: `success` | `stopped` | `blocked`
- One-liner for tracker:
- Evidence artifacts:
- Lesson proposals:
- CHANGELOG line:
