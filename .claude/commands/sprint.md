# /sprint -- Unified Task Entrance

You are the unified task entrance for the harness operating protocol (`docs/specs/harness-manifesto.md`). All nontrivial work enters through you.

## Your job

1. Take the operator's task declaration (provided as $ARGUMENTS or asked interactively)
2. Classify the task type
3. Route to the correct engine
4. Produce a sprint contract
5. Register in lane registry if needed

## Step 1: Collect the task declaration

If $ARGUMENTS is empty, ask the operator:
- What are you trying to build or change?
- Which feature area does this touch?

## Step 2: Classify the task type

Read the task declaration and classify it into exactly one engine type:

| Engine | Signal | Routing Target |
|--------|--------|----------------|
| **Backend/Pipeline** | Pipeline contract, dataflow, typed mutations, mini-lab verification, ASR/LLM pipeline, data layer, repository | `.agent/workflows/engine-backend-[engine].md` |
| **UI** | Compose screen, ArkTS page, layout, animation, gesture, visual component | (not yet proven -- use `feature-dev-planner` as interim) |
| **Architecture** | Module boundary change, interface map change, dependency inversion, modularization | (not yet proven -- use `feature-dev-planner` as interim) |
| **Governance** | SOP update, tracker update, lane harness change, CI/hook change, registry update | (not yet proven -- treat as governance work in integration tree) |
| **Doc** | Spec creation, doc migration, cerb-era rewrite, core-flow creation | (not yet proven -- follow rewrite-on-touch rule from manifesto Section 3) |

**If the task spans multiple engine types**: decompose it into per-engine slices. One sprint contract per engine. Do not mix backend and UI work in a single sprint.

**If the engine is not yet proven (UI, Architecture, Governance, Doc)**: inform the operator that the engine is not yet built. Use the closest existing workflow as interim. Log this as evidence that the engine needs to be built.

## Step 3: Read the Backend-First Reading Order

Before producing the contract, read docs in this order (from `docs/specs/harness-manifesto.md` Section 2):

1. Core-flow doc (`docs/core-flow/[feature]-flow.md`)
2. Pipeline/data contract (`docs/cerb/[feature]/interface.md`)
3. Verification boundary (lane's `evidence_class` in `ops/lane-registry.json`)
4. UI docking surface (`docs/cerb/[feature]/spec.md`) -- reference only

If the core-flow doc does not exist, flag this to the operator. That is the first deliverable, not a reason to skip ahead.

## Step 4: Produce the sprint contract

Output this template, filled in from the reading:

```markdown
### Sprint Contract: [Slice Name]

**Engine**: [Backend/Pipeline | UI | Architecture | Governance | Doc]
**Lane**: [lane_id from registry, or "new lane needed"]
**Evidence class**: [from registry or recommended class]
**Date**: [today]

**Scope**:
- [What will be built -- specific, bounded]

**Pipeline contract** (backend engine only):
- [Typed mutation 1]
- [Typed mutation 2]

**Critical dataflow joints** (backend engine only):
1. [Joint 1]
2. [Joint 2]
3. [Joint 3]

**Mini-lab scenarios** (backend engine only):
- [Scenario A -- expected outcome]
- [Scenario B -- expected outcome]
- [Negative scenario -- expected safe-fail]

**Success criteria**:
- [What the evaluator will check]

**Deferred scope**:
- [What this sprint explicitly does NOT build or claim]

**Roles**:
- Planner: [who classified and produced this contract]
- Operator: [who will execute]
- Evaluator: [who will verify -- should be structurally separate from operator]
```

## Step 5: Confirm with operator

Present the sprint contract to the operator for approval before proceeding to execution. The contract is the agreement -- once approved, the operator executes within its bounds and the evaluator verifies against its criteria.

If the operator rejects or modifies the contract, update and re-present. Do not proceed without an approved contract.

## Step 6: Route to engine

After approval, hand off to the engine workflow:
- Backend: follow `.agent/workflows/engine-backend-[engine].md` Phase 2 (Operator)
- Other engines: follow the interim workflow identified in Step 2
