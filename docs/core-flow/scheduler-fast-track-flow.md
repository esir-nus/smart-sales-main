# Core Flow: Scheduler Fast-Track (Path A)

> **Role**: Core Flow
> **Authority**: Behavioral North Star
> **Status**: Ahead of Spec and Code
> **Development Chain**: Core Flow -> Spec -> Code -> PU Test -> Fix Spec -> Fix Code
> **Scope**: Path A Scheduler Fast-Track execution from audio/transcript intake to UI-visible result or explicit fast-fail feedback.
> **Testing Directive**: Validate one universe or one safety branch per run. Do not combine multiple timelines in a single PU run.

---

## How To Read This Doc

This document defines **what must happen**, not the final DTO shape or the current implementation details.

- If this document conflicts with lower specs, treat the lower specs as drift candidates first.
- If this document conflicts with code, treat the code as behind or off-contract first.
- If a PU test does not cover a branch defined here, the validation surface is incomplete.

This file is allowed to be ahead of the codebase.

---

## Downstream Sync Targets

| Layer | Responsibility | Must Eventually Reflect This Core Flow |
|------|----------------|-----------------------------------------|
| **Spec** | How the behavior is encoded | `docs/specs/scheduler-path-a-execution-prd.md`, future/updated scheduler specs, UI specs, interface docs |
| **Code** | Delivered behavior | Fast-track parser/linter, mutation engine, repositories, schedule board, scheduler UI |
| **PU Test** | Behavioral validation | `docs/plans/PU-tracker.md`, L2 scenario runner, acceptance flows, relevant domain/UI tests |

---

## Non-Negotiable Invariants

These are behavioral laws. Lower layers may choose different types or DTO shapes, but they must preserve these outcomes.

1. **No fabricated time**: Missing temporal data must never be silently invented.
2. **No silent drops for real user intent**: Meaningful schedulable input must become either a task, a flagged task, or explicit user-facing failure feedback.
3. **Inspiration is not a task**: Timeless intent must never bleed into the task table.
4. **Conflict does not reject creation**: A schedulable task with a time conflict is still accepted, persisted, and visibly flagged.
5. **Null or meaningless input does not mutate state**: Empty or useless input must not write a task or inspiration record.
6. **Bad target resolution does not mutate state**: No-match or ambiguous reschedule targeting must fail safely with explicit feedback.
7. **Path A uses small session memory**: Follow-up utterances may rely on short-lived scheduling context from the active session.
8. **Reschedule is replacement, not surgical edit**: Path A should prefer contextual recreate-then-retire semantics over field-by-field correction semantics. Time-anchor retitle keeps this rule: the old task is replaced with a same-time, same-duration task carrying the new title, not patched through a separate title-edit path. Cancel wording plus a replacement event at the same time anchor, such as `晚上8点的开会取消了，得去机场接人。`, is this same replacement behavior; pure cancellation without a replacement event remains unsupported unless a separate delete contract is approved.
9. **Path A and Path B are decoupled**: They run independently and do not block each other.
10. **Unified GUID is the bridge**: Path A and Path B share one pipeline GUID for mapping, correlation, and later status linkage.
11. **Expanded scheduler card is read-only enrichment**: Path B may feed relevant memory context into the expanded card window, but that surface must not mutate CRM/entity memory.
12. **Shared pre-fork extraction is reusable**: GUID, urgency level, and first-pass candidate entity extraction should happen once and feed both paths.
13. **Path B is value-gated**: Only high-value tasks should pay the Path B enrichment cost.
14. **FIRE_OFF tasks bypass collision logic**: One-time fire-off tasks do not collide with other tasks, even if the UI still reflects urgency visually.
15. **Exact-task conflict occupancy is domain-owned**: For non-`FIRE_OFF` exact tasks, conflict evaluation must use explicit duration when present, otherwise a domain-owned conflict-only occupancy window derived from task semantics and then urgency fallback. This occupancy window is for collision evaluation only and must not silently overwrite the user-visible persisted duration.
16. **Scheduler cards preserve separate glanceable signals**: Task level / urgency must stay glanceable on scheduler cards, while conflict state and completion state remain independently visible rather than collapsing into one merged signal.

