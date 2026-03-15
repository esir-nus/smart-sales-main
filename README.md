# Smart Sales OS & Documentation Index

> **Navigation hub for AI agents and developers**  
> **Architecture**: Prism (Unified Pipeline)  
> **Last Updated**: 2026-03-15

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
docs/
├── AGENTS.md        # 🤖 AI collaboration rules (generic behavior)
├── specs/           # 🔒 Locked contracts (WHAT to build)
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
├── rules/smart-sales.md   # 📍 Project context (nav + commands)
└── workflows/             # ⚡ Slash commands
```

---

## 1️⃣ Locked Contracts (`specs/` & `cerb*/`)
*The unchangeable laws of the OS.*

| File | Domain | Purpose |
|------|--------|---------|
| [README.md](docs/specs/README.md) | Navigation | **Spec Navigation SOP** — reading order |
| [Architecture.md](docs/specs/Architecture.md) | Architecture | **SOT** — The Data-Oriented OS Migration Guide |
| [gateway-spec.md](docs/cerb-plugin/architecture/gateway-spec.md) | Extensibility | **SOT** — System III Plugin Gateway & Protocol |
| [prism-ui-ux-contract.md](docs/specs/prism-ui-ux-contract.md) | UX | **INDEX** — Modules, Flows, Components |
| [style-guide.md](docs/specs/style-guide.md) | UI | Visual design system, typography, components |
| [connectivity-spec.md](docs/cerb/connectivity-bridge/spec.md) | Connectivity | BLE/WiFi/HTTP contracts |
| [GLOSSARY.md](docs/specs/GLOSSARY.md) | Terminology | Terms, no-synonyms rule |

---

## 2️⃣ Living Trackers (`plans/` & `reports/`)
*The current state of the world. Updated daily.*

| File | Purpose |
|------|---------|
| **[tracker.md](docs/plans/tracker.md)** | **Main tracker** — architecture, modules, milestones (Always start here) |
| [interface-map.md](docs/cerb/interface-map.md) | **Interface boundaries** — what module owns what |
| [pipeline-valves.md](docs/plans/telemetry/pipeline-valves.md) | **Telemetry** — OS GPS Checkpoints |
| [changelog.md](docs/plans/changelog.md) | History of completed epics |

---

## 3️⃣ Quick Navigation for Agents & Devs

### I'm implementing a new feature
1. Read `Architecture.md` to understand the rules.
2. Read [docs/sops/feature-development.md](docs/sops/feature-development.md) — **START HERE**.
3. Use `/feature-dev-planner` workflow to begin.

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

## Source of Truth Hierarchy
When documents conflict, follow this precedence:
1. **The Kotlin `data class`** in `:domain` (Ultimate SSD Contract).
2. **Architecture.md** (Architectural Laws).
3. **tracker.md / interface-map.md** (Current State).
4. Module/Spec definitions.
