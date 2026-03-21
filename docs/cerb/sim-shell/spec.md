# SIM Shell Spec

> **Scope**: Standalone SIM prototype shell inside this repository
> **Status**: SPEC_ONLY
> **Behavioral Authority Above This Doc**:
> - `docs/core-flow/sim-shell-routing-flow.md`
> - `docs/to-cerb/sim-standalone-prototype/concept.md`
> **Audit Evidence**:
> - `docs/reports/20260319-sim-standalone-code-audit.md`
> - `docs/reports/20260319-sim-clarification-evidence-audit.md`

---

## 1. Purpose

`SIM Shell` is the standalone application shell for the simplified Prism prototype.

It exists to preserve the current Prism family look while preventing contamination from the smart-agent runtime.

`SIM Shell` owns:

- standalone app entry
- top-level drawer orchestration
- simple navigation between discussion chat, scheduler, and audio
- shell support surfaces such as history, new page/session, connectivity entry, and settings entry
- shell-owned badge scheduler follow-up continuity binding metadata
- prototype-only dependency composition boundary

`SIM Shell` does not own:

- smart-agent orchestration
- plugin execution framework
- mascot behavior
- the current smart app's Hilt root

---

## 2. Product Role

This shell should feel visually continuous with the current app, but operationally it is a different product mode.

The user should perceive:

- the same Prism shell language
- the same drawer family and glass/light treatment
- a smaller, more literal app focused on two main feature lanes
- ordinary shell affordances that still feel normal, such as history, new page/session, connectivity entry, and settings

The user should not perceive:

- hidden smart-agent behavior
- unrelated drawers and labs
- runtime surfaces that imply the old agent OS still exists behind the prototype

---

## 3. Non-Negotiable Rules

1. `SIM Shell` must be a standalone entry path, not a conditional branch jammed into the smart shell.
2. `SIM Shell` must not reuse `AgentShell` as its composition root.
3. `SIM Shell` must not depend on the smart app's full `PrismModule` graph.
4. `SIM Shell` may reuse existing composables only through controlled seams.
5. `SIM Shell` must expose the two main product lanes and only the SIM-approved support surfaces:
   scheduler,
   audio/transcription plus simple audio-grounded chat,
   history/new page/connectivity/settings as simplified shell practices.

---

## 4. Reuse Contract

### Allowed UI Reuse

- `AgentIntelligenceScreen`
- `SchedulerDrawer`
- `HistoryDrawer`
- `ConnectivityModal`
- `UserCenterScreen`
- shared tokens, surfaces, and shell styling

### Conditional UI Reuse

- `AudioDrawer` only after its state/runtime seam is separated from shared audio/session behavior
- Wave 1 may use a SIM-owned audio drawer wrapper while the current shared `AudioDrawer` remains coupled to shared repositories and concrete audio viewmodel behavior

### Forbidden Direct Reuse

- `AgentMainActivity`
- `AgentShell`
- `AgentViewModel`
- current global `PrismModule` as the SIM composition root

### Allowed Replacement Pattern

If an existing UI surface already accepts an interface contract, SIM should provide its own implementation rather than duplicating the UI.

Current target seams:

- `IAgentViewModel`
- `ISchedulerViewModel`

### Naming Rule

If current smart-app names blur standalone ownership, SIM should prefer explicit standalone naming over legacy familiarity.

Examples:

- `SimMainActivity` over reuse of `AgentMainActivity`
- `SimShell` over reuse of `AgentShell`

Do not rename only for style; rename when boundary truth improves.

---

## 5. Navigation Model

`SIM Shell` owns three primary surfaces:

1. `Discussion Chat Surface`
2. `Scheduler Drawer`
3. `Audio Drawer`

`SIM Shell` also permits support surfaces/actions:

- history drawer
- new page / new session action
- connectivity entry for badge connection management
- settings entry for profile/metadata controls

Expected navigation:

- opening scheduler does not require the smart shell
- opening audio drawer does not require the smart shell
- opening history does not require smart memory architecture
- opening connectivity uses a SIM-owned state-aware route:
  - `NeedsSetup` opens the bootstrap modal
  - configured/non-setup states open the connectivity manager directly
- choosing setup from the connectivity modal opens a full-screen SIM setup branch backed by onboarding pairing steps
- successful connectivity setup enters the SIM connectivity manager
- closing the connectivity manager returns the user to normal SIM chat
- `Ask AI` from audio opens the simple chat surface
- selecting audio from chat reopens the audio drawer instead of Android file picker

### Connectivity Boundary Rule

