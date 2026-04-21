# /sprint -- Unified Task Entrance

You are the unified task entrance for the harness operating protocol (`docs/specs/harness-manifesto.md`). All nontrivial work enters through you.

## Your job

1. Take the operator's task declaration (provided as $ARGUMENTS or asked interactively)
2. Classify the task intent
3. Produce a sprint contract keyed to that intent
4. Register in lane registry if needed
5. Route to execution

## Step 1: Collect the task declaration

If $ARGUMENTS is empty, ask the operator:
- What are you trying to build or change?
- Which feature area does this touch?
- Is this a net-new feature, a behavior tweak, or pure aesthetic polish on an already-shipped surface?

## Step 2: Classify the task intent

Read the task declaration and classify it into exactly one intent:

| Intent | Signal | Evidence focus |
|--------|--------|----------------|
| **dataflow** | New feature, behavior change, pipeline contract, typed mutations, repository/flow/mapper/ViewModel/state change, ASR/LLM pipeline, data layer. Owns the minimal UI surface needed to exercise the pipeline (utilitarian, not polished). | pipeline contract + critical telemetry joints + mini-lab scenarios. Screenshots are support only. |
| **cosmetic** | Pure aesthetic work on UI elements **already registered** in `docs/specs/ui_element_registry.md`. Spacing, typography, color, motion, style-guide compliance, theme tokens, Compose modifiers, ArkTS style props. Zero behavior change. | screenshots (authoritative) + registry compliance. No pipeline section. |
| **hybrid** | Small tweak on an **already-shipped** surface that genuinely touches both. Size-capped: **≤3 files total**. Must list both sub-intents explicitly at contract time. | union of both: joints from dataflow + screenshots from cosmetic. No bypass on either. |

**Hard rules:**

1. **Net-new feature work must be `dataflow`.** Cosmetic and hybrid cannot be the entry contract for a new feature. If the feature doesn't exist yet, the first contract is dataflow; polish comes in a follow-up cosmetic sprint.
2. **`cosmetic` cannot create new UI elements.** It only restyles elements already registered in `docs/specs/ui_element_registry.md`. New elements require a dataflow sprint (which owns the minimal UI surface).
3. **`cosmetic` cannot touch dataflow files.** Any edit to files matching `**/*ViewModel.kt`, `**/*Repository.kt`, `**/repository/**`, `**/data/**`, `**/domain/**`, `**/flow/**`, `**/*Mapper.kt`, `**/*UseCase.kt`, or equivalent ArkTS state/service files invalidates the contract. Re-declare as dataflow.
4. **`cosmetic` that uncovers a behavior bug halts immediately.** No mid-sprint scope change. Close the cosmetic contract as deferred, open a new dataflow contract.
5. **`hybrid` is capped at ≤3 files.** If the diff grows beyond the cap, split into dataflow + cosmetic. Cap enforcement is at ship time; planner should reject hybrid for any task that obviously can't fit.
6. **`hybrid` is not a shortcut for new features.** Hybrid is only valid against already-shipped surfaces.

**Drift signal:** if `hybrid` contracts exceed 30% of ships in a rolling window of 20 ships (tracked via `docs/plans/changelog.md` lane + intent entries), the harness flags re-enforcement and the next `/sprint` invocation rejects hybrid until the ratio falls back below threshold. This keeps hybrid from becoming the default.

### Engine mapping

The intent axis classifies *contracts*. Execution still uses engines (per `docs/specs/harness-manifesto.md` §5). Mapping:

- `dataflow` → Backend/Pipeline engine (proven); planner/operator/evaluator roles per manifesto §5
- `cosmetic` → UI execution (engine not yet proven; use closest existing UI workflow as interim; evidence is `ui-visible` screenshots from operator)
- `hybrid` → runs both, sequenced dataflow-first then cosmetic, within one contract

## Step 3: Read the Backend-First Reading Order

Before producing the contract, read docs in this order (from `docs/specs/harness-manifesto.md` §2):

1. Core-flow doc (`docs/core-flow/[feature]-flow.md`)
2. Pipeline/data contract (`docs/cerb/[feature]/interface.md`) -- dataflow and hybrid only
3. Verification boundary (evidence class: L1 unit/module, L2 simulated, L3 on-device)
4. UI docking surface (`docs/cerb/[feature]/spec.md`) -- reference only; cosmetic additionally reads `docs/specs/ui_element_registry.md` and `docs/specs/style-guide.md`
5. Device-loop protocol (`docs/specs/device-loop-protocol.md`) -- required reading when evidence class is L3 on `lane: harmony` or `lane: android`

