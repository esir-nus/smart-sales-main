# Smart Sales Documentation Index

> **Navigation hub for AI agents and developers**  
> **Architecture**: Prism (Unified Pipeline)  
> **Last Updated**: 2026-02-01

---

## Folder Structure

```
docs/
├── AGENTS.md        # 🤖 AI collaboration rules (generic behavior)
├── specs/           # 🔒 Locked contracts (WHAT to build)
│   └── README.md    # 📍 Spec navigation SOP
├── cerb/            # 🔲 Cerb-compliant modules (self-contained specs)
│   └── memory-center/  # Memory Center spec + interface
├── sops/            # 📋 Standard Operating Procedures
├── plans/           # 🔄 Living trackers (WHERE we are)
├── dev-notes/       # 📝 Human handovers (agent ignores)
└── archived/        # 💀 Dead docs

.agent/
├── rules/smart-sales.md   # 📍 Project context (nav + commands)
└── workflows/             # ⚡ Slash commands
```

---

## Three-Layer Doc System

| Layer | Purpose | Mutation |
|-------|---------|----------|
| **specs/** | Defines behavior, contracts, interfaces | Rarely changed, versioned |
| **cerb/** | Self-contained module specs (Cerb-compliant) | Per-feature, blackbox contract |
| **plans/** | Tracks implementation status, deviation log | Updated every session |
| **sops/** | Standard procedures for common tasks | Evolves with process |

---

## cerb/ (🔲 Self-Contained Modules)

> **Cerb = Context Boundary** — Each module is blackbox. Read interface, ignore implementation.

| Module | Spec | Interface | Status |
|--------|------|-----------|--------|
| **Memory Center** | [spec.md](./cerb/memory-center/spec.md) | [interface.md](./cerb/memory-center/interface.md) | Wave 1 ✅ |

---

## specs/ (🔒 Locked Contracts)

| File | Domain | Purpose |
|------|--------|---------|
| [README.md](./specs/README.md) | Navigation | **Spec Navigation SOP** — reading order |
| [Prism-V1.md](./specs/Prism-V1.md) | Architecture | **SOT** — Unified Pipeline, Memory, Modes |
| [prism-ui-ux-contract.md](./specs/prism-ui-ux-contract.md) | UX | **INDEX** — Modules, Flows, Components |
| [gateway-spec.md](./cerb-plugin/architecture/gateway-spec.md) | Architecture | **SOT** — System III Plugin Gateway & Protocol |
| [GLOSSARY.md](./specs/GLOSSARY.md) | Terminology | Terms, no-synonyms rule |
| [style-guide.md](./specs/style-guide.md) | UI | Visual design system, typography, components |
| [connectivity-spec.md](./cerb/connectivity-bridge/spec.md) | Connectivity | BLE/WiFi/HTTP contracts |
| [esp32-protocol.md](./specs/esp32-protocol.md) | Connectivity | Hardware protocol |

---

## plans/ (🔄 Living Trackers)

| File | Purpose |
|------|---------|
| [tracker.md](./plans/tracker.md) | **Main tracker** — architecture, modules, milestones |
| [prism-ui-ux-contract.md](./specs/prism-ui-ux-contract.md) | UX state inventories, microcopy, component status |

---

## sops/ (📋 Standard Procedures)

| File | Purpose |
|------|---------|
| [feature-development.md](./sops/feature-development.md) | Feature dev SOP — spec reading order, Trinity checkpoints |

---

## Source of Truth Hierarchy

When documents conflict, follow this precedence:

1. **Prism-V1.md** — Current architecture spec (Unified Pipeline)
2. **prism-ui-ux-contract.md** — UI behavior boundaries (INDEX)
3. **GLOSSARY.md** — Terminology and rules
4. **Module/Flow/Component specs** — Feature-specific details
5. Existing implementation and tests

---

## Quick Navigation

### I'm implementing a feature
1. Read [docs/sops/feature-development.md](./sops/feature-development.md) — **START HERE**
2. Follow the 5-phase SOP
3. Use `/10-feature-build-mode` workflow

### I'm working on connectivity
1. Read [connectivity-spec.md](./cerb/connectivity-bridge/spec.md) for contracts
2. Check [esp32-protocol.md](./specs/esp32-protocol.md) for hardware

### I'm designing UI/UX
1. Read [prism-ui-ux-contract.md](./specs/prism-ui-ux-contract.md) for INDEX
2. Navigate to specific module/flow/component spec
3. Apply [style-guide.md](./specs/style-guide.md) for visuals

### I'm refactoring code
1. Follow [tracker.md](./plans/tracker.md) blueprint
2. Follow [AGENTS.md](./AGENTS.md) purification rules
