---
description: Backend/pipeline harness engine - plans, executes, and evaluates backend-first slices using mini-lab verification
last_reviewed: 2026-04-13
engine_type: backend
era: post-harness
---

# Backend Engine

> **Engine Type**: Backend/Pipeline
> **Proven By**: Harmony scheduler slice (`docs/platforms/harmony/scheduler-backend-first.md`)
> **Roles**: Planner, Operator, Evaluator
> **Evidence Classes**: `platform-runtime`, `contract-test`

This engine handles backend and pipeline tasks: dataflow verification, pipeline contract implementation, typed mutation work, and mini-lab bounded slices. It does not handle UI composition, governance, or doc-only work -- those require their own engines.

---

## Entry Point

This engine is invoked by the `/sprint` skill after task classification. Do not invoke it directly. If you are starting backend work without a sprint contract, stop and run `/sprint` first.

---

## Phase 1: Planner

The planner reads docs in **Backend-First Reading Order** and produces a sprint contract.

### 1.1 Read (in this order, no skipping)

| Step | Doc | Purpose |
|------|-----|---------|
| 1 | `docs/core-flow/[feature]-flow.md` | Behavioral north star -- what the pipeline means |
| 2 | `docs/cerb/[feature]/interface.md` | Pipeline contract -- what the backend guarantees, typed mutations |
| 3 | `ops/lane-registry.json` | Lane's `evidence_class` -- what proof looks like |
| 4 | `.agent/rules/lessons-learned.md` | Check for matching triggers in this domain |
| 5 | `docs/cerb/[feature]/spec.md` | Reference for intent only -- do not derive pipeline behavior from UI descriptions |

If the core-flow doc does not exist: **stop**. Create it first using the post-harness doc shape (pipeline contract, verification boundary, UI docking point, deferred scope).

### 1.2 Classify the slice

| Question | Answer |
|----------|--------|
| What pipeline contract is this slice proving? | (name the typed mutations, data flows) |
| What are the critical dataflow joints? | (list 3-5 boundary events where telemetry must exist) |
| What is the mini-lab scope? | (operator-seeded scenarios that exercise the joints) |
| What is explicitly deferred? | (what this slice does NOT claim) |

### 1.3 Produce the sprint contract

```markdown
### Sprint Contract: [Slice Name]

**Engine**: Backend/Pipeline
**Lane**: [lane_id from registry]
**Evidence class**: [from registry]

**Pipeline contract**:
- [Typed mutation 1]
- [Typed mutation 2]

**Critical dataflow joints**:
1. [Joint 1 -- e.g., ingress accepted]
2. [Joint 2 -- e.g., command classification chosen]
3. [Joint 3 -- e.g., commit checkpoint]
4. [Joint 4 -- e.g., local persistence]

**Mini-lab scenarios**:
- [Scenario A -- expected telemetry conclusion]
- [Scenario B -- expected telemetry conclusion]
- [Negative scenario -- expected safe-fail behavior]

**Deferred scope**:
- [What this slice does NOT build or claim]

**Evaluation criteria**:
- Each scenario reaches expected telemetry conclusion at declared joints
- Mutation scenarios persist honest local snapshots
- Negative scenarios produce no mutation and leave state unchanged
```

**Stop if the sprint contract is not filled. Do not proceed to execution without a signed contract.**

---

## Phase 2: Operator

The operator builds within the sprint contract's bounded scope.

### 2.1 Implementation rules

- Build pipeline contract implementation within the lane's `owned_paths`
- Add telemetry (`Log.d` on Android, `hilog` on HarmonyOS) at every declared critical joint
- Build mini-lab verification sandbox with operator-seeded scenarios
- Do not build UI beyond a minimal operator surface for running mini-lab and inspecting traces
- Do not claim parity with features outside the sprint contract's scope

### 2.2 Telemetry tag convention

Android:
```
Log.d("ClassName", "jointName: key=$value")
```

