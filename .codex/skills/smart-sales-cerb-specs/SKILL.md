---
name: smart-sales-cerb-specs
description: Cerb concept, spec, interface, UI, plugin, and test-document scaffolding for the Smart Sales repository. Use when Codex needs to create or update Smart Sales docs before implementation, choose between concept and formal spec paths, scaffold `docs/cerb/**` artifacts, register tracker or interface-map entries, or adapt the Antigravity Cerb template workflows into Codex behavior.
---

# Smart Sales Cerb Specs

Use this skill when the repo needs documentation work before or alongside implementation.

## Choose the right document path

When a feature already has a `docs/core-flow/**` document, treat that as the behavioral north star and write the Cerb spec beneath it.

### Use a concept doc first when:

- The domain is new and still fuzzy
- The user flow is unclear
- The task needs unconstrained brainstorming before contracts

Concept output belongs under `docs/to-cerb/**`.

### Use a formal Cerb doc when:

- The feature, UI, plugin, or test area is understood enough to specify
- An owning shard should exist in `docs/cerb/**`, `docs/cerb-ui/**`, `docs/cerb-plugin/**`, or `docs/cerb-e2e-test/**`

## Scaffolding Workflow

### 1. Identify the doc type

Pick the narrowest match:

- Feature contract: `spec.md` plus `interface.md`
- UI contract: UI state, intents, layout, and interaction rules
- Plugin contract: public interface, data models, execution flow
- E2E test contract: boundaries, scenarios, and wave plan
- Concept doc: mental model, user flow, boundaries, and anti-hallucination constraints

### 2. Load shared context

- `docs/plans/tracker.md`
- `docs/cerb/interface-map.md`
- Relevant specs, UX contracts, or SOPs
- Existing neighboring shard docs so names and ownership stay consistent

### 3. Write the minimum valid contract

The created doc should define:

- Ownership and boundary
- Public interface or major states
- Data models or execution flow as needed
- Wave or implementation plan
- Registration targets such as tracker or interface map updates

### 4. Preserve human reality

Every meaningful spec should capture the messy real-world constraints, not only the clean flow.

Include an intricacies section when relevant:

- Organic UX
- Data reality
- Failure gravity

### 5. Register the new shard

If the new contract changes project topology:

- Update `docs/plans/tracker.md`
- Update `docs/cerb/interface-map.md`
- Update any navigation docs that should point to the new shard

### 6. Collapse stale concept docs

If you convert `docs/to-cerb/**` thinking into a formal Cerb shard:

- Migrate the useful constraints into the formal doc
- Remove or archive the stale incubation copy so the repo does not keep duplicate truth

## Anti-Patterns

- Writing code without first creating the missing spec
- Creating a new shard for work that is actually cross-cutting and already owned elsewhere
- Copying neighboring specs without rethinking ownership
- Leaving a concept doc and a formal spec in conflict

## Source References

Read `references/source-map.md` for the exact Antigravity template workflows this skill adapts.
