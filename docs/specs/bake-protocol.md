# BAKE — Backend Contract Protocol

> **Status**: Active northstar doctrine
> **Role**: Defines the kind of behavioral truth the BAKE transformation produces
> **Relationship to harness**: Companion doc — `harness-manifesto.md` governs how work is planned, verified, and closed; BAKE defines what kind of output to aim for. BAKE does not replace the harness.
> **Date**: 2026-04-28
> **Companion docs**:
> - `docs/specs/harness-manifesto.md` — execution protocol (how)
> - `docs/specs/Architecture.md` — runtime laws (telemetry joints, RAM/SSD model, typed mutation)
> - `docs/specs/base-runtime-unification.md` — BAKE/MONO feature boundary
> - `docs/specs/cross-platform-sync-contract.md` — how shared contracts flow to HarmonyOS and iOS
> **Scope**: Project northstar for backend-grounded, cross-platform-readable pipeline contracts across all Smart Sales backend domains

---

## 1. Why BAKE

**Cerb was right for 0→1.** When the product was undefined, UI-first specs were the fastest shared language. Screens and gestures anchored design conversations that pure backend contracts could not. That job is done — the product identity is proven, the code is battle-tested. Cerb specs served as the discovery tool; they are not the right tool for delivery at scale.

**BAKE is needed for 1→N and cross-platform.** HarmonyOS and iOS engineers cannot translate from Android UI patterns. Backend pipeline contracts are the only durable shared truth across platforms. Now that the code is battle-tested, the code itself is a more reliable record of delivered behavior than any cerb spec written from a UI-first perspective. BAKE formalizes this reality: write down what the battle-tested code actually does, in a format any platform engineer can translate from, and record the gap against the behavioral north star so nothing is lost.

---

## 2. BAKE vs Harness: Companion, Not Replacement

These two documents answer different questions.

| | `harness-manifesto.md` | `bake-protocol.md` |
|---|---|---|
| Question answered | How is work planned, executed, evidenced, and closed? | What kind of behavioral truth does a verified output contain? |
| Owns | Sprint contracts, evidence classes, operator/evaluator/planner roles, evaluation gates | Pipeline contract format, BAKE/MONO boundary definition, cross-platform readiness rules |
| Relationship | Active repo operating protocol | Active project northstar doctrine |

A BAKE contract is produced through the harness engine: a sprint contract scopes the work, the operator writes the contract, the evaluator verifies it against evidence, the user gates the close. The harness verifies the output; BAKE defines what a correct output looks like.

BAKE does not modify the harness resolution chain, the sprint contract schema, or any evidence class. Those remain governed by `harness-manifesto.md`.

---

## 3. BAKE vs MONO: Two Non-Inclusive Layers

```
BAKE ─────────────────────────────────────────────────────────────
  Verified pipeline contracts for active base-runtime domains.
  Grounded in battle-tested code.
  Complete and shippable without MONO.
──────────────────────────────────────────────────────────────────
              extends at declared surfaces, never restructures
MONO ─────────────────────────────────────────────────────────────
  Intelligence augmentation: Kernel memory, CRM/entity loading,
  Path B enrichment, plugin/tool runtime.
  Builds against BAKE's declared extension surfaces.
  Not prescribed until deliberately reintroduced.
──────────────────────────────────────────────────────────────────
```

### 3.1 Domain runtime classification

Every domain falls into one of three cases:

**base-runtime-active** — the domain is a current delivery target. A full BAKE implementation contract is written. A MONO extension surface may be declared if a clear seam exists, but is optional. The contract is complete and shippable as written.

**partially-shipped** — the base-runtime slice of the domain is live; an intelligence slice is defined but intentionally deferred. A full BAKE implementation contract is written for the base-runtime slice. A MONO extension surface declaration is required, identifying exactly where the intelligence layer would attach.

**mono-deferred** — the domain belongs entirely to the deferred intelligence layer. No BAKE implementation contract is written yet. The domain is tracked as a deferred entry in the triage seed (see §7) and will receive a BAKE contract when the Mono layer is eventually built. Calling a deferred northstar description a "contract" conflates implementation record with aspiration; the distinction is kept strict.

### 3.2 Invariants

- A BAKE contract (base-runtime-active or partially-shipped) is complete and shippable without MONO.
- MONO never restructures the Pipeline Contract section of an existing BAKE contract.
- When MONO is reintroduced for a domain, the engineer reads the partially-shipped contract's MONO Extension Surface declaration and builds against it. No BAKE contract rewrite is required.

