# Unified Pipeline (System II)

> **OS Layer**: RAM Application (Context Assembly & Routing)
> **Replaces**: `analyst-orchestrator`
> **State**: PARTIAL

## Overview

The `unified-pipeline` is the central traffic cop for the Assistant (System II), operating strictly *after* the Mascot has filtered out NOISE and GREETINGS. 
It replaces the complex state-machine driven `analyst-orchestrator` with a linear, high-performance Extract-Transform-Load (ETL) pipeline, entirely decoupled from background Reinforcement Learning operations. It is directly invoked by the `IntentOrchestrator` (Layer 3), never by the presentation layer.

## The Linear Execution Flow

When a user provides substantive input, the pipeline executes the following sequence:

### 1. Kickoff (Fast Path)
- **Background (Write/RL) [WIP]**: *Planned* copy of the input dispatched to the RL Module to trigger habit learning. Currently deferred/faked.
- **Foreground (Read)**: A fast Turbo model extracts core clues (`time`, `location`, `person`, `intent`).

### 2. Semantic Disambiguation (The Gateway)
- `UnifiedPipeline` checks `EntityDisambiguator.process(input)`. 
- If input is currently intercepted, it evaluates the clarification. If resolved, `UnifiedPipeline` natively writes the new alias/profile update via `EntityWriter`.
- If pass-through, `InputParser` executes. If ambiguous entities are found, it triggers `startDisambiguation()` and yields back to the user.

### 3. Context ETL (Only when `info_sufficient = true`)
- Once entities are resolved, executes parallel DB reads:
  - User Metadata
  - Habits (read-only, written asynchronously by RL)
  - Session Memory
  - Kanban / Schedule
- Assembles a single `PipelineContext` payload. Note: The pipeline relies on pure Coroutines (`async`/`await`), entirely avoiding complex Redux/State Machines for data fetching.
- **State Streaming**: Emits `PipelineResult.Progress` at major milestones (e.g., "正在梳理上下文...") to update the UI instantly without waiting for the full LLM completion.

### 4. LLM Execution & Twin Writers
- The Agent (`Executor`) receives the compiled context (`PromptCompiler`) and returns a structured JSON result.
- The result is validated via Evaluators (e.g., `SchedulerLinter` enforcing the strict `UnifiedMutation` contract).
- **Typed Branch Split**:
  - Entity/profile mutations become `MutationProposal` and commit later through `EntityWriter`.
  - Scheduler create/delete/reschedule outcomes become typed scheduler task-command proposals, not generic plugin dispatch.
  - Pure workflow suggestions remain recommendations for the plugin lane.
- **Execution Ownership**: The `IntentOrchestrator` owns confirmation and execution handoff. Scheduler task commands are executed through scheduler-owned paths (`FastTrackMutationEngine` / `ScheduledTaskRepository` / `ScheduleBoard`), while plugin execution remains outside this seam.

## Scheduler Routing Contract (2026-04-03)

Scheduler-bound text in `RealUnifiedPipeline` now follows the shared scheduler intelligence stack before it trusts the generic analyst JSON block.

Delivered routing order:

1. Build the shared `SchedulerIntelligenceRouter` with the core `SchedulerPathACreateInterpreter` plus shared reschedule extractors.
2. Route the text as `PATH_B_TEXT`:
   - global reschedule detection + extraction first
   - then deterministic create / chained create / `Uni-M`
   - then `Uni-A` / `Uni-B`
3. Emit a typed `PipelineResult.TaskCommandProposal` with one of:
   - `SchedulerTaskCommand.CreateTasks`
   - `SchedulerTaskCommand.CreateVagueTask`
   - `SchedulerTaskCommand.CreateBatch`
   - `SchedulerTaskCommand.RescheduleTask`
4. Fall back to the legacy `UnifiedMutation` JSON scheduler parsing path only when the shared scheduler router is unavailable or returns no scheduler match.

This closes the earlier Path A / Path B drift where voice used the newer extraction contracts but text still relied directly on the legacy JSON mutation block.

## Later-Lane Suppression Contract

`UnifiedPipeline` may still continue after an early Path A scheduler commit, but later scheduler work is no longer allowed to mutate the same thread twice.

Delivered rule:

- if `IntentOrchestrator` already recorded a terminal scheduler commit for the current `unifiedId`, later scheduler `TaskCommandProposal` or scheduler tool dispatch from `UnifiedPipeline` must be suppressed
- the suppression guard now lives in the shared Path A owner through `SchedulerTerminalCommit`
- non-scheduler downstream work may still continue normally

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core Linear ETL | ✅ SHIPPED | Interface + Fake + RealImpl (Parallel kickoff, Context Assembly) |
| **2** | Semantic Disambiguation | ✅ SHIPPED | `EntityDisambiguator` integration and explicit writing via `EntityWriter` |
| **3** | Extracted LLM Execution | ✅ SHIPPED | PromptCompiler, Executor, and SchedulerLinter integration |
| **4** | Transparent Mind State Streaming | ✅ SHIPPED | Emit intermediate `PipelineResult.Progress` during execution |
| **5** | Analyst Harmonization (Mono Contract) | ✅ SHIPPED | Migrate Analyst/Consultant pipeline to conform to multiple-choice selection |
