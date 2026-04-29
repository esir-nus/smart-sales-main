# Core Flow: SIM Shell Routing

> **Role**: Core Flow
> **Authority**: Behavioral North Star
> **Status**: Ahead of Spec and Code
> **Development Chain**: Core Flow -> Spec -> Code -> PU Test -> Fix Spec -> Fix Code
> **Scope**: Standalone SIM app entry, shell routing, and top-level surface transitions between discussion chat, scheduler drawer, audio drawer, history, connectivity entry, and settings/new-session actions.
> **Testing Directive**: Validate one shell route or one safety branch per run. Do not mix multiple unrelated shell transitions in a single PU run.

---

## How To Read This Doc

This document defines **what the current legacy-named SIM/base-runtime shell must do**, not the final class names or DI layout.

Authority note:

- treat this flow as the detailed shell-routing authority beneath `docs/core-flow/base-runtime-ux-surface-governance-flow.md` for the current shared base-runtime shell lane
- deprecated SIM shell/connectivity Cerb shards are migration memory only and must not be used as the active spec layer
- current `SIM` naming in this file is legacy naming for the canonical base-runtime shell lane, not permission for a second non-Mono product line

- If this document conflicts with lower specs, treat the lower specs as drift candidates first.
- If this document conflicts with code, treat the code as behind or off-contract first.
- If a PU test does not cover a branch defined here, the validation surface is incomplete.

This file is allowed to be ahead of the codebase.

Sprint 08 alignment note:

- Target behavior remains governed by this flow and the UX governance flow, not by delivered code drift.
- Delivered gap: Sprint 08 found first-launch code returns to home after forced onboarding, with a shell-owned scheduler handoff gate possibly auto-opening the real scheduler drawer; this matches `docs/core-flow/base-runtime-ux-surface-governance-flow.md`.
- BAKE gap: Sprint 08 found delivered `PARTIAL_WIFI_DOWN` persistent dynamic-island takeover, but Wi-Fi mismatch and manager-only diagnostic states remain excluded from the target island takeover rules.
- Delivered gap: Sprint 08 found current telemetry does not prove the canonical valves `SIM_ENTRY_STARTED`, `SIM_SHELL_MOUNTED`, drawer-open/close valves, or `SMART_SURFACE_BLOCKED`.
- Test target: the BAKE contract must require direct Drawer Conflict proof and new-session follow-up-clear coverage; static source checks alone are not sufficient runtime proof.

Sprint 14 agent-chat alignment note:

- Target behavior remains general-chat-first: blank/general SIM chat is a real
  shell surface before any audio is attached. Composer send creates or reuses a
  SIM-local session and may later receive audio through drawer-based `Ask AI` or
  audio reselect.
- User-facing chat context may use system persona, user metadata, and local SIM
  session history, but shell routing must not depend on Mono/smart-agent memory,
  CRM/entity loading, plugin/tool runtime, mascot, or task-board surfaces.
- Session persistence is SIM-only. Durable message types are bounded to user
  text, AI response, AI audio artifacts, and AI error; voice draft, thinking,
  streaming, and transient presentation state are not durable shell truth.
- FunASR voice draft is draft-only. The shell may host mic capture and editable
  draft text in the composer, but explicit send remains required.
- Scheduler-shaped pre-route is a boundary, not shell ownership of scheduler
  truth. The shell may pass eligible general-chat input to scheduler-owned
  routing before LLM fallback while task storage, alarm/reminder behavior, and
  mutation authority remain scheduler-owned.
- Badge scheduler follow-up is hosted by the shell as prompt-first,
  task-scoped follow-up. It must not force-switch chat, become generic SIM
  chat, or create a second memory lane.
- BAKE gap: telemetry and runtime evidence remain incomplete for blank chat,
  composer send, provider/network behavior, voice draft/FunASR, scheduler
  pre-route, follow-up prompt/action strip, and logcat delivery. Sprint 16 must
  record these as contract gaps unless fresh runtime evidence exists.

---

## Downstream Sync Targets

| Layer | Responsibility | Must Eventually Reflect This Core Flow |
|------|----------------|-----------------------------------------|
| **Spec** | How the behavior is encoded | `docs/core-flow/base-runtime-ux-surface-governance-flow.md`, `docs/specs/prism-ui-ux-contract.md`, `docs/specs/base-runtime-unification.md`, `docs/cerb-ui/home-shell/spec.md`, `docs/specs/flows/OnboardingFlow.md`, `docs/cerb/interface-map.md`, and `docs/cerb-ui/dynamic-island/spec.md` |
| **Code** | Delivered behavior | `MainActivity`, `RuntimeShell`, and the current prototype-owned shell/viewmodel/DI composition |
| **PU Test** | Behavioral validation | future SIM shell routing tests, drawer transition tests, isolation tests |

