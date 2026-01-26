---
description: Log Frank's stream of consciousness and thinking patterns for self-reflection
---

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

**Location**: `.agent/frank_thinking_journal.md`

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
