---
era: post-harness
---

# Deterministic Device-Loop Protocol

> **Status:** Active operating protocol
> **Date:** 2026-04-21
> **Position:** Callable from `/sprint` planner output and `/ship` evaluator step
> **Origin:** HS-010 / HS-006 scheduler harness on 2026-04-20 (`docs/platforms/harmony/test-signing-ledger.md` §6a)

This protocol defines the deterministic device-loop pattern that Codex follows when executing a forwarded sprint contract whose evidence class is L3 (on-device). It is the canonical expansion of the "HS-010-style harness loop" referenced in the test-signing ledger.

It is not full UI automation in the Selenium sense. It is a deterministic device-harness loop: the repetitive machine steps are scripted, the app path is constrained to a stable preset, runtime telemetry is mandatory, and human visual confirmation remains load-bearing.

---

## 1. Pipeline Contract

The device loop is a contract chain, not separate chores. Every L3 sprint produces the following typed evidence in this order, atomically per pass:

| Phase | Output | Trust level |
|---|---|---|
| build | unsigned artifact path | compile-success only -- not runtime proof |
| sign | signed artifact bound to the lane's real signing identity | identity proof, not behavior proof |
| install | device id + install confirmation | reachability proof |
| launch | entry component started | startup proof |
| drive | bounded preset path executed | path-coverage proof |
| capture | screenshots + layout dump + runtime log | observation evidence |
| relaunch | cold restart of the entry component | persistence boundary cross |
| verify | log + UI evidence after restart | behavior proof |

The chain may not be split. Compile success without launch + cold-relaunch is rejected as evidence by the evaluator.

---

## 2. Verification Boundary

**Evidence class:** `platform-runtime` (per `docs/specs/harness-manifesto.md` §5).

**Critical telemetry joints** declared per sprint. For the originating HS-006 slice these were:

- `[HS-006][FileStore] loadTasks success count=N`
- `[HS-006][SchedulerRepository] initialize restored count=N`

A sprint contract lists its own joints in the `Critical dataflow joints` field; the device loop captures runtime log lines for each joint on both the first pass and the cold-relaunch pass.

**Hierarchy** (from `CLAUDE.md` Debugging section): logs are primary evidence, screenshots are supporting evidence. An agent assertion is not visual evidence; a passing unit test is not runtime proof.

---

## 3. Core Invariants

These five invariants make the loop deterministic. They are non-negotiable; violating any of them breaks the trust contract with Codex at ship time.

1. **Deterministic preset entry, not free-form text automation.** Use in-app preset buttons (e.g., `Load HS-006 Demo Values`) that route through the real business path. Do not rely on Harmony `uitest` IME or Android `input text` for business-meaningful values.
2. **Real signed device path, not simulator-only proof.** Sign against the lane's real signing identity (Harmony: AGC-backed `smartsales.HOS.test`; Android: configured signing config or debug keystore for non-release lanes). Install on a real device with a recorded device id.
3. **Cold-relaunch verification, not just happy-path click-through.** Kill the process and relaunch from cold. Re-inspect logs and UI for restored state, completed state, and post-delete empty state.
4. **Logs as proof, screenshots as support.** A screenshot without the corresponding log line for the declared joint is incomplete evidence.
5. **Explicit honesty about deferred capability.** When a step cannot complete (e.g. Harmony reminders adapter missing -- see `docs/plans/harmony-tracker.md` HS-011), record it as deferred in the evidence schema. Do not fake parity.

---

## 4. Loop Steps

Eight phases, ordered, applies to both lanes:

1. **build** the artifact for the declared lane
2. **sign** against the lane's real signing identity
3. **install** to a real device (recorded device id)
4. **launch** the entry component
5. **drive** a bounded preset path through the real business pipeline
6. **capture** screenshots + layout dump + runtime log for declared joints
7. **cold relaunch** (kill + restart)
8. **verify** persistence / restore / completion / delete from logs and UI evidence

The loop must be stateful and regression-oriented: create -> restore -> complete -> delete -> cold restart -> re-check empty state. Single-shot happy-path evidence is rejected.

---

## 5. Lane Instances

### 5.1 Harmony

