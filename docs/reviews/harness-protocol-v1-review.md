# Harness Protocol v1 Review

> **Status**: Review memo
> **Date**: 2026-04-12
> **Reviewer**: Claude (Opus 4.6)
> **Scope**: Smart Sales Harness Protocol v1 proposal

---

## 1. Verdict

The evidence-class model is the one genuinely new idea in this proposal. Ship it. Human-evidence escalation and `Awaiting evidence` are good companion ideas that should land in existing artifacts. The rest of the proposal is naming and ceremony layered on top of machinery the repo already has.

The repo already runs a lane harness (`ops/lane-registry.json`, `scripts/lane_guard.py`, `.githooks/`, CI), a proven execution-brief template (26 existing briefs), a handoff contract (`handoffs/README.md`), and a dirty-tree quarantine board. The protocol proposes a four-layer hierarchy, five engines, four roles, and a new artifact name — none of which add safety, enforcement, or clarity beyond what already exists.

Recommendation: absorb the evidence-class model and its companions into the existing lane/brief/handoff/validator system. Drop the new hierarchy, roles, engines, and Sprint Contract vocabulary.

### Scorecard

| Proposal Section | Rating | Position |
|---|---|---|
| 1. Architecture model (four layers) | Drop | New hierarchy is naming, not new safety |
| 2. Role model (four roles) | Drop | Conflicts with interchangeable-worker reality |
| 3. Sprint Contract | Absorb into existing | Keep execution briefs; add missing fields |
| 4. Evidence-class model | Ship | Best genuinely new idea in the proposal |
| 5. Human-evidence escalation | Absorb into existing | Good companion rule; land in current artifacts |
| 6. Control-plane binding | Absorb into existing | Describes machinery that already exists |
| 7. Acceptance / stop semantics | Absorb into existing | Keep `Awaiting evidence`; extend current status enum |

---

## 2. What Works — Evidence-Class Model

The five evidence classes are genuinely useful:

- `ui-visible` — operator-supplied real-device screenshots for user-facing changes
- `runtime-telemetry` — logs, telemetry, monitoring traces for backend/runtime work
- `contract-test` — compile/test/contract verification for shared-contract changes
- `platform-runtime` — adb logcat plus runtime proof (Android), install/deploy/signing chain (Harmony)
- `governance-proof` — docs, validator behavior, hooks, CI alignment

The single most valuable idea in the entire proposal is: the evaluator must reject the wrong proof modality. UI work should not be accepted from logs alone. Backend/runtime/platform work should not be accepted from screenshots alone. This is a real gap in the current system — the dirty-tree-quarantine board has a `Required Evidence` column, but nothing enforces that the evidence actually matches the nature of the work.

Human-evidence escalation (Section 5) is a natural companion. The rule that agents should exhaust accessible evidence first and only then pause for human proof — that is already how the repo works informally, but making it explicit is worth doing. `Awaiting evidence` as a workflow state is the right way to model this — it is not a failure, it is a normal lifecycle moment.

These ideas should ship into existing artifacts (`ops/lane-registry.json`, execution brief template, `docs/plans/dirty-tree-quarantine.md`), not as a new top-layer protocol.

---

## 3. Problem 1 — Disconnected from Existing Machinery

The protocol never mentions a single concrete repo artifact by name. It talks about an "Execution Control Plane" and "lane/worktree/tracker/handoff governance" without ever saying `ops/lane-registry.json`, `scripts/lane_guard.py`, `.githooks/pre-commit`, `.githooks/pre-push`, `.github/workflows/platform-governance-check.yml`, `scripts/lane`, or `handoffs/README.md`.

Section 6 of the proposal describes a safety system. That safety system already exists — it is the lane harness described in `docs/sops/lane-worktree-governance.md`, enforced by `scripts/lane_guard.py`, backed by git hooks and CI. The protocol paraphrases it without acknowledging it.

| Protocol concept | Existing artifact already doing this | Action |
|---|---|---|
| Execution Control Plane | `docs/sops/lane-worktree-governance.md` | Absorb |
| Machine lane ledger | `ops/lane-registry.json` | Extend with `evidence_class` |
| Validator / enforcement | `scripts/lane_guard.py` | Extend with evidence validation |
| Local enforcement | `.githooks/pre-commit`, `.githooks/pre-push` | Extend |
| CI backstop | `.github/workflows/platform-governance-check.yml` | Extend |
| Lane commands | `scripts/lane` | Extend |
| Paused-lane transfer | `handoffs/README.md` + `handoffs/*.md` | Strengthen |

Every row in this table is something the repo already has. The protocol should have started from here.

---

## 4. Problem 2 — Role Model Is Wrong

The four roles — Planner, Builder, Evaluator, Runtime Examiner — assume specialized departments where different actors play different parts. That is not how this repo works. Codex, Antigravity, and Claude are interchangeable workers. Any agent picks up any slice and does the full lifecycle. Frank is the human escalation point for device evidence and judgment calls. The lane harness coordinates who works where, not what role they play.