---

## Non-Negotiable Invariants

1. **SIM boots into a standalone shell**: app launch must not silently boot the current smart shell.
2. **Only SIM-approved surfaces are reachable by default**: the standalone shell exposes only SIM-approved surfaces as normal behavior.
3. **Only one drawer is open at a time**: scheduler and audio drawers remain atomically exclusive.
4. **Audio re-entry is drawer-based**: selecting audio from inside chat must reopen the audio drawer instead of launching Android file management.
5. **Visual continuity does not justify runtime reuse**: the shell may look like Prism while still using a different composition root.
6. **Ordinary shell practices may survive, but smart runtime meaning must not**: history, new page/session, connectivity entry, and settings are allowed when simplified for SIM; mascot overlay, debug HUD, plugin task board, and smart-only right drawers are not required SIM behavior.
7. **Shell state must not require the smart app's orchestration model**: general SIM chat may track user metadata, local SIM history, and optional attached audio, but SIM must not depend on the smart app's broader memory architecture by default.
8. **Badge scheduler follow-up is prompt-first**: badge-origin scheduler success may create or rebind a task-scoped follow-up session, but SIM must surface it through an in-shell prompt/chip rather than force-switching chat immediately.
9. **Dynamic island routing follows the visible lane**: scheduler remains the default island lane, connectivity may interrupt in the center slot, tap follows the visible item, and downward drag remains scheduler-only.
10. **Shared UI must stay explicit**: shared shell/chat/scheduler surfaces may consume shell-owned adjunct state, but that state must be passed explicitly from `RuntimeShell` rather than inferred by shared-UI downcasts or legacy wrapper defaults.

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

- app launch with completed SIM first-launch gate -> discussion chat
- app launch after fresh install / reinstall -> forced SIM onboarding setup blocks normal shell use -> normal home shell; post-onboarding scheduler auto-open may occur only through the shell-owned handoff gate
- discussion chat -> normal message send/receive without any audio precondition
- discussion chat -> scheduler drawer
- discussion chat -> audio drawer
- discussion chat -> history drawer
- discussion chat -> new page/session
- discussion chat -> connectivity management
- discussion chat -> settings
- audio drawer -> simple chat via `Ask AI`
- simple chat -> audio drawer for re-selection
- badge-origin scheduler completion -> follow-up prompt/chip -> task-scoped follow-up session

Forbidden shell assumptions:

- mascot is part of the shell heartbeat
- plugin tasks or smart tips define the shell behavior
- history implies smart memory or smart-agent orchestration

## Dynamic Island Arbitration

The RuntimeShell-owned dynamic island in SIM follows these shell-routing rules:

- scheduler is the default lane and still draws its content from scheduler-owned projections
- connectivity takeover uses transport-truth `ConnectivityViewModel.connectionState`, not `effectiveState` or manager-only refinement state
- `CONNECTED` may interrupt the scheduler lane for `5s` on state change so the success state remains readable in the shell header
- `DISCONNECTED` may interrupt the scheduler lane for `3s` on state change
- `CONNECTED` may also heartbeat back into view every `30s` for `5s` when no other takeover is active
- `DISCONNECTED` may also heartbeat back into view every `30s` for `2.5s` when no other takeover is active
- `RECONNECTING` and `NEEDS_SETUP` stay pinned until the underlying transport state clears
- connectivity takeover is suppressed while the scheduler drawer is open or while any connectivity-owned surface (`MODAL`, `SETUP`, `MANAGER`) is already visible
- island tap routes to the currently visible lane: scheduler-visible tap opens scheduler, connectivity-visible tap opens connectivity entry
- downward drag on the island remains scheduler-only and is lawful only when the scheduler lane is visible
- update-check, updating, `PARTIAL_WIFI_DOWN`, Wi-Fi mismatch, and other manager-only refinements are excluded from island takeover in this slice

Target behavior: the island takeover list above remains the target. Delivered gap: Sprint 08 found `PARTIAL_WIFI_DOWN` currently renders as a persistent connectivity takeover in delivered code. That is a BAKE gap to repair or justify in the implementation sprint, not permission to widen the target island authority.

