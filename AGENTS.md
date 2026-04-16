# Codex Repository Guidelines

This file is the Codex-facing repository contract.

Antigravity assets in `.agent/` remain intact and valid for Antigravity or IDE-based review flows. Both runtimes share the same project docs in `docs/`.

## Runtime Split

- `AGENTS.md`: Codex-native working rules
- `.codex/skills/*.md`: Codex-native project skills
- `.agent/rules/*.md`: Antigravity-native working rules
- `docs/**`: Shared project memory, specs, trackers, reports, and SOPs

Do not treat the Codex layer as a replacement for `.agent/`. Preserve both unless the user explicitly asks to migrate or delete one.

## Core Operating Model

This is a docs-first project.

- Read docs before code.
- Use docs to understand intent, then verify against code.
- Do not invent behavior when specs or trackers are silent.
- Keep the existing doc structure intact unless the user explicitly asks for structural changes.

Working order for most tasks:
1. `docs/plans/tracker.md`
2. Relevant spec or SOP in `docs/specs/`, `docs/cerb/`, `docs/sops/`
3. Relevant code
4. Supporting references such as `docs/reference/**` or `.agent/rules/**` when useful for project-specific nuance

## Source of Truth

Default principle: `Docs > Code > Guessing`.

When documents conflict, use this order unless the task establishes a narrower local SOT:
1. Domain contracts and real data models actually used by the current system
2. Architecture and module specs
3. Trackers and interface maps
4. Supporting references and historical notes

Archived or historical material is not authoritative unless the user explicitly asks for it.

## Delivery Hierarchy

For product and feature delivery in this repo, use this stricter development chain:

1. Core Flow: what must happen
2. Spec: how to implement it
3. Code: current delivered behavior
4. PU Test: validates delivered code against Core Flow
5. Fix loop: repair Spec, then code, until behavior matches Core Flow

Interpret this hierarchy strictly:

- Core Flow can be ahead of spec and code.
- If Core Flow conflicts with lower docs, assume the lower docs may be stale before assuming the Core Flow is wrong.
- If Spec conflicts with Code, treat that as implementation drift or an incomplete spec sync.
- If PU coverage does not represent a Core Flow branch, the validation surface is incomplete.

`docs/core-flow/**` is the behavioral north-star layer when it exists for a feature.

## Shared Project Rules

### Language

- Agent communication: English
- Documentation prose: English
- Code comments: Simplified Chinese
- File headers and inline annotations: Simplified Chinese when the codebase expects them
- Documentation, trackers, plans, changelogs, and reports must not use emoji
- Use plain text, checkboxes, and status words instead of decorative symbols or emoji in repo prose

### Docs-First Delivery

- No code without reading the relevant docs first.
- No behavior change without syncing the relevant docs in the same session.
- If the spec does not cover the behavior, stop and ask the user or update the spec first when that is clearly part of the task.
- If interfaces, states, ownership, or wave/status tracking change, update the corresponding docs.
- When a `docs/core-flow/**` document exists, read it before treating specs as the final behavioral source.
- Do not downgrade a Core Flow doc just because the codebase is behind it; instead, identify which lower layer must catch up.

### Lessons-Learned Discipline

- For greenfield, migration, cross-platform translation, platform-divergence, repeated-failure, or other known-risk work, read `.agent/rules/lessons-learned.md` before planning or editing.
- If any trigger matches, or if the index is too thin for the current risk, read `docs/reference/agent-lessons-details.md` before acting.
- Treat the lessons index as a preflight check for Codex too, not only for `.agent/workflows/**`.
- Add new lessons only after the user confirms the problem is fixed, and keep the index concise while storing the operating detail in `docs/reference/agent-lessons-details.md`.

### Evidence Over Assumption

- Verify claims with repo evidence before stating them.
- Search before assuming a class, flow, state, or interface exists.
- Read files before editing them.
- Prefer concrete verification over “looks right”.
- For Android runtime debugging, `adb logcat` is mandatory evidence, not an optional extra.
- Treat screenshots, user recollection, and transient UI text as supporting evidence only; they are not root-cause proof by themselves.
- If the bug involves UI behavior, lifecycle, background work, BLE/connectivity, notifications, alarms, networking, or device/emulator integration, start by capturing the relevant `adb logcat` output before claiming a diagnosis.
- If current logs are insufficient, add targeted tags/logging and rerun the repro instead of guessing.
- If adb is genuinely unavailable, say so explicitly and lower confidence rather than pretending the diagnosis is proven.

