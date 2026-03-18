---
name: smart-sales-acceptance
description: Acceptance and verification workflow for the Smart Sales repository. Use when Codex needs to validate completed work, design or run verification plans, write acceptance reports, apply anti-illusion testing rules, perform L1-L3 checks, or adapt the Antigravity acceptance-team and on-device-test workflows into Codex behavior.
---

# Smart Sales Acceptance

Use this skill when work is entering verification, acceptance, or release-readiness review.

## Core Rule

Verification must be concrete, hostile, and evidence-based.

- Do not claim “verified manually” without concrete steps.
- Prefer affected-module verification over a single cherry-picked test.
- Include failure-path thinking, not only the happy path.
- When a corresponding `docs/core-flow/**` document exists, acceptance MUST read it first and use it as the behavioral north star.
- A green build does not override Core Flow drift. Acceptance must explicitly judge whether the delivered result aligns with the Core Flow, not only whether tests passed.

Validate against the Core Flow north star when one exists, not only against the current implementation.

## Acceptance Workflow

### 1. Load the contract

- Read the relevant Core Flow doc when it exists. This is mandatory, not optional.
- Read the owning spec, interface, or acceptance target doc.
- Read the touched code.
- Read relevant tests before judging coverage.

If no Core Flow doc exists, say that explicitly in the acceptance result instead of silently falling back to lower docs.

### 2. Run the four examiners

#### Spec examiner

- Does the implementation match the required behavior and states?
- Are exact strings, states, or structure requirements satisfied?
- Does the implementation align with the corresponding Core Flow branches, invariants, and exit conditions?

#### Contract examiner

- Do interfaces, data models, and ownership boundaries still line up?
- Are there undocumented signature or dependency changes?

#### Build examiner

- What relevant builds or tests were run?
- If nothing ran, state that clearly.

#### Break-it examiner

- What happens with empty input, invalid identifiers, offline or missing dependencies, and partial context?
- What would fail if the core logic were bypassed?

### 3. Apply anti-illusion testing rules

- Prefer fakes over mocks for stateful layers.
- Do not use mocks to fabricate a fully satisfied pipeline state.
- Verify downstream payloads, not only final success states.
- Require tests for partial or insufficient context when the pipeline has gating behavior.

### 4. Use L1, L2, and L3 appropriately

- L1: unit or module-level verification
- L2: simulated or debug-assisted validation
- L3: full app or on-device validation when requested or necessary

For UI-heavy or device-specific work, use on-device validation when possible and report what was or was not exercised.

### 5. Write the verdict

An acceptance result should state:

- What was checked
- What evidence exists
- What failed or remains unverified
- Whether the change is accepted, conditionally accepted, or blocked
- Whether the result is aligned with the corresponding Core Flow doc, and if not, which branch or invariant still drifts

## Anti-Patterns

- Declaring success from green mocks
- Skipping negative cases
- Reporting only final UI state without checking what was passed downstream
- Hiding missing verification behind optimistic language

## Source References

Read `references/source-map.md` for the exact Antigravity workflows and rules this skill adapts.
