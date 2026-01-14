# Smart Sales Documentation Index

> **Navigation hub for AI agents and developers**  
> **Architecture**: Lattice (Box-API pattern)  
> **Last Updated**: 2026-01-14

---

## Folder Structure

```
docs/
├── AGENTS.md      # 🤖 AI collaboration rules (generic behavior)
├── specs/         # 🔒 Locked contracts (WHAT to build)
├── plans/         # 🔄 Living trackers (WHERE we are)
├── dev-notes/     # 📝 Human handovers (agent ignores)
└── archived/      # 💀 Dead docs

.agent/
├── rules/smart-sales.md   # 📍 Project context (nav + commands)
└── workflows/             # ⚡ Slash commands
```

---

## Three-Layer Doc System

| Layer | Purpose | Mutation |
|-------|---------|----------|
| **specs/** | Defines behavior, contracts, interfaces | Rarely changed, versioned |
| **plans/** | Tracks implementation status, deviation log | Updated every session |
| **AGENTS.md** | How we work (AI behavior rules) | Evolves with process |

---

## specs/ (🔒 Locked Contracts)

| File | Domain | Purpose |
|------|--------|---------|
| [Orchestrator-Lattice.md](./specs/Orchestrator-Lattice.md) | Architecture | **CURRENT** — Lattice Box-API architecture |
| [Orchestrator-V1.md](./specs/Orchestrator-V1.md) | Algorithms | DEPRECATED — V1 appendices only (Disector, Publisher) |
| [ux-contract.md](./specs/ux-contract.md) | UX | Data contracts, feature boundaries |
| [style-guide.md](./specs/style-guide.md) | UI | Visual design system, typography, components |
| [connectivity-spec.md](./specs/connectivity-spec.md) | Connectivity | BLE/WiFi/HTTP contracts |
| [esp32-protocol.md](./specs/esp32-protocol.md) | Connectivity | Hardware protocol |
| [lattice-interfaces.md](./specs/lattice-interfaces.md) | All | Lattice Service interface catalog |

---

## plans/ (🔄 Living Trackers)

| File | Purpose |
|------|---------|
| [tracker.md](./plans/tracker.md) | **Main tracker** — architecture, modules, milestones |
| [ux-experience.md](./plans/ux-experience.md) | UX state inventories, microcopy, component status |

---

## Source of Truth Hierarchy

When documents conflict, follow this precedence:

1. **Orchestrator-Lattice.md** — Current architecture spec (Box-API pattern)
2. **Orchestrator-V1.md Appendices** — Algorithm specs (Disector, Publisher, State Recovery)
3. **orchestrator-v1.schema.json** — JSON schema for data models
4. **ux-contract.md** — UI behavior boundaries
5. **style-guide.md** — Visual design
6. Existing implementation and tests

---

## Quick Navigation

### I'm implementing a feature
1. Read [tracker.md](./plans/tracker.md) for current status
2. Read [Orchestrator-Lattice.md](./specs/Orchestrator-Lattice.md) for architecture and module boundaries
3. Follow [AGENTS.md](./AGENTS.md) for coding rules

### I'm working on connectivity
1. Read [connectivity-spec.md](./specs/connectivity-spec.md) for contracts
2. Check [esp32-protocol.md](./specs/esp32-protocol.md) for hardware

### I'm designing UI/UX
1. Check [ux-contract.md](./specs/ux-contract.md) for constraints
2. Check [ux-experience.md](./plans/ux-experience.md) for state inventories
3. Apply [style-guide.md](./specs/style-guide.md) for visuals

### I'm refactoring code
1. Follow [tracker.md](./plans/tracker.md) blueprint
2. Follow [AGENTS.md](./AGENTS.md) purification rules