---

## Path A / Path B Split

### Path A: Execute

Path A is the fast voice-native execution path.

- create scheduled tasks
- create vague / needs-time tasks
- detect visible conflict
- handle reschedule by contextual replacement
- keep UX lightweight and optimistic
- reflect urgency visually in the scheduler when useful

Path A may use **small session memory** to understand a user follow-up such as:

- "ok, 9 pm"
- "actually, the meeting is 9pm three days later"

This session memory is short-lived continuation context for the active scheduling thread.
It is not the same as CRM/entity memory.
It also does not authorize omitted-target reschedule mutation: reschedule still requires an explicit target plus a new exact time in the current utterance.

### Path B: Inform

Path B is the later memory/entity relevance path.

- reads CRM/memory entities
- finds relevant people, locations, events, or related conflicts
- feeds the user contextual information
- does not block Path A execution
- only runs when the task passes the Path B value gate

### The Connection

Path A and Path B are connected by a **unified GUID** created at the beginning of the pipeline.

The GUID allows:

- mapping Path B relevance back to the Path A task
- later status linkage across independently running paths
- SQL-level correlation without tightly coupling the runtime logic

This is the only required bridge.

Identity rule:

- the Path A `unified GUID` is the stable scheduler thread identity
- in Path B terminology, this same stable identity is `root_guid`
- reschedule or radical reinterpretation may advance `revision_no`
- a revisioned view such as `lineage_guid = root_guid/revision_no` may be used for downstream replacement and supersession logic

This means `unified GUID == root_guid`.
`lineage_guid` is the revision-scoped projection of that same scheduler thread, not a separate second identity.

### Shared Pre-Fork Extraction

After the intent gate confirms the input is schedulable, but before Path A and Path B diverge, the pipeline should produce one shared scheduling envelope:

- unified GUID
- schedulable classification
- urgency / task level
- first-pass candidate entity extracted from user input when present

Then:

- one copy feeds Path A execution
- one copy may feed Path B enrichment

Path B should reuse this first-pass candidate instead of rediscovering the same first entity from scratch.

### Urgency / Task Classification Semantics

The shipped domain enum uses `UrgencyLevel` values rather than the older `L1/L2/L3/L4` task-level shorthand.
This classification is a hard behavioral contract, not a soft hint.

- `FIRE_OFF`: one-time fire-off reminder or chore
- `L3_NORMAL`: normal scheduled task
- `L2_IMPORTANT`: externally impactful task such as meetings, calls, or reports
- `L1_CRITICAL`: highest-risk task such as flights, interviews, or signing deadlines

Meaning:

- `FIRE_OFF` tasks are one-time fire-off actions. They may still look urgent in kanban, but they do not participate in collision logic and do not enter Path B.
- `L3_NORMAL` tasks are normal schedulable work. They participate in normal Path A scheduling and collision behavior, but do not automatically enter Path B.
- `L2_IMPORTANT` tasks are high-value scheduled work. They still participate in normal Path A collision behavior and may enter Path B when the value gate and candidate extraction justify enrichment.
- `L1_CRITICAL` tasks are the highest-risk scheduled work. They still participate in normal Path A collision behavior and may enter Path B when the value gate and candidate extraction justify enrichment.

Reminder cascade contract:

- `L1_CRITICAL` must arm `-60m`, `-10m`, and `0m`
- `L2_IMPORTANT` must arm `-30m` and `0m`
- `L3_NORMAL` must arm `0m` only
- `FIRE_OFF` must arm `0m` only
- `L3_NORMAL` and `FIRE_OFF` may share the same reminder count, but they do not share semantics: `L3_NORMAL` remains a normal scheduled task, while `FIRE_OFF` remains a one-shot fire-off task that bypasses collision logic and must never enter Path B
- lower layers must normalize reminder metadata such as `hasAlarm` and `alarmCascade` from the task's current urgency contract rather than trusting stale persisted cascade data

Hard gate:

- Path A may schedule `FIRE_OFF`, `L3_NORMAL`, `L2_IMPORTANT`, and `L1_CRITICAL`
- Path B must never run for `FIRE_OFF`
- Path B is value-gated for higher-value exact tasks rather than being implied by every scheduled item

