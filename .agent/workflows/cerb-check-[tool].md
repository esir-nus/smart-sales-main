---
description: Audit any Cerb doc (Feature, UI, Plugin, Testing) for context boundary, OS Model, and anti-illusion compliance
last_reviewed: 2026-04-13
---

# Cerb Doc Check (The Context-Aware Linter)

Verify that a Cerb doc follows context boundary principles and its specific domain architecture.

---

## When to Use

- **Mandatory**: After completing Wave 1 of any `/cerb-*-template` workflow.
- **Optional**: When updating existing Cerb docs (run on modified files only).
- **Migration**: Auditing legacy docs for compliance.

---

## Prerequisites

This workflow validates docs created via `/cerb-spec-template`, `/cerb-ui-template`, `/cerb-plugin-template`, or `/cerb-e2e-testing-template`.

Every domain relies on the **`spec.md` + `interface.md`** duality:
- `spec.md` = Internal Implementation
- `interface.md` = External Contract (Method signatures, State Definitions, Schema, or Matrix)

---

## Stage 0: Presence & Routing Triage

If the target Cerb doc does NOT exist, ABORT the check and evaluate the task:
- Does it need visual states? → Run `/cerb-ui-template`
- Is it a standalone tool? → Run `/cerb-plugin-template`
- Is it a QA script? → Run `/cerb-e2e-testing-template`
- Is it core logic? → Run `/cerb-spec-template`
- Does it span multiple modules/features? → **STOP.** Re-evaluate the architecture. Do not force cross-cutting execution. Create a new, higher-level integration Cerb doc or break down the task.

---

## Stage 1: Universal Rules (Apply to ALL Specs)

Before checking domain rules, verify these baseline invariants:

### 1. Context Boundaries vs Reference Context
| Check | Pass | Fail |
|-------|------|------|
| **Spec → Interface** | ✅ Terminal link | — |
| **Spec → Glossary** | ✅ Quick lookup | — |
| **Spec → Code file** | ✅ Implementation ref | — |
| **Spec → Another Interface** | ✅ Reference bounds | — |
| **Spec → Another Spec** | ⚠️ Avoid | ❌ Business logic leak |

- **Business Logic Ownership**: A spec MUST own 100% of its own business logic. No "See X for how this algorithm works."
- **Reference Context**: A spec CAN and SHOULD link to other `interface.md` files when defining its dependencies (e.g., "We query `docs/cerb/entity-registry/interface.md`"). 
- **Single-Spec Focus**: In most cases, it is strictly one Cerb spec per task. If a task spans 5 modules and demands cross-cutting context checks across multiple specs, **take this as a sign for re-evaluation**. Do not force a cross-cutting check; instead, evaluate if a new, higher-level Cerb doc (like an integration spec) needs to be created, or if the task should be broken down.

**Run:** `grep -n "See\|see\|参见" [doc.md]` to find cross-references.

### 2. Self-Containment
| Question | Expected |
|----------|----------|
| Can an agent complete the task reading ONLY this doc? | ✅ Yes |
| Are all necessary domain models defined inline? | ✅ Yes |
| Are edge cases defined inline, not linked? | ✅ Yes |

### 3. Atomicity (The "Cerb Shard" Rule)
| Check | Pass | Fail |
|-------|------|------|
| **Scope** | ✅ Single component/slice | ❌ "God Spec" covering entire subsystems |
| **Responsibility** | ✅ One clear architectural role | ❌ Mixes SSD mutation, RAM caching, and UI layouts |

- **No God Specs**: A spec must represent a manageable "Cerb Shard" (e.g., just the Repository layer, or just the Pipeline router). If a spec dictates UI animations *and* database migrations, it FAILS atomicity and must be broken down.

### 4. Wave Planning
| Check | Expected |
|-------|----------|
| Does `spec.md` contain a Wave Plan (delivery states) table? | ✅ Required for all domains |

---

## Stage 2: Domain-Specific Gauntlet

Identify the **Spec Type** based on its file path or header, then run ONLY the relevant domain checks.

### Domain A: Feature Specs (`docs/cerb/[feature]/`)
> *Core system logic (e.g., Memory Center, Entity Writer).*

| Check | Requirement |
|-------|-------------|
| **OS Layer** | MUST declare an OS Layer in header: `RAM Application`, `SSD Storage`, `Kernel`, or `File Explorer`. |
| **OS Interaction** | Application layer CANNOT directly access `.Repository` (Must go through SessionWorkingSet). |
| **Interface Clarity** | `interface.md` MUST list Kotlin method signatures and I/O types explicitly. |
| **Boundary Guard** | `interface.md` MUST contain a "You Should NOT" section establishing misuse rules. |

### Domain B: UI Specs (`docs/cerb-ui/[feature]/`)
> *Visual components and screens.*

| Check | Requirement |
|-------|-------------|
| **State Definitions** | `interface.md` MUST define EXACT UI States (e.g., Loading, Valid, Error, Empty). |
| **VM Isolation** | Spec MUST prove it is a "Dumb UI". UI cannot make network/database calls directly; it only receives state from ViewModel. |
| **Interactions** | User gestures (clicks, swipes) MUST be mapped to Intent triggers. |

### Domain C: Plugin Specs (`docs/cerb-plugin/[plugin]/` or `docs/cerb/`)
> *System III executable workflows.*

| Check | Requirement |
|-------|-------------|
| **Manifest** | Spec MUST define a Permission Manifest (what data/APIs it needs). |
| **Gateway Sync** | MUST declare its Gateway Rendezvous type (Synchronous fast-track vs Asynchronous background). |
| **I/O Schema** | `interface.md` MUST strictly define the JSON input arguments and output format expected by the LLM. |

### Domain D: Testing Specs (`spec_test-[feature].md` or `docs/cerb-test/`)
> *QA and E2E verification.*

| Check | Requirement |
|-------|-------------|
| **Pillar Mapping** | MUST explicitly map to one of the **6 E2E Pillars** (from `smart_sales_testing_protocol.md`). |
| **Fake > Mock** | Spec MUST declare the use of `Fake` repositories. `Mockito.mock` is banned for Layer 2/3 state layers per Anti-Illusion protocol. |
| **RED-First** | MUST explicitly define negative testing scenarios (Missing context, nulls, timeouts). |
| **Verify Payloads** | If downstream mocks are used (e.g., L1 tests), MUST explicitly require `verify(mock).method(argThat { ... })`. |

---

## Output Format

Generate your audit report precisely in this format:

```markdown
## Cerb Audit: [doc name]
**Classification**: [Feature | UI | Plugin | Testing]

### Stage 1: Universal Rules
- [x] Link Purity (No spec-to-spec logic chains)
- [x] Self-containment (Models/Edge cases inline)
- [x] Atomicity (Scope is a manageable 'Cerb Shard', not a God Spec)
- [ ] FAILED: Wave Plan missing from spec.md

### Stage 2: [Domain Name] Checks
- [x] [Check 1 mapping]
- [x] [Check 2 mapping]
- [ ] FAILED: [Explanation of what failed based on the Domain table]

### Lifecycle Gate (Doc → Plan → Execute)
- [ ] Spec is validated.
- 🛑 **STOP**: Do NOT write code yet. You must now run `@[/feature-dev-planner]` to generate the implementation plan. 

### Verdict
**[PASS / NEEDS WORK / FAIL]**

### Fixes Required
1. [Specific actionable fix to satisfy the failed rules]
```
