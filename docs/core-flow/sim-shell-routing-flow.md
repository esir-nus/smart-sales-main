# Core Flow: SIM Shell Routing

> **Role**: Core Flow
> **Authority**: Behavioral North Star
> **Status**: Ahead of Spec and Code
> **Development Chain**: Core Flow -> Spec -> Code -> PU Test -> Fix Spec -> Fix Code
> **Scope**: Standalone SIM app entry, shell routing, and top-level surface transitions between discussion chat, scheduler drawer, audio drawer, history, connectivity entry, and settings/new-session actions.
> **Testing Directive**: Validate one shell route or one safety branch per run. Do not mix multiple unrelated shell transitions in a single PU run.

---

## How To Read This Doc

This document defines **what the SIM shell must do**, not the final class names or DI layout.

- If this document conflicts with lower specs, treat the lower specs as drift candidates first.
- If this document conflicts with code, treat the code as behind or off-contract first.
- If a PU test does not cover a branch defined here, the validation surface is incomplete.

This file is allowed to be ahead of the codebase.

---

## Downstream Sync Targets

| Layer | Responsibility | Must Eventually Reflect This Core Flow |
|------|----------------|-----------------------------------------|
| **Spec** | How the behavior is encoded | `docs/cerb/sim-shell/spec.md`, `docs/cerb/sim-shell/interface.md`, `docs/cerb/sim-connectivity/spec.md` |
| **Code** | Delivered behavior | `SimMainActivity`, `SimShell`, prototype-only shell/viewmodel/DI composition |
| **PU Test** | Behavioral validation | future SIM shell routing tests, drawer transition tests, isolation tests |

---

## Non-Negotiable Invariants

1. **SIM boots into a standalone shell**: app launch must not silently boot the current smart shell.
2. **Only SIM-approved surfaces are reachable by default**: the standalone shell exposes only SIM-approved surfaces as normal behavior.
3. **Only one drawer is open at a time**: scheduler and audio drawers remain atomically exclusive.
4. **Audio re-entry is drawer-based**: selecting audio from inside chat must reopen the audio drawer instead of launching Android file management.
5. **Visual continuity does not justify runtime reuse**: the shell may look like Prism while still using a different composition root.
6. **Ordinary shell practices may survive, but smart runtime meaning must not**: history, new page/session, connectivity entry, and settings are allowed when simplified for SIM; mascot overlay, debug HUD, plugin task board, and smart-only right drawers are not required SIM behavior.
7. **Shell state must not require the smart app's orchestration model**: audio-grounded chat may track selected audio and local SIM history, but SIM must not depend on the smart app's broader memory architecture by default.

---

## Shell Surface Model

SIM has three primary surfaces and several support surfaces.

Primary surfaces:

- `Discussion Chat Surface`
- `Scheduler Drawer`
- `Audio Drawer`

Support surfaces/actions:

- `History Drawer`
- `New Page / New Session`
- `Connectivity Entry`
- `Settings Entry`

Permitted shell transitions:

- app launch -> discussion chat
- discussion chat -> scheduler drawer
- discussion chat -> audio drawer
- discussion chat -> history drawer
- discussion chat -> new page/session
- discussion chat -> connectivity management
- discussion chat -> settings
- audio drawer -> simple chat via `Ask AI`
- simple chat -> audio drawer for re-selection

Forbidden shell assumptions:

- mascot is part of the shell heartbeat
- plugin tasks or smart tips define the shell behavior
- history implies smart memory or smart-agent orchestration

---

## Canonical Valves

These valve names are the behavioral checkpoints for downstream specs and tests.
The exact telemetry names in code may differ today, but they should converge toward this model.

- `SIM_ENTRY_STARTED`
- `SIM_SHELL_MOUNTED`
- `CHAT_VISIBLE`
- `SCHEDULER_DRAWER_OPENED`
- `SCHEDULER_DRAWER_CLOSED`
- `AUDIO_DRAWER_OPENED`
- `AUDIO_DRAWER_CLOSED`
- `HISTORY_DRAWER_OPENED`
- `NEW_CHAT_REQUESTED`
- `CONNECTIVITY_ENTRY_OPENED`
- `SETTINGS_OPENED`
- `ASK_AI_REQUESTED`
- `AUDIO_RESELECT_REQUESTED`
- `AUDIO_RESELECT_RETURNED`
- `SMART_SURFACE_BLOCKED`

---

## Master Routing Flow

This is the top-level routing model for the SIM shell.

```text
                    +----------------------+
                    |  App Launch (SIM)    |
                    +----------+-----------+
                               |
                               v
                    +----------------------+
                    | Standalone SIM Entry |
                    +----------+-----------+
                               |
                               v
                    +----------------------+
                    |   SIM Shell Mounted  |
                    +----------+-----------+
                               |
                               v
                    +----------------------+
                    | Discussion Chat      |
                    | Visible              |
                    +---+----+-----+-----+-+
                        |    |     |     |
                        v    v     v     v
              +-----------+  |  +------+ +----------------+
              | Scheduler |  |  |Audio | | History / New |
              | Drawer    |  |  |Drawer| | / Connectivity|
              +-----+-----+  |  +--+---+ +--------+-------+
                    |        |     |              |
                    v        |     v              v
             [Close Drawer]  | [Ask AI]   [Return to Chat]
                    |        |     |
                    v        |     v
          +--------------------+   +--------------------+
          | Discussion Chat    |   | Discussion Chat    |
          | Visible            |   | Visible            |
          +--------------------+   +---------+----------+
                                             |
                                             v
                                  +----------------------+
                                  | Reselect Audio       |
                                  +----------+-----------+
                                             |
                                             v
                                  [Reopen Audio Drawer]
```

---

## Safety Branches

### Branch-S1: Smart Surface Blocked

If SIM code attempts to route into a smart-only shell surface:

- that route must be blocked
- the shell must remain on a valid SIM surface
- the product must not silently degrade into the smart shell

### Branch-S2: Drawer Conflict

If a second drawer is triggered while one is already open:

- the shell must atomically switch active drawers or keep the current one
- it must not render overlapping drawers

### Branch-S3: Chat Audio Reselect

When the user requests new audio from chat:

- the audio drawer must reopen
- Android native file manager must not become the default route for that chat flow

### Branch-S4: History Without Smart Memory

When the user opens history:

- SIM may show grouped chat sessions such as today / week / month
- SIM may allow rename, delete, and pin
- the surface must not depend on Oasis-style memory or smart-agent reasoning state

---

## Done-When Definition

The SIM shell is behaviorally ready only when:

- SIM launch mounts a standalone shell
- scheduler and audio drawers route correctly
- `Ask AI` transitions into simple chat
- audio re-selection returns to the audio drawer
- ordinary SIM shell practices remain available without reviving the smart runtime
- smart-only shell surfaces are not required for normal SIM use