Conflict occupancy clarification:

- `FIRE_OFF` bypasses conflict evaluation entirely
- non-`FIRE_OFF` exact tasks use explicit duration when present
- otherwise deterministic conflict occupancy falls back to task semantics first, then urgency defaults
- these fallback occupancy windows exist only for conflict evaluation and must not mutate the persisted visible duration

Clarification:

- `L2_IMPORTANT` and `L1_CRITICAL` may still validly no-op in Path B when no usable candidate entity is produced
- this is acceptable for impersonal high-value tasks such as flights or self-only tasks
- no clarification loop is required by this Core Flow when candidate extraction is absent
- future habit-based or suggestion-based enrichment may be added later as tech debt, but it is not required behavior here

### Scheduler Card Signal Rule

The scheduler card surface must preserve three separate user-visible meanings:

- task level / urgency stays glanceable on the collapsed card
- conflict state remains independently visible from urgency
- completion state remains independently visible from urgency

Lower layers own the concrete bar, chip, icon, color, or typography treatment.
This Core Flow only requires that those meanings do not collapse into a single visual channel.

### Expanded Card Window Rule

When a scheduler card is expanded, the small detail window may display Path B memory context relevant to that scheduled task.

That window is:

- for information feeding
- for relevance surfacing
- for helping the user notice related people, events, locations, or conflicts

That window is **not**:

- a CRM editing surface
- a memory-writing surface
- an entity override surface
- an interactive Path B workflow

---

## Canonical Valves

These valve names are the behavioral checkpoints for downstream specs and tests.
The exact telemetry event names in code may differ today, but they should converge toward this model.

- `ASR_CAPTURED`
- `GUID_ALLOCATED`
- `TASK_LEVEL_CLASSIFIED`
- `ENTITY_CANDIDATE_EXTRACTED`
- `ASR_EMPTY_OR_GARBLED`
- `INTENT_CLASSIFIED`
- `TASK_EXTRACTED`
- `TASK_EXTRACTED_VAGUE`
- `THOUGHT_EXTRACTED`
- `TARGET_RESOLVED`
- `TARGET_MISSING`
- `TARGET_AMBIGUOUS`
- `CONFLICT_EVALUATED`
- `CONFLICT_BYPASSED_FIRE_OFF`
- `DB_WRITE_EXECUTED`
- `UI_RENDERED`
- `FAST_FAIL_RETURNED`

---

## Master Routing Flow

This is the top-level routing model for Path A.

```text
                    +------------------+
                    |   ASR Capture    |
                    +---------+--------+
                              |
                              v
                    +------------------+
                    |  GUID Allocate   |
                    +---------+--------+
                              |
                              v
                    +------------------+
                    |   Intent Gate    |
                    +---------+--------+
                              |
          +-------------------+-------------------+
          |                   |                   |
          v                   v                   v
   +--------------+   +---------------+   +----------------+
   | NULL / NOISE |   |  INSPIRATION  |   |  SCHEDULABLE   |
   +------+-------+   +-------+-------+   +--------+-------+
          |                   |                    |
          v                   v                    v
   [Fast-Fail Path]   [Inspiration Path]   +------------------+
                                            | Level / Candidate |
                                            | Extract           |
                                            +---------+--------+
                                                      |
                                                      v
                                            +------------------+
                                            | FastTrack Parser |
                                            +---------+--------+
                                                      |
                           +--------------------------+--------------------------+
                           |                          |                          |
                           v                          v                          v
                  +----------------+        +----------------+        +----------------------+
                  | CREATE EXACT   |        | CREATE VAGUE   |        | RESCHEDULE FOLLOW-UP |
                  +--------+-------+        +--------+-------+        +----------+-----------+
                           |                         |                           |
                           v                         v                           v
              +--------------------------+   [Skip Conflict]      +---------------------------+
              | Conflict Gate            |          |             | Session Memory / Target   |
              +------------+-------------+          |             | Context Resolve           |
                           |                        |             +-------------+-------------+
                   +-------+-------+                |                           |
                   |               |                |             +-------------+-------------+
                   v               v                v             |             |             |
           [Conflict Check] [Fire-Off Bypass] [Vague Create]     v             v             v
                   |               |                        [Replacement OK] [No Match] [Ambiguous]
          +--------+--------+      |                               |
          |                 |      |                               v
          v                 v      v                     [Create New -> Retire Old]
   [Specific Create] [Time Conflict] [Specific Create]
```

