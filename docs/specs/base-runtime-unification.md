# Base Runtime Unification

> **Role**: Canonical product/runtime model
> **Status**: Active planning law
> **Date**: 2026-03-31
> **Purpose**: Remove non-Mono SIM/full behavioral drift by defining one shared base runtime and one later Mono augmentation layer.
> **Related Docs**:
> - `docs/plans/base-runtime-unification-campaign.md`
> - `docs/reports/20260331-base-runtime-unification-drift-audit.md`

---

## 1. Product Truth

There is no longer a valid non-Mono split between a `SIM version` and a `full version`.

The canonical model is:

- **Base Runtime**: the shared shell, shared UI/UX, shared Tingwu/audio lane, shared Path A scheduler lane, and bounded local/session continuity needed for normal product use
- **Mono Layer**: the later architecture augmentation that adds deeper Kernel-owned memory, CRM/entity loading, Path B scheduler enrichment, plugin/tool runtime, and related memory-backed intelligence

This means:

- non-Mono work must target one product truth
- Mono is an augmentation layer, not a second product line
- legacy `SIM` naming may remain temporarily in code/docs, but it must not be used as permission to fork non-Mono behavior
- a separate SIM entry/runtime boundary may still remain real in implementation, but that boundary does not create a second non-Mono product truth

---

## 2. Base Runtime Boundary

The base runtime includes:

- shared shell/UI/UX
- discussion chat baseline
- Tingwu/audio artifact flow
- Path A scheduler create/delete/reschedule behavior
- local session/history
- bounded follow-up continuity
- small Path A carry-forward context already allowed by scheduler core flow

The base runtime does **not** require:

- Kernel-owned session-memory architecture
- CRM/entity loading
- Path B scheduler enrichment
- plugin/tool runtime
- smart-agent orchestration

Rule:

- bounded short-lived local/session continuity is not automatically Mono
- if the behavior can ship without the deeper memory/entity/plugin architecture, it belongs to the base runtime

---

## 3. Mono Boundary

The Mono layer begins where the product depends on the deeper architecture rather than the shared base shell.

Mono-owned capabilities include:

- Kernel-owned session memory as a first-class operating lane
- CRM/entity loading and deeper RAM assembly
- Path B scheduler enrichment
- plugin/tool runtime
- memory-backed scheduler intelligence beyond the shared Path A/base runtime contract

Rule:

- Mono may augment the base runtime
- Mono must not redefine shared shell/UI truth
- Mono must not force a second non-Mono implementation path

---

## 4. Naming and Ownership Rule

Current `SIM` docs and `Sim*` code paths are retained as the practical baseline because they currently lead the shipped shell/UI direction.

Interpretation rule:

- treat current `SIM` shell/scheduler/audio docs as the best available base-runtime baseline where they are ahead
- do not treat the `SIM` label itself as proof that the behavior is permanently separate
- do not treat a standalone SIM root/composition boundary as proof of a second non-Mono product line
- do not require repo-wide renaming before unification is complete

Production root unification landed on 2026-04-01:

- `MainActivity` is now the single production activity
- `RuntimeShell` is now the single production shell host
- former `AgentMainActivity`, `SimMainActivity`, `AgentShell`, and `SimShell` are retired from production ownership
- the current canonical base-runtime owners under that shell are `SimAgentViewModel` for chat/session/audio/follow-up and `SimSchedulerViewModel` for scheduler/reminder-banner/top-summary behavior

Temporary legacy wrappers may remain only where deeper Mono or legacy-full internals are still being reduced, but they are compatibility owners, not product-truth owners.

Current wrapper-debt hosts in code:

- `app-core/.../AgentViewModel.kt`
- `app-core/.../drawers/scheduler/SchedulerViewModel.kt`

These files may remain as compatibility hosts while shared base-runtime truth lives in current docs and lower support files.
They must not remain the default owners for shared composables or production shell wiring. Future non-Mono work must not reintroduce separate SIM-vs-full product truth.

---

## 5. Shared UI Contract Rule

Shared presentation truth should continue to flow through existing UI seams:

- `IAgentViewModel`
- `ISchedulerViewModel`
- `AgentIntelligenceScreen`
- `SchedulerDrawer`

Rules:

- do not create separate SIM/full UI interfaces
- do not fork shared UI behavior for non-Mono work
- keep shell/UI decisions capability-layered, not version-layered
- shared composables must receive explicit `IAgentViewModel` / `ISchedulerViewModel` inputs rather than silently defaulting to legacy wrapper owners
- shell-only adjunct state such as SIM transcript-reveal or voice-draft control must be passed explicitly from `RuntimeShell` rather than inferred by downcasting shared UI to a concrete ViewModel

---

## 6. Delivery Classification Rule

Future work must be classified as exactly one of:

1. **Base / Shared**
2. **Mono-only**
3. **Legacy wrapper debt**

Forbidden category:

- `SIM-only but not Mono`
- `full-only but not Mono`

If a proposal cannot be expressed cleanly in the three allowed categories, it is probably drifting.

---

## 7. Organic God-File Teardown Rule

This unification campaign may trigger structural cleanup, but only organically.

A file qualifies for teardown only when it mixes one or more of:

- shared UI truth
- base runtime behavior
- Mono-only behavior
- legacy wrapper glue

Rules:

- refactor by ownership seam, not by LOC alone
- do not widen this into a repo-wide beautification campaign
- large files that are not blocking unification may be deferred

---

## 8. Acceptance Rule

The unification is directionally successful only when:

- non-Mono behavior is specified once, not once for SIM and again for full
- the base-vs-Mono boundary is explicit
- future engineers/agents do not need to decide whether a non-Mono feature is for `SIM` or `full`
- legacy wrappers no longer define product truth
