# Entity Disambiguation Spec

> **Cerb-compliant spec** — The "Interrupt & Resume" state machine for curing CRM entities.
> **OS Layer**: RAM Application (Stateful Router)
> **State**: ✅ SHIPPED

---

## Overview

The `EntityDisambiguator` is a **Stateful RAM Application**. It prevents the `PrismOrchestrator` from becoming a bloated "God Object" by encapsulating the complex "Interrupt & Resume" logic required when an entity is missing or ambiguous.

**Core Responsibilities:**
1. **Hold State**: Maintain the `PendingIntent` when a pipeline is paused.
2. **Intercept Input**: Catch user replies that are meant to cure the ambiguous entity via `InputParser`.
3. **Yield Result**: Return a `Resolved` state with an `EntityDeclaration` constraint.
4. **Delegate Persistence**: Leave actual `EntityWriter` persistence to the parent `UnifiedPipeline`.

---

## Interaction Rules

| Rule | Explanation |
|------|-------------|
| **Pipeline Gateway First** | `UnifiedPipeline` must pass raw input to this service *before* normal parsing. If this returns `Intercepted` or `Resumed`, the Pipeline handles it directly. |
| **No Direct Persistence** | This module does not write to SSD directly nor does it call `EntityWriter`. It delegates entirely to `UnifiedPipeline` to call `EntityWriter.upsertFromClue()` via the `Resolved` return object. |
| **Volatile State** | `PendingIntent` lives entirely in short-lived memory. If the app dies, the disambiguation state dies (acceptable degradation). |
| **Orchestrator Yield** | Because `IntentOrchestrator` routes substantive intent through `UnifiedPipeline`, the disambiguation lifecycle naturally shields standard LLM processing from unresolved entities. |

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core State Machine | ✅ SHIPPED | `EntityDisambiguationService` interface, Models, `RealEntityDisambiguationService` with mock intercept. |
| **2** | InputParser Upgrade | ✅ SHIPPED | Add `EntityDeclaration` extraction to `RealInputParserService` to power the true interception block. |
| **3** | Orchestrator Wiring | ✅ SHIPPED | Inject into `PrismOrchestrator`. Route `NeedsClarification` → `startDisambiguation`. Route `process()` → Gateway check. |