---

## The Gatekeeper

Before any scheduler mutation occurs, the system classifies input into one of these categories:

1. **Null / Noise / Meaningless**
   - Empty capture
   - Garbled transcript
   - Greeting or chatter with no actionable or inspirational value
   - Result: fast-fail with explicit non-mutation

2. **Inspiration (Timeless)**
   - Aspirations
   - Ideas
   - Notes that do not belong on a strict timeline
   - Result: persist to inspiration flow only

3. **Schedulable**
   - Actionable task or event intent
   - Can branch into:
     - exact create
     - vague create
     - reschedule
   - Exact vs vague determination is semantic extraction work, not string-heuristic work
   - Carries shared pre-fork data:
     - lineage GUID
     - urgency / task level
     - first-pass candidate entity, if present

---

## Core Universes

These are the main behavioral universes for Path A creation flow.

### Uni-A: Specific Creation (Perfect Creation)
**Scenario**: User provides actionable intent with a specific time and no conflict exists.

**Extraction rule**: `Uni-A` exact-create should be decided by a lightweight semantic parsing pass, such as a small fast model, not by hardcoded Kotlin heuristics pretending to understand human time semantics.

**Anchor rule**: relative-date wording is not one single bucket.
- `明天` / `tomorrow` / `后天` anchor to the real current date
- `下一天` / `后一天` anchor to the calendar date currently being displayed in the scheduler UI, when that UI context exists
- if UI-relative wording is used but no displayed-date anchor exists, the system must not silently guess a page-relative date
- this closed-set anchor family must be enforced deterministically after semantic extraction; the model may propose a date, but code must normalize or reject any anchor that violates the law

**Time-default rule**: colloquial Chinese hour expressions are not neutral.
- bare `一点` / `1点` default to `13:00`
- explicit early-morning wording like `凌晨一点` anchors to `01:00`
- the system must treat this as semantic interpretation law, not local regex/date-math guesswork

**Anti-fabrication rule**: date-only input is not exact input.
- `明天提醒我打电话` / `tomorrow remind me to go to the airport` must not become `00:00`, current-clock time, lunch time, or any other guessed exact time
- those inputs belong to `Uni-B` as long as the day anchor is real
- lawful day-anchor input with an explicit clock cue such as `后天晚上九点去接李总` is still an exact-create universe, not a vague one
- lawful day-anchor input with an explicit clock cue plus a remaining wake/reminder body such as `明天早上九点喊我起来` is also an exact-create universe and should land as a scheduler task rather than falling through to inspiration-style failure copy
- if a fallback vague-shaped payload still carries a lawful day anchor plus explicit clock evidence, downstream validation must promote it back into exact create instead of persisting a `时间待定` card

```text
[User Input] "Tomorrow at 3pm, team standup"
      |
      v
+-----------------+
| ASR Capture     | --> Transcript captured
+--------+--------+     [Guardrail] Empty/garbled capture must not become a blank task.
         |
         v
+-----------------+
| GUID Allocate   | --> Unified pipeline GUID created for this scheduling thread
+--------+--------+     [Guardrail] This GUID must be attached to Path A output and remain available to Path B as the cross-path bridge.
  [Valve: GUID_ALLOCATED]
         |
         v
+-----------------+
| Intent Gate     | --> Classified as SCHEDULABLE
+--------+--------+
  [Valve: INTENT_CLASSIFIED]
         |
         v
+-----------------+
| Lightweight     | --> Semantic scheduler extraction returns exact title + exact absolute time
| Semantic Parse  |
+--------+--------+     [Guardrail] Relative wording may be spoken by user, but downstream behavior must resolve to an exact schedulable time.
  [Guardrail] Hardcoded regex/heuristic parsing is not the behavioral source of truth for exact understanding.
  [Guardrail] Real-day language like `明天` / `后天` must not be re-anchored to the currently opened calendar page.
  [Guardrail] UI-relative language like `下一天` / `后一天` may use the displayed calendar date as anchor when the UI provides it.
  [Guardrail] Bare colloquial Chinese `一点` must not silently default to `01:00`; only explicit early-morning wording may anchor there.
  [Valve: TASK_EXTRACTED]
         |
         v
+-----------------+
| Conflict Check  | --> PASS
+--------+--------+
  [Valve: CONFLICT_EVALUATED]
         |
         v
+-----------------+
| Persist Task    | --> Task written with non-vague, non-conflict state
+--------+--------+
  [Valve: DB_WRITE_EXECUTED]
         |
         v
+-----------------+
| UI Render       | --> Standard scheduled task card appears on timeline
+-----------------+
  [Valve: UI_RENDERED]
```

