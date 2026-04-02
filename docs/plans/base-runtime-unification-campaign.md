# Base Runtime Unification Campaign

> **Status**: Investigation package delivered; implementation Slices 1-5 landed
> **Date**: 2026-03-31
> **North Star**: `docs/specs/base-runtime-unification.md`
> **Current Reading Priority**: Active campaign memory for base-runtime unification; deprecated SIM-family references below are historical context only.
> **Audit**: `docs/reports/20260331-base-runtime-unification-drift-audit.md`

---

## 1. Campaign Goal

Unify the current SIM/full split into one canonical base runtime model so future non-Mono work no longer needs two behavioral truths.

This campaign also authorizes an **organic god-file teardown** only when a file blocks that unification by mixing:

- shared UI truth
- base runtime logic
- Mono-only logic
- legacy wrapper glue

---

## 2. Deliverables

- canonical product/runtime model
- drift audit
- doc sync map
- phased migration map
- god-file candidate decision list

Delivered in this round:

- `docs/specs/base-runtime-unification.md`
- `docs/reports/20260331-base-runtime-unification-drift-audit.md`
- this campaign doc

---

## 3. Doc Sync Map

Current authoritative docs for this campaign:

- `docs/specs/base-runtime-unification.md`
- `docs/plans/tracker.md`
- `docs/specs/prism-ui-ux-contract.md`
- `docs/cerb/interface-map.md`

Historical and deprecated campaign context only; do not use these as current governing truth:

- `docs/to-cerb/sim-standalone-prototype/mental-model.md`
- `docs/to-cerb/sim-standalone-prototype/concept.md`
- `docs/plans/sim-tracker.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `docs/cerb/sim-scheduler/spec.md`
- `docs/cerb/sim-scheduler/interface.md`
- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/sim-audio-chat/interface.md`
Supporting references:

- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/core-flow/scheduler-fast-track-flow.md`
- `docs/core-flow/system-session-memory-flow.md`

---

## 4. Phase Map

### Phase 1: Model lock

- state one canonical product model
- stop treating SIM/full as two non-Mono truths
- keep current SIM-led shell as the practical baseline while naming cleanup is deferred

### Phase 2: Drift audit

- classify shell/chat/scheduler/audio differences into:
  - unify now
  - Mono-only
  - temporary wrapper debt
  - invalid drift

### Phase 3: Runtime split definition

- define the stable target:
  - shared UI contracts stay `IAgentViewModel` and `ISchedulerViewModel`
  - one base runtime path
  - one future capability/profile seam for Mono enablement

### Phase 4: Organic god-file teardown

Primary audit targets:

- `AgentShell.kt`
- `AgentViewModel.kt`
- `SchedulerViewModel.kt`
- `SimAgentViewModel.kt` when SIM-only growth re-expands the shared host seam

Execution law:

- refactor only if the file is proven to mix shared truth, base runtime, Mono-only logic, or legacy wrapper glue
- do not widen into a general cleanup wave

### Phase 5: Wrapper retirement

- turn old full-side runtime owners into thin compatibility wrappers
- retire them only after the base runtime becomes the clear product truth

---

## 5. Campaign Acceptance

This campaign is ready to move into implementation only when:

- docs specify non-Mono behavior once
- the base-vs-Mono boundary is explicit
- the god-file candidate list is narrowed to real blockers
- future non-Mono work no longer needs the question: `is this for SIM or full?`

---

## 6. Implementation Landings

On 2026-03-31, Slice 1 of the implementation phase landed as a wrapper-only `AgentShell.kt` cleanup:

- `AgentShell.kt` now keeps only Hilt acquisition, lifecycle observation, and top-level callback wiring
- rendering moved to `AgentShellContent.kt`
- shell state/reducer helpers moved to `AgentShellState.kt` and `AgentShellReducer.kt`
- ghost-handle/right-stub support moved to `AgentShellSupport.kt`

Law for this landing:

- preserve current full-app behavior exactly
- treat the full shell as compatibility/wrapper debt, not product truth
- defer actual behavior convergence with the SIM-led base runtime to later evidence-led slices

On 2026-03-31, Slice 2 of the implementation phase landed as a wrapper-only `SchedulerViewModel.kt` cleanup:

- `SchedulerViewModel.kt` now keeps only public state ownership, collaborator construction, init wiring, and delegating entrypoints
- timeline and crossed-off memory projection moved to `SchedulerViewModelProjectionSupport.kt`
- legacy delete/cross-off/conflict helpers moved to `SchedulerViewModelLegacyActions.kt`
- scheduler-drawer audio ingress and pipeline status handling moved to `SchedulerViewModelAudioIngressCoordinator.kt`

Law for this landing:

- preserve current full-side scheduler behavior exactly
- treat the full scheduler host as compatibility/wrapper debt, not product truth
- defer any full-vs-SIM behavior convergence until later evidence-led slices
- if verification is blocked by unrelated compile failures outside the scheduler slice, record the block explicitly rather than forcing a false acceptance state

On 2026-03-31, Slice 3 of the implementation phase landed as a wrapper-only `AgentViewModel.kt` cleanup:

- `AgentViewModel.kt` now keeps only public state ownership, collaborator construction, init wiring, and delegating entrypoints
- session lifecycle and linked-audio hydration moved to `AgentSessionCoordinator.kt`
- pipeline send/confirm/result-reduction ownership moved to `AgentPipelineCoordinator.kt`
- task-board and direct tool execution moved to `AgentToolCoordinator.kt`
- host-owned mutable state access moved behind `AgentUiBridge.kt`
- hero dashboard / idle watcher support moved to `AgentRuntimeSupport.kt`
- debug scenario emission moved to `AgentDebugSupport.kt`

Law for this landing:

- preserve current full-side agent behavior exactly
- keep `IAgentViewModel` unchanged
- treat the full-side agent host as compatibility/wrapper debt, not product truth
- defer any SIM/full behavior convergence and any Mono extraction to later evidence-led slices

On 2026-03-31, Slice 4 of the implementation phase landed as a wrapper-only `SimAgentViewModel.kt` follow-up cleanup:

- `SimAgentViewModel.kt` now keeps public state ownership, collaborator construction, init wiring, and delegating entrypoints
- the SIM voice-draft lane moved to `SimAgentVoiceDraftCoordinator.kt`
- the existing `SimAgentUiBridge` in `SimAgentSessionCoordinator.kt` was extended minimally so voice-draft state, send-state reads, and toast/input writes still flow through one host-owned seam
- the host now measures `522 LOC`, so the temporary SIM ViewModel exception can retire back into the accepted guardrail lane

Law for this landing:

- preserve current SIM chat, follow-up, and Wave 14 voice-draft behavior exactly
- keep `IAgentViewModel` unchanged
- do not widen this slice into SIM/full behavior convergence or Mono extraction
- treat the SIM host reduction as wrapper cleanup that keeps future base-runtime unification work easier and less drift-prone

On 2026-03-31, Slice 5 of the implementation phase landed as a docs-and-guardrail truth lock:

- `docs/specs/base-runtime-unification.md`, `docs/cerb/interface-map.md`, and `docs/plans/tracker.md` now state one canonical non-Mono product truth more explicitly
- the SIM concept, mental model, tracker, and leading shell/scheduler/audio shards now keep their real standalone/runtime boundary language while also stating that they are the best available base-runtime baseline for non-Mono work
- `docs/specs/prism-ui-ux-contract.md` now states that a standalone SIM shard may remain a real implementation boundary without becoming a second UI/product truth
- a focused JVM doc guardrail now enforces that the authoritative unification docs and SIM baseline docs keep that framing visible

Law for this landing:

- preserve all current runtime behavior exactly
- do not rename the repo or remove real SIM-owned runtime boundaries
- do not reopen onboarding cleanup or new structural teardown in this slice
- keep the guardrail focused on authoritative docs and top-level framing rather than scanning the whole docs tree
