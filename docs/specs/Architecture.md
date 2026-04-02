# Architecture Guide (Base Runtime Laws and Mono Augmentation)

**Status:** Active Supporting Architecture Reference  
**Last Updated:** 2026-04-01  
**Successor to:** Prism-V1.md, Lattice Architecture  
**Product North Star:** [`SmartSales_PRD.md`](../../SmartSales_PRD.md)  
**Primary Boundary Doc:** `docs/specs/base-runtime-unification.md`  
**Use This Doc When:** the task depends on typed mutation architecture, RAM/SSD boundaries, Kernel-owned memory, plugin/tool runtime, or pipeline observability laws.

> **Preamble**: This document is no longer the repo-default product-truth doc for every task. Read `SmartSales_PRD.md` first for app identity and major journeys, then read `docs/specs/base-runtime-unification.md` for the base-vs-Mono boundary, then the relevant `docs/core-flow/**` and feature specs. Use this file for stable cross-cutting architecture laws and for the deferred Mono augmentation layer.

---

## 1. Architectural Role And Precedence

This document defines the repo's deeper architecture laws.
It covers:

- cross-cutting runtime boundaries
- typed mutation and persistence laws
- RAM/SSD mental-model rules
- plugin and observability rules
- lifecycle rules for work that enters the deeper architecture

This document does **not** own feature-specific behavior.
That belongs to `docs/core-flow/**` and the owning feature specs.

### Current precedence rule

Treat architecture guidance in this order:

1. `SmartSales_PRD.md` for app identity, major surfaces, core journeys, and product-level UX laws
2. `docs/specs/base-runtime-unification.md` for shared non-Mono posture and the base-vs-Mono boundary
3. relevant `docs/core-flow/**` for behavioral north-star truth
4. relevant `docs/cerb/**`, `docs/cerb-ui/**`, and feature specs for implementation contracts
5. `docs/specs/Architecture.md` for deeper system laws that apply when the feature touches typed mutation, RAM/SSD ownership, plugins, or Mono augmentation
6. code and validation evidence

### What belongs here

Keep only these categories in `Architecture.md`:

- architectural laws that generalize across features
- stable patterns for typed data movement and persistence
- observability and runtime boundary rules
- Mono augmentation rules and limits

Do not use this file as a dumping ground for feature-specific branch logic.
Do not use the word `Mono` as shorthand for all current product work.

---

## 2. Current Repo Posture

### 2.1 Base runtime first, Mono later

The current repo posture is:

- one shared **base runtime** for shell/UI/UX, Tingwu/audio, Path A scheduler, and bounded local/session continuity
- one later **Mono augmentation layer** for deeper memory, CRM/entity loading, Path B enrichment, plugin/tool runtime, and related intelligence
- no lawful non-Mono split between `SIM truth` and `full truth`

This means:

- non-Mono work should not start from a Mono-first assumption
- separate SIM entry roots or namespaced persistence may still exist in code, but they do not create a second product truth
- this file must not be read as permission to bypass `docs/specs/base-runtime-unification.md`

### 2.2 What Mono still means

`Project Mono` remains a valid architectural term for the deeper augmentation target.
It is useful when a task actually depends on:

- Kernel-owned RAM/session-memory lifecycle
- CRM/entity loading and deeper context assembly
- plugin/tool runtime
- typed mutation architecture across richer reasoning lanes
- broader observability and write-through coordination

Historical note:

- older repo material may describe Mono as if it were the default destination for all work
- current repo posture does **not** treat Mono as the first-stop product truth for routine shell, scheduler, Tingwu/audio, or onboarding delivery

### 2.3 The core problem this architecture solves

Earlier generations relied on free-form prompt contracts and hand-maintained parsing layers.
That created drift, ghost fields, and hidden coupling between prompt text, linter logic, and persistence.

The enduring architecture law is:

- query-like reasoning must move toward grounded identifiers and bounded context
- durable mutations must terminate in strict typed payloads rather than shapeless text
- user-facing prose is not a persistence contract

### 2.4 The one-currency rule

At architectural boundaries, the system must not trade in multiple ambiguous currencies.

Preferred currencies:

- **query lane**: verified identity plus bounded runtime context
- **mutation lane**: strict Kotlin data shapes and controlled persistence inputs

Rule:

- do not force the LLM to act as a currency exchange between raw strings, ad hoc JSON, and hidden database expectations

---

## 3. Runtime Classification

Do not assume every feature uses the deepest architecture.
Classify work first.