---

## 4. Authority Model

BAKE contracts are **verified implementation contracts** — they sit below core-flow docs in the behavioral truth hierarchy and above cerb specs for implementation authority, but only after they are verified for a given domain through a transformation sprint.

```
Core-flow doc     behavioral north star; may be current or ahead of implementation
     |
BAKE contract     verified implementation record (battle-tested, grounded in code)
     |            records any gap against core-flow explicitly
Cerb spec         historical reference (design intent, UX reasoning; no pipeline authority)
     |
Code              implementation
```

**Before a BAKE contract is written for a domain:**
- The core-flow doc remains the behavioral north star.
- The cerb spec remains a reference candidate — not authoritative, not yet archived.
- The code is the implementation ground truth.

**After a BAKE contract is verified for a domain:**
- The BAKE contract is the authoritative implementation contract for that domain.
- The core-flow doc remains the behavioral north star above it. The BAKE contract records any gap explicitly.
- The cerb spec is demoted to reference-only for that domain through the transformation sprint that verified the BAKE contract.

**The global resolution chain in `harness-manifesto.md` is not modified by this doc.** When BAKE contracts exist for enough domains to justify a governance sprint, that sprint will update the chain. That is not this step.

---

## 5. The BAKE Contract Format

This is the recommended template for a backend-centric pipeline contract. It becomes a repo-wide mandate only after transformation sprints validate it against real domains. Early transformation sprints may propose minor adjustments; those proposals are evaluated before the template is locked.

```
---
protocol: BAKE
version: 1.0
domain: <domain-name>
layer: L<1-5>
runtime: base-runtime-active | partially-shipped
owner: <owning Kotlin module(s)>
core-flow-doc: <relative path to companion core-flow doc, if one exists>
last-verified: <YYYY-MM-DD>
---

## Pipeline Contract

### Inputs
What this pipeline receives. Named and typed, not prose.

### Outputs / Guarantees
What it emits. What the caller can rely on.

### Invariants
Rules that can never be violated. Each stated as MUST.

### Error Paths
How it fails safely. Every named non-happy path.
A contract with only happy-path coverage is incomplete.

## Telemetry Joints
Per Architecture.md section 6 — standardized checkpoint family.
- [INPUT_RECEIVED]: ...
- [ROUTER_DECISION]: ...
Add domain-specific joints as needed. If a critical joint cannot be traced, the
observability surface is incomplete and the contract is not verifiable.

## UI Docking Surface
Where the frontend attaches. One paragraph maximum.
Describes presentation behavior only — not pipeline behavior.
Composable hierarchy and animation details are not recorded here.

## Core-Flow Gap
Omit this section if the contract fully implements the companion core-flow doc.

Where the battle-tested code currently falls short of the core-flow behavioral
north star. Each gap is a named transformation backlog entry for a future sprint.
Record gaps explicitly rather than silently accepting them.

Example:
  Gap: core-flow section 3 requires a bounded grace window for HTTP delay;
  current code treats HTTP failure as an immediate reconnect trigger. Tracked
  for a future connectivity cleanup sprint.

## MONO Extension Surface
Optional for base-runtime-active: include only if a clear interface seam exists
where the intelligence layer would later attach.

Required for partially-shipped: the base slice is live; the intelligence slice
is deferred. Declare the extension surface explicitly so future Mono work has
a named target.

Passive declaration only. Nothing is implemented in this section.

Example:
  MONO extension: ContextBuilder would assemble full Kernel RAM at this boundary
  instead of bounded session context. Interface seam: EnhancedContext.kernelSlot
  (currently always null in base runtime).

## Test Contract
- Evidence class: contract-test | platform-runtime | ui-visible
- Named test files or test cases that verify the critical invariants.
- Minimum evidence required to consider this contract verified.

## Cross-Platform Notes
What a HarmonyOS or iOS engineer needs to translate this contract natively.
No Android-specific implementation details. Behavior contract and
platform-neutral constraints only. If behavior is identical across platforms,
this section may say so explicitly and be otherwise empty.
```

---

## 6. Transformation Path

The northstar doc defines the target state. Getting there is a separate transformation project run through the harness: sprint contracts authored by Claude, operated by Codex, evidenced per harness evidence classes, gated by the user at close.

