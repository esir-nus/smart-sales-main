# Core Flow: System Query Assembly

> **Role**: Core Flow
> **Authority**: Behavioral North Star
> **Status**: Ahead of Spec and Code
> **Development Chain**: Architecture -> Core Flow -> Spec -> Code -> PU Test -> Fix Spec -> Fix Code
> **Scope**: The architecture-level sync/query lane from raw user input through phase-0 gateway, alias resolution, RAM assembly, and user-visible response.
> **Parent Constitution**: `docs/specs/Architecture.md`
> **Testing Directive**: Validate one query universe or one safety branch per run. Do not mix multiple grounding timelines in one PU test.

---

## How To Read This Doc

This document defines **what the system query lane must do**.

- If this document conflicts with lower specs, treat the lower specs as drift candidates first.
- If this document conflicts with code, treat the code as behind or transitional first.
- Feature flows may specialize this behavior, but they must stay inside these system rules.

This file is allowed to be ahead of the codebase.

---

## Downstream Sync Targets

| Layer | Responsibility | Must Eventually Reflect This Core Flow |
|------|----------------|-----------------------------------------|
| **Feature Core Flow** | Feature behavior inside the system | Scheduler Path B, future CRM query flows, future chat grounding flows |
| **Spec** | How the query lane is encoded | pipeline specs, context specs, alias/disambiguation specs, session-memory specs, plugin gateway specs |
| **Code** | Delivered behavior | `IntentOrchestrator`, Lightning Router, alias cache, disambiguation services, `ContextBuilder`, prompt compiler |
| **PU Test** | Behavioral validation | pipeline tests, grounding tests, ambiguity tests, valve audits |

---

## Non-Negotiable Invariants

These are system laws. Lower layers may choose different classes, prompts, or DTOs, but they must preserve these outcomes.

1. **Phase-0 gateway happens before deep work**: The system must first decide whether the input should be short-circuited, clarified, delegated, or sent deeper into System II.
2. **Noise does not consume System II**: Meaningless input must not trigger heavy RAM assembly or mutation logic.
3. **Grounding prefers fast alias resolution first**: Candidate names should hit L1 alias/cache logic before heavy SSD graph fetch.
4. **Clarification loops are anti-guessing gates**: If required certainty is missing, the system must ask rather than invent.
5. **Ambiguity halts the deep path**: If multiple grounded candidates remain, the system must yield disambiguation rather than pretend certainty.
6. **Verified IDs precede heavy world fetch**: Heavy SSD graph load should happen after grounding, not before.
7. **Kernel owns RAM assembly**: The query lane may ask for context, but only the Kernel determines what enters the Session Working Set.
8. **Ungrounded queries may still proceed minimally**: If the query does not require entity grounding, the system may continue with minimal or partial RAM rather than forcing false clarification.
9. **The LLM reads bounded reality**: The Brain acts on assembled RAM, not on guessed world state.
10. **Query output is not mutation currency**: Conversational response text is not the same thing as structured mutation payload.
11. **Minor loops resume the parent lane**: Clarification and disambiguation should suspend unsafe progress, repair certainty, then resume the query lane.
12. **Presentation is downstream of RAM truth**: UI state may reflect the query result, but presentation state is not itself domain or RAM truth.
13. **The lane is observable**: The main toll booths must be traceable through valve checkpoints.

---

## Minor Loop Rule

The system query lane contains bounded trust-preserving minor loops.

### Purpose

These loops prevent confident-looking guessing that later causes user distrust.

### Minor Loops In This Lane

- **Clarification loop**: asks for missing certainty when no valid candidate or required grounding input exists
- **Disambiguation loop**: asks the user to choose when multiple grounded candidates remain

### Resume Pattern

The pattern is:

1. suspend the deeper path
2. yield the clarification request
3. receive the user repair input
4. resume the parent query lane

These loops are not separate major lanes.
They are semi-loops inside the query lane.

---

## Canonical Valves

These valve names are the architectural checkpoints for downstream specs and tests.

