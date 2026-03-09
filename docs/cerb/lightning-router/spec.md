# Lightning Router (Phase 0)

> **Cerb-compliant spec** — Intent & Fast-Track Evaluation Gateway
> **OS Layer**: RAM Application (operates on input EnhancedContext)
> **State**: IN PROGRESS (Migration to Top-Level)

---

## Overview

The `LightningRouter` acts as the human-in-the-loop gatekeeper for the entire Dual-Engine architecture. It sits at **Phase 0** within the `IntentOrchestrator`, determining whether user input should be handled by the ephemeral **Mascot (System I)** or routed to the heavy-duty **UnifiedPipeline (System II)**. 

It evaluates the user's request against a minimal `EnhancedContext` (RAM), acting as a **4-Tier Intent Gateway** to protect expensive downstream LLM calls from conversational noise.

### The 4-Tier Intent Gateway

1. **`NOISE`**: Unintelligible ASR artifacts. 
   - *Routing*: Handled by Mascot (System I).
2. **`GREETINGS`**: Pure social interaction requiring no CRM state.
   - *Routing*: Handled entirely by Mascot (System I) to provide immediate emotional feedback.
3. **`SIMPLE_QA`**: Factual queries solvable purely from the minimal loaded RAM without a structured plan. 
   - *Routing*: Fast-tracked by the Lightning Router itself using `qwen-plus`, returning an immediate answer.
4. **`DEEP_ANALYSIS` / `CRM_TASK`**: Requires multi-step reasoning, schedule modification, or deep investigation.
   - *Routing*: Passed directly to the full `UnifiedPipeline` (System II) without further inspection by this router.

---

## Interaction Rules

| Rule | Enforcer | Explanation |
|------|----------|-------------|
| **Gateway Routing** | IntentOrchestrator| If `query_quality` is NOISE or GREETINGS, `UnifiedPipeline` is never called. |
| **Lazy Context** | ContextBuilder | The Router runs on bare minimum context (`MINIMAL`) to ensure near-zero latency. |
| **No File Execution** | Prompt | Router must not attempt to use execution tools. |

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Interface & Fakes** | ✅ SHIPPED | `ConsultantService` (Legacy name), `ConsultantResult`, `FakeConsultantService`. |
| **2** | **LLM Integration** | ✅ SHIPPED | `RealConsultantService` using `qwen3-max`. System prompt and simple JSON parser. |
| **3** | **Entity Intent Extraction** | ✅ SHIPPED | JSON schema update to extract `missing_entities` for disambiguation loop. |
| **4** | **Lightning Router (Phase 1)** | ✅ SHIPPED | `query_quality` 4-tier routing with `qwen-turbo` and `ContextDepth.MINIMAL` to solve latency. |
| **5** | **Architecture Nuke & Pave** | ✅ DONE | Move Router to the top of `IntentOrchestrator`, severing ties from `AnalystPipeline`. Rename interfaces. Migrated to UnifiedPipeline. |
