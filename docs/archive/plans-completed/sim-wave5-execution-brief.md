# SIM Wave 5 Execution Brief

**Status:** Wave 5 Accepted
**Date:** 2026-03-21
**Wave:** 5
**Mission:** SIM standalone prototype
**Behavioral Authority:** `docs/core-flow/sim-shell-routing-flow.md`
**Current Reading Priority:** Historical reference only; not current source of truth.
**Historical Owning Spec At The Time:** `docs/cerb/sim-connectivity/spec.md`
**Current Active Truth:** `docs/plans/tracker.md`, `docs/specs/flows/OnboardingFlow.md`, `docs/cerb/connectivity-bridge/spec.md`, `docs/cerb/connectivity-bridge/interface.md`, `docs/core-flow/sim-shell-routing-flow.md`
**Historical Companion Spec At The Time:** `docs/specs/connectivity-spec.md`

---

## 1. Purpose

This brief compresses Wave 5 into one practical execution artifact.

Wave 5 exists to hard-migrate the already mature connectivity support module into SIM without turning connectivity into a third main product lane and without dragging smart-shell assumptions into the standalone runtime.

---

## 2. Last Completed Slice

The final Wave 5 slice was `T5.4: Validation`.

This slice is now accepted on **2026-03-21**.

Delivered proof:

- focused L3 proof that scheduler remains usable while connectivity is absent
- focused L3 proof that already-persisted SIM audio artifacts remain usable while connectivity is absent
- focused L3 proof that artifact-grounded `Ask AI` remains usable while connectivity is absent
- focused L3 proof that disconnected manual `sync from badge` fails explicitly and non-destructively after the new browse-mode manual sync action and best-effort browse-open auto-sync path landed
- one honest acceptance note that records what was actually exercised on device and what evidence was user-observed versus log-backed

Still deferred:

- physical-badge-only follow-up work outside the disconnected connectivity-absent rerun
- any attempt to redefine connectivity as a primary SIM lane

---

## 3. Locked Product Decisions

- keep the existing SIM shell connectivity entry points rather than introducing a second connectivity launcher
- use `ConnectivityModal` only as the bootstrap entry for `NeedsSetup`
- let `ConnectivityModal` hand off into setup only when the user chooses `开始配网`
- mount the onboarding pairing subset (`HARDWARE_WAKE` through `FIRMWARE_CHECK`) as a SIM-owned full-screen overlay, not a new activity and not a smart-shell route
- on successful setup, enter a contained SIM connectivity manager support panel
- once configured, later connectivity entry opens the manager directly
- manager closes back to normal SIM chat
- the SIM manager stays connection-only for this slice rather than recovering the historical full `DeviceManager` product surface
- treat connectivity as a support surface, not a third primary feature lane

---

## 4. Execution Rules

### Shell Ownership

- `SimShell` owns the connectivity route state
- the connectivity module continues to own BLE/Wi-Fi behavior and connection truth
- do not move SIM shell navigation decisions into connectivity viewmodels
- do not let T5.4 validation work redefine shipped runtime behavior; this slice is acceptance closeout, not a route rewrite

### Boundary Freeze

- bless unchanged reuse of `ConnectivityBridge`, `ConnectivityService`, `ConnectivityViewModel`, `PairingService`, and the onboarding pairing subset
- bless shell-only ownership of entry sources, `MODAL/SETUP/MANAGER` route state, overlay semantics, close-back-to-chat, and SIM route telemetry
- state explicitly that scheduler must not depend on connectivity contracts
- state explicitly that audio may depend on `ConnectivityBridge` only for badge-origin recording ingress and badge file operations

### T5.4 Validation Focus

- treat `T5.3` as implemented background, not as the active Wave 5 slice
- validate the connectivity-absent contract against the SIM shell/audio core flows and `sim-connectivity` ownership rules
- reuse the new SIM-local telemetry/log markers for evidence rather than inventing new debug-only routes
- prove disconnected manual sync failure is explicit and non-blocking while keeping existing inventory usable
- prove browse-open auto-sync does not hijack the drawer when connectivity is absent or not ready
- do not mark T5.4 closed from L1 evidence alone; closure requires the recorded L3 device pass

### No-Scope Rule

- do not do the deferred manager UI refinement in T5.4
- do not reopen the shipped Wave 5 route contract while closing this validation slice
- do not widen the connectivity manager beyond the existing connection-only steady-state surface
- do not claim closure for branches that were not actually rerun on device

---

## 5. Validation Checklist

Wave 5 T5.4 verification had to prove:

- scheduler remains reachable and meaningful without forcing the user through connectivity setup/manager
- already-persisted SIM audio artifacts remain openable while disconnected
- artifact-grounded `Ask AI` still opens the SIM chat continuation surface while disconnected
- disconnected manual `sync from badge` fails explicitly, non-destructively, and without clearing the existing inventory
- browse-open auto-sync stays best-effort only and does not hijack shell routing when readiness is absent
- the new SIM-local evidence markers are captured honestly where available:
  - `SIM audio persisted artifact opened`
  - `SIM audio grounded chat opened from artifact`
  - `SIM audio sync failed while connectivity unavailable`
- the final note clearly distinguishes previously green manager-routing proof from this separate connectivity-absent acceptance slice

Accepted evidence:

- manager-routing proof: `docs/reports/tests/L3-20260321-sim-wave5-connectivity-validation.md`
- connectivity-absent proof: `docs/reports/tests/L3-20260321-sim-wave5-connectivity-absent-validation.md`

Post-acceptance cleanup now in code:

- the manager now renders as a contained support panel instead of a visually dominant full-screen surface
- disconnected manual badge sync now maps raw transport literals such as `oss_unknown null` to human-readable SIM copy
- focused L1 verification is green with `:app-core:compileDebugKotlin`, `SimAudioDebugScenarioTest`, `SimAudioDrawerViewModelTest`, and `SimConnectivityRoutingTest`

---

## 6. Doc Sync Targets

If T5.4 advances in the current session, sync these docs in the same session:

- `docs/plans/tracker.md`
- `docs/reports/tests/L3-20260321-sim-wave5-connectivity-absent-rerun-plan.md`
- `docs/reports/tests/L3-20260321-sim-wave5-connectivity-absent-validation.md`
- `docs/specs/flows/OnboardingFlow.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`

---

## 7. Done-When Summary

T5.4 is done when:

- disconnected device proof shows scheduler use still works without connectivity
- disconnected device proof shows persisted artifact open still works without connectivity
- disconnected device proof shows artifact-grounded `Ask AI` still works without connectivity
- disconnected manual sync failure is recorded as explicit, non-blocking, and inventory-safe
- the Wave 5 brief and trackers no longer describe the disconnected sync-failure rerun as pending debt

Current result:

- Done on **2026-03-21** via `docs/reports/tests/L3-20260321-sim-wave5-connectivity-absent-validation.md`