The protocol should define gates, not roles. Five lifecycle gates that any worker (human or agent) must pass:

- **Classify** — entry: task exists. Exit: source of truth, lane/work_class, evidence class, and bounded outcome are explicit.
- **Execute** — entry: slice is classified. Exit: implementation or analysis work is complete enough to gather proof.
- **Evidence** — entry: there is something to verify. Exit: required logs/tests/screenshots/handoff context are collected for the declared evidence class.
- **Evaluate** — entry: evidence exists or missing-evidence state is explicit. Exit: accepted / conditional / blocked / awaiting evidence / rejected decision is recorded.
- **Close** — entry: evaluation has completed. Exit: integrated, deferred, or paused-with-handoff state is explicit in both the lane registry and the dirty-tree-quarantine board.

No role assignment needed. Any worker passes the same gates in order.

---

## 5. Problem 3 — Engines Are Redundant

The five proposed engines (UI, Backend/Shared-Contract, Runtime/Device, Harmony, Governance) overlap with the `work_class` taxonomy already present in `ops/lane-registry.json`:

- `android-beta`
- `shared-contract`
- `cross-platform-governance`
- `harmony-native`
- `foreign-local-artifact`

Engines add new names but no new enforcement, no new safety, no new validator behavior. If the repo needs finer distinctions beyond `work_class`, the right move is to add `evidence_class` as a second field — not to create a parallel engine taxonomy that duplicates what `work_class` already does.

Drop engines entirely. Express differences through `work_class` + `evidence_class` + lifecycle gates.

---

## 6. Problem 4 — Sprint Contract Activation Threshold Is Vague

The threshold — "multi-step, multi-role, cross-boundary, runtime-sensitive, or nontrivial user-visible slice" — is so broad that nearly everything qualifies. The repo already has 26 execution briefs with a proven, consistent template (Status, Date, Mission, Primary Tracker, Source Documents, Purpose, Scope Boundaries, Deliverables, Verification Status, Acceptance Bar, Related Documents). They work.

Keep execution briefs. Do not rename them to Sprint Contracts. Add the good missing fields from the proposal: `evidence_class`, `stop_conditions`, and an explicit lane reference. That gets the value without the ceremony overhead of a new artifact type and a vague activation rule.

---

## 7. Problem 5 — Multi-Agent Coordination Unaddressed

The proposal talks about pause/resume but never binds to the actual multi-agent coordination mechanism. `handoffs/README.md` already defines 13 required sections for any paused or transferred lane:

1. Lane ID
2. Registry Lane ID
3. Branch
4. Recommended Worktree
5. Scope
6. Owned Paths
7. Current Repo State / Implementation Truth
8. What Is Finished
9. What Is Still Open
10. Doc-Code Alignment
11. Required Evidence / Verification
12. Safe Next Actions
13. Do Not Touch / Collision Notes

The `handoff_path` field in `ops/lane-registry.json` links each paused lane to its handoff file. The lane-worktree-governance SOP requires that paused lanes have current handoffs. This is the multi-agent coordination system. The protocol should strengthen it — for example by adding `evidence_class` to the handoff contract so the next worker knows what proof modality to collect — not introduce a parallel abstraction.

---

## 8. Problem 6 — State Model Gaps

The proposal's state model (Draft, Active, Paused, Review, Awaiting evidence, Accepted, Blocked) is missing two states and includes one that does not pull its weight:

- Missing **Rejected** — evaluator says no. The current system has no way to record that a lane was evaluated and found deficient without silently reverting it to Active or Paused.
- Missing **Integrated** — lane has actually landed in the trunk. Currently `Accepted` is the terminal state, but there is a real gap between "accepted as complete" and "actually merged."
- **Blocked** vs **Awaiting evidence** — the proposal tries to distinguish these, but the distinction is too subtle. `Awaiting evidence` is the useful one; `Blocked` should just use `Paused` with a handoff note explaining the blocker.

Extend the existing enum from `docs/plans/dirty-tree-quarantine.md`:

| Status | Meaning |
|---|---|
| `Proposed` | Exists but not active |
| `Active` | Currently being worked |
| `Paused` | Stopped, handoff required |
| `Awaiting evidence` | Work done, waiting for proof |
| `Review` | No longer taking edits, waiting for evaluation |
| `Accepted` | Evaluated and approved |
| `Rejected` | Evaluated and found deficient |
| `Integrated` | Landed in trunk |
| `Deferred` | Parked, excluded from current path |

---

## 9. Structural Concern — No Enforcement Mechanism

The entire proposal is rules. Zero validators, zero hooks, zero CI checks. The existing system already has enforcement: `scripts/lane_guard.py` validates lane ownership, `.githooks/pre-commit` and `.githooks/pre-push` run that validator locally, `.github/workflows/platform-governance-check.yml` runs it in CI.

