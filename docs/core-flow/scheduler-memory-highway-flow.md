# Core Flow: Scheduler Memory Highway (Path B)

> **Role**: Core Flow
> **Authority**: Behavioral North Star
> **Status**: Ahead of Spec and Code
> **Development Chain**: Core Flow -> Spec -> Code -> PU Test -> Fix Spec -> Fix Code
> **Scope**: Path B asynchronous enrichment for scheduler tasks. Starts from a Path A-created lineage GUID, re-enters the main architecture pipeline, builds RAM from CRM/SSD knowledge, and produces read-only task-relevant memory context.
> **Plugin Layer**: Treat Path B as a plugin-level / System III feature that executes through the repo's plugin/gateway architecture rather than as a hidden side-effect inside Path A.
> **Testing Directive**: Validate one Path B universe or safety branch per run. Do not mix multiple entity-resolution timelines in one PU test.

---

## How To Read This Doc

This document defines **what Path B must do**, not the final implementation class names or DTO details.

- If this document conflicts with lower specs, treat the lower specs as drift candidates first.
- If this document conflicts with code, treat the code as behind or off-contract first.
- If a PU test does not cover a branch defined here, the validation surface is incomplete.

This file is allowed to be ahead of the codebase.

---

## Relationship To Path A

Path A and Path B are intentionally separate.

- **Path A**: execute fast, save the task, show the card
- **Path B**: enrich later, build contextual memory, feed the expanded card

Path B must never block Path A creation, vague creation, conflict visibility, or reschedule replacement.

The two paths are synchronized through lineage GUIDs, not by direct runtime coupling.

Companion north-star doc:
- `docs/core-flow/scheduler-fast-track-flow.md`

---

## Plugin-Level Positioning

Path B should be treated like a plugin-level feature operating on the highway side of the system:

- it is a bounded workflow
- it runs through curated platform/gateway access
- it must not reach directly into raw SQL or internal state machines without the allowed gateway/kernel path
- it should read world knowledge via the same architectural discipline used by the main pipeline

Path B is not “just more scheduler parsing.”
It is a memory-enrichment workflow that reuses the OS mental model:

1. Reuse shared scheduling-envelope candidate
2. Alias / disambiguation
3. SSD graph fetch
4. RAM assembly
5. Derived relevance output

---

## Non-Negotiable Invariants

These are behavioral laws. Lower layers may choose different tables, DTOs, or services, but they must preserve these outcomes.

1. **Path B never blocks Path A**: A scheduler card must exist or visibly fail from Path A regardless of whether Path B succeeds.
2. **Path B starts from lineage identity**: Every Path B run is anchored by the scheduler task's lineage GUID.
3. **Path B reuses shared pre-fork extraction**: If Path A already extracted a first-pass candidate entity and urgency level, Path B should consume that shared seed instead of rediscovering the same first candidate from scratch.
4. **Path B uses the main RAM architecture**: Candidate entities must then flow through router/alias/disambiguation/SSD/RAM logic, not through ad-hoc direct memory peeks.
5. **Expanded card is read-only**: Path B feeds information; it does not turn the card into a CRM editor or workflow console.
6. **Canonical entities are durable**: Path B cleanup must never blindly delete real CRM entities like Person, Event, or Location records just because a task was rescheduled.
7. **Derived context is lineage-scoped**: Path B outputs must be marked by lineage GUID / revision so they can be precisely replaced without damaging unrelated memory.
8. **Latest revision wins**: When reschedule creates a new lineage revision, only the latest revision remains active for Path B display and decision feeding.
9. **Old derived context is superseded, not merged by hand**: Radical reschedule should prefer rerun-and-replace semantics over complicated field-level correction logic.
10. **Disambiguation is reusable architecture**: Name changes like Jack -> Tom must trigger the same disambiguation architecture again, not a shortcut patch.
11. **Path B may enrich around a task; it may not redefine Path A's core execution result**.
12. **Only high-value tasks enter Path B**: Default gate is `L3` and `L4` only.
13. **L1 fire-off tasks do not enter Path B**: One-time fire-off chores do not justify heavy memory enrichment and do not participate in collision logic.

---

## Lineage GUID Model

Path B exists on top of scheduler lineage identity.

### Root Principle

- Path A creates the task thread.
- Path B enriches that thread.
- The thread is tracked by lineage GUID.

### Recommended Shape

Use lineage identity that supports revisioning:

- `root_guid`: stable task-thread identity, identical to the Path A `unified GUID`
- `revision_no`: version of the task thread after reschedule or radical change
- optional combined view such as `lineage_guid = root_guid/revision_no`

