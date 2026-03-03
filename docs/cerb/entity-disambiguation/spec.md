# Entity Disambiguation Spec

> **Cerb-compliant spec** — The "Interrupt & Resume" state machine for curing CRM entities.
> **OS Layer**: RAM Application (Stateful Router)
> **State**: ✅ SHIPPED

---

## Overview

The `EntityDisambiguator` is a **Stateful RAM Application**. It prevents the `PrismOrchestrator` from becoming a bloated "God Object" by encapsulating the complex "Interrupt & Resume" logic required when an entity is missing or ambiguous.

**Core Responsibilities:**
1. **Hold State**: Maintain the `PendingIntent` when a pipeline is paused.
2. **Intercept Input**: Catch user replies that are meant to cure the ambiguous entity.
3. **Write-Through**: Route the cure to `EntityWriter` to update the RAM cache.
4. **Resume**: Signal the Orchestrator to replay the original intent.

---

## Interaction Rules

| Rule | Explanation |
|------|-------------|
| **Orchestrator Gateway First** | `PrismOrchestrator` must pass all raw input to this service *before* calling the standard pipeline routes. If this returns `Intercepted` or `Resumed`, the Orchestrator bypasses its normal flow. |
| **No Direct Persistence** | This module does not write to SSD directly. It delegates to `EntityWriter.upsertFromClue()`. |
| **Volatile State** | `PendingIntent` lives entirely in short-lived memory. If the app dies, the disambiguation state dies (acceptable degradation). |
| **Analyst Mode Yield** | Because `PrismViewModel` currently bypasses the Orchestrator for Analyst mode, we must ensure Analyst inputs ALSO pass through this gateway to cure entities detected during Analyst workflows. |

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core State Machine | ✅ SHIPPED | `EntityDisambiguationService` interface, Models, `RealEntityDisambiguationService` with mock intercept. |
| **2** | InputParser Upgrade | ✅ SHIPPED | Add `EntityDeclaration` extraction to `RealInputParserService` to power the true interception block. |
| **3** | Orchestrator Wiring | ✅ SHIPPED | Inject into `PrismOrchestrator`. Route `NeedsClarification` → `startDisambiguation`. Route `process()` → Gateway check. |