### 6.1 Complete Fix and Backfill Rule

BAKE transformation does not use runtime evidence only to label gaps. When L3
device-loop evidence reveals a real bug in the behavior a BAKE sprint claims or
future BAKE contracts will rely on, the operator must treat the bug as part of
the sprint's completion work unless the user explicitly splits it into a new
catch-up sprint.

The close path is:

1. Capture the failing branch with the deterministic device-loop protocol.
2. If logs are insufficient, add the smallest targeted telemetry and rerun the
   same L3 scenario before changing behavior.
3. Fix the implementation branch that caused the failed runtime claim.
4. Add or update focused L1/L2 tests for the corrected branch.
5. Rebuild, reinstall or cold-launch as appropriate, and rerun the same L3
   scenario until it passes, blocks on hardware, or hits the declared stop
   criteria.
6. Backfill the BAKE contract, core-flow gap notes, sprint ledger, tracker, and
   any relevant interface-map or supporting docs so the documents describe the
   repaired behavior, not the pre-fix assumption.

Adjacent findings that do not affect the sprint's claim may still become
follow-up work, but they must be named as unverified or deferred branches. A
known product-path bug must not be hidden as a harmless documentation gap.

### 6.2 Code Delta Transparency

Any BAKE sprint that changes code must close with an explicit code-delta
transparency table. This is required even when the implementation is small.

The table exists to make the engineering effect visible, not to restate the
diff. It must identify what system truth became explicit, what hidden
assumption was killed, whether the code actually became simpler, and what debt
or unverified branch remains.

Minimum rows:

| Area | Required disclosure |
|---|---|
| Contract delta | What state, ownership, interface, invariant, or pipeline contract became explicit that was previously implicit, flattened, or wrong. |
| Behavior delta | What runtime behavior changed for users, devices, queues, retries, errors, or UI. |
| Simplification delta | What became easier to reason about: fewer branches, fewer late checks, less global state, clearer ownership, or a narrower state machine. |
| Drift corrected | Which doc-code, test-code, state-machine, telemetry, or UI drift was corrected. |
| Assumption killed | Which happy-path, single-device, always-ready, always-online, or "current active thing is correct" assumption was removed. |
| Duplication/dead code | What duplicate or dead logic was removed, merged, or intentionally left because removing it would exceed scope. |
| Blast radius | Which modules/files were touched, and whether the sprint reduced, increased, or contained large-file/module pressure. |
| Tests added/changed | What exact invariant each new or changed test protects. |
| Runtime evidence | What L3/device evidence proved, what it did not prove, and why. |
| Residual risk/debt | What remains risky, deferred, partially proven, or only covered below L3. |
| Net judgment | One plain-language sentence: cleaner, neutral, or worse, and why. |

If a row does not apply, write `None in this sprint` plus the reason. Do not
omit rows because the answer is uncomfortable or uneventful.

Each BAKE sprint that changes code must also receive three 1-5 scores after the
code-delta table:

1. **Pre-BAKE codebase score** — the quality of the incoming codebase slice
   before this sprint's changes.
2. **Work score** — how well this sprint was executed.
3. **Baked-codebase score** — the quality of the resulting codebase slice after
   this sprint, regardless of whether the sprint itself was well executed.

| Score | Meaning |
|---|---|
| 5 | Excellent: explicit contracts, simple ownership, low drift, focused blast radius, strong negative coverage, and L3 proof where runtime matters. |
| 4 | Good: clear improvement and solid tests/evidence, with limited unproven branches or remaining structural debt. |
| 3 | Adequate: useful and mostly correct, but simplification, evidence, or codebase quality remains meaningfully incomplete. |
| 2 | Weak: behavior moved, but complexity, drift, weak tests, or broad blast radius make the outcome questionable. |
| 1 | Poor: contract truth is still unclear, code became harder to reason about, or verification is insufficient for the claim. |

All scores must include one-sentence justifications. Do not round up for green
tests alone. Penalize hidden assumptions, broad blast radius, missing negative
cases, god-file worsening, and unproven hardware branches. It is valid for the
scores to differ; for example, a sprint can start from a 2/5 slice, execute at
4/5, and leave a 3/5 baked codebase if inherited structural debt remains.

The transformation project follows this pattern:

