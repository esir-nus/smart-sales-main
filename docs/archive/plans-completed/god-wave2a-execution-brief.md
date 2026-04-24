# God Wave 2A Execution Brief

**Status:** L1 Accepted
**Date:** 2026-03-24
**Wave:** 2A
**Mission:** `SchedulerLinter.kt` structural cleanup
**Primary Tracker:** `docs/projects/god-file-cleanup/tracker.md`
**Structure Law:** `docs/specs/code-structure-contract.md`
**Current Reading Priority:** Historical execution reference only; not current source of truth.
**Current Active Truth:** `docs/projects/god-file-cleanup/tracker.md`, `docs/plans/tracker.md`, `docs/specs/code-structure-contract.md`, `docs/core-flow/scheduler-fast-track-flow.md`, `docs/core-flow/sim-scheduler-path-a-flow.md`, `docs/cerb/scheduler-path-a-spine/spec.md`, `docs/cerb/scheduler-path-a-uni-a/spec.md`, `docs/cerb/scheduler-path-a-uni-b/spec.md`, `docs/cerb/scheduler-path-a-uni-c/spec.md`, `docs/cerb/scheduler-path-a-uni-d/spec.md`, `docs/cerb-ui/scheduler/contract.md`, `docs/cerb/interface-map.md`
**Historical Deprecated Context:** `docs/cerb/sim-scheduler/spec.md`
**Validation Report:** `docs/reports/tests/L1-20260324-god-wave2a-scheduler-linter.md`

---

## 1. Purpose

Wave 2A is the first business-logic cleanup slice in Wave 2.

It targets `SchedulerLinter.kt`, which currently mixes normalization, temporal parsing, DTO assembly, and validation/legacy support inside one oversized domain file.

Wave 2A should reduce that file into stable role-based support files while keeping the public `SchedulerLinter` seam source-compatible.

---

## 2. Current Active Truth

Wave 2A should now be read against the shared scheduler doc set, not the deprecated SIM scheduler shard.

Current active truth:

- `docs/core-flow/scheduler-fast-track-flow.md`
- `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/cerb/scheduler-path-a-spine/spec.md`
- `docs/cerb/scheduler-path-a-uni-a/spec.md`
- `docs/cerb/scheduler-path-a-uni-b/spec.md`
- `docs/cerb/scheduler-path-a-uni-c/spec.md`
- `docs/cerb/scheduler-path-a-uni-d/spec.md`
Wave 2A historical context at the time:

- `docs/cerb/sim-scheduler/spec.md`

Wave 2A also repaired the stale `docs/cerb/interface-map.md` reference that had pointed `SchedulerLinter` to a nonexistent shard.

---

## 3. Wave 2A Law

Wave 2A may do:

- public-seam reduction for `SchedulerLinter.kt`
- extraction of stable normalization/time helpers
- extraction of parse-lane helpers across the current Uni and follow-up paths
- extraction of DTO assembly and validation/legacy support helpers
- local structure-test and guardrail updates needed for the accepted shape
- tracker/interface-map/doc sync for the delivered split

Wave 2A must **not** do:

- public `SchedulerLinter` API changes
- scheduler behavior expansion beyond the already accepted Path A / SIM contract
- `SimRescheduleTimeInterpreter.kt` cleanup except for minimal helper movement required by the accepted linter split
- new architecture layers or abstractions added only to satisfy LOC pressure

---

## 4. Delivered Structure

Wave 2A leaves `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinter.kt` as the public seam file.

Delivered extraction map:

- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterSupport.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterParsingSupport.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterLegacySupport.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterLegacyContracts.kt`

Additional Wave 2A adjustments:

- `SchedulerLinter.kt` now measures `100 LOC`, below the transitional service/manager/linter/gateway budget
- `GodStructureGuardrailTest` now treats `SchedulerLinter.kt` as an accepted active Wave 2 row
- `SchedulerLinterStructureTest` enforces the new split
- existing callers remain source-compatible through the public `SchedulerLinter` seam

---

## 5. Verification Status

Wave 2A acceptance uses focused domain verification plus guardrail coverage.

Executed commands:

- `./gradlew :domain:scheduler:test --tests com.smartsales.prism.domain.scheduler.SchedulerLinterTest --tests com.smartsales.prism.domain.scheduler.SchedulerLinterStructureTest`
- `./gradlew :domain:scheduler:compileKotlin`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`

---

## 6. Wave 2A Acceptance Bar

Wave 2A is complete only when:

- `SchedulerLinter.kt` is under the service/manager/linter/gateway budget or backed by a valid active exception
- the tracker row moves to `Accepted`
- the dead interface-map reference is repaired
- focused linter behavior tests and the structure test stay green
- the public `SchedulerLinter` seam remains source-compatible

---

## 7. Related Documents

- `docs/projects/god-file-cleanup/tracker.md`
- `docs/plans/tracker.md`
- `docs/plans/god-wave2-execution-brief.md`
- `docs/specs/code-structure-contract.md`
- `docs/cerb/interface-map.md`
- `docs/reports/tests/L1-20260324-god-wave2a-scheduler-linter.md`
