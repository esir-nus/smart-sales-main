# Scheduler Drawer UI Contract

> **Status:** Active UI state contract
> **Last Updated:** 2026-03-18
> **Behavioral Authority Above This Doc:** `docs/core-flow/scheduler-fast-track-flow.md`
> **Implementation Shards Beneath This Doc:**
> - `docs/cerb/scheduler-path-a-spine/spec.md`
> - `docs/cerb/scheduler-path-a-uni-a/spec.md`

## Purpose

This document is the UI-facing source of truth for the sealed states emitted by the Scheduler Drawer presentation layer.
It also owns the scheduler card indicator contract used by the scheduler surface outside the transient drawer states.

It does not redefine scheduler business behavior.
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

## Scheduler Card Indicator Contract

Collapsed scheduler cards must preserve separate glanceable signals for urgency, conflict, and completion.

### Urgency Channel

- The vertical urgency bar is owned by `ScheduledTask.urgencyLevel`.
- The accepted urgency values are `L1_CRITICAL`, `L2_IMPORTANT`, `L3_NORMAL`, and `FIRE_OFF`.
- Lower UI layers may choose the exact color tokens, but the bar must remain the scheduler card's urgency channel rather than being repurposed for conflict or done state.

### Conflict Channel

- Conflict visibility is a separate card treatment from urgency.
- A conflicted task must still show its urgency bar.
- Conflict state must not be hidden, replaced, or implied only through the urgency treatment.

### Completion Channel

- Completion visibility is a separate card treatment from urgency.
- A completed task may be visually de-emphasized, but completion must remain explicitly visible.
- Completion state must not erase or redefine the underlying urgency classification.

## Calendar Date Attention Contract

The scheduler calendar may render temporary date-attention signaling separate from card urgency/conflict/completion.

- `unacknowledgedDates` is the normal-attention channel
- `rescheduledDates` is the warning-priority channel
- if a date exists in both sets, the warning-priority channel wins visually
- first user tap on that date must acknowledge and clear the attention state for that date
