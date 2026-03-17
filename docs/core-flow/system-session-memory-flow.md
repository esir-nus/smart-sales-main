# Core Flow: System Session Memory Extension

> **Role**: Core Flow
> **Authority**: Behavioral North Star
> **Status**: Ahead of Spec and Code
> **Development Chain**: Architecture -> Core Flow -> Spec -> Code -> PU Test -> Fix Spec -> Fix Code
> **Scope**: The architecture-level session-memory lane that extends RAM with bounded recent-turn context without treating that transient material as SSD truth.
> **Parent Constitution**: `docs/specs/Architecture.md`
> **Testing Directive**: Validate one session-memory universe or one bounded-memory failure branch per run. Do not mix multiple retention timelines in one PU test.

---

## How To Read This Doc

This document defines **what the system session-memory lane must do**.

- If this document conflicts with lower specs, treat the lower specs as drift candidates first.
- If this document conflicts with code, treat the code as transitional first unless the architecture itself is obsolete.
- Other system lanes may consume session memory, but they must not silently redefine its ownership rules.

This file is allowed to be ahead of the codebase.

---

## Downstream Sync Targets

| Layer | Responsibility | Must Eventually Reflect This Core Flow |
|------|----------------|-----------------------------------------|
| **Feature Core Flow** | Feature behavior that depends on recent-turn buildup | Scheduler Path A follow-up context, future clarification-resume flows, future multi-turn query flows |
| **Spec** | How session-memory extension is encoded | Session Working Set specs, RAM-loading specs, bounded retention specs |
| **Code** | Delivered behavior | `ContextBuilder`, session-memory manager, RAM loaders, bounded history providers |
| **PU Test** | Behavioral validation | session-memory retention tests, decay tests, clarification-resume tests, valve audits |

---

## Non-Negotiable Invariants

1. **Session memory is RAM, not SSD truth**: Recent-turn buildup is transient active-session material, not durable world knowledge by default.
2. **Kernel owns admission into Session Working Set**: Other lanes may request or consume session memory, but only the Kernel decides what enters active RAM.
3. **Recent turns may extend RAM without full SSD reload**: The system may carry bounded recent-turn context forward directly when the architecture allows it.
4. **Extension must stay bounded**: Session memory is not an unbounded transcript dump.
5. **Extension must decay or be replaced cleanly**: Stale or superseded recent-turn context must not linger indefinitely as if it were permanent truth.
6. **Session memory supports resume behavior**: Clarification, disambiguation, and feature follow-up flows may use session memory to resume the parent lane instead of starting from zero.
7. **Session memory is shared utility, not a private lane cache**: Query, RL, and feature flows may read it, but no lane owns it as a hidden private state machine.
8. **UI state is downstream of RAM, not part of RAM truth**: Presentation may reflect recent-turn context, but UI flags or rendering state do not become session-memory truth.
9. **The lane is observable**: Session-memory admission, refresh, and eviction should be traceable.

---

## Canonical Valves

- `INPUT_RECEIVED`
- `SESSION_MEMORY_UPDATED`
- `LIVING_RAM_ASSEMBLED`
- `UI_STATE_EMITTED`

---

## Master Routing Flow

```text
                 +----------------------+
                 | New User Turn        |
                 +----------+-----------+
                            |
                            v
                 +----------------------+
                 | Kernel Admission     |
                 | Decision             |
                 +----------+-----------+
                            |
              +-------------+-------------+
              |                           |
              v                           v
     [Reject / Ignore]         [Bounded Context Accepted]
              |                           |
              v                           v
        [No Extension]           +----------------------+
                                 | Session Memory       |
                                 | Update / Replace     |
                                 +----------+-----------+
                                            |
                                            v
                                 +----------------------+
                                 | RAM Re-Assembly /    |
                                 | Refresh              |
                                 +----------+-----------+
                                            |
                                            v
                                 +----------------------+
                                 | Downstream Lane      |
                                 | Reads Updated RAM    |
                                 +----------+-----------+
                                            |
                                            v
                                 +----------------------+
                                 | UI State Emitted     |
                                 +----------------------+
```

---

## Core Universes

### Uni-SM1: Clarification Resume Uses Session Memory
**Scenario**: A query lane asks for clarification, receives repair input, then resumes using bounded recent-turn context.

```text
[Initial Turn] "What did he say about pricing?"
      |
      v
+----------------------+
| Parent Lane Suspends | --> Missing certainty detected
+----------+-----------+
           |
           v
+----------------------+
| Repair Input         | --> User answers "Tom"
+----------+-----------+
  [Valve: INPUT_RECEIVED]
           |
           v
+----------------------+
| Kernel Admission     | --> Repair input is attached to the active thread context
+----------+-----------+
  [Valve: SESSION_MEMORY_UPDATED]
           |
           v
+----------------------+
| RAM Refresh          | --> Session Working Set now carries the repaired context
+----------+-----------+
  [Valve: LIVING_RAM_ASSEMBLED]
           |
           v
+----------------------+
| Parent Lane Resumes  | --> Query proceeds without pretending it is a new unrelated thread
+----------------------+
```

---

### Uni-SM2: Bounded Carry-Forward Without SSD Reload
**Scenario**: A follow-up turn relies mainly on fresh session context rather than a full world reload.

```text
[Recent Turn] "Let's talk about Tom's pricing call"
[Latest Turn] "Keep it shorter"
      |
      v
+----------------------+
| Input Received       | --> New turn enters the OS
+----------+-----------+
  [Valve: INPUT_RECEIVED]
           |
           v
+----------------------+
| Kernel Admission     | --> Relevant recent-turn context is retained in bounded form
+----------+-----------+
  [Valve: SESSION_MEMORY_UPDATED]
           |
           v
+----------------------+
| RAM Refresh          | --> Active RAM reflects the new recent-turn buildup
+----------+-----------+
  [Valve: LIVING_RAM_ASSEMBLED]
           |
           v
+----------------------+
| Downstream Lane      | --> Query or RL consumes the bounded session context
+----------------------+
```

---

### Uni-SM3: Stale Context Evicted
**Scenario**: Old recent-turn material is no longer valid and must not continue contaminating active RAM.

```text
[Context] Prior clarification thread is abandoned or superseded
      |
      v
+----------------------+
| Kernel Admission     | --> Old thread context no longer qualifies for carry-forward
+----------+-----------+
           |
           v
+----------------------+
| Session Memory       | --> Stale material is evicted or replaced cleanly
| Eviction             |
+----------+-----------+
  [Valve: SESSION_MEMORY_UPDATED]
           |
           v
+----------------------+
| RAM Refresh          | --> Active session no longer presents stale context as live truth
+----------+-----------+
  [Valve: LIVING_RAM_ASSEMBLED]
           |
           v
+----------------------+
| UI State Emitted     | --> Presentation reflects the cleaned active state
+----------------------+
  [Valve: UI_STATE_EMITTED]
```

---

## What Must Sync Next

1. Session / RAM specs
   - Session Working Set admission rules
   - bounded recent-turn retention
   - eviction / decay behavior

2. dependent system flows
   - query clarification and disambiguation resume
   - RL learning material ownership
   - feature follow-up and reschedule context rules

3. code and tests
   - ensure only the Kernel admits recent-turn context into RAM
   - ensure bounded carry-forward exists without full SSD reload every turn
   - ensure stale context is evicted cleanly
