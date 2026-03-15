---
description: Pipeline Valve Audit & Sync (Evidence-Based Telemetry Check)
---
# Pipeline Valve Audit

This workflow acts as an evidence-based audit of the Data-Oriented OS Pipeline. It verifies that data payloads (Passengers) are correctly tracked across architectural junctions (Toll Booths) and ensures the documentation matches reality.

> **Persona Override**: When executing this workflow, operate with the brutal pragmatism of `/01-senior-reviewr`. Rely entirely on `grep` and grep patterns, not assumptions.

## Phase 1: The Evidence Gatherer (Code vs Docs)

1. **Read the Spec**: 
   - `view_file docs/specs/Architecture.md` (Read Section 4 for the intended checkpoints).
   - `view_file docs/plans/telemetry/pipeline-valves.md` (Read the current tracker status).
2. **Scan the Codebase (The Ground Truth)**:
   - Run a strict grep to find every implemented valve: 
     `grep -rn "PipelineValve.tag(.*Checkpoint\." app-core/ core/ data/ feature/`
3. **Compare**: Map the `grep` results against the checkboxes in `pipeline-valves.md`.

## Phase 2: Manual Sync (The Rectification)

If there is a discrepancy between the codebase (grep output) and `pipeline-valves.md`, **fix the documentation immediately**.
- If a valve exists in code but is 🔲 PENDING in docs -> Mark ✅ SHIPPED.
- If a valve is ✅ SHIPPED in docs but `grep` cannot find it -> Mark 🔲 PENDING (and raise a Red Flag).

## Phase 3: The Effectiveness Evaluation

The Senior Engineer evaluates the *quality* of the current valve implementations.

**Evaluation Criteria:**
- **Payload Metric Quality**: Is `payloadSize` a meaningful integer (e.g., list size, string length), or did someone just pass `0` to be lazy?
- **Data Dump Value**: Does `rawDataDump` actually contain the payload, or just a useless static string?
- **Coverage**: Are there blind spots? (e.g., "We track when it enters the LLM, but not when the Linter fails to parse it.")

## Phase 4: Identify Missing Junctions

Based on the `interface-map.md` and the existing valves, identify where passengers are currently driving "off the map" without a toll booth.

**Common Blind Spots to Look For:**
- **Plugin Boundaries**: When the `Core Pipeline` hands data off to a specific Plugin (e.g., Scheduler, CRM), is there a valve on the Plugin's side acknowledging receipt?
- **Database Writes**: When `EntityWriter` or a Repository actually commits to Room/SSD, is there a valve documenting the exact JSON/Fields written?
- **UI State Emssion**: When a ViewModel finally exposes `UiState` to Compose, is there a valve? 

## Output Format

Generate a strict markdown report for the user:

```markdown
## 📋 Pipeline Valve Audit Report

### 1. Code vs Tracker Sync
- [ ] List of valves found in code but missing from tracker (SYNCED)
- [ ] List of valves in tracker but missing from code (RED FLAG)

### 2. Effectiveness Evaluation
**🔴 Hard No (Useless Valves)**:
[List valves that have bad payload metrics or useless rawDataDumps]

**�� Good Calls (Strong Telemetry)**:
[List valves doing exactly what they should]

### 3. Missing Junctions (The Blind Spots)
Based on the architecture, we urgently need valves at:
1. `[PROPOSED_CHECKPOINT_NAME]` in `[File.kt]` because [Reason]
2. `[PROPOSED_CHECKPOINT_NAME]` in `[File.kt]` because [Reason]

### 4. Next Actions
Run `/feature-dev-planner` to implement the missing junctions, or manual repair of 🔴 Hard No valves.
```
