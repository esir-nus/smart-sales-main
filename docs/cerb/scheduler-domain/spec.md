> **OS Layer**: SSD Storage

# Scheduler Domain Spec

## Overview
This shard is the absolute Source of Truth for Scheduler data models and constraints. It acts as the "SSD Storage" layer. It only understands data integrity.

## Task Completion Lifecycle
Tasks have an `isDone` state.
- `isDone = false`: Standard task rules. Active in Actionable feed.
- `isDone = true`: Deleted from Actionable feed, migrated to Memory Center (`SCHEDULE_ITEM`). Alarms are cancelled. **One-Way Migration**.

## Alarm Cascade System
This layer computes absolute alarm times based on the relative array `alarmCascade`.
- Example: `L1_CRITICAL` generates `[-2h, -1h, -30m, -15m, -5m, 0m]`. `AlarmScheduler` receives absolute times based on `startTime`.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1-13** | Core Persistence | ✅ SHIPPED | `TaskEntity`, `ScheduledTaskRepository`, Alarms. |
| **14** | `unifiedID` Infrastructure | ✅ IMPLEMENTED | ASR generates `unifiedID`, Entity acts on it. |