### Meaning

- `root_guid` says “this is the same schedule thread”
- `revision_no` says “this is the newest interpretation of that thread”
- `lineage_guid` is the revision-scoped projection used for precise replacement / supersession of derived Path B output

There is one stable identity, not two:

- Path A allocates `unified GUID`
- Path B treats that same value as `root_guid`
- revisioned cleanup and display logic may use `lineage_guid`

### Cleanup Rule

When a new revision becomes active:

- old revision-derived Path B outputs are superseded or deleted
- only the latest revision remains active for display
- canonical CRM entities remain intact unless a separate garbage-collection rule proves they are orphaned

---

## What Path B Writes

Path B should primarily write **derived, lineage-scoped associations**, not indiscriminately mutate canonical memory.

Examples of safe Path B outputs:

- task-to-person links
- task-to-location links
- task-to-event links
- relevance summaries
- conflict hints
- enriched “why this matters” context for the expanded card

These outputs must carry lineage provenance so SQL can precisely supersede the old revision without touching unrelated memory.

Canonical entities such as `Tom`, `Jack`, or `Tiffany` may remain durable in CRM memory independent of any one scheduler thread.

---

## Canonical Valves

These valve names are the behavioral checkpoints for downstream specs and tests.
The exact telemetry names may differ today, but lower layers should converge toward this model.

- `GUID_RECEIVED`
- `TASK_LEVEL_RECEIVED`
- `ENTITY_CANDIDATE_RECEIVED`
- `PATH_B_GATE_PASSED`
- `PATH_B_GATE_REJECTED`
- `PATH_B_TRIGGERED`
- `ENTITY_CANDIDATES_EXTRACTED`
- `ALIAS_LOOKUP_EXECUTED`
- `DISAMBIGUATION_REQUIRED`
- `ENTITY_IDS_RESOLVED`
- `SSD_GRAPH_FETCHED`
- `LIVING_RAM_ASSEMBLED`
- `RELEVANCE_CONTEXT_DERIVED`
- `OLD_REVISION_SUPERSEDED`
- `NEW_REVISION_CONTEXT_WRITTEN`
- `EXPANDED_CARD_PROJECTION_READY`
- `PATH_B_NOOP`

---

## Master Routing Flow

This is the top-level routing model for Path B.

```text
                    +----------------------+
                    | Path A Task Exists   |
                    | + lineage GUID       |
                    | + task level         |
                    | + first candidate    |
                    +----------+-----------+
                               |
                               v
                    +----------------------+
                    | Path B Value Gate    |
                    | (L3 / L4 only)       |
                    +----------+-----------+
                               |
                  +------------+------------+
                  |                         |
                  v                         v
            [Gate Reject / No-Op]    [Gate Pass / Trigger]
                                             |
                                             v
                    +----------------------+
                    | Path B Triggered     |
                    +----------+-----------+
                               |
                               v
                    +----------------------+
                    | Candidate Seed Load  |
                    +----------+-----------+
                               |
                +--------------+---------------+
                |                              |
                v                              v
        +---------------+              +-------------------+
        | No Candidates |              | Seed Candidates   |
        +-------+-------+              +---------+---------+
                |                                |
                v                                v
         [Path B No-Op]                 +----------------------+
                                        | Alias / Resolution   |
                                        +----------+-----------+
                                                   |
                        +--------------------------+--------------------------+
                        |                          |                          |
                        v                          v                          v
                +---------------+         +----------------+         +----------------+
                | Exact Resolve |         | Ambiguous Name |         | Missing Resolve |
                +-------+-------+         +--------+-------+         +--------+-------+
                        |                          |                          |
                        v                          v                          v
                 [SSD -> RAM]               [Disambiguation]            [Partial / No-Op]
                        |                          |                          |
                        +--------------+-----------+--------------------------+
                                       |
                                       v
                          +---------------------------+
                          | Derived Relevance Output  |
                          +-------------+-------------+
                                        |
                                        v
                          +---------------------------+
                          | Expanded Card Projection  |
                          +---------------------------+
```

---

## Path B And The Main RAM Pipeline

Path B should learn from the same architecture that builds RAM for the main system.

### Behavioral Rule

When Path B sees candidate entities in the context of a scheduler task, it should re-enter the main memory-building discipline:

1. receive lineage GUID, urgency level, and first-pass candidate from the shared scheduling envelope
2. apply the Path B value gate (`L3` / `L4`)
3. run alias lookup on the candidate set
4. trigger disambiguation when needed
5. fetch relevant SSD graph
6. assemble RAM context
7. derive relevance for the scheduler card

