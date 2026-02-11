# Spec Federation — Navigation SOP

> **For Agents**: Read in order. Stop when you have enough context.
> 
> **For Humans**: This folder uses a federated structure. Large specifications are split into focused modules.

---

## 🔍 Quick Reference (Always Load First)

| Order | File | Purpose |
|-------|------|---------|
| 1 | [GLOSSARY.md](./GLOSSARY.md) | Terms, no-synonyms rule (Rule #1), ASCII SOT (Rule #4) |
| 2 | [prism-ui-ux-contract.md](./prism-ui-ux-contract.md) | UI INDEX — modules, flows, components routing table |

---

## 🏗️ Architecture Deep Dive (If Needed)

| Order | File | Purpose |
|-------|------|---------|
| 3 | [Prism-V1.md](./Prism-V1.md) | Core pipeline, memory, modes, visibility system |

**⚠️ Do NOT read entire Prism-V1.md upfront.** Use TOC to jump to relevant sections.

---

## 📦 Feature-Specific Routing

| Task | Read These |
|------|-----------|
| **Home Screen** | `modules/HomeScreen.md` |
| **Audio Drawer** | `modules/AudioDrawer.md` |
| **User Center** | `modules/UserCenter.md` |
| **Connectivity Modal** | `modules/ConnectivityModal.md` |
| **Analyst Mode** | `Prism-V1.md §4.6` → `flows/AnalystFlows.md` |
| **Coach Mode** | `flows/CoachFlows.md` |
| **Scheduler** | `flows/SchedulerFlows.md` |
| **Agent Visibility** | `components/AgentActivityBanner.md` |
| **Memory/RAG** | `Prism-V1.md §5` |

---

## 📁 Folder Structure

```
docs/specs/
├── README.md                 ← YOU ARE HERE (Navigation SOP)
├── GLOSSARY.md               ← Terms & Rules
├── Prism-V1.md               ← Architecture SOT
├── prism-ui-ux-contract.md   ← UI INDEX
├── modules/                  ← UI module specs (<100 lines each)
│   ├── HomeScreen.md
│   ├── AudioDrawer.md
│   └── ...
├── flows/                    ← User flow specs
│   ├── SchedulerFlows.md
│   ├── CoachFlows.md
│   └── ...
└── components/               ← Component specs
    ├── AgentActivityBanner.md
    └── ...
```

---

## 🚫 Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|------|
| Read entire Prism-V1.md first | Read GLOSSARY → INDEX → then jump to section |
| Guess file locations | Use this README to route |
| Read specs top-to-bottom | Read by feature need |

---

## 🔗 Related

- **Workflows**: See `.agent/workflows/` for `/cerb-check`, `/06-audit`
- **Design Tokens**: See `docs/design/design-tokens.json`
- **Tracker**: See `docs/plans/tracker.md` for implementation status
