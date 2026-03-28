# Core Flow: SIM Scheduler Path A Overlay

> **Role**: Core Flow
> **Authority**: Behavioral North Star for the SIM scheduler slice
> **Status**: Ahead of Spec and Code
> **Development Chain**: Core Flow -> Spec -> Code -> PU Test -> Fix Spec -> Fix Code
> **Scope**: SIM-specific narrowing of scheduler behavior to the allowed Path A slice inside the standalone app.
> **Companion North Star**: `docs/core-flow/scheduler-fast-track-flow.md`
> **Testing Directive**: Validate one SIM scheduler branch or one safety branch per run. Do not treat this file as a rewrite of the full scheduler north star.

---

## How To Read This Doc

This document is a **SIM overlay**, not a full replacement for the existing scheduler north star.

Interpretation rule:

- use `docs/core-flow/scheduler-fast-track-flow.md` for baseline Path A behavioral laws
- use this file to understand what SIM keeps, narrows, or explicitly excludes

If this file conflicts with lower specs or code, treat the lower layers as drift candidates first.

---

## Downstream Sync Targets

| Layer | Responsibility | Must Eventually Reflect This Core Flow |
|------|----------------|-----------------------------------------|
| **Spec** | How the behavior is encoded | `docs/cerb/sim-scheduler/spec.md`, `docs/cerb/sim-scheduler/interface.md` |
| **Code** | Delivered behavior | `SimSchedulerViewModel`, SIM scheduler wiring, Path A-only scheduler integration |
| **PU Test** | Behavioral validation | future SIM Path A scheduler tests and isolation checks |

---

## What SIM Inherits From The Main Scheduler Core Flow

SIM inherits these Path A truths:

- no fabricated time
- no silent drop for real schedulable intent
- conflict-visible creation remains valid
- bad target resolution must fail safely
- reschedule remains replacement-oriented
- scheduler cards keep separate urgency/conflict/completion meanings

SIM must not break those laws just because it is a smaller app.

---

## What SIM Excludes

SIM V1 explicitly excludes these expectations from its required delivered behavior:

- Path B memory highway
- CRM/entity enrichment
- plugin-driven scheduler re-entry
- smart-shell-specific scheduler session behavior

If the smart app implements those behaviors elsewhere, SIM is not required to carry them.

---

## Non-Negotiable Invariants

1. **SIM scheduler is Path A-backed**: normal scheduler use in SIM must complete without requiring Path B.
2. **Path B absence does not reduce Path A truth**: SIM still must obey Path A laws around time, conflict, and safe failure.
3. **Conflict-visible creation remains valid**: conflict does not become a silent reject just because SIM is simplified.
4. **Reschedule safety remains valid**: no-match or ambiguous targeting must not mutate state.
5. **UI reuse does not justify runtime overreach**: reusing the scheduler drawer UI does not authorize importing unrelated smart runtime collaborators.
6. **SIM may defer non-core scheduler extras**: inspiration shelf, tips, completed-memory merge, and smart voice adjuncts may be deferred if they would reintroduce contamination or overbuild.
7. **Scheduler-drawer voice reschedule remains scheduler-scoped**: if SIM accepts a voice reschedule from the scheduler drawer mic, that authority belongs only to the scheduler drawer Path A lane and must not silently widen ordinary chat or audio routes into scheduler mutation surfaces.

---

## SIM Branch Set

### Included Branches

- single-task explicit relative-time exact create
- exact create
- conflict-visible create
- delete
- reschedule
- scheduler-drawer voice reschedule
- explicit fast-fail feedback

### Optional / Deferred Branches

- inspiration shelf behavior
- completed-memory merge projection
- tips generation
- smart badge-driven voice entry as a required V1 route

If a branch is deferred, SIM must do so explicitly rather than pretending the branch still exists.

---

## Canonical Valves

These valve names are the behavioral checkpoints for downstream specs and tests.
The exact telemetry names in code may differ today, but they should converge toward this model.

- `SIM_SCHEDULER_ENTERED`
- `SIM_PATH_A_REQUESTED`
- `TASK_EXTRACTED`
- `TARGET_RESOLVED`
- `TARGET_MISSING`
- `TARGET_AMBIGUOUS`
- `CONFLICT_EVALUATED`
- `DB_WRITE_EXECUTED`
- `SIM_SCHEDULER_RENDERED`
- `SIM_FAST_FAIL_RETURNED`
- `SIM_PATH_B_SUPPRESSED`

