# Analyst Consultant

> **Cerb-compliant spec** — Phase 1: Chat & Intent Evaluation for System II (Orchestrator).
> **OS Layer**: RAM Application (operates on input EnhancedContext)
> **State**: PARTIAL

---

## Overview

The `analyst-consultant` acts as the human-in-the-loop gatekeeper for deep analysis. It evaluates the user's request against the `EnhancedContext` (RAM) passed by the Orchestrator, acting as a **3-Tier Intent Gateway** to protect expensive downstream LLM calls from conversational noise.

### 4-Tier Intent Gateway (Lightning Router)
Before loading heavy context or running deep analysis, the Consultant classifies the raw input using the fast `EXTRACTOR` model (`qwen-turbo`) with `ContextDepth.MINIMAL` (no document, no DB):
1. **NOISE** (`noise`): Greetings, acknowledgments (e.g., "Got it", "Okay"). Halts pipeline, returns conversational ACK.
2. **SIMPLE_QA** (`simple_qa`): Quick fact retrieval from transcripts ("What's the price?"). Bypasses Planner, triggers `DOCUMENT_QA` answer.
3. **DEEP_ANALYSIS** (`deep_analysis`): Complex strategy, comparisons. Proceeds to full pipeline (Planner Table) with `FULL` context.
4. **CRM_TASK** (`crm_task`): Entity creation, disambiguation ("Add Lei Jun"). Evaluates `info_sufficient` and `missing_entities`.

### Evaluation Logic (CRM_TASK / DEEP_ANALYSIS only)
If the query is a CRM Task or Deep Analysis, it evaluates sufficiency. If the context is missing required CRM entities (and the user explicitly asks about them), it asks for clarification or triggers disambiguation. 

**Key Principles**:
1. **Intent First**: Always classify `query_quality` using a fast router before extracting entities or building massive DB contexts.
2. **Lazy Context**: The Consultant runs on bare minimum context (`MINIMAL`) to ensure near-zero latency.
3. **Conversational**: Generates a natural conversational response if blocked by Noise, or provides direct answers for Simple QA.

---

## Interaction Rules

| Rule | Enforcer | Explanation |
|------|----------|-------------|
| **Gateway Short-Circuit** | Pipeline | If `query_quality` is NOISE or VAGUE, `Architect` is never called. |
| **Boolean Fallback** | JSON Parser | Always fallback to `false` for `info_sufficient` if parsing fails. |
| **No File Execution** | Prompt | Consultant must not attempt to use `TaskBoard` or execution tools. |

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Interface & Fakes** | ✅ SHIPPED | `ConsultantService`, `ConsultantResult`, `FakeConsultantService`. |
| **2** | **LLM Integration** | ✅ SHIPPED | `RealConsultantService` using `qwen3-max`. System prompt and simple JSON parser. |
| **3** | **Entity Intent Extraction** | ✅ SHIPPED | JSON schema update to extract `missing_entities` for disambiguation loop. |
| **4** | **Lightning Router** | ✅ SHIPPED | `query_quality` 4-tier routing with `qwen-turbo` and `ContextDepth.MINIMAL` to solve latency and rigidity. |