Rules without validators are governance debt, not governance. Any new rule that cannot be validated locally or in CI should not be introduced as formal repo law. The evidence-class idea is only worth shipping if it comes with a `validate-evidence` command in `lane_guard.py` and hook integration.

---

## 10. Structural Concern — CLAUDE.md Relationship Undefined

A protocol is invisible if it does not bind to the instruction surfaces agents actually read: `AGENTS.md`, `CLAUDE.md`, `.agent/rules/**`, `.codex/skills/**`. The proposal never defines how it relates to any of these. If the evidence-class model ships, it needs to be referenced from the doc-reading protocol in `CLAUDE.md`, the feature-development skill in `.codex/skills/smart-sales-feature-delivery/SKILL.md`, and the acceptance skill in `.codex/skills/smart-sales-acceptance/SKILL.md`. Otherwise it exists only as a review artifact, not as runtime behavior.

---

## 11. Concrete Recommendations

### A. Add `evidence_class` to `ops/lane-registry.json`

New field on each lane object. Enum: `ui-visible`, `runtime-telemetry`, `contract-test`, `platform-runtime`, `governance-proof`.

```diff
 {
   "lane_id": "DTQ-04",
   "title": "Runtime shell and SIM chrome",
   "status": "active",
   "work_class": "android-beta",
+  "evidence_class": "ui-visible",
   "branch": null,
```

### B. Add `evidence_class` and `stop_conditions` to execution brief template

Keep the artifact name "execution brief." Add two fields to the header block and a new section:

```diff
 **Primary Tracker:** `docs/plans/ui-tracker.md`
+**Lane Reference:** `DTQ-04`
+**Evidence Class:** `ui-visible`

 ## Current Active Truth
 ...

+## Stop Conditions
+- missing required evidence for the declared evidence class
+- lane ownership conflict or unresolved collision
+- source-of-truth conflict that cannot be resolved within the slice
+- required device or operator evidence unavailable
```

### C. Add `Awaiting evidence`, `Rejected`, `Integrated` to the status enum

Update both `docs/plans/dirty-tree-quarantine.md` and the validation logic in `ops/lane-registry.json`. Note that reserved/path-blocking states (Active, Paused, Review, Awaiting evidence) should remain distinct from terminal states (Accepted, Rejected, Integrated, Deferred) if the validator needs that distinction.

### D. Add evidence-class validation to `scripts/lane_guard.py`

New command: `validate-evidence`. Checks: every active/review/awaiting-evidence lane has a valid `evidence_class`; declared class is one of the allowed enum values; required supporting artifacts exist when applicable. Integrate into `.githooks/pre-push` and optionally into the CI workflow. This is the enforcement path that makes the evidence-class idea real.

### E. Replace the role model with lifecycle gates

The five gates defined in Section 4 (Classify, Execute, Evidence, Evaluate, Close) replace the four roles. Each gate has entry/exit criteria. Any worker passes them. Document the gates in `docs/sops/lane-worktree-governance.md` as a new section, and reference them from execution brief templates.

### F. Mapping Table — Protocol Ideas to Existing Artifacts

| Proposal section | Proposal idea | Existing artifact(s) | Action |
|---|---|---|---|
| 1. Architecture model | Four-layer hierarchy | No equivalent needed | Drop |
| 2. Role model | Planner/Builder/Evaluator/Runtime Examiner | Lifecycle gates (new) | Drop roles; ship gates into lane SOP |
| 3. Sprint Contract | Bounded delivery artifact | Execution briefs (26 existing) | Absorb good fields; keep existing name |
| 3. Sprint Contract | Evidence class field | `ops/lane-registry.json`, brief template | Ship as new field |
| 3. Sprint Contract | Stop conditions | Brief template | Ship as new section |
| 4. Evidence-class model | Five evidence classes + wrong-modality rejection | `ops/lane-registry.json`, `lane_guard.py` | Ship |
| 5. Human-evidence escalation | Exhaust accessible evidence, then ask human | Lane SOP, handoff contract | Absorb into existing docs |
| 5. Human-evidence escalation | `Awaiting evidence` state | Dirty-tree-quarantine status enum | Ship as new status |
| 6. Control-plane binding | Lane/worktree/tracker governance | `docs/sops/lane-worktree-governance.md` | Already exists |
| 7. Acceptance semantics | Slice-done criteria | Execution brief acceptance bar | Already exists |
| 7. Stop semantics | Explicit stop conditions | Brief template (new section) | Ship |

---

## 12. What to Drop

- The four-layer hierarchy (Harness Protocol > Execution Control Plane > Harness Engines > Sprint Contracts) — naming, not a system
- The five engines — redundant with `work_class`
- The four roles — wrong model for interchangeable workers
- The Sprint Contract name — execution briefs are the proven artifact
- Engine annexes — no evidence they add value over `work_class` + `evidence_class`

Keep the evidence rules, strengthen the existing lane/brief/handoff/validator system, and avoid inventing a parallel vocabulary for machinery the repo already has.
