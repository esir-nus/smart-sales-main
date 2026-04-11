---
name: smart-sales-feature-delivery
description: Docs-first feature planning and delivery for the Smart Sales repository. Use when Codex needs to plan, implement, or finish a feature in this repo, decide the owning spec, build an implementation plan, follow ship gates, or adapt the Antigravity feature-dev workflow into Codex behavior.
---

# Smart Sales Feature Delivery

Use this skill for feature work in this repository. It ports the intent of the Antigravity feature-planning workflow into a Codex-native execution pattern.

## Lane Harness Rule

Feature delivery in this repo is lane-first by default.

- Treat `docs/sops/lane-worktree-governance.md` and `ops/lane-registry.json` as operational law, not optional references.
- Before any feature edit, decide whether the task fits an existing lane or requires a new lane.
- Do not continue feature edits in the integration tree.
- If lane ownership, worktree, or owned paths are unclear, stop and resolve that first.
- Use `scripts/lane` commands for lane creation, validation, status, and publication instead of improvising raw git workflow.

This rule should be enforced wisely:

- governance-only work may remain in the integration tree when it fits the declared admin/control-plane scope
- feature planning may begin before implementation, but the plan must still identify the lane/worktree decision before code edits
- cross-runtime collaboration with Codex, Antigravity, and Claude is normal; shared control-plane surfaces still need explicit lane ownership

## Delivery Order

Follow this repository-specific order:

1. Core Flow: what must happen
2. Spec: how to implement it
3. Code: delivered behavior
4. PU Test: validates code against Core Flow
5. Fix loop: repair lower layers until they match

If a feature has a `docs/core-flow/**` document, treat it as the behavioral north star before treating lower specs as final.

## Load Context

Start with the repo contract and the owning docs:

1. Read `AGENTS.md`.
2. Read `docs/sops/lane-worktree-governance.md`.
3. Read `docs/plans/dirty-tree-quarantine.md`.
4. Read the relevant `docs/core-flow/**` doc when it exists.
5. Read `docs/plans/tracker.md`.
6. Read the owning spec or SOP in `docs/cerb/**`, `docs/specs/**`, or `docs/sops/**`.
7. Read `docs/cerb/interface-map.md` if the change spans modules.
8. Read `.agent/rules/lessons-learned.md` if the task resembles a known trap.

Open `docs/specs/Architecture.md` only when the task clearly requires architecture-law interpretation or the user explicitly wants that document consulted.

If the task has no owning spec or interface, switch to `$smart-sales-cerb-specs` or create/update the docs first before planning code.

## Delivery Workflow

### 1. Scope the work

- Prefer one feature task to map to one owning `spec.md` plus one owning `interface.md`.
- If the request is truly cross-cutting, say so explicitly instead of inventing a fake single owner.
- Treat tracker entries as navigation, not business logic.

### 1A. Decide the lane before editing

- Determine whether the task fits an existing lane in `ops/lane-registry.json` and `docs/plans/dirty-tree-quarantine.md`.
- If the task does not fit a current lane, define a new bounded lane before implementation starts.
- Identify `owned_paths`, any narrowly justified `allowed_shared_paths`, expected branch, and recommended worktree.
- If implementation is expected, create or attach the lane worktree and validate it before editing feature files.
- If the user has switched Codex into Plan mode, the lane/worktree decision should still be part of the plan before any code step.

### 2. Quote the source behavior

- Pull implementation intent from the owning spec, not from a wave title or tracker bullet.
- If a Core Flow doc exists, use it as the behavioral authority and use the spec as the implementation contract beneath it.
- If the spec is silent, stop and ask or update the spec first.
- Do not invent behavior from adjacent modules.

### 3. Check the shape of the system

- Verify who owns the data in `docs/cerb/interface-map.md`.
- Keep `domain/` pure: no `android.*`, no legacy imports, no accidental cross-module ownership edges.
- Read legacy code only to understand behavior, then implement from current contracts.

### 4. Search the code before planning

Use repo evidence before writing the plan:

```bash
rg -n "FeatureName|TargetType|TargetState" .
rg --files | rg "Feature|Module|ViewModel|Repository"
```

- Reuse existing UI if it already exists and is merely unwired.
- Prefer wiring or rewriting over parallel duplicate implementations.

### 5. Clean before build

- Remove placeholder branches, fake success data, or stale TODO scaffolding in the touched path when they directly interfere with the feature.
- Do not preserve skeleton behavior just because it is already checked in.

### 6. Plan from first principles

Ask:

- What user problem is being solved?
- Is there a simpler implementation?
- Does this need LLM reasoning or just deterministic code?
- What breaks if the assumption is wrong?

Prefer simple local code over abstractions added “for future flexibility”.

### 7. Implement with observable behavior

- Add or preserve enough logging and traceability to debug the flow.
- Keep naming literal and searchable.
- Avoid scattering config or ownership across unrelated modules.

### 8. Ship the whole slice

After the code change:

- Update the owning spec if behavior, states, or models changed.
- Update `docs/plans/tracker.md` when status or debt changed.
- Update `docs/plans/dirty-tree-quarantine.md` and `ops/lane-registry.json` when lane ownership, status, handoff, or bounded scope changed.
- Update `docs/cerb/interface-map.md` if module edges changed.
- Run the relevant verification.
- Use `scripts/lane status`, `scripts/lane commit`, `scripts/lane push`, or `scripts/lane ship` for lane-local publication.

## Mandatory Checks

Before saying a feature plan is ready, verify:

- The lane decision is explicit: existing lane, new lane, or governance-only exception.
- The feature will not be edited from the integration tree.
- The Core Flow source is identified when one exists.
- The owning spec and interface are identified.
- The behavior is quoted from the correct source.
- Cross-module ownership is understood.
- The implementation path respects clean boundaries.
- Verification steps are listed.
- The doc sync target list is clear.

## Anti-Patterns

- Planning from tracker bullets alone
- Treating lane setup as optional for feature work
- Starting implementation in the integration tree and trusting hooks to sort it out later
- Treating current code as more authoritative than a valid Core Flow north star
- Implementing code before reading the owning spec
- Copying legacy structure instead of rewriting from the current contract
- Treating a cross-cutting task as one fake shard
- Shipping code and promising to sync docs later

## Source References

Read `references/source-map.md` when you need the exact Antigravity workflow and rule files this skill adapts.