| Concern | Typical Components | Classification | Notes |
|---|---|---|---|
| Shared shell/UI/UX | `AgentIntelligenceScreen`, `SchedulerDrawer`, shared shell docs | Base runtime | Shared non-Mono presentation truth belongs here. |
| Path A scheduler behavior | scheduler drawer, scheduler path docs | Base runtime | Shared non-Mono scheduling must not fork into SIM-vs-full truth. |
| Tingwu/audio artifact flow | audio drawer, Tingwu pipeline, artifact chat lane | Base runtime | Source-led long-form audio remains shared product truth. |
| Bounded local/session continuity | local session stores, short-lived continuity helpers | Base runtime | Short-lived continuity alone does not make a feature Mono. |
| Phase-0 routing and lightweight gating | `IntentOrchestrator`, lightweight routing helpers | Shared support | May exist in base runtime without forcing full Mono posture. |
| Kernel-owned RAM/session memory | `ContextBuilder`, richer working-set assembly | Mono augmentation | Use this doc's deeper RAM/SSD laws when work enters this layer. |
| CRM/entity loading | CRM/domain loaders, graph assembly | Mono augmentation | Not required for shared base-runtime delivery. |
| Plugin/tool runtime | plugin gateways, capability SDK | Mono augmentation | Plugins must not redefine shared shell truth. |
| Legacy wrapper hosts | `AgentShell.kt`, `AgentViewModel.kt`, `SchedulerViewModel.kt` | Wrapper debt | Compatibility owners are not product-truth owners. |

Rule:

- if a task can ship inside the base runtime, do not reframe it as Mono-only
- if a task truly depends on deeper RAM/entity/plugin architecture, apply the rest of this document strictly

---

## 4. The OS Mental Model

When a feature enters the deeper architecture, Prism should still be understood through the OS mental model.
This model protects the boundary between transient runtime context and durable world truth.

