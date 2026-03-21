# SIM Wave 5 Execution Brief

**Status:** T5.2 Boundary Frozen
**Date:** 2026-03-20
**Wave:** 5
**Mission:** SIM standalone prototype
**Behavioral Authority:** `docs/core-flow/sim-shell-routing-flow.md`
**Owning Spec:** `docs/cerb/sim-connectivity/spec.md`
**Companion Specs:** `docs/specs/connectivity-spec.md`, `docs/cerb/connectivity-bridge/spec.md`
**Mission Tracker:** `docs/plans/sim-tracker.md`

---

## 1. Purpose

This brief compresses Wave 5 into one practical execution artifact.

Wave 5 exists to hard-migrate the already mature connectivity support module into SIM without turning connectivity into a third main product lane and without dragging smart-shell assumptions into the standalone runtime.

---

## 2. Current Slice

The active Wave 5 slice is `T5.2: Spec / Hard Migration Boundary`.

This slice must deliver:

- one explicit boundary contract for what connectivity pieces SIM reuses unchanged
- one explicit boundary contract for what `SimShell` owns only as route/presentation glue
- lane rules proving scheduler stays connectivity-free
- lane rules proving audio may use `ConnectivityBridge` only as badge-origin ingress/file-ops backend
- explicit carry debt for the remaining contamination points that belong to T5.3

This slice intentionally defers:

- manager presentation/UI containment refinement
- code-level isolation fixes
- deeper T5.4 proof that scheduler/audio remain meaningful with connectivity absent

---

## 3. Locked Product Decisions

- keep the existing SIM shell connectivity entry points rather than introducing a second connectivity launcher
- use `ConnectivityModal` only as the bootstrap entry for `NeedsSetup`
- let `ConnectivityModal` hand off into setup only when the user chooses `开始配网`
- mount the onboarding pairing subset (`HARDWARE_WAKE` through `FIRMWARE_CHECK`) as a SIM-owned full-screen overlay, not a new activity and not a smart-shell route
- on successful setup, enter a full-screen SIM connectivity manager surface
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
- do not let T5.2 redefine shipped runtime behavior; this is a boundary freeze, not a route rewrite

### Boundary Freeze

- bless unchanged reuse of `ConnectivityBridge`, `ConnectivityService`, `ConnectivityViewModel`, `PairingService`, and the onboarding pairing subset
- bless shell-only ownership of entry sources, `MODAL/SETUP/MANAGER` route state, overlay semantics, close-back-to-chat, and SIM route telemetry
- state explicitly that scheduler must not depend on connectivity contracts
- state explicitly that audio may depend on `ConnectivityBridge` only for badge-origin recording ingress and badge file operations
- record the `PairingService -> BlePeripheral` leak and onboarding profile/account collaborator carry-over as T5.3 debt

### No-Scope Rule

- do not patch runtime code in T5.2
- do not do the deferred manager UI refinement in T5.2
- do not rewrite pairing/profile dependencies yet; only record them as explicit next-slice debt

---

## 5. Validation Checklist

Wave 5 T5.2 verification must prove:

- scheduler currently has no connectivity imports
- shell currently owns connectivity route handling only
- audio currently consumes `ConnectivityBridge` for badge-origin ingress/file operations
- setup currently reuses onboarding pairing subset plus `PairingService`
- `PairingService -> BlePeripheral` contamination is real and explicitly recorded as debt
- onboarding profile/account collaborator carry-over is real and explicitly recorded as debt

---

## 6. Doc Sync Targets

If T5.2 lands as a boundary freeze, sync these docs in the same session:

- `docs/plans/sim-tracker.md`
- `docs/plans/tracker.md`
- `docs/cerb/sim-connectivity/spec.md`
- `docs/cerb/sim-connectivity/interface.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `docs/cerb/sim-scheduler/spec.md`
- `docs/cerb/sim-scheduler/interface.md`
- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/sim-audio-chat/interface.md`
- `docs/reports/20260321-sim-wave5-boundary-audit.md`

---

## 7. Done-When Summary

T5.2 is done when:

- the shell/connectivity ownership split is explicit across the Wave 5 doc set
- scheduler and audio lane docs state the allowed/forbidden connectivity coupling directly
- the remaining contamination points are recorded as T5.3 debt instead of implicit ambiguity
- the Wave 5 brief and trackers no longer describe the boundary as unresolved
