# Core Flow: System Plugin Gateway

> **Role**: Core Flow
> **Authority**: Behavioral North Star
> **Status**: Ahead of Spec and Code
> **Development Chain**: Architecture -> Core Flow -> Spec -> Code -> PU Test -> Fix Spec -> Fix Code
> **Scope**: The architecture-level System III plugin lane from plugin recommendation or dispatch through bounded sub-pipeline execution, result handoff, and return to the main orchestration loop.
> **Parent Constitution**: `docs/specs/Architecture.md`
> **Testing Directive**: Validate one plugin universe or one plugin failure branch per run. Do not mix multiple plugin lifecycles in one PU test.

---

## How To Read This Doc

This document defines **what the plugin lane must do**.

- If this document conflicts with lower specs, treat the lower specs as drift candidates first.
- If this document conflicts with code, treat the code as transitional first unless the architecture itself is obsolete.
- Feature plugin flows may specialize this behavior, but they must remain within these system boundaries.

This file is allowed to be ahead of the codebase.

---

## Downstream Sync Targets

| Layer | Responsibility | Must Eventually Reflect This Core Flow |
|------|----------------|-----------------------------------------|
| **Feature Core Flow** | Plugin-specific behavior | Scheduler Memory Highway, future PDF / email / export flows |
| **Spec** | How plugin execution is encoded | plugin gateway specs, plugin interface docs, plugin-specific Cerb docs |
| **Code** | Delivered behavior | tool registry, plugin gateway, plugin implementations |
| **PU Test** | Behavioral validation | plugin dispatch tests, plugin failure tests, valve audits |

---

## Non-Negotiable Invariants

1. **The main orchestrator owns the outer loop**: User dialogue, recommendation, dispatch approval, return narration, and next-step routing remain in the primary workflow.
2. **Plugins are bounded sub-pipelines**: A plugin is not a hidden extension of the core chat loop; it is a delegated workflow entered temporarily and exited explicitly.
3. **Dispatch is explicit**: Plugin work begins only after recommendation, dispatch, or direct approved route.
4. **Entry routing stays simple and semantic**: The outer loop should select a stable plugin entry ID plus bounded parameters, not expose deep internal capability choreography to the LLM.
5. **Internal plugin work may be richer than entry routing**: Once entered, a plugin may run its own local planning and capability sequence, but that sub-pipeline remains inside the plugin boundary.
6. **Plugins respect OS ownership**: A plugin may not bypass the OS model and mutate arbitrary state machines directly.
7. **Plugins are built against capability APIs**: Plugin development should target approved SDK capabilities created from real use cases, not ad-hoc direct module wiring.
8. **Plugins use approved boundaries**: External calls, storage access, and world reads must go through approved gateways or owned modules.
9. **Plugin-caused writes re-enter through the mutation lane**: If plugin work needs to change SSD truth, it must hand off into the typed mutation / central writer path rather than writing directly.
10. **Plugins do not own UI state directly**: They yield results back to the OS / presentation layer rather than rendering themselves.
11. **Completion returns to the outer loop**: After a plugin yields success, failure, or no-op, control returns to the main orchestration workflow for the next round of routing or recommendation.
12. **Plugins stay observable**: Dispatch, internal routing, capability calls, transfer points, and yield/failure should be traceable.
13. **Plugin failure must fail safely**: A plugin may fail or no-op without corrupting the main conversational state.
14. **Plugin outputs must re-enter through approved result contracts**: The OS should receive typed or explicitly bounded result payloads rather than shapeless side effects.
15. **Presentation remains downstream**: Plugin results may influence UI state, but plugins do not author domain truth or presentation truth directly.

---

## Canonical Valves

- `PLUGIN_DISPATCH_RECEIVED`
- `PLUGIN_INTERNAL_ROUTING`
- `PLUGIN_CAPABILITY_CALL`
- `PLUGIN_EXTERNAL_CALL`
- `PLUGIN_YIELDED_TO_OS`
- `UI_STATE_EMITTED`

---

## Outer Loop Rule

The plugin lane is not the whole product workflow.

The canonical architecture is:

1. the main orchestrator talks with the user
2. the main orchestrator routes the current turn
3. the main orchestrator may recommend or dispatch a plugin
4. the plugin runs a bounded sub-pipeline through approved capability APIs
5. the plugin yields a bounded result back to the OS
6. the main orchestrator resumes the next conversational round

So the plugin lane is best understood as an **excursion under the outer loop**, not as a second top-level brain.

---

## Master Routing Flow