---

## Master Routing Flow

This is the top-level routing model for the SIM scheduler slice.

```text
                    +----------------------+
                    | Scheduler Drawer Open|
                    +----------+-----------+
                               |
                               v
                    +----------------------+
                    | SIM Path A Request   |
                    +----------+-----------+
                               |
                               v
                    +----------------------+
                    | Path A Classification|
                    +----+------+------+---+
                         |      |      |
                         v      v      v
                   [Create] [Delete] [Reschedule]
                         |      |      |
                         v      v      v
                +-----------------------------+
                | Conflict / Target Checks    |
                +----------+------------------+
                           |
              +------------+-------------+
              |                          |
              v                          v
      [Safe Failure / Feedback]   [DB Write Executed]
                                             |
                                             v
                                  +----------------------+
                                  | Scheduler UI Rendered|
                                  +----------+-----------+
                                             |
                                             v
                                  [Remain within SIM]
```

---

## Safety Branches

### Branch-C1: Single-Task Explicit Relative-Time Create

If the scheduler drawer mic receives one clear create transcript with one lawful explicit relative-time phrase:

- SIM should resolve the exact relative time locally against `nowIso`
- if stripping that phrase leaves one non-empty task body, SIM should create one exact task through a scheduler-owned deterministic branch before `Uni-M` / `Uni-A` / `Uni-B` / `Uni-C`
- this branch is limited to explicit duration phrasing and must not widen into fuzzy phrases such as `待会儿`

### Branch-C2: Malformed Explicit Relative-Time Create

If that explicit relative-time phrase resolves to an exact time but stripping it leaves no task body:

- no scheduler mutation occurs
- SIM must return scheduler-owned safe-fail feedback
- it must not fall through into inspiration-style or generic chat-style failure copy

### Branch-S1: Path B Suppressed

If a reused implementation attempts to route into Path B behavior:

- SIM must suppress or bypass that path
- normal Path A completion must remain valid

### Branch-S2: Ambiguous Target

If a delete or reschedule target is ambiguous:

- no scheduler mutation occurs
- explicit failure or clarification-visible feedback is required
- if the request came from the scheduler drawer voice path, the failure must still remain inside scheduler scope rather than bouncing into unrelated chat/audio mutation paths

### Branch-S3: No Match

If a delete or reschedule target cannot be resolved:

- no scheduler mutation occurs
- explicit safe failure is required

### Branch-S4: Scheduler-Drawer Voice Target Resolution

If the scheduler drawer mic receives a reschedule-style transcript:

- SIM must resolve the target against scheduler-owned global task truth using confidence-gated matching
- the candidate space must come from a scheduler-owned active retrieval index derived from all non-done tasks, not a 7-day UI window and not a separate session-memory lane
- that retrieval index may expose a bounded shortlist context pack to the extractor, but the extractor remains advisory only
- it may use normalized title, persisted participant/location metadata, notes digest, and a weak recent-task-set prior as supporting signals
- current selected/opened task state and current visible page/date must not become semantic authority for target choice
- it must not rely on plain exact-title or SQL-only equality as the product contract
- raw model output alone must not authorize a write; the scheduler-owned final gate must still accept `resolved / ambiguous / no-match`
- once one target is resolved, explicit day+clock phrasing such as `明天早上8点` must be accepted through scheduler-owned deterministic parsing before any model-led exact-time fallback
- once one target is resolved, explicit delta phrasing such as `推迟1小时` / `提前半小时` must anchor to that task's current persisted start time rather than `nowIso`
- low-confidence resolution must degrade to explicit safe failure, not guessed mutation

The same global target-resolution law also applies to the task-scoped follow-up reschedule lane:

- follow-up reschedule may pass the currently bound task set only as a weak recent-task prior
- selected-task UI state may still drive quick actions such as delete/done, but it must not override global reschedule resolution

---

## Done-When Definition

The SIM scheduler overlay is behaviorally ready only when:

- the standalone app delivers the approved Path A branch set
- the lack of Path B does not break Path A truth
- scheduler drawer behavior remains Prism-like
- the implementation does not silently depend on smart-only scheduler runtime behavior
