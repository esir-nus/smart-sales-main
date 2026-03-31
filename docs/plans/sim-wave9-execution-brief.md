# SIM Wave 9 Execution Brief

**Status:** Prepared  
**Date:** 2026-03-22  
**Wave:** 9  
**Mission:** SIM standalone prototype post-closeout hardware validation mini-wave  
**Behavioral Authority:** `docs/core-flow/sim-shell-routing-flow.md`  
**Current Reading Priority:** Historical reference only; not current source of truth.
**Historical Owning Specs At The Time:** `docs/cerb/sim-shell/spec.md`, `docs/cerb/sim-scheduler/spec.md`, `docs/cerb/sim-audio-chat/spec.md`
**Current Active Truth:** `docs/plans/tracker.md`, `docs/core-flow/sim-shell-routing-flow.md`, `docs/core-flow/sim-scheduler-path-a-flow.md`, `docs/core-flow/sim-audio-artifact-chat-flow.md`, `docs/cerb/interface-map.md`

---

## 1. Purpose

Wave 9 reopens the final SIM follow-up debt as a verification-only mini-wave.

This is not a new feature wave.
It exists to prove that the accepted Wave 8 follow-up lane survives true physical-badge ingress rather than only debug-assisted ingress.

If hardware validation exposes drift, Wave 9 must close as blocked and spawn a separate repair mini-wave.
Do not silently convert this brief into implementation work.

---

## 2. Required Read Order

1. `docs/plans/tracker.md`
2. `docs/core-flow/sim-shell-routing-flow.md`
3. `docs/core-flow/sim-scheduler-path-a-flow.md`
4. `docs/core-flow/sim-audio-artifact-chat-flow.md`
5. `docs/cerb/interface-map.md`
6. `docs/reports/tests/L3-20260322-sim-wave8-follow-up-validation.md`
7. this file as historical execution context

---

## 3. Wave Objective

Close the remaining physical-badge hardware L3 gap by proving that real badge-origin follow-up can:

- enter SIM through the true hardware path
- surface the visible follow-up prompt/chip
- open the correct scoped follow-up session
- execute one real single-task bound mutation
- preserve the accepted multi-task no-selection safe-fail law

Wave 9 closes only if the single-task mutation proof and multi-task safe-fail proof are both captured on real hardware.

---

## 4. Locked Test Decisions

- this is a verification-only mini-wave; no code or interface change is planned by default
- debug-assisted evidence does not count as hardware proof
- visible UI proof alone is insufficient; downstream mutation proof is required
- single-task closeout action defaults to `完成`
- multi-task selected-task mutation is informative but non-blocking unless the hardware path diverges from accepted Wave 8 behavior
- any hardware-exposed drift must be recorded as a blocked verdict and moved into a separate repair mini-wave

---

## 5. Execution Slice

### Step 1: Contract freeze

- load the controlling docs and Wave 8 acceptance note before the run
- freeze the exact acceptance bar before touching the device
- record device, build, badge, and connectivity preconditions in the final note

### Step 2: Fixture definition

- prepare one real badge single-task fixture and one real badge multi-task fixture
- write down the expected task title(s), expected task count, and expected action before the run starts
- identify the exact UI and log/persistence evidence that will count as proof for each fixture

### Step 3: Single-task hardware E2E

- trigger a real badge-origin single-task create event
- prove hardware-origin ingress reached SIM
- prove the visible follow-up prompt/chip appears
- open the prompt and prove the correct scoped session is bound
- execute `完成` and prove the correct task mutated downstream

### Step 4: Multi-task hardware safe-fail

- trigger a real badge-origin multi-task create event
- prove hardware-origin ingress and visible prompt/session state
- attempt a mutation without selecting a task first
- prove no mutation occurs and explicit selection is still required

### Step 5: Boundary check

- confirm the lane stays prompt-first and task-scoped
- confirm no smart-agent memory or open-ended assistant behavior appears
- confirm the hardware path is not silently replaced by a debug surrogate

### Step 6: Verdict and sync

- write a dedicated L3 hardware validation note with exact steps and evidence
- if accepted, close the remaining hardware debt in the SIM tracker
- if blocked, create a separate repair mini-wave instead of leaving the failure implicit

---

## 6. Expected Evidence

| Checkpoint | Expected Behavior | Required Evidence |
| :--- | :--- | :--- |
| Single-task ingress | Real badge follow-up reaches SIM | device observation plus hardware-origin log evidence |
| Prompt/chip | Visible prompt appears without auto-opening chat | screenshot or direct on-device observation |
| Single-task mutation | `完成` mutates the correct bound task | UI success state plus downstream log/persistence proof |
| Multi-task safe-fail | No-selection mutation does not write | UI observation plus no-write evidence |
| Boundary guard | Lane stays SIM-owned and task-scoped | run notes plus absence of smart-agent drift |

---

## 7. Closure Rule

Wave 9 may be accepted only if:

- the single-task hardware path proves visible prompt/chip plus correct bound-task mutation
- the multi-task hardware path proves no-selection safe-fail with no unintended write
- the final note stays explicit about any evidence that was or was not captured

Wave 9 is blocked if:

- hardware ingress cannot be distinguished from a debug surrogate
- the prompt/chip does not appear from the real badge path
- correct bound-task mutation cannot be proven
- multi-task no-selection can mutate the wrong task
- the hardware run cannot be completed honestly

---

## 8. Doc Sync Targets

- `docs/plans/tracker.md`
- `docs/reports/tests/L3-20260322-sim-wave9-hardware-validation.md`