- `INPUT_RECEIVED`
- `ROUTER_DECISION`
- `ALIAS_RESOLUTION`
- `SSD_GRAPH_FETCHED`
- `LIVING_RAM_ASSEMBLED`
- `LLM_BRAIN_EMISSION`
- `UI_STATE_EMITTED`

---

## Master Routing Flow

This is the top-level routing model for the architecture-level query lane.

```text
                 +----------------------+
                 | Raw User Input       |
                 +----------+-----------+
                            |
                            v
                 +----------------------+
                 | Phase-0 Gateway      |
                 | + Router Decision    |
                 +----------+-----------+
                            |
         +------------------+-------------------+------------------+
         |                  |                   |                  |
         v                  v                   v                  v
   [Noise / Greeting] [Clarification Loop] [Delegate / Plugin] [Proceed System II]
         |                  |                   |                  |
         v                  v                   v                  v
    [Short Response]   [Yield Question]   [Bounded Handoff] +------------------+
                              |                              | Alias / Ground   |
                              |                              +---------+--------+
                              |                                        |
                              +---------------[Repair Input]-----------+
                                                                       |
                                           +---------------------------+---------------------------+
                                           |                           |                           |
                                           v                           v                           v
                                   [Exact Match]         [Disambiguation Loop]          [No Entity Needed]
                                           |                           |                           |
                                           v                           v                           v
                                  +------------------+          [Yield Options]          +------------------+
                                  | SSD -> RAM       |                                   | Minimal / Partial |
                                  | Assembly         |                                   | RAM Assembly      |
                                  +---------+--------+                                   +---------+--------+
                                            ^                                                      |
                                            |                                                      |
                                            +-----------[Repair Input / Resume]--------------------+
                                            |                                                      |
                                            +--------------------+---------------------------------+
                                                                 |
                                                                 v
                                                        +------------------+
                                                        | LLM Brain        |
                                                        +---------+--------+
                                                                  |
                                                                  v
                                                        +------------------+
                                                        | User-Visible     |
                                                        | Response / State |
                                                        +------------------+
```

---

## Core Universes

### Uni-Q1: Short-Circuit Query
**Scenario**: Input is greeting, noise, or other low-value chatter.

```text
[User Input] "hello"
      |
      v
+----------------------+
| Input Received       | --> Raw input enters the OS
+----------+-----------+
  [Valve: INPUT_RECEIVED]
           |
           v
+----------------------+
| Phase-0 Gateway      | --> Classified as GREETING / NOISE
+----------+-----------+
  [Valve: ROUTER_DECISION]
           |
           v
+----------------------+
| Short Response       | --> Minimal response or mascot-style intercept
+----------+-----------+
           |
           v
+----------------------+
| UI State Emitted     | --> No deep RAM path consumed
+----------------------+
  [Valve: UI_STATE_EMITTED]
```

---

### Uni-Q2: Exact Grounded Query
**Scenario**: Input refers to a resolvable entity and requires grounded context.

```text
[User Input] "What did Tom say about pricing?"
      |
      v
+----------------------+
| Input Received       | --> Raw input enters the OS
+----------+-----------+
  [Valve: INPUT_RECEIVED]
           |
           v
+----------------------+
| Phase-0 Gateway      | --> Routed into System II
+----------+-----------+
  [Valve: ROUTER_DECISION]
           |
           v
+----------------------+
| Alias Resolution     | --> "Tom" resolves to one verified entity ID
+----------+-----------+
  [Valve: ALIAS_RESOLUTION]
           |
           v
+----------------------+
| SSD Graph Fetch      | --> Heavy world knowledge fetched
+----------+-----------+
  [Valve: SSD_GRAPH_FETCHED]
           |
           v
+----------------------+
| Living RAM Assembly  | --> Kernel assembles bounded context
+----------+-----------+
  [Valve: LIVING_RAM_ASSEMBLED]
           |
           v
+----------------------+
| LLM Brain            | --> Response generated from bounded RAM
+----------+-----------+
  [Valve: LLM_BRAIN_EMISSION]
           |
           v
+----------------------+
| UI State Emitted     | --> User sees grounded answer
+----------------------+
  [Valve: UI_STATE_EMITTED]
```

