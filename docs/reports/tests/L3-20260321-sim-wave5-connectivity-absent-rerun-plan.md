# L3 On-Device Test Plan: SIM Wave 5 Connectivity-Absent Rerun

**Date**: 2026-03-21
**Tester**: Agent + user
**Target Build**: `:app-core:installDebug`
**Status**: PREPARED_ONLY

---

## 1. Purpose

This note prepares the remaining `T5.4` L3 device rerun for the SIM connectivity-absent slice.

It does **not** claim the rerun already happened.

The goal is to close the final Wave 5 validation branch against:

- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/sim-connectivity/spec.md`
- `docs/cerb/sim-audio-chat/spec.md`

Specifically, this rerun must prove that SIM scheduler and SIM audio/chat stay meaningful when connectivity is absent, and that the new drawer-owned badge sync ingress fails explicitly and non-destructively when invoked while disconnected.

---

## 2. Required Entry State

- Latest debug APK installed from `:app-core:installDebug`
- SIM launched through `com.smartsales.prism/.SimMainActivity`
- Connectivity absent/disconnected for the whole rerun window
- At least one already-persisted transcribed SIM audio item available in inventory before the disconnected checks begin
- The new browse-mode manual `sync from badge` action present in the SIM audio drawer

If the device cannot start from a genuinely disconnected state, the rerun is not valid for T5.4 closeout.

---

## 3. Focused Execution Plan

### T1: Scheduler Meaningful While Disconnected

- Launch SIM while badge connectivity is unavailable
- Open the scheduler drawer from the normal SIM shell
- Confirm the drawer is usable and does not force connectivity setup/manager just to reach scheduler behavior

### T2: Persisted Artifact Reuse While Disconnected

- Open the SIM audio drawer in browse mode
- Select an already-transcribed local SIM audio card
- Confirm the persisted artifact opens while disconnected

### T3: Grounded `Ask AI` While Disconnected

- From that opened artifact, tap `Ask AI`
- Confirm SIM opens the grounded chat continuation surface rather than redirecting into connectivity management

### T4: Disconnected `sync from badge` Failure

- Return to the audio drawer browse surface while still disconnected
- Trigger the manual `sync from badge` action
- Confirm SIM surfaces explicit non-blocking failure
- Confirm existing inventory remains visible and usable after the failure
- Confirm no shell hijack into connectivity modal/setup/manager occurs just because the sync failed

### T4b: Browse-Open Auto-Sync Safety Check

- Reopen the browse drawer again while still disconnected
- Confirm browse-open behavior stays usable and does not block on connectivity readiness
- If no auto-sync attempt starts because readiness is absent, record that as the expected best-effort skip rather than as a failure

---

## 4. Expected Evidence to Capture

| Checkpoint | Expected Behavior | Evidence to Capture |
| :--- | :--- | :--- |
| **T1 Scheduler** | Scheduler remains reachable/usable while disconnected | short user-observed note; screenshot optional |
| **T2 Artifact Open** | Persisted artifact opens while disconnected | UI observation plus `VALVE_PROTOCOL` / `SimAudioOffline` evidence for `SIM audio persisted artifact opened` when available |
| **T3 Grounded Chat** | `Ask AI` opens grounded chat from the artifact while disconnected | UI observation plus `VALVE_PROTOCOL` / `SimAudioChatRoute` evidence for `SIM audio grounded chat opened from artifact` when available |
| **T4 Manual Sync Failure** | Manual badge sync fails explicitly, non-blockingly, and keeps inventory intact | UI observation plus `VALVE_PROTOCOL` / `SimAudioOffline` evidence for `SIM audio sync failed while connectivity unavailable` when available |
| **T4b Auto-Sync Safety** | Browse-open stays usable; absence of readiness causes skip rather than shell hijack | UI observation; note whether any visible feedback appeared |

---

## 5. Result Capture Template

Fill this section only after the device rerun actually happens.

| Checkpoint | Actual Behavior | Result |
| :--- | :--- | :---: |
| **T1 Scheduler** | Pending device rerun | ☐ |
| **T2 Artifact Open** | Pending device rerun | ☐ |
| **T3 Grounded Chat** | Pending device rerun | ☐ |
| **T4 Manual Sync Failure** | Pending device rerun | ☐ |
| **T4b Auto-Sync Safety** | Pending device rerun | ☐ |

---

## 6. Closure Rule

`T5.4` may be closed only if:

- `T1` through `T4` are green on device
- no observed step forces connectivity as a hidden prerequisite for scheduler or persisted-audio/chat use
- the manual sync failure is explicit and non-destructive
- the final note stays honest about what telemetry/log evidence was or was not captured

If the rerun is not executed, or if `T4` is not exercised, Wave 5 remains validation-open.
