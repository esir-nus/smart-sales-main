# Analyst Consultant

> **Cerb-compliant spec** — Phase 1: Chat & Intent Evaluation for Analyst Mode.
> **OS Layer**: RAM Application (operates on input EnhancedContext)
> **State**: PARTIAL

---

## Overview

The `analyst-consultant` acts as the human-in-the-loop gatekeeper for deep analysis. It evaluates the user's request against the `EnhancedContext` (RAM) passed by the Orchestrator, acting as a **3-Tier Intent Gateway** to protect expensive downstream LLM calls from conversational noise.

### 3-Tier Intent Gateway (query_quality)
Before running deep analysis, the Consultant classifies the raw input:
1. **NOISE** (`noise`): Greetings, acknowledgments (e.g., "Got it", "Okay"). Halts pipeline, returns conversational ACK.
2. **VAGUE** (`vague`): Unclear references (e.g., "What did he say?"). Halts pipeline, returns clarification request.
3. **ACTIONABLE** (`actionable`): Clear business intent. Proceeds to evaluate `info_sufficient` and `missing_entities`.

### Evaluation Logic (Actionable only)
If the query is Actionable, it evaluates sufficiency. If the context is missing required CRM entities (and the user explicitly asks about them), it asks for clarification or triggers disambiguation. 

**Key Principles**:
1. **Intent First**: Always classify `query_quality` before extracting entities to prevent hallucinated traps on noise.
2. **Stateless Logic**: Relies entirely on the `EnhancedContext` and `sessionHistory` provided.
3. **Conversational**: Generates a natural conversational response if blocked by Noise, Vague intent, or insufficient info.

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
| **4** | **3-Tier Intent Gateway** | 🔲 PENDING | `query_quality` classification (noise, vague, actionable) to short-circuit pipeline. |
