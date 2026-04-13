---
description: "[ARCHIVED 2026-04-13] Log Frank's stream of consciousness and thinking patterns for self-reflection. Produced 1 entry in 15 months. No longer always-on."
trigger: manual
last_reviewed: 2026-04-13
---

> **Status: Archived 2026-04-13.** This workflow was always-on but produced minimal output. Demoted to manual trigger. The frank_*.md files are preserved as historical reference.

# Frank Reflection

Passive observation layer between Real Frank and the agent persona.

---

## Purpose

This is an **I/O layer** for learning the real Frank. It:
- **Observes** Frank's thinking patterns
- **Logs** without judgment or action
- **Never auto-executes** — only acts on explicit instruction

The output (thinking journal) is primarily FOR Frank's self-reflection. The agent reads it only when explicitly helpful.

---

## Default Mode: Passive Logger

When invoked or at session end, append observations to the journal:

```markdown
## [Date]

### Observed Thinking
- [Pattern, jump, connection noticed]

### Communication Style
- [How Frank expressed something]

### Session Flow
- [Arc of the conversation]
```

**Location**: `docs/archive/agent-pre-harness/frank_thinking_journal.md` (archived 2026-04-13)

**No grading. No actions. Just observation.**

---

## On Explicit Instruction

If Frank says "analyze", "summarize", "find patterns", etc., THEN:
- Read recent journal entries
- Perform the requested analysis
- Output to Frank (not to journal)

Otherwise, stay passive.

---

## Relationship to /frank-grading

| /frank-reflect | /frank-grading |
|----------------|----------------|
| For Real Frank | For project work |
| Passive | Active |
| Logs thinking patterns | Logs successful practices |
| Points to grading for more | Does NOT point back |

**They evolve separately.** If a thinking pattern is also a successful practice, `/frank-grading` logs it independently. Reflect does NOT feed into grading automatically.

---

## Logging Hygiene

| What | Mode |
|------|------|
| New journal entries | **STACK** (append) |
| Same-day entries | Append to same date section |
| Quarterly | Archive to `frank_thinking_journal_archive_YYYY.md` |

**Never prune mid-session.** Journal is raw material.
