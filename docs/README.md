# Smart Sales Documentation Index

**Active documentation for AI coding agents and developers.**

---

## Folder Structure

```
docs/
├── specs/       # 🔒 Locked contracts & schemas
├── guides/      # 📝 Living how-to docs
├── plans/       # 🔄 WIP tracking
├── logs/        # 📜 Historical records
├── archived/    # 💀 Dead docs
├── dev-notes/   # Non-normative
└── modules/     # Module-specific
```

---

## Source of Truth Hierarchy

When documents conflict, follow this precedence order:

1. **[Orchestrator-V1.md](./specs/Orchestrator-V1.md)** — V1 architecture spec (modules, contracts, invariants)
2. **[orchestrator-v1.schema.json](./specs/orchestrator-v1.schema.json)** — JSON schema for V1 data models
3. **[ux-contract.md](./specs/ux-contract.md)** — UI behavior, layout, flows, and interaction patterns
4. **[style-guide.md](./guides/style-guide.md)** — Visual design language (colors, typography, components)
5. Existing Android implementation and tests

---

## Specs (🔒 Locked Contracts)

| File | Purpose |
|------|---------|
| [Orchestrator-V1.md](./specs/Orchestrator-V1.md) | **CURRENT** V1 architecture spec: modules, pipelines, failure semantics, retry policies |
| [orchestrator-v1.schema.json](./specs/orchestrator-v1.schema.json) | JSON schema for all V1 artifacts (DisectorPlan, PublishedChatTurn, M1-M4, etc) |
| [orchestrator-v1.examples.json](./specs/orchestrator-v1.examples.json) | Example payloads for V1 artifacts with edge cases |
| [ux-contract.md](./specs/ux-contract.md) | **CANONICAL** (🔒 locked): data contracts, pipelines, feature boundaries |
| [api-contracts.md](./specs/api-contracts.md) | UI ↔ Orchestrator facade contracts (event streams, HUD, error semantics) |
| [source-repo.json](./specs/source-repo.json) | Provider ledger (OSS, Tingwu, XFyun, DashScope): endpoints, auth, guardrails |
| [source-repo.schema.json](./specs/source-repo.schema.json) | JSON schema for source-repo.json |

---

## Guides (📝 Living Docs)

| File | Purpose |
|------|---------|
| [AGENTS.md](./guides/AGENTS.md) | Agent collaboration rules, doc precedence, build commands, coding style |
| [style-guide.md](./guides/style-guide.md) | Visual design system: colors, typography, spacing, component patterns |
| [ux-experience.md](./guides/ux-experience.md) | **EXPERIENCE** (📝 UX-owned): state inventories, microcopy, timing, layout |
| [parallel-building.md](./guides/parallel-building.md) | **Contract** for parallel frontend/backend development |

---

## Plans (🔄 WIP Tracking)

| File | Purpose |
|------|---------|
| [RealizeTheArchi.md](./plans/RealizeTheArchi.md) | **Blueprint** for architecture realization: Portable Core, KMP prep, milestone tracking |
| `contract-delta-*.md` | Contract change proposals |

---

## Logs (📜 Historical Records)

| File | Purpose |
|------|---------|
| [CHANGELOG.md](./logs/CHANGELOG.md) | Spec versioning history (V1.x documentation updates) |
| [REFACTORING-LOG.md](./logs/REFACTORING-LOG.md) | Architecture refactoring waves (Wave 1-19 code changes) |

---

## Other Directories

| Directory | Purpose |
|-----------|---------|
| [archived/](./archived/) | Historical specs (do NOT use as implementation targets) |
| [dev-notes/](./dev-notes/) | Audit logs, debug investigations (non-normative) |
| [modules/](./modules/) | Module-specific documentation |

---

## Quick Navigation

### I'm implementing a feature
1. Read [Orchestrator-V1.md](./specs/Orchestrator-V1.md) for module boundaries
2. Check [api-contracts.md](./specs/api-contracts.md) for facade contracts
3. Verify [ux-contract.md](./specs/ux-contract.md) for UI behavior
4. Follow [RealizeTheArchi.md](./plans/RealizeTheArchi.md) patterns

### I'm integrating a provider
1. Read [source-repo.json](./specs/source-repo.json) for endpoint registry

### I'm designing UI
1. Check [ux-contract.md](./specs/ux-contract.md) for canonical constraints (WHAT to present)
2. Check [ux-experience.md](./guides/ux-experience.md) for state inventories and microcopy (HOW to present)
3. Apply [style-guide.md](./guides/style-guide.md) for visuals

### I'm refactoring code
1. Follow [RealizeTheArchi.md](./plans/RealizeTheArchi.md) blueprint
2. Update [REFACTORING-LOG.md](./logs/REFACTORING-LOG.md) with new waves
3. Respect source of truth hierarchy from [AGENTS.md](./guides/AGENTS.md)