---

## Canonical Valves

These valve names are the behavioral checkpoints for downstream specs and tests.
The exact telemetry names in code may differ today, but they should converge toward this model.
Delivered gap from Sprint 08: current static evidence did not prove these canonical valves by name for `SIM_ENTRY_STARTED`, `SIM_SHELL_MOUNTED`, drawer open/close coverage, or `SMART_SURFACE_BLOCKED`; convergence remains the target.

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
                +--------------+--------------+
                |                             |
                v                             v
   +-----------------------------+   +----------------------+
   | SIM First-Launch Gate Open? |   |   SIM Shell Mounted  |
   +-------------+---------------+   +----------+-----------+
                 | yes                            |
                 v                                v
   +-----------------------------+      +----------------------+
   | Forced SIM Onboarding Setup |      | Discussion Chat      |
   | Visible                     |      | Visible              |
   +-------------+---------------+      +---+----+-----+-----+-+
                 |                             |    |     |     |
                 v                             v    v     v     v
   +-----------------------------+   +-----------+  |  +------+ +----------------+
   | Connectivity Manager        |   | Scheduler |  |  |Audio | | History / New |
   | Visible                     |   | Drawer    |  |  |Drawer| | / Connectivity|
   +-------------+---------------+   +-----+-----+  |  +--+---+ +--------+-------+
                 |                         |        |     |              |
                 v                         v        |     v              v
   +-----------------------------+  [Close Drawer]  | [Ask AI]   [Return to Chat]
   | Discussion Chat             |         |        |     |
   | Visible                     |         v        |     v
   +-----------------------------+  +--------------------+   +--------------------+
                                    | Discussion Chat    |   | Discussion Chat    |
                                    | Visible            |   | Visible            |
                                    +--------------------+   +---------+----------+
                                                                     ^
                                                                     |
                                                         +-----------+-----------+
                                                         | Badge Follow-Up Prompt|
                                                         +-----------+-----------+
                                                                     ^
                                                                     |
                                                        [Badge Scheduler Completion]
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
- the block must emit or record the `SMART_SURFACE_BLOCKED` checkpoint so the attempted route is auditable

### Branch-S2: Drawer Conflict

If a second drawer is triggered while one is already open:

- the shell must atomically switch active drawers or keep the current one
- it must not render overlapping drawers
- the BAKE contract must require direct Drawer Conflict proof rather than relying only on reducer source shape

### Branch-S3: Chat Audio Reselect

When the user requests new audio from chat:

- the audio drawer must reopen
- the current session must remain the same session while the active audio context is added or switched
- Android native file manager must not become the default route for that chat flow

### Branch-S4: History Without Smart Memory

When the user opens history:

- SIM may show grouped chat sessions such as today / week / month
- SIM may allow rename, delete, and pin
- the surface must not depend on Oasis-style memory or smart-agent reasoning state

### Branch-S5: Multi-Task Follow-Up Selection

When badge-origin completion created a follow-up session with multiple bound tasks:

- SIM may reuse the normal chat surface
- mutation-capable follow-up must remain blocked until the user explicitly selects one task
- no delete, mark-done, or reschedule mutation may silently guess a target task

---

## Done-When Definition

The SIM shell is behaviorally ready only when:

- fresh SIM install / reinstall bootstraps the shell into forced onboarding before ordinary shell use
- forced first-launch onboarding completion returns to normal shell use; if the scheduler quick-start completion armed a handoff, the real scheduler drawer may auto-open once through the shell-owned gate
- SIM launch mounts a standalone shell
- blank/new SIM chat is directly usable from the home surface
- scheduler and audio drawers route correctly
- `Ask AI` transitions into chat with audio pre-attached rather than defining the only legal chat entry
- audio re-selection returns to the audio drawer
- badge-origin scheduler follow-up stays prompt-first and task-scoped
- ordinary SIM shell practices remain available without reviving the smart runtime
- smart-only shell surfaces are not required for normal SIM use
- the RuntimeShell dynamic island defaults to scheduler, applies the approved connectivity interrupt/heartbeat rules, routes tap to the visible lane, and keeps downward drag scheduler-only
- Wi-Fi mismatch, `PARTIAL_WIFI_DOWN`, update, and manager-only diagnostic refinements must not take over the dynamic island unless a later target-flow update explicitly expands the authority boundary
- new-session behavior clears active badge scheduler follow-up prompts when appropriate and remains covered by executable tests