For Wave 5 boundary purposes, `SIM Shell` owns only:

- connectivity entry sources
- route selection between `MODAL`, `SETUP`, and `MANAGER`
- overlay layering and dismiss semantics
- close-back-to-chat behavior
- SIM route telemetry/logging

`SIM Shell` does not own:

- badge connection truth
- BLE scan or Wi-Fi provisioning behavior
- reconnect/disconnect/unpair behavior
- audio-ingress file operations

Those remain owned by the existing connectivity and pairing contracts.

Excluded shell surfaces:

- mascot overlay
- debug HUD
- right-side Tingwu/artifact stubs from the smart shell
- plugin task-board shell behavior

---

## 6. State Model

`SIM Shell` should be controlled by a prototype-only shell state holder.

Minimum top-level state:

```kotlin
enum class SimDrawerType {
    SCHEDULER,
    AUDIO
}

data class SimShellState(
    val activeDrawer: SimDrawerType? = null,
    val activeChatAudioId: String? = null,
    val activeConnectivitySurface: SimConnectivitySurface? = null,
    val isChatReady: Boolean = true,
    val showHistory: Boolean = false
)
```

Notes:

- `activeChatAudioId` identifies the current audio-grounded session context
- `activeConnectivitySurface` distinguishes the scrim-backed bootstrap modal from the full-screen setup and manager branches
- no shell state should imply smart history/session browsing or smart-agent orchestration requirements

### Badge Follow-Up Continuity Binding

`SIM Shell` may keep one in-memory, shell-owned metadata record for a badge-origin scheduler follow-up thread.

This continuity binding exists only to preserve ownership across shell surfaces.

It may track:

- badge-origin thread identity
- the bound SIM chat session id
- the last active shell surface
- create/update timestamps

It must not track:

- follow-up transcript turns
- private session-memory summaries
- a second local memory lane separate from SIM chat history

The bound SIM chat session remains the conversational source of truth, even though current SIM chat behavior is still placeholder-level and does not yet provide real scheduler follow-up intelligence.

In Wave 5, this continuity binding now starts from the real badge pipeline ingress.

- `SimShell` consumes `BadgeAudioPipeline.events`
- only `PipelineEvent.Complete` with scheduler-real outcomes may start or replace the binding
- accepted outcomes are `TaskCreated` and non-empty `MultiTaskCreated`
- raw recording arrival, shelf-card `Ask AI`, and scheduler dev/test mic paths must not start or mutate this binding
- each new accepted badge scheduler completion replaces the previous active binding until the pipeline exposes an explicit lineage id

Lifecycle rules:

- overlay navigation across chat, scheduler, history, connectivity, and settings may update the last active surface
- explicit new session clears the binding
- switching to an unrelated session clears the binding
- deleting the bound session clears the binding
- process death clears the binding because lifetime is in-memory only

---

## 7. Composition Boundary

`SIM Shell` must introduce a prototype-only composition root.

Minimum expected composition units:

- `SimMainActivity`
- `SimShell`
- `SimAgentViewModel`
- `SimSchedulerViewModel`
- prototype-only DI module set

This boundary is the contamination firewall.

---

## 8. Human Reality

### Organic UX

The user expects "same shell" to mean visual familiarity, not identical internal architecture.

So the shell may simplify behavior as long as:

- entry feels familiar
- drawers feel familiar
- history/new-page/connectivity/settings remain coherent
- the app does not suddenly expose missing smart surfaces and dead controls

### Data Reality

The shell must not assume it can safely share all smart-app session state.

Session identity, chat context, and audio bindings may need prototype-specific storage or namespacing.

### Failure Gravity

The most dangerous failure is a fake standalone build where the shell still boots the smart runtime under the hood.

That failure is worse than minor UI drift.

---

## 9. Wave Plan

| Wave | Focus | Status | Deliverable |
|------|-------|--------|-------------|
| 1 | Standalone entry and shell contract | IMPLEMENTED_PENDING_RUNTIME_VERIFICATION | `SimMainActivity`, `SimShell`, shell spec/interface |
| 2 | Chat-shell reuse via interface seam | IMPLEMENTED | `SimAgentViewModel : IAgentViewModel` |
| 3 | Drawer orchestration cleanup | IN_PROGRESS | scheduler/audio/history support behavior |
| 4 | Isolation validation | PLANNED | proof that smart runtime is not the SIM root |

---

## 10. Registration Notes

This shard defines planning truth for a future standalone topology.

`docs/cerb/interface-map.md` should not be updated until the package/composition boundary is chosen and real implementation ownership exists.
