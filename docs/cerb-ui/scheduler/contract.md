# Scheduler Drawer UI Contract

> **Status:** Active UI state contract
> **Last Updated:** 2026-04-01
> **Behavioral Authority Above This Doc:** `docs/core-flow/base-runtime-ux-surface-governance-flow.md` (`UX.SCHEDULER.*`) plus `docs/core-flow/scheduler-fast-track-flow.md`
> **Implementation Shards Beneath This Doc:**
> - `docs/cerb/scheduler-path-a-spine/spec.md`
> - `docs/cerb/scheduler-path-a-uni-a/spec.md`

## Purpose

This document is the local UI-facing source of truth for the sealed states emitted by the Scheduler Drawer presentation layer beneath the shared UX surface contract.
It also owns the scheduler card indicator contract used by the scheduler surface outside the transient drawer states.

It does not redefine scheduler business behavior or the shared scheduler surface composition already owned by `docs/core-flow/base-runtime-ux-surface-governance-flow.md`.
It defines the exact UI projection names that must stay aligned with `SchedulerUiState`.

## State Contract

```kotlin
sealed class SchedulerUiState {
    object Idle : SchedulerUiState()
    object ScanningTimeline : SchedulerUiState()

    data class ConflictDetected(
        val existingTaskTitle: String,
        val proposedTime: String,
        val conflictReason: String
    ) : SchedulerUiState()

    data class TaskConfirm(
        val title: String,
        val startTime: String,
        val endTime: String,
        val urgency: String,
        val contextTags: List<String>
    ) : SchedulerUiState()

    object Executing : SchedulerUiState()
    data class Success(val message: String) : SchedulerUiState()
    data class Error(val message: String) : SchedulerUiState()
}
```

## Invariants

- `Idle` means the drawer is stable and not presenting a transient execution branch.
- `ScanningTimeline` means the UI is resolving or refreshing scheduler context before showing the next branch.
- `ConflictDetected` means creation remains viable, but the user must see the visible collision context.
- `TaskConfirm` means the system has a concrete candidate task ready for explicit confirmation.
- `Executing` means the confirmed mutation is in flight and the drawer should suppress duplicate user actions.
- `Success` means the mutation completed and the drawer can surface the completion message.
- `Error` means the drawer must show explicit fast-fail feedback instead of silently dropping the action.
- scheduler warning/error copy must remain scheduler-owned UI language; raw model, extractor, classifier, or JSON/parser wording is not a lawful visible `Error` message.

## Scheduler Card Indicator Contract

Collapsed scheduler cards must preserve separate glanceable signals for urgency, conflict, and completion.

### Urgency Channel

- The vertical urgency bar is owned by `ScheduledTask.urgencyLevel`.
- The accepted urgency values are `L1_CRITICAL`, `L2_IMPORTANT`, `L3_NORMAL`, and `FIRE_OFF`.
- The shared drawer currently maps them as:
  - `L1_CRITICAL` -> red danger bar
  - `L2_IMPORTANT` -> amber warning bar
  - `L3_NORMAL` -> blue default bar
  - `FIRE_OFF` -> muted neutral low-emphasis bar
- Lower UI layers may restyle the exact tokens, but the bar must remain the scheduler card's urgency channel rather than being repurposed for conflict or done state.

### Conflict Channel

- Conflict visibility is a separate card treatment from urgency.
- A conflicted task must still show its urgency bar.
- Conflict state must not be hidden, replaced, or implied only through the urgency treatment.

### Completion Channel

- Completion visibility is a separate card treatment from urgency.
- A completed task may visually de-emphasize the same urgency bar, but it must not replace it with a completion-only color.
- Completion state must not erase or redefine the underlying urgency classification.

### Reminder Bell Progress Contract

- Collapsed scheduler cards must render reminder progress from the normalized reminder cascade rather than from raw `hasAlarm` alone.
- The collapsed reminder indicator is a bell strip: one bell per normalized cascade slot.
- Active bells keep the task urgency-owned accent color.
- Fired bells turn muted grey.
- The normalized bell counts are:
  - `L1_CRITICAL` -> 3 bells
  - `L2_IMPORTANT` -> 2 bells
  - `L3_NORMAL` -> 1 bell
  - `FIRE_OFF` -> 1 bell
- Reminder progress is per-slot. Example: a 3-slot cascade after the first reminder fires shows `1 grey + 2 active`; after the second fires it shows `2 grey + 1 active`.
- Completed/crossed-off cards may keep the bell strip only when the completed-memory projection still has reminder metadata; when present, those bells render muted grey.

## Calendar Date Attention Contract

The scheduler calendar may render temporary date-attention signaling separate from card urgency/conflict/completion.

- `unacknowledgedDates` is the normal-attention channel
- `rescheduledDates` is the warning-priority channel
- if a date exists in both sets, the warning-priority channel wins visually
- first user tap on that date must acknowledge and clear the attention state for that date
