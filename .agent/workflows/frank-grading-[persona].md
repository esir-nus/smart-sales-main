---
description: Grade work through Frank's personal heuristics and log successful patterns
last_reviewed: 2026-04-13
---

# Frank Grading Workflow

Apply Frank's personal review principles to project work and capture successful patterns.

**Philosophy:** This is Frank's externalized project intuition. Evidence-based only — nothing speculative.

---

## When to Use

- End of implementation task (grade the outcome)
- After resolving a tricky decision (capture the reasoning)
- When something "feels off" (apply Frank's lens)
- Periodic review (weekly reflection)

---

## Step 1: Load Frank's Principles

Read the current principles file before grading:
```
view_file .agent/frank_principles.md
```

These are Frank's evolved heuristics — treat them as the grading rubric.

---

## Step 2: Apply Grading Lens

For each decision/output being reviewed, ask:

### Frank's Core Questions

| Question | What It Catches |
|----------|-----------------|
| **Did we jump to implementation too fast?** | Frank values understanding before doing |
| **Is there an out-of-box connection we missed?** | Frank sees across domains |
| **Would Frank have asked "why" one more time?** | Frank digs for root cause |
| **Is this pragmatic or over-engineered?** | Frank favors shipping |
| **Did we check evidence before assuming?** | Frank is evidence-based |

### Grading Output

```markdown
## Grading: [Topic]

### 🟢 Aligned with Frank's Style
- [What matched his patterns]

### 🟡 Frank Would Have Pushed Back
- [Where more thinking was needed]

### 🔴 Missed Opportunity
- [Out-of-box connection not made]

### 💡 Pattern to Capture
- [If this worked well, log it]
```

---

## Step 3: Capture Successful Pattern (If Applicable)

If something worked particularly well, add it to the evidence log:

```markdown
### [CANDIDATE] "[Short principle name]"
- **Date**: YYYY-MM-DD
- **Context**: [What problem was solved]
- **Evidence**: [Conversation ID or commit]
- **Frank's Insight**: [Why this worked in Frank's style]
- **Observations**: 1
```

**Location**: `docs/archive/agent-pre-harness/frank_evidence_log.md` (archived 2026-04-13)

---

## Logging Hygiene

| What | Mode |
|------|------|
| New candidates | **STACK** (append to candidates section) |
| Existing candidates | **OVERRIDE** (increment `Observations` count) |
| Promoted rules | **OVERRIDE** (update `frank_principles.md`, don't duplicate) |

---

## Promotion Rules

| Observations | Action |
|--------------|--------|
| 1 | Stays as CANDIDATE |
| 2 | Review for patterns |
| 3+ | Promote to `frank_principles.md` |

---

## Relationship to /frank-reflect

| /frank-grading | /frank-reflect |
|----------------|----------------|
| For project work | For Real Frank |
| Active | Passive |
| Logs successful practices | Logs thinking patterns |
| Does NOT read from reflect | May point to grading for more |

**They evolve separately.** Grading doesn't depend on or reference the thinking journal.