```text
┌─────────────────────────────────────────────────────────────────┐
│                    THE OS MENTAL MODEL                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────────┐       ┌─────────────────┐                 │
│   │    OS: RAM      │◄──────│    OS: SSD      │                 │
│   │ (Active Session)│ Load  │ (Knowledge Base)│                 │
│   └────────┬────────┘       └─────────────────┘                 │
│            │                         ▲                          │
│            │ Write-Through (Async)   │                          │
│            └─────────────────────────┘                          │
│                                                                 │
│   + OS: Kernel (ContextBuilder, Loader)                         │
│   + OS: Apps   (Pipeline, plugins, bounded helpers)             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

| OS Concept | Typical Owner | Responsibility |
|---|---|---|
| **OS: RAM** | session-scoped runtime context | Ephemeral active workspace during a turn or bounded flow. |
| **OS: SSD** | repositories and durable stores | Persistent truth for entities, logs, habits, and approved artifacts. |
| **OS: Kernel** | context builders / deeper memory loaders | Owns what enters RAM when the feature depends on deeper assembly. |
| **OS: Apps** | reasoning pipeline, plugins, bounded helpers | Operate on approved contracts rather than inventing storage rules. |
| **OS: Explorer** | dashboards, history views, read-only surfaces | Reads durable truth for inspection outside the live conversational lane. |

### 4.1 Strict interaction rules

1. Applications should work on approved runtime context rather than inventing hidden storage reads.
2. Heavy world loading belongs to explicit SSD -> RAM assembly, not accidental side effects.
3. Approved mutations must write through to durable storage via explicit repository/writer paths.
4. The owner of RAM lifecycle must stay explicit; random helpers must not quietly become memory kernels.

### 4.2 Base-runtime reminder

Not every current feature needs full Kernel-owned RAM.
Base-runtime work may stay much simpler.
The rule is boundary clarity, not forced complexity.

---

## 5. Dual-Lane Reasoning And Mutation Pattern

When a feature depends on deeper reasoning architecture, keep the lanes distinct.

### 5.1 Query / sync lane

Purpose:

- ground the request
- assemble only the needed context
- generate user-visible output from bounded reality

Typical flow:

1. phase-0 gateway evaluates short-circuit vs deeper handling
2. lightweight routing and candidate grounding happen first
3. if needed, explicit fetch/assembly builds the runtime context
4. the reasoning layer receives bounded context and emits response

### 5.2 Mutation / async lane

Purpose:

- convert approved outputs into strict typed persistence work
- keep durable writes off the fragile user-facing text path

Typical flow:

1. structured output is decoded into strict Kotlin shapes
2. repositories/writers validate and persist the change
3. runtime state refreshes through approved write-through or follow-up reload

### 5.3 Minor loops

Minor loops are trust-preserving interrupts, not failures.
Use them when certainty is insufficient.

Key minor loops:

- **clarification loop** when required certainty is missing
- **disambiguation loop** when multiple grounded candidates remain

Resume rule:

1. suspend the deeper path
2. yield the repair request to the user
3. receive the repair input
4. resume with the repaired certainty

### 5.4 Architecture takeaways

- do not treat the LLM as a free-form text-to-database bridge
- do not collapse query grounding and durable mutation into one shapeless step
- base-runtime features may use lighter plumbing, but the typed mutation boundary still matters whenever durable truth changes

---

## 6. Pipeline Valve Protocol

For pipeline-oriented features, debugging requires payload tracing rather than impressionistic logs.

### The mental model

- code paths are roads
- payloads are vehicles
- pipeline valves are checkpoints

When data disappears or mutates incorrectly, the fix starts by locating the checkpoint where the shape changed.

### Required checkpoint family

Use standardized checkpoints such as:

1. `[INPUT_RECEIVED]`
2. `[ROUTER_DECISION]`
3. `[ALIAS_RESOLUTION]` when applicable
4. `[SSD_GRAPH_FETCHED]` when deeper fetch happens
5. `[LIVING_RAM_ASSEMBLED]` when deeper RAM assembly happens
6. `[LLM_BRAIN_EMISSION]` when the reasoning layer emits structured output
7. `[LINTER_DECODED]` when strict typed decoding succeeds or fails

Path-specific checkpoints may include:

- optimistic parse or fast-lane parse
- plugin dispatch received
- repository write executed
- UI state emitted

Rule:

- if a pipeline feature cannot be traced through its required checkpoints, the observability surface is incomplete

---

## 7. Lifecycle Rule

For architecture-sensitive delivery, follow this sequence:

1. **Classify the work** using `docs/specs/base-runtime-unification.md`: base/shared, Mono-only, or wrapper debt
2. **Confirm architecture scope** here only if the task truly touches deeper architectural laws
3. **Update Core Flow** if behavior is changing
4. **Update feature specs / interfaces** for concrete implementation contracts
5. **Update interface map / trackers** when ownership or campaign state changes
6. **Execute** code against the current contracts
7. **Validate** with the appropriate test, runtime, or acceptance surface

Rule:

- do not skip the classification step
- do not invoke Mono architecture language for routine base-runtime work unless the dependency is real

---

## 8. Validation Gates

When reviewing architecture-sensitive work, verify the following:

- **No hardcoded mutation schemas**: typed contracts should come from real serializers/models, not prompt folklore.
- **Domain purity**: durable data shapes should stay out of Android-bound layers.
- **Typed mutation boundary**: durable writes must terminate in strict decoding rather than ad hoc regex or string surgery.
- **Linter simplicity**: parsing should be serializer-centered, not regex-centered.
- **JSON coercion resilience**: LLM-facing JSON parsing must tolerate benign null/value drift through the repo's approved serializer settings.
- **Enum safety**: persistence-to-enum decoding must use safe fallbacks rather than crash-prone direct `valueOf()` assumptions.
- **Spec alignment**: user-visible copy and contract details must match the owning docs.
- **Domain vs UI state decoupling**: factual domain truth and render-state flags must remain separate.
- **Central writer rule**: SSD mutation should flow through explicit repository/writer ownership rather than scattered writes.

---

## 9. Source-Of-Truth Rule

Do not treat architecture questions as one flat list.
Resolve conflicts by category.

### 9.1 Product identity and boundary conflicts

Use this order:

1. `SmartSales_PRD.md` for app identity, major surfaces, and journey-level intent
2. `docs/specs/base-runtime-unification.md` for shared non-Mono posture and the base-vs-Mono boundary
3. `docs/specs/Architecture.md` for deeper architectural law when that layer is truly in play
4. relevant tracker / interface-map notes

### 9.2 Feature behavior conflicts

Use this order:

1. relevant `docs/core-flow/**`
2. owning feature specs and interfaces
3. code and validation evidence

### 9.3 Concrete contract conflicts

Use this order:

1. actual Kotlin/domain contract and real data model
2. owning interface/spec docs
3. tracker and campaign notes

Practical rule:

- `base-runtime-unification.md` decides whether work is base-runtime or Mono
- `Architecture.md` constrains deeper system law when that deeper architecture is actually in play
- `core-flow/**` decides what the feature must do
- feature specs decide how to implement it

---

## 10. User POV And UX Implications

From the user's perspective, the architecture should stay invisible.
What matters is:

- grounded outputs instead of ghosted data
- calm clarification when certainty is missing
- durable writes that reflect validated truth rather than speculative prose

If a deeper architecture decision leaks confusion into the UI, the architecture has failed the product.

---

## 11. Glossary

- **Base runtime**: shared non-Mono product layer for shell/UI/UX, Path A scheduler, Tingwu/audio, and bounded continuity
- **Mono augmentation**: later deeper layer for Kernel memory, CRM/entity loading, plugin/tool runtime, and related intelligence
- **Core Flow**: feature behavior north star
- **RAM**: bounded active runtime context
- **SSD**: durable persistent truth
- **Kernel**: explicit owner of deeper RAM lifecycle
- **Phase-0 Gateway**: early routing/gating layer before deeper reasoning
- **Typed mutation boundary**: the point where durable writes must use strict structured contracts
- **Pipeline Valve**: standardized checkpoint for payload tracing
- **Wrapper debt**: compatibility host code that remains necessary temporarily but does not own product truth
