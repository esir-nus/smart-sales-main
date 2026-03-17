# Smart Sales OS & Documentation Index

> **Navigation hub for AI agents and developers**  
> **Architecture**: Prism (Unified Pipeline)  
> **Last Updated**: 2026-03-17

---

## 🏗️ Project Mono: The Data-Oriented OS
A self-contained multi-module Android project implementing the Dual-Engine Architecture (Sync/Query vs Async/Command).

To run the application:
```bash
./gradlew :app:assembleDebug
```

---

## 🗂️ Folder Structure

```
AGENTS.md            # 🤖 Codex-native repository instructions

.codex/
└── skills/          # 🧠 Codex-native project skills

docs/
├── AGENTS.md        # 📘 Human-readable shared guidance for both runtimes
├── core-flow/       # 🧭 Behavioral north-star flows (WHAT must happen)
├── specs/           # 🔒 Architecture + feature contracts
│   └── README.md    # 📍 Spec navigation SOP
├── cerb/            # 🔲 Cerb-compliant modules (self-contained specs)
│   └── memory-center/  # Memory Center spec + interface
├── cerb-plugin/     # 🔌 System III Plugins (Towns)
├── sops/            # 📋 Standard Operating Procedures
├── plans/           # 🔄 Living trackers (WHERE we are)
├── reports/         # 📊 Audit and Verification Reports
├── dev-notes/       # 📝 Human handovers (agent ignores)
└── archived/        # 💀 Dead docs

.agent/
├── rules/smart-sales.md   # 📍 Antigravity project context (nav + commands)
└── workflows/             # ⚡ Antigravity workflows; many are reusable playbooks for Codex
```

---

## 1️⃣ Documentation Hierarchy
*Read the repo from top to bottom, not sideways.*

1. **Architecture**: what kind of system this repo is
2. **Core Flow**: what a feature must do inside that architecture
3. **Feature Spec / Cerb Spec**: how to build it
4. **Code**: delivered implementation
5. **PU / Acceptance**: validation against core flow and architecture

---

## 2️⃣ Architectural Constitution (`specs/` & `cerb*/`)
*The stable laws of the OS and module system.*

| File | Domain | Purpose |
|------|--------|---------|
| [Architecture.md](docs/specs/Architecture.md) | Architecture | **Top Architectural SOT** — System constitution, OS model, boundaries, observability, lifecycle |
| [gateway-spec.md](docs/cerb-plugin/architecture/gateway-spec.md) | Extensibility | **SOT** — System III Plugin Gateway & Protocol |
| [prism-ui-ux-contract.md](docs/specs/prism-ui-ux-contract.md) | UX | **INDEX** — Modules, Flows, Components |
| [style-guide.md](docs/specs/style-guide.md) | UI | Visual design system, typography, components |
| [connectivity-spec.md](docs/cerb/connectivity-bridge/spec.md) | Connectivity | BLE/WiFi/HTTP contracts |
| [testing-protocol.md](docs/cerb-e2e-test/testing-protocol.md) | Testing | **SOT** — The 3-Level Standard & E2E Pillars |
| [GLOSSARY.md](docs/specs/GLOSSARY.md) | Terminology | Terms, no-synonyms rule |

## 2.5️⃣ Core Flows (`core-flow/`)
*Behavioral north stars that may run ahead of spec and code.*

Core Flow docs derive from `Architecture.md`.

Use this chain when a core-flow doc exists:
1. Architecture: what kind of system this is
2. Core Flow: what must happen
3. Spec: how to implement it
4. Code: delivered result
5. PU Test: validates code against Core Flow
6. Fix loop: repair lower layers until they match

---

## 3️⃣ Living Trackers (`plans/` & `reports/`)
*The current state of the world. Updated daily.*

| File | Purpose |
|------|---------|
| **[tracker.md](docs/plans/tracker.md)** | **Main tracker** — architecture, modules, milestones (Always start here) |
| [interface-map.md](docs/cerb/interface-map.md) | **Interface boundaries** — what module owns what |
| [pipeline-valves.md](docs/plans/telemetry/pipeline-valves.md) | **Telemetry Protocol** — OS GPS Checkpoints (Must be updated on new edges) |
| [changelog.md](docs/plans/changelog.md) | History of completed epics |

---

## 4️⃣ Quick Navigation for Agents & Devs

### I'm implementing a new feature
1. Read `Architecture.md` to understand the rules.
2. Read the owning `docs/core-flow/**` doc if one exists.
3. Read [docs/sops/feature-development.md](docs/sops/feature-development.md) — **START HERE**.
4. Use `/feature-dev-planner` workflow to begin.

### I'm building a System III Plugin
1. Read the Extensibility manifesto: [gateway-spec.md](docs/cerb-plugin/architecture/gateway-spec.md).
2. Look at the reference implementation: `docs/cerb-plugin/echo-test/spec.md`.
3. Use `/cerb-plugin-template` to scaffold your new town.

### I'm debugging missing data
1. Open Logcat and filter by `VALVE_PROTOCOL`.
2. Check [pipeline-valves.md](docs/plans/telemetry/pipeline-valves.md) to see which GPS checkpoint the payload vanished at.

### I'm designing UI/UX
1. Read [prism-ui-ux-contract.md](docs/specs/prism-ui-ux-contract.md) for INDEX.
2. Apply [style-guide.md](docs/specs/style-guide.md) for visuals.

---

## 5️⃣ Core Protocols (`.agent/rules/`)
*The behavioral, architectural, and quality rules agents must follow.*

These remain the Antigravity-native rule set. Codex uses root `AGENTS.md`, while both runtimes share the same `docs/` source-of-truth system.

| Protocol | Purpose |
|----------|---------|
| [anti-drift-protocol.md](.agent/rules/anti-drift-protocol.md) | Spec-First development, preventing skeleton code & assumed features |
| [anti-laziness.md](.agent/rules/anti-laziness.md) | Concrete, comprehensive, negative verification requirements |
| [docs-first-protocol.md](.agent/rules/docs-first-protocol.md) | The 3 Gates: No code without docs. Sync specs on every change |
| [context-boundaries.md](.agent/rules/context-boundaries.md) | Anthropic context separation rules (Same Context vs Blackbox) |
| [prism-clean-env.md](.agent/rules/prism-clean-env.md) | Golden Rule: `domain/` must never import Android or Legacy code |
| [human-intricacies-protocol.md](.agent/rules/human-intricacies-protocol.md) | Handling organic UX chaos instead of building "perfect" pipelines |
| [cross-module-check.md](.agent/rules/cross-module-check.md) | Interface Map checks before crossing domain boundaries |
| [lessons-learned.md](.agent/rules/lessons-learned.md) | Index of previous agent hallucination traps and architectural fixes |

## 6️⃣ Workflow Library (`.agent/workflows/`)
*Antigravity-native execution patterns, many of which are portable in spirit to Codex.*

Practical transfer candidates include:
- `feature-dev-planner-[tool].md`
- `04-doc-sync-[tool].md`
- `06-audit-[tool].md`
- `acceptance-team-[tool].md`
- `cerb-check-[tool].md`
- `interface-alignment-check-[tool].md`

Use these as shared project playbooks. Keep the files intact for Antigravity, but allow Codex to reuse the logic and checklists when the task matches.

---

## Source of Truth Hierarchy
When documents conflict, follow this precedence:
1. **Architecture.md**: system constitution
2. **docs/core-flow/**: feature behavioral north star
3. **Feature specs / Cerb specs**: implementation contract
4. **Domain data classes and code**: delivered shape
5. **tracker.md / interface-map.md**: current state and ownership
