# SIM Connectivity Spec

> **Scope**: Badge BLE/Wi-Fi connection management as a decoupled support module inside the standalone SIM prototype
> **Status**: SPEC_ONLY
> **Behavioral Authority Above This Doc**:
> - `docs/to-cerb/sim-standalone-prototype/concept.md`
> - `docs/core-flow/sim-shell-routing-flow.md`
> - `docs/specs/connectivity-spec.md`
> - `docs/cerb/connectivity-bridge/spec.md`
> - `docs/cerb/connectivity-bridge/interface.md`
> **Audit Evidence**: `docs/reports/20260319-sim-standalone-code-audit.md`

---

## 1. Purpose

`SIM Connectivity` defines how the standalone SIM prototype reuses the mature connectivity module without coupling it back into the smart-agent runtime.

This shard exists so SIM can:

- expose a shell-level connectivity entry/icon
- let users manage badge BLE/Wi-Fi connection state
- keep connectivity behavior as isolated infrastructure/support logic

This shard does not turn connectivity into a third smart feature lane.

---

## 2. Included Scope

- shell-level entry into badge connection management
- reuse of existing connectivity bridge/service contracts
- setup, reconnect, disconnect, and adjacent connection-management behavior already defined by connectivity specs
- return path from connectivity management back into normal SIM shell use

---

## 3. Excluded Scope

- redefining BLE/Wi-Fi protocol behavior locally in SIM
- coupling scheduler execution to badge connectivity by default
- coupling audio discussion chat to smart-agent runtime through connectivity
- treating connectivity as a hidden orchestration lane

---

## 4. Ownership

`SIM Connectivity` owns:

- the SIM-facing shell entry contract
- the isolation rule between connectivity and the two main feature lanes

`SIM Connectivity` reuses:

- `ConnectivityBridge`
- `ConnectivityService`
- existing connectivity UI flows where they can be mounted without smart-agent contamination

`SIM Connectivity` must not assume:

- direct imports from legacy connectivity internals are acceptable in new SIM domain code
- connectivity state should reshape SIM scheduler/audio business rules by default

---

## 5. Hard Migration Rule

Connectivity is a strong hard-migration candidate because it is already mature and decoupled.

SIM may:

- reuse existing connectivity bridge and service contracts
- reuse connectivity modal/setup surfaces with shell-level adaptation
- preserve current BLE/Wi-Fi behavior as long as SIM routing stays isolated

SIM may not:

- rebuild connectivity logic from scratch without evidence-based need
- use connectivity reuse as permission to pull smart-app shell/runtime dependencies into SIM

---

## 6. Shell Entry Rule

The SIM shell should expose a connectivity entry/icon as a support action.

Expectations:

- entry is reachable from normal shell use
- user can inspect or manage connection state
- closing or completing the flow returns the user to SIM shell operation

This entry does not change the rule that SIM has only two main product lanes.

---

## 7. Human Reality

### Organic UX

Users expect badge connection management to exist because the product still talks to hardware.
They do not expect connection management to behave like a smart-agent feature.

### Data Reality

Connection state is already defined by the existing connectivity contracts.
SIM should inherit those contracts rather than invent a second meaning of "connected."

### Failure Gravity

The worst failure is letting connectivity reuse drag smart runtime assumptions into SIM.

---

## 8. Wave Plan

| Wave | Focus | Status | Deliverable |
|------|-------|--------|-------------|
| 1 | SIM connectivity contract freeze | PLANNED | SIM connectivity spec/interface |
| 2 | Shell entry and reuse decision | PLANNED | SIM entry wiring plan |
| 3 | Isolation validation | PLANNED | proof connectivity stays decoupled |

---

## 9. Done-When Definition

SIM connectivity is ready only when:

- a SIM shell entry/icon exists for badge connection management
- existing connectivity contracts remain the behavioral source
- connectivity use does not contaminate scheduler/audio/chat runtime behavior
