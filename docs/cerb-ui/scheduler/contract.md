# Scheduler Drawer UI Contract

> **Status:** Active UI state contract
> **Last Updated:** 2026-03-18
> **Behavioral Authority Above This Doc:** `docs/core-flow/scheduler-fast-track-flow.md`
> **Implementation Shards Beneath This Doc:**
> - `docs/cerb/scheduler-path-a-spine/spec.md`
> - `docs/cerb/scheduler-path-a-uni-a/spec.md`

## Purpose

This document is the UI-facing source of truth for the sealed states emitted by the Scheduler Drawer presentation layer.

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
