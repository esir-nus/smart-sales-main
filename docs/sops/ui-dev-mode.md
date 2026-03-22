# UI Dev Mode

> Purpose: Define the controlled UI development model for designers, developers, and agents.
>
> Goal: Allow free visual exploration without losing consistency, professionalism, or implementation control.
>
> Status: Active SOP
>
> Last Updated: 2026-03-22

---

## Why This Mode Exists

This repo treats UI as a surface layer connected to product behavior through explicit contracts.

The system should support both:

- visual freedom at the prototype stage
- strict consistency at the production stage

UI work must not depend on repeated codebase spelunking just to rediscover how the product should look and feel.

Instead, UI work should follow a stable document chain:

1. surface contract
2. visual identity guide
3. UI element registry
4. prototype-first workflow
5. approved production transplant
6. UI tracker

---

## Core Mental Model

Use this model when discussing UI work:

- skeleton = domain truth, business rules, persistent data
- muscles = state machines, ViewModels, intents, orchestration
- skin = layout, styling, motion, visual hierarchy, presentation

The skin may evolve freely, but it must stay attached to the same behavioral body.

That means:

- visual form may change
- UX invariants may not break
- business logic may not leak into raw UI styling decisions
- production implementation must still obey the real state and intent contracts

---

## Working Names

Some existing files still use older naming. For this workflow, use the intuitive names below when thinking and communicating:

| Working Name | Current Repo File | Role |
|--------------|-------------------|------|
| Surface Contract | `docs/specs/prism-ui-ux-contract.md` | UI boundary, state/intent decoupling, feature-spec index |
| Visual Identity Guide | `docs/specs/style-guide.md` | Global visual law, aesthetic standards, token intent |
| UI Element Registry | `docs/specs/ui_element_registry.md` | Component behavior, states, layers, invariants |
| Prototype-First UI Workflow | `docs/sops/ui-building.md` | Prototype, approval, transplant sequence |
| UI Tracker | `docs/plans/ui-tracker.md` | UI-specific execution tracking, approval state, visual drift |

Rule:

- prefer intuitive names in discussion
- preserve existing file paths unless a deliberate doc migration is approved

---

## When To Use UI Dev Mode

Use this mode when:

- designing a new screen
- redesigning a major surface
- creating a new visual pattern
- refining interaction polish
- building a web prototype before Android implementation
- transplanting an approved prototype into Compose

Do not use this mode for small copy-only edits or tiny visual bug fixes unless they affect shared patterns.

---

## Which SOP To Invoke

For actual interface-building work, invoke:

- `docs/sops/ui-building.md`

Treat this file as the governing model behind that SOP.

Simple rule:

- `ui-building.md` = the execution SOP
- `ui-dev-mode.md` = the background law and explanation

Developers normally should not need to invoke both separately.

---

## Required Reading Order

Before doing major UI work, read in this order:

1. `docs/plans/ui-tracker.md`
2. `docs/specs/prism-ui-ux-contract.md`
3. `docs/specs/style-guide.md`
4. `docs/specs/ui_element_registry.md`
5. feature-specific spec in `docs/cerb-ui/**` or `docs/cerb/**`
6. `docs/sops/ui-building.md`
7. `docs/plans/tracker.md` when the UI task is part of a larger product/feature wave

If a `docs/core-flow/**` doc exists for the feature, read it before treating lower docs as final.

---

## The Workflow

### Phase 1: Frame the Surface

Understand:

- the feature goal
- the real user task
- the state and intent boundary
- the UX invariants that must survive any redesign

Output:

- concise UI brief
- list of locked constraints
- list of flexible visual areas

### Phase 2: Prototype First

Create a web prototype before Android implementation for major UI work.

The prototype should:

- follow the Visual Identity Guide
- reuse existing registry patterns where possible
- explore layout and visual direction freely
- avoid violating platform or product reality

Rule:

- do not treat prototype code as production code

### Phase 3: Screenshot Critique

After the prototype is rendered, review it from screenshots.

Check:

- visual consistency
- hierarchy clarity
- spacing rhythm
- component fidelity
- professionalism
- accessibility and contrast risks
- drift from the Visual Identity Guide
- drift from the UI Element Registry

This is a critique step, not vague “learning.”

### Phase 4: Human Approval

Before production transplant, the human reviewer must confirm the prototype direction.

No major UI redesign should skip this gate.

### Phase 5: Production Transplant

Only after approval:

- map prototype elements to registry elements
- map approved visuals to real Compose structure
- connect the UI to the real state and intent contract
- implement production-safe behavior

Rule:

- transplant approved design intent, not raw prototype hacks

### Phase 6: Verification

Verify production UI against:

- approved prototype
- Visual Identity Guide
- UI Element Registry
- Surface Contract
- feature behavior spec

---

## Non-Negotiable Rules

### 1. Contract First

The UI reads state and emits intents.

It must not directly depend on:

- database row shapes
- pipeline internals
- LLM internal structures
- storage-layer implementation details

### 2. Prototype Before Major Android UI Work

For meaningful screen or pattern work:

- prototype first
- approve
- transplant second

Do not jump straight into Compose unless the task is minor.

### 2a. Current UI Is Not The Aesthetic Teacher

The current app UI often exists partly for testing convenience, state validation, and flow coverage.

Therefore:

- agents may inspect current UI to understand logic, state flow, interaction wiring, and implementation constraints
- agents must not treat current UI as the visual source of truth by default

Do not learn the target aesthetic, polish standard, spacing quality, or motion language from the current production UI unless it is explicitly marked as an approved visual reference.

Visual source-of-truth order:

1. approved prototype screenshots
2. Visual Identity Guide
3. UI Element Registry
4. explicit approved visual references
5. current UI code and screenshots for logic understanding only

### 3. No Silent New Design Language

If a task introduces a new recurring pattern, update the relevant guide or registry in the same session.

Examples:

- new card family
- new button style
- new overlay layer
- new motion rule
- new spacing/radius convention

### 4. Registry Beats Improvisation

If a known element already exists in the registry, reuse or extend it deliberately.

Do not create near-duplicates by accident.

### 5. Human Approval Is a Real Gate

Prototype approval is not optional for major UI changes.

### 6. Production Still Obeys Real Contracts

Visual freedom does not allow:

- breaking state semantics
- hiding critical feedback
- removing required error/disabled/loading states
- bypassing existing feature ownership

---

## Expected Deliverables For Major UI Work

For a meaningful UI task, the working set should usually include:

- UI brief
- prototype
- screenshot set
- critique notes
- approval note
- transplant mapping
- production implementation
- verification notes
- UI tracker entry

---

## Done Criteria

A major UI task is not done until:

- the prototype direction is approved
- production code matches the approved direction closely enough
- the implementation still obeys the real state/intent contract
- any new shared pattern is documented
- relevant verification is complete

---

## Relationship To Existing Docs

This SOP explains the dev model.

It does not replace:

- the Surface Contract
- the Visual Identity Guide
- the UI Element Registry
- feature-level UI specs

It tells developers and agents how to use them together in one controlled system.
