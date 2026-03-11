# Sentinel Audit - Batch 2 Report
**Date:** 2026-03-11
**Target Domains:** `session-context`, `session-history`, `scheduler`, `entity-writer`

## Executive Summary
This audit evaluated the core contextual and scheduling modules against their Cerb specifications. Two significant cases of architectural drift were discovered and remediated natively.

## Findings & Remediations

### 1. Scheduler Pipeline → EntityWriter Disconnect (Code Drift)
**Issue:** The Scheduler's `CRM_TASK` execution path in `RealUnifiedPipeline.kt` completely bypassed the `EntityWriter` for CRM entity creation. Instead of the spec-mandated 3-step EntityWriter process (Create PERSON, Create ACCOUNT, Link), the layer 3 code was improperly calling `entityDisambiguationService.startDisambiguation` and ignoring Company linkages.
**Remediation:** Rewrote the `CRM_TASK` execution block in `RealUnifiedPipeline.kt` to natively invoke `EntityWriter.upsertFromClue` for the Person and Company, and linked them via `entityWriter.updateProfile`.

### 2. Session Auto-Renaming Architecture (Spec Drift)
**Issue:** `session-history/spec.md` documented that `AgentViewModel` originated session renaming by analyzing `ChatTurn` history at the UI layer. However, the system had evolved to handle this functionally inside Layer 3 (`RealUnifiedPipeline`) via `PipelineResult.AutoRenameTriggered` using raw JSON from the LLM.
**Remediation:** Updated `session-history/spec.md` to align with the actual Layer 3 architectural implementation of the `SessionTitleGenerator`.

### 3. Test Suite Integrity
**Issue:** A previous interface update to `RealHabitListener` omitted updating `RealHabitListenerTest`, causing a compilation failure.
**Remediation:** Injected the missing `ContextBuilder` mock. The entire test suite for `:app-core` and `:core:pipeline` now passes successfully.

## Status
Batch 2 Audit is complete and all detected drift has been sealed. The OS Model boundaries between Session Context (RAM), Session History (SSD), Scheduler (Consumer), and EntityWriter (Centralized Mutator) are now rigorously enforced in both code and specification.
