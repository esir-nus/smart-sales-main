# Pipeline Valve Telemetry Status

> **Purpose**: This document tracks the implementation status of `PipelineValve` checkpoints across the Data-Oriented OS (Project Mono).
> **Goal**: 100% Observability. Zero "Ghost Passengers" in the ETL flow.

## 🚦 Core Pipeline (The Main Highway)

These checkpoints track the primary life cycle of a user request through `IntentOrchestrator` and `RealUnifiedPipeline`.

| Checkpoint | Location | Status | Payload Metric |
|------------|----------|--------|----------------|
| `[INPUT_RECEIVED]` | `IntentOrchestrator.kt` | ✅ SHIPPED | String Length |
| `[ROUTER_DECISION]` | `IntentOrchestrator.kt` | ✅ SHIPPED | 1 (Classification) |
| `[ALIAS_RESOLUTION]` | `RealUnifiedPipeline.kt` | ✅ SHIPPED | Entity Count |
| `[LIVING_RAM_ASSEMBLED]` | `RealUnifiedPipeline.kt`| ✅ SHIPPED | Node Count (History + Profiles) |
| `[LLM_BRAIN_EMISSION]` | `RealUnifiedPipeline.kt` | ✅ SHIPPED | String Length (JSON) |
| `[LINTER_DECODED]` | `RealUnifiedPipeline.kt` | ✅ SHIPPED | Mutation Count |

## 🏙️ Specific Domains (The Towns)

As we decentralize the monolith into specific domain plugins (like Scheduler, CRM), they must implement their own local valves to track data entering and leaving their specific boundaries.

### Wave 16: Scheduler Plugin

| Checkpoint | Planned Location | Status | Payload Metric |
|------------|------------------|--------|----------------|
| `[PLUGIN_DISPATCH_RECEIVED]` | `SchedulerToolPlugin.kt` | 🔲 PENDING | Parameter Count |
| `[DB_WRITE_EXECUTED]` | `RealScheduledTaskRepository.kt` | 🔲 PENDING | Entity ID |
| `[UI_STATE_EMITTED]` | `SchedulerViewModel.kt` | 🔲 PENDING | UI State Variant |

### CRM Writer Plugin

| Checkpoint | Planned Location | Status | Payload Metric |
|------------|------------------|--------|----------------|
| `[ENTITY_MERGE_STARTED]` | `EntityWriter.kt` | 🔲 PENDING | Schema Diff Size |
| `[DB_WRITE_EXECUTED]` | `EntityWriter.kt` | 🔲 PENDING | Entity ID |

---

## Maintenance Rules

1. **Never mark SHIPPED without a Test Run**: You must prove the valve prints to `adb logcat -s VALVE_PROTOCOL` before checking the box.
2. **One Currency**: Payload metrics should represent structural complexity (count, size, ID), not arbitrary text.