### Verification Standard

- Run relevant tests for the affected area whenever feasible.
- Prefer full affected-module verification over a single narrow happy-path test.
- Include negative-path thinking: empty input, invalid state, missing data, offline/failure paths where relevant.
- If you cannot run verification, say so explicitly.
- For compiler/build failures, compiler output is the primary evidence.
- For pure unit/instrumentation test failures, test output is the primary evidence.
- For Android runtime behavior, `adb logcat` remains mandatory even when screenshots or test failures also exist.

### Simplicity Standard

- Prefer clear, local implementations over clever abstractions.
- Do not introduce an interface for a single implementation unless the repo already requires that seam.
- Rewrite misaligned legacy patterns from current specs instead of copying them forward.

### Prism Boundary Discipline

- Keep `domain/` pure: no Android imports, no legacy contamination.
- Read legacy code only to understand behavior; implement against current contracts and architecture.
- Do not create undocumented cross-module ownership edges. Check `docs/cerb/interface-map.md` when a change spans modules.

### Human Reality

- Design and implementation must account for messy user behavior, not only ideal flows.
- When obvious human constraints appear, translate them into concrete engineering constraints.
- When those constraints require a structural tradeoff, stop and discuss instead of burying the decision in code.

## Codex-Specific Expectations

- Use `AGENTS.md` as the primary repo instruction file when working in Codex.
- Keep changes minimal and surgical when editing docs or code.
- Do not modify `.agent/rules/**` or `.agent/workflows/**` unless the user explicitly asks for Antigravity changes too.
- When Antigravity guidance contains project knowledge that matters to the task, adapt the intent into Codex behavior rather than rewriting Antigravity files by default.
- When the user asks for a commit message, suggest the message only. Light formatting or brief commentary is allowed, but never run `git commit`, never present the change as already committed, and assume the user will commit manually unless they explicitly ask for execution.

## Workflow Portability

The `.agent/workflows/**` layer is important project knowledge.

- Treat those files as reusable project playbooks, not as dead Antigravity residue.
- For Codex work, reuse the workflow logic when the task matches, even if the Antigravity slash-command wrapper does not apply directly.
- Do not assume every workflow should become a standalone Codex skill. Some belong in repo instructions, some remain reference SOPs, and some are runtime-specific.
- Workflows carry a `last_reviewed` frontmatter date. Workflows without a `last_reviewed` date within 6 months should be considered stale candidates for archival.

High-transfer workflows for Codex-style execution include:
- `feature-dev-planner-[tool].md`
- `04-doc-sync-[tool].md`
- `06-audit-[tool].md`
- `acceptance-team-[tool].md`
- `cerb-check-[tool].md`
- `interface-alignment-check-[tool].md`
- `on-device-test-[tool].md`
- `oem-permission-hardening-[tool].md`
- `18-pipeline-valve-check-[tool].md`

Adapt-with-care workflows:
- Persona files such as `01-senior-reviewr-[persona].md`, `08-ux-specialistr-[persona].md`, `12-ui-director-[persona].md`, `16-ui-designer-[persona].md`, and `20-sales-expert-[persona].md`
- These are useful as review lenses and output formats, but they should not be copied mechanically into Codex as fake personas unless the user explicitly wants that style.

Antigravity-native mechanics that usually stay in `.agent/workflows/**`:
- Slash-command routing
- Auto-trigger metadata tied to Antigravity behavior
- Antigravity-specific orchestration assumptions

If a workflow proves repeatedly useful in Codex, promote it deliberately into one of these forms:
1. A standing rule in `AGENTS.md`
2. A shared SOP in `docs/sops/**`
3. A dedicated Codex skill when the task is specialized, repeatable, and benefits from bundled resources

## Quick Reference

- Agent coalition contract: `docs/specs/agent-coalition.md`
- Main tracker: `docs/plans/tracker.md`
- UX SOT: `docs/specs/prism-ui-ux-contract.md`
- Interface ownership: `docs/cerb/interface-map.md`
- Feature SOP: `docs/sops/feature-development.md`
- Codex skill library: `.codex/skills/*/SKILL.md`
- Human reference: `docs/AGENTS.md`
- Antigravity runtime rules: `.agent/rules/*.md`
- Antigravity workflow library: `.agent/workflows/*.md`