Behavioral rule:

- if the lightweight semantic parse yields an exact schedulable task, Path A may commit `Uni-A`
- if that parse does not yield exact time semantics, Path A must not fake `Uni-A` by inventing exact structure from heuristic code
- if a later fallback branch surfaces the same utterance as day-anchored plus explicit-clock, Path A must recover the exact-create outcome rather than freezing the mistake into `Uni-B`
- those cases should fall through to vague, inspiration, or later handling according to the actual extracted meaning
- after `DB_WRITE_EXECUTED`, scheduler timeline refresh must come from canonical storage invalidation and projection recompute; downstream UI must not depend on a parallel manual refresh trigger to surface the committed task

---

### Uni-B: Vague Creation (Purgatory)
**Scenario**: User provides actionable intent but does not provide enough time information.

```text
[User Input] "Schedule team standup"
      |
      v
+-----------------+
| ASR Capture     | --> Transcript captured
+--------+--------+
         |
         v
+-----------------+
| GUID Allocate   | --> Unified pipeline GUID created for this scheduling thread
+--------+--------+
  [Valve: GUID_ALLOCATED]
         |
         v
+-----------------+
| Intent Gate     | --> Classified as SCHEDULABLE
+--------+--------+
  [Valve: INTENT_CLASSIFIED]
         |
         v
+-----------------+
| FastTrack Parse | --> Action extracted, time remains unresolved
+--------+--------+     [Guardrail] The system must not hallucinate "today" or any other fake schedule time.
  [Valve: TASK_EXTRACTED_VAGUE]
         |
         v
+-----------------+
| Conflict Check  | --> BYPASSED
+--------+--------+     [Guardrail] A missing time must not be checked against the board as if it were exact.
         |
         v
+-----------------+
| Persist Task    | --> Task written in explicit vague / needs-time state
+--------+--------+
  [Valve: DB_WRITE_EXECUTED]
         |
         v
+-----------------+
| UI Render       | --> Red-flagged / awaiting-time schedulable card appears outside normal exact-slot rendering
+-----------------+
  [Valve: UI_RENDERED]
```

**Behavioral note**: Lower specs/code may encode this as `isVague`, `needsClarification`, nullable time, omitted time, or a dedicated vague DTO. The encoding may change. The behavior must not.

`Uni-B` only owns unresolved-time input.
If the transcript or preserved time hint still contains an explicit clock cue on a lawful day anchor, the runtime must up-convert that payload back into exact Path A create instead of persisting vague.

---

### Uni-C: Inspiration (Timeless Query)
**Scenario**: User records a thought, aspiration, or note that should not become a scheduled task.

```text
[User Input] "I should really learn to play guitar someday"
      |
      v
+-----------------+
| ASR Capture     | --> Transcript captured
+--------+--------+
         |
         v
+-----------------+
| GUID Allocate   | --> Unified pipeline GUID created for this scheduling thread
+--------+--------+
  [Valve: GUID_ALLOCATED]
         |
         v
+-----------------+
| Intent Gate     | --> Classified as INSPIRATION
+--------+--------+     [Guardrail] This branch must halt schedulable mutation flow.
  [Valve: INTENT_CLASSIFIED]
         |
         v
+-----------------+
| FastTrack Parse | --> Core idea extracted
+--------+--------+
  [Valve: THOUGHT_EXTRACTED]
         |
         v
+-----------------+
| Persist Idea    | --> Written only to inspiration storage
+--------+--------+     [Guardrail] Inspiration landing in the task table is critical data bleed.
  [Valve: DB_WRITE_EXECUTED]
         |
         v
+-----------------+
| UI Render       | --> Distinct inspiration card or note appears outside calendar task lane
+-----------------+
  [Valve: UI_RENDERED]
```

