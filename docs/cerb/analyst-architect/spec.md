# Analyst Architect

> **Cerb-compliant spec** — Phase 2 & 3: Planning and Investigation for Analyst Mode.
> **OS Layer**: RAM Application (operates on input EnhancedContext)
> **State**: SPEC_ONLY

---

## Overview

The `analyst-architect` is the workhorse of Analyst Mode. It handles two distinct structural phases assigned by the `analyst-orchestrator`:
1. **Phase 2 (Structured Planning)**: Generates a multi-step investigation plan formatted for the UI's `PlannerTable`.
2. **Phase 3 (Investigation)**: Once the user confirms the plan, this phase performs deep reasoning over the `EnhancedContext` and produces the final analysis and Task Board suggestions.

**Key Principles**:
1. **Strict Plan-to-Execution Separation**: Phase 2 only plans. Phase 3 only executes. They are completely separate contextual passes.
2. **RAM-Bound Reasoning**: The architect does not execute backend queries. It reads the `EnhancedContext` (Section 1: Entities, Section 2: User Habits, Section 3: Client Habits) assembly.
3. **Workflow Suggestions**: Phase 3 output maps to predefined Vault IDs (e.g. `GENERATE_PDF`) for the Task Board.

---

## Interaction Rules

| Rule | Enforcer | Explanation |
|------|----------|-------------|
| **Structured Output** | PlanLinter | Phase 2 output must be strictly parsed into `AnalysisStep` objects for the UI. |
| **Deferred Action** | Prompts | LLM must not claim to actually "send emails" or "export files"; it only suggests workflows. |

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Interface & Fakes** | 🔲 PLANNED | `ArchitectService`, data models, `FakeArchitectService`. |
| **2** | **Phase 2 Planning** | 🔲 PLANNED | `PlanLinter`, structured JSON prompt for plan generation. |
| **3** | **Phase 3 Investigation** | 🔲 PLANNED | `InvestigationLinter`, deep reasoning prompt, Task Board suggestion parsing. |
