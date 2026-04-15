# SIM Wave 12 Execution Brief

**Status:** L1 Accepted  
**Date:** 2026-03-23  
**Wave:** 12  
**Mission:** SIM standalone prototype post-closeout mini-wave  
**Behavioral Authority:** `docs/core-flow/sim-scheduler-path-a-flow.md`  
**Current Reading Priority:** Historical reference only; not current source of truth.
**Historical Owning Specs At The Time:** `docs/cerb/sim-scheduler/spec.md`, `docs/cerb/sim-scheduler/interface.md`
**Current Active Truth:** `docs/plans/tracker.md`, `docs/core-flow/sim-scheduler-path-a-flow.md`, `docs/cerb/scheduler-path-a-spine/spec.md`, `docs/cerb/scheduler-path-a-spine/interface.md`, `docs/cerb-ui/scheduler/contract.md`, `docs/cerb/interface-map.md`
**Validation Report:** `docs/reports/tests/L1-20260323-sim-wave12-scheduler-drawer-reschedule.md`

---

## 1. Purpose

Wave 12 closes the missing scheduler-drawer voice reschedule lane.

This is not an audio-drawer feature and not a reopening of ordinary SIM chat into scheduler mutation.
It is a narrow scheduler-only mini-wave.

---

## 2. Required Read Order

1. `docs/plans/tracker.md`
2. `docs/core-flow/sim-scheduler-path-a-flow.md`
3. `docs/cerb/scheduler-path-a-spine/spec.md`
4. `docs/cerb/scheduler-path-a-spine/interface.md`
5. `docs/cerb-ui/scheduler/contract.md`
6. `docs/cerb/interface-map.md`
7. this file as historical execution context

---

## 3. Wave Objective

Deliver scheduler-drawer voice reschedule that:

- starts from the scheduler drawer mic path only
- resolves one intended task within scheduler-owned scope
- safe-fails on no-match or ambiguity without mutating state
- preserves existing reschedule conflict/reminder/attention semantics
- does not widen audio drawer or ordinary chat into scheduler mutation surfaces

---

## 4. Locked Product Decisions

- scope is scheduler drawer only
- audio drawer is out of scope
- ordinary `GENERAL` and `AUDIO_GROUNDED` SIM chat remain out of scope
- badge-origin task-scoped follow-up remains a separate already-accepted mutation lane
- target resolution must be confidence-gated and ASR-tolerant
- exact-title / SQL-only equality is not a sufficient product contract
- low-confidence resolution must safe-fail instead of guessing

---

## 5. Execution Slice

### Step 1: Docs-first lock

- register Wave 12 in `docs/plans/tracker.md`
- sync the current shared scheduler core-flow/spec/interface docs to the locked scope

### Step 2: Scheduler-owned target resolution

- add a scheduler-owned target-resolution seam on top of `ScheduleBoard`
- allow scheduler-local signals such as normalized title, participant/location hints, and nearby visible date context
- return typed results for resolved / ambiguous / no-match instead of pretending substring matching is enough

### Step 3: Scheduler drawer runtime

- remove the old hard-coded voice reschedule gap in `SimSchedulerViewModel`
- keep the runtime inside scheduler-owned collaborators
- reuse the delivered exact-time reschedule path after target resolution succeeds

### Step 4: Focused verification

- prove one successful scheduler-drawer voice reschedule in L1
- prove ambiguity/no-match safe-fail with no write
- prove audio drawer and ordinary SIM chat still do not gain scheduler mutation rights

---

## 6. Validation Checklist

- scheduler drawer mic can reschedule one clearly identified task
- no-match stays explicit and non-mutating
- ambiguous target stays explicit and non-mutating
- reminder cancel/rearm behavior remains intact after successful reschedule
- reschedule motion and destination-date attention remain intact after successful reschedule
- audio drawer and normal chat boundaries stay unchanged

---

## 7. Doc Sync Targets

- `docs/plans/tracker.md`
- `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/cerb/scheduler-path-a-spine/spec.md`
- `docs/cerb/scheduler-path-a-spine/interface.md`
- `docs/cerb-ui/scheduler/contract.md`
- `docs/cerb/interface-map.md`
