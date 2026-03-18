# Core Flow: System Reinforcement Write-Through

> **Role**: Core Flow
> **Authority**: Behavioral North Star
> **Status**: Ahead of Spec and Code
> **Development Chain**: Architecture -> Core Flow -> Spec -> Code -> PU Test -> Fix Spec -> Fix Code
> **Scope**: The architecture-level reinforcement loop that listens in the background, extracts habit observations, writes them to SSD, and refreshes RAM without blocking the main path.
> **Parent Constitution**: `docs/specs/Architecture.md`
> **Testing Directive**: Validate one reinforcement universe or one no-op branch per run. Do not mix multiple habit timelines in one PU test.

---

## How To Read This Doc

This document defines **what the RL / habit lane must do**.

- If this document conflicts with lower specs, treat the lower specs as drift candidates first.
- If this document conflicts with code, treat the code as transitional first unless the architecture itself is obsolete.

This file is allowed to be ahead of the codebase.

---

## Downstream Sync Targets

| Layer | Responsibility | Must Eventually Reflect This Core Flow |
|------|----------------|-----------------------------------------|
| **Spec** | How the habit lane is encoded | RL specs, habit repository specs, Session Working Set specs |
| **Code** | Delivered behavior | habit listener, reinforcement learner, habit repository, context builder |
| **PU Test** | Behavioral validation | RL extraction tests, write-through tests, no-op tests |

---

## Non-Negotiable Invariants

1. **Reinforcement never blocks the main response**: The user-facing query lane must not wait on the habit learner to finish.
2. **The trigger is always the latest user input**: The reinforcement lane starts from a new user utterance event, not from continuous rescanning of the whole session.
3. **Learning material is contextual**: The learner should assess the latest user input together with bounded session history and active entity context, not the latest utterance in isolation.
4. **Observation extraction is typed**: Background observations must be parsed into structured RL payloads before they reach storage.
5. **Empty learning is a valid outcome**: If no habit is learned, the system should no-op quietly.
6. **SSD truth and RAM should converge**: When a new habit is accepted, the active session should refresh the relevant RAM sections through approved write-through.
7. **Global and contextual habits stay distinct**: User-global habits and entity-contextual habits must not collapse into one shapeless bucket.
8. **The active entity set matters**: Contextual habits should derive from the currently active session context, not random global lookup.
9. **Fragmented turns are normal**: Users may express a habit across multiple short or incomplete turns, so the learner must treat the session as accumulating material rather than demand one perfect sentence.
10. **Session-memory ownership stays outside RL**: The learner may consume bounded recent-turn context, but it does not own session-memory admission rules.
11. **Broader RAM expansion must be explicit**: Existing habit context, tool artifacts, and document context do not silently enter the learning packet unless a later spec explicitly adds them.
12. **Scheduler signals are user-habit only by default**: Scheduler-derived pattern signals may inform user-global habits, but they must not silently bleed into client/entity habit inference.
13. **Contextual habits require active entity grounding**: A non-null entity habit must resolve against the active entity set in session context before it is accepted.
14. **Presentation is downstream of learned truth**: UI may reflect new habits after RAM refresh, but presentation state is not itself the learned fact.
15. **The lane is observable**: Background learning and refresh should be testable and traceable even if it is not user-blocking.

---

## Learning Material Contract

The reinforcement lane should learn from a bounded contextual packet, not from isolated raw text.

### Trigger

The trigger is always:

- a new user input arriving in the system

This means:

- the learner is event-driven
- the learner does not continuously rescan the full session by itself
- each new user input creates one new chance to learn

### Required Learning Material

- latest user input
- bounded recent session history
- active entity context

### Optional Supporting Signal

- `schedulerPatternContext` for **user-global** habit learning only

### Why

Users often speak in fragments.
The real signal may only emerge across turns, for example:

- "No, not in the morning"
- "Make it shorter next time"
- "Actually with Tom, keep it brief"

The learner should therefore treat the newest utterance as the trigger, but use recent session buildup plus the active entity set as the learning material.

### Rule

The newest user input is the **trigger** and **entrypoint**.
The recent session history plus active entity context are the **interpretation frame**.

The learner must not require all habit meaning to be fully contained inside one isolated utterance.
Broader RAM fields may be added later, but only through an explicit contract update.

### Scheduler-Derived Habit Signals

Scheduler behavior is a legitimate source of **user-global** habit learning, but it should be treated carefully.

Allowed direction:

- learn stable user scheduling tendencies from scheduler-derived pattern signals
- examples:
  - preferred meeting times
  - preferred durations
  - scheduling lead time
  - reschedule tendency
  - urgency style

Not allowed by default:

- using raw scheduler context as a blanket RL packet
- using scheduler-derived signals to infer **client/entity** habits unless a later feature spec explicitly allows it

Rule:

- scheduler-derived signals may enrich **user habit** learning
- active entity context remains the default source for **client habit** learning
- if scheduler context is later added to the runtime packet, it should enter as a narrow summarized signal such as `schedulerPatternContext`, not as a raw transcript dump

---

## Canonical Valves

