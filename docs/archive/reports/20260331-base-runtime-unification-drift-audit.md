# Base Runtime Unification Drift Audit

> **Date**: 2026-03-31
> **Purpose**: Convert the SIM/full version drift problem into an explicit ownership audit for one base runtime plus a later Mono layer.
> **North Star**: `docs/specs/base-runtime-unification.md`

---

## 1. Audit Scope

Inspected docs:

- `docs/plans/tracker.md`
- `docs/to-cerb/sim-standalone-prototype/mental-model.md`
- `docs/to-cerb/sim-standalone-prototype/concept.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/core-flow/scheduler-fast-track-flow.md`
- `docs/core-flow/system-session-memory-flow.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `docs/cerb/sim-scheduler/spec.md`
- `docs/cerb/sim-scheduler/interface.md`
- `docs/cerb/interface-map.md`
- `docs/specs/Architecture.md`
- `docs/specs/code-structure-contract.md`
- `docs/projects/god-file-cleanup/tracker.md`

Inspected code:

- `app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt`

Current observed seam sizes:

| File | LOC | Audit Reading |
|------|-----|---------------|
| `AgentShell.kt` | 381 | legacy full host still acts as a product-truth surface |
| `AgentViewModel.kt` | 628 | large legacy full host mixing shared UI-facing behavior with deeper runtime concerns |
| `SchedulerViewModel.kt` | 375 | legacy full scheduler host still mixes shared UI projection with memory merge and voice/orchestrator behavior |
| `SimShell.kt` | 239 | already cleaned into a host-style seam; use as reference baseline |
| `SimAgentViewModel.kt` | 699 | large in raw LOC, but current campaign evidence shows most heavy roles are already delegated out |
| `SimSchedulerViewModel.kt` | 257 | already cleaned into a host-style seam; use as reference baseline |

---

## 2. Core Findings

### Finding A: The repo already has shared UI seams

Shared presentation reuse is already real:

- `IAgentViewModel`
- `ISchedulerViewModel`
- `AgentIntelligenceScreen`
- `SchedulerDrawer`

This means the product does **not** need two separate UI contracts. The drift problem is mainly one of runtime ownership and doc framing.

### Finding B: The current SIM shell is ahead for shell/UI/UX

The current SIM path already owns:

- the stronger shell identity
- the shared chat/scheduler/audio baseline
- the more disciplined shell routing split

Practical conclusion:

- current SIM docs/code should be treated as the **base-runtime UX baseline**
- the legacy full path should catch up through wrappers and migration, not by redefining product truth

### Finding C: Docs still frame SIM as a separate product line

Current SIM concept and mental-model docs still read primarily as:

- a separate prototype app
- a separate stripped-down mode

That framing was useful for isolation, but it now causes planning drift for non-Mono work because the repo already shares UI and product family behavior.

### Finding D: The real intentional difference is architectural depth, not shell truth

Valid deeper differences map cleanly to Mono:

- Kernel-owned session memory
- CRM/entity loading
- Path B scheduler enrichment
- plugin/tool runtime

Invalid difference pattern:

- any non-Mono shell/chat/scheduler/audio behavior that still forks into SIM-only vs full-only

### Finding E: Scheduler context needs one important clarification

`docs/core-flow/scheduler-fast-track-flow.md` already allows **small session memory** for Path A follow-up.

Interpretation:

- bounded Path A carry-forward context remains valid in the base runtime
- Mono begins at the deeper Kernel/session-memory/entity/plugin architecture layer
- the team must not misclassify every short-lived follow-up context as Mono

---

## 3. Drift Classification

### Must unify now

- shell/UI/UX truth
- chat/audio/scheduler baseline ownership
- non-Mono delivery language in docs
- planning language that still requires choosing SIM or full for base features

### Valid Mono-only difference

- deeper memory architecture
- CRM/entity-linked intelligence
- Path B scheduler enrichment
- plugin/tool runtime

### Temporary wrapper debt

- `AgentShell.kt` as a legacy full host
- `AgentViewModel.kt` as a legacy full host for the older runtime path
- `SchedulerViewModel.kt` as a legacy full scheduler host

### Invalid drift

- introducing new non-Mono behavior as SIM-only
- introducing new non-Mono behavior as full-only
- letting legacy full wrappers define base product truth

---

## 4. Organic God-File Candidate Decisions

| File | Decision | Why |
|------|----------|-----|
| `AgentShell.kt` | Refactor candidate | legacy full shell host still reads like product truth instead of wrapper glue |
| `AgentViewModel.kt` | Refactor candidate | likely mixed seam between shared UI behavior, base runtime behavior, and deeper legacy runtime ownership |
| `SchedulerViewModel.kt` | Refactor candidate | likely mixed seam between shared scheduler UI projection and deeper memory/voice/orchestrator behavior |
| `SimShell.kt` | Keep as reference | already cleaned in the active pilot; use as convergence reference |
| `SimAgentViewModel.kt` | Keep for now | current problem is less raw host purity and more naming/product-truth framing; re-audit only when base-vs-Mono split reaches its implementation phase |
| `SimSchedulerViewModel.kt` | Keep as reference | already cleaned and aligned with the SIM-led base-runtime direction |
| `OnboardingScreen.kt` | Defer | very large, but not yet proven to block shell/chat/scheduler/audio unification |

Rule for the next implementation phase:

- only refactor the three legacy full-side candidates if the implementation audit proves they still mix shared UI truth, base runtime, Mono-only behavior, or wrapper glue in one seam

---

## 5. Resulting Direction

The repo should now treat the problem as:

- one **shared base runtime**
- one later **Mono augmentation layer**
- one **organic structural cleanup lane** on the remaining legacy full-side hosts

The repo should no longer treat the problem as:

- two parallel product truths called SIM and full

