# Core Flow: System Plugin Gateway

> **Role**: Core Flow
> **Authority**: Behavioral North Star
> **Status**: Ahead of Spec and Code
> **Development Chain**: Architecture -> Core Flow -> Spec -> Code -> PU Test -> Fix Spec -> Fix Code
> **Scope**: The architecture-level System III plugin lane from plugin recommendation or dispatch through bounded execution, progress signaling, and OS-visible result.
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

1. **Plugins are bounded workflows**: A plugin is not a hidden extension of the core chat loop.
2. **Dispatch is explicit**: Plugin work begins only after recommendation, dispatch, or direct approved route.
3. **Plugins respect OS ownership**: A plugin may not bypass the OS model and mutate arbitrary state machines directly.
4. **Plugins are built against capability APIs**: Plugin development should target approved SDK capabilities created from real use cases, not ad-hoc direct module wiring.
5. **Plugins use approved boundaries**: External calls, storage access, and world reads must go through approved gateways or owned modules.
6. **Plugin-caused writes re-enter through the mutation lane**: If plugin work needs to change SSD truth, it must hand off into the typed mutation / central writer path rather than writing directly.
7. **Plugins do not own UI state directly**: They yield results back to the OS / presentation layer rather than rendering themselves.
8. **Plugins stay observable**: Dispatch, internal routing, capability calls, and yield/failure should be traceable.
9. **Plugin failure must fail safely**: A plugin may fail or no-op without corrupting the main conversational state.
10. **Plugin outputs must re-enter through approved result contracts**: The OS should receive typed or explicitly bounded result payloads rather than shapeless side effects.
11. **Presentation remains downstream**: Plugin results may influence UI state, but plugins do not author domain truth or presentation truth directly.

---

## Canonical Valves

- `PLUGIN_DISPATCH_RECEIVED`
- `PLUGIN_INTERNAL_ROUTING`
- `PLUGIN_CAPABILITY_CALL`
- `PLUGIN_EXTERNAL_CALL`
- `PLUGIN_YIELDED_TO_OS`
- `UI_STATE_EMITTED`

---

## Master Routing Flow

```text
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
                                              |
                                              v
                                 +----------------------+
                                 | Plugin Internal      |
                                 | Routing / Planning   |
                                 +----------+-----------+
                                            |
                                            v
                                 +----------------------+
                                 | Capability SDK /     |
                                 | Approved APIs        |
                                 +----------+-----------+
                                            |
                         +------------------+------------------+
                         |                                     |
                         v                                     v
                [Local Capability]                  [External / Heavy Capability]
                         |                                     |
                         +------------------+------------------+
                                            |
                                            v
                                 +----------------------+
                                 | Plugin Result /      |
                                 | Failure / Progress   |
                                 +----------+-----------+
                                            |
                          +-----------------+-----------------+
                          |                                   |
                          v                                   v
                 [Read / UI Result]             [Typed Mutation Handoff]
                          |                                   |
                          v                                   v
                 +----------------------+         +----------------------+
                 | Yield Back To OS     |         | System Typed         |
                 |                      |         | Mutation Lane        |
                 +----------+-----------+         +----------------------+
                            |
                            v
                 +----------------------+
                 | UI State Emitted     |
                 +----------------------+
```

---

## Core Universes

### Uni-P1: Recommended Plugin Then User Commit
**Scenario**: System II recommends a plugin, then the OS dispatches it through the plugin gateway.

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
+----------------------+
  [Valve: UI_STATE_EMITTED]
```

---

### Uni-P2: Voice Auto-Commit Plugin
**Scenario**: An approved runtime path auto-commits a plugin dispatch without an extra confirmation step.

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
+----------------------+
  [Valve: PLUGIN_YIELDED_TO_OS]
```

---

### Uni-P3: Plugin Failure Safe Halt
**Scenario**: The plugin accepts dispatch but fails during internal or external execution.

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
+----------------------+
  [Valve: UI_STATE_EMITTED]
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