- `LIVING_RAM_ASSEMBLED`
- `RL_LISTENER_TRIGGERED`
- `RL_SCHEDULER_PATTERN_ATTACHED`
- `RL_EXTRACTION_EMITTED`
- `RL_PAYLOAD_DECODED`
- `RL_HABIT_WRITE_EXECUTED`
- `RL_RAM_REFRESH_APPLIED`
- `DB_WRITE_EXECUTED`

---

## Master Routing Flow

```text
                 +----------------------+
                 | Main Query Lane      |
                 | Receives New Input   |
                 | + Builds RAM + Reply |
                 +----------+-----------+
                            |
                            v
                 +----------------------+
                 | Background Listener  |
                 | Fires On Latest      |
                 | Input Event          |
                 +----------+-----------+
                            |
                            v
                 +----------------------+
                 | RL Extraction        |
                 | Uses Latest Input    |
                 | + History + RAM      |
                 +----------+-----------+
                            |
              +-------------+-------------+
              |                           |
              v                           v
         [No Habit]                 [Observations Found]
              |                           |
              v                           v
         [Silent No-Op]          +----------------------+
                                 | Habit Repository     |
                                 | SSD Write            |
                                 +----------+-----------+
                                            |
                                            v
                                 +----------------------+
                                 | RAM Habit Refresh    |
                                 | Section 2 / 3        |
                                 +----------------------+
```

---

## Core Universes

### Uni-R1: Global Habit Learned
**Scenario**: The user expresses a general personal preference.

```text
[User Input] "I prefer morning meetings"
      |
      v
+----------------------+
| Main Query Lane      | --> User still gets normal main response
+----------+-----------+
           |
           v
+----------------------+
| Background Listener  | --> Latest input + recent history + active RAM copied asynchronously
+----------+-----------+
  [Valve: LIVING_RAM_ASSEMBLED]
           |
           v
+----------------------+
| RL Extraction        | --> Typed global observation extracted
+----------+-----------+
  [Valve: LLM_BRAIN_EMISSION]
  [Valve: LINTER_DECODED]
           |
           v
+----------------------+
| Habit SSD Write      | --> Global habit is persisted
+----------+-----------+
  [Valve: DB_WRITE_EXECUTED]
           |
           v
+----------------------+
| RAM Refresh          | --> Section 2 reflects the new preference
+----------------------+
```

---

### Uni-R2: Contextual Client Habit Learned
**Scenario**: The learned preference applies to the currently active entity context.

```text
[Context] Active client entity in RAM
[User Input] "Tom usually wants short meetings"
      |
      v
+----------------------+
| Background Listener  | --> Active entity context is available
+----------+-----------+
           |
           v
+----------------------+
| RL Extraction        | --> Typed observation linked to active entity
+----------+-----------+
  [Valve: LLM_BRAIN_EMISSION]
  [Valve: LINTER_DECODED]
           |
           v
+----------------------+
| Habit SSD Write      | --> Contextual habit is persisted
+----------+-----------+
  [Valve: DB_WRITE_EXECUTED]
           |
           v
+----------------------+
| RAM Refresh          | --> Section 3 reflects the new client habit
+----------------------+
```

---

### Uni-R3: No Learnable Habit
**Scenario**: Input contains no stable habit signal.

```text
[User Input] "The weather is good today"
      |
      v
+----------------------+
| Background Listener  | --> Input copied asynchronously
+----------+-----------+
           |
           v
+----------------------+
| RL Extraction        | --> No habit observations found
+----------+-----------+
           |
           v
+----------------------+
| Silent No-Op         | --> No SSD write, no RAM refresh
+----------------------+
```

---

### Uni-R4: Fragmented Preference Across Turns
**Scenario**: The user reveals the habit through multiple short turns rather than one self-contained sentence.

```text
[Recent History] "Set the meeting for Tom tomorrow morning"
[Latest Input] "No, keep it short next time"
      |
      v
+----------------------+
| Background Listener  | --> Latest input is triggered with recent session buildup attached
+----------+-----------+
           |
           v
+----------------------+
| RL Extraction        | --> Learner infers a contextual preference using history + active RAM
+----------+-----------+
  [Valve: LLM_BRAIN_EMISSION]
  [Valve: LINTER_DECODED]
           |
           v
+----------------------+
| Habit SSD Write      | --> Contextual habit is persisted if confidence is sufficient
+----------+-----------+
  [Valve: DB_WRITE_EXECUTED]
           |
           v
+----------------------+
| RAM Refresh          | --> Active session reflects the newly learned contextual preference
+----------------------+
```

**Behavioral note**: The learning trigger is still the latest utterance, but the meaning is resolved against recent session buildup. This prevents the learner from missing fragmented human behavior.

---

## What Must Sync Next

1. RL specs
   - observation schema
   - learning material contract: latest input + bounded history + active RAM
   - global vs contextual habit ownership
   - write-through refresh rules

2. Session / context specs
   - Section 2 and Section 3 refresh semantics
   - session-memory provider ownership

3. code and tests
   - ensure RL remains asynchronous
   - ensure habit writes refresh RAM
   - ensure no-habit cases remain quiet