| Phase | Tooling |
|---|---|
| build | `hvigor` build of the declared `platforms/harmony/<app>` package |
| sign | AGC-backed `smartsales.HOS.test` lane (see `docs/platforms/harmony/test-signing-ledger.md`) |
| install | `hdc install -r <signed.hap>` against the recorded device id |
| launch | `hdc shell aa start -a EntryAbility -b <bundle>` |
| drive | in-app preset button(s) routed through the real submit pipeline |
| capture | `hdc shell snapshot_display`, `hdc shell hidumper -s WindowManagerService -a '-a'`, `hdc shell hilog -x | grep <joint-tag>` |
| cold relaunch | `hdc shell aa force-stop <bundle>` then re-launch via `aa start` |
| verify | re-run the `hilog` filter for each declared joint; capture final screenshot |

Reference evidence note: `docs/platforms/harmony/test-signing-ledger.md` §6a (HS-006 scheduler, device `4NY0225613001090`, 2026-04-20).

### 5.2 Android

| Phase | Tooling |
|---|---|
| build | `./gradlew :app:assembleDebug` (or declared variant) |
| sign | debug keystore for non-release lanes; configured signing config for release lanes |
| install | `adb install -r <signed.apk>` against the recorded device id |
| launch | `adb shell am start -n com.smartsales/.MainActivity` (or declared entry) |
| drive | in-app preset path (debug menu, dev preset button, or instrumented preset entry) routed through the real submit pipeline |
| capture | `adb shell screencap -p`, `adb shell uiautomator dump`, `adb logcat -d -s <tag1>,<tag2>` for declared joints |
| cold relaunch | `adb shell am force-stop com.smartsales` then re-launch via `am start` |
| verify | re-run the `logcat` filter for each declared joint; capture final screenshot |

The first Android L3 sprint is responsible for committing the concrete preset entry point and the canonical logcat tag conventions back into this section. Until that sprint runs, treat the row above as a template.

---

## 6. Evidence Schema

The handoff block delivered to Codex (via `/codex-handoff` or sprint-contract-driven `/ship`) must include the following when the sprint declared `Device loop: required`:

```
### Device-loop evidence

- Lane: harmony | android
- Device id: <serial>
- Signed artifact: <path or AGC build id>
- Joints captured (first pass):
  - <tag> -> <log excerpt>
  - <tag> -> <log excerpt>
- Cold-relaunch evidence:
  - <tag> -> <log excerpt>
- Screenshots: <repo paths or attachment refs>
- UI states verified: create, restore, complete, delete-empty
- Deferred: <list of steps that did not complete and why; empty if none>
```

Codex rejects ship if any required field above is absent for an L3 sprint. The "Deferred" field is mandatory but may be empty -- explicit emptiness signals the operator considered the question.

---

## 7. When Required

Mandatory:

- any sprint contract with `evidence_class: L3` on `lane: harmony`
- any sprint contract with `evidence_class: L3` on `lane: android`

Optional:

- L1 (unit/module) and L2 (simulated) sprints -- the loop is not the right tool; rely on the test suite and the simulated mini-lab.

The mandatory boundary is enforced at ship time by `.codex/skills/ship/SKILL.md`. The contract field is set at planner time by `.claude/commands/sprint.md`.

---

## 8. UI Docking Point

The device loop produces evidence consumed by:

- the `/ship` evaluator step in `.codex/skills/ship/SKILL.md` (Step 2 verification, blocker on missing device-loop evidence for L3)
- the trace log entry written in Step 7 of the same skill (`docs/plans/changelog.md`)
- the `docs/platforms/harmony/test-signing-ledger.md` ledger (Harmony lane evidence notes)

The protocol does not introduce any UI surface of its own.

---

## 9. Deferred Scope

This protocol does not claim:

- Harmony `uitest` IME automation for free-form text -- explicitly avoided in favor of preset entry
- automated screenshot-diff regression -- screenshots remain support evidence, not gating evidence
- agent-side pixel observation -- the operator remains the visual evaluator (see `docs/specs/harness-manifesto.md` §7 belief 6)
- Android instrumented-test harness coverage -- a future Android L3 sprint may codify this and update §5.2
- iOS lane -- not yet started; will be added when the iOS port reaches L3

---

## 10. Drift Guardrail

Before changing any of the following, update this protocol first:

- the eight loop phases or their order
- the evidence schema fields in §6
- the "when required" boundary in §7
- the lane-instance tooling in §5

If a new lane (iOS, web, etc.) reaches L3, add a §5.x lane instance in the same session that the first L3 sprint ships.
