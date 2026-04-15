# L1 SIM Wave 12 Scheduler-Drawer Voice Reschedule Validation

Date: 2026-03-23
Status: Accepted
Owner: Codex

## Scope

Validate the Wave 12 scheduler-drawer-only reschedule slice:

1. the scheduler drawer mic can reschedule one clearly resolved task
2. no-match and ambiguous targeting stay explicit and non-mutating
3. ordinary SIM chat and audio-grounded chat still do not gain scheduler mutation authority

## Source of Truth

- `docs/plans/sim-tracker.md`
- `docs/plans/sim-wave12-execution-brief.md`
- `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/cerb/sim-scheduler/spec.md`
- `docs/cerb/sim-scheduler/interface.md`
- `docs/cerb/interface-map.md`

## What Was Checked

### 1. Scheduler drawer voice reschedule now executes inside scheduler scope

Examined the delivered scheduler path:

- `SimSchedulerViewModel` no longer hard-safe-fails every reschedule-style transcript from the scheduler drawer mic
- scheduler-owned target resolution now returns typed `Resolved` / `Ambiguous` / `NoMatch` results
- once a target is clearly resolved, the existing scheduler-local reschedule path still preserves reminder, conflict, motion, and calendar-attention behavior

Evidence:

- `SimSchedulerViewModelTest.processAudio scheduler drawer voice reschedule updates matched task`
- `FastTrackMutationEngineTest`

### 2. No-match and ambiguity remain safe-fail

Focused scheduler tests prove the new resolution seam does not guess:

- missing target stays explicit and non-mutating
- ambiguous target stays explicit and non-mutating

Evidence:

- `SimSchedulerViewModelTest.processAudio scheduler drawer voice reschedule safe fails on no match`
- `SimSchedulerViewModelTest.processAudio scheduler drawer voice reschedule safe fails on ambiguity`

### 3. General chat and audio-grounded chat remain non-mutating

Focused SIM chat regression coverage proves the Wave 12 authority did not leak:

- plain `GENERAL` chat with reschedule-like wording still behaves like chat only
- `AUDIO_GROUNDED` chat with reschedule-like wording still behaves like grounded discussion only
- neither path mutates scheduler state

Evidence:

- `SimAgentViewModelTest.general send with reschedule wording does not mutate scheduler state`
- `SimAgentViewModelTest.audio grounded send with reschedule wording does not mutate scheduler state`

## Verification Run

### Focused L1 validation pack

- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimAgentViewModelTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimSchedulerViewModelTest`
- `./gradlew :domain:scheduler:test --tests com.smartsales.prism.domain.scheduler.FastTrackMutationEngineTest`
- `./gradlew :app-core:compileDebugKotlin`

## Verdict

Accepted for the current Wave 12 implementation slice.

Current evidence is sufficient to say:

- scheduler-drawer voice reschedule now works for one clearly resolved target
- no-match and ambiguity stay explicit and non-mutating
- ordinary SIM chat and audio-grounded chat still do not inherit scheduler mutation rights
