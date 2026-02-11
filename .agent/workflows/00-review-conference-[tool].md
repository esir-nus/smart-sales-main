---
description: Plan and orchestrate a multi-persona review conference, then run /01-senior-reviewr with the assembled panel
---

# Review Conference Planner

A **preparatory tool** that plans multi-persona review meetings before invoking `/01-senior-reviewr`. Use this when you need comprehensive cross-functional review, not for quick sanity checks.

> **Flow**: `/00-review-conference` → Plans meeting → Invokes `/01-senior-reviewr` with panel

---

## When to Use

| Scenario | Use This? |
|----------|-----------|
| Quick code review | ❌ Just use `/01-senior-reviewr` directly |
| Architecture decision with UX impact | ✅ Yes |
| Feature design touching multiple domains | ✅ Yes |
| Implementation plan review | ✅ Yes |
| "Is this over-engineered?" | ❌ Just use `/01-senior-reviewr` directly |

---

## Phase 1: Triage (Agent Does This)

Before convening anyone, assess:

```markdown
### Review Subject
[What is being reviewed? Code, architecture, plan, feature?]

### Review Type Classification
- [ ] Code Quality (logic, patterns, bugs)
- [ ] Architecture (structure, contracts, layers)
- [ ] UI/UX (flows, interactions, visuals)
- [ ] Business/Product (value, positioning, messaging)
- [ ] Cross-cutting (touches multiple areas)

### Complexity Level
- [ ] Simple (1-2 personas sufficient)
- [ ] Medium (3 personas)
- [ ] Complex (4+ personas, formal conference)
```

---

## Phase 2: Panel Selection

Based on triage, select the **minimum effective panel**:

### Selection Matrix

| Review Type | Required | Optional |
|-------------|----------|----------|
| **Code Quality** | `/01-senior-reviewr` | `/06-audit` |
| **Architecture** | `/01-senior-reviewr`, `/17-lattice-review` | `/cerb-check` |
| **UI/UX** | `/01-senior-reviewr`, `/08-ux-specialist` | `/12-ui-director`, `/16-ui-designer` |
| **Business/Product** | `/01-senior-reviewr`, `/20-sales-expert` | — |
| **Cross-cutting** | `/01-senior-reviewr` + 2-3 from above | As needed |

### Panel Size Rules

- **Maximum panel**: 4 personas (avoid committee bloat)
- **Senior is always chair**: Final synthesis authority
- **One voice per domain**: No duplicate perspectives

---

## Phase 3: Agenda Setting

Before the conference, define:

```markdown
### Conference Agenda

**Subject**: [What's being reviewed]
**Time Budget**: [Quick: 1 response | Standard: 2-3 | Deep: Full session]

**Panel**:
1. `/01-senior-reviewr` (Chair) — Final synthesis
2. [Persona 2] — [Specific focus area]
3. [Persona 3] — [Specific focus area]

**Questions for Each Panelist**:
- [Persona 2]: [Specific question within their domain]
- [Persona 3]: [Specific question within their domain]

**Expected Output**:
- [ ] Red/Yellow/Green assessment
- [ ] Specific recommendations
- [ ] Suggested tools to run
```

---

## Phase 4: Execute Conference

Run the conference in this order:

1. **Domain experts first** — Each gives brief input (2-3 key points max)
2. **Senior synthesizes** — Produces the final `/01-senior-reviewr` style report
3. **Prescribe next steps** — Tools to run, actions to take

---

## Output Format

The conference produces a single report:

```markdown
## 📋 Review Conference Report

**Subject**: [What was reviewed]
**Panel**: [List of personas involved]

---

### Panel Input Summary

#### [Persona Name] — [Domain]
- Key insight 1
- Key insight 2

#### [Persona Name] — [Domain]
- Key insight 1
- Key insight 2

---

### 🔴 Hard No (Consensus)
[Issues all panelists agree are blockers]

### 🟡 Yellow Flags
[Concerns raised, with attribution if debated]

### 🟢 Good Calls
[What's working well]

### 💡 Senior's Synthesis
[Final recommendation from Senior Engineer]

---

### 🔧 Prescribed Tools

Based on this review, run these next:
1. `/tool-name` — [Why]
2. `/tool-name` — [Why]
```

---

## Example Invocation

**User**: "I need a comprehensive review of the new Scheduler architecture before we commit"

**Agent Response**:

```markdown
### Triage
- **Subject**: Scheduler architecture
- **Type**: Architecture + UI/UX (cross-cutting)
- **Complexity**: Medium

### Panel Selected
1. `/01-senior-reviewr` (Chair) — Architecture sanity, patterns
2. `/08-ux-specialist` — Interaction flows, user experience
3. `/17-lattice-review` — Layer compliance, contract integrity

### Agenda
- UX: Is the drawer interaction intuitive? Does it follow the contract?
- Lattice: Is the domain/platform separation clean?
- Senior: Overall verdict, ship or rework?

[Proceeds to run conference...]
```

---

## Hierarchy Clarity

```
┌─────────────────────────────────────────┐
│  /00-review-conference (Orchestrator)   │
│                                         │
│  Plans → Selects → Runs conference      │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │  /01-senior-reviewr (Chair)     │    │
│  │                                 │    │
│  │  Synthesizes all panel input    │    │
│  │  Makes final call               │    │
│  │  Prescribes tools               │    │
│  └─────────────────────────────────┘    │
│          ▲           ▲           ▲      │
│          │           │           │      │
│    ┌─────┴───┐ ┌─────┴───┐ ┌─────┴───┐  │
│    │ Persona │ │ Persona │ │ Persona │  │
│    │   2     │ │   3     │ │   4     │  │
│    └─────────┘ └─────────┘ └─────────┘  │
└─────────────────────────────────────────┘
```

**Rule**: Personas report UP to Senior. Senior reports OUT to user.
