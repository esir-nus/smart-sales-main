# SIM Wave 1 Execution Brief

**Status:** Accepted
**Date:** 2026-03-19
**Wave:** 1
**Mission:** SIM standalone prototype
**Behavioral Authority:** `docs/core-flow/sim-shell-routing-flow.md`
**Owning Spec:** `docs/cerb/sim-shell/spec.md`
**Boundary Brief:** `docs/plans/sim_implementation_brief.md`
**Mission Tracker:** `docs/plans/sim-tracker.md`

---

## 1. Purpose

This brief compresses Wave 1 into one practical handoff artifact.

Use it when implementation starts, when Wave 1 is reviewed, and when later waves need to confirm what shell boundary work was supposed to happen first.

This is not the product PRD and not the long-term boundary constitution.
It is the execution brief for the standalone shell wave.

---

## 2. Required Read Order

Before coding Wave 1, read in this order:

1. `docs/plans/sim-tracker.md`
2. `docs/plans/sim_implementation_brief.md`
3. `docs/core-flow/sim-shell-routing-flow.md`
4. `docs/cerb/sim-shell/spec.md`
5. this file

If code reality forces a boundary change, update the tracker and the owning shell docs in the same session.

---

## 3. Wave Objective

Wave 1 exists to prove that SIM has a real standalone shell boundary.

Wave 1 must deliver:

- separate SIM app entry through `SimMainActivity`
- standalone shell mount through `SimShell`
- prototype-only shell state ownership
- discussion chat as the default visible surface
- drawer routing for scheduler and audio
- simplified history, connectivity, settings, and new-session shell practices
- explicit suppression of smart-only shell surfaces

Wave 1 is successful only if SIM can boot and route like Prism without silently reusing the smart shell as its runtime root.

---

## 4. Out Of Scope

Do not try to finish these in Wave 1:

- Tingwu readability-polisher logic
- transcript streaming or pseudo-streaming cosmetics
- full audio artifact rendering
- audio-grounded discussion behavior beyond shell handoff
- scheduler business logic beyond shell entry and drawer mounting
- connectivity business rewrites

Those belong to later waves even if a shell stub is needed now.

---

## 5. Allowed Touch Map

### New Artifacts Expected

- `SimMainActivity`
- `SimShell`
- `SimShellState` or equivalent SIM-owned shell state holder
- `SimShellDependencies`
- `SimAgentViewModel`
- `SimSchedulerViewModel`
- SIM-owned history/session filter or adapter boundary if current history storage is reused

### Existing Files Safe To Reuse Through Controlled Seams

- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/settings/UserCenterScreen.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/IAgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/ISchedulerViewModel.kt`

### Conditional Reuse

- `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioDrawer.kt` only after its audio/session seam is separated from shared runtime behavior
- if that seam is not extracted yet, use a SIM-owned drawer wrapper for Wave 1 routing and shell validation

### Forbidden Runtime Roots

- `app-core/src/main/java/com/smartsales/prism/AgentMainActivity.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/di/PrismModule.kt`

---

## 6. Execution Sequence

### Step 1: Standalone Entry

- create `SimMainActivity`
- mount `SimShell` directly from that entry
- prove normal SIM boot does not route through `AgentMainActivity` or `AgentShell`

### Step 2: Prototype-Only Composition Root

- introduce SIM-owned dependency assembly
- keep smart-app singleton/root ownership out of the SIM boot path
- reuse leaf UI contracts only through explicit seams

### Step 3: Shell State Holder

Minimum responsibilities:

- track the active drawer
- enforce one-drawer-at-a-time
- track the active audio discussion binding at shell level if needed
- track history visibility
- track support-surface visibility when SIM needs it

### Step 4: Discussion Surface Baseline

- make discussion chat the default visible SIM surface
- back it with `SimAgentViewModel`
- keep the Wave 1 implementation minimal and shell-oriented
- do not revive plugin board, agent memory, mascot, or smart orchestration assumptions

### Step 5: Drawer Routing

- wire scheduler drawer open/close
- wire audio drawer open/close
- keep drawer behavior atomically exclusive
- make `Ask AI` a valid route from informational mode into discussion mode even if later waves deepen the behavior

### Step 6: SIM Shell Practices

- keep a simplified history drawer
- keep new page/session entry
- keep connectivity entry
- keep settings entry for profile/metadata controls

These controls may stay visually familiar, but their runtime meaning must remain SIM-safe.

### Step 7: History and Session Isolation

- do not expose smart-agent sessions in SIM by accident
- if history persistence is reused, add a SIM-owned adapter or filter boundary
- treat this as part of Wave 1 shell safety, not as an optional later cleanup

### Step 8: Smart Surface Suppression

- suppress mascot overlay
- suppress debug HUD
- suppress plugin/task-board shell behavior
- suppress smart-only right-side shell meaning and stubs

---

## 7. Validation Checklist

Wave 1 verification must prove:

- SIM boots into `SimMainActivity`
- `SimShell` mounts without using the smart shell as root
- scheduler drawer opens and closes
- audio drawer opens and closes
- only one drawer is open at a time
- `Ask AI` can hand off from informational mode into discussion mode at the shell-routing level
- history, connectivity, settings, and new-session practices work without smart runtime assumptions
- smart sessions are not surfaced accidentally through SIM history
- mascot, debug HUD, plugin board, and smart-only shell surfaces are absent in normal SIM operation

If any of these fail, Wave 1 is not done even if the UI looks correct.

---

## 8. Doc Sync Targets

If Wave 1 changes runtime ownership, file paths, or shell behavior, sync these docs in the same session:

- `docs/plans/sim-tracker.md`
- `docs/plans/tracker.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `docs/cerb/interface-map.md` if module ownership edges become real

If the implementation reveals a better SIM root naming choice, update the implementation brief too:

- `docs/plans/sim_implementation_brief.md`

---

## 9. Done-When Summary

Wave 1 is done when:

- SIM has a real standalone entry and shell root
- shell routing works for the approved SIM surfaces
- shell support practices exist in simplified SIM-safe form
- history/session exposure is filtered for SIM
- smart shell behavior is suppressed rather than silently inherited
