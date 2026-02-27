# Prism Spec Glossary

> **Authority**: This glossary defines the official terminology for all Prism specifications.  
> All spec files MUST use these terms consistently. No synonyms.

---

## Hierarchy Overview

```
App
├── Modes (Coach, Analyst, Scheduler)
│   └── Each Mode activates specific Module(s)
│
├── Modules (AudioDrawer, ThinkingBox, TaskBoard)
│   ├── State Machine (Idle → Loading → Loaded)
│   └── Components (GlassCard, Button)
│
├── Components (Pure UI, stateless)
│
└── Flows (OnboardingFlow, ConnectivityFlow)
    └── Phases (Phase 1 → Phase 2 → Phase 3)
```

---

## Core Terms

| Term | Definition | Example | NOT This |
|------|------------|---------|----------|
| **Mode** | Top-level user context that changes entire app behavior. Mutually exclusive. Only one Mode active at a time. | `Coach`, `Analyst`, `Scheduler` | Not a screen, not a feature |
| **Module** | Self-contained feature with its own state, UI, and business logic. Has a dedicated spec file. Owned by one Mode or shared. | `AudioDrawer`, `ThinkingBox`, `TaskBoard`, `ConnectivityModal` | Not a Flow (modules don't have steps) |
| **Component** | Reusable UI element with **no business logic**. Pure presentation. Stateless or locally stateful only. | `GlassCard`, `PrismButton`, `TaskBoardItem`, `ModeToggle` | Not a Module (no domain state) |
| **Flow** | Multi-step user journey that may cross multiple screens/modules. Described as an ordered sequence of Phases. | `OnboardingFlow`, `ConnectivityFlow`, `AnalystFlow` | Not a Module (flows are temporal, modules are spatial) |
| **Phase** | A sequential step within a Flow. Phases are **ordered** and **numbered**. | `Phase 1 (Consultant)`, `Phase 2 (Architect)` | Not a State (phases imply sequence) |
| **State** | A discrete condition of a Module at a point in time. Finite, enumerable. States are **not ordered**. | `Idle`, `Loading`, `Conversing`, `Structured`, `Error` | Not a Phase (states can transition in any order) |

---

## Contract Terms

| Term | Definition | Example |
|------|------------|---------|
| **Contract** | Explicit agreement between layers (UI ↔ Domain ↔ Data). Defines inputs, outputs, and guarantees. | `info_sufficient: Boolean` triggers Phase 2 |
| **Spec** | A documentation file that completely defines a Module, Component, or Flow. Self-contained. | `AnalystMode.md`, `ThinkingBox.md` |
| **Index** | A navigation file that links to Specs and tracks their status. Does NOT contain implementation details. | `prism-ui-ux-contract.md` (as INDEX) |

---

## Architecture Terms

| Term | Definition | Context |
|------|------------|---------|
| **Layer** | A horizontal slice of the architecture (UI, Domain, Data, Platform). | Lattice architecture |
| **Coordinator** | A class that orchestrates a Flow by calling multiple use cases. | `AnalystFlowControllerV2` |
| **Task** | A scheduled unit of work. | `ScheduledTaskEntity` |
| **Parser** | A class that transforms raw output into structured data. | `InvestigationLinter` |

---

## OS Model Terms

> **Reference**: [`os-model-architecture.md`](./os-model-architecture.md)

| Term | Definition | OS Analogy |
|------|------------|------------|
| **RAM** | Session Working Set — per-session workspace with 3 sections. All modules operate here during a chat session. | Computer RAM |
| **SSD** | Room DB (all repositories) — permanent storage, source of truth. | Hard drive |
| **Kernel** | ContextBuilder — loads data from SSD into RAM, manages what's active. Only component that owns the RAM lifecycle. | OS Kernel |
| **Application** | A module that reads/writes through RAM, never accesses SSD directly. Uses write-through persistence. | User-space app |
| **File Explorer** | CRM Hub — reads SSD directly for dashboard/history views. Not session-scoped. | File manager |
| **SessionWorkingSet** | The RAM data structure with 3 sections: Distilled Memory, User Habits, Client Habits. Replaces `SessionContext`. | — |
| **Write-Through** | Every RAM write simultaneously persists to SSD. No flush, no sync, no crash risk. | Cache write-through |
| **Distilled Memory** | RAM Section 1 — resolved entity pointers + active memory references. Drives auto-population of Section 3. | — |

---

## UI Terms

| Term | Definition | Example |
|------|------------|---------|
| **Drawer** | A slide-in panel from screen edge. Contains a Module. | `AudioDrawer`, `HistoryDrawer`, `SchedulerDrawer` |
| **Modal** | A centered overlay that blocks interaction until dismissed. | `ConnectivityModal` |
| **Sheet** | A bottom sheet that partially covers the screen. | `UserCenterSheet` |
| **Card** | A contained UI surface with elevation/glass effect. | `GlassCard`, `AudioCard` |
| **Banner** | A horizontal strip showing status or activity. | `AgentActivityBanner` |

---

## Analyst Mode Specific

| Term | Definition |
|------|------------|
| **Thinking Trace** | The visible CoT reasoning from the LLM (`reasoning_content`). Displayed in ThinkingBox. |
| **Markdown Strategy** | A standard markdown text block displaying the analysis strategy. Read-only display. |
| **TaskBoard** | Sticky top layer in Analyst mode providing actionable workflow shortcuts. |
| **ThinkingBox** | UI component displaying the agent's raw reasoning stream (Thought Trace). |
| **Phase 1 (Consultant)** | Conversational planning phase to clarify intent. |
| **Phase 2 (Architect)** | Markdown strategy generation. Produces Markdown Strategy bubble. |
| **info_sufficient** | Boolean flag from Phase 1. When `true`, triggers Phase 2. |

---

## Usage Rules

1. **Every spec file** must reference this glossary:
   ```markdown
   ## Definitions
   This spec uses terms from [GLOSSARY.md](./GLOSSARY.md).
   ```

2. **No synonyms allowed**. Use the exact term from this glossary.
   - ❌ "Analyst screen" → ✅ "Analyst Mode"
   - ❌ "Task list" → ✅ "TaskBoard"
   - ❌ "Thinking bubble" → ✅ "ThinkingBox"

3. **When adding new terms**, update this glossary FIRST, then use in specs.

4. **ASCII Blueprints are SOT (Source of Truth)**:
   > **Critical**: ASCII diagrams define **atomic-level UI rules**. They are NOT inspiration — they are CONTRACT.

   - Every icon placement is intentional (hamburger = hamburger, not menu)
   - Every position is intentional (title left of star = must be left of star)
   - Every element inclusion is intentional (if it's in ASCII, it's required)

   **When checking specs, agents MUST verify BOTH:**
   - Descriptive content (text explanations)
   - Visual content (ASCII blueprints)

   **Together they form the complete specification.** Neither is optional.

---

## Version History

| Date | Change |
|------|--------|
| 2026-02-01 | Initial creation. Established core terminology. |
| 2026-02-10 | Added OS Model Terms (RAM, SSD, Kernel, Application, File Explorer, SessionWorkingSet, Write-Through, Distilled Memory). |
