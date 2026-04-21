# Harness Operating Protocol

> **Status:** Active operating protocol -- position 0 in resolution chain
> **Scope:** Governs how every agent and human reads, plans, builds, and verifies in this repo
> **Date:** 2026-04-13
> **Precedence:** This document is read before SmartSales_PRD.md, before any spec, before any code

---

## 1. The Paradigm Shift

This repo operates in a post-harness, backend-verification-led paradigm.

Pre-harness, work was UI-feature-led: screen, gesture, spec, then figure out the backend. Post-harness, work is backend-verification-led: pipeline contract, mini-lab proof, then UI docking. The UI is a thin surface that attaches to proven backend contracts, not the driver of what gets built.

This shift is not aspirational. It is proven. The Harmony scheduler slice (`docs/platforms/harmony/scheduler-backend-first.md`) demonstrated the model: backend dataflow was verified at four critical joints (ingress, classification, Path A commit, local persistence) with operator-only telemetry before any UI translation was attempted. That experiment is the template for all future slices.

Every agent runtime that reads this repo must internalize this paragraph before reading anything else.

---

## 2. Backend-First Reading Order

When starting any task, agents must read docs in this order. This overrides the feature-shaped reading protocol in CLAUDE.md.

| Priority | What to Read | Why |
|---|---|---|
| 1 | **Core-flow doc** (`docs/core-flow/`) | Behavioral north star -- what the feature means |
| 2 | **Pipeline/data contract** (`docs/cerb/*/interface.md`) | What the backend guarantees -- typed mutations, data flow |
| 3 | **Verification boundary** (lane's `evidence_class` in `ops/lane-registry.json`) | What proof looks like for this task |
| 4 | **UI docking surface** (`docs/cerb/*/spec.md`, `docs/cerb-ui/`) | Reference only, not source of truth for backend behavior |

This is the **Backend-First Reading Order**. Agents must follow it by name.

If a core-flow doc does not exist for the feature, that is the first thing to create -- not a reason to skip to the UI spec.

---

## 3. The Cerb-Era Boundary

This repo contains two eras of documentation. The boundary must be explicit.

### Cerb-era docs (pre-harness)

`docs/cerb/*/spec.md` and `docs/cerb-ui/*/spec.md` were written when UI choreography was the source of truth. They describe screens, gestures, state machines, and UX flows. They are:

- **Valid for**: understanding feature intent, UX patterns, and the original design thinking
- **Not valid for**: deriving pipeline behavior, inventing backend contracts, or treating UI descriptions as implementation requirements

### Cerb-era reference directories

These directories are frozen at cerb-era structure. Do not treat them as active implementation authority:

| Directory | Contains | Status |
|---|---|---|
| `docs/design/briefs/` | UI prototype briefs | Reference only |
| `docs/artifacts/` | UX visualizations | Reference only |
| `docs/CN_Dev/` | Chinese-language dev guides | Frozen, cerb-era |
| `docs/walkthroughs/` | UI walkthroughs | Reference only |

### Post-harness docs

New docs and rewritten docs must follow the post-harness shape:

```yaml
---
era: post-harness
---
```

Required sections:

1. **Pipeline contract** -- what data flows, what guarantees exist, what mutations are typed
2. **Verification boundary** -- what mini-lab proof looks like (evidence class, L1/L2/L3 level, critical telemetry joints)
3. **UI docking point** -- where the frontend attaches, as a thin layer, not the driver
4. **Deferred scope** -- what this doc explicitly does not claim

### The rewrite-on-touch rule

When a cerb-era doc is opened for active work, the agent must rewrite it to post-harness shape in the same session. This makes doc migration organic and grounded in real runtime truth, not speculative reformatting. Do not mandate bulk rewrite.

---

## 4. What Agents Must Not Do with Old Docs

- Do not invent backend behavior from UI spec descriptions
- Do not treat gesture/animation details as pipeline requirements
- Do not create new cerb-era-shaped docs (UI-first specs)
- Do not read `docs/design/briefs/` before reading the owning core-flow
- Do not treat `docs/cerb/*/spec.md` as the source of truth for what the backend should do
- Do not treat tracker wave titles as specs -- they are index labels, not behavior definitions

---

## 5. Harness Engines

Different tasks require different harness engines. A backend/pipeline task has fundamentally different planning, execution, and evaluation needs than a UI task or a governance task. The harness is not one monolithic process -- it is a set of task-type-specific engines that share common beliefs but differ in their mechanics.

### Engine structure

Each engine defines three roles:

| Role | Responsibility |
|---|---|
| **Planner** | Reads the right docs in the right order, classifies the task, produces a bounded execution brief with success criteria |
| **Evaluator** | Verifies the work against the success criteria using evidence appropriate to the task type |
| **Operator** | Executes the work within the bounded scope declared by the planner |

These are not separate agents. They are roles that one or more agents fulfill during a task lifecycle. The same agent may be planner and operator. The evaluator should be structurally separate from the operator to defeat self-evaluation bias (Anthropic, "Harness Design for Long-Running Application Development," 2025).

### Proven engine: Backend/Pipeline

The first proven engine, grounded in the Harmony scheduler slice experiment (`docs/platforms/harmony/scheduler-backend-first.md`):

**Planner reads** (in order):
1. Core-flow doc -- behavioral north star
2. Pipeline contract (`interface.md`) -- what the backend guarantees
3. Verification boundary -- evidence class, critical telemetry joints
4. Deferred scope -- what this slice explicitly does not claim

**Operator builds**:
- Pipeline contract implementation within the lane's owned scope
- Mini-lab verification sandbox (operator-seeded scenarios)
- Telemetry at critical dataflow joints

**Evaluator verifies** (by evidence class):
- `platform-runtime`: agent operates the **Deterministic Device-Loop Protocol** (`docs/specs/device-loop-protocol.md`) on the declared lane. Compile success is not runtime proof; cold-relaunch plus per-joint log capture (Android `adb logcat` or HarmonyOS `hdc shell hilog`) is the minimum bar.
- `contract-test`: agent runs test suite, verifies typed mutations match contract
- `ui-visible`: agent requests human operator to supply real-device screenshots -- the agent cannot see phone pixels, the operator is the evaluator's eyes

The evaluator rejects evidence of the wrong modality. A passing test is not runtime proof. An agent's assertion is not visual evidence.

### Future engines (declared, not yet proven)

The Backend/Pipeline engine is the only proven engine. Other work is classified at contract time by **intent**, not by engine, using the axis defined in `.claude/commands/sprint.md` §2:

| Intent | What it means | Execution path | Evidence focus |
|---|---|---|---|
| **dataflow** | New feature, behavior change, typed mutations, pipeline contract. Owns the minimal UI surface needed to exercise the pipeline. | Backend/Pipeline engine (above) | pipeline contract + critical joints + mini-lab scenarios; screenshots support only |
| **cosmetic** | Pure aesthetic polish on UI elements already registered in `docs/specs/ui_element_registry.md`. Zero behavior change. | Interim UI workflow (engine not yet formally built) | `ui-visible` screenshots (authoritative) + registry compliance |
| **hybrid** | Small tweak against an already-shipped surface that genuinely touches both, capped at ≤3 files. | Dataflow slice runs first, then cosmetic slice, within one contract. | Union of dataflow and cosmetic evidence; no bypass on either |

Hard rules (enforced at `/sprint` planner time and `/ship` evaluator time):

- Net-new feature work must be `dataflow`. `cosmetic` and `hybrid` cannot be the entry contract for a new surface.
- `cosmetic` cannot create new UI elements. It restyles elements already registered in `ui_element_registry.md`.
- `cosmetic` cannot touch dataflow files (ViewModel, Repository, flow, mapper, domain, UseCase, ArkTS state/service).
- `cosmetic` that uncovers a behavior bug halts and hands off to a new `dataflow` contract. No mid-sprint scope change.
- `hybrid` is capped at ≤3 files. Violation triggers split into `dataflow` + `cosmetic`.
- Drift signal: if `hybrid` exceeds 30% of ships in a 20-ship rolling window (tracked via `docs/plans/changelog.md`), the next `/sprint` invocation rejects hybrid until the ratio returns below threshold.

The planner/operator/evaluator role structure from §5 above is preserved across all three intents -- only the template fields, evidence class, and evaluator gates differ. Architecture, Governance, and Doc work route through the intent axis as well: module-boundary or interface-map changes are `dataflow` (they modify behavior contracts); SOP/tracker/registry edits are `cosmetic` when they only restyle existing structure and `dataflow` when they change governance behavior; new spec creation is `dataflow` (net-new artifact, new reading contract).

Do not build speculative engine scaffolding ahead of proven need. New engines emerge when a real task demands one, proves its mechanics on a bounded slice, and then codifies.

---

## 6. Unified Task Entrance

All tasks enter the harness through a single entrance point: the `/sprint` skill.

The entrance skill:
1. **Takes** a task declaration from the operator (human or agent)
2. **Classifies** the task type (backend, UI, architecture, governance, doc)
3. **Routes** to the correct engine
4. **Produces** a sprint contract: bounded scope, success criteria, evidence class, planner/evaluator/operator assignments
5. **Registers** the sprint in the lane registry if the task requires a lane

The sprint contract is the agreement between planner and evaluator before any code is written. It makes the task explicit and wavering-free: what will be built, what proof will be collected, and what is explicitly out of scope.

This is the only sanctioned way to start nontrivial work. Skipping the entrance skill and going straight to code is the pre-harness pattern and produces drift.

```
Operator declares task
        |
        v
   /sprint skill
        |
        v
  Classify task type
        |
  ┌─────┼─────┬──────────┬────────────┐
  v     v     v          v            v
Backend  UI  Arch    Governance     Doc
engine  engine engine   engine     engine
        |
        v
  Sprint contract
  (scope, evidence, roles)
        |
        v
  Execute → Evaluate → Close
```

---

## 7. Core Beliefs

These beliefs are shared across all engines. They are the axioms that make the engine model coherent.

1. **Fluency is not competence.** LLMs that are wrong look identical to LLMs that are right. Verification must be structural (test suite, spec check, telemetry), not observational ("looks good"). (`.agent/rules/anti-illusion-protocol.md`, `.agent/rules/anti-laziness.md`)

2. **Drift is the default.** Without anchoring, every conversation drifts from spec. The repo documents 30+ distinct drift categories, each traced to a real incident. Anchoring means reading the spec first, quoting the text you are implementing, and stopping when the spec is silent. (`.agent/rules/anti-drift-protocol.md`, `.agent/rules/lessons-learned.md`)

3. **The unit of trust is evidence, not identity.** It does not matter whether the worker is human or AI. The lane's `evidence_class` governs what proof is accepted. Five classes, five different questions -- mixing them is a category error. (`.agent/rules/anti-laziness.md`, `ops/lane-registry.json`)

4. **The system fails closed.** Ambiguous ownership blocks. Missing spec stops work. The harness prefers visible friction over invisible rot. Seven hard invariants enforce this. (`docs/sops/lane-worktree-governance.md`)

5. **Docs come first because they persist.** Docs are the only artifact that survives context-window boundaries. Code is mutable, context is ephemeral. "Docs > Code > Guessing" is a control-flow statement, not a preference. (`.agent/rules/docs-first-protocol.md`)

6. **The human-in-the-loop for visual evidence is load-bearing.** On mobile, agents cannot see phone pixels. For `ui-visible` evidence, the operator supplies real-device screenshots. This is not a limitation to remove -- it is the authoritative visual evidence until agents can observe and prove rendering fidelity on real devices. (`.agent/rules/anti-laziness.md`)

---

## 8. Constraint Categories

Not every rule has the same shelf life. Three categories:

**Load-bearing (keep forever)** -- coordination problems, not competence problems. Would still be needed if the agent were a flawless senior engineer: domain purity, canonical trunk, interface map discipline, lane ownership, typed evidence, data-class-as-spec, fail-closed defaults, human intricacies protocol, docs-first protocol.

**Evolving (relax as capability grows)** -- compensate for current LLM limitations: mandatory spec quoting, feature-dev-planner triage gates, anti-drift checklists, three-level test ordering, lessons-learned preflight, context-boundary rules.

**Temporary (remove when condition resolves)** -- tied to transient repo state: god-file LOC budgets, god-tracker exceptions, Harmony isolation strictness, compose-scrim-drawer pattern.

Relaxation uses a five-rung ladder: Mandatory, Enforced-with-override, Advisory, Reference, Archived. Movement is one step at a time. Re-tightening is not failure; it is feedback.

---

## 9. Resolution Chain

This is the document reading order for resolving conflicts. The harness operating protocol sits at position 0.

| Position | Document | Governs |
|---|---|---|
| 0 | **This document** (`docs/specs/harness-manifesto.md`) | How to read everything else |
| 1 | `SmartSales_PRD.md` | Product identity, surfaces, journeys |
| 2 | `docs/specs/base-runtime-unification.md` | Base-runtime vs Mono boundary |
| 3 | `docs/core-flow/**` | Behavioral north star (may be ahead of spec/code) |
| 4 | `docs/cerb/**`, `docs/cerb-ui/**` | Implementation contracts (cerb-era: reference for intent) |
| 5 | `docs/specs/Architecture.md` | Deeper system laws |
| 6 | Code and validation evidence | Runtime truth |

When documents conflict, higher-position documents govern. Core-flow can be ahead of specs and code -- treat lower layers as drift candidates before assuming the core-flow is wrong.