---

### Uni-Q3: Ambiguous Grounding
**Scenario**: A candidate name maps to multiple valid entities.

```text
[User Input] "What did Jack say?"
      |
      v
+----------------------+
| Input Received       | --> Raw input enters the OS
+----------+-----------+
           |
           v
+----------------------+
| Phase-0 Gateway      | --> Routed into System II
+----------+-----------+
  [Valve: ROUTER_DECISION]
           |
           v
+----------------------+
| Alias Resolution     | --> Multiple candidate IDs remain
+----------+-----------+
  [Valve: ALIAS_RESOLUTION]
           |
           v
+----------------------+
| Disambiguation Loop  | --> User is shown grounded options instead of a guessed answer
+----------------------+
  [Valve: UI_STATE_EMITTED]
```

---

### Uni-Q4: Clarification Repair And Resume
**Scenario**: The system initially lacks enough certainty, asks the user, then resumes the same query lane after repair.

```text
[Initial Input] "What did he say about pricing?"
      |
      v
+----------------------+
| Input Received       | --> Raw input enters the OS
+----------+-----------+
           |
           v
+----------------------+
| Phase-0 Gateway      | --> Missing certainty detected
+----------+-----------+
  [Valve: ROUTER_DECISION]
           |
           v
+----------------------+
| Clarification Loop   | --> User is asked to repair the missing certainty
+----------+-----------+
  [Valve: UI_STATE_EMITTED]
           |
           v
+----------------------+
| Repair Input         | --> User answers "Tom"
+----------+-----------+
           |
           v
+----------------------+
| Parent Lane Resumes  | --> Query lane continues with repaired grounding
+----------+-----------+
           |
           v
+----------------------+
| Alias -> SSD -> RAM  | --> System proceeds without guessing
+----------+-----------+
  [Valve: ALIAS_RESOLUTION / SSD_GRAPH_FETCHED / LIVING_RAM_ASSEMBLED]
           |
           v
+----------------------+
| LLM Brain            | --> Response generated from repaired certainty
+----------+-----------+
  [Valve: LLM_BRAIN_EMISSION]
           |
           v
+----------------------+
| UI State Emitted     | --> User sees answer after the same lane resumes
+----------------------+
  [Valve: UI_STATE_EMITTED]
```

---

### Uni-Q5: Minimal Query Without Grounding
**Scenario**: Input does not require entity grounding, but still deserves deep handling.

```text
[User Input] "How should I respond to a price objection?"
      |
      v
+----------------------+
| Input Received       | --> Raw input enters the OS
+----------+-----------+
           |
           v
+----------------------+
| Phase-0 Gateway      | --> Routed into System II
+----------+-----------+
  [Valve: ROUTER_DECISION]
           |
           v
+----------------------+
| Minimal RAM Assembly | --> No entity graph required
+----------+-----------+
  [Valve: LIVING_RAM_ASSEMBLED]
           |
           v
+----------------------+
| LLM Brain            | --> Response generated from bounded non-entity context
+----------+-----------+
  [Valve: LLM_BRAIN_EMISSION]
           |
           v
+----------------------+
| UI State Emitted     | --> User sees answer without fake grounding
+----------------------+
  [Valve: UI_STATE_EMITTED]
```

---

## What Must Sync Next

The lower layers should converge toward this flow in the following order:

1. pipeline and intent specs
   - phase-0 gateway
   - router / alias / disambiguation ownership
   - minimal-vs-grounded RAM rules

2. context and kernel specs
   - RAM assembly boundaries
   - Session Working Set loading rules
   - session-memory resume ownership

3. neighboring system flows
   - session-memory extension flow
   - plugin gateway flow when bounded handoff is involved

4. feature core-flow docs
   - scheduler memory highway
   - future CRM query features

5. code and tests
   - ensure the active entrypoint honors the gateway -> alias -> RAM architecture
   - ensure ambiguity yields instead of guessing
   - ensure valve coverage remains observable