This prevents Path B from turning into a separate ad-hoc CRM query layer.

### Task Level Semantics And Hard Gate

Task level is a hard behavioral gate, not a confidence score.

- `L1`: one-time fire-off chore
- `L2`: normal scheduled task
- `L3`: relationship-relevant task
- `L4`: high-stakes business task

Path B entry rule:

- `L1` does not enter Path B
- `L2` does not enter Path B
- `L3` enters Path B
- `L4` enters Path B

Interpretation notes:

- `L1` may still look urgent in kanban, but urgency styling does not make it memory-worthy.
- `L2` is a real schedule task and may still collide in Path A, but it does not justify Path B enrichment by default.
- `L3` and `L4` are memory-worthy tasks and should be enriched through Path B.

There is no separate “candidate quality” gate.
Once a task passes the `L3` / `L4` gate, Path B should run the same alias / disambiguation / SSD / RAM pipeline whether the first-pass candidate looks clean or messy.

If no valid candidate entity is produced, Path B may still validly no-op even for `L3` or `L4`.
This is acceptable for impersonal but important tasks, including self-only tasks such as catching a flight.
The absence of a candidate does not require a clarification loop in the current Core Flow.
Future suggestion/habit enrichment can be treated as follow-up tech debt rather than current required behavior.

---

## Core Universes

### Uni-B1: Exact Enrichment (Single Clear Entity)
**Scenario**: Path A task references a person or context that resolves cleanly.

```text
[Path A State] Task exists with lineage GUID 1234/1
[User Meaning] "Meet Tom tomorrow at 3pm"
      |
      v
+----------------------+
| GUID Received        | --> Path B receives lineage GUID 1234/1
+----------+-----------+
  [Valve: GUID_RECEIVED]
           |
           v
+----------------------+
| Level/Candidate In   | --> Receives level L3/L4 + candidate entity "Tom" from shared scheduling envelope
+----------+-----------+
  [Valve: TASK_LEVEL_RECEIVED]
  [Valve: ENTITY_CANDIDATE_RECEIVED]
           |
           v
+----------------------+
| Path B Gate          | --> PASS
+----------+-----------+
  [Valve: PATH_B_GATE_PASSED]
           |
           v
+----------------------+
| Alias Lookup         | --> Exactly one Tom resolved
+----------+-----------+
  [Valve: ENTITY_IDS_RESOLVED]
           |
           v
+----------------------+
| SSD Graph Fetch      | --> Relevant Tom-centered memory retrieved
+----------+-----------+
  [Valve: SSD_GRAPH_FETCHED]
           |
           v
+----------------------+
| RAM Assembly         | --> Distilled task-relevant memory built
+----------+-----------+
  [Valve: LIVING_RAM_ASSEMBLED]
           |
           v
+----------------------+
| Derived Relevance    | --> Relevant people/events/locations summarized
+----------+-----------+
  [Valve: RELEVANCE_CONTEXT_DERIVED]
           |
           v
+----------------------+
| Projection Ready     | --> Expanded card can show read-only context
+----------------------+
  [Valve: EXPANDED_CARD_PROJECTION_READY]
```

---

### Uni-B2: Ambiguous Entity (Disambiguation Required)
**Scenario**: Path A task introduces a candidate name that maps to multiple memory entities.

```text
[Path A State] Task exists with lineage GUID 1234/1
[User Meaning] "Meet Jack tomorrow"
      |
      v
+----------------------+
| GUID Received        | --> Path B receives lineage GUID 1234/1
+----------+-----------+
           |
           v
+----------------------+
| Level/Candidate In   | --> Receives level L3/L4 + candidate entity "Jack" from shared scheduling envelope
+----------+-----------+
  [Valve: TASK_LEVEL_RECEIVED]
  [Valve: ENTITY_CANDIDATE_RECEIVED]
           |
           v
+----------------------+
| Path B Gate          | --> PASS
+----------+-----------+
  [Valve: PATH_B_GATE_PASSED]
           |
           v
+----------------------+
| Alias Lookup         | --> Multiple Jacks found
+----------+-----------+
  [Valve: DISAMBIGUATION_REQUIRED]
           |
           v
+----------------------+
| Disambiguation Path  | --> Main architecture disambiguation runs
+----------+-----------+
           |
           v
+----------------------+
| SSD -> RAM           | --> Winning Jack context fetched and assembled
+----------+-----------+
  [Valve: LIVING_RAM_ASSEMBLED]
           |
           v
+----------------------+
| Projection Ready     | --> Expanded card shows resolved read-only context
+----------------------+
  [Valve: EXPANDED_CARD_PROJECTION_READY]
```

