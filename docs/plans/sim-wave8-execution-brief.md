# SIM Wave 8 Execution Brief

**Status:** In Progress  
**Date:** 2026-03-22  
**Wave:** 8  
**Mission:** SIM standalone prototype post-closeout mini-wave  
**Behavioral Authority:** `docs/core-flow/sim-shell-routing-flow.md`  
**Owning Specs:** `docs/cerb/sim-shell/spec.md`, `docs/cerb/sim-scheduler/spec.md`, `docs/cerb/sim-audio-chat/spec.md`  
**Mission Tracker:** `docs/plans/sim-tracker.md`

---

## 1. Purpose

Wave 8 adds the first real SIM user-facing scheduler follow-up lane after badge-origin task creation.

This is not a reopening of the accepted Wave 7 mission gates.
It is a narrow post-closeout mini-wave.

---

## 2. Required Read Order

1. `docs/plans/sim-tracker.md`
2. `docs/core-flow/sim-shell-routing-flow.md`
3. `docs/cerb/sim-shell/spec.md`
4. `docs/cerb/sim-scheduler/spec.md`
5. `docs/cerb/sim-audio-chat/spec.md`
6. this file

---

## 3. Wave Objective

Deliver a task-scoped scheduler follow-up session that:

- starts only from real badge-origin scheduler completion
- creates or rebinds a SIM follow-up session without auto-forcing chat open
- shows an in-shell prompt/chip so the user can enter that session deliberately
- keeps follow-up actions scoped to the created task set
- reuses the SIM chat surface without reviving smart-agent behavior

---

## 4. Locked Product Decisions

- foreground behavior is `create/rebind session + show prompt`, not auto-open
- follow-up uses `quick actions + narrow free-text`
- free-text remains task-scoped to the bound task set
- multi-task ambiguity must safe-fail until the user explicitly selects one task
- no new unrelated task creation belongs in this lane

---

## 5. Execution Slice

### Step 1: Session model

- persist a SIM session kind for scheduler follow-up
- persist badge thread id plus bound task summaries on session metadata
- keep shell active binding in-memory only

### Step 2: Badge ingress

- only `BadgeAudioPipeline` scheduler-complete results may create/rebind the lane
- accepted outcomes stay `TaskCreated` and non-empty `MultiTaskCreated`
- scheduler dev/test mic and shelf `Ask AI` must stay out

### Step 3: Shell prompt

- render a visible prompt/chip when a follow-up session exists but is not the active chat
- tapping the prompt opens the bound session
- do not auto-switch surfaces while the user is elsewhere in SIM

### Step 4: Scoped follow-up actions

- support explain/status/mark-done/delete directly
- support reschedule through narrow free-text against the selected task
- update persisted follow-up task summaries after delete or reschedule
- safe-fail when no target task is selected in a multi-task session

---

## 6. Validation Checklist

- badge-origin scheduler completion creates/rebinds a follow-up session
- follow-up prompt is visible without auto-jumping chat
- session history persists follow-up context across cold start
- quick actions mutate only bound tasks
- multi-task no-selection reschedule safe-fails without mutation
- no smart-agent memory/plugin behavior appears in this lane

---

## 7. Doc Sync Targets

- `docs/plans/sim-tracker.md`
- `docs/plans/tracker.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `docs/cerb/sim-scheduler/spec.md`
- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/interface-map.md`
