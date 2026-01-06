# Smart Sales Documentation Index

**Active documentation for AI coding agents and developers.**

---

## Source of Truth Hierarchy

When documents conflict, follow this precedence order:

1. **[Orchestrator-V1.md](./Orchestrator-V1.md)** — V1 architecture spec (modules, contracts, invariants)
2. **[orchestrator-v1.schema.json](./orchestrator-v1.schema.json)** — JSON schema for V1 data models
3. **[ux-contract.md](./ux-contract.md)** — UI behavior, layout, flows, and interaction patterns
4. **[style-guide.md](./style-guide.md)** — Visual design language (colors, typography, components)
5. Existing Android implementation and tests

---

## Active Documents

### Architecture & Specifications

| File | Purpose |
|------|---------|
| [Orchestrator-V1.md](./Orchestrator-V1.md) | **CURRENT** V1 architecture spec: modules, pipelines, failure semantics, retry policies |
| [orchestrator-v1.schema.json](./orchestrator-v1.schema.json) | JSON schema for all V1 artifacts (DisectorPlan, PublishedChatTurn, M1-M4, etc) |
| [orchestrator-v1.examples.json](./orchestrator-v1.examples.json) | Example payloads for V1 artifacts with edge cases |
| [api-contracts.md](./api-contracts.md) | UI ↔ Orchestrator facade contracts (event streams, HUD, error semantics) |

### UX & Design

| File | Purpose |
|------|---------|
| [ux-contract.md](./ux-contract.md) | **BEHAVIORAL AUTHORITY** for UI: message rendering, chat modes, HUD, entry points |
| [style-guide.md](./style-guide.md) | Visual design system: colors, typography, spacing, component patterns |

### Project Governance

| File | Purpose |
|------|---------|
| [AGENTS.md](./AGENTS.md) | Agent collaboration rules, doc precedence, build commands, coding style |
| [ArchitectureRefactoring.md](./ArchitectureRefactoring.md) | **North Star** for architecture evolution: Phase 3 roadmap, portable reducers, cross-platform prep |
| [CHANGELOG.md](./CHANGELOG.md) | Spec versioning history (V1.x documentation updates) |
| [REFACTORING-LOG.md](./REFACTORING-LOG.md) | Architecture refactoring waves (Wave 1-19 code changes) |

### Third-Party Integration

| File | Purpose |
|------|---------|
| [source-repo.json](./source-repo.json) | Provider ledger (OSS, Tingwu, XFyun, DashScope): endpoints, auth, guardrails, failure modes |
| [source-repo.schema.json](./source-repo.schema.json) | JSON schema for source-repo.json |
| [xfyun-asr-rest-api.md](./xfyun-asr-rest-api.md) | **Authoritative XFyun REST details** (params, signature, error codes) |

### Development Notes (Non-Normative)

Reference materials and historical investigations. **Not authoritative for implementation.**

| File | Purpose |
|------|---------|
| [dev-notes/](./dev-notes/) | Audit logs, version comparisons, debug investigations |

### Workflows

Workflows are defined in `.agent/workflows/` directory. See [archived/BudgetRule.md](./archived/BudgetRule.md) for legacy budget governance (deprecated).

---

## Archived Documents

Historical specs and legacy designs are in [archived/](./archived/). **Do NOT use archived docs as implementation targets.**

Key archived specs:
- `archived/Orchestrator-MetadataHub-V7.md` — Superseded by Orchestrator-V1.md
- `archived/role-contract.md` — Legacy multi-agent workflow (not current)

---

## Quick Navigation

### I'm implementing a feature
1. Read [Orchestrator-V1.md](./Orchestrator-V1.md) for module boundaries
2. Check [api-contracts.md](./api-contracts.md) for facade contracts
3. Verify [ux-contract.md](./ux-contract.md) for UI behavior
4. Follow [ArchitectureRefactoring.md](./ArchitectureRefactoring.md) patterns (Reducers, Coordinators)

### I'm integrating a provider
1. Read [source-repo.json](./source-repo.json) for endpoint registry
2. For XFyun: use [xfyun-asr-rest-api.md](./xfyun-asr-rest-api.md) as authority

### I'm designing UI
1. Check [ux-contract.md](./ux-contract.md) for behavior (what happens when)
2. Apply [style-guide.md](./style-guide.md) for visuals (colors, spacing, components)

### I'm refactoring code
1. Follow [ArchitectureRefactoring.md](./ArchitectureRefactoring.md) Phase 3 patterns
2. Update [REFACTORING-LOG.md](./REFACTORING-LOG.md) with new waves
3. Respect source of truth hierarchy from [AGENTS.md](./AGENTS.md)
