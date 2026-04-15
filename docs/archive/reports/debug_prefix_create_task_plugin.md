# 🔧 Pre-Fix Report: The Missing Scheduler Plugin

## 1. Evidence-Based Root Cause (DIAGNOSE)
- **Symptom**: "No cards were shown" after a voice prompt like "明天下午三点跟进新客户"
- **Audit Findings**: 
  - The voice prompt lacked explicit CRM context, so `LightningRouter` bypassed Path A (Fast-Track).
  - The prompt fell through to Path B (System II Pipeline).
  - `RealUnifiedPipeline` successfully decoded the LLM output into `TaskMutation` and emitted `PipelineResult.ToolDispatch("CREATE_TASK", params)`.
  - `IntentOrchestrator` detected `isVoice = true` and perfectly executed its expected role: auto-committing the plugin via `toolRegistry.executeTool("CREATE_TASK")`.
  - **BUG**: `RealToolRegistry` failed silently with `flowOf(UiState.Error("Unknown tool ID", false))` because the codebase **does not contain any `SchedulerPlugin` implementation for `CREATE_TASK`**.
- **Conclusion**: The task vanishes into essentially a black hole in System III.

## 2. Literal Spec Alignment Gate (/06-audit)
**Component**: `RealUnifiedPipeline` vs `plugins`

| Spec Line | Spec Says (VERBATIM) | Code Says (VERBATIM) | Match |
| :--- | :--- | :--- | :---: |
| `SchedulerDrawer.md` L28 | `Atomic UI Teardown & Clarification...` | No mention of plugin | - |
| `RealUnifiedPipeline.kt` L268| `// Wave 16 T1: Safely pass the raw scheduling data as tool dispatches until T3 wires up the real plugin` | `pipeline` emits `CREATE_TASK` | ❌ GAP |

**Verdict**: This confirms a documented tech debt hole. The pipeline delegates to an unimplemented System III plugin.

## 3. Senior Engineer Synthesis (/01-senior-reviewr)
### 🔴 Hard No (Stop Doing This)
Letting the gatekeeper (`IntentOrchestrator`) silently swallow `UiState.Error` emitted by the `RealToolRegistry` without bubbling an exception or at least an `Log.e` makes this system impossible to inspect without an IDE debugger. 

### 💡 What I'd Actually Do (Pragmatic Path)
1. Stop patching view models. The architecture assumes a `SchedulerPlugin` exists. Build it.
2. The `SchedulerPlugin` should implement `PrismPlugin`, parse the `TaskMutation` JSON, and inject the `ScheduledTaskRepository` to execute the database insertion logic.

## 4. Proposed Fix Plan (PROPOSE)
- **Target**: Create `SchedulerPlugin.kt` in `:app-core` (or wherever plugins live).
- **Target 2**: Register it in `RealToolRegistry` DI binding.
- **Target 3**: Add error logging in `IntentOrchestrator`'s auto-commit block so future missing plugins don't fail silently.
- **Readiness Score**: **98%** (Root cause explicitly verified by missing code file).

**[🔒 PROPOSE -> FIX Gate]**: I am halted at the Absolute Zero Violation Gate. Please review this evidence-based report and reply "approve" to authorize creating the `SchedulerPlugin`.