---

### Uni-D: Time Conflict (Colliding Schedules)
**Scenario**: User provides an exact schedulable task that overlaps an existing exact task.

```text
[User Input] "Meet with Zhang at 3pm"
[Existing State] "Dentist at 3pm" already occupies the slot
      |
      v
+-----------------+
| ASR Capture     | --> Transcript captured
+--------+--------+
         |
         v
+-----------------+
| GUID Allocate   | --> Unified pipeline GUID created for this scheduling thread
+--------+--------+
  [Valve: GUID_ALLOCATED]
         |
         v
+-----------------+
| Intent Gate     | --> Classified as SCHEDULABLE
+--------+--------+
  [Valve: INTENT_CLASSIFIED]
         |
         v
+-----------------+
| FastTrack Parse | --> Title extracted + exact time extracted
+--------+--------+
  [Valve: TASK_EXTRACTED]
         |
         v
+-----------------+
| Conflict Check  | --> FAIL / overlap detected
+--------+--------+     [Guardrail] Conflict must not reject the user's intent outright.
  [Valve: CONFLICT_EVALUATED]
         |
         v
+-----------------+
| Persist Task    | --> Task written with conflict-visible state
+--------+--------+
  [Valve: DB_WRITE_EXECUTED]
         |
         v
+-----------------+
| UI Render       | --> Card appears with caution / conflict treatment requiring user attention
+-----------------+
  [Valve: UI_RENDERED]
```

---

## Safety And Operational Branches

These branches are part of Path A behavior even if they are not yet tracked as PU universes downstream.

### Branch-S0: Null Input / Fast-Fail
**Scenario**: Empty audio, silent capture, or unusable transcript.

```text
[User Input] <empty> or silence
      |
      v
+-----------------+
| ASR Capture     | --> Empty / garbled result
+--------+--------+
         |
         v
+-----------------+
| GUID Allocate   | --> Unified pipeline GUID created even for fast-fail traceability
+--------+--------+     [Guardrail] No mutation may occur, but the failed thread should still be traceable.
  [Valve: GUID_ALLOCATED]
         |
         v
+-----------------+
| ASR Evaluate    | --> Empty / garbled confirmed
+--------+--------+
  [Valve: ASR_EMPTY_OR_GARBLED]
         |
         v
+-----------------+
| Fast-Fail       | --> Return explicit no-input / no-op feedback
+--------+--------+     [Guardrail] No task or inspiration write is allowed.
  [Valve: FAST_FAIL_RETURNED]
         |
         v
+-----------------+
| UI Feedback     | --> Banner / toast / visible non-mutation feedback
+-----------------+
```

---

### Branch-S1: Reschedule Happy Path
**Scenario**: User follows up on an existing scheduling thread and the system has enough session context to replace the old task with a new one.

