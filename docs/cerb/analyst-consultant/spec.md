# Analyst Consultant

> **Cerb-compliant spec** — Phase 1: Chat & Intent Evaluation for Analyst Mode.
> **OS Layer**: RAM Application (operates on input EnhancedContext)
> **State**: SPEC_ONLY

---

## Overview

The `analyst-consultant` acts as the human-in-the-loop gatekeeper for deep analysis.
It evaluates the user's request against the `EnhancedContext` (RAM) passed by the Orchestrator.
If the context is insufficient (e.g. asking about a client not in RAM and not explicitly stated in chat), it asks for clarification.
It does not build the plan or perform the analysis.

**Key Principles**:
1. **Simple Parsing**: Returns a simple boolean for sufficiency. Do not invent complex linters here.
2. **Stateless Logic**: Relies entirely on the `EnhancedContext` and `sessionHistory` provided.
3. **Conversational**: Generates a natural conversational response if `info_sufficient` is false.

---

## Interaction Rules

| Rule | Enforcer | Explanation |
|------|----------|-------------|
| **Boolean Fallback** | JSON Parser | Always fallback to `false` for `info_sufficient` if parsing fails. |
| **No File Execution** | Prompt | Consultant must not attempt to use `TaskBoard` or execution tools. |

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Interface & Fakes** | 🔲 PLANNED | `ConsultantService`, `ConsultantResult`, `FakeConsultantService`. |
| **2** | **LLM Integration** | 🔲 PLANNED | `RealConsultantService` using `qwen3-max`. System prompt and simple JSON parser. |