HarmonyOS:
```
hilog.info(0x0000, "ClassName", "jointName: key=%{public}s", value)
```

Telemetry rule: log boundary events and payload shape summaries. Do not dump huge payloads or pretend telemetry itself is the product.

### 2.3 Anti-drift checkpoint

Before handing off to evaluator:

```markdown
### Operator Anti-Drift Check
- [ ] Implementation stays within sprint contract scope
- [ ] Telemetry exists at every declared critical joint
- [ ] Mini-lab scenarios are seeded and runnable
- [ ] No UI beyond operator debug surface
- [ ] Deferred scope items are NOT implemented
- [ ] No behavior invented from cerb-era UI specs
```

---

## Phase 3: Evaluator

The evaluator is structurally separate from the operator. It verifies work against the sprint contract using typed evidence.

### 3.1 Evidence collection (by class)

**`platform-runtime`** (primary for backend engine):
- Agent runs `adb logcat -s Tag:D` (Android) or `hdc log` (HarmonyOS)
- Filter for telemetry at each declared critical joint
- Confirm each mini-lab scenario reaches expected telemetry conclusion
- Confirm negative scenarios produce no mutation

**`contract-test`**:
- Agent runs `./gradlew testDebugUnitTest` (Android) or hvigor test (HarmonyOS)
- Confirm typed mutations match pipeline contract data classes
- Confirm deserialization succeeds for valid payloads and fails for invalid ones

**`ui-visible`** (only if sprint contract includes operator surface):
- Agent requests human operator to supply real-device screenshots
- Agent cannot see phone pixels -- operator is the evaluator's eyes
- Agent's assertion that UI "looks correct" is not evidence

### 3.2 Evaluation checklist

```markdown
### Evaluator Verdict: [Slice Name]

**Sprint contract**: [reference]

**Joint telemetry** (paste filtered log output):
- Joint 1: [PASS/FAIL -- paste evidence]
- Joint 2: [PASS/FAIL -- paste evidence]
- Joint 3: [PASS/FAIL -- paste evidence]
- Joint 4: [PASS/FAIL -- paste evidence]

**Mini-lab scenarios**:
- Scenario A: [PASS/FAIL -- paste evidence]
- Scenario B: [PASS/FAIL -- paste evidence]
- Negative scenario: [PASS/FAIL -- paste evidence]

**Contract tests**: [PASS/FAIL -- paste test output summary]

**Verdict**: [ACCEPT / REJECT / NEEDS EVIDENCE]

**If REJECT**: [what specifically failed, what the operator must fix]
**If NEEDS EVIDENCE**: [what evidence is missing, who must supply it]
```

### 3.3 Evidence rejection rules

- A passing unit test is not runtime proof -- both are required when the sprint contract declares both
- An agent's text assertion is not evidence in any class
- Screenshots from an agent-generated mock are not `ui-visible` evidence -- only real-device operator screenshots count
- Telemetry from a different scenario than the one declared in the sprint contract does not count

---

## Phase 4: Close

After evaluator accepts:

1. Update the owning core-flow doc if the slice changed behavioral truth
2. Update `docs/cerb/[feature]/interface.md` if typed mutations changed
3. Update `ops/lane-registry.json` lane status
4. If the sprint touched a cerb-era doc for active work, rewrite it to post-harness shape (rewrite-on-touch rule)
5. Run `/cerb-check` on all modified docs

---

## Related Docs

- `docs/specs/harness-manifesto.md` -- operating protocol (position 0)
- `docs/platforms/harmony/scheduler-backend-first.md` -- proving experiment
- `.agent/rules/anti-illusion-protocol.md` -- test integrity
- `.agent/rules/anti-laziness.md` -- verification standards
- `.agent/rules/anti-drift-protocol.md` -- spec anchoring
- `docs/sops/lane-worktree-governance.md` -- lane lifecycle
