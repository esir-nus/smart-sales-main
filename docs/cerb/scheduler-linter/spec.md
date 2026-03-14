> **OS Layer**: RAM Application

# Scheduler Linter Spec

## Overview
This shard governs the intelligence side of Scheduling: text-to-JSON parsing, validation rules (linting), Urgency-to-Cascade translation, and LLM orchestration (Path B). It sits in RAM and processes text into Domain mutations.

## Voice Command Classification Scope
| Classification | Input Examples | Action |
|----------------|---------------|--------|
| `schedulable`  | "明天下午2点开会" | Validates time, resolves to Success or Incomplete |
| `deletion`     | "取消会议", "不去开会了" | Fuzzy match -> Deletion |
| `reschedule`   | "把会推迟两小时" | Fuzzy match -> Reschedule |
| `non_intent`   | "你好" | Reject |

## Dual-Path Pipeline (Town and Highway) protocol (Wave 14)
- **Path A (Town)**: Optimistic, lightweight parser. Triggers creation instantly based on `unifiedID`.
- **Path B (Highway)**: Heavyweight CRM lookup (UnifiedPipeline) targeting the same `unifiedID`. If ambiguous, yields State to UI without blocking creation.

## Validation Rules
- **StartTime**: Must be valid ISO.
- **Past Date**: StartTime >= today start. Cannot schedule purely in past.
- **DateTime Normalization**: Regex normalization protects against missing spaces in LLM strings before strict ISO parsing.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1-11** | Core Linting + Voice scopes | ✅ SHIPPED | `SchedulerLinter`, Deletion/Reschedule rules. |
| **14** | Path A/B Dual-Routing | 🔲 PLANNED | Fork Pipeline logic via `unifiedID`. |