- **Triage sprint first**: classify all active domains using §3.1's runtime classification and §7's triage seed as a starting point. Produce a prioritized backlog. No code or doc changes in this sprint.
- **Domain sprints**: one sprint per domain cluster. For each: read the battle-tested code, read the core-flow doc, write the BAKE contract grounded in code, verify with evidence, demote the cerb spec to reference-only, update the interface-map to cite the BAKE contract.
- **Cerb archival**: once a BAKE contract has been verified through at least one sprint close for a domain, the cerb spec for that domain is moved to `docs/cerb/archive/<domain>/`. Cerb specs are never deleted — they carry design reasoning that may be useful for future platform translation.
- **Core-flow reclassification**: on first touch of any core-flow doc during transformation, add a `scope:` frontmatter field (base-runtime-active | mono-deferred | mixed). Do not reclassify docs that have not been opened for active work.
- **Governance update**: when BAKE contracts cover enough domains to justify it, a governance sprint updates `harness-manifesto.md` and `cross-platform-sync-contract.md` to reflect BAKE contracts as a named shared artifact. This is a late-stage step, not an early one.

The badge-session-lifecycle sprint 08 (blocked on device access) is the natural first domain to absorb into transformation: the connectivity corridor has already been cleaned, and a BAKE contract writeup is the missing piece needed to close it with proper evidence.

This northstar doc is the prerequisite for the transformation project. The transformation project is not launched here.

---

## 7. Non-Binding Triage Seed

> **Non-binding.** This table is a first-pass classification to seed the transformation project's triage sprint. Every entry is subject to confirmation when the domain is actually opened. Do not treat this as an authority assignment — it is a reading aid only.

| Core-flow doc | Likely classification | Notes |
|---|---|---|
| `badge-connectivity-lifecycle.md` | base-runtime north star | Tier 2 format (no formal metadata); needs format upgrade on touch |
| `badge-session-lifecycle.md` | base-runtime north star | Tier 2 format; needs format upgrade on touch |
| `sim-shell-routing-flow.md` | base-runtime north star | |
| `sim-scheduler-path-a-flow.md` | base-runtime north star | |
| `scheduler-fast-track-flow.md` | base-runtime north star | |
| `sim-audio-artifact-chat-flow.md` | base-runtime north star | Feature in progress |
| `base-runtime-ux-surface-governance-flow.md` | base-runtime north star | Different header format; UX governance focus |
| `system-typed-mutation-flow.md` | mixed | Base typed mutation shipped; Mono enrichment deferred |
| `system-query-assembly-flow.md` | mixed | Phase-0 + base LLM shipped; CRM/entity Mono-deferred |
| `system-session-memory-flow.md` | mixed | Bounded continuity shipped; Kernel RAM Mono-deferred |
| `system-reinforcement-write-through-flow.md` | mixed | RLModule shipped; deeper intelligence Mono-deferred |
| `system-plugin-gateway-flow.md` | Mono north star | PluginRegistry base shell shipped; richer capability Mono-deferred |
| `scheduler-memory-highway-flow.md` | Mono north star | Path B fully Mono-deferred |

**mono-deferred domains** in the above table will not receive BAKE contracts until a deliberate decision is made to reintroduce the intelligence layer for that domain. Their core-flow docs remain valuable as behavioral north stars for that future work. They are not archived.

---

## 8. Cross-Platform Readiness

BAKE contracts are the shared product truth for all platforms. The cross-platform flow is:

```
BAKE contract (backend-grounded, platform-neutral)
     |
     +--> Android implements natively in Kotlin/Compose
     +--> HarmonyOS translates natively in ArkTS/ArkUI
     +--> iOS translates natively in Swift/SwiftUI (future)
```

`docs/specs/cross-platform-sync-contract.md` currently lists `docs/cerb/**` as the shared implementation contracts. As BAKE contracts are verified domain by domain, a governance sprint will update that table to cite BAKE contracts as the primary shared implementation authority for each domain.

Rules for cross-platform-safe BAKE contracts:
- The Pipeline Contract, Invariants, and Error Paths sections must contain no Android-specific details.
- The UI Docking Surface section describes behavior and entry points, not Compose composable structure.
- The Cross-Platform Notes section explicitly surfaces anything the translator needs to handle differently per platform.
- The MONO Extension Surface is platform-neutral by design — the intelligence layer is a shared architectural concern, not a platform-specific one.
- If a cerb spec contains Android-specific UI reasoning that is irrelevant to the pipeline contract, it is left in the cerb archive and not carried into the BAKE contract.