```text
                 +----------------------+
                 | User Turn /          |
                 | Main Orchestrator    |
                 +----------+-----------+
                            |
                            v
                 +----------------------+
                 | Recommendation /     |
                 | Direct Dispatch      |
                 +----------+-----------+
                            |
                            v
                 +----------------------+
                 | Tool Registry /      |
                 | Plugin Gateway       |
                 +----------+-----------+
                            |
              +-------------+-------------+
              |                           |
              v                           v
        [Reject / No-Op]           [Dispatch Accepted]
              |                                 |
              |                                 v
              |                    +----------------------+
              |                    | Plugin Internal      |
              |                    | Routing / Planning   |
              |                    +----------+-----------+
              |                               |
              |                               v
              |                    +----------------------+
              |                    | Capability SDK /     |
              |                    | Approved APIs        |
              |                    +----------+-----------+
              |                               |
              |            +------------------+------------------+
              |            |                                     |
              |            v                                     v
              |   [Local Capability]                  [External / Heavy Capability]
              |            |                                     |
              |            +------------------+------------------+
              |                               |
              |                               v
              |                    +----------------------+
              |                    | Plugin Result /      |
              |                    | Failure / Progress   |
              |                    +----------+-----------+
              |                               |
              |             +-----------------+-----------------+
              |             |                                   |
              |             v                                   v
              |    [Read / UI Result]             [Typed Mutation Handoff]
              |             |                                   |
              |             v                                   v
              |    +----------------------+         +----------------------+
              |    | Yield Back To OS     |         | System Typed         |
              |    |                      |         | Mutation Lane        |
              |    +----------+-----------+         +----------------------+
              |               |
              +---------------+-------------------+
                                              |
                                              v
                                 +----------------------+
                                 | UI State Emitted     |
                                 +----------+-----------+
                                            |
                                            v
                                 +----------------------+
                                 | Return To Main       |
                                 | Orchestrator Loop    |
                                 +----------------------+
```

---

## Core Universes

### Uni-P1: Recommended Plugin Then User Commit
**Scenario**: System II recommends a plugin, the OS dispatches it through the plugin gateway, and the result returns to the outer loop.

```text
[Brain Output] Plugin recommendation present
      |
      v
+----------------------+
| Recommendation Seen  | --> OS receives bounded recommendation
+----------+-----------+
           |
           v
+----------------------+
| Plugin Dispatch      | --> Tool registry accepts execution request
+----------+-----------+
  [Valve: PLUGIN_DISPATCH_RECEIVED]
           |
           v
+----------------------+
| Internal Routing     | --> Plugin maps request into its own bounded workflow
+----------+-----------+
  [Valve: PLUGIN_INTERNAL_ROUTING]
           |
           v
+----------------------+
| Capability API Call  | --> Plugin uses approved SDK capability instead of direct cross-module wiring
+----------+-----------+
  [Valve: PLUGIN_CAPABILITY_CALL]
           |
           v
+----------------------+
| Plugin Result        | --> Result is produced without bypassing OS ownership
+----------+-----------+
  [Valve: PLUGIN_YIELDED_TO_OS]
           |
           v
+----------------------+
| UI State Emitted     | --> OS presents the result
+----------+-----------+
  [Valve: UI_STATE_EMITTED]
           |
           v
+----------------------+
| Outer Loop Resumes   | --> Main orchestrator may narrate next step, recommend another tool, or continue chat
+----------------------+
```

---

### Uni-P2: Voice Auto-Commit Plugin
**Scenario**: An approved runtime path auto-commits a plugin dispatch without an extra confirmation step, then returns control safely to the main workflow.

```text
[Input Context] Approved voice or hardware lane
      |
      v
+----------------------+
| Plugin Dispatch      | --> Dispatch accepted automatically
+----------+-----------+
  [Valve: PLUGIN_DISPATCH_RECEIVED]
           |
           v
+----------------------+
| Internal Routing     | --> Plugin executes in bounded background lane
+----------+-----------+
  [Valve: PLUGIN_INTERNAL_ROUTING]
           |
           v
+----------------------+
| Capability API Call  | --> Approved SDK capability executes under the plugin gateway
+----------+-----------+
  [Valve: PLUGIN_CAPABILITY_CALL]
           |
           v
+----------------------+
| Yield To OS          | --> Plugin result returns safely to OS state
+----------+-----------+
  [Valve: PLUGIN_YIELDED_TO_OS]
           |
           v
+----------------------+
| Outer Loop Resumes   | --> Main orchestrator remains the owner of the next conversational round
+----------------------+
```

---

### Uni-P3: Plugin Failure Safe Halt
**Scenario**: The plugin accepts dispatch but fails during internal or external execution, then returns a bounded failure to the outer loop.

```text
[Plugin Dispatch] Accepted
      |
      v
+----------------------+
| Internal Routing     | --> Plugin begins bounded work
+----------+-----------+
  [Valve: PLUGIN_INTERNAL_ROUTING]
           |
           v
+----------------------+
| Capability API Call  | --> SDK capability starts and reaches external or heavy work
+----------+-----------+
  [Valve: PLUGIN_CAPABILITY_CALL]
           |
           v
+----------------------+
| External / Internal  | --> Failure occurs
| Call Fails           |
+----------+-----------+
  [Valve: PLUGIN_EXTERNAL_CALL]
           |
           v
+----------------------+
| Safe Yield / Error   | --> OS receives bounded failure, not silent corruption
+----------+-----------+
  [Valve: PLUGIN_YIELDED_TO_OS]
           |
           v
+----------------------+
| UI State Emitted     | --> User sees safe failure state or recoverable message
+----------+-----------+
  [Valve: UI_STATE_EMITTED]
           |
           v
+----------------------+
| Outer Loop Resumes   | --> Failure stays local; next routing turn remains possible
+----------------------+
```

---

## What Must Sync Next

1. plugin gateway specs
   - dispatch contract
   - capability SDK contract
   - progress / result contract
   - external boundary rules

2. plugin-specific core flows
   - scheduler memory highway
   - future PDF / email / export flows

3. code and tests
   - ensure dispatch is explicit and observable
   - ensure plugins use approved capability APIs instead of direct module wiring
   - ensure plugin-caused writes hand off into the typed mutation lane
   - ensure plugin failure yields bounded OS-visible state
   - ensure plugins do not bypass owned architectural boundaries