**Behavioral note**: Path B may need full disambiguation architecture here, but it still must not block Path A card creation.

---

### Uni-B3: Radical Reschedule (Entity Center Changes)
**Scenario**: The task was previously enriched around one entity, but reschedule radically changes the semantic center.

```text
[Old Active State] lineage GUID 1234/1 centered around Jack
[New Path A State] reschedule creates latest revision 1234/2 centered around Tom
      |
      v
+----------------------+
| New Revision Seen    | --> Path B receives 1234/2 as latest revision
+----------+-----------+
  [Valve: GUID_RECEIVED]
           |
           v
+----------------------+
| Level/Candidate In   | --> Receives latest level + latest first-pass candidate "Tom"
+----------+-----------+
  [Valve: TASK_LEVEL_RECEIVED]
  [Valve: ENTITY_CANDIDATE_RECEIVED]
           |
           v
+----------------------+
| Path B Gate          | --> PASS
+----------+-----------+
  [Valve: PATH_B_GATE_PASSED]
           |
           v
+----------------------+
| Supersede Old Links  | --> Old Jack-derived revision outputs for 1234/1 are marked superseded / removed
+----------+-----------+
  [Valve: OLD_REVISION_SUPERSEDED]
           |
           v
+----------------------+
| Candidate Seed Refresh | --> Shared seed "Tom" is accepted and expanded into the latest candidate set
+----------+-----------+
  [Valve: ENTITY_CANDIDATES_EXTRACTED]
           |
           v
+----------------------+
| Alias / Disambig     | --> Main architecture reruns on Tom
+----------+-----------+
           |
           v
+----------------------+
| SSD -> RAM           | --> Tom-centered memory graph loaded
+----------+-----------+
  [Valve: LIVING_RAM_ASSEMBLED]
           |
           v
+----------------------+
| New Derived Output   | --> New Tom-scoped relevance and links written for 1234/2
+----------+-----------+
  [Valve: NEW_REVISION_CONTEXT_WRITTEN]
           |
           v
+----------------------+
| Projection Ready     | --> Expanded card now reflects Tom-centered context only
+----------------------+
  [Valve: EXPANDED_CARD_PROJECTION_READY]
```

**Guardrail**:

- old GUID-scoped derived outputs may be superseded
- canonical Jack entity must not be blindly deleted
- canonical Tom entity may already exist or may be newly introduced

---

### Uni-B4: No Candidate Or Low-Value Task
**Scenario**: Path A task exists, but either the task level is below the hard gate or no candidate seed is present.

```text
[Path A State] Task exists with lineage GUID 1234/1
[User Meaning] "Buy milk tomorrow"
      |
      v
+----------------------+
| GUID Received        | --> Path B receives 1234/1
+----------+-----------+
           |
           v
+----------------------+
| Level/Candidate In   | --> Receives task level or detects that no candidate seed is present
+----------+-----------+
  [Valve: TASK_LEVEL_RECEIVED]
  [Valve: ENTITY_CANDIDATE_RECEIVED]
           |
           v
+----------------------+
| Path B Gate / No-Op  | --> Reject because level is below gate or no candidate seed exists
+----------------------+
  [Valve: PATH_B_GATE_REJECTED]
  [Valve: PATH_B_NOOP]
```

**Behavioral note**: Lack of enrichment is a valid Path B outcome. Path B does not need to manufacture context just to populate the expanded card. `L1` fire-off chores should usually land here, `L2` tasks should also no-op by default, and even `L3` / `L4` may no-op when no valid candidate entity exists.

---

## Expanded Card Window Rule

The expanded scheduler card is the display surface for Path B output.

It may show:

- relevant people
- related events
- relevant locations
- conflict hints
- memory snippets useful for decision-making

It must remain:

- read-only
- informational
- lightweight
- subordinate to the scheduler task thread

It must not become:

- a CRM edit console
- a merge-resolution workbench
- a direct entity mutation surface

---

## What Must Sync Next

The lower layers should converge toward this flow in the following order:

1. Path B plugin/concept/spec docs under `docs/cerb-plugin/**`
   - formalize plugin boundary
   - formalize gateway usage
   - formalize lineage-scoped outputs

2. Scheduler-related specs and interfaces
   - define how Path B reads from Path A lineage identity
   - define expanded-card projection contract

3. Tracker / interface map
   - register Path B ownership and plugin-level topology

4. Code and tests
   - ensure router, alias/disambiguation, RAM assembly, lineage cleanup, and projection logic honor this flow
   - ensure shared candidate extraction feeds both paths
   - ensure only `L3` / `L4` tasks enter Path B by default