```text
[User Input] "Move the client meeting to 5pm"
      |
      v
+-----------------+
| ASR Capture     | --> Transcript captured
+--------+--------+
         |
         v
+-----------------+
| GUID Allocate   | --> Existing lineage GUID is inherited and advanced into the latest revision for this follow-up
+--------+--------+     [Guardrail] Reschedule may create a new task version, but it must preserve the schedule thread lineage for cross-path mapping and traceability.
  [Valve: GUID_ALLOCATED]
         |
         v
+-----------------+
| Intent Gate     | --> Classified as SCHEDULABLE / RESCHEDULE SHAPE
+--------+--------+
  [Valve: INTENT_CLASSIFIED]
         |
         v
+-----------------+
| Session Memory  | --> Active scheduling context reconstructed
+--------+--------+     [Guardrail] Follow-up may revise more than time. It must not be constrained to a surgical patch model.
  [Valve: TARGET_RESOLVED]
         |
         v
+-----------------+
| FastTrack Parse | --> New intended task state extracted from latest utterance + context
+--------+--------+
  [Valve: TASK_EXTRACTED]
         |
         v
+-----------------+
| Conflict Check  | --> PASS or conflict-visible handling for the NEW task candidate
+--------+--------+
  [Valve: CONFLICT_EVALUATED]
         |
         v
+-----------------+
| Persist New     | --> New task version created first
+--------+--------+
  [Valve: DB_WRITE_EXECUTED]
         |
         v
+-----------------+
| Retire Old      | --> Old task deleted / closed after new task is safely created
+--------+--------+
         |
         v
+-----------------+
| UI Render       | --> User sees the new task as the active result of the follow-up
+-----------------+
  [Valve: UI_RENDERED]
```

**Behavioral note**: Path A follow-up handling should assume the user may revise any meaningful field, not only the missing time field. Replacement semantics reduce liability compared with surgical edit semantics in a voice UX.

---

### Branch-S2: Reschedule No-Match
**Scenario**: User asks to move a task, but no valid target can be found.

```text
[User Input] "Move that meeting to 5pm"
      |
      v
+-----------------+
| ASR Capture     | --> Transcript captured
+--------+--------+
         |
         v
+-----------------+
| GUID Allocate   | --> Existing lineage GUID is inherited for this follow-up
+--------+--------+
  [Valve: GUID_ALLOCATED]
         |
         v
+-----------------+
| Intent Gate     | --> Classified as SCHEDULABLE / RESCHEDULE SHAPE
+--------+--------+
  [Valve: INTENT_CLASSIFIED]
         |
         v
+-----------------+
| FastTrack Parse | --> Reschedule intent extracted
+--------+--------+
         |
         v
+-----------------+
| Target Resolve  | --> No matching task found
+--------+--------+
  [Valve: TARGET_MISSING]
         |
         v
+-----------------+
| Fast-Fail       | --> Explicit "not found / be more specific" feedback
+--------+--------+     [Guardrail] No mutation is allowed.
  [Valve: FAST_FAIL_RETURNED]
         |
         v
+-----------------+
| UI Feedback     | --> Visible failure feedback only
+-----------------+
```

---

### Branch-S3: Reschedule Ambiguous Match
**Scenario**: User asks to move a task, but multiple candidates match.

```text
[User Input] "Move the meeting to 5pm"
      |
      v
+-----------------+
| ASR Capture     | --> Transcript captured
+--------+--------+
         |
         v
+-----------------+
| GUID Allocate   | --> Existing lineage GUID is inherited for this follow-up
+--------+--------+
  [Valve: GUID_ALLOCATED]
         |
         v
+-----------------+
| Intent Gate     | --> Classified as SCHEDULABLE / RESCHEDULE SHAPE
+--------+--------+
  [Valve: INTENT_CLASSIFIED]
         |
         v
+-----------------+
| FastTrack Parse | --> Reschedule intent extracted
+--------+--------+
         |
         v
+-----------------+
| Target Resolve  | --> 2+ candidates match
+--------+--------+
  [Valve: TARGET_AMBIGUOUS]
         |
         v
+-----------------+
| Fast-Fail       | --> Explicit ambiguity feedback
+--------+--------+     [Guardrail] No mutation is allowed.
  [Valve: FAST_FAIL_RETURNED]
         |
         v
+-----------------+
| UI Feedback     | --> User is told to clarify or resolve manually
+-----------------+
```

---

## What Must Sync Next

The lower layers should converge toward this flow in the following order:

1. `docs/specs/scheduler-path-a-execution-prd.md`
   - align vague-task representation
   - align null-input and reschedule safety branches
   - align exact downstream valve vocabulary where useful

2. `docs/plans/PU-tracker.md`
   - update universe naming
   - add missing safety / reschedule branches if they are intended PU coverage

3. Scheduler specs / interface docs
   - reflect exact behavioral branching from this file

4. Code and tests
   - ensure parser, mutation engine, UI, and acceptance flows honor these branch outcomes
