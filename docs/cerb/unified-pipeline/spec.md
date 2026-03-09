# Unified Pipeline (System II)

> **OS Layer**: RAM Application (Context Assembly & Routing)
> **Replaces**: `analyst-orchestrator`
> **State**: PARTIAL

## Overview

The `unified-pipeline` is the central traffic cop for the Assistant (System II), operating strictly *after* the Mascot has filtered out NOISE and GREETINGS. 
It replaces the complex state-machine driven `analyst-orchestrator` with a linear, high-performance Extract-Transform-Load (ETL) pipeline, entirely decoupled from background Reinforcement Learning operations. It is directly invoked by the `IntentOrchestrator` (Layer 3), never by the presentation layer.

## The Linear Execution Flow

When a user provides substantive input, the pipeline executes the following sequence:

### 1. Parallel Kickoff (Fast Path)
- **Background (Write)**: A copy of the input is dispatched to the RL Module to trigger habit learning. This is a fire-and-forget operation to prevent latency.
- **Foreground (Read)**: A fast Turbo model extracts core clues (`time`, `location`, `person`, `intent`).

### 2. Semantic Disambiguation
- Fetches a Candidate List of recent entities from the SSD (EntityRegistry).
- Passes the Clues and Candidate List to a lightweight LLM Disambiguator to handle typos, aliases, and homophones (bypassing strict SQL matching).
- **The "Two-Ask" CRM Rule**: If the entity is central (Person/Company) and missing or highly ambiguous, the pipeline halts and asks for clarification once (*"Did you mean Acme Corp?"*). If still unclear after 2 turns, it saves as a Draft/Unassigned entity instead of trapping the user in a loop.

### 3. Context ETL (Only when `info_sufficient = true`)
- Once entities are resolved, executes parallel DB reads:
  - User Metadata
  - Habits (read-only, written asynchronously by RL)
  - Session Memory
  - Kanban / Schedule
- Assembles a single `PipelineContext` payload. Note: The pipeline relies on pure Coroutines (`async`/`await`), entirely avoiding complex Redux/State Machines for data fetching.

### 4. LLM Execution & Routing
The Agent receives the context and determines the outcome:
- **Conversational Verdicts**: If the LLM determines a specific plugin (e.g., Export CSV, Talk Simulator) is needed, it generates a native text tip explaining *why* it recommends the tool rather than rendering intrusive UI buttons automatically.
- **Expert Bypass (Functional Calling)**: If the user explicitly asks for a tool ("Give me the PDF report"), the pipeline bypasses conversational planning and triggers the plugin immediately.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core Linear ETL | ✅ SHIPPED | Interface + Fake + RealImpl (Parallel kickoff, Context Assembly) |
| **2** | Semantic Disambiguation | 🔲 PLANNED | Re-wire lightweight LLM Disambiguator + "Two-Ask" logic |
| **3** | Tool Execution Routing | 🔲 PLANNED | Hook up Plugin Registry for Functional Calling and Verdicts |