If the core-flow doc does not exist for a dataflow task, flag this to the operator. That is the first deliverable, not a reason to skip ahead.

## Step 4: Produce the sprint contract

Use the template keyed to the declared intent. All templates share the header block; only the body differs.

### Shared header (all intents)

```markdown
### Sprint Contract: [Slice Name]

**Intent**: [dataflow | cosmetic | hybrid]
**Branch**: [develop | feature/{name} | platform/harmony]
**Evidence class**: [L1 | L2 | L3]
**Device loop**: [required (L3 on lane=harmony|android) | n/a]
**Device-loop joints**: [list; omit if Device loop is n/a]
**Date**: [today]

**Scope**:
- [What will be built -- specific, bounded]

**Success criteria**:
- [What the evaluator will check]

**Deferred scope**:
- [What this sprint explicitly does NOT build or claim]

**Roles**:
- Planner: [who classified and produced this contract]
- Operator: [who will execute]
- Evaluator: [who will verify -- structurally separate from operator]
```

### Dataflow body

```markdown
**Pipeline contract**:
- [Typed mutation 1]
- [Typed mutation 2]

**Critical dataflow joints**:
1. [Joint 1]
2. [Joint 2]
3. [Joint 3]

**Mini-lab scenarios**:
- [Scenario A -- expected outcome]
- [Scenario B -- expected outcome]
- [Negative scenario -- expected safe-fail]

**Minimal UI surface**:
- [Utilitarian UI added/touched to exercise the pipeline; may be unpolished]
- [Explicitly: no style-guide compliance required here; polish is a follow-up cosmetic sprint]
```

### Cosmetic body

```markdown
**Target UI elements** (all must already exist in `docs/specs/ui_element_registry.md`):
- [element ref 1]
- [element ref 2]

**Aesthetic changes**:
- [Spacing / typography / color / motion / modifier tweak 1]
- [Tweak 2]

**Registry compliance**:
- [Style-guide section(s) this aligns with, e.g. `docs/specs/style-guide.md#spacing-tokens`]

**Screenshot evidence plan**:
- [Before/after screenshots per target element]
- [Device or emulator the shots will be taken on]

**File-path invariant**:
- Diff must touch only Compose modifier files, theme files, drawables, ArkTS style props, the registry itself, or style-guide docs. Any hit on ViewModel/Repository/flow/mapper/state files invalidates the contract.
```

### Hybrid body

```markdown
**Size cap**: ≤3 files total. Declared file list:
- [file 1]
- [file 2]
- [file 3]

**Dataflow sub-intent** (one line):
- [what behavior is changing]

**Dataflow joints**:
1. [Joint 1]
2. [Joint 2]

**Cosmetic sub-intent** (one line):
- [what aesthetic is changing]

**Cosmetic target** (must exist in `ui_element_registry.md`):
- [element ref]

**Mini-lab scenario** (single, focused):
- [Scenario -- expected outcome]

**Screenshot evidence plan**:
- [Before/after for the cosmetic slice]

**Not-a-new-feature assertion**:
- [One line confirming the surface is already shipped; cite the prior dataflow sprint or PR]
```

## Step 5: Confirm with operator

Present the contract for approval before execution. Surface intent-specific warnings explicitly:

- **dataflow + `Device loop: required`**: "L3 sprint; Codex will block `/ship` step 2 unless the device-loop evidence schema (`docs/specs/device-loop-protocol.md` §6) is attached to the handoff."
- **cosmetic**: "Codex will block `/ship` step 2 if the diff touches any dataflow file (ViewModel/Repository/flow/mapper/state). If behavior needs to change, re-declare as dataflow before starting."
- **hybrid**: "Codex will block `/ship` step 2 if the diff exceeds 3 files or if the declared scope implies a net-new feature. If unsure, split into dataflow + cosmetic."

If the operator rejects or modifies the contract, update and re-present. Do not proceed without an approved contract.

## Step 6: Route to execution

After approval:
- **dataflow** → follow `.agent/workflows/engine-backend-[engine].md` Phase 2 (Operator)
- **cosmetic** → interim UI workflow; evaluator is `ui-visible` (operator supplies real-device or emulator screenshots)
- **hybrid** → run dataflow slice first, verify joints, then cosmetic slice, then collect screenshots. One contract, two sub-phases, one ship.
