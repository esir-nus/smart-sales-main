# Scheduler Domain (Dedicated Mutation Module)

> **OS Layer**: RAM Application (Orchestrates Memory/SSD Persistence)

## Context
This is the core domain engine responsible for executing the One-Currency LLM intents parsed by the `SchedulerLinter`. It operates purely in Kotlin to guarantee deterministic behavior, mathematical time checks, and atomic Database transactions.

## Core Responsibilities

1. **Atomic Batch Creation**: Execute lists of new `TaskDefinition` inputs into actual `ScheduledTask` entities, safely batch-saving them to the Room DB.
2. **Deterministic Conflict Evaluation**: Before inserting, query the `ScheduleBoard` to find temporal overlaps. If found, the task is **still created**, but flagged with `hasConflict = true` to trigger the "Small Attention Flow" in the UI.
3. **Optimistic Vague States**: Tasks missing explicit time boundaries are saved with `isVague = true` (placed in Purgatory, off the main board) but visually red-flagged.
4. **Lexical Fuzzy Matching**: The LLM provides the `targetQuery` (e.g. "下午的会"). This module iterates over active `upcomingItems` to find exactly ONE match.
5. **Atomic Reschedule (Delete -> Insert)**: A `Reschedule` strictly consists of deleting the old matched task, and inserting the new mapped task while **forcefully inheriting the original GUID**. This is wrapped in a Room `@Transaction` to emit only one UI update to Compose.

## Missing Rules and Edges
- **Global Constraints**: Voice deletion is blocked upstream (Linter). Batch reschedule is blocked.
- **Auto-Expiry**: Past-due tasks implicitly expire and are dropped from active `ScheduleBoard` matching to prevent lexical hallucination.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **17** | Atomic Operations & Conflict Evaluation | ✅ SHIPPED | Lexical Matching, Conflict Checks, `@Transaction` Rescheduling |
