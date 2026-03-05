# Analyst Architect

> **Cerb-compliant spec** — Phase 2 & 3: Planning and Investigation for System II.
> **OS Layer**: RAM Application (operates on input EnhancedContext)
> **State**: DEPRECATED (Superseded by `unified-pipeline` and `plugin-registry`)

---

## Overview

The `analyst-architect` is the workhorse of System II. It handles two distinct structural phases assigned by the `analyst-orchestrator`:
1. **Phase 2 (Strategy Planning)**: Generates a clear investigation strategy formatted as natural Markdown.
2. **Phase 3 (Investigation)**: Once the user confirms the strategy, this phase performs deep reasoning over the `EnhancedContext` and produces the final analysis and Task Board suggestions.

**Key Principles**:
1. **Strict Plan-to-Execution Separation**: Phase 2 only plans. Phase 3 only executes. They are completely separate contextual passes.
2. **RAM-Bound Reasoning**: The architect does not execute backend queries. It reads the `EnhancedContext` (Section 1: Entities, Section 2: User Habits, Section 3: Client Habits) assembly.
3. **Workflow Suggestions**: Phase 3 output maps to predefined Vault IDs (e.g. `GENERATE_PDF`) for the Task Board.

---

## Interaction Rules

| Rule | Enforcer | Explanation |
|------|----------|-------------|
| **Markdown Output** | Orchestrator | Phase 2 output must be pure Markdown text for direct display via standard Bubble UI. |
| **Deferred Action** | Prompts | LLM must not claim to actually "send emails" or "export files"; it only suggests workflows. |

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Interface & Fakes** | ✅ SHIPPED | `ArchitectService`, data models, `FakeArchitectService`. |
| **2** | **Phase 2 Planning** | ✅ SHIPPED | Pure Markdown prompt for plan generation (replaces JSON/PlannerTable). |
| **3** | **Phase 3 Investigation** | ✅ SHIPPED | `InvestigationLinter`, deep reasoning prompt, Task Board suggestion parsing. |
